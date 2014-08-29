/*
 * Copyright (c) 2012-2014 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.spotify.elasticsearch.plugins.indexwindow;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

public class IndexWindowPlugin extends AbstractPlugin {

  @Override
  public String name() {
    return "index-window";
  }

  @Override
  public String description() {
    return "Helps defining a fixed time-based window of indices. It assumes that time is encoded as postfix in name "
           + "of the index. It continuesly removes the indices that are out of the time window.";
  }

  @Override
  public void processModule(Module module) {
    if (module instanceof RestModule) {
      ((RestModule) module).addRestAction(IndexWindowAction.class);
    }
  }
}
