package com.francescoz.fract.utils;

import android.app.Activity;
import android.os.Bundle;

import com.francescoz.fract.engine.FractEngine;
import com.francescoz.fract.engine.FractResourcesDef;

public abstract class FractFullscreenActivity extends Activity {

    private final FractEngine.Config config;
    private FractEngine engine;

    public FractFullscreenActivity() {
        config = new FractEngine.Config();
    }

    public FractFullscreenActivity(FractEngine.Config config) {
        this.config = config;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engine = new FractEngine(this, config) {
            @Override
            protected FractResourcesDef createResources(FractPixel resolution) {
                return FractFullscreenActivity.this.createResources();
            }
        };
        setContentView(engine.getView());
    }

    public FractEngine getEngine() {
        return engine;
    }

    protected abstract FractResourcesDef createResources();

}
