package com.francescoz.fracttest;

import android.app.Activity;
import android.os.Bundle;

import com.francescoz.fract.engine.FractEngine;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FractEngine engine = new FractEngine.Test(this);
        setContentView(engine.getView());
    }
}
