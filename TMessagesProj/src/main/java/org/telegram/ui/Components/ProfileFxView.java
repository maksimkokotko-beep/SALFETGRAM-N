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
 * VibeGram: "NFT", блики и северное сияние на фотографии профиля.
 * Всё статично и привязано к области фото. В режиме редактора слой можно
 * таскать пальцем и масштабировать пинчем.
 */
public class ProfileFxView extends View {

    private final Paint auroraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix glareMatrix = new Matrix();
    private final RectF glareRect = new RectF();

    private final int[] auroraColors = new int[]{
            0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFF6366F1
    };

    private static final float[][] ORB_SPOTS = new float[][]{
            {0.13f, 0.18f, 6.0f, 0.9f},
            {0.86f, 0.12f, 4.5f, 0.75f},
            {0.92f, 0.34f, 6.5f, 1.0f},
            {0.08f, 0.55f, 5.0f, 0.7f},
            {0.78f, 0.72f, 4.0f, 0.6f},
            {0.23f, 0.84f, 6.0f, 0.85f},
            {0.62f, 0.93f, 4.5f, 0.65f},
            {0.42f, 0.07f, 5.0f, 0.8f},
            {0.55f, 0.48f, 3.5f, 0.5f},
            {0.05f, 0.88f, 4.0f, 0.6f},
            {0.95f, 0.62f, 5.5f, 0.8f},
            {0.36f, 0.63f, 3.5f, 0.45f}
    };

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

        orbRimPaint.setStyle(Paint.Style.STROKE);
        orbRimPaint.setStrokeWidth(AndroidUtilities.dp(1f));
        orbRimPaint.setColor(0xCCFFFFFF);

        orbFillPaint.setColor(0x33FFFFFF);
        orbLightPaint.setColor(0xE6FFFFFF);

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
        final float baseDx = VibeGramStyle.fxDx();
        final float baseDy = VibeGramStyle.fxDy();
        final float ndx = Math.max(0f, Math.min(1f, (baseDx + dragDx) / 300f + 0.5f));
        final float ndy = Math.max(0f, Math.min(1f, (baseDy + dragDy) / 300f + 0.5f));
        VibeGramStyle.setFloat(VibeGramStyle.FX_DX, ndx);
        VibeGramStyle.setFloat(VibeGramStyle.FX_DY, ndy);
        VibeGramStyle.setInt(VibeGramStyle.FX_SCALE, Math.round(100 * pinchScale));
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
        canvas.translate(VibeGramStyle.fxDx() * AndroidUtilities.density + dragDx, VibeGramStyle.fxDy() * AndroidUtilities.density + dragDy);
        canvas.scale(pinchScale * VibeGramStyle.fxScale() / 100f, pinchScale * VibeGramStyle.fxScale() / 100f, w / 2f, size / 2f);

        final float cx = w / 2f;
        final float cy = size / 2f;
        final float radius = size / 2f;

        final float auroraStrength = VibeGramStyle.fxAurora() / 100f;
        final float glareStrength = VibeGramStyle.fxGlare() / 100f;

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
            final float rr = radius * blob[2];
            final int base = auroraColors[(int) blob[3]];
            final int centerColor = Color.argb(Math.round(46 * auroraStrength), Color.red(base), Color.green(base), Color.blue(base));
            auroraPaint.setShader(new RadialGradient(px, py, rr, centerColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, rr, auroraPaint);
        }
        auroraPaint.setShader(null);

        // Диагональный блик на весь фон аватара.
        final float glareWidth = size * 1.35f;
        final float glareHeight = size * 2.6f;
        glareRect.set(cx - glareWidth / 2f, cy - glareHeight / 2f, cx + glareWidth / 2f, cy + glareHeight / 2f);
        glarePaint.setShader(new LinearGradient(0, glareRect.top, 0, glareRect.bottom,
                new int[]{Color.TRANSPARENT, Color.argb(10, 255, 255, 255), Color.argb(Math.round(88 * glareStrength), 255, 255, 255), Color.argb(10, 255, 255, 255), Color.TRANSPARENT},
                new float[]{0f, 0.32f, 0.5f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        glareMatrix.reset();
        glareMatrix.preRotate(-32f, cx, cy);
        glarePaint.getShader().setLocalMatrix(glareMatrix);
        canvas.drawRect(glareRect, glarePaint);
        glarePaint.setShader(null);

        // Стеклянные шарики-"NFT" вокруг аватара, на своих местах.
        final int orbs = Math.max(0, Math.min(ORB_SPOTS.length, VibeGramStyle.fxOrbs()));
        for (int i = 0; i < orbs; i++) {
            final float[] orb = ORB_SPOTS[i];
            final float ox = w * orb[0];
            final float oy = size * orb[1];
            final float orbR = AndroidUtilities.dp(orb[2]);
            final float strength = orb[3];

            orbFillPaint.setAlpha((int) (60 * strength));
            canvas.drawCircle(ox, oy, orbR, orbFillPaint);

            orbRimPaint.setAlpha((int) (200 * strength));
            canvas.drawCircle(ox, oy, orbR - AndroidUtilities.dp(0.5f), orbRimPaint);

            orbLightPaint.setAlpha((int) (235 * strength));
            canvas.drawCircle(ox - orbR * 0.32f, oy - orbR * 0.34f, orbR * 0.24f, orbLightPaint);
        }

        canvas.restore();

        if (VibeGramStyle.editor()) {
            canvas.drawRect(2, 2, w - 2, size - 2, editorPaint);
        }
    }
}