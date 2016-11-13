package com.francescoz.fract.engine;

import android.opengl.GLES20;

import com.francescoz.fract.utils.FractColor;
import com.francescoz.fract.utils.FractMatrix;
import com.francescoz.fract.utils.FractOrigin;
import com.francescoz.fract.utils.FractSizing;
import com.francescoz.fract.utils.FractTransform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


class FractBatch {

    private static final int POSITION_ATTRIB_LOC = 0;
    private static final int COLOR_ATTRIB_LOC = 1;
    private static final int TEXTURECOORD_ATTRIB_LOC = 2;
    private static final float[] QUAD_VERTICES;
    private static final String POSITION_ATTRIB_NAME = "a_position";
    private static final String COLOR_ATTRIB_NAME = "a_color";
    private static final String TEXTURECOORD_ATTRIB_NAME = "a_texturecoord";
    private static final String TEXTURE_UNIFORM_NAME = "u_texture";
    private static final String VERTEX_SHADER_SOURCE;
    private static final String FRAGMENT_SHADER_SOURCE;
    private static final float DEFAULT_COLOR_PACKED = FractColor.packFloat(FractColor.WHITE);
    private static final float[] SCREEN_MAP_VERTICES;
    private static final FractResourcesDef.Filter FBO_FILTER = new FractResourcesDef.Filter(FractResourcesDef.Filter.Type.NEAREST, FractResourcesDef.Filter.Type.NEAREST);

    static {
        VERTEX_SHADER_SOURCE =
                "attribute lowp vec2 " + POSITION_ATTRIB_NAME + ";\n" +
                        "attribute lowp vec4 " + COLOR_ATTRIB_NAME + ";\n" +
                        "attribute lowp vec2 " + TEXTURECOORD_ATTRIB_NAME + ";\n" +
                        "varying lowp vec4 v_color;\n" +
                        "varying lowp vec2 v_textcoord;\n" +
                        "void main () {\n" +
                        "v_color = " + COLOR_ATTRIB_NAME + ";\n" +
                        "v_textcoord = " + TEXTURECOORD_ATTRIB_NAME + ";\n" +
                        "gl_Position = vec4(" + POSITION_ATTRIB_NAME + ", 0.0, 1.0 ); }\n";
        FRAGMENT_SHADER_SOURCE =
                "uniform sampler2D " + TEXTURE_UNIFORM_NAME + ";\n" +
                        "varying lowp vec4 v_color;\n" +
                        "varying lowp vec2 v_textcoord;\n" +
                        "void main () {\n" +
                        "gl_FragColor = texture2D(" + TEXTURE_UNIFORM_NAME + ", v_textcoord) * v_color; }\n";
        QUAD_VERTICES = new float[]{
                -0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f
        };
        SCREEN_MAP_VERTICES = new float[]{
                -1, 1, DEFAULT_COLOR_PACKED, 0, 1,
                1, 1, DEFAULT_COLOR_PACKED, 1, 1,
                -1, -1, DEFAULT_COLOR_PACKED, 0, 0,
                1, -1, DEFAULT_COLOR_PACKED, 1, 0
        };
    }

    private final float[] vertices;
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int maxSprites;
    private final float[] quadPositions;
    private final FractMatrix matrix;
    private int program;
    private int spritesInBatch;
    private int textureUniformLoc;
    private FractResources.Texture[] units;
    private int last;
    private int current;

    FractBatch(int maxSprites) {
        this.maxSprites = maxSprites;
        int floatCount = 5 * 4 * maxSprites;
        vertices = new float[floatCount];
        ByteBuffer vbb = ByteBuffer.allocateDirect(floatCount * Float.SIZE / 8);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        int shortCount = 6 * maxSprites;
        ByteBuffer ibb = ByteBuffer.allocateDirect(shortCount * Short.SIZE / 8);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.position(0);
        short[] indices = new short[shortCount];
        int i = 0;
        for (int q = 0; q < maxSprites; q++) {
            int qn = q * 4;
            indices[i++] = (short) (qn);
            indices[i++] = (short) (qn + 1);
            indices[i++] = (short) (qn + 2);
            indices[i++] = (short) (qn + 2);
            indices[i++] = (short) (qn + 1);
            indices[i++] = (short) (qn + 3);
        }
        indexBuffer.put(indices, 0, shortCount);
        program = 0;
        quadPositions = new float[8];
        matrix = new FractMatrix();
    }

