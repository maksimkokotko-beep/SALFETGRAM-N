package xyz.nextalone.nagram;

import android.content.SharedPreferences;

import org.telegram.messenger.MessagesController;

/**
 * Per feature switches for the VibeGram animations. Everything is on by
 * default, the master switch turns the whole set off in one tap and every
 * single animation can be toggled on its own.
 */
public final class VibeGramAnims {

    public static final String KEY_MASTER = "vibegram_anim_master";
    public static final String KEY_CHAT_LIST = "vibegram_anim_chat_list";
    public static final String KEY_REACTIONS = "vibegram_anim_reactions";
    public static final String KEY_SMOOTH_SCROLL = "vibegram_anim_smooth_scroll";
    public static final String KEY_BULLETINS = "vibegram_anim_bulletins";
    public static final String KEY_PROFILE = "vibegram_anim_profile";

    private VibeGramAnims() {
    }

    public static boolean isEnabled(String key) {
        final SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        return preferences.getBoolean(KEY_MASTER, true) && preferences.getBoolean(key, true);
    }

    public static boolean isOn(String key) {
        return MessagesController.getGlobalMainSettings().getBoolean(key, true);
    }

    public static void setOn(String key, boolean value) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(key, value).apply();
    }

    public static void toggle(String key) {
        setOn(key, !isOn(key));
    }
}