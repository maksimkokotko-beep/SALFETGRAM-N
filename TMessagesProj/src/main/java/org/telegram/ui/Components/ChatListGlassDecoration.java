package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import xyz.nextalone.nagram.VibeGramAnims;
import xyz.nextalone.nagram.VibeGramStyle;

/**
 * VibeGram: каждый чат в списке — стеклянная карточка с очертанием.
 * Все размеры, прозрачности и радиус берутся из редактора VibeGram.
 */
public class ChatListGlassDecoration extends RecyclerView.ItemDecoration {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Rect bounds = new Rect();

    private boolean enabled() {
        return VibeGramAnims.isEnabled(VibeGramAnims.KEY_CHAT_LIST);
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!enabled()) {
            return;
        }
        final boolean dark = Theme.isCurrentThemeDark();
        final float radius = AndroidUtilities.dp(VibeGramStyle.chatRadius());
        final float insetH = AndroidUtilities.dp(VibeGramStyle.chatInsetH());
        final float insetV = AndroidUtilities.dp(VibeGramStyle.chatInsetV());
        final int fillAlpha = Math.round(255 * VibeGramStyle.chatFill() / 100f * (dark ? 0.45f : 2.2f));
        final int fillTop = Color.argb(Math.min(255, fillAlpha), 255, 255, 255);
        final int fillBottom = Color.argb(Math.min(255, Math.round(fillAlpha * 0.4f)), 255, 255, 255);
        final int rimColor = Color.argb(Math.min(255, Math.round(255 * VibeGramStyle.chatRim() / 100f)), 255, 255, 255);
        final int topColor = Color.argb(Math.min(255, Math.round(255 * VibeGramStyle.chatTop() / 100f)), 255, 255, 255);

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(AndroidUtilities.dp(VibeGramStyle.chatRimWidth()));
        rimPaint.setColor(rimColor);

        topPaint.setStyle(Paint.Style.STROKE);
        topPaint.setStrokeWidth(AndroidUtilities.dp(Math.max(0.8f, VibeGramStyle.chatRimWidth() + 0.2f)));
        topPaint.setColor(topColor);

        if (radius <= 0f) {
            return;
        }

        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            if (child.getHeight() < AndroidUtilities.dp(52)) {
                continue;
            }
            parent.getDecoratedBoundsWithMargins(child, bounds);
            rect.set(bounds.left + insetH, bounds.top + insetV, bounds.right - insetH, bounds.bottom - insetV);
            if (rect.width() <= 0 || rect.height() <= 0) {
                continue;
            }

            fillPaint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, fillTop, fillBottom, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, fillPaint);
            fillPaint.setShader(null);

            if (VibeGramStyle.chatRimWidth() > 0f) {
                canvas.drawRoundRect(rect, radius, radius, rimPaint);
            }

            // Верхняя световая кромка, как у стекла.
            canvas.save();
            canvas.clipRect(rect.left, rect.top, rect.right, rect.top + Math.min(radius, rect.height() / 2f));
            canvas.drawRoundRect(rect, radius, radius, topPaint);
            canvas.restore();
        }
    }
}