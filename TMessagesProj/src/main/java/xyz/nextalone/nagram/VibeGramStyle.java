package xyz.nextalone.nagram;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.MessagesController;

/**
 * VibeGram: значения кастомизации интерфейса. Всё хранится в общих настройках,
 * поэтому редактор, пресеты и экспорт работают без отдельной базы.
 */
public final class VibeGramStyle {

    // Карточки чатов
    public static final String CHAT_RADIUS = "vg_chat_radius";       // dp, 0..40
    public static final String CHAT_INSET_H = "vg_chat_inset_h";     // dp, 0..24
    public static final String CHAT_INSET_V = "vg_chat_inset_v";     // dp, 0..16
    public static final String CHAT_FILL = "vg_chat_fill";           // %, 0..100
    public static final String CHAT_RIM = "vg_chat_rim";             // %, 0..100
    public static final String CHAT_RIM_W = "vg_chat_rim_w";         // dp, 0..5
    public static final String CHAT_TOP = "vg_chat_top";             // %, 0..100
    public static final String CHAT_GLOW = "vg_chat_glow";           // %, 0..150
    public static final String CHAT_GLOW_COLOR = "vg_chat_glow_color"; // 0 = тема

    // FX аватара
    public static final String FX_AURORA = "vg_fx_aurora";           // %, 0..200
    public static final String FX_GLARE = "vg_fx_glare";             // %, 0..200
    public static final String FX_ORBS = "vg_fx_orbs";               // шт, 0..12
    public static final String FX_SCALE = "vg_fx_scale";             // %, 50..200
    public static final String FX_DX = "vg_fx_dx";                   // %, 0..1 → -150..150dp
    public static final String FX_DY = "vg_fx_dy";                   // %, 0..1 → -150..150dp

    // Кнопки профиля
    public static final String ACTIONS_SIZE = "vg_actions_size";     // dp, 30..72
    public static final String ACTIONS_ALPHA = "vg_actions_alpha";   // %, 10..100
    public static final String ACTIONS_DX = "vg_actions_dx";         // %, 0..1
    public static final String ACTIONS_DY = "vg_actions_dy";         // %, 0..1

    // Переходы
    public static final String TABS_SCALE = "vg_tabs_scale";         // %, 0..40
    public static final String TABS_DURATION = "vg_tabs_duration";   // ms, 200..900
    public static final String EDITOR = "vg_editor_mode";            // bool

    public static final String[] ALL = new String[]{
            CHAT_RADIUS, CHAT_INSET_H, CHAT_INSET_V, CHAT_FILL, CHAT_RIM, CHAT_RIM_W,
            CHAT_TOP, CHAT_GLOW, CHAT_GLOW_COLOR,
            FX_AURORA, FX_GLARE, FX_ORBS, FX_SCALE, FX_DX, FX_DY,
            ACTIONS_SIZE, ACTIONS_ALPHA, ACTIONS_DX, ACTIONS_DY,
            TABS_SCALE, TABS_DURATION
    };

    private VibeGramStyle() {
    }

    private static SharedPreferences prefs() {
        return MessagesController.getGlobalMainSettings();
    }

    public static float getFloat(String key, float def) {
        final Object value = prefs().getAll().get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return def;
    }

    public static int getInt(String key, int def) {
        final Object value = prefs().getAll().get(key);
        if (value instanceof Number) {
            return Math.round(((Number) value).floatValue());
        }
        return def;
    }

    public static void setFloat(String key, float value) {
        prefs().edit().putFloat(key, value).apply();
    }

    public static void setInt(String key, int value) {
        prefs().edit().putInt(key, value).apply();
    }

    /* ---------- готовые значения ---------- */

    public static float chatRadius() {
        return getFloat(CHAT_RADIUS, 18f);
    }

    public static float chatInsetH() {
        return getFloat(CHAT_INSET_H, 6f);
    }

    public static float chatInsetV() {
        return getFloat(CHAT_INSET_V, 3f);
    }

    public static int chatFill() {
        return getInt(CHAT_FILL, 18);
    }

    public static int chatRim() {
        return getInt(CHAT_RIM, 38);
    }

    public static float chatRimWidth() {
        return getFloat(CHAT_RIM_W, 1f);
    }

    public static int chatTop() {
        return getInt(CHAT_TOP, 90);
    }

    public static int chatGlow() {
        return getInt(CHAT_GLOW, 100);
    }

    public static int glowColor() {
        return getInt(CHAT_GLOW_COLOR, 0);
    }

    public static int fxAurora() {
        return getInt(FX_AURORA, 100);
    }

    public static int fxGlare() {
        return getInt(FX_GLARE, 100);
    }

    public static int fxOrbs() {
        return getInt(FX_ORBS, 7);
    }

    public static int fxScale() {
        return getInt(FX_SCALE, 100);
    }

    public static float fxDx() {
        return (getFloat(FX_DX, 0.5f) - 0.5f) * 300f;
    }

    public static float fxDy() {
        return (getFloat(FX_DY, 0.5f) - 0.5f) * 300f;
    }

    public static int actionsSize() {
        return getInt(ACTIONS_SIZE, 46);
    }

    public static int actionsAlpha() {
        return getInt(ACTIONS_ALPHA, 100);
    }

    public static float actionsDx() {
        return (getFloat(ACTIONS_DX, 0.5f) - 0.5f) * 300f;
    }

    public static float actionsDy() {
        return (getFloat(ACTIONS_DY, 0.5f) - 0.5f) * 300f;
    }

    public static int tabsScale() {
        return getInt(TABS_SCALE, 14);
    }

    public static int tabsDuration() {
        return getInt(TABS_DURATION, 540);
    }

    public static boolean editor() {
        return prefs().getBoolean(EDITOR, false);
    }

    public static void setEditor(boolean value) {
        prefs().edit().putBoolean(EDITOR, value).apply();
    }

    /* ---------- пресеты и экспорт ---------- */

    public static JSONObject toJson() {
        final JSONObject object = new JSONObject();
        final SharedPreferences p = prefs();
        for (String key : ALL) {
            try {
                object.put(key, p.getAll().get(key));
            } catch (JSONException ignore) {
            }
        }
        return object;
    }

    public static void fromJson(JSONObject object) {
        if (object == null) {
            return;
        }
        final SharedPreferences.Editor editor = prefs().edit();
        for (String key : ALL) {
            if (!object.has(key)) {
                continue;
            }
            final Object value = object.opt(key);
            if (value instanceof Number) {
                editor.putFloat(key, ((Number) value).floatValue());
            }
        }
        editor.apply();
    }

    public static void savePreset(int slot) {
        prefs().edit().putString("vg_preset_" + slot, toJson().toString()).apply();
    }

    public static boolean hasPreset(int slot) {
        return prefs().contains("vg_preset_" + slot);
    }

    public static void loadPreset(int slot) {
        final String raw = prefs().getString("vg_preset_" + slot, null);
        if (raw == null) {
            return;
        }
        try {
            fromJson(new JSONObject(raw));
        } catch (JSONException ignore) {
        }
    }

    public static String exportString() {
        return toJson().toString();
    }

    public static boolean importString(String raw) {
        try {
            fromJson(new JSONObject(raw));
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static void resetAll() {
        final SharedPreferences.Editor editor = prefs().edit();
        for (String key : ALL) {
            editor.remove(key);
        }
        editor.apply();
    }
}