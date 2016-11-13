package com.francescoz.fract.utils;

public final class FractMath {

    public static final double TO_LOG2 = 1.0 / Math.log(2);
    public static final float TO_DEGREES = 180.0f / (float) Math.PI;
    public static final float TO_RADIANS = (float) Math.PI / 180.0f;
    public static final float NANO_TO_SECONDS = 1 / 1000000000.0f;

    private FractMath() {
    }

    public static float clamp(float value, float min, float max) {
        if (value <= min)
            return min;
        if (value >= max)
            return max;
        return value;
    }

    public static float cosDegress(float angle) {
        return (float) Math.cos(angle * TO_RADIANS);
    }

    public static float sinDegress(float angle) {
        return (float) Math.sin(angle * TO_RADIANS);
    }

    public static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    public static float dst2(float x1, float y1, float x2, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return x * x + y * y;
    }

    public static float dst(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(dst2(x1, y1, x2, y2));
    }

    public static float dst2(float x1, float y1, FractVec vec2) {
        return dst2(x1, y1, vec2.x, vec2.y);
    }

    public static float dst(float x1, float y1, FractVec vec2) {
        return dst(x1, y1, vec2.x, vec2.y);
    }

    public static float dst2(FractVec vec1, FractVec vec2) {
        return dst2(vec1.x, vec1.y, vec2.x, vec2.y);
    }

    public static float dst(FractVec vec1, FractVec vec2) {
        return dst(vec1.x, vec1.y, vec2.x, vec2.y);
    }

    public static float angleRad(float x1, float y1, float x2, float y2) {
        return (float) Math.atan((y2 - y1) / (x2 - x1));
    }

    public static float angleRad(FractVec vec1, FractVec vec2) {
        return angleRad(vec1.x, vec1.y, vec2.x, vec2.y);
    }
}
