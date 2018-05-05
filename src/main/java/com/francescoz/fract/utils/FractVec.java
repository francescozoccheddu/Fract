package com.francescoz.fract.utils;

public class FractVec implements FractCoder.Codable {

    public static final FractVec Y = new FractVec(0, 1);
    public static final FractVec X = new FractVec(1, 0);
    public static final FractVec ZERO = new FractVec(1, 0);
    public static final FractVec ONE = new FractVec(1, 1);
    public static final FractCoder.Decoder<FractVec> DECODER = new FractCoder.Decoder<FractVec>() {
        @Override
        public FractVec decode(FractCoder.Node node) {
            FractVec vec = new FractVec();
            vec.decode(node);
            return vec;
        }
    };
    public float x, y;

    public FractVec(float x, float y) {
        set(x, y);
    }

    public FractVec(FractVec clone) {
        set(clone);
    }

    public FractVec(FractPixel pixel) {
        set(pixel);
    }

    public FractVec() {

    }

    public FractVec set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public FractVec setY(float y) {
        this.y = y;
        return this;
    }

    public FractVec setX(float x) {
        this.x = x;
        return this;
    }

    public FractVec set(float xy) {
        this.x = xy;
        this.y = xy;
        return this;
    }

    public FractVec set(FractVec clone) {
        this.x = clone.x;
        this.y = clone.y;
        return this;
    }

    public FractVec set(FractPixel pixel) {
        this.x = pixel.x;
        this.y = pixel.y;
        return this;
    }

    public FractVec scl(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public FractVec scl(FractVec scalar) {
        this.x *= scalar.x;
        this.y *= scalar.y;
        return this;
    }

    public FractVec div(float scalar) {
        this.x /= scalar;
        this.y /= scalar;
        return this;
    }

    public FractVec div(FractVec scalar) {
        this.x /= scalar.x;
        this.y /= scalar.y;
        return this;
    }

    public FractVec add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public FractVec add(float scalar) {
        this.x += scalar;
        this.y += scalar;
        return this;
    }

    public FractVec add(FractVec scalar) {
        this.x += scalar.x;
        this.y += scalar.y;
        return this;
    }

    public FractVec scl(float x, float y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public FractVec div(float x, float y) {
        this.x /= x;
        this.y /= y;
        return this;
    }

    public FractVec sub(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public FractVec sub(float scalar) {
        this.x -= scalar;
        this.y -= scalar;
        return this;
    }

    public FractVec sub(FractVec scalar) {
        this.x -= scalar.x;
        this.y -= scalar.y;
        return this;
    }

    public float dst2(FractVec vec) {
        return FractMath.dst2(this, vec);
    }

    public float dst(FractVec vec) {
        return FractMath.dst(this, vec);
    }

    public float dst2(float x, float y) {
        return FractMath.dst2(x, y, this);
    }

    public float dst(float x, float y) {
        return FractMath.dst(x, y, this);
    }

    public FractVec setZero() {
        return set(0, 0);
    }

    public FractVec setOne() {
        return set(1, 1);
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.floatData.put("x", x);
        n.floatData.put("y", y);
        return n;
    }

    @Override
    public void decode(FractCoder.Node node) {
        x = node.floatData.get("x");
        y = node.floatData.get("y");
    }

    public boolean equals(Object obj) {
        if (obj instanceof FractVec) {
            FractVec v = (FractVec) obj;
            return equals(v);
        }
        return false;
    }

    public boolean equals(FractVec vec) {
        return equals(vec.x, vec.y);
    }

    public boolean equals(float x, float y) {
        return this.x == x && this.y == y;
    }
}
