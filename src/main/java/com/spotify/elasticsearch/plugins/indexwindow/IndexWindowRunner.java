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

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is class is responsible for making sure that the window applies. As soon as an object of this class is
 * instantiated, the continuous check starts.
 *
 * @author mehrdad
 */
public class IndexWindowRunner implements Runnable {

  private final IndexWindow window;
  private final ScheduledExecutorService executor;
  private final Client client;
  private final ESLogger log = Loggers.getLogger(IndexWindowRunner.class);

  /**
   * @param client The elasticsearch client through which this class communicates with the cluster
   * @param window The window to be kept by this class
   */
  public IndexWindowRunner(Client client, IndexWindow window) {
    this.window = window;
    this.client = client;
    this.executor = initializeExecutor();
    log.info("Starting to run index window for " + window);
  }

  /**
   * Initialize and run the executor that keeps the window
   *
   * @return the initialized executor
   */
  private ScheduledExecutorService initializeExecutor() {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    executor.scheduleAtFixedRate(this, 10, window.getCheckInterval(), TimeUnit.MILLISECONDS);
    return executor;
  }

  @Override
  public void run() {
    log.info("I am going to do some clean up for index window: " + window);
    IndicesStatusResponse indicesStatusResponse = client.admin().indices().prepareStatus().execute().actionGet();
    Set<String> allIndices = indicesStatusResponse.getIndices().keySet();
    Set<String> toBeRemoved = getToBeRemovedIndices(allIndices);

    if (toBeRemoved.isEmpty()) {
      log.info("All good... no index to be removed.");
      return;
    }

    for (String index : toBeRemoved) {
      log.info("This index is going to be removed: " + index);
      try {
        DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
        if (deleteResponse.isAcknowledged()) {
          log.info("Delete successful for: " + index);
        } else {
          log.info("Delete not successful for: " + index);
        }
      } catch (Exception e) {
        log.error("Something went wrong while deleting index " + index, e);
      }
    }
  }

  private Set<String> getToBeRemovedIndices(Set<String> allIndices) {
    Set<String> toBeRemoved = new HashSet<String>();
    TreeSet<TimestampedIndex> relevantIndices = getRelevantIndices(allIndices);

    if (relevantIndices.size() <= window.getKeep()) {
      // We keep it all. Nothing is going to be removed.
      return Collections.emptySet();
    }

    TimestampedIndex[] relevantIndicesArray = relevantIndices.toArray(new TimestampedIndex[relevantIndices.size()]);
    for (int i = 0; i < relevantIndicesArray.length - window.getKeep(); i++) {
      toBeRemoved.add(relevantIndicesArray[i].getFullIndexName());
    }

    return toBeRemoved;
  }

  /**
   * Filters the given indices and returns only the indices that match the window, i.e., indices that have the same
   * prefix as the window and their postfix timestamp matches the date format of the window.
   *
   * @return A sorted set of indices that match the window. The set is sorted chronologically. index 0 is the oldest and
   * the last element is the most recent.
   */
  private TreeSet<TimestampedIndex> getRelevantIndices(Set<String> allIndices) {
    TreeSet<TimestampedIndex> result = new TreeSet<TimestampedIndex>();
    String indexPrefix = window.getIndexPrefix();
    SimpleDateFormat dateFormat = new SimpleDateFormat(window.getDateFormat());

    for (String index : allIndices) {
      if (!index.startsWith(indexPrefix)) {
        continue;
      }

      String indexDateStr = index.substring(indexPrefix.length(), index.length());
      try {
        final Date indexDate = dateFormat.parse(indexDateStr);
        result.add(new TimestampedIndex(index, indexPrefix, indexDate));
      } catch (ParseException e) {
        // The timestamp does not match the date format, so we move on
      }
    }

    return result;
  }

  /**
   * This method must be called when this runner is not going to be used anymore. Otherwise the window will be checked
   * for ever and that might not be desirable. For example in case of configuration change, the old window should be
   * disposed and the corresponding runner should be canceled.
   */
  public void close() {
    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("IndexWindow executor termination got interrupted.", e);
    }
  }
}
