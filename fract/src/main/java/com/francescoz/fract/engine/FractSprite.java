package com.francescoz.fract.engine;

import com.francescoz.fract.utils.FractCoder;
import com.francescoz.fract.utils.FractColor;
import com.francescoz.fract.utils.FractOrigin;
import com.francescoz.fract.utils.FractSizing;
import com.francescoz.fract.utils.FractTransform;

public class FractSprite implements FractCoder.Codable {

    public FractColor color;
    public FractTransform transform;
    public String drawableKey;
    public FractSizing sizing;
    public FractOrigin horizontalOrigin;
    public FractOrigin verticalOrigin;
    private FractResources.Drawable cachedDrawable;

    public FractSprite() {
        sizing = FractSizing.FIXED_WH;
        horizontalOrigin = FractOrigin.CENTER;
        verticalOrigin = FractOrigin.CENTER;
    }

    FractResources.Drawable pullCache(FractResources resources) {
        if (cachedDrawable != null && resources == cachedDrawable.getResources() && cachedDrawable.key.equals(drawableKey))
            return cachedDrawable;
        return null;
    }

    void pushCache(FractResources.Drawable drawable) {
        cachedDrawable = drawable;
    }


    @Override
    public void decode(FractCoder.Node node) {
        sizing = node.getEncodableIf("sizing", FractSizing.DECODER);
        horizontalOrigin = node.getEncodableIf("sizing", FractOrigin.DECODER);
        verticalOrigin = node.getEncodableIf("sizing", FractOrigin.DECODER);
        transform = node.decodeOrGetEncodable("transform", FractTransform.DECODER, transform);
        color = node.decodeOrGetEncodable("color", FractColor.RGB.DECODER, (FractColor.RGB) color);
        drawableKey = node.stringData.get("drawableKey");
    }

    @Override
    public FractCoder.Node encode() {
        FractCoder.Node n = new FractCoder.Node();
        n.putEncodableIf("sizing", sizing);
        n.putEncodableIf("horizontalOrigin", horizontalOrigin);
        n.putEncodableIf("verticalOrigin", verticalOrigin);
        n.putEncodableIf("transform", transform);
        n.putEncodableIf("color", color);
        n.stringData.put("drawableKey", drawableKey);
        return n;
    }
}
