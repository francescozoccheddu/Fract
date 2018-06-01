package com.francescoz.fract.utils;

public enum FractOrigin implements FractCoder.Encodable {
    LEFT_BOTTOM(0.5f), CENTER(0), RIGHT_TOP(-0.5f);
    public final float alpha;

    FractOrigin(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.integerData.put("ordinal", ordinal());
        return n;
    }

    public static final FractCoder.Decoder<FractOrigin> DECODER = new FractCoder.Decoder<FractOrigin>() {
        @Override
        public FractOrigin decode(FractCoder.Node node) {
            return values()[node.integerData.get("ordinal")];
        }
    };
}