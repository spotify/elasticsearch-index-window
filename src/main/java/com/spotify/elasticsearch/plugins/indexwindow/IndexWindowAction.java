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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestXContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class IndexWindowAction extends BaseRestHandler {

  private static final String PARAM_DELETE_INDEX = "delete_index";
  private static final String PARAM_CHECK_INTERVAL = "check_interval";
  private static final String PARAM_KEEP = "keep";
  private static final String PARAM_DATE_FORMAT = "date_format";
  private static final String PARAM_INDEX_PREFIX = "index_prefix";

  private static final TimeValue DEFAULT_PARAM_CHECK_INTERVAL = new TimeValue(30, TimeUnit.MINUTES);
  private static final int DEFAULT_PARAM_KEEP = 7;
  private static final String DEFAULT_PARAM_DATE_FORMAT = "yyyy.MM.dd";

  private static final long LOAD_RETRY_INTERVAL = 20000;
  private static final long LOAD_TIMEOUT = 200000;
  private static final String META_TYPE = "window";
  private static final String META_INDEX = "index-window";
  private Map<String, IndexWindowRunner> activeWindows;

  private final ObjectMapper mapper;

  @Inject
  public IndexWindowAction(Settings settings, Client client, RestController controller) {
    super(settings, client);
    controller.registerHandler(PUT, "/" + META_INDEX, this);
    controller.registerHandler(POST, "/" + META_INDEX, this);
    controller.registerHandler(DELETE, "/" + META_INDEX + "/{" + PARAM_DELETE_INDEX + "}", this);

    mapper = new ObjectMapper();
    loadActiveWindows();
  }

  @Override
  public void handleRequest(RestRequest request, RestChannel channel) {
    switch (request.method()) {
      case DELETE:
        handleDeleteRequest(request, channel);
        break;
      default:
        handleCreateRequest(request, channel);
        break;
    }
  }

  /**
   * This method is called when an index window is going to be created, or updated.
   */
  private void handleCreateRequest(RestRequest request, RestChannel channel) {
    try {
      String indexPrefix = request.param(PARAM_INDEX_PREFIX, "");
      if (indexPrefix.isEmpty()) {
        respondBadRequest(request, channel, PARAM_INDEX_PREFIX + " missing");
        return;
      }
      String format = request.param(PARAM_DATE_FORMAT, DEFAULT_PARAM_DATE_FORMAT);
      try {
        // Try to parse the provided date format
        new SimpleDateFormat(format);

      } catch (Exception e) {
        respondBadRequest(request, channel, "invalid " + PARAM_DATE_FORMAT);
        return;
      }

      Integer keep = request.paramAsInt(PARAM_KEEP, DEFAULT_PARAM_KEEP);
      if (keep < 0) {
        respondBadRequest(request, channel, PARAM_KEEP + " cannot be negative");
        return;
      }

      TimeValue checkInterval = null;
      try {
        checkInterval = request.paramAsTime(PARAM_CHECK_INTERVAL, DEFAULT_PARAM_CHECK_INTERVAL);
      } catch (Exception e) {
        respondBadRequest(request, channel, "invalid " + PARAM_CHECK_INTERVAL);
        return;
      }
      IndexWindow window = new IndexWindow(indexPrefix, format, keep, checkInterval.millis());
      addOrReplaceWindow(window);

      XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
      builder.startObject();
      builder.field("acknowledge", true);
      builder.field("source", window);
      builder.endObject();
      channel.sendResponse(new XContentRestResponse(request, RestStatus.OK, builder));
    } catch (IOException e) {
      try {
        channel.sendResponse(new XContentThrowableRestResponse(request, e));
      } catch (Exception ex) {
        logger.error("Unknown problem occurred", ex);
      }
    }
  }

  private void handleDeleteRequest(RestRequest request, RestChannel channel) {
    if (request.hasParam(PARAM_DELETE_INDEX)) {
      String deleteIndex = request.param(PARAM_DELETE_INDEX);
      boolean found = removeWindow(deleteIndex);
      if (found) {
        logger.info("index window removed: " + deleteIndex);
      }

      try {
        XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
        builder.startObject();
        builder.field("acknowledge", true);
        builder.field("found", found);
        builder.field("deleted_index", deleteIndex);
        builder.endObject();
        channel.sendResponse(new XContentRestResponse(request, RestStatus.OK, builder));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        respondBadRequest(request, channel, "invalid request");
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void respondBadRequest(RestRequest request, RestChannel channel, String message) throws IOException {
    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
    builder.startObject();
    builder.field("error", message);
    builder.endObject();
    channel.sendResponse(new XContentRestResponse(request, RestStatus.BAD_REQUEST, builder));
  }

  /**
   * Loads index windows configurations from elasticsearch and starts their runners.
   */
  private void loadActiveWindows() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        boolean loadSuccess = false;
        long loadStart = System.currentTimeMillis();
        while (!loadSuccess && (System.currentTimeMillis() - loadStart) <= LOAD_TIMEOUT) {
          try {
            Thread.sleep(LOAD_RETRY_INTERVAL);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          logger.info("Trying to load active index windows...");
          loadSuccess = doLoadActiveWindows();
          if (!loadSuccess) {
            logger.info("Failed to load active index windows... maybe the shards are not loaded yet...");
          } else {
            logger.info("Loading index windows successful!");
          }
        }
        if (!loadSuccess) {
          logger.warn("Giving up on loading active index windows... Either there is not window to load, "
                      + "or the cluster is taking too long time to start up. Timeout: " + LOAD_TIMEOUT);
        }
      }
    }).start();
  }

  private boolean doLoadActiveWindows() {
    clearActiveWindows();
    activeWindows = new HashMap<String, IndexWindowRunner>();
    SearchHits hits;

    try {
      hits = client.prepareSearch(META_INDEX).execute().actionGet().getHits();
    } catch (Exception e) {
      return false;
    }

    for (SearchHit hit : hits) {
      String json = hit.getSourceAsString();
      try {
        IndexWindow window = mapper.readValue(json, IndexWindow.class);
        activeWindows.put(window.getIndexPrefix(), new IndexWindowRunner(client, window));
      } catch (JsonParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (JsonMappingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return true;
  }

  private void clearActiveWindows() {
    if (activeWindows == null) {
      return;
    }

    Collection<IndexWindowRunner> windows = activeWindows.values();
    for (IndexWindowRunner w : windows) {
      w.close();
    }

    activeWindows.clear();
  }

  private void addOrReplaceWindow(IndexWindow indexWindow) {
    removeWindow(indexWindow.getIndexPrefix());
    writeToElasticsearch(indexWindow);
    activeWindows.put(indexWindow.getIndexPrefix(), new IndexWindowRunner(client, indexWindow));
  }

  private void writeToElasticsearch(IndexWindow indexWindow) {
    try {
      String json = mapper.writeValueAsString(indexWindow);
      client.prepareIndex(META_INDEX, META_TYPE, indexWindow.getIndexPrefix()).setSource(json).execute().actionGet();
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private boolean removeWindow(String indexPrefix) {
    boolean found = false;

    client.prepareDelete(META_INDEX, META_TYPE, indexPrefix).execute().actionGet();
    IndexWindowRunner removedWindow = activeWindows.remove(indexPrefix);
    if (removedWindow != null) {
      removedWindow.close();
      found = true;
    }

    return found;
  }
}
