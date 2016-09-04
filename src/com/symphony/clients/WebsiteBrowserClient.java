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

package com.symphony.clients;

import com.symphony.configurations.ConfigurationProvider;
import com.symphony.configurations.IConfigurationProvider;
import com.symphony.models.WebsiteBrowserArticle;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

/**
 * Created by ryan.dsouza on 8/6/16.
 *
 * Interacts with the Diffbot API to return the text from a website
 */

public class WebsiteBrowserClient implements IWebsiteBrowserClient {

  protected static final Logger LOG = LoggerFactory.getLogger(WebsiteBrowserClient.class);

  private static final String BASE_URL = "http://api.diffbot.com/v3/";
  private static final String ANALYZE_URL = BASE_URL + "analyze";

  private final IConfigurationProvider configurationProvider;
  private final String diffBotApiKey;

  public WebsiteBrowserClient(IConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
    this.diffBotApiKey = configurationProvider.getDiffbotApiKey();
  }

  /**
   * For testing
   */
  public static void main(String[] ryan) {
    IConfigurationProvider configurationProvider = new ConfigurationProvider();
    IWebsiteBrowserClient browser = new WebsiteBrowserClient(configurationProvider);

    String url =
        "http://www.businessinsider.com/republicans-starting-to-panic-about-how-trump-could"
            + "-affect-others-on-ballot-2016-8";

    WebsiteBrowserArticle websiteResponse = browser.getTextFromWebsite(url);
    System.out.println("BIG TITLE: " + websiteResponse.getArticleTitle());

    for (int i = 0; i < websiteResponse.getArticles().size(); i++) {
      List<WebsiteBrowserArticle.Article> articles = websiteResponse.getArticles().get(i);

      for (int y = 0; y < articles.size(); y++) {
        WebsiteBrowserArticle.Article article = articles.get(y);
        System.out.println("\t" + article.getText());
      }
    }
  }

  /**
   * Returns the contents of a website, or null if the website wasn't found
   * @param websiteUrl
   * @return
   */
  public WebsiteBrowserArticle getTextFromWebsite(String websiteUrl) {

    String encodedUrl = "";
    try {
      encodedUrl = URLEncoder.encode(websiteUrl, "UTF-8");
    } catch (IOException exception) {
      LOG.error("Error encoding: " + websiteUrl, exception);
      return null;
    }

    String fullURL = ANALYZE_URL + "?token=" + this.diffBotApiKey + "&url=" + encodedUrl;
    String jsonResponse = ClientBuilder.newClient()
        .target(fullURL)
        .request(MediaType.APPLICATION_JSON)
        .get(String.class);

    try {
      JSONObject response = new JSONObject(jsonResponse);
      WebsiteBrowserArticle websiteResponse = new WebsiteBrowserArticle(response);
      return websiteResponse;
    } catch (JSONException exception) {
      LOG.info("Error getting website", exception);
    }

    return null;
  }
}