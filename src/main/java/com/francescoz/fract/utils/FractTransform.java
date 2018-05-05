package com.francescoz.fract.utils;

public class FractTransform implements FractCoder.Codable {

    public final FractVec translation;
    public final FractVec scale;
    public FractTransform parent;
    public float rotation;

    public FractTransform() {
        translation = new FractVec();
        rotation = 0;
        scale = new FractVec(FractVec.ONE);
    }

    public void reset() {
        translation.setZero();
        scale.setOne();
        rotation = 0;
    }

    public static final FractCoder.Decoder<FractTransform> DECODER = new FractCoder.Decoder<FractTransform>() {
        @Override
        public FractTransform decode(FractCoder.Node node) {
            FractTransform transform = new FractTransform();
            transform.decode(node);
            return transform;
        }
    };

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.putEncodable("translation", translation);
        n.putEncodable("scale", scale);
        n.floatData.put("rotation", rotation);
        return n;
    }

    @Override
    public void decode(FractCoder.Node node) {
        node.decode("translation", translation);
        node.decode("scale", scale);
        rotation = node.floatData.get("rotation");
    }
}
