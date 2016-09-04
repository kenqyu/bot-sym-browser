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

package com.symphony.models;

/**
 * Created by ryan.dsouza on 7/26/16.
 */
public class SymphonyMessage implements ISymphonyMessage {

  private final ISymphonyUser symphonyUser;
  private final Message message;

  public SymphonyMessage(ISymphonyUser symphonyUser, Message message) {
    this.symphonyUser = symphonyUser;
    this.message = message;
  }

  @Override
  public ISymphonyUser getSymphonyUser() {
    return symphonyUser;
  }

  @Override
  public Message getMessage() {
    return message;
  }

  @Override
  public String getStreamId() {
    return this.message.getStreamId();
  }

  @Override
  public String getAuthorName() {
    return this.symphonyUser.getDisplayName();
  }

  @Override
  public String getMessageText() {
    return this.message.getMessage();
  }

  @Override
  public String toString() {
    return "SymphonyMessage{" +
        "symphonyUser=" + symphonyUser +
        ", messageList=" + message +
        '}';
  }
}