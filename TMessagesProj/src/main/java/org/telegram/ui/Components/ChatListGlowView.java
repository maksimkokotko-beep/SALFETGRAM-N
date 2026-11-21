package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import xyz.nextalone.nagram.VibeGramAnims;

/**
 * VibeGram: мягкое живое сияние поверх списка чатов. Слой не перехватывает
 * касания и не мешает читать список — просто добавляет цвета и движения.
 */
public class ChatListGlowView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator animator;
    private float time;

    public ChatListGlowView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private boolean isGlowOn() {
        return VibeGramAnims.isEnabled(VibeGramAnims.KEY_CHAT_GLOW);
    }

    private void startAnimating() {
        if (!isGlowOn() || animator != null || getWindowVisibility() != VISIBLE) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(14000);
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

    private int accent() {
        try {
            return Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        } catch (Exception e) {
            return 0xFF3B82F6;
        }
    }

    private int blend(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isGlowOn() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        final float w = getWidth();
        final float h = getHeight();
        final float phase = time * (float) (Math.PI * 2);
        final int accent = accent();

        // Верхнее сияние: пара мягких цветных пятен, медленно плавающих.
        final float topRadius = w * 0.85f;
        final float x1 = w * (0.22f + 0.12f * (float) Math.sin(phase));
        final float y1 = -topRadius * 0.35f + AndroidUtilities.dp(40) * (float) Math.cos(phase * 0.8f);
        paint.setShader(new RadialGradient(x1, y1, topRadius, blend(accent, 44), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(x1, y1, topRadius, paint);

        final int violet = 0xFF8B5CF6;
        final float x2 = w * (0.82f + 0.1f * (float) Math.cos(phase * 0.9f));
        final float y2 = -topRadius * 0.3f + AndroidUtilities.dp(30) * (float) Math.sin(phase * 1.1f);
        paint.setShader(new RadialGradient(x2, y2, topRadius * 0.8f, blend(violet, 34), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(x2, y2, topRadius * 0.8f, paint);

        // Снизу — второй очаг, чтобы экран не выглядел пустым у навигации.
        final float bottomRadius = w * 0.75f;
        final float x3 = w * (0.7f + 0.15f * (float) Math.sin(phase * 0.7f + 1.2f));
        final float y3 = h + bottomRadius * 0.55f;
        paint.setShader(new RadialGradient(x3, y3, bottomRadius, blend(0xFF22D3EE, 26), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(x3, y3, bottomRadius, paint);

        // Лёгкий белый блик, проходящий по верху списка.
        final float bandTop = AndroidUtilities.dp(90) + AndroidUtilities.dp(60) * (float) Math.sin(phase * 0.6f);
        paint.setShader(new LinearGradient(0, bandTop - AndroidUtilities.dp(120), 0, bandTop + AndroidUtilities.dp(120),
                new int[]{Color.TRANSPARENT, Color.argb(14, 255, 255, 255), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, AndroidUtilities.dp(300), paint);
        paint.setShader(null);
    }
}