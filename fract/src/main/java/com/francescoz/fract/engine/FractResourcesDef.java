package com.francescoz.fract.engine;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.francescoz.fract.utils.FractCoder;

import java.util.ArrayList;

public class FractResourcesDef {

    private final ArrayList<Drawable> drawableDefs;
    public Filter filter;

    public FractResourcesDef(Drawable... drawableDefs) {
        this();
        addDrawables(drawableDefs);
    }

    public FractResourcesDef() {
        drawableDefs = new ArrayList<>();
        filter = Filter.DEFAULT;
    }

    public void addDrawable(Drawable drawableDef) {
        if (drawableDef.key == null)
            throw new RuntimeException("Drawable key cannot be null");
        for (Drawable d : drawableDefs)
            if (d.key.equals(drawableDef.key))
                throw new RuntimeException("Drawable key already exists");
        drawableDefs.add(drawableDef);
    }

    public void addDrawables(Drawable... drawableDefs) {
        for (Drawable drawableDef : drawableDefs)
            addDrawable(drawableDef);
    }

    Drawable[] getDrawables() {
        Drawable[] array = new Drawable[drawableDefs.size()];
        drawableDefs.toArray(array);
        return array;
    }

    public static final class Filter implements FractCoder.Encodable {
        public static final Filter DEFAULT = new Filter(false, false);
        public static final FractCoder.Decoder<Filter> DECODER = new FractCoder.Decoder<Filter>() {
            @Override
            public Filter decode(FractCoder.Node node) {
                return new Filter(node.integerData.get("minFilter"), node.integerData.get("magFilter"), node.booleanData.get("generateMipmaps"));
            }
        };
        final int minFilter;
        final int magFilter;
        final boolean generateMipmaps;

        public Filter(boolean minInterpolation, boolean magInterpolation, boolean mipmapInterpolation) {
            this.minFilter = resolveFilter(minInterpolation, mipmapInterpolation);
            this.magFilter = resolveFilter(magInterpolation);
            generateMipmaps = true;
        }

        private Filter(int minFilter, int magFilter, boolean generateMipmaps) {
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.generateMipmaps = generateMipmaps;
        }

        public Filter(boolean minInterpolation, boolean magInterpolation) {
            this.minFilter = resolveFilter(minInterpolation);
            this.magFilter = resolveFilter(magInterpolation);
            generateMipmaps = false;
        }

        private int resolveFilter(boolean interpolation) {
            return interpolation ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
        }

        private int resolveFilter(boolean interpolation, boolean mipmapInterpolation) {
            if (mipmapInterpolation)
                return interpolation ? GLES20.GL_LINEAR_MIPMAP_LINEAR : GLES20.GL_LINEAR_MIPMAP_NEAREST;
            return interpolation ? GLES20.GL_NEAREST_MIPMAP_LINEAR : GLES20.GL_NEAREST_MIPMAP_NEAREST;
        }

        @Override
        public FractCoder.Node encode() {
            FractCoder.Node n = new FractCoder.Node();
            n.integerData.put("minFilter", minFilter);
            n.integerData.put("magFilter", magFilter);
            n.booleanData.put("generateMipmaps", generateMipmaps);
            return n;
        }

    }

    public static class FontDrawable extends Drawable {

        private final StaticLayout layout;
        private final int width;

        public FontDrawable(String text, TextPaint paint) {
            layout = new StaticLayout(text, paint, Integer.MAX_VALUE, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            width = (int) Math.ceil(paint.measureText(text));
        }

        public FontDrawable(int priority, String key, String text, int size, Typeface typeface) {
            this(text, size, typeface);
            super.priority = priority;
            super.key = key;
        }

        public FontDrawable(String text, int size, Typeface typeface) {
            this(text, createPaint(size, typeface));
        }

        private static TextPaint createPaint(int size, Typeface typeface) {
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setStrokeWidth(0);
            textPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
            textPaint.setTextSize(size);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTypeface(typeface);
            textPaint.setStrikeThruText(false);
            return textPaint;
        }

        public static FontDrawable[] create(String[] texts, int size, Typeface typeface) {
            TextPaint textPaint = createPaint(size, typeface);
            int len = texts.length;
            FontDrawable[] drawables = new FontDrawable[len];
            for (int i = 0; i < len; i++)
                drawables[i] = new FontDrawable(texts[i], textPaint);
            return drawables;
        }

        @Override
        public void draw(Canvas canvas) {
            layout.draw(canvas);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return layout.getHeight();
        }
    }

    public static class CircleDrawable extends PaintDrawable {

        public CircleDrawable(int priority, String key, int radius) {
            super(priority, key, radius, radius);
        }

        public CircleDrawable(int radius) {
            super(radius, radius);
        }

        @Override
        public void draw(Canvas canvas) {
            float c = width / 2.0f;
            canvas.drawCircle(c, c, c, paint);
        }
    }

    public static abstract class PaintDrawable extends Drawable {
        protected final Paint paint;
        protected final int width, height;

        public PaintDrawable(int priority, String key, int width, int height) {
            super(priority, key);
            this.width = width;
            this.height = height;
            this.paint = createPaint();
        }

        public PaintDrawable(int width, int height) {
            this.width = width;
            this.height = height;
            this.paint = createPaint();
        }

        private static Paint createPaint() {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            return paint;
        }

        @Override
        public final int getWidth() {
            return width;
        }

        @Override
        public final int getHeight() {
            return height;
        }
    }

    public static class RectDrawable extends Drawable {
        public RectDrawable(int priority, String key) {
            super(priority, key);
        }

        public RectDrawable() {
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);
        }

        @Override
        public int getWidth() {
            return 2;
        }

        @Override
        public int getHeight() {
            return 2;
        }
    }

    public static class BitmapDrawable extends Drawable {

        private static final BitmapFactory.Options DEFAULT_OPTIONS;

        static {
            DEFAULT_OPTIONS = new BitmapFactory.Options();
            DEFAULT_OPTIONS.inScaled = true;
        }

        private final Bitmap bitmap;

        public BitmapDrawable(int priority, String key, Resources resources, int drawableResId) {
            this(resources, drawableResId);
            super.priority = priority;
            super.key = key;
        }

        public BitmapDrawable(Resources resources, int drawableResId) {
            this(resources, drawableResId, DEFAULT_OPTIONS);
        }

        public BitmapDrawable(int priority, String key, Resources resources, int drawableResId, BitmapFactory.Options options) {
            this(resources, drawableResId, options);
            super.priority = priority;
            super.key = key;
        }

        public BitmapDrawable(Resources resources, int drawableResId, BitmapFactory.Options options) {
            this(BitmapFactory.decodeResource(resources, drawableResId, options));
        }

        public BitmapDrawable(int priority, String key, Bitmap bitmap) {
            super(priority, key);
            this.bitmap = bitmap;
        }

        public BitmapDrawable(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

        @Override
        public int getWidth() {
            return bitmap.getWidth();
        }

        @Override
        public int getHeight() {
            return bitmap.getHeight();
        }
    }

    public static abstract class Drawable {

        public int priority;
        public String key;

        public Drawable(int priority, String key) {
            this.priority = priority;
            this.key = key;
        }

        public Drawable() {
        }

        public abstract void draw(Canvas canvas);

        public abstract int getWidth();

        public abstract int getHeight();

    }
}
