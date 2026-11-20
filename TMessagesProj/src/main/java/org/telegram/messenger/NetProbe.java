package org.telegram.messenger;

import android.content.Context;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * VIBEGRAM diagnostics: identifies which network path the app actually uses
 * (VPN tunnel or direct ISP), whether Telegram is reachable from it, and
 * whether an MTProto request completes.
 */
public final class NetProbe {

    private static final String[] DCS = {
            "149.154.167.51",  // DC2
            "149.154.175.53",  // DC4 mirror
            "91.108.56.130"    // DC5
    };

    private NetProbe() {
    }

    private static int rpcLines;

    /** request-level trace for login debugging (bounded so it never floods) */
    public static synchronized void rpc(String line) {
        if (rpcLines++ > 200) {
            return;
        }
        CrashLogWriter.log(line);
    }

    public static void start(final Context ctx) {
        new Thread("sg-netprobe") {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignore) {
                }
                try {
                    probe(ctx);
                } catch (Throwable t) {
                    CrashLogWriter.log("NET probe crashed: " + t);
                }
            }
        }.start();
    }

    private static void probe(Context ctx) {
        CrashLogWriter.log("NET start api_id=" + BuildVars.APP_ID + " native=" + NativeLoader.loaded());

        try {
            List<NetworkInterface> ifs = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : ifs) {
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a.isLoopbackAddress()) {
                        continue;
                    }
                    sb.append(a.getHostAddress()).append(' ');
                }
                if (sb.length() > 0) {
                    CrashLogWriter.log("NET if=" + ni.getName() + " " + sb);
                }
            }
        } catch (Throwable t) {
            CrashLogWriter.log("NET if err " + t);
        }

        CrashLogWriter.log("NET ip " + http("https://api.ipify.org"));
        CrashLogWriter.log("NET example " + http("https://example.com"));
        CrashLogWriter.log("NET tgorg " + http("https://telegram.org"));

        try {
            long t0 = System.currentTimeMillis();
            InetAddress a = InetAddress.getByName("api.telegram.org");
            CrashLogWriter.log("NET dns ok " + a.getHostAddress() + " " + (System.currentTimeMillis() - t0) + "ms");
        } catch (Throwable t) {
            CrashLogWriter.log("NET dns FAIL " + t);
        }

        for (String ip : DCS) {
            Socket s = new Socket();
            long t0 = System.currentTimeMillis();
            try {
                s.connect(new InetSocketAddress(ip, 443), 3000);
                CrashLogWriter.log("NET tcp " + ip + " connect ok " + (System.currentTimeMillis() - t0) + "ms");
            } catch (Throwable t) {
                CrashLogWriter.log("NET tcp " + ip + " connect FAIL " + (System.currentTimeMillis() - t0) + "ms " + t);
            } finally {
                try {
                    s.close();
                } catch (Throwable ignore) {
                }
            }
        }

        try {
            CrashLogWriter.log("NET proxy=" + SharedConfig.isProxyEnabled()
                    + " " + (SharedConfig.currentProxy == null ? "none" : SharedConfig.currentProxy.settings.getType()
                    + " " + SharedConfig.currentProxy.settings.getAddress() + ":" + SharedConfig.currentProxy.settings.getPort())
                    + " pool=" + SharedConfig.proxyList.size()
                    + " rotation=" + SharedConfig.proxyRotationEnabled);
        } catch (Throwable t) {
            CrashLogWriter.log("NET proxy err " + t);
        }

        CrashLogWriter.log("NET iphttps " + http("https://1.1.1.1/cdn-cgi/trace"));
        CrashLogWriter.log("NET dc443 " + rawTls("149.154.167.51", 443));
        CrashLogWriter.log("NET dc80 " + rawTls("149.154.167.51", 80));

        try {
            ConnectionsManager cm = ConnectionsManager.getInstance(0);
            CrashLogWriter.log("NET state0=" + cm.getConnectionState() + " dc=" + cm.getCurrentDatacenterId());
        } catch (Throwable t) {
            CrashLogWriter.log("NET state0 err " + t);
        }

        try {
            TLRPC.TL_help_getConfig req = new TLRPC.TL_help_getConfig();
            ConnectionsManager.getInstance(0).sendRequest(req, (response, error) -> {
                if (error != null) {
                    CrashLogWriter.log("NET rpc ERR " + error.code + " " + error.text);
                } else {
                    CrashLogWriter.log("NET rpc OK " + (response == null ? "null" : response.getClass().getSimpleName()));
                }
            });
            CrashLogWriter.log("NET rpc sent");
        } catch (Throwable t) {
            CrashLogWriter.log("NET rpc send err " + t);
        }

        int last = -1;
        for (int i = 0; i < 36; i++) {
            try {
                ConnectionsManager cm = ConnectionsManager.getInstance(0);
                int st = cm.getConnectionState();
                if (st != last) {
                    last = st;
                    CrashLogWriter.log("NET state=" + st + " dc=" + cm.getCurrentDatacenterId() + " t=" + (i * 5) + "s");
                } else if (i % 12 == 11) {
                    CrashLogWriter.log("NET alive state=" + st + " t=" + (i * 5) + "s");
                }
            } catch (Throwable t) {
                CrashLogWriter.log("NET state err " + t);
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
        }
        CrashLogWriter.log("NET done");
    }

    private static String rawTls(String ip, int port) {
        java.net.Socket plain = null;
        javax.net.ssl.SSLSocket ssl = null;
        long t0 = System.currentTimeMillis();
        try {
            plain = new java.net.Socket();
            plain.connect(new InetSocketAddress(ip, port), 4000);
            plain.setSoTimeout(6000);
            javax.net.ssl.SSLSocketFactory f = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
            ssl = (javax.net.ssl.SSLSocket) f.createSocket(plain, ip, port, true);
            ssl.setSoTimeout(6000);
            ssl.startHandshake();
            return "tls ok " + (System.currentTimeMillis() - t0) + "ms";
        } catch (Throwable t) {
            return "ERR " + (System.currentTimeMillis() - t0) + "ms " + t;
        } finally {
            try {
                if (ssl != null) {
                    ssl.close();
                }
            } catch (Throwable ignore) {
            }
            try {
                if (plain != null) {
                    plain.close();
                }
            } catch (Throwable ignore) {
            }
        }
    }

    private static String http(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestProperty("User-Agent", "SalfetGram-probe");
            int code = c.getResponseCode();
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                int n = 0;
                while ((line = r.readLine()) != null && n < 2) {
                    sb.append(line.trim()).append(' ');
                    n++;
                }
                r.close();
            }
            String body = sb.toString().trim();
            if (body.length() > 60) {
                body = body.substring(0, 60);
            }
            return code + " " + body;
        } catch (Throwable t) {
            return "ERR " + t;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
    }
}