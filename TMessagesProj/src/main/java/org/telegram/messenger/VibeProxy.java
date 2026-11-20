package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VibeGram proxy cleanup.
 *
 * Earlier builds shipped a pool of public MTProto proxies. The pool was
 * removed on request, so this class now only deletes those once-installed
 * proxies from user config, leaving the standard Telegram proxy settings
 * untouched for anyone who adds a proxy manually.
 */
public final class VibeProxy {

    private static final int POOL_VERSION = 2;
    private static final String PREFS = "vg_proxy";

    private VibeProxy() {
    }

    public static void install(final Context ctx) {
        if (ctx == null) {
            return;
        }
        Thread t = new Thread("vg-proxy") {
            @Override
            public void run() {
                try {
                    cleanupInternal(ctx);
                } catch (Throwable t) {
                    CrashLogWriter.log("VIBEPROXY cleanup err " + t);
                }
            }
        };
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static void cleanupInternal(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getInt("version", 0) >= POOL_VERSION) {
            return;
        }
        final boolean hadPool = p.getInt("version", 0) > 0;
        if (hadPool) {
            try {
                SharedConfig.loadProxyList();
                SharedConfig.deleteAllProxy();
                SharedConfig.proxyRotationEnabled = false;
                SharedConfig.saveConfig();
                CrashLogWriter.log("VIBEPROXY removed built-in pool, proxies left="
                        + SharedConfig.proxyList.size());
            } catch (Throwable t) {
                CrashLogWriter.log("VIBEPROXY remove failed " + t);
            }
        }
        p.edit().putInt("version", POOL_VERSION).apply();
    }
}