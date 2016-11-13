package com.francescoz.fract.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.francescoz.fract.utils.FractCoder;
import com.francescoz.fract.utils.FractMath;
import com.francescoz.fract.utils.FractPixel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class FractDrawablePack {

    private static final Comparator<FractResourcesDef.Drawable> PRIORITY_COMPARATOR = new Comparator<FractResourcesDef.Drawable>() {
        @Override
        public int compare(FractResourcesDef.Drawable o1, FractResourcesDef.Drawable o2) {
            return o1.priority - o2.priority;
        }
    };

    final PackedDrawable[] packedDrawables;
    final Bitmap bitmap;


    FractDrawablePack(PackedDrawable[] packedDrawables, Bitmap bitmap) {
        this.packedDrawables = packedDrawables;
        this.bitmap = bitmap;
    }

    private FractDrawablePack(PackedBitmap[] packedBitmaps, boolean halfBits) {
        this.packedDrawables = packedBitmaps;
        int w = 0;
        int h = 0;
        for (PackedBitmap packedBitmap : packedBitmaps) {
            FractPixel bottomRightVertex = packedBitmap.bottomRightVertex;
            if (bottomRightVertex.x > w)
                w = bottomRightVertex.x;
            if (bottomRightVertex.y > h)
                h = bottomRightVertex.y;
        }
        int width = (int) Math.pow(2, Math.ceil(Math.log(w) * FractMath.TO_LOG2));
        int height = (int) Math.pow(2, Math.ceil(Math.log(h) * FractMath.TO_LOG2));
        bitmap = Bitmap.createBitmap(width, height, halfBits ? Bitmap.Config.ARGB_4444 : Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        for (PackedBitmap packedBitmap : packedBitmaps)
            packedBitmap.draw(canvas);
    }

    private static void splitAndPack(FractResourcesDef.Drawable[] drawableDefs, int maximumSize, int border, List<FractDrawablePack> packList, boolean halfBits) {
        FractDrawablePack fullPack = pack(drawableDefs, maximumSize, border, halfBits);
        if (fullPack != null)
            packList.add(fullPack);
        else if (drawableDefs.length < 2)
            throw new RuntimeException("Unpackable drawable");
        else if (drawableDefs.length == 2) {
            packList.add(pack(new FractResourcesDef.Drawable[]{drawableDefs[0]}, maximumSize, border, halfBits));
            packList.add(pack(new FractResourcesDef.Drawable[]{drawableDefs[1]}, maximumSize, border, halfBits));
        } else {
            int firstDiff = drawableDefs[1].priority - drawableDefs[0].priority;
            int minDiff = firstDiff;
            int maxDiff = firstDiff;
            for (int i = 1; i < drawableDefs.length; i++) {
                int diff = drawableDefs[i].priority - drawableDefs[i - 1].priority;
                if (diff > maxDiff)
                    maxDiff = diff;
                if (diff < minDiff)
                    minDiff = diff;
            }
            if (maxDiff == minDiff)
                throw new RuntimeException("Unpackable indivisible drawable group");
            int from = 0;
            for (int i = 1; i < drawableDefs.length; i++) {
                if (drawableDefs[i].priority - drawableDefs[i - 1].priority == maxDiff) {
                    int len = i - from;
                    FractResourcesDef.Drawable[] part = new FractResourcesDef.Drawable[len];
                    for (int j = 0; j < len; j++)
                        part[j] = drawableDefs[j + from];
                    packList.add(pack(part, maximumSize, border, halfBits));
                    from = i;
                }
            }
            int len = drawableDefs.length - 1 - from;
            if (len > 0) {
                FractResourcesDef.Drawable[] part = new FractResourcesDef.Drawable[len];
                for (int i = 0; i < len; i++)
                    part[i] = drawableDefs[i + from];
                packList.add(pack(part, maximumSize, border, halfBits));
            }
        }
    }

    static FractDrawablePack[] splitAndPack(FractResourcesDef.Drawable[] drawableDefs, int maximumSize, int border, boolean halfBits) {
        ArrayList<FractDrawablePack> packList = new ArrayList<>();
        Arrays.sort(drawableDefs, PRIORITY_COMPARATOR);
        splitAndPack(drawableDefs, maximumSize, border, packList, halfBits);
        FractDrawablePack[] packs = new FractDrawablePack[packList.size()];
        packList.toArray(packs);
        return packs;
    }

    private static FractDrawablePack pack(FractResourcesDef.Drawable[] drawableDefs, int maximumSize, int border, boolean halfBits) {
        if (drawableDefs.length == 0) throw new RuntimeException("No drawableDefss");
        ArrayList<Candidate> candidateList = new ArrayList<>();
        for (FractResourcesDef.Drawable drawableDef : drawableDefs) {
            candidateList.add(new Candidate(drawableDef, false));
            if (drawableDef.bitmap.getHeight() != drawableDef.bitmap.getWidth())
                candidateList.add(new Candidate(drawableDef, true));
        }
        Collections.sort(candidateList, Candidate.HEIGHT_COMPARATOR);
        Candidate[] candidates = new Candidate[candidateList.size()];
        candidateList.toArray(candidates);
        int area = 0;
        for (FractResourcesDef.Drawable drawableDef : drawableDefs) {
            Bitmap bitmap = drawableDef.bitmap;
            area += bitmap.getWidth() * bitmap.getHeight();
        }
        int exp = (int) Math.ceil(Math.log(area) * FractMath.TO_LOG2 / 2.0);
        int size = 0;
        PackedBitmap[] packedBitmaps = null;
        while (packedBitmaps == null && size < maximumSize) {
            size = (int) Math.pow(2, exp++);
            packedBitmaps = pack(size, size, border, candidates);
        }
        return packedBitmaps == null ? null : new FractDrawablePack(packedBitmaps, halfBits);
    }

    private static PackedBitmap[] pack(int width, int height, int border, Candidate[] candidates) {
        int cellsX = 1, cellsY = 1;
        ArrayList<ArrayList<Boolean>> occupiedCells = new ArrayList<>();
        ArrayList<Integer> cellsRightX = new ArrayList<>();
        ArrayList<Integer> cellsBottomY = new ArrayList<>();
        ArrayList<PackedBitmap> packedBitmapList = new ArrayList<>();
        occupiedCells.add(new ArrayList<Boolean>());
        occupiedCells.get(0).add(false);
        int packWidth = width + border;
        int packHeight = height + border;
        cellsRightX.add(packWidth);
        cellsBottomY.add(packHeight);
        candidateIterator:
        for (Candidate candidate : candidates) {
            if (candidate.shouldSkip(packedBitmapList))
                continue;
            int drawableWidth = candidate.getWidth() + border;
            int drawableHeight = candidate.getHeight() + border;
            columnIterator:
            for (int x = 0; x < cellsX; x++) {
                int columnLeftX = x == 0 ? 0 : cellsRightX.get(x - 1);
                if (packWidth - columnLeftX < drawableWidth)
                    if (candidate.shouldReturn())
                        return null;
                    else
                        continue candidateIterator;
                ArrayList<Boolean> occupiedColumn = occupiedCells.get(x);
                rowIterator:
                for (int y = 0; y < cellsY; y++) {
                    int rowTopY = y == 0 ? 0 : cellsBottomY.get(y - 1);
                    if (packHeight - rowTopY < drawableHeight)
                        continue columnIterator;
                    if (occupiedColumn.get(y))
                        continue;
                    int toX = x;
                    int cellWidth = 0;
                    while (cellWidth < drawableWidth) {
                        cellWidth = cellsRightX.get(toX) - columnLeftX;
                        toX++;
                    }
                    int toY = y;
                    int cellHeight = 0;
                    while (cellHeight < drawableHeight) {
                        cellHeight = cellsBottomY.get(toY) - rowTopY;
                        toY++;
                    }
                    for (int cx = x; cx < toX; cx++) {
                        for (int cy = y; cy < toY; cy++) {
                            if (occupiedCells.get(cx).get(cy))
                                continue rowIterator;
                        }
                    }
                    packedBitmapList.add(new PackedBitmap(candidate.drawableDef, columnLeftX, rowTopY, candidate.rotated));
                    int packedRightX, packedBottomY;
                    packedRightX = columnLeftX + drawableWidth;
                    packedBottomY = rowTopY + drawableHeight;
                    if (cellWidth != drawableWidth) {
                        cellsX++;
                        ArrayList<Boolean> newColumn = new ArrayList<>();
                        newColumn.addAll(occupiedCells.get(toX - 1));
                        occupiedCells.add(toX - 1, newColumn);
                        cellsRightX.add(toX - 1, packedRightX);
                    }
                    if (cellHeight != drawableHeight) {
                        cellsY++;
                        for (ArrayList<Boolean> column : occupiedCells) {
                            column.add(toY - 1, column.get(toY - 1));
                        }
                        cellsBottomY.add(toY - 1, packedBottomY);
                    }
                    for (int cx = x; cx < toX; cx++) {
                        for (int cy = y; cy < toY; cy++) {
                            occupiedCells.get(cx).set(cy, true);
                        }
                    }
                    continue candidateIterator;
                }
            }
            if (candidate.shouldReturn())
                return null;
        }
        if (packedBitmapList.isEmpty())
            return null;
        PackedBitmap[] packedBitmaps = new PackedBitmap[packedBitmapList.size()];
        packedBitmapList.toArray(packedBitmaps);
        return packedBitmaps;
    }


    private static final class Candidate {

        private static final Comparator<? super Candidate> HEIGHT_COMPARATOR = new Comparator<Candidate>() {
            @Override
            public int compare(Candidate o1, Candidate o2) {
                return o2.getHeight() - o1.getHeight();
            }
        };
        private final FractResourcesDef.Drawable drawableDef;
        private final boolean rotated;

        Candidate(FractResourcesDef.Drawable drawableDef, boolean rotated) {
            this.drawableDef = drawableDef;
            this.rotated = rotated;
        }

        int getWidth() {
            return rotated ? drawableDef.bitmap.getHeight() : drawableDef.bitmap.getWidth();
        }

        int getHeight() {
            return rotated ? drawableDef.bitmap.getWidth() : drawableDef.bitmap.getHeight();
        }

        boolean shouldSkip(ArrayList<PackedBitmap> packedBitmaps) {
            if (isGreaterThanItsRotation())
                return false;
            String key = drawableDef.key;
            for (PackedBitmap packedBitmap : packedBitmaps)
                if (packedBitmap.key.equals(key)) return true;
            return false;
        }

        private boolean isGreaterThanItsRotation() {
            return getHeight() > getWidth();
        }

        boolean shouldReturn() {
            return !isGreaterThanItsRotation();
        }

    }

    static class PackedDrawable implements FractCoder.Encodable {
        static final FractCoder.Decoder<PackedDrawable> DECODER = new FractCoder.Decoder<PackedDrawable>() {
            @Override
            public PackedDrawable decode(FractCoder.Node node) {
                String key = node.stringData.get("key");
                boolean rotated = node.booleanData.get("rotated");
                FractPixel topLeftVertex = FractPixel.DECODER.decode(node.nodeData.get("topLeftVertex"));
                FractPixel bottomRightVertex = FractPixel.DECODER.decode(node.nodeData.get("bottomRightVertex"));
                return new PackedDrawable(key, topLeftVertex, bottomRightVertex, rotated);
            }
        };
        final String key;
        final FractPixel topLeftVertex, bottomRightVertex;
        final boolean rotated;

        private PackedDrawable(String key, FractPixel topLeftVertex, FractPixel bottomRightVertex, boolean rotated) {
            this.key = key;
            this.topLeftVertex = topLeftVertex;
            this.bottomRightVertex = bottomRightVertex;
            this.rotated = rotated;
        }

        private PackedDrawable(String key, int x, int y, int w, int h, boolean rotated) {
            this.key = key;
            this.rotated = rotated;
            topLeftVertex = new FractPixel(x, y);
            if (rotated)
                bottomRightVertex = new FractPixel(x + h, y + w);
            else
                bottomRightVertex = new FractPixel(x + w, y + h);

        }

        @Override
        public FractCoder.Node encode() {
            FractCoder.Node n = new FractCoder.Node();
            n.stringData.put("key", key);
            n.booleanData.put("rotated", rotated);
            n.nodeData.put("topLeftVertex", topLeftVertex.encode());
            n.nodeData.put("bottomRightVertex", bottomRightVertex.encode());
            return n;
        }
    }

    private static final class PackedBitmap extends PackedDrawable {
        private static final Matrix MATRIX = new Matrix();
        private final Bitmap bitmap;

        private PackedBitmap(FractResourcesDef.Drawable drawableDef, int x, int y, boolean rotated) {
            super(drawableDef.key, x, y, drawableDef.bitmap.getWidth(), drawableDef.bitmap.getHeight(), rotated);
            this.bitmap = drawableDef.bitmap;
        }

        void draw(Canvas canvas) {
            MATRIX.reset();
            if (rotated) {
                MATRIX.setTranslate(-bitmap.getWidth(), 0);
                MATRIX.postRotate(-90);
            }
            MATRIX.postTranslate(topLeftVertex.x, topLeftVertex.y);
            canvas.drawBitmap(bitmap, MATRIX, null);
        }


    }
}
