package com.francescoz.fract.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.francescoz.fract.utils.FractCoder;
import com.francescoz.fract.utils.FractVec;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.microedition.khronos.opengles.GL10;

class FractResources {

    private static final Comparator<Drawable> KEY_COMPARATOR = new Comparator<Drawable>() {
        @Override
        public int compare(Drawable o1, Drawable o2) {
            return o1.key.compareTo(o2.key);
        }
    };
    private static final String CODER_TAG = "fractresourcesv1";
    private final Texture[] textures;
    private final Drawable[] drawables;
    private final String[] keys;

    private FractResources(PackedResourceDef packedResourceDef) {
        int textureCount = packedResourceDef.drawablePacks.length;
        int[] textureIds = new int[textureCount];
        GLES20.glGenTextures(textureCount, textureIds, 0);
        textures = new Texture[textureCount];
        drawables = new Drawable[packedResourceDef.drawableCount];
        int drawableIndex = 0;
        for (int t = 0; t < textureCount; t++) {
            FractDrawablePack pack = packedResourceDef.drawablePacks[t];
            Bitmap bitmap = pack.bitmap;
            Texture texture = new Texture(bitmap, packedResourceDef.filter, textureIds[t]);
            textures[t] = texture;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            FractDrawablePack.PackedDrawable[] packedDrawables = pack.packedDrawables;
            for (FractDrawablePack.PackedDrawable packedDrawable : packedDrawables)
                drawables[drawableIndex++] = new Drawable(texture, packedDrawable, width, height);
        }
        int len = drawables.length;
        Arrays.sort(drawables, KEY_COMPARATOR);
        keys = new String[len];
        for (int i = 0; i < len; i++)
            keys[i] = drawables[i].key;
    }

    private static int getMaxTextureSize() {
        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        return maxTextureSize[0];
    }

