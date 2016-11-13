package com.francescoz.fract.utils;

public class FractPixel implements FractCoder.Encodable {
    public static final FractCoder.Decoder<FractPixel> DECODER = new FractCoder.Decoder<FractPixel>() {


        @Override
        public FractPixel decode(FractCoder.Node node) {
            return new FractPixel(node.integerData.get("x"), node.integerData.get("y"));
        }
    };
    public final int x;
    public final int y;

    public FractPixel(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.integerData.put("x", x);
        n.integerData.put("y", y);
        return n;
    }
}
