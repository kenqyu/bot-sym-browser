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

import com.symphony.clients.IWebsiteBrowserClient;
import com.symphony.clients.WebsiteBrowserClient;
import com.symphony.configurations.IConfigurationProvider;
import com.symphony.formatters.MessageML;
import com.symphony.models.WebsiteBrowserArticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ryan.dsouza on 8/7/16.
 *
 * The context to allow browsing the Internet
 */

public class WebBrowserContext extends ServiceContext {

  protected static final Logger LOG = LoggerFactory.getLogger(RedditContext.class);

  private static final String contextName = "WebBrowser";

  private final IWebsiteBrowserClient websiteBrowserClient;
  private final int maxArticles;

  private Command lastUsedCommand;
  private WebsiteBrowserArticle lastUsedArticle;
  private List<List<WebsiteBrowserArticle.Article>> lastUsedComments;

  public WebBrowserContext(IConfigurationProvider configurationProvider) {
    super(configurationProvider);

    this.maxArticles = configurationProvider.getDiffbotMaxArticles();
    this.websiteBrowserClient = new WebsiteBrowserClient(configurationProvider);
    this.lastUsedCommand = Command.UNRECOGNIZED;
  }

  @Override
  public void authenticate() {
    //No authentication needed for this context
  }

  public List<MessageML> getResponseFromLookingUp(String action) {

    String websiteUrl = action.toLowerCase();

    for (String trigger : Command.LOOKUP.getTriggerWords()) {
      if (websiteUrl.contains(trigger)) {
        int start = websiteUrl.indexOf(trigger);
        websiteUrl = websiteUrl.substring(start).replace(trigger, "");
      }
    }
    websiteUrl = websiteUrl.replace(" ", "");

    MessageML response = new MessageML();

    try {
      this.lastUsedArticle = this.websiteBrowserClient.getTextFromWebsite(websiteUrl);
      this.lastUsedComments = this.lastUsedArticle.getArticles();
      return getMessageWithLastUsedArticle();
    } catch (Exception exception) {
      LOG.error("Error getting text from website: " + websiteUrl, exception);
      response.addParagraph("Error getting text from: " + websiteUrl);
    }

    return Collections.singletonList(response);
  }

  private List<MessageML> getNextComments(String action) {

    if (this.lastUsedArticle == null) {
      MessageML noArticle = new MessageML();
      noArticle.addParagraph(
          "Must call 'Browser lookup <websiteUrl>' before calling 'Browser next'");
      return Collections.singletonList(noArticle);
    }
    if (this.lastUsedComments == null) {
      this.lastUsedComments = this.lastUsedArticle.getArticles();
    }

    if (this.lastUsedComments.isEmpty() || this.lastUsedComments.get(0).isEmpty()) {
      MessageML noComments = new MessageML();
      noComments.addParagraph("No more articles or comments to show");
      return Collections.singletonList(noComments);
    }

    return getMessageWithLastUsedArticle();
  }

  private List<MessageML> getMessageWithLastUsedArticle() {
    List<MessageML> messageMLs = new ArrayList<>();
    int counter = 0;

    MessageML title = new MessageML();
    title.addParagraph(this.lastUsedArticle.getArticleTitle());
    messageMLs.add(title);

    for (int i = 0; i < this.lastUsedComments.size(); i++) {

      MessageML messageML = new MessageML();
      messageMLs.add(messageML);

      List<WebsiteBrowserArticle.Article> articles = this.lastUsedComments.get(i);
      if (counter > this.maxArticles) {
        if (this.lastUsedComments.size() > 0) {
          this.lastUsedComments.remove(0);
        }
        return messageMLs;
      }

      messageML.addLineBreak();

      String indentation = "";
      for (int y = 0; y < i; y++) {
        if (i > 1) {
          indentation += "------";
          if (y == (i - 1)) {
            indentation += ">";
          }
        }
      }

      for (int y = 0; y < articles.size(); y++) {
        messageML.addParagraph(indentation);
        messageML.addParagraph(articles.get(y).getText());

        //Remove subarticles - as opposed to index to keep track of which one we're on
        if (counter > this.maxArticles) {
          if (articles.size() > 0) {
            articles.remove(y);
            y--;
          }
          return messageMLs;
        }
        counter++;
      }

      //Remove articles - as opposed to index to keep track of which one we're on
      this.lastUsedComments.remove(i);
      i--;
    }
    return messageMLs;
  }

  private List<MessageML> getHelpCommands(String action) {
    MessageML response = new MessageML();
    updateMessageMLWithRecognizedCommands(response);
    return Collections.singletonList(response);
  }

  private List<MessageML> getUnRecognizedCommand(String action) {
    MessageML response = new MessageML();

    if (action == null || action.isEmpty()) {
      response.addParagraph("Did not recognize command");
    } else {
      response.addParagraph("Did not recognize '" + action + "' command");
    }

    response.addLineBreak();
    this.updateMessageMLWithRecognizedCommands(response);

    return Collections.singletonList(response);
  }

  private void updateMessageMLWithRecognizedCommands(MessageML messageML) {
    messageML.addParagraph("Recognized WebBrowser commands: ");
    messageML.addLineBreak();

    messageML.addBoldText("Browser lookup <websiteUrl>: ");
    messageML.addParagraph("Get the text from a website URL");
    messageML.addLineBreak();

    messageML.addParagraph("After choosing a link to lookup, type: ");
    messageML.addLineBreak();

    messageML.addParagraph("--------> ");
    messageML.addBoldText("Browser next: ");
    messageML.addParagraph("View the next set of items from that website");
    messageML.addLineBreak();
  }

  @Override
  public List<MessageML> responsesToAction(String action) {

    Command command = Command.getCommandForText(action);

    if (command != Command.UNRECOGNIZED) {
      this.lastUsedCommand = command;
    }
    action = action.toLowerCase();
    if (action.indexOf("webbrowser") == 0) {
      action = action.substring("webbrowser".length());
    }

    switch (command) {
      case LOOKUP:
        LOG.debug("Looking up website");
        return getResponseFromLookingUp(action);

      case NEXT_COMMENTS:
        LOG.debug("Next group of comments");
        return getNextComments(action);
      case HELP:
        LOG.debug("Getting recognized commands");
        return getHelpCommands(action);

      default:
        LOG.debug("Getting recognized commands - command was not recognied");
        return getUnRecognizedCommand(action);
    }
  }


  @Override
  public String getContextName() {
    return contextName;
  }

  /**
   * Enum of possible WebBrowser commands
   */
  private enum Command {

    LOOKUP("lookup", "look up", "look_up", "openlink", "open link", "open_link"),
    NEXT_COMMENTS("next", "next page", "next_page", "more", "next comments", "next_comments"),
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
        for (String triggerWord : command.getTriggerWords()) {
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
