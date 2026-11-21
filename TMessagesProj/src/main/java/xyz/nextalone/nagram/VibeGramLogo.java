package xyz.nextalone.nagram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

/**
 * VibeGram wordmark.
 * <p>
 * The fork used to ship a hand drawn "Nagram" lettering as a vector. Instead of
 * redrawing letter forms by hand, the wordmark is rendered once from the real
 * app name, so every place that shows the brand (intro screen, chats title,
 * stories header, settings preview) always reads "VibeGram".
 * <p>
 * The bitmap is white, callers tint it with a ColorFilter, exactly like the old
 * drawable was used.
 */
public final class VibeGramLogo {

    private VibeGramLogo() {
    }

    public static Drawable wordmark(Context context, int widthDp, int heightDp) {
        final int width = Math.max(1, AndroidUtilities.dp(widthDp));
        final int height = Math.max(1, AndroidUtilities.dp(heightDp));

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(AndroidUtilities.bold());
        paint.setColor(Color.WHITE);

        final String text = context.getString(R.string.NekoX);

        float size = height * 0.86f;
        paint.setTextSize(size);
        float textWidth = paint.measureText(text);
        if (textWidth > width) {
            size = size * width / textWidth;
            paint.setTextSize(size);
            textWidth = paint.measureText(text);
        }

        final Paint.FontMetrics metrics = paint.getFontMetrics();
        final float y = height / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, (width - textWidth) / 2f, y, paint);

        final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        drawable.setBounds(0, 0, width, height);
        return drawable;
    }

    /**
     * Same wordmark, but as a plain Typeface so custom views can draw it directly.
     */
    public static Typeface typeface() {
        return AndroidUtilities.bold();
    }
}