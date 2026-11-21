package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

import xyz.nextalone.nagram.VibeGramAnims;
import xyz.nextalone.nagram.VibeGramStyle;

/**
 * VibeGram: северное сияние и блик на фотографии профиля.
 * Всё статично и привязано к области фото. В режиме редактора слой можно
 * таскать пальцем и масштабировать пинчем.
 */
public class ProfileFxView extends View {

    private final Paint auroraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix glareMatrix = new Matrix();
    private final RectF glareRect = new RectF();

    private static final int[] THEME_AURORA = new int[]{
            0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFF6366F1
    };

    private final int[] auroraColors = new int[4];

    private float parentExpanded = -1f;

    // Живые значения, меняются редактором.
    private float dragDx, dragDy;
    private float pinchScale = 1f;
    private float lastX, lastY;

    private final ScaleGestureDetector scaleDetector;

    public ProfileFxView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        editorPaint.setStyle(Paint.Style.STROKE);
        editorPaint.setStrokeWidth(AndroidUtilities.dp(1.5f));
        editorPaint.setColor(0xCCFFD54F);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                pinchScale = Math.max(0.5f, Math.min(2f, pinchScale * detector.getScaleFactor()));
                invalidate();
                return true;
            }
        });

        setAlpha(1f);
    }

    public void setParentExpanded(float expanded) {
        if (parentExpanded != expanded) {
            parentExpanded = expanded;
            final float target = expanded <= 0f ? 1f : Math.max(0f, 1f - expanded * 1.6f);
            setAlpha(target);
            setVisibility(target <= 0.01f ? GONE : VISIBLE);
        }
    }

    private boolean isFxEnabled() {
        return VibeGramAnims.isEnabled(VibeGramAnims.KEY_PROFILE_FX);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!VibeGramStyle.editor()) {
            return super.onTouchEvent(event);
        }
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    dragDx += event.getX() - lastX;
                    dragDy += event.getY() - lastY;
                    lastX = event.getX();
                    lastY = event.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                persistTransform();
                return true;
        }
        return true;
    }

    private void persistTransform() {
        final float density = Math.max(1f, AndroidUtilities.density);
        final float baseDx = VibeGramStyle.fxDx();
        final float baseDy = VibeGramStyle.fxDy();
        final float ndx = Math.max(0f, Math.min(1f, (baseDx + dragDx / density) / 300f + 0.5f));
        final float ndy = Math.max(0f, Math.min(1f, (baseDy + dragDy / density) / 300f + 0.5f));
        VibeGramStyle.setFloat(VibeGramStyle.FX_DX, ndx);
        VibeGramStyle.setFloat(VibeGramStyle.FX_DY, ndy);
        VibeGramStyle.setInt(VibeGramStyle.FX_SCALE, Math.round(100 * pinchScale * VibeGramStyle.fxScale() / 100f));
        dragDx = dragDy = 0f;
        pinchScale = 1f;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // В редакторе таскаем только область фото, ниже — обычная прокрутка профиля.
        if (VibeGramStyle.editor() && event.getY() <= Math.min(getWidth(), getHeight()) + AndroidUtilities.dp(8)) {
            onTouchEvent(event);
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void updateAuroraColors() {
        final int accent = VibeGramStyle.customAccent();
        if (accent == 0) {
            System.arraycopy(THEME_AURORA, 0, auroraColors, 0, auroraColors.length);
            return;
        }
        final float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        auroraColors[0] = accent;
        auroraColors[1] = Color.HSVToColor(new float[]{(hsv[0] + 45f) % 360f, Math.min(1f, hsv[1] + 0.15f), Math.min(1f, hsv[2] + 0.05f)});
        auroraColors[2] = Color.HSVToColor(new float[]{(hsv[0] + 120f) % 360f, Math.min(1f, hsv[1] + 0.1f), Math.min(1f, hsv[2] + 0.05f)});
        auroraColors[3] = Color.HSVToColor(new float[]{(hsv[0] + 200f) % 360f, Math.min(1f, hsv[1] + 0.05f), Math.min(1f, hsv[2])});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isFxEnabled() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        final float w = getWidth();
        final float h = getHeight();
        final float size = Math.min(w, h);

        canvas.save();
        canvas.clipRect(0, 0, w, size);
        canvas.translate(
                VibeGramStyle.fxDx() * AndroidUtilities.density + dragDx,
                VibeGramStyle.fxDy() * AndroidUtilities.density + dragDy);
        final float totalScale = pinchScale * VibeGramStyle.fxScale() / 100f;
        canvas.scale(totalScale, totalScale, w / 2f, size / 2f);

        final float cx = w / 2f;
        final float cy = size / 2f;
        final float radius = size / 2f;

        final float auroraStrength = VibeGramStyle.fxAurora() / 100f;
        final float glareStrength = VibeGramStyle.fxGlare() / 100f;
        final float auroraScale = VibeGramStyle.fxAuroraScale() / 100f;

        updateAuroraColors();

        // Мягкое сияние по фото.
        final float[][] blobs = new float[][]{
                {0.18f, 0.22f, 0.95f, 0},
                {0.82f, 0.30f, 0.85f, 1},
                {0.30f, 0.86f, 0.80f, 2},
                {0.72f, 0.78f, 0.70f, 3}
        };
        for (float[] blob : blobs) {
            final float px = w * blob[0];
            final float py = size * blob[1];
            final float rr = radius * blob[2] * auroraScale;
            final int base = auroraColors[(int) blob[3]];
            final int centerColor = Color.argb(Math.round(46 * auroraStrength), Color.red(base), Color.green(base), Color.blue(base));
            auroraPaint.setShader(new RadialGradient(px, py, rr, centerColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, rr, auroraPaint);
        }
        auroraPaint.setShader(null);

        // Диагональный блик на весь фон аватара.
        final float glareWidth = size * 1.35f * VibeGramStyle.fxGlareWidth() / 100f;
        final float glareHeight = size * 2.6f;
        glareRect.set(cx - glareWidth / 2f, cy - glareHeight / 2f, cx + glareWidth / 2f, cy + glareHeight / 2f);
        glarePaint.setShader(new LinearGradient(0, glareRect.top, 0, glareRect.bottom,
                new int[]{Color.TRANSPARENT, Color.argb(10, 255, 255, 255), Color.argb(Math.round(88 * glareStrength), 255, 255, 255), Color.argb(10, 255, 255, 255), Color.TRANSPARENT},
                new float[]{0f, 0.32f, 0.5f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        glareMatrix.reset();
        glareMatrix.preRotate(VibeGramStyle.fxGlareAngle(), cx, cy);
        glarePaint.getShader().setLocalMatrix(glareMatrix);
        canvas.drawRect(glareRect, glarePaint);
        glarePaint.setShader(null);

        canvas.restore();

        if (VibeGramStyle.editor()) {
            canvas.drawRect(2, 2, w - 2, size - 2, editorPaint);
        }
    }
}