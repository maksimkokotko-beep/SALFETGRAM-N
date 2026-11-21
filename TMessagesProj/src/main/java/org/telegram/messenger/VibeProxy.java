package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * VibeGram: встроенный MTProto-прокси.
 *
 * В клиент зашит набор прокси (fake-TLS, порт 8443) и он включён по
 * умолчанию: при первом запуске список ставится в настройки, выбирается
 * первый сервер, включается прокси и авто-переключение
 * ({@link ProxyRotationController}). Если сервер не отвечает, клиент сам
 * переходит на следующий живой из списка.
 *
 * Раз в несколько часов список подтягивается из открытого источника
 * (https://tgmtproxy.github.io/mtproxy), чтобы мёртвые серверы заменялись
 * новыми. Обновление — best effort: без интернета или при блокировке
 * источника остаётся зашитый набор.
 *
 * Прокси не может «работать всегда и на любом интернете»: любой публичный
 * сервер можно заблокировать или перегрузить. Для 100% гарантии нужен
 * собственный сервер.
 */
public final class VibeProxy {

    private static final String PREFS = "vg_proxy";
    private static final int POOL_VERSION = 3;

    private static final String SOURCE = "https://tgmtproxy.github.io/mtproxy/proxies.txt";
    private static final long REFRESH_INTERVAL = 6 * 60 * 60 * 1000L;
    private static final long REFRESH_RETRY = 30 * 60 * 1000L;
    private static final int MAX_POOL = 60;

    private static final String SECRET_TROVICH = "ee7809adb838f02953d8f53df91d2e4b35706574726f766963682e7275";
    private static final String SECRET_PINGER = "ee161a50ae8c2609a3df6daa30ca2dfb9f786170692e6f7a6f6e2e7275";
    private static final String SECRET_GOOGLE = "eed8e23b55798e53b64877d42e6ef94a3f64726976652e676f6f676c652e636f6d";

    private static final String[] HOSTS_TROVICH = {
            "haven", "phoenix", "pine", "saturn", "amber", "galaxy", "spark",
            "lotus", "reef", "zenith", "astra", "glow", "quasar"
    };
    private static final String[] HOSTS_PINGER = {
            "summit", "noble", "blaze", "violet", "rocket", "mint", "apollo",
            "lark", "birch", "aspen", "horizon", "eagle", "crest", "zephyr"
    };
    private static final String[] HOSTS_GOOGLE = {
            "vista", "bolt", "echo", "beacon", "planet", "iris", "polar",
            "crystal", "ivory", "laser", "harbor", "karma", "ember"
    };

    private VibeProxy() {
    }

    // ---------------------------------------------------------------- install

    public static void install(final Context ctx) {
        if (ctx == null) {
            return;
        }
        final SharedPreferences p = prefs(ctx);
        AndroidUtilities.runOnUIThread(() -> {
            try {
                applyPool(p);
            } catch (Throwable t) {
                log("apply failed " + t);
            }
        });
        startRefresh(p, false, null);
    }

    private static void applyPool(SharedPreferences p) {
        if (!p.getBoolean("enabled", true)) {
            return;
        }
        SharedConfig.loadProxyList();

        if (p.getInt("version", 0) < POOL_VERSION) {
            final boolean userActive = SharedConfig.currentProxy != null && SharedConfig.isProxyEnabled();
            int added = 0;
            for (String url : bakedPool()) {
                final SharedConfig.ProxyInfo info = parse(url);
                if (info == null || contains(info)) {
                    continue;
                }
                SharedConfig.proxyList.add(0, info);
                added++;
            }
            if (added > 0) {
                SharedConfig.saveProxyList();
            }
            if (!userActive && !SharedConfig.proxyList.isEmpty()) {
                activate(SharedConfig.proxyList.get(0));
            }
            p.edit().putInt("version", POOL_VERSION).putBoolean("enabled", true).apply();
            log("baked pool installed +" + added + " total=" + SharedConfig.proxyList.size());
        }

        // Авто-переключение на живой сервер при сбое текущего.
        if (!SharedConfig.proxyRotationEnabled) {
            SharedConfig.proxyRotationEnabled = true;
            try {
                ctx().getSharedPreferences("userconfing", Context.MODE_PRIVATE).edit()
                        .putBoolean("proxyRotationEnabled", true).apply();
            } catch (Throwable ignore) {
            }
        }

        ensureActive();
    }

    private static void ensureActive() {
        if (SharedConfig.proxyList.isEmpty()) {
            return;
        }
        if (SharedConfig.currentProxy != null && SharedConfig.isProxyEnabled()) {
            return;
        }
        activate(SharedConfig.proxyList.get(0));
    }

    private static void activate(SharedConfig.ProxyInfo info) {
        if (info == null) {
            return;
        }
        SharedConfig.setCurrentProxy(info);
        final SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        info.settings.toSharedPreferences(editor);
        editor.putBoolean("proxy_enabled", true);
        editor.apply();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        log("activated " + info.settings.getAddress() + ":" + info.settings.getPort());
    }

    // ----------------------------------------------------------------- settings

    public static boolean isEnabled(Context ctx) {
        return prefs(ctx).getBoolean("enabled", true);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean("enabled", enabled).apply();
        if (enabled) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    applyPool(prefs(ctx));
                } catch (Throwable t) {
                    log("enable failed " + t);
                }
            });
            startRefresh(prefs(ctx), false, null);
        } else {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    SharedConfig.setProxyEnable(false);
                } catch (Throwable ignore) {
                }
            });
        }
    }

    public static String status(Context ctx) {
        try {
            SharedConfig.loadProxyList();
            if (!isEnabled(ctx)) {
                return "выключен";
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("включён · ").append(SharedConfig.proxyList.size()).append(" серверов");
            final SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
            if (current != null && current.settings != null) {
                sb.append(" · ").append(current.settings.getAddress())
                        .append(':').append(current.settings.getPort());
            }
            return sb.toString();
        } catch (Throwable t) {
            return "нет данных";
        }
    }

    /** Ручное обновление списка из настроек. */
    public static void refreshNow(Context ctx, Utilities.Callback<String> done) {
        startRefresh(prefs(ctx), true, done);
    }

    // ------------------------------------------------------------------ refresh

    private static void startRefresh(final SharedPreferences p, final boolean force, final Utilities.Callback<String> done) {
        if (!force && !isDue(p)) {
            return;
        }
        Thread t = new Thread("vg-proxy-refresh") {
            @Override
            public void run() {
                List<String> urls = fetch(SOURCE);
                final long now = System.currentTimeMillis();
                if (urls.isEmpty()) {
                    p.edit().putLong("last_try", now).apply();
                    if (done != null) {
                        AndroidUtilities.runOnUIThread(() -> done.run("Не удалось обновить список"));
                    }
                    return;
                }
                final List<String> found = urls;
                AndroidUtilities.runOnUIThread(() -> {
                    int added = merge(found);
                    p.edit().putLong("last_try", now).putLong("last_refresh", now).apply();
                    if (done != null) {
                        done.run(added > 0
                                ? "Добавлено " + added + ", всего " + SharedConfig.proxyList.size()
                                : "Список актуален");
                    }
                });
            }
        };
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static boolean isDue(SharedPreferences p) {
        final long now = System.currentTimeMillis();
        final long lastRefresh = p.getLong("last_refresh", 0);
        if (now - lastRefresh < REFRESH_INTERVAL) {
            return false;
        }
        final long lastTry = p.getLong("last_try", 0);
        return now - lastTry >= REFRESH_RETRY;
    }

    private static int merge(List<String> urls) {
        int added = 0;
        try {
            SharedConfig.loadProxyList();
            for (String url : urls) {
                if (SharedConfig.proxyList.size() >= MAX_POOL) {
                    break;
                }
                final SharedConfig.ProxyInfo info = parse(url);
                if (info == null || contains(info)) {
                    continue;
                }
                SharedConfig.proxyList.add(0, info);
                added++;
            }
            if (added > 0) {
                SharedConfig.saveProxyList();
            }
            ensureActive();
            log("refresh +" + added + " total=" + SharedConfig.proxyList.size());
        } catch (Throwable t) {
            log("merge failed " + t);
        }
        return added;
    }

    private static List<String> fetch(String source) {
        final List<String> urls = new ArrayList<>();
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "VibeGram");
            connection.setRequestProperty("Cache-Control", "no-cache");
            if (connection.getResponseCode() != 200) {
                return urls;
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() < 12 || line.startsWith("#")) {
                    continue;
                }
                if (line.contains("t.me/proxy") || line.contains("telegram.me/proxy")
                        || line.contains("telegram.dog/proxy") || line.startsWith("tg://proxy")) {
                    urls.add(line);
                }
            }
        } catch (Throwable t) {
            log("fetch failed " + t);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable ignore) {
            }
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable ignore) {
            }
        }
        return urls;
    }

    // ------------------------------------------------------------------ helpers

    private static SharedConfig.ProxyInfo parse(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        try {
            final SharedConfig.ProxyInfo info = SharedConfig.ProxyInfo.fromUrl(url);
            if (info == null || info.settings == null || TextUtils.isEmpty(info.settings.getAddress())) {
                return null;
            }
            return info;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean contains(SharedConfig.ProxyInfo info) {
        for (SharedConfig.ProxyInfo existing : SharedConfig.proxyList) {
            if (existing != null && Objects.equals(existing.settings, info.settings)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> bakedPool() {
        final List<String> urls = new ArrayList<>();
        addAll(urls, HOSTS_TROVICH, SECRET_TROVICH);
        addAll(urls, HOSTS_PINGER, SECRET_PINGER);
        addAll(urls, HOSTS_GOOGLE, SECRET_GOOGLE);
        return urls;
    }

    private static void addAll(List<String> out, String[] hosts, String secret) {
        for (String host : hosts) {
            out.add("tg://proxy?server=" + host + ".tealbriar.xyz&port=8443&secret=" + secret);
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        final Context c = ctx != null ? ctx : ApplicationLoader.applicationContext;
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Context ctx() {
        return ApplicationLoader.applicationContext;
    }

    private static void log(String message) {
        try {
            CrashLogWriter.log("VIBEPROXY " + message);
        } catch (Throwable ignore) {
        }
    }
}