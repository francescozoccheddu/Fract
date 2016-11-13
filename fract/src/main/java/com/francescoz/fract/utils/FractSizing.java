package com.francescoz.fract.utils;

public enum FractSizing implements FractCoder.Encodable {
    FIXED_WH, FIXED_W, FIXED_H;

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.integerData.put("ordinal", ordinal());
        return n;
    }

    public static final FractCoder.Decoder<FractSizing> DECODER = new FractCoder.Decoder<FractSizing>() {
        @Override
        public FractSizing decode(FractCoder.Node node) {
            return values()[node.integerData.get("ordinal")];
        }
    };
}