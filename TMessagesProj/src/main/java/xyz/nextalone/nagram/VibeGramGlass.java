package xyz.nextalone.nagram;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * VibeGram: «жидкое стекло». Один рисовальщик для карточек чатов, подарков
 * и панелей профиля, чтобы стекло везде выглядело одинаково.
 */
public final class VibeGramGlass {

    private static final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private VibeGramGlass() {
    }

    private static int blend(int from, int to, float amount) {
        final float t = Math.max(0f, Math.min(1f, amount));
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Стеклянная карточка: заливка, обводка в цвет акцента и блик по верхней кромке.
     *
     * @param strength 0..1 — насколько сильно проявить стекло (мелкие карточки меньше).
     */
    public static void drawCard(Canvas canvas, RectF rect, float radius, float strength) {
        if (rect == null || rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }
        final float s = Math.max(0f, Math.min(1f, strength));
        final boolean dark = Theme.isCurrentThemeDark();
        final int accent = VibeGramStyle.accentColor();
        final float tint = Math.max(0f, Math.min(1f, VibeGramStyle.chatTint() / 100f)) * s;

        final int base = blend(0xFFFFFFFF, accent, tint * 0.55f);
        final int fillAlpha = Math.round(255 * VibeGramStyle.chatFill() / 100f * (dark ? 0.45f : 1.4f) * s);
        final int topColor = withAlpha(base, Math.min(255, fillAlpha));
        final int bottomColor = withAlpha(base, Math.min(255, Math.round(fillAlpha * 0.35f)));

        fillPaint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
        fillPaint.setShader(null);

        final int rimAlpha = Math.round(255 * VibeGramStyle.chatRim() / 100f * s);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(AndroidUtilities.dp(Math.max(0.8f, VibeGramStyle.chatRimWidth())));
        rimPaint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                withAlpha(blend(0xFFFFFFFF, accent, tint * 0.9f), Math.min(255, Math.round(rimAlpha * 1.4f))),
                withAlpha(blend(0xFFFFFFFF, accent, tint * 0.5f), Math.round(rimAlpha * 0.35f)),
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, rimPaint);
        rimPaint.setShader(null);

        // Верхняя световая кромка — «толщина» стекла.
        canvas.save();
        canvas.clipRect(rect.left, rect.top, rect.right, rect.top + Math.max(AndroidUtilities.dp(10), rect.height() * 0.22f));
        glossPaint.setStyle(Paint.Style.STROKE);
        glossPaint.setStrokeWidth(AndroidUtilities.dpf2(1.4f));
        glossPaint.setColor(withAlpha(0xFFFFFFFF, Math.round(190 * s)));
        canvas.drawRoundRect(rect, radius, radius, glossPaint);
        canvas.restore();
    }

    /** Круглая стеклянная линза (для подарков и мелких иконок). */
    public static void drawCircle(Canvas canvas, float cx, float cy, float radius, float strength) {
        if (radius <= 0f) {
            return;
        }
        final float s = Math.max(0f, Math.min(1f, strength));
        final boolean dark = Theme.isCurrentThemeDark();
        final int accent = VibeGramStyle.accentColor();
        final float tint = Math.max(0f, Math.min(1f, VibeGramStyle.chatTint() / 100f)) * s;
        final int base = blend(0xFFFFFFFF, accent, tint * 0.55f);

        final int fillAlpha = Math.round(255 * VibeGramStyle.chatFill() / 100f * (dark ? 0.4f : 1.2f) * s);
        fillPaint.setShader(new RadialGradient(cx, cy - radius * 0.35f, radius * 1.15f,
                withAlpha(base, Math.min(255, fillAlpha)), withAlpha(base, Math.round(fillAlpha * 0.3f)), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, fillPaint);
        fillPaint.setShader(null);

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(AndroidUtilities.dpf2(1.1f));
        rimPaint.setShader(new LinearGradient(0, cy - radius, 0, cy + radius,
                withAlpha(blend(0xFFFFFFFF, accent, tint), Math.round(220 * s)),
                withAlpha(blend(0xFFFFFFFF, accent, tint), Math.round(40 * s)),
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, rimPaint);
        rimPaint.setShader(null);
    }
}