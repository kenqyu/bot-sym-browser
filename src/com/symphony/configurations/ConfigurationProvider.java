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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by ryan.dsouza on 7/22/16.
 *
 * Defines specifics needed for configuring the Symphony Client
 */

public class ConfigurationProvider implements IConfigurationProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationProvider.class);

  private static final String PROP_FILE = "/symbrowser.properties";

  private Properties properties = new Properties();

  public ConfigurationProvider() {
    try {
      properties.load(getClass().getResourceAsStream(PROP_FILE));
    } catch (IOException e) {
      throw new RuntimeException("failed to load configuration from properties file: " + PROP_FILE,
          e);
    }
  }

  @Override
  public String getRedditClientId() {
    return properties.getProperty("redditClientId");
  }

  @Override
  public String getRedditSecret() {
    return properties.getProperty("redditSecret");
  }

  @Override
  public int getMaxRedditPosts() {
    return Integer.parseInt(properties.getProperty("redditMaxPosts"));
  }

  @Override
  public int getMaxRedditComments() {
    return Integer.parseInt(properties.getProperty("redditMaxComments"));
  }

  @Override
  public int getNumWorkerThreads() {
    return Integer.parseInt(properties.getProperty("numWorkerThreads"));
  }

  @Override
  public long getRequestProcessingTimeout() {
    return Long.parseLong(properties.getProperty("requestProcessingTimeout"));
  }

  @Override
  public long getBotUserId() {
    return Long.parseLong(properties.getProperty("myUserId"));
  }

  @Override
  public File getCertificateFile() {
    String classpathResource = properties.getProperty("certificateResource");
    LOG.info("attempting to load certificate file as classpath resource at " + classpathResource);
    File certificate = new File(getClass().getResource(classpathResource).getFile());
    if (!certificate.exists()) {
      throw new RuntimeException("no certificate found at " + certificate.getAbsolutePath());
    }
    return certificate;
  }

  @Override
  public String getSymphonyKeystorePassword() {
    return properties.getProperty("keystorePassword");
  }

  @Override
  public String getSymphonyKeystoreType() {
    return properties.getProperty("keystoreType");
  }

  @Override
  public String getSymphonyWebControllerUrl() {
    return properties.getProperty("symphonyWebControllerUrl");
  }

  @Override
  public String getSymphonyBaseUrl() {
    return properties.getProperty("symphonyBaseUrl");
  }

  @Override
  public String getSymphonyUserInfoPath() {
    return getSymphonyWebControllerUrl() + properties.getProperty("pathUserInfo");
  }

  @Override
  public String getSymphonyPodPath() {
    return getSymphonyBaseUrl() + properties.getProperty("pathPod");
  }

  @Override
  public String getSymphonyAgentPath() {
    return getSymphonyBaseUrl() + properties.getProperty("pathAgent");
  }

  @Override
  public String getSymphonySbePath() {
    return getSymphonyBaseUrl() + properties.getProperty("pathSessionAuth");
  }

  @Override
  public String getSymphonyKeyManagerPath() {
    return getSymphonyBaseUrl() + properties.getProperty("pathKeyAuth");
  }

  @Override
  public String getDiffbotApiKey() {
    return properties.getProperty("diffbotApiKey");
  }

  @Override
  public int getDiffbotMaxArticles() {
    return Integer.parseInt(properties.getProperty("diffbotMaxArticles"));
  }
}
