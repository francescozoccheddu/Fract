package com.francescoz.fract.utils;

public class FractRange {
    public float min, max;


    public float getAvg() {
        return (max - min) / 2.0f;
    }

    public void limit(float value) {
        if (value < min)
            min = value;
        if (value > max)
            max = value;
    }

    public void set(FractRange range) {
        min = range.min;
        max = range.max;
    }

    public void set(float min, float max) {

    }
}
