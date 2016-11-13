package com.francescoz.fract.engine;

import android.graphics.Bitmap;
import android.opengl.GLES20;

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

    public void addDrawable(int priority, Bitmap bitmap, String key) {
        addDrawable(new Drawable(priority, bitmap, key));
    }

    Drawable[] getDrawables() {
        Drawable[] array = new Drawable[drawableDefs.size()];
        drawableDefs.toArray(array);
        return array;
    }

    public static final class Filter implements FractCoder.Encodable {
        public static final Filter DEFAULT = new Filter(Type.NEAREST, Type.NEAREST);
        public static final FractCoder.Decoder<Filter> DECODER = new FractCoder.Decoder<Filter>() {
            @Override
            public Filter decode(FractCoder.Node node) {
                return new Filter(node.integerData.get("minFilter"), node.integerData.get("magFilter"), node.booleanData.get("generateMipmaps"));
            }
        };
        final int minFilter;
        final int magFilter;
        final boolean generateMipmaps;

        public Filter(Type minFilter, Type mipmapMinFilter, Type magFilter) {
            this.minFilter = resolveFilter(mipmapMinFilter, minFilter);
            this.magFilter = resolveFilter(magFilter);
            generateMipmaps = true;
        }

        private Filter(int minFilter, int magFilter, boolean generateMipmaps) {
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.generateMipmaps = generateMipmaps;
        }

        public Filter(Type minFilter, Type magFilter) {
            this.minFilter = resolveFilter(minFilter);
            this.magFilter = resolveFilter(magFilter);
            generateMipmaps = false;
        }

        private int resolveFilter(Type filterType) {
            switch (filterType) {
                case NEAREST:
                    return GLES20.GL_NEAREST;
                case LINEAR:
                    return GLES20.GL_LINEAR;
                default:
                    throw new RuntimeException("Unknown Type");
            }
        }

        private int resolveFilter(Type mipmapFilterType, Type filterType) {
            switch (mipmapFilterType) {
                case NEAREST:
                    switch (filterType) {
                        case NEAREST:
                            return GLES20.GL_NEAREST_MIPMAP_NEAREST;
                        case LINEAR:
                            return GLES20.GL_NEAREST_MIPMAP_LINEAR;
                        default:
                            throw new RuntimeException("Unknown Type");
                    }
                case LINEAR:
                    switch (filterType) {
                        case NEAREST:
                            return GLES20.GL_LINEAR_MIPMAP_NEAREST;
                        case LINEAR:
                            return GLES20.GL_LINEAR_MIPMAP_LINEAR;
                        default:
                            throw new RuntimeException("Unknown Type");
                    }
                default:
                    throw new RuntimeException("Unknown mipmap Type");
            }
        }

        @Override
        public FractCoder.Node encode() {
            FractCoder.Node n = new FractCoder.Node();
            n.integerData.put("minFilter", minFilter);
            n.integerData.put("magFilter", magFilter);
            n.booleanData.put("generateMipmaps", generateMipmaps);
            return n;
        }

        public enum Type {
            NEAREST, LINEAR
        }

    }

    public static class Drawable {

        public int priority;
        public Bitmap bitmap;
        public String key;

        public Drawable(int priority, Bitmap bitmap, String key) {
            this.priority = priority;
            this.bitmap = bitmap;
            this.key = key;
        }

        public Drawable() {
        }


    }
}
