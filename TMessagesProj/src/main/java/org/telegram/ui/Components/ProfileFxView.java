package org.telegram.ui.Components;

import android.animation.ValueAnimator;
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
import android.view.animation.LinearInterpolator;

import org.telegram.messenger.AndroidUtilities;

import xyz.nextalone.nagram.VibeGramAnims;

/**
 * VibeGram: "NFT", блики и северное сияние поверх фотографии профиля.
 * Слой лежит выше галереи аватара и ниже стеклянных кнопок действий.
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

    private ValueAnimator animator;
    private float time;

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
            if (target > 0.01f) {
                startAnimating();
            } else {
                stopAnimating();
            }
        }
    }

    private boolean isFxEnabled() {
        return VibeGramAnims.isEnabled(VibeGramAnims.KEY_PROFILE_FX);
    }

    private void startAnimating() {
        if (!isFxEnabled() || animator != null || getWindowVisibility() != VISIBLE) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(9000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            time = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopAnimating() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimating();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimating();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startAnimating();
        } else {
            stopAnimating();
        }
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
        final float phase = time * (float) (Math.PI * 2);

        // Мягкое сияние вокруг/под аватарой.
        for (int i = 0; i < auroraColors.length; i++) {
            final float angle = phase + i * (float) (Math.PI * 2 / auroraColors.length);
            final float px = cx + (float) Math.cos(angle) * radius * 0.55f;
            final float py = cy + (float) Math.sin(angle) * radius * 0.5f;
            final float rr = radius * (0.85f + 0.1f * (float) Math.sin(phase * 0.7f + i));
            final int base = auroraColors[i];
            final int centerColor = Color.argb(58, Color.red(base), Color.green(base), Color.blue(base));
            auroraPaint.setShader(new RadialGradient(px, py, rr, centerColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, rr, auroraPaint);
        }
        auroraPaint.setShader(null);

        // Диагональный блик на весь фон аватара.
        final float glareAngle = -28f + 6f * (float) Math.sin(phase * 0.5f);
        final float glareWidth = size * 1.35f;
        final float glareHeight = size * 2.6f;
        glareRect.set(cx - glareWidth / 2f, cy - glareHeight / 2f, cx + glareWidth / 2f, cy + glareHeight / 2f);
        final int glarePeak = Color.argb(66, 255, 255, 255);
        glarePaint.setShader(new LinearGradient(0, glareRect.top, 0, glareRect.bottom,
                new int[]{Color.TRANSPARENT, Color.argb(10, 255, 255, 255), glarePeak, Color.argb(10, 255, 255, 255), Color.TRANSPARENT},
                new float[]{0f, 0.32f, 0.5f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        glareMatrix.reset();
        glareMatrix.preRotate(glareAngle, cx, cy);
        glarePaint.getShader().setLocalMatrix(glareMatrix);
        canvas.save();
        canvas.clipRect(0, 0, w, size);
        canvas.drawRect(glareRect, glarePaint);
        canvas.restore();
        glarePaint.setShader(null);

        // Летающие стеклянные шарики-"NFT" вокруг аватара.
        final int orbs = 7;
        for (int i = 0; i < orbs; i++) {
            final float baseAngle = (float) (Math.PI * 2 * i / orbs);
            final float speed = i % 2 == 0 ? 1f : -0.75f;
            final float angle = baseAngle + phase * speed;
            final float orbit = radius * (1.02f + 0.06f * (float) Math.sin(phase * 2f + i * 1.7f));
            final float ox = cx + (float) Math.cos(angle) * orbit;
            final float oy = cy + (float) Math.sin(angle) * orbit * 0.92f;
            final float orbR = AndroidUtilities.dp(5.5f + (i % 3) * 2.6f);
            final float depth = 0.55f + 0.45f * (float) ((Math.sin(angle) + 1f) / 2f);

            orbFillPaint.setAlpha((int) (60 * depth));
            canvas.drawCircle(ox, oy, orbR, orbFillPaint);

            orbRimPaint.setAlpha((int) (200 * depth));
            canvas.drawCircle(ox, oy, orbR - AndroidUtilities.dp(0.5f), orbRimPaint);

            orbLightPaint.setAlpha((int) (235 * depth));
            canvas.drawCircle(ox - orbR * 0.32f, oy - orbR * 0.34f, orbR * 0.24f, orbLightPaint);
        }
    }
}