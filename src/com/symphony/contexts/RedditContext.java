/*
 *
 *
 * Copyright 2016 Symphony Communication Services, LLC
 *
 * Licensed to Symphony Communication Services, LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.symphony.contexts;

import com.symphony.configurations.IConfigurationProvider;
import com.symphony.formatters.MessageML;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by ryan.dsouza on 7/27/16.
 *
 * The context that allows browsing Reddit
 */

public class RedditContext extends ServiceContext {

  protected static final Logger LOG = LoggerFactory.getLogger(RedditContext.class);

  private static final String contextName = "Reddit";

  private final RedditClient redditClient;

  private Command lastUsedCommand;
  private SubredditPaginator paginator;
  private List<Submission> lastUsedListing;
  private Submission lastUsedSubmission;
  private CommentNode lastUsedComment;

  private int maxRedditComments;
  private int maxRedditPosts;

  public RedditContext(IConfigurationProvider configurationProvider) {
    super(configurationProvider);
    this.redditClient = new RedditClient(UserAgent.of("RedditContextBot"));
    this.lastUsedListing = new ArrayList<>();

    this.lastUsedCommand = Command.UNRECOGNIZED;
    this.maxRedditComments = configurationProvider.getMaxRedditComments();
    this.maxRedditPosts = configurationProvider.getMaxRedditPosts();
  }

  private static void addCommentToMessageML(CommentNode comment, MessageML messageML) {
    String commentBody = comment.getComment().getBody();
    commentBody = commentBody.replace("\n", " ");

    if (!commentBody.isEmpty()) {
      messageML.addLineBreak();
      for (int i = 0; i < comment.getDepth(); i++) {
        if (comment.getDepth() > 1) {
          messageML.addParagraph("------");
          if (i == comment.getDepth() - 1) {
            messageML.addParagraph(">");
          }
        }
      }
      messageML.addParagraph(commentBody);
    }

    messageML.addItalicText(" -- " + comment.getComment().getAuthor());
    messageML.addParagraph(" (score: " + comment.getComment().getScore() + ")");
  }

  public void authenticate() {
    Credentials credentials = Credentials.userless(configurationProvider.getRedditClientId(),
        configurationProvider.getRedditSecret(), UUID.randomUUID());
    try {
      OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
      redditClient.authenticate(authData);
      LOG.debug("Authenticated Reddit Context");
    } catch (OAuthException exception) {
      LOG.error("Error authenticating Reddit client", exception);
    }
  }

  public List<MessageML> getFrontPage(String fullCommand) {

    this.paginator = new SubredditPaginator(this.redditClient);
    this.paginator.setLimit(this.maxRedditPosts);

    Listing<Submission> frontPageSubmissions = paginator.next();

    if (frontPageSubmissions != null && frontPageSubmissions.size() > 0) {
      this.lastUsedListing.clear();
      this.lastUsedListing.addAll(frontPageSubmissions);
    }

    MessageML response = new MessageML();
    response.addParagraph("Type 'Reddit frontpage #' where # is the article to view");
    response.addLineBreak();
    updateMessageMLWithListing(response);

    return Collections.singletonList(response);
  }

  public List<MessageML> getSubReddit(String fullCommand) {

    String subRedditName = fullCommand.toLowerCase();

    Command subredditCommand = Command.SUBREDDIT;
    for (String trigger : subredditCommand.getTriggerWords()) {
      if (subRedditName.contains(trigger)) {
        int start = subRedditName.indexOf(trigger);
        subRedditName = subRedditName.substring(start).replace(trigger, "");
      }
    }
    subRedditName = subRedditName.replace(" ", "");

    this.paginator = new SubredditPaginator(this.redditClient);
    paginator.setSubreddit(subRedditName);
    this.paginator.setLimit(this.maxRedditPosts);

    MessageML response = new MessageML();

    try {
      Listing<Submission> subredditSubmissions = paginator.next();

      if (subredditSubmissions != null && subredditSubmissions.size() > 0) {
        this.lastUsedListing.clear();
        this.lastUsedListing.addAll(subredditSubmissions);
        response.addParagraph("Browsing " + subRedditName + " subreddit. Type article # to view: ");
        response.addLineBreak();
        updateMessageMLWithListing(response);
      } else {
        response.addParagraph("No posts found for subreddit '" + subRedditName + "'");
        response.addLineBreak();
      }
    } catch (NetworkException exception) {
      response.addParagraph("'" + subRedditName + "' was not recognized as a valid subreddit");
      response.addLineBreak();
    }
    return Collections.singletonList(response);
  }

