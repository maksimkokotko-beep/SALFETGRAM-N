package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

/**
 * VibeGram built-in proxy pool.
 *
 * Ships a list of public MTProto proxies so the app can reach Telegram on
 * networks where it is blocked, without any external VPN. The list is stored
 * as regular Telegram proxies, then the stock proxy rotation picks whichever
 * server actually answers.
 */
public final class VibeProxy {

    private static final int POOL_VERSION = 1;
    private static final String PREFS = "vg_proxy";

    private static final String[] POOL = {
        "103.102.229.210|443|ee06dfdbdf271bab18c6b606484c237384796b696d612e64617661792e636c69636b",
        "104.105.18.26|443|ee3a4c2b816be46f6a0decf43209106a307777772e636c6f7564666c6172652e636f6d",
        "108.181.122.255|443|ee1603010200010001fc030386e24c3add63646e2e79656b74616e65742e636f6d",
        "109.122.197.158|443|ee1603010200010001fc030386e24c3add626973636f7474692e79656b74616e65742e636f6d",
        "13.143.175.84|443|eea0cf003f6b75c55303f3698f24886ad96578616d706c652e636f6d",
        "132.145.20.133|443|eedaf457af4bf8a3eb93f9b546cea9505b7777772e6d6963726f736f66742e636f6d",
        "143.246.57.51|443|eed7b564f2e6798df5a7aa226b94769634636c6f7564666c6172652e636f6d",
        "147.45.41.22|443|eea65c7fc5a0d5669ba371a7ca88fc19496d2e6265626f6f2e7275",
        "150.241.65.135|443|ee678bdaf3464b2d9fdeaaad417d2260ed7275747562652e7275",
        "155.212.142.105|443|ee0dbf98440246f4b9228dd2b704256cad6f7a6f6e2e7275",
        "159.194.241.128|443|ee7836950bdff24a3e73923a7a4c26f9856f7a6f6e2e7275",
        "171.22.114.173|443|ee4ffcf6088fa708d2b11d752fc91ddc036d2e6265626f6f2e7275",
        "176.65.151.224|443|eedd0e75ad282a7f84e9600a03bd4638337777772e636c6f7564666c6172652e636f6d",
        "178.236.245.218|443|ee68ae8284982465dfcb198342d01cdedb6f7a6f6e2e7275",
        "185.86.145.78|443|ee050c8f82b0877cccd23a08c65f062b987275747562652e7275",
        "186.246.16.221|443|eec11798ab008831b474066c9e1ebf5c99617669746f2e7275",
        "191.44.108.160|443|eea339ca5cc76d3f7946a1356a63f0947d6b69636b2e636f6d",
        "193.227.241.168|443|eec92963b1219183c7ac3674e90d2f6e56636c6f75642e6d61696c2e7275",
        "193.32.8.47|443|eea65c7fc5a0d5669ba371a7ca88fc19496d2e6265626f6f2e7275",
        "194.117.64.10|443|ee1603010200010001fc030386e24c3add626973636f7474692e79656b74616e65742e636f6d",
        "195.123.6.88|8443|ee31bd764f90373e3d7ff81b3829b6c1ea7777772e636c6f7564666c6172652e636f6d",
        "2.26.254.220|443|ee05c6cef4c32559e2a8fdc779655298d77777772e626f6f6b696e672e636f6d",
        "212.33.198.29|443|ee000000000000000000000000000000097777772e636c6f7564666c6172652e636f6d",
        "213.21.254.53|443|eea339ca5cc76d3f7946a1356a63f0947d6b69636b2e636f6d",
    };

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
                    installInternal(ctx);
                } catch (Throwable t) {
                    CrashLogWriter.log("VIBEPROXY err " + t);
                }
            }
        };
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static void installInternal(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getInt("version", 0) >= POOL_VERSION) {
            CrashLogWriter.log("VIBEPROXY installed already, enabled=" + SharedConfig.isProxyEnabled());
            return;
        }
        SharedConfig.loadProxyList();
        SharedConfig.ProxyInfo first = null;
        int added = 0;
        for (String entry : POOL) {
            try {
                String[] parts = entry.split("\\|");
                if (parts.length < 3) {
                    continue;
                }
                String link = "tg://proxy?server=" + parts[0] + "&port=" + parts[1] + "&secret=" + parts[2];
                ProxySettings settings = ProxySettings.fromUri(Uri.parse(link));
                if (settings == null || !settings.isValid()) {
                    continue;
                }
                SharedConfig.ProxyInfo info = SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings));
                if (first == null) {
                    first = info;
                }
                added++;
            } catch (Throwable t) {
                CrashLogWriter.log("VIBEPROXY bad entry " + entry + ": " + t);
            }
        }
        if (first != null && SharedConfig.currentProxy == null) {
            SharedConfig.proxyRotationEnabled = true;
            SharedConfig.proxyRotationTimeout = ProxyRotationController.DEFAULT_TIMEOUT_INDEX;
            SharedConfig.saveConfig();
            android.content.SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
            first.settings.toSharedPreferences(editor);
            editor.putBoolean("proxy_enabled", true);
            editor.commit();
            SharedConfig.currentProxy = first;
            ConnectionsManager.setProxySettings(true, first.settings);
            CrashLogWriter.log("VIBEPROXY enabled with " + added + " servers, rotation on");
        } else {
            CrashLogWriter.log("VIBEPROXY loaded " + added + " servers, keeping user proxy=" + (SharedConfig.currentProxy != null));
        }
        p.edit().putInt("version", POOL_VERSION).apply();
    }
}
