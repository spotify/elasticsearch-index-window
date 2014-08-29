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

import java.util.Date;

public class TimestampedIndex implements Comparable<TimestampedIndex> {

  private final String fullIndexName;
  private final String indexPrefix;
  private final Date timestamp;

  public TimestampedIndex(String fullIndexName, String indexPrefix, Date timestamp) {
    super();
    this.fullIndexName = fullIndexName;
    this.indexPrefix = indexPrefix;
    this.timestamp = timestamp;
  }

  public int compareTo(TimestampedIndex o) {
    final int prefixComparison = indexPrefix.compareTo(o.indexPrefix);
    if (prefixComparison != 0) {
      return prefixComparison;
    }
    return timestamp.compareTo(o.timestamp);
  }

  public String getFullIndexName() {
    return fullIndexName;
  }
}
