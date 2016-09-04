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

package com.symphony;

import com.symphony.api.pod.model.Stream;
import com.symphony.api.pod.model.User;
import com.symphony.clients.ISymphonyClient;
import com.symphony.clients.SymphonyClient;
import com.symphony.configurations.ConfigurationProvider;
import com.symphony.configurations.IConfigurationProvider;
import com.symphony.contexts.RedditContext;
import com.symphony.contexts.ServiceContext;
import com.symphony.contexts.WebBrowserContext;
import com.symphony.formatters.MessageML;
import com.symphony.models.ISymphonyMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ryan.dsouza on 7/27/16.
 *
 * The main class of the bot --> Run from here
 */

public class SymBrowserBot {

  private static final Logger LOG = LoggerFactory.getLogger(SymBrowserBot.class);

  //Triggers the bot to respond
  private static final List<String> botTriggerWords = new ArrayList<String>() {{
    add("reddit");
    add("browser");
    add("symbrowser");
    add("sym browser");
    add("sym_browser");
  }};

  //Configuration information
  private final IConfigurationProvider configurationProvider;
  private final ISymphonyClient symphonyClient;
  private final long myUserId;

  //All possible services
  private final Map<String, ServiceContext> services;

  //For multiple rooms + concurrent response handling
  private final ConcurrentMap<String, ServiceContext> contextToChat;
  private final ExecutorService sendMessageExecutor;
  private final ExecutorService informationExecutor;


  public SymBrowserBot(IConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
    this.symphonyClient = new SymphonyClient(this.configurationProvider);
    this.myUserId = configurationProvider.getBotUserId();

    this.sendMessageExecutor = Executors.newFixedThreadPool(2); //Sends messages
    this.informationExecutor = Executors.newFixedThreadPool(10); //Gets information from Services
    this.contextToChat = new ConcurrentHashMap<String, ServiceContext>();

    WebBrowserContext webBrowserContext = new WebBrowserContext(configurationProvider);
    ServiceContext redditContext = new RedditContext(configurationProvider);
    redditContext.authenticate();

    this.services = new HashMap<String, ServiceContext>();
    this.services.put(webBrowserContext.getContextName(), webBrowserContext);
    this.services.put(redditContext.getContextName(), redditContext);
  }

  /**
   * Checks to see if the message contains a trigger word
   * @param messageText
   * @return
   */
  private static boolean containsTriggerWord(String messageText) {
    for (String triggerWord : botTriggerWords) {
      if (messageText.contains(triggerWord)) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] ryan) {

    IConfigurationProvider configurationProvider = new ConfigurationProvider();
    SymBrowserBot browserBot = new SymBrowserBot(configurationProvider);
    browserBot.start();
  }

  /**
   * Starts the bot
   */
  public void start() {
    symphonyClient.authenticate();

    User userRyan = symphonyClient.getUserForEmailAddress("ryan.dsouza@symphony.com");
    Stream ourChat = symphonyClient.getStreamWithUsers(userRyan);
    symphonyClient.sendMessage(ourChat.getId(), "Up and running");

    //Infinite loop to always listen for responses
    while (true) {
      List<ISymphonyMessage> messages = symphonyClient.getSymphonyMessages();
      for (ISymphonyMessage message : messages) {
        if (message.getSymphonyUser().getUserId() != myUserId) {
          handleIncomingMessage(message);
        }
      }
    }
  }

  /**
   * Responsible for actually handling the incoming messages
   * @param message
   */
  private void handleIncomingMessage(ISymphonyMessage message) {

    //The service they are referring to
    String messageText = message.getMessageText().toLowerCase();

    if (!containsTriggerWord(messageText)) {
      return;
    }

    //Get the relevant context
    ServiceContext relevantService = null;
    for (ServiceContext service : this.services.values()) {
      if (messageText.contains(service.getContextName().toLowerCase())) {
        relevantService = service;
      }
    }

    ServiceContext lastUsedService = contextToChat.get(message.getStreamId());

    //If they're not referring to a known service and we don't have a previously saved service
    if (lastUsedService == null && relevantService == null) {
      //Get possible service options and send them that message
      List<MessageML> possibleServices = messageMLForPossibleServiceOptions();
      Runnable possibleServicesR = new SendMessageML(possibleServices, message.getStreamId());
      this.sendMessageExecutor.execute(possibleServicesR);
    }

    //If we do have a service
    else {

      //And it's a new service, update the contextToChat room Map
      if (relevantService != null && !relevantService.equals(lastUsedService)) {
        lastUsedService = relevantService;
        contextToChat.put(message.getStreamId(), lastUsedService);
      }

      //Async process to tell the user we're getting the information
      sendTemporaryResponse(lastUsedService, message.getStreamId());

      //Actually get the information in a threadpool and send it when available
      Runnable getResponse = new GetResponse(message.getStreamId(),
          message.getMessageText(), lastUsedService);
      this.informationExecutor.execute(getResponse);
    }
  }

  /**
   * Adds a new thread to the pool that sends a message saying that we are in the process
   * of getting a reply from the relevant context
   * @param serviceContext
   * @param threadId
   */
  private void sendTemporaryResponse(ServiceContext serviceContext, String threadId) {
    MessageML tempResponse = new MessageML();
    tempResponse.addParagraph("Getting response from " + serviceContext.getContextName() +
        " context");
    Runnable temporarySender = new SendMessageML(Collections.singletonList(tempResponse), threadId);
    this.sendMessageExecutor.execute(temporarySender);
  }

  /**
   * Returns the possible service options
   * @return
   */
  private List<MessageML> messageMLForPossibleServiceOptions() {
    MessageML messageML = new MessageML();
    messageML.addParagraph("Possible services include: ");

    for (String contextName : this.services.keySet()) {
      messageML.addLineBreak();
      messageML.addBoldText(contextName);
    }

    return Collections.singletonList(messageML);
  }


  /**
   * Simple Runnable class to get the response from a context
   * And add the response to the thread pool so it can be sent
   */
  private class GetResponse implements Runnable {

    private final String threadId;
    private final String messageText;
    private final ServiceContext serviceContext;

    public GetResponse(String threadId, String messageText, ServiceContext serviceContext) {
      this.threadId = threadId;
      this.messageText = messageText;
      this.serviceContext = serviceContext;
    }

    @Override
    public void run() {
      //Actually get the response
      List<MessageML> response = serviceContext.responsesToAction(messageText);
      Runnable messageSender = new SendMessageML(response, threadId);

      //Add it the messages to the message sender threadPool
      SymBrowserBot.this.sendMessageExecutor.execute(messageSender);
    }
  }


  /**
   * Simple Runnable class to send a message
   */
  private class SendMessageML implements Runnable {

    private final List<MessageML> messageMLs;
    private final String threadId;

    public SendMessageML(List<MessageML> messageMLs, String threadId) {
      this.messageMLs = messageMLs;
      this.threadId = threadId;
    }

    @Override
    public void run() {
      //If we actually have messages
      if (this.messageMLs != null && !this.messageMLs.isEmpty()) {
        //Send each one
        for (MessageML messageML : messageMLs) {
          SymBrowserBot.this.symphonyClient.sendMessage(threadId, messageML);
        }
      }
    }
  }
}