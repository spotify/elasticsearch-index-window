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
