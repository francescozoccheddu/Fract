package com.francescoz.fract.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.francescoz.fract.R;
import com.francescoz.fract.utils.FractColor;
import com.francescoz.fract.utils.FractInput;
import com.francescoz.fract.utils.FractMath;
import com.francescoz.fract.utils.FractOrigin;
import com.francescoz.fract.utils.FractSizing;
import com.francescoz.fract.utils.FractTransform;
import com.francescoz.fract.utils.FractVec;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public abstract class FractEngine {

    private final FractBatch batch;
    private final File diskCache;
    private final Surface surface;
    private final FractBatch.Masker masker;
    private FractScreen currentScreen, nextScreen;
    private FractResources resources;

    public FractEngine(Context context, Config config) {
        if (!isSupported(context))
            throw new RuntimeException("FractEngine is not supported by this device");
        splash(context);
        this.diskCache = config.diskCache;
        currentScreen = new FractScreen.DefaultScreen();
        batch = new FractBatch(config.spriteBufferSize);
        masker = config.requireMasking ? batch.new Masker(this) : null;
        surface = new Surface(context, config.allowLowPrecisionColors, config.requireTransparentSurface);
    }

    private static final void splash(Context context) {
        Toast.makeText(context, R.string.splash, Toast.LENGTH_LONG).show();
    }

    public static boolean isSupported(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    public final boolean hasMask() {
        return masker != null;
    }

    public final FractScreen getScreen() {
        return currentScreen;
    }

    public final void setScreen(FractScreen fractScreen) {
        nextScreen = fractScreen;
    }

    public final FractScreen getNextScreen() {
        return nextScreen;
    }

    public final File getDiskCache() {
        return diskCache;
    }

    public final boolean hasDiskCache() {
        return diskCache != null;
    }

    public final View getView() {
        return surface;
    }

    private final void reloadResources() {
        if (resources != null) resources.destroy();
        if (hasDiskCache()) {
            if ((resources = FractResources.load(diskCache)) == null) {
                try {
                    resources = FractResources.createAndSave(createResources(), surface.halfBits, diskCache);
                    Log.d("FractEngine", "Resources successfully created and written to file");
                } catch (IOException e) {
                    if (diskCache != null) diskCache.delete();
                    throw new RuntimeException("Error while writing disk cache");
                }
            } else
                Log.d("FractEngine", "Resources successfully loaded from file");
        } else {
            resources = FractResources.create(createResources(), surface.halfBits);
        }
    }

    protected abstract FractResourcesDef createResources();

    public static final class Config {

        public File diskCache;
        public boolean allowLowPrecisionColors;
        public boolean requireTransparentSurface;
        public boolean requireMasking;
        public int spriteBufferSize;

        public Config() {
            spriteBufferSize = 128;
        }
    }

    private static final class EGLMatch {
        private static final int SAMPLES_COST = 2;
        private final int cost;
        private final boolean halfBitsRGB;
        private final EGLConfig config;

        private EGLMatch(EGL10 egl, EGLDisplay display, EGLConfig config, boolean alpha) {
            this.config = config;
            int[] pointer = new int[1];
            int r = get(egl, display, EGL10.EGL_RED_SIZE, pointer);
            int g = get(egl, display, EGL10.EGL_GREEN_SIZE, pointer);
            int b = get(egl, display, EGL10.EGL_BLUE_SIZE, pointer);
            int a = get(egl, display, EGL10.EGL_ALPHA_SIZE, pointer);
            int depth = get(egl, display, EGL10.EGL_DEPTH_SIZE, pointer);
            int stencil = get(egl, display, EGL10.EGL_STENCIL_SIZE, pointer);
            int luminance = get(egl, display, EGL10.EGL_LUMINANCE_SIZE, pointer);
            int alphaMask = get(egl, display, EGL10.EGL_ALPHA_MASK_SIZE, pointer);
            int samples = get(egl, display, EGL10.EGL_SAMPLES, pointer);
            int uselessCost = depth + stencil + luminance + alphaMask + samples * SAMPLES_COST + (alpha ? 0 : a);
            cost = r + g + b + uselessCost + (alpha ? a : 0);
            halfBitsRGB = r <= 4 && g <= 4 && b <= 4 && a <= 4;
            Log.d("FractEngine", "Supported Config:" +
                    " r:" + r +
                    " g:" + g +
                    " b:" + b +
                    " a:" + a +
                    " dpt:" + depth +
                    " stnc:" + stencil +
                    " lum:" + luminance +
                    " amsk:" + alphaMask +
                    " smpls:" + samples +
                    " cost:" + cost);
        }

        private int get(EGL10 egl, EGLDisplay display, int attrib, int[] pointer) {
            egl.eglGetConfigAttrib(display, config, attrib, pointer);
            return pointer[0];
        }
    }

    private static final class InputHandler implements View.OnTouchListener {

        private final Pointer[] pointers;
        private FractScreen screen;
        private float width, height;

        private InputHandler() {
            pointers = new Pointer[FractInput.MAX_FINGERS];
            for (int i = 0; i < FractInput.MAX_FINGERS; i++)
                pointers[i] = new Pointer();
        }

        private void setSize(float width, float height) {
            this.width = width;
            this.height = height;
            cancel();
        }

        private void cancel() {
            synchronized (pointers) {
                for (Pointer p : pointers)
                    p.cancel();
            }
        }

        private void setScreen(FractScreen screen) {
            this.screen = screen;
            cancel();
        }

        private void fire() {
            FractInput.Listener listener = screen.inputListener;
            synchronized (pointers) {
                for (int id = 0; id < FractInput.MAX_FINGERS; id++) {
                    Pointer p = pointers[id];
                    if (p.wasPressed != p.pressed)
                        if (p.pressed) listener.onTouchDown(id, p.position.x, p.position.y);
                        else listener.onTouchUp(id, p.position.x, p.position.y);
                    if (p.moved)
                        listener.onTouchMoved(id, p.position.x, p.position.y);
                    p.proceed();
                }
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            synchronized (pointers) {
                FractScreen.Viewport vp = screen.viewport;
                int id = event.getActionIndex();
                switch (event.getActionMasked()) {
                    default:
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        cancel();
                        return true;
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (id > FractInput.MAX_FINGERS)
                            return false;
                        pointers[id].press(vp.getLeftX() + (event.getX(id) / width) * vp.getWidth(), vp.getBottomY() + (1 - (event.getY(id) / height)) * vp.getHeight());
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (id > FractInput.MAX_FINGERS)
                            return false;
                        pointers[id].release(vp.getLeftX() + (event.getX(id) / width) * vp.getWidth(), vp.getBottomY() + (1 - (event.getY(id) / height)) * vp.getHeight());
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (id > FractInput.MAX_FINGERS)
                            return false;
                        float l = vp.getLeftX();
                        float b = vp.getBottomY();
                        float w = vp.getWidth();
                        float h = vp.getHeight();
                        int pc = event.getPointerCount();
                        for (int i = 0; i < pc; i++)
                            pointers[i].setPosition(l + (event.getX(i) / width) * w, b + (1 - (event.getY(i) / height)) * h);
                        return true;
                }
            }
        }

        private static final class Pointer {
            private final FractVec position;
            private boolean pressed;
            private boolean wasPressed;
            private boolean moved;

            private Pointer() {
                position = new FractVec();
            }

            private void cancel() {
                pressed = wasPressed = moved = false;
            }

            private void proceed() {
                wasPressed = pressed;
                moved = false;
            }

            private void press(float x, float y) {
                pressed = true;
                position.set(x, y);
            }

            private void release(float x, float y) {
                pressed = false;
                position.set(x, y);
            }

            private void setPosition(float x, float y) {
                if (pressed && !position.equals(x, y)) {
                    position.set(x, y);
                    moved = true;
                }
            }
        }
    }

    private final class Surface extends GLSurfaceView {

        private final boolean halfBits;
        private final InputHandler inputHandler;

        private Surface(Context context, final boolean halfColor, final boolean alpha) {
            super(context);
            final boolean[] halfBitsPointer = new boolean[1];
            setEGLConfigChooser(new EGLConfigChooser() {
                @Override
                public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                    int[] numConfigs = new int[1];
                    egl.eglGetConfigs(display, null, 0, numConfigs);
                    EGLConfig[] configs = new EGLConfig[numConfigs[0]];
                    int minRGBA = halfColor ? 4 : 8;
                    int attribs[] = new int[]{
                            EGL10.EGL_RED_SIZE, minRGBA,
                            EGL10.EGL_GREEN_SIZE, minRGBA,
                            EGL10.EGL_BLUE_SIZE, minRGBA,
                            EGL10.EGL_ALPHA_SIZE, alpha ? minRGBA : 0,
                            EGL10.EGL_NONE
                    };
                    if (egl.eglChooseConfig(display, attribs, configs, configs.length, numConfigs)) {
                        int configCount = numConfigs[0];
                        EGLMatch c = new EGLMatch(egl, display, configs[0], alpha);
                        for (int i = 1; i < configCount; i++) {
                            EGLMatch e = new EGLMatch(egl, display, configs[i], alpha);
                            if (e.cost < c.cost)
                                c = e;
                        }
                        halfBitsPointer[0] = c.halfBitsRGB;
                        return c.config;
                    }
                    throw new RuntimeException("Unsupported surface configuration");
                }
            });
            halfBits = halfBitsPointer[0];
            setEGLContextClientVersion(2);
            setRenderer(new Renderer());
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            inputHandler = new InputHandler();
            setOnTouchListener(inputHandler);
        }

        private final class Renderer implements GLSurfaceView.Renderer {

            private final Drawer drawer;
            private long time;
            private int width, height;

            Renderer() {
                drawer = hasMask() ? new MaskDrawer() : new Drawer();
            }

            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                reloadResources();
                batch.create();
                time = System.nanoTime();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int width, int height) {
                this.width = width;
                this.height = height;
                if (hasMask())
                    masker.create(width, height);
                inputHandler.setSize(width, height);
                currentScreen.resize(width, height);
                time = System.nanoTime();
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                if (nextScreen != null) {
                    currentScreen.hide();
                    currentScreen = nextScreen;
                    nextScreen = null;
                    inputHandler.setScreen(currentScreen);
                    currentScreen.set(width, height);
                }
                inputHandler.fire();
                FractColor.RGB c = currentScreen.clearColor;
                GLES20.glClearColor(c.r, c.g, c.b, c.a);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                long actualTime = System.nanoTime();
                float deltaTime = (float) (actualTime - time) * FractMath.NANO_TO_SECONDS;
                drawer.valid = true;
                currentScreen.render(drawer, deltaTime);
                drawer.valid = false;
                batch.flush();
                time = actualTime;
            }
        }


    }

    public final class MaskDrawer extends Drawer {

        private MaskDrawer() {
        }

        public void drawMasked(FractMaskCallback maskDrawer, boolean inverted) {
            validate();
            valid = false;
            masker.draw(maskDrawer, inverted);
            valid = true;
        }
    }

    public class Drawer {

        boolean valid;

        Drawer() {
        }


        public final void draw(FractSprite sprite) {
            validate();
            FractResources.Drawable drawable = sprite.pullCache(resources);
            if (drawable == null) {
                drawable = resources.getDrawable(sprite.drawableKey);
                sprite.pushCache(drawable);
            }
            batch.draw(drawable, currentScreen.viewport, sprite.color, sprite.transform, sprite.sizing, sprite.horizontalOrigin, sprite.verticalOrigin);
        }

        public final void draw(String drawableKey, FractColor color, FractTransform transform, FractSizing sizing, FractOrigin horizontalOrigin, FractOrigin verticalOrigin) {
            validate();
            batch.draw(resources.getDrawable(drawableKey), currentScreen.viewport, color, transform, sizing, horizontalOrigin, verticalOrigin);
        }

        final void validate() {
            if (valid) return;
            throw new RuntimeException("Cannot draw outside method");
        }

    }

}
