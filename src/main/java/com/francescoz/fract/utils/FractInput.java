package com.francescoz.fract.utils;

import java.util.ArrayList;

public final class FractInput {

    public static final int MAX_FINGERS = 10;

    private FractInput() {
    }

    public interface Listener {

        Listener NULL_LISTENER = new Listener() {
            @Override
            public void onTouchDown(int pointer, float x, float y) {

            }

            @Override
            public void onTouchMoved(int pointer, float x, float y) {

            }

            @Override
            public void onTouchUp(int pointer, float x, float y) {

            }

            @Override
            public void onTouchCancelled() {

            }
        };

        void onTouchDown(int pointer, float x, float y);

        void onTouchMoved(int pointer, float x, float y);

        void onTouchUp(int pointer, float x, float y);

        void onTouchCancelled();
    }

    public static final class Finger {
        private final FractVec position;
        private final FractVec pressPosition;
        private final FractVec releasePosition;
        private final int id;
        private long pressTime;
        private long releaseTime;
        private boolean pressed;
        private boolean wasPressed;

        public Finger(int id) {
            position = new FractVec();
            pressPosition = new FractVec();
            releasePosition = new FractVec();
            this.id = id;
        }

        void proceed() {
            wasPressed = pressed;
        }

        void press(float x, float y) {
            pressTime = System.nanoTime();
            pressed = true;
            pressPosition.set(x, y);
            position.set(x, y);
        }

        void release(float x, float y) {
            releaseTime = System.nanoTime();
            pressed = false;
            releasePosition.set(x, y);
            position.set(x, y);
        }

        void cancel() {
            pressed = wasPressed = false;
        }

        void setPosition(float x, float y) {
            position.set(x, y);
        }

        public int getIndex() {
            return id;
        }

        public long getPressNanoTime() {
            return pressTime;
        }

        public long getReleaseNanoTime() {
            return releaseTime;
        }

        public float getPressElapsedTime() {
            return (System.nanoTime() - pressTime) * FractMath.NANO_TO_SECONDS;
        }

        public float getReleaseElapsedTime() {
            return (System.nanoTime() - releaseTime) * FractMath.NANO_TO_SECONDS;
        }

        public float getX() {
            return position.x;
        }

        public float getY() {
            return position.y;
        }

        public float getReleaseX() {
            return releasePosition.x;
        }

        public float getPressX() {
            return pressPosition.x;
        }

        public float getReleaseY() {
            return releasePosition.y;
        }

        public float getPressY() {
            return pressPosition.y;
        }

        public boolean isPressed() {
            return pressed;
        }

        public boolean isJustPressed() {
            return pressed && !wasPressed;
        }

        public boolean isJustReleased() {
            return wasPressed && !pressed;
        }

        public void getReleasePosition(FractVec out) {
            out.set(releasePosition);
        }

        public void getPressPosition(FractVec out) {
            out.set(pressPosition);
        }

        public void getPosition(FractVec out) {
            out.set(position);
        }
    }

    public static class FingerDispatcher extends Dispatcher {
        private final Finger[] fingers;

        public FingerDispatcher() {
            fingers = new Finger[MAX_FINGERS];
        }

        public FingerDispatcher(Listener... listeners) {
            super(listeners);
            fingers = new Finger[MAX_FINGERS];
        }

        public Finger get(int index) {
            return fingers[index];
        }

        public Finger getFirst() {
            return fingers[0];
        }

        public Finger getLastPressed() {
            for (int i = MAX_FINGERS - 1; i >= 0; i--)
                if (fingers[i].isPressed()) return fingers[i];
            return null;
        }

        public int getTouchCount() {
            int c = 0;
            for (Finger f : fingers)
                if (f.isPressed()) c++;
            return c;
        }

        @Override
        public void onTouchDown(int pointer, float x, float y) {
            fingers[pointer].press(x, y);
            super.onTouchDown(pointer, x, y);
        }

        @Override
        public void onTouchMoved(int pointer, float x, float y) {
            fingers[pointer].setPosition(x, y);
            super.onTouchMoved(pointer, x, y);
        }

        @Override
        public void onTouchUp(int pointer, float x, float y) {
            fingers[pointer].release(x, y);
            super.onTouchUp(pointer, x, y);
        }

        @Override
        public void onTouchCancelled() {
            for (Finger f : fingers)
                f.cancel();
            super.onTouchCancelled();
        }
    }

    public static class Dispatcher implements Listener {

        protected final ArrayList<Listener> listeners;

        public Dispatcher() {
            listeners = new ArrayList<>();
        }

        public Dispatcher(Listener... listeners) {
            this();
            add(listeners);
        }

        public final void add(Listener... listeners) {
            for (Listener listener : listeners)
                add(listener);
        }

        public final void add(Listener listener) {
            if (listeners.contains(listener))
                throw new RuntimeException("Already added listener");
            listeners.add(listener);
        }

        public final void remove(Listener... listeners) {
            for (Listener listener : listeners)
                remove(listener);
        }

        public final void remove(Listener listener) {
            if (!listeners.remove(listener))
                throw new RuntimeException("Never added listener");
        }

        @Override
        public void onTouchDown(int pointer, float x, float y) {
            for (Listener listener : listeners)
                listener.onTouchDown(pointer, x, y);
        }

        @Override
        public void onTouchMoved(int pointer, float x, float y) {
            for (Listener listener : listeners)
                listener.onTouchMoved(pointer, x, y);
        }

        @Override
        public void onTouchUp(int pointer, float x, float y) {
            for (Listener listener : listeners)
                listener.onTouchUp(pointer, x, y);
        }

        @Override
        public void onTouchCancelled() {
            for (Listener listener : listeners)
                listener.onTouchCancelled();
        }
    }
}