    static FractResources load(File file) {
        try {
            if (file.isFile() && file.canRead()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                ZipInputStream zipIn = new ZipInputStream(fileInputStream);
                ZipEntry zipEntry;
                Map<String, File> fileMap = new HashMap<>();
                while ((zipEntry = zipIn.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory()) {
                        File tmp = File.createTempFile("extracted", ".res");
                        tmp.createNewFile();
                        fileMap.put(zipEntry.getName(), tmp);
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp));
                        byte[] bytesIn = new byte[4096];
                        int read;
                        while ((read = zipIn.read(bytesIn)) != -1) {
                            bos.write(bytesIn, 0, read);
                        }
                        bos.close();
                    }
                    zipIn.closeEntry();
                }
                zipIn.close();
                fileInputStream.close();
                FractCoder coder = new FractCoder();
                coder.parseAndMerge(fileMap.get("res.txt"));
                FractCoder.Node root = coder.getNodeRoot();
                if (root.stringData.get("coderTag").equals(CODER_TAG)) {
                    int maxTextureSize = getMaxTextureSize();
                    int maxTextureCacheSize = root.integerData.get("maxTextureSize");
                    FractResourcesDef.Filter filter = root.getEncodable("filter", FractResourcesDef.Filter.DECODER);
                    FractCoder.Node packsNode = root.nodeData.get("packs");
                    Set<Map.Entry<String, FractCoder.Node>> packNodeSet = packsNode.nodeData.getEntrySet();
                    FractDrawablePack packs[] = new FractDrawablePack[packNodeSet.size()];
                    if (maxTextureCacheSize < maxTextureSize) {
                        if (packs.length > 1)
                            return null;
                    } else if (maxTextureCacheSize > maxTextureSize) {
                        for (FractDrawablePack pack : packs) {
                            Bitmap bitmap = pack.bitmap;
                            if (bitmap.getWidth() > maxTextureSize || bitmap.getHeight() > maxTextureSize)
                                return null;
                        }
                    }
                    int packIndex = 0;
                    for (Map.Entry<String, FractCoder.Node> entry : packNodeSet) {
                        File bitmapFile = fileMap.get(entry.getKey());
                        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
                        Collection<FractCoder.Node> drawableNodeSet = entry.getValue().nodeData.getValuesSet();
                        FractDrawablePack.PackedDrawable[] packedDrawables = new FractDrawablePack.PackedDrawable[drawableNodeSet.size()];
                        int drawableIndex = 0;
                        for (FractCoder.Node drawableNode : drawableNodeSet) {
                            FractDrawablePack.PackedDrawable drawable = FractDrawablePack.PackedDrawable.DECODER.decode(drawableNode);
                            packedDrawables[drawableIndex++] = drawable;
                        }
                        packs[packIndex++] = new FractDrawablePack(packedDrawables, bitmap);
                    }
                    return new FractResources(new PackedResourceDef(packs, filter));
                }
            }
        } catch (Exception e) {
            Log.d("FractResources", "Exception while loading file");
            e.printStackTrace();
        }
        file.delete();
        return null;
    }

    static FractResources createAndSave(FractResourcesDef resourcesDef, boolean halfBits, File file) throws IOException {
        int maxTextureSize = getMaxTextureSize();
        PackedResourceDef packedResourceDef = new PackedResourceDef(resourcesDef, maxTextureSize, halfBits);
        FractResources resources = new FractResources(packedResourceDef);
        file.createNewFile();
        FractCoder.Node packsNode = new FractCoder.Node();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        int packIndex = 0;
        for (FractDrawablePack pack : packedResourceDef.drawablePacks) {
            String name = "tex" + packIndex++ + ".png";
            zipOutputStream.putNextEntry(new ZipEntry(name));
            pack.bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOutputStream);
            zipOutputStream.closeEntry();
            FractCoder.Node packNode = new FractCoder.Node();
            int drawableIndex = 0;
            for (FractDrawablePack.PackedDrawable packedDrawable : pack.packedDrawables)
                packNode.putEncodable("drw" + drawableIndex++, packedDrawable);
            packsNode.nodeData.put(name, packNode);
        }
        FractCoder coder = new FractCoder();
        FractCoder.Node rootNode = coder.getNodeRoot();
        rootNode.stringData.put("coderTag", CODER_TAG);
        rootNode.integerData.put("maxTextureSize", maxTextureSize);
        rootNode.putEncodable("filter", packedResourceDef.filter);
        rootNode.nodeData.put("packs", packsNode);
        zipOutputStream.putNextEntry(new ZipEntry("res.txt"));
        BufferedWriter bufferedWriter = new BufferedWriter(new PrintWriter(zipOutputStream));
        coder.write(bufferedWriter);
        bufferedWriter.flush();
        zipOutputStream.closeEntry();
        zipOutputStream.close();
        fileOutputStream.close();
        return resources;
    }

    static FractResources create(FractResourcesDef resourcesDef, boolean halfBits) {
        return new FractResources(new PackedResourceDef(resourcesDef, getMaxTextureSize(), halfBits));
    }

    void destroy() {
        int textureCount = textures.length;
        int[] ids = new int[textureCount];
        for (int i = 0; i < textureCount; i++) {
            ids[i] = textures[i].textureID;
        }
        GLES20.glDeleteTextures(textureCount, ids, 0);
    }

    Drawable getDrawable(String drawableKey) {
        int index = Arrays.binarySearch(keys, drawableKey);
        if (index < 0)
            throw new IllegalArgumentException("No such Drawable with key '" + drawableKey + "'");
        return drawables[index];
    }

    private static final class PackedResourceDef {

        private final FractDrawablePack[] drawablePacks;
        private final FractResourcesDef.Filter filter;
        private final int drawableCount;

        private PackedResourceDef(FractResourcesDef resourcesDef, int maxTextureSize, boolean halfBits) {
            drawablePacks = FractDrawablePack.splitAndPack(resourcesDef.getDrawables(), maxTextureSize, 4, halfBits);
            filter = resourcesDef.filter;
            int drawableCount = 0;
            for (FractDrawablePack pack : drawablePacks)
                drawableCount += pack.packedDrawables.length;
            this.drawableCount = drawableCount;
        }

        private PackedResourceDef(FractDrawablePack[] drawablePacks, FractResourcesDef.Filter filter) {
            this.drawablePacks = drawablePacks;
            this.filter = filter;
            int drawableCount = 0;
            for (FractDrawablePack pack : drawablePacks)
                drawableCount += pack.packedDrawables.length;
            this.drawableCount = drawableCount;
        }
    }


    static class Texture {

        final float aspectRatio;
        final int textureID;

        Texture(int width, int height, FractResourcesDef.Filter filter, int textureID, int format) {
            this.textureID = textureID;
            if (textureID == 0)
                throw new RuntimeException("Unable to generate textures");
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter.minFilter);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter.magFilter);
            aspectRatio = width / (float) height;
        }

        private Texture(Bitmap bitmap, FractResourcesDef.Filter filter, int textureID) {
            this.textureID = textureID;
            if (textureID == 0)
                throw new RuntimeException("Unable to generate textures");
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter.minFilter);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter.magFilter);
           /*if (filter.generateMipmaps)
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);*/
            aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        }

        void bind(int textureUnit) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        }

    }

    class Drawable {

        final Texture texture;
        final float[] textureCoords;
        final String key;
        final boolean rotated;

        private Drawable(Texture texture, FractDrawablePack.PackedDrawable packedDrawable, int width, int height) {
            this(texture, packedDrawable.key, new FractVec(packedDrawable.topLeftVertex).div(width, height), new FractVec(packedDrawable.bottomRightVertex).div(width, height), packedDrawable.rotated);
        }

        private Drawable(Texture texture, String key, FractVec topLeftVertex, FractVec bottomRightVertex, boolean rotated) {
            this.texture = texture;
            this.key = key;
            this.textureCoords = new float[8];
            if (rotated) {
                textureCoords[2] = topLeftVertex.x;
                textureCoords[3] = topLeftVertex.y;
                textureCoords[6] = bottomRightVertex.x;
                textureCoords[7] = topLeftVertex.y;
                textureCoords[0] = topLeftVertex.x;
                textureCoords[1] = bottomRightVertex.y;
                textureCoords[4] = bottomRightVertex.x;
                textureCoords[5] = bottomRightVertex.y;
            } else {
                textureCoords[0] = topLeftVertex.x;
                textureCoords[1] = topLeftVertex.y;
                textureCoords[2] = bottomRightVertex.x;
                textureCoords[3] = topLeftVertex.y;
                textureCoords[4] = topLeftVertex.x;
                textureCoords[5] = bottomRightVertex.y;
                textureCoords[6] = bottomRightVertex.x;
                textureCoords[7] = bottomRightVertex.y;
            }
            this.rotated = rotated;
        }

        FractResources getResources() {
            return FractResources.this;
        }

    }
}
