package com.symphony.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ryan.dsouza on 8/6/16.
 *
 * Represents the response from calling the Diffbot API
 * Used as the response from WebBrowser
 */

public class WebsiteBrowserArticle {

  protected static final Logger LOG = LoggerFactory.getLogger(WebsiteBrowserArticle.class);

  private final String articleTitle;
  private final String type;
  private final List<List<Article>> articles;

  public WebsiteBrowserArticle(JSONObject object) {
    this.articleTitle = object.getString("title");
    this.type = object.getString("type");
    this.articles = new ArrayList<List<Article>>();

    //Articles - the simplest
    if (this.type.equalsIgnoreCase("article")) {
      JSONArray subarticles = object.getJSONArray("objects");
      List<Article> articles = new ArrayList<Article>();

      //For each subarticle, get its information
      for (int i = 0; i < subarticles.length(); i++) {
        JSONObject subarticle = subarticles.getJSONObject(i);
        try {
          articles.add(new Article(subarticle));
        } catch (JSONException exception) {
          LOG.error("Error parsing articles", exception);
        }
      }
      this.articles.add(articles);
    }

    //Discussions - usually boards or threads
    else if (this.type.equalsIgnoreCase("discussion")) {
      JSONArray comments = object.getJSONArray("objects");

      //For each comment
      for (int i = 0; i < comments.length(); i++) {

        Map<Integer, List<Article>> parentAndSubComments = new HashMap<>();

        //Get its title and information
        JSONObject comment = comments.getJSONObject(i);
        String title = comment.getString("title");
        JSONArray posts = comment.getJSONArray("posts");

        //And for each of the comment's children
        for (int y = 0; y < posts.length(); y++) {

          //Get the author and text, format it, and make an Article from it
          JSONObject post = posts.getJSONObject(y);
          String commentText = post.getString("author") + ": " + post.getString("text");
          Article article = new Article(title, commentText);

          //Is a subcomment --> Get the parent list and add the comment to the list
          if (post.has("parentId")) {
            Integer parentid = post.getInt("parentId");
            List<Article> otherChildComments = parentAndSubComments.get(parentid);
            if (otherChildComments == null) {
              otherChildComments = new ArrayList<>();
              parentAndSubComments.put(parentid, otherChildComments);
            }
            otherChildComments.add(article);
          }
          //Is a parent comment --> create a list and add the parent to it
          else if (post.has("id")) {
            Integer postId = post.getInt("id");
            List<Article> subcomments = new ArrayList<>();
            subcomments.add(article);
            parentAndSubComments.put(postId, subcomments);
          }
        }

        //For each of the parent comments we have (in order)
        for (int y = 0; y <= parentAndSubComments.size(); y++) {
          //If we have that parent comment id
          if (parentAndSubComments.containsKey(y)) {
            List<Article> parentComment = parentAndSubComments.get(y);
            if (!parentComment.isEmpty()) {
              this.articles.add(parentComment);
            }
          }
        }
      }
    }
  }

  public WebsiteBrowserArticle(String articleTitle, String type,
      List<List<Article>> articles) {
    this.articleTitle = articleTitle;
    this.type = type;
    this.articles = articles;
  }

  public String getArticleTitle() {
    return articleTitle;
  }

  public String getType() {
    return type;
  }

  public List<List<Article>> getArticles() {
    return articles;
  }

  @Override
  public String toString() {
    return "WebsiteBrowserArticle{" +
        "articleTitle='" + articleTitle + '\'' +
        ", type='" + type + '\'' +
        ", articles=" + articles +
        '}';
  }

  /**
   * Represents a comment or article within a post or website
   */
  public class Article {
    private final String text;
    private final String title;

    public Article(JSONObject object) {
      this(object.getString("title"), object.getString("text"));
    }

    public Article(String title, String text) {
      this.text = text;
      this.title = title;
    }

    public String getText() {
      return text;
    }

    public String getTitle() {
      return title;
    }

    @Override
    public String toString() {
      return "Article{" +
          "text='" + text + '\'' +
          ", title='" + title + '\'' +
          '}';
    }
  }
}