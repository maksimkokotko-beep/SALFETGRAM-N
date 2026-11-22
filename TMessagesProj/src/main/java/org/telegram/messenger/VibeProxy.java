package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VibeGram proxy cleanup.
 *
 * Отдельные сборки VibeGram ставили встроенный набор прокси. Пул убран,
 * поэтому класс теперь только один раз удаляет установленные им прокси
 * и выключает прокси в настройках. Обычные прокси, добавленные вручную,
 * при этом тоже очищаются — это осознанный полный откат.
 */
public final class VibeProxy {

    private static final int POOL_VERSION = 4;
    private static final String PREFS = "vg_proxy";

    private VibeProxy() {
    }

    public static void install(final Context ctx) {
        if (ctx == null) {
            return;
        }
        final SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final boolean hadPool = p.getInt("version", 0) > 0;
        if (p.getInt("version", 0) >= POOL_VERSION) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (hadPool) {
                    cleanup();
                }
                p.edit().putInt("version", POOL_VERSION)
                        .putBoolean("enabled", false)
                        .putLong("last_refresh", 0)
                        .apply();
            } catch (Throwable t) {
                CrashLogWriter.log("VIBEPROXY cleanup err " + t);
            }
        });
    }

    private static void cleanup() {
        SharedConfig.loadProxyList();
        SharedConfig.deleteAllProxy();
        SharedConfig.proxyRotationEnabled = false;
        try {
            ApplicationLoader.applicationContext
                    .getSharedPreferences("userconfing", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("proxyRotationEnabled", false)
                    .apply();
        } catch (Throwable ignore) {
        }
        CrashLogWriter.log("VIBEPROXY removed built-in pool, proxies left="
                + SharedConfig.proxyList.size());
    }
}