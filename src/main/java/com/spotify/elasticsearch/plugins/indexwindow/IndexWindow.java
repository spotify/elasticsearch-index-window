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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class IndexWindow {

    @Getter
    @Setter
    private String indexPrefix;

    @Getter
    @Setter
    private String dateFormat;

    @Getter
    @Setter
    private int keep;

    @Getter
    @Setter
    private long checkInterval;

    public IndexWindow() {
    }

    /**
     * @param indexPrefix
     *            The prefix of indices to be windowed
     * @param dateFormat
     *            The date format of the postfix timestamp
     * @param keep
     *            Number of recent indices to be kept, i.e., size of the window
     * @param checkInterval
     *            How often to check for deprecated (out-of-window) indices. The
     *            unit is milliseconds.
     */
    public IndexWindow(String indexPrefix, String dateFormat, int keep,
            long checkInterval) {
        super();
        this.indexPrefix = indexPrefix;
        this.dateFormat = dateFormat;
        this.keep = keep;
        this.checkInterval = checkInterval;
    }

    public static void main(String[] args) {
        System.out.println(new Date(1405432065579l));
    }
}
