package xyz.nextalone.nagram;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Renders the VibeGram mark into a normal PNG so it can be used as a profile
 * picture or wallpaper. The launcher icon itself is a vector, so there is no
 * ready made bitmap in the APK to copy: it is drawn here once and dropped into
 * the gallery (Pictures/VibeGram).
 */
public final class VibeGramAssets {

    private static final String PREF_EXPORTED = "vibegram_icon_exported";
    private static final int SIZE = 1024;

    private VibeGramAssets() {
    }

    public static void exportIconOnce(Context context) {
        if (context == null) {
            return;
        }
        try {
            final SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            if (preferences.getBoolean(PREF_EXPORTED, false)) {
                return;
            }
            final Bitmap bitmap = renderIcon(context);
            if (bitmap == null) {
                return;
            }
            final boolean saved = Build.VERSION.SDK_INT >= 29
                ? saveToGallery(context, bitmap)
                : saveToAppPictures(context, bitmap);
            if (saved) {
                preferences.edit().putBoolean(PREF_EXPORTED, true).apply();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static Bitmap renderIcon(Context context) {
        final Drawable background = ContextCompat.getDrawable(context, R.drawable.sg_icon_bg);
        final Drawable foreground = ContextCompat.getDrawable(context, R.drawable.sg_icon_fg);
        if (background == null || foreground == null) {
            return null;
        }
        final Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        // Adaptive icons are drawn on a 108 unit grid, but launchers only show
        // the inner 72 units. Zoom in the same way so the gallery copy looks
        // exactly like the icon on the home screen instead of having a margin.
        final float zoom = 108f / 72f;
        canvas.scale(zoom, zoom, SIZE / 2f, SIZE / 2f);
        background.setBounds(0, 0, SIZE, SIZE);
        background.draw(canvas);
        foreground.setBounds(0, 0, SIZE, SIZE);
        foreground.draw(canvas);
        return bitmap;
    }

    private static boolean saveToGallery(Context context, Bitmap bitmap) {
        final ContentResolver resolver = context.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "VibeGram-icon.png");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VibeGram");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        final Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return false;
        }
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                return false;
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
        final ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(uri, done, null, null);
        return true;
    }

    private static boolean saveToAppPictures(Context context, Bitmap bitmap) {
        final File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "VibeGram");
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        final File file = new File(dir, "VibeGram-icon.png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return true;
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }
}