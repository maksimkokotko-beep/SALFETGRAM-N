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

/**
 * VibeGram: каждый чат в списке — стеклянная карточка с очертанием.
 * Рисуется поверх ячеек, но очень прозрачно, чтобы не мешать тексту.
 */
public class ChatListGlassDecoration extends RecyclerView.ItemDecoration {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Rect bounds = new Rect();

    private final float insetH = AndroidUtilities.dp(6);
    private final float insetV = AndroidUtilities.dp(3);
    private final float radius = AndroidUtilities.dp(18);

    private boolean enabled() {
        return VibeGramAnims.isEnabled(VibeGramAnims.KEY_CHAT_LIST);
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!enabled()) {
            return;
        }
        final boolean dark = Theme.isCurrentThemeDark();
        final int fillTop = dark ? Color.argb(20, 255, 255, 255) : Color.argb(90, 255, 255, 255);
        final int fillBottom = dark ? Color.argb(8, 255, 255, 255) : Color.argb(40, 255, 255, 255);
        final int rimColor = dark ? Color.argb(38, 255, 255, 255) : Color.argb(110, 255, 255, 255);
        final int topColor = dark ? Color.argb(90, 255, 255, 255) : Color.argb(170, 255, 255, 255);

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(AndroidUtilities.dp(1f));
        rimPaint.setColor(rimColor);

        topPaint.setStyle(Paint.Style.STROKE);
        topPaint.setStrokeWidth(AndroidUtilities.dp(1.2f));
        topPaint.setColor(topColor);

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

            canvas.drawRoundRect(rect, radius, radius, rimPaint);

            // Верхняя световая кромка, как у стекла.
            canvas.save();
            canvas.clipRect(rect.left, rect.top, rect.right, rect.top + radius);
            canvas.drawRoundRect(rect, radius, radius, topPaint);
            canvas.restore();
        }
    }
}