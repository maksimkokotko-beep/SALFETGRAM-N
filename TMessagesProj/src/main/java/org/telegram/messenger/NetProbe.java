package org.telegram.messenger;

import android.content.Context;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * SALFETGRAM diagnostics: checks from inside the app whether Telegram is
 * reachable and whether an MTProto request actually completes.
 */
public final class NetProbe {

    private static final String[] DCS = {
            "149.154.167.51",    // DC4 ipv4
            "149.154.175.53",    // DC2 ipv4
            "91.108.56.130",     // DC5 ipv4
            "2001:b28:f23d:f001::a" // DC4 ipv6
    };

    private NetProbe() {
    }

    public static void start(final Context ctx) {
        new Thread("sg-netprobe") {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
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
                s.connect(new InetSocketAddress(ip, 443), 8000);
                s.setTcpNoDelay(true);
                // MTProto obfuscated2: the client starts with 64 random bytes.
                // A live DC keeps the socket open and answers once it gets a valid frame.
                byte[] head = new byte[64];
                new java.util.Random().nextBytes(head);
                head[56] = head[57] = head[58] = head[59] = 0xEF;
                s.getOutputStream().write(head);
                s.getOutputStream().flush();
                s.setSoTimeout(5000);
                int r = s.getInputStream().read();
                CrashLogWriter.log("NET tcp " + ip + ":443 OK " + (System.currentTimeMillis() - t0) + "ms firstByte=" + r);
            } catch (Throwable t) {
                CrashLogWriter.log("NET tcp " + ip + ":443 " + (System.currentTimeMillis() - t0) + "ms err " + t);
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

        try {
            TLRPC.TL_help_getConfig req = new TLRPC.TL_help_getConfig();
            ConnectionsManager.getInstance(0).sendRequest(req, (response, error) -> {
                if (error != null) {
                    CrashLogWriter.log("NET rpc getConfig ERR " + error.code + " " + error.text);
                } else {
                    CrashLogWriter.log("NET rpc getConfig OK " + (response == null ? "null" : response.getClass().getSimpleName()));
                }
            });
            CrashLogWriter.log("NET rpc getConfig sent");
        } catch (Throwable t) {
            CrashLogWriter.log("NET rpc send err " + t);
        }

        int last = -1;
        for (int i = 0; i < 30; i++) {
            try {
                ConnectionsManager cm = ConnectionsManager.getInstance(0);
                int st = cm.getConnectionState();
                if (st != last) {
                    last = st;
                    CrashLogWriter.log("NET state=" + st + " dc=" + cm.getCurrentDatacenterId() + " t=" + i * 2 + "s");
                }
            } catch (Throwable t) {
                CrashLogWriter.log("NET state err " + t);
                break;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignore) {
            }
        }
        CrashLogWriter.log("NET probe done abis=" + Arrays.toString(android.os.Build.SUPPORTED_ABIS));
    }
}