  private void updateMessageMLWithListing(MessageML response, List<Submission> submissions,
      int startCounter) {

    for (int i = 0; i < submissions.size(); i++) {
      Submission submission = submissions.get(i);

      try {
        response.addParagraph(String.valueOf(i + startCounter + 1) + ": ");
        response.addBoldText(submission.getTitle());
        response.addLineBreak();
        response.addParagraph("------> from ");
        response.addItalicText(submission.getSubredditName());
        response.addParagraph(" (score: " + submission.getScore());
        response.addParagraph(", comments: " + submission.getCommentCount() + ")");
        response.addLineBreak();
      } catch (NullPointerException exception) {
        LOG.error("Nullpointer exception: " + exception);
      }
    }
  }

  private void updateMessageMLWithListing(MessageML response) {
    updateMessageMLWithListing(response, this.lastUsedListing, 0);
  }

  private void updateMessageMLWithRecognizedCommands(MessageML messageML) {
    messageML.addParagraph("Recognized Reddit commands: ");
    messageML.addLineBreak();

    messageML.addBoldText("Reddit frontpage: ");
    messageML.addParagraph("View the frontpage");
    messageML.addLineBreak();

    messageML.addBoldText("Reddit subreddit <subredditName>: ");
    messageML.addParagraph("View subreddit");
    messageML.addLineBreak();

    messageML.addParagraph("After choosing frontpage or a subreddit, type: ");
    messageML.addLineBreak();

    messageML.addParagraph("--------> ");
    messageML.addBoldText("Reddit <articleNumber>: ");
    messageML.addParagraph("the number of the post to view");
    messageML.addLineBreak();

    messageML.addParagraph("--------> ");
    messageML.addBoldText("Reddit next: ");
    messageML.addParagraph("to view the next page");
    messageML.addLineBreak();

    messageML.addParagraph("--------> ");
    messageML.addBoldText("Reddit comment: ");
    messageML.addParagraph("to view the post's comments");
  }

  public List<MessageML> getRecognizedCommands() {
    MessageML response = new MessageML();
    updateMessageMLWithRecognizedCommands(response);
    return Collections.singletonList(response);
  }

  public List<MessageML> getUnrecognizedCommand(String fullCommand) {
    MessageML response = new MessageML();

    if (fullCommand == null || fullCommand.isEmpty()) {
      response.addParagraph("Did not recognize command");
    } else {
      response.addParagraph("Did not recognize '" + fullCommand + "' command");
    }

    response.addLineBreak();
    this.updateMessageMLWithRecognizedCommands(response);

    return Collections.singletonList(response);
  }

  public List<MessageML> getNextPage() {
    Listing<Submission> nextSubmission = paginator.next();
    MessageML response = new MessageML();
    response.addParagraph("More reddit");
    response.addLineBreak();
    updateMessageMLWithListing(response, nextSubmission, this.lastUsedListing.size());
    this.lastUsedListing.addAll(nextSubmission);
    return Collections.singletonList(response);
  }

  public List<MessageML> getArticleForNumber(String fullCommand) {
    fullCommand = fullCommand.replace(" ", "");
    MessageML messageML = new MessageML();
    try {
      int articleNumber = Integer.parseInt(fullCommand) - 1;
      if (this.lastUsedListing == null || this.lastUsedListing.size() == 0) {
        messageML.addParagraph("Must say either 'Reddit frontpage' or " +
            "'Reddit Subreddit subRedditName' before chosing an article");
      } else if (articleNumber < 0 || articleNumber >= this.lastUsedListing.size()) {
        messageML.addParagraph("Error: invalid article selection" +
            "Valid range: 1 to " + this.lastUsedListing.size());
      } else {
        this.lastUsedSubmission = this.lastUsedListing.get(articleNumber);
        updateMessageWithLastUsedSubmission(messageML);
      }
    } catch (NumberFormatException exception) {
      messageML.addParagraph("Error parsing your article selection: " + fullCommand);
      LOG.error("Error parsing", exception);
    }

    return Collections.singletonList(messageML);
  }

