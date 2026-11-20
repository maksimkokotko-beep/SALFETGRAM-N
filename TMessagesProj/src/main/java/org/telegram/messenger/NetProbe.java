package org.telegram.messenger;

import android.content.Context;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * SALFETGRAM diagnostics: checks from inside the app whether Telegram servers
 * are reachable, and reports the result over the diag channel.
 */
public final class NetProbe {

    private static final String[] DCS = {
            "149.154.167.51", // DC4
            "149.154.175.53", // DC2
            "91.108.56.130",  // DC5
    };

    private NetProbe() {
    }

    public static void start(final Context ctx) {
        new Thread("sg-netprobe") {
            @Override
            public void run() {
                try {
                    Thread.sleep(6000);
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
        try {
            CrashLogWriter.log("NET start api_id=" + BuildVars.APP_ID
                    + " hash=" + (BuildVars.APP_HASH == null ? "null" : (BuildVars.APP_HASH.length() + "ch"))
                    + " native=" + NativeLoader.loaded());
        } catch (Throwable t) {
            CrashLogWriter.log("NET start err " + t);
        }

        try {
            long t0 = System.currentTimeMillis();
            InetAddress[] all = InetAddress.getAllByName("api.telegram.org");
            StringBuilder sb = new StringBuilder();
            for (InetAddress a : all) {
                sb.append(a.getHostAddress()).append(' ');
            }
            CrashLogWriter.log("NET dns api.telegram.org -> " + sb + (System.currentTimeMillis() - t0) + "ms");
        } catch (Throwable t) {
            CrashLogWriter.log("NET dns FAIL " + t);
        }

        for (String ip : DCS) {
            Socket s = new Socket();
            long t0 = System.currentTimeMillis();
            try {
                s.connect(new InetSocketAddress(ip, 443), 7000);
                CrashLogWriter.log("NET tcp " + ip + ":443 OK " + (System.currentTimeMillis() - t0) + "ms");
                try {
                    SSLSocket ss = (SSLSocket) SSLSocketFactory.getDefault().createSocket(s, ip, 443, true);
                    ss.setSoTimeout(7000);
                    ss.startHandshake();
                    CrashLogWriter.log("NET tls " + ip + " OK " + ss.getSession().getProtocol());
                    ss.close();
                } catch (Throwable t) {
                    CrashLogWriter.log("NET tls " + ip + " err " + t);
                }
            } catch (Throwable t) {
                CrashLogWriter.log("NET tcp " + ip + ":443 FAIL " + t);
            } finally {
                try {
                    s.close();
                } catch (Throwable ignore) {
                }
            }
        }

        try {
            CrashLogWriter.log("NET proxy=" + SharedConfig.currentProxy);
        } catch (Throwable t) {
            CrashLogWriter.log("NET proxy err " + t);
        }

        for (int i = 0; i < 5; i++) {
            try {
                ConnectionsManager cm = ConnectionsManager.getInstance(0);
                CrashLogWriter.log("NET state=" + cm.getConnectionState() + " dc=" + cm.getCurrentDatacenterId());
            } catch (Throwable t) {
                CrashLogWriter.log("NET state err " + t);
                break;
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignore) {
            }
        }
    }
}