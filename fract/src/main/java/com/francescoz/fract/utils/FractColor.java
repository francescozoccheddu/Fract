package com.francescoz.fract.utils;


import android.graphics.Color;

public abstract class FractColor implements FractCoder.Codable {

    public static final int BLACK = Color.BLACK;
    public static final int DARK_GRAY = Color.DKGRAY;
    public static final int GRAY = Color.GRAY;
    public static final int LIGHT_GRAY = Color.LTGRAY;
    public static final int WHITE = Color.WHITE;
    public static final int RED = Color.RED;
    public static final int GREEN = Color.GREEN;
    public static final int BLUE = Color.BLUE;
    public static final int YELLOW = Color.YELLOW;
    public static final int CYAN = Color.CYAN;
    public static final int MAGENTA = Color.MAGENTA;
    public static final int TRANSPARENT = Color.TRANSPARENT;
    public float a;

    private FractColor() {
        a = 1;
    }

    public static float getR(int color) {
        return Color.red(color) / 255.0f;
    }

    public static float getG(int color) {
        return Color.green(color) / 255.0f;
    }

    public static float getB(int color) {
        return Color.blue(color) / 255.0f;
    }

    public static final float packFloat(int c) {
        return Float.intBitsToFloat((c & 0xFF00FF00) | ((c & 0x00FF0000) >> 16) | ((c & 0x000000FF) << 16));
    }

    public abstract int packInt();

    public boolean equals(int color) {
        return color == packInt();
    }

    public boolean equals(Object obj) {
        if (obj instanceof FractColor)
            return ((FractColor) obj).packInt() == packInt();
        return false;
    }

    public final float packFloat() {
        return packFloat(packInt());
    }

    public abstract void set(int color);

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.integerData.put("color", packInt());
        return n;
    }

    @Override
    public void decode(FractCoder.Node node) {
        set(node.integerData.get("color"));
    }

    public static class RGB extends FractColor {

        public static final FractCoder.Decoder<RGB> DECODER = new FractCoder.Decoder<RGB>() {
            @Override
            public RGB decode(FractCoder.Node node) {
                return new RGB(node.integerData.get("color"));
            }
        };
        public float r, g, b;

        public RGB() {
        }

        public RGB(int color) {
            set(color);
        }

        public RGB(RGB color) {
            set(color);
        }

        public RGB(float r, float g, float b, float a) {
            set(r, g, b, a);
        }

        public RGB(float r, float g, float b) {
            set(r, g, b);
        }

        public static int packInt(float r, float g, float b) {
            return packInt(r, g, b, 1.0f);
        }

        public static int packInt(float r, float g, float b, float a) {
            return Color.argb((int) (a * 255), (int) (r * 255), (int) (g * 255), (int) (b * 255));
        }

        public void set(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public void lerp(float r, float g, float b, float progress) {
            set(FractMath.lerp(this.r, r, progress),
                    FractMath.lerp(this.g, g, progress),
                    FractMath.lerp(this.b, b, progress));
        }

        public void lerp(float r, float g, float b, float a, float progress) {
            set(FractMath.lerp(this.r, r, progress),
                    FractMath.lerp(this.g, g, progress),
                    FractMath.lerp(this.b, b, progress),
                    FractMath.lerp(this.a, a, progress));
        }

        public void set(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public int packInt() {
            return packInt(r, g, b, a);
        }

        @Override
        public void set(int color) {
            r = Color.red(color) / 255.0f;
            g = Color.green(color) / 255.0f;
            b = Color.blue(color) / 255.0f;
            a = Color.alpha(color) / 255.0f;
        }

        public void set(RGB color) {
            set(color.r, color.g, color.b, color.a);
        }

        public boolean equals(RGB color) {
            return color.r == r && color.g == g && color.b == b && color.a == a;
        }


    }

    public static class HSV extends FractColor {
        public static final FractCoder.Decoder<FractColor.HSV> DECODER = new FractCoder.Decoder<FractColor.HSV>() {
            @Override
            public FractColor.HSV decode(FractCoder.Node node) {
                return new HSV(node.integerData.get("color"));
            }
        };
        private static final float[] HSV = new float[3];
        public float h, s, v;

        public HSV() {
        }

        public HSV(int color) {
            set(color);
        }

        public HSV(FractColor.HSV color) {
            set(color);
        }

        public HSV(float h, float s, float v, float a) {
            set(h, s, v, a);
        }

        public HSV(float h, float s, float v) {
            set(h, s, v);
        }

        public static int packInt(float h, float s, float v) {
            return packInt(h, s, v, 1.0f);
        }

        public static int packInt(float h, float s, float v, float a) {
            HSV[0] = h * 360.0f;
            HSV[1] = s;
            HSV[2] = v;
            return Color.HSVToColor(HSV);
        }

        public void set(float h, float s, float v, float a) {
            this.h = h;
            this.s = s;
            this.v = v;
            this.a = a;
        }

        public void set(float h, float s, float v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }

        public void lerp(float h, float s, float b, float progress) {
            set(FractMath.lerp(this.h, h, progress),
                    FractMath.lerp(this.s, s, progress),
                    FractMath.lerp(this.v, v, progress));
        }

        public void lerp(float h, float s, float v, float a, float progress) {
            set(FractMath.lerp(this.h, h, progress),
                    FractMath.lerp(this.s, s, progress),
                    FractMath.lerp(this.v, v, progress),
                    FractMath.lerp(this.a, a, progress));
        }

        @Override
        public int packInt() {
            return packInt(h, s, v, a);
        }

        @Override
        public void set(int color) {
            Color.colorToHSV(color, HSV);
            h = HSV[0] / 360f;
            s = HSV[1];
            v = HSV[2];
            a = Color.alpha(color) / 255.0f;
        }

        public void set(FractColor.HSV color) {
            set(color.h, color.s, color.v, color.a);
        }

        public boolean equals(FractColor.HSV color) {
            return color.h == h && color.s == s && color.v == v && color.a == a;
        }
    }
}
