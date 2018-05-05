package com.francescoz.fract.engine;

import android.graphics.Color;

import com.francescoz.fract.utils.FractCoder;
import com.francescoz.fract.utils.FractColor;
import com.francescoz.fract.utils.FractInput;
import com.francescoz.fract.utils.FractMatrix;
import com.francescoz.fract.utils.FractOrigin;
import com.francescoz.fract.utils.FractSizing;

public abstract class FractScreen {

    public final Viewport viewport;
    public FractColor clearColor;
    public FractInput.Listener inputListener;

    public FractScreen(FractColor clearColor, Viewport viewport, FractInput.Listener inputListener) {
        this.clearColor = clearColor;
        this.viewport = viewport;
        this.inputListener = inputListener;
    }

    public FractScreen(FractInput.Listener inputListener) {
        this(new FractColor.RGB(), new Viewport(), inputListener);
    }

    public FractScreen(Viewport viewport, FractInput.Listener inputListener) {
        this(new FractColor.RGB(), viewport, inputListener);
    }

    public FractScreen(FractColor.RGB clearColor, FractInput.Listener inputListener) {
        this(clearColor, new Viewport(), inputListener);
    }

    public FractScreen(FractColor.RGB clearColor, Viewport viewport) {
        this(clearColor, viewport, FractInput.Listener.NULL_LISTENER);
    }

    public FractScreen(Viewport viewport) {
        this(new FractColor.RGB(), viewport);
    }

    public FractScreen(FractColor.RGB clearColor) {
        this(clearColor, new Viewport());
    }

    public FractScreen() {
        this(new FractColor.RGB(), new Viewport());
    }


    protected abstract void render(FractEngine.Drawer drawer, float deltaTime);

    protected abstract void hide();

    protected abstract void show();

    protected void size() {
    }

    final void resize(int width, int height) {
        viewport.size(width, height);
        size();
    }

    final void set(int width, int height) {
        viewport.size(width, height);
        show();
    }

    static final class DefaultScreen extends FractScreen {

        DefaultScreen() {
            clearColor.set(Color.BLUE);
        }

        @Override
        protected void render(FractEngine.Drawer drawer, float deltaTime) {
        }

        @Override
        protected void hide() {

        }

        @Override
        protected void show() {

        }

    }

    public static final class Viewport implements FractCoder.Codable {

        public static final FractCoder.Decoder<Viewport> DECODER = new FractCoder.Decoder<Viewport>() {
            @Override
            public Viewport decode(FractCoder.Node node) {
                Viewport viewport = new Viewport();
                viewport.decode(node);
                return viewport;
            }
        };
        public FractSizing sizing;
        public FractOrigin horizontalOrigin;
        public FractOrigin verticalOrigin;
        public float fixedSize;
        private float aspect;

        public Viewport() {
            sizing = FractSizing.FIXED_W;
            horizontalOrigin = FractOrigin.LEFT_BOTTOM;
            verticalOrigin = FractOrigin.LEFT_BOTTOM;
            fixedSize = 1;
        }

        public void set(Viewport viewport) {
            this.sizing = viewport.sizing;
            this.fixedSize = viewport.fixedSize;
            this.horizontalOrigin = viewport.horizontalOrigin;
            this.verticalOrigin = viewport.verticalOrigin;
        }

        public float getBottomY() {
            return getHeight() * (verticalOrigin.alpha - 0.5f);
        }

        public float getTopY() {
            return getHeight() * (verticalOrigin.alpha + 0.5f);
        }

        public float getRightX() {
            return getWidth() * (horizontalOrigin.alpha + 0.5f);
        }

        public float getLeftX() {
            return getWidth() * (horizontalOrigin.alpha - 0.5f);
        }

        public float getWidth() {
            return sizing == FractSizing.FIXED_H ? fixedSize / aspect : fixedSize;
        }

        public float getHeight() {
            return sizing == FractSizing.FIXED_W ? fixedSize * aspect : fixedSize;
        }

        private void size(int width, int height) {
            aspect = height / (float) width;
        }

        void concat(FractMatrix matrix) {
            float s = 2.0f / fixedSize;
            float tx = horizontalOrigin.alpha * -2.0f;
            float ty = verticalOrigin.alpha * -2.0f;
            switch (sizing) {
                case FIXED_WH:
                    matrix.concat(tx, ty, s, s);
                    break;
                case FIXED_H:
                    matrix.concat(tx, ty, s * aspect, s);
                    break;
                case FIXED_W:
                    matrix.concat(tx, ty, s, s / aspect);
                    break;
                default:
                    throw new RuntimeException("Unknown FractSizing");
            }
        }

        @Override
        public void decode(FractCoder.Node node) {
            sizing = node.getEncodableIf("sizing", FractSizing.DECODER);
            verticalOrigin = node.getEncodableIf("sizing", FractOrigin.DECODER);
            horizontalOrigin = node.getEncodableIf("sizing", FractOrigin.DECODER);
            fixedSize = node.floatData.get("fixedSize", 0.0f);
        }

        @Override
        public FractCoder.Node encode() {
            FractCoder.Node n = new FractCoder.Node();
            n.putEncodableIf("sizing", sizing);
            n.putEncodableIf("verticalOrigin", verticalOrigin);
            n.putEncodableIf("horizontalOrigin", horizontalOrigin);
            n.floatData.put("fixedSize", fixedSize);
            return n;
        }
    }

}