  private void updateMessageWithLastUsedSubmission(MessageML messageML) {

    String selfText = this.lastUsedSubmission.getSelftext();

    messageML.addParagraph(this.lastUsedSubmission.getTitle());
    messageML.addLineBreak();

    if (selfText != null && selfText.length() > 1) {
      String[] paragraphs = selfText.split("\n");
      for (String paragraph : paragraphs) {
        messageML.addParagraph(paragraph);
        messageML.addLineBreak();
      }
    } else {
      messageML.addParagraph(this.lastUsedSubmission.getUrl());
    }
  }

  public List<MessageML> getCommentsForPost() {
    MessageML response = new MessageML();

    if (this.lastUsedListing == null || this.lastUsedListing.size() == 0) {
      response.addParagraph("Must call 'Reddit frontpage' or 'Reddit subreddit <subredditName> ");
      response.addParagraph("before being able to view comments");
    } else if (this.lastUsedSubmission == null) {
      response.addParagraph("Must choose a Reddit post ('Reddit <postNumber>') ");
      response.addParagraph("before being able to view comments");
    } else {
      this.lastUsedSubmission = this.redditClient.getSubmission(this.lastUsedSubmission.getId());
      this.lastUsedComment = this.lastUsedSubmission.getComments();
      Iterable<CommentNode> iterable = this.lastUsedComment.walkTree();

      response.addParagraph("Comments for " + this.lastUsedSubmission.getTitle());
      response.addLineBreak();

      int counter = 0;

      for (CommentNode node : iterable) {
        if (counter > this.maxRedditComments) {
          break;
        }
        addCommentToMessageML(node, response);
        counter++;
      }
    }

    return Collections.singletonList(response);
  }

  @Override
  public List<MessageML> responsesToAction(String action) {

    Command command = Command.getCommandForText(action);

    if (command != Command.UNRECOGNIZED) {
      this.lastUsedCommand = command;
    }
    action = action.toLowerCase();
    if (action.indexOf("reddit") == 0) {
      action = action.substring("reddit".length());
    }

    switch (command) {
      case FRONTPAGE:
        LOG.debug("Getting front page");
        return getFrontPage(action);
      case SUBREDDIT:
        LOG.debug("Getting subreddit");
        return getSubReddit(action);
      case NUMBER:
        LOG.debug("Getting article number");
        return getArticleForNumber(action);
      case HELP:
        LOG.debug("Getting recognized commands");
        return getRecognizedCommands();
      case NEXTPAGE:
        LOG.debug("Getting next page");
        return getNextPage();
      case COMMENTS:
        LOG.debug("Getting comments");
        return getCommentsForPost();
    }

    LOG.debug("Unrecognized command");
    return getUnrecognizedCommand(action);
  }

  @Override
  public String getContextName() {
    return contextName;
  }

  /**
   * Enum of possible Reddit commands
   */
  private enum Command {

    FRONTPAGE("frontpage", "front_page", "front page", "fp"),
    SUBREDDIT("subreddit", "sub reddit", "sub_reddit", "sr"),
    NEXTPAGE("next", "next page", "next_page", "more"),
    NUMBER(1, 100),
    COMMENTS("comment", "comments"),
    HELP("help", "h", "instructions", "guide", "reddit"),
    UNRECOGNIZED();

    private final List<String> triggerWords;

    Command(String... triggerWords) {
      this.triggerWords = new ArrayList<String>();

      for (String triggerWord : triggerWords) {
        this.triggerWords.add(triggerWord);
      }
    }

    Command(int rangeStart, int rangeEnd) {
      this.triggerWords = new ArrayList<String>();

      for (int i = rangeStart; i <= rangeEnd; i++) {
        this.triggerWords.add(String.valueOf(i));
      }
    }

    public static Command getCommandForText(String text) {
      text = text.toLowerCase();
      for (Command command : Command.values()) {
        List<String> triggerWords = command.getTriggerWords();
        for (String triggerWord : triggerWords) {
          if (text.contains(triggerWord)) {
            return command;
          }
        }
      }
      return UNRECOGNIZED;
    }

    public List<String> getTriggerWords() {
      return this.triggerWords;
    }

    @Override
    public String toString() {
      return "Command{" +
          "triggerWords=" + triggerWords +
          '}';
    }
  }


  ;
}