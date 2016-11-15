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
        for (Drawable d : drawableDefs)
            if (d.key.equals(drawableDef.key))
                throw new RuntimeException("Key already exists");
        drawableDefs.add(drawableDef);
    }

    public void addDrawables(Drawable... drawableDefs) {
        for (Drawable drawableDef : drawableDefs)
            addDrawable(drawableDef);
    }

    public void addDrawable(int priority, String key, Resources resources, int drawableResID, BitmapFactory.Options options) {
        addDrawable(priority, key, BitmapFactory.decodeResource(resources, drawableResID, options));
    }

    public void addDrawable(int priority, String key, Bitmap bitmap) {
        addDrawable(new Drawable(priority, key, bitmap));
    }

    Drawable[] getDrawables() {
        Drawable[] array = new Drawable[drawableDefs.size()];
        drawableDefs.toArray(array);
        return array;
    }

    public void addFont(int priority, FontDrawableKeyChooser keyChooser, String chars, int size, Typeface typeface) {
        addFont(priority, keyChooser, chars.toCharArray(), size, typeface);
    }

    public void addFont(int priority, FontDrawableKeyChooser keyChooser, char[] chars, int size, Typeface typeface) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrikeThruText(false);
        paint.setStrokeWidth(0);
        paint.setTypeface(typeface);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        for (char c : chars) {
            String text = String.valueOf(c);
            StaticLayout l = new StaticLayout(text, paint, size, Layout.Alignment.ALIGN_NORMAL, 0, 0, false);
            String key = keyChooser.choose(c);
            Bitmap bitmap = Bitmap.createBitmap(l.getWidth(), l.getHeight(), Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(bitmap);
            l.draw(canvas);
            addDrawable(priority, key, bitmap);
        }
    }

    public interface FontDrawableKeyChooser {
        FontDrawableKeyChooser DEFAULT = new FontDrawableKeyChooser() {
            @Override
            public String choose(char c) {
                return String.valueOf(c);
            }
        };

        String choose(char c);

        class Prefix implements FontDrawableKeyChooser {
            public final String prefix;

            public Prefix(String prefix) {
                this.prefix = prefix;
            }

            @Override
            public String choose(char c) {
                return prefix + c;
            }
        }
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

    public static class Drawable {

        public int priority;
        public Bitmap bitmap;
        public String key;

        public Drawable(int priority, String key, Bitmap bitmap) {
            this.priority = priority;
            this.bitmap = bitmap;
            this.key = key;
        }

        public Drawable() {
        }

    }
}
