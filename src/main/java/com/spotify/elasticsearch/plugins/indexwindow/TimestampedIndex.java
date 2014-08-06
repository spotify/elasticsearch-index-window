package com.spotify.elasticsearch.plugins.indexwindow;

import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class TimestampedIndex implements Comparable<TimestampedIndex> {
    @Getter
    private final String fullIndexName;

    @Getter
    private final String indexPrefix;

    @Getter
    private final Date timestamp;

    public TimestampedIndex(String fullIndexName, String indexPrefix,
            Date timestamp) {
        super();
        this.fullIndexName = fullIndexName;
        this.indexPrefix = indexPrefix;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(TimestampedIndex o) {
        final int prefixComparison = indexPrefix.compareTo(o.indexPrefix);
        if (prefixComparison != 0) {
            return prefixComparison;
        }
        return timestamp.compareTo(o.timestamp);
    }
}
