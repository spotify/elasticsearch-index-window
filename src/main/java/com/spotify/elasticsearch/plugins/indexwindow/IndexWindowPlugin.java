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
        return "Helps defining a fixed time-based window of indices. It assumes that time is encoded as postfix in name of the index. It continuesly removes the indices that are out of the time window.";
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof RestModule)
            ((RestModule) module).addRestAction(IndexWindowAction.class);
    }
}
