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

import com.symphony.api.auth.model.Token;
import com.symphony.configurations.IConfigurationProvider;
import com.symphony.models.ISymphonyUser;
import com.symphony.models.Message;
import com.symphony.models.SymphonyUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by ryan.dsouza on 7/26/16.
 *
 * Simple class to make calls to Symphony's REST API
 */

public class SymphonyRESTClient {

  protected static final Logger LOG = LoggerFactory.getLogger(SymphonyRESTClient.class);

  private final IConfigurationProvider configurationProvider;
  private final long myUserId;

  private Token sessionToken;
  private Token keyManagerToken;

  public SymphonyRESTClient(IConfigurationProvider configurationProvider, Token sessionToken,
      Token keyManagerToken) {
    this.configurationProvider = configurationProvider;
    this.myUserId = configurationProvider.getBotUserId();

    this.sessionToken = sessionToken;
    this.keyManagerToken = keyManagerToken;
  }

  /**
   * Returns the user for that userId
   * @return
   */
  public ISymphonyUser getUserForId(Long userId) {

    String fullURL = configurationProvider.getSymphonyUserInfoPath();

    MultivaluedMap formData = new MultivaluedHashMap();
    formData.add("action", "usercurrent");
    formData.add("userid", String.valueOf(userId));
    formData.add("includeFollowing", String.valueOf(false));

    String jsonResponse = ClientBuilder.newClient()
        .target(fullURL)
        .request(MediaType.APPLICATION_JSON)
        .header("X-Symphony-CSRF-Token", sessionToken.getToken())
        .cookie("skey", sessionToken.getToken())
        .post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

    try {
      JSONObject response = new JSONObject(jsonResponse);

      if (response.getString("status").equals("OK")) {
        JSONObject person = response.getJSONObject("person");
        String emailAddress = person.getString("emailAddress");
        String displayName = person.getString("prettyName");
        return new SymphonyUser(userId, emailAddress, displayName);
      }
    } catch (JSONException e) {
      LOG.error("Error parsing JSON when getting user " + jsonResponse, e);
    }

    return null;
  }

  public List<Message> getMessagesForDataFeed(String datafeedId) {

    String url =
        configurationProvider.getSymphonyAgentPath() + "/v2/datafeed/" + datafeedId + "/read";
    List<Message> messages = new ArrayList<>();

    String jsonResponse = ClientBuilder.newClient()
        .target(url)
        .request(MediaType.APPLICATION_JSON)
        .header("sessionToken", sessionToken.getToken())
        .header("keyManagerToken", keyManagerToken.getToken())
        .get(String.class);

    if (jsonResponse == null || jsonResponse.length() == 0) {
      return messages;
    }

    try {
      JSONArray array = new JSONArray(jsonResponse);
      for (int i = 0; i < array.length(); i++) {
        JSONObject object = array.getJSONObject(i);
        if (object.getString("v2messageType").equals("V2Message")) {
          long senderId = object.getLong("fromUserId");
          if (senderId != this.myUserId) {
            Message message = new Message(object);
            messages.add(message);
          }
        }
      }
    } catch (JSONException exception) {
      LOG.error(exception.toString());
    }
    return messages;
  }

  public void setSessionToken(Token sessionToken) {
    this.sessionToken = sessionToken;
  }

  public void setKeyManagerToken(Token keyManagerToken) {
    this.keyManagerToken = keyManagerToken;
  }
}