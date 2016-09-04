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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ryan.dsouza on 7/28/16.
 *
 * Represents the skeleton of a Context which is extended/implemented by unique services
 */

public abstract class ServiceContext {

  protected final IConfigurationProvider configurationProvider;
  protected final List<String> possibleCommands;

  public ServiceContext(IConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
    this.possibleCommands = new ArrayList<String>();
  }

  public abstract void authenticate();

  public abstract String getContextName();

  public abstract List<MessageML> responsesToAction(String action);

  public List<String> getPossibleCommands() {
    return possibleCommands;
  }

  @Override
  public boolean equals(Object otherContext) {
    if (!(otherContext instanceof ServiceContext)) {
      return false;
    }

    ServiceContext serviceContext = (ServiceContext) otherContext;
    return this.getContextName().equals(serviceContext.getContextName());
  }
}