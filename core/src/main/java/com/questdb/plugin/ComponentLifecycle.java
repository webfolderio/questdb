package com.questdb.plugin;

import com.questdb.cairo.CairoEngine;

import java.io.Closeable;

public interface ComponentLifecycle extends Closeable {
    void init(CairoEngine cairoEngine);
}