    private static int createShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Shader not created");
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader not compiled: \n" + GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private void unbind(int unit) {
        units[unit] = null;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void bind(FractResources.Texture texture) {
        for (int i = 0; i < units.length; i++)
            if (units[i] == texture) {
                current = i;
                return;
            }
        units[last] = texture;
        texture.bind(last);
        current = last;
        last = (last + 1) % units.length;
    }

    void create() {
        if (program != 0)
            destroy(program);
        program = GLES20.glCreateProgram();
        if (program == 0)
            throw new RuntimeException("Program not created");
        int vs = createShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE);
        int fs = createShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE);
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glBindAttribLocation(program, POSITION_ATTRIB_LOC, POSITION_ATTRIB_NAME);
        GLES20.glBindAttribLocation(program, COLOR_ATTRIB_LOC, COLOR_ATTRIB_NAME);
        GLES20.glBindAttribLocation(program, TEXTURECOORD_ATTRIB_LOC, TEXTURECOORD_ATTRIB_NAME);
        GLES20.glLinkProgram(program);
        GLES20.glDetachShader(program, vs);
        GLES20.glDetachShader(program, fs);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Program not linked: \n" + GLES20.glGetProgramInfoLog(program));
        }
        textureUniformLoc = GLES20.glGetUniformLocation(program, TEXTURE_UNIFORM_NAME);
        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 6 * maxSprites * Short.SIZE / 8, indexBuffer.position(0), GLES20.GL_STATIC_DRAW);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
        GLES20.glDepthMask(false);
        resetBlendFunc();
        spritesInBatch = 0;
        int[] maxTextureUnits = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureUnits, 0);
        units = new FractResources.Texture[maxTextureUnits[0]];
        GLES20.glUseProgram(program);
        last = current = 0;
    }

    private void resetBlendFunc() {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }


    void flush() {
        if (spritesInBatch == 0) return;
        vertexBuffer.position(0);
        vertexBuffer.put(vertices, 0, spritesInBatch * 5 * 4);
        int stride = 5 * Float.SIZE / 8;
        GLES20.glUniform1i(textureUniformLoc, current);
        GLES20.glEnableVertexAttribArray(POSITION_ATTRIB_LOC);
        GLES20.glVertexAttribPointer(POSITION_ATTRIB_LOC, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer.position(0));
        GLES20.glEnableVertexAttribArray(COLOR_ATTRIB_LOC);
        GLES20.glVertexAttribPointer(COLOR_ATTRIB_LOC, 4, GLES20.GL_UNSIGNED_BYTE, true, stride, vertexBuffer.position(2));
        GLES20.glEnableVertexAttribArray(TEXTURECOORD_ATTRIB_LOC);
        GLES20.glVertexAttribPointer(TEXTURECOORD_ATTRIB_LOC, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer.position(3));
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, spritesInBatch * 6, GLES20.GL_UNSIGNED_SHORT, 0);
        spritesInBatch = 0;
    }

    void draw(FractResources.Drawable drawable, FractScreen.Viewport viewport, FractColor color, FractTransform transform, FractSizing sizing, FractOrigin horizontalOrigin, FractOrigin verticalOrigin) {
        if (color != null && color.a <= 0) return;
        FractResources.Texture texture = drawable.texture;
        float textureAspectRatio = drawable.rotated ? 1.0f / texture.aspectRatio : texture.aspectRatio;
        for (int i = 0; i < QUAD_VERTICES.length; i++)
            quadPositions[i] = QUAD_VERTICES[i];
        horizontalOrigin = horizontalOrigin == null ? FractOrigin.CENTER : horizontalOrigin;
        verticalOrigin = verticalOrigin == null ? FractOrigin.CENTER : verticalOrigin;
        matrix.identity();
        switch (sizing == null ? FractSizing.FIXED_WH : sizing) {
            case FIXED_WH:
                matrix.concat(horizontalOrigin.alpha, verticalOrigin.alpha, 1, 1);
                break;
            case FIXED_H:
                matrix.concat(horizontalOrigin.alpha * textureAspectRatio, verticalOrigin.alpha, textureAspectRatio, 1);
                break;
            case FIXED_W:
                float h = 1.0f / textureAspectRatio;
                matrix.concat(horizontalOrigin.alpha, verticalOrigin.alpha * h, 1, h);
                break;
            default:
                throw new RuntimeException("Unknown Sizing");
        }
        if (transform != null) matrix.concat(transform);
        viewport.concat(matrix);
        matrix.transformArray(quadPositions);
        boolean visibile = false;
        for (int i = 0; i < 4; i++) {
            int vi = i * 2;
            float x = quadPositions[vi];
            float y = quadPositions[vi + 1];
            if (x < 1 && x > -1 && y < 1 && y > -1) {
                visibile = true;
                break;
            }
        }
        if (!visibile) return;
        if (units[current] != texture) {
            flush();
            bind(texture);
        }
        int verticesIndex = 20 * spritesInBatch++;
        int quadIndex = 0;
        int textureCoordsIndex = 0;
        float colorPacked = color == null ? DEFAULT_COLOR_PACKED : color.packFloat();
        float[] textureCoords = drawable.textureCoords;
        for (int v = 0; v < 4; v++) {
            vertices[verticesIndex++] = quadPositions[quadIndex++];
            vertices[verticesIndex++] = quadPositions[quadIndex++];
            vertices[verticesIndex++] = colorPacked;
            vertices[verticesIndex++] = textureCoords[textureCoordsIndex++];
            vertices[verticesIndex++] = textureCoords[textureCoordsIndex++];
        }
        if (spritesInBatch >= maxSprites)
            flush();
    }

    private void destroy(int program) {
        GLES20.glUseProgram(0);
        GLES20.glDeleteProgram(program);
    }

    final class Masker {

        private final FractEngine.Drawer maskDrawer, maskedDrawer;
        private FractResources.Texture maskTexture, maskedTexture;
        private int maskFB, maskedFB;

        Masker(FractEngine engine) {
            maskedDrawer = engine.new Drawer();
            maskDrawer = engine.new Drawer();
        }

        void create(int width, int height) {
            FractResources.Texture boundTexture = units[current];
            int boundTextureID = boundTexture == null ? 0 : boundTexture.textureID;
            int[] textureID = new int[2];
            if (maskTexture != null) {
                textureID[0] = maskTexture.textureID;
                textureID[1] = maskedTexture.textureID;
                GLES20.glDeleteTextures(2, textureID, 0);
            }
            GLES20.glGenTextures(2, textureID, 0);
            maskTexture = new FractResources.Texture(width, height, FBO_FILTER, textureID[0], GLES20.GL_RGBA);
            maskedTexture = new FractResources.Texture(width, height, FBO_FILTER, textureID[1], GLES20.GL_RGBA);
            int[] fboID = new int[2];
            if (maskFB != 0) {
                fboID[0] = maskFB;
                fboID[1] = maskedFB;
                GLES20.glDeleteFramebuffers(2, fboID, 0);
            }
            GLES20.glGenFramebuffers(2, fboID, 0);
            maskFB = fboID[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, maskFB);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, maskTexture.textureID, 0);
            maskedFB = fboID[1];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, maskedFB);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, maskedTexture.textureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, boundTextureID);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        void draw(FractMaskCallback maskCallback, boolean inverted) {
            flush();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, maskFB);
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            maskDrawer.valid = true;
            maskCallback.drawMask(maskDrawer);
            maskDrawer.valid = false;
            flush();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, maskedFB);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            maskedDrawer.valid = true;
            maskCallback.drawMasked(maskedDrawer);
            maskedDrawer.valid = false;
            flush();
            GLES20.glBlendFuncSeparate(GLES20.GL_ZERO, GLES20.GL_ONE, GLES20.GL_ZERO, inverted ? GLES20.GL_ONE_MINUS_SRC_ALPHA : GLES20.GL_SRC_ALPHA);
            draw(maskTexture);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            resetBlendFunc();
            draw(maskedTexture);
        }

        private void draw(FractResources.Texture texture) {
            spritesInBatch = 1;
            bind(texture);
            for (int i = 0; i < 20; i++)
                vertices[i] = SCREEN_MAP_VERTICES[i];
            flush();
            unbind(current);
        }

    }

}
