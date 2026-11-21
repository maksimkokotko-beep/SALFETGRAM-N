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
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

import xyz.nextalone.nagram.VibeGramAnims;

/**
 * VibeGram: "NFT", блики и северное сияние на фотографии профиля.
 * Всё статично и привязано к области фото — двигается только вместе с аватарой.
 */
public class ProfileFxView extends View {

    private final Paint auroraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix glareMatrix = new Matrix();
    private final RectF glareRect = new RectF();

    private final int[] auroraColors = new int[]{
            0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFF6366F1
    };

    // Фиксированные позиции шариков: x, y (в долях от размера фото), радиус (dp), яркость.
    private static final float[][] ORBS = new float[][]{
            {0.13f, 0.18f, 6.0f, 0.9f},
            {0.86f, 0.12f, 4.5f, 0.75f},
            {0.92f, 0.34f, 6.5f, 1.0f},
            {0.08f, 0.55f, 5.0f, 0.7f},
            {0.78f, 0.72f, 4.0f, 0.6f},
            {0.23f, 0.84f, 6.0f, 0.85f},
            {0.62f, 0.93f, 4.5f, 0.65f}
    };

    private float parentExpanded = -1f;

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
    protected void onDraw(Canvas canvas) {
        if (!isFxEnabled() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        final float w = getWidth();
        final float h = getHeight();
        final float size = Math.min(w, h);
        final float cx = w / 2f;
        final float cy = size / 2f;
        final float radius = size / 2f;

        // Всё рисуем только в пределах фото.
        canvas.save();
        canvas.clipRect(0, 0, w, size);

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
            final int centerColor = Color.argb(46, Color.red(base), Color.green(base), Color.blue(base));
            auroraPaint.setShader(new RadialGradient(px, py, rr, centerColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, rr, auroraPaint);
        }
        auroraPaint.setShader(null);

        // Диагональный блик на весь фон аватара.
        final float glareWidth = size * 1.35f;
        final float glareHeight = size * 2.6f;
        glareRect.set(cx - glareWidth / 2f, cy - glareHeight / 2f, cx + glareWidth / 2f, cy + glareHeight / 2f);
        glarePaint.setShader(new LinearGradient(0, glareRect.top, 0, glareRect.bottom,
                new int[]{Color.TRANSPARENT, Color.argb(10, 255, 255, 255), Color.argb(88, 255, 255, 255), Color.argb(10, 255, 255, 255), Color.TRANSPARENT},
                new float[]{0f, 0.32f, 0.5f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        glareMatrix.reset();
        glareMatrix.preRotate(-32f, cx, cy);
        glarePaint.getShader().setLocalMatrix(glareMatrix);
        canvas.drawRect(glareRect, glarePaint);
        glarePaint.setShader(null);

        // Стеклянные шарики-"NFT" вокруг аватара, на своих местах.
        for (float[] orb : ORBS) {
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
    }
}