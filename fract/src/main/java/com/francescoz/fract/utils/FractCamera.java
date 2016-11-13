package com.francescoz.fract.utils;

public class FractCamera implements FractCoder.Codable {
    public final FractVec position;
    private final FractTransform transform;
    public final FractVec scale;
    public float angle;
    public float zoom;

    public FractCamera() {
        transform = new FractTransform();
        position = new FractVec();
        scale = new FractVec(1, 1);
        angle = 0;
        zoom = 1;
    }

    public FractTransform updateTransform() {
        transform.reset();
        transform.scale.div(scale.x, scale.y).scl(zoom);
        transform.translation.sub(position);
        transform.rotation = angle;
        return transform;
    }

    public FractTransform getTransform() {
        return transform;
    }

    @Override
    public void decode(FractCoder.Node node) {
        node.decode("position", position);
        node.decode("scale", scale);
        angle = node.floatData.get("angle");
        zoom = node.floatData.get("zoom");
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.putEncodable("position", position);
        n.putEncodable("scale", scale);
        n.floatData.put("angle", angle);
        n.floatData.put("zoom", zoom);
        return n;
    }

    public static final FractCoder.Decoder<FractCamera> DECODER = new FractCoder.Decoder<FractCamera>() {
        @Override
        public FractCamera decode(FractCoder.Node node) {
            FractCamera camera = new FractCamera();
            camera.decode(node);
            return camera;
        }
    };
}
