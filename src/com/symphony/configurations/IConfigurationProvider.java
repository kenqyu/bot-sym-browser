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

package com.symphony.configurations;

import java.io.File;

/**
 * Provides configurations for setting up the FAQ bot
 */

public interface IConfigurationProvider {

  /**
   * Service configurations
   */
  String getRedditClientId();

  String getRedditSecret();

  int getMaxRedditPosts();

  int getMaxRedditComments();

  /**
   * Number of working threads
   */
  int getNumWorkerThreads();

  /**
   * How much time, in milliseconds, to allow for request processing before giving up
   */
  long getRequestProcessingTimeout();

  /**
   * The bot's user ID
   */
  long getBotUserId();

  /**
   * Name of certificate file for authentication with Symphony
   */
  File getCertificateFile();

  /**
   * Password of keystore for authentication with Symphony
   */
  String getSymphonyKeystorePassword();

  /**
   * Type of keystore for authentication with Symphony (e.g. pkcs12)
   */
  String getSymphonyKeystoreType();

  /**
   * URLs for setting up the API Managers
   */
  String getSymphonyBaseUrl();

  String getSymphonyWebControllerUrl();

  String getSymphonyUserInfoPath();

  String getSymphonyPodPath();

  String getSymphonyAgentPath();

  String getSymphonySbePath();

  String getSymphonyKeyManagerPath();

  /**
   * For setting up Diffbot to extract text from articles
   */
  String getDiffbotApiKey();

  int getDiffbotMaxArticles();
}