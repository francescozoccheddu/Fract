package com.francescoz.fract.utils;

public final class FractMatrix implements FractCoder.Codable {

    private float m00, m01, m02, m10, m11, m12;

    public FractMatrix() {
    }

    public void identity() {
        m00 = 1;
        m01 = 0;
        m02 = 0;
        m10 = 0;
        m11 = 1;
        m12 = 0;
    }

    public void concat(FractTransform transform) {
        if (transform.parent != null) concat(transform.parent);
        concat(transform.translation, transform.rotation, transform.scale);
    }

    public void concat(FractVec translation, float rotation, FractVec scale) {
        concat(translation.x, translation.y, rotation, scale.x, scale.y);
    }

    public void concat(float translationX, float translationY, float rotation, float scaleX, float scaleY) {
        if (rotation == 0) {
            concat(translationX, translationY, scaleX, scaleY);
            return;
        }
        float cos = FractMath.cosDegress(rotation);
        float sin = FractMath.sinDegress(rotation);
        float t00 = scaleX * cos;
        float t10 = -scaleX * sin;
        float t01 = scaleY * sin;
        float t11 = scaleY * cos;
        float v00 = t00 * m00 + t01 * m10;
        float v01 = t00 * m01 + t01 * m11;
        float v02 = t00 * m02 + t01 * m12 + translationX;
        float v10 = t10 * m00 + t11 * m10;
        float v11 = t10 * m01 + t11 * m11;
        float v12 = t10 * m02 + t11 * m12 + translationY;
        m00 = v00;
        m10 = v10;
        m01 = v01;
        m11 = v11;
        m02 = v02;
        m12 = v12;
    }

    public void concat(FractVec translation, FractVec scale) {
        concat(translation.x, translation.y, scale.x, scale.y);
    }

    public void concat(float translationX, float translationY, float scaleX, float scaleY) {
        float v00 = scaleX * m00;
        float v01 = scaleX * m01;
        float v02 = scaleX * m02 + translationX;
        float v10 = scaleY * m10;
        float v11 = scaleY * m11;
        float v12 = scaleY * m12 + translationY;
        m00 = v00;
        m10 = v10;
        m01 = v01;
        m11 = v11;
        m02 = v02;
        m12 = v12;
    }

    public void transformArray(float[] array) {
        int pointCount = array.length / 2;
        int v = 0;
        for (int i = 0; i < pointCount; i++) {
            int iX = v++;
            int iY = v++;
            float x = array[iX];
            float y = array[iY];
            array[iX] = m00 * x + m01 * y + m02;
            array[iY] = m10 * x + m11 * y + m12;
        }
    }

    public void transformVec(FractVec vec) {
        float x = vec.x;
        float y = vec.y;
        vec.x = m00 * x + m01 * y + m02;
        vec.y = m10 * x + m11 * y + m12;
    }

    @Override
    public void decode(FractCoder.Node node) {
        m00 = node.floatData.get("m00");
        m01 = node.floatData.get("m01");
        m02 = node.floatData.get("m02");
        m10 = node.floatData.get("m10");
        m11 = node.floatData.get("m11");
        m12 = node.floatData.get("m12");
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.floatData.put("m00", m00);
        n.floatData.put("m01", m01);
        n.floatData.put("m02", m02);
        n.floatData.put("m10", m10);
        n.floatData.put("m11", m11);
        n.floatData.put("m12", m12);
        return n;
    }

    public static final FractCoder.Decoder<FractMatrix> DECODER = new FractCoder.Decoder<FractMatrix>() {
        @Override
        public FractMatrix decode(FractCoder.Node node) {
            FractMatrix matrix = new FractMatrix();
            matrix.decode(node);
            return matrix;
        }
    };
}
