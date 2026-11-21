package simulation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import positions.PlayerReturner;

/**
 * v1.5c: the box scores of this season's played games, which a save used to drop
 * (a reloaded season showed empty game summaries and play-by-play).
 *
 * <p>Saved as one league list, {@code LL:boxScores<TAB>z1<TAB><base64>}: the payload is
 * Deflate-compressed UTF-8 text with one line per played game,
 * {@code slot<TAB>key=value<TAB>...}, items escaped like other list items.
 * Compressing all games together matters: the logs and stat lines of a full season
 * (~870 games, ~4.3 MB of text) come to ~1.2 MB in the save, about half of what
 * compressing each game alone gives. Saves without the list (v1.5b and older) load with
 * empty box scores; a damaged list is skipped the same way.
 */
final class BoxScoreCodec {

    /** League list key. */
    static final String KEY = "boxScores";
    /** Payload encoding tag: Deflate + Base64 of the text described above. */
    private static final String FORMAT_Z1 = "z1";

    private BoxScoreCodec() {
    }

    /** The list items for {@code games} (slot -> game; unplayed games are skipped). */
    static List<String> encode(Map<Integer, Game> games) {
        StringBuilder text = new StringBuilder(1 << 16);
        for (Map.Entry<Integer, Game> e : games.entrySet()) {
            Game g = e.getValue();
            if (g == null || !g.hasPlayed) {
                continue;
            }
            text.append(e.getKey());
            for (String item : gameItems(g)) {
                text.append('\t').append(SaveManager.escapeItem(item));
            }
            text.append('\n');
        }
        List<String> out = new ArrayList<>(2);
        if (text.length() == 0) {
            return out;
        }
        out.add(FORMAT_Z1);
        out.add(base64(deflate(text.toString().getBytes(StandardCharsets.UTF_8))));
        return out;
    }

    /** Restores the saved box scores onto the rebuilt games (slot -> game). */
    static void apply(List<String> items, Map<Integer, Game> games) {
        if (items == null || items.size() < 2 || !FORMAT_Z1.equals(items.get(0))) {
            return;
        }
        String text;
        try {
            text = new String(inflate(unbase64(items.get(1))), StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException e) {
            PlatformLog.w("BoxScoreCodec", "Skipping unreadable box scores in save", e);
            return;
        }
        for (String line : text.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split("\t", -1);
            try {
                Game g = games.get(Integer.parseInt(p[0]));
                if (g != null) {
                    applyGame(g, p);
                }
            } catch (RuntimeException e) {
                PlatformLog.w("BoxScoreCodec", "Skipping a damaged box score line", e);
            }
        }
    }

    private static List<String> gameItems(Game g) {
        List<String> items = new ArrayList<>(24);
        items.add("s=" + csv(g.homeYards, g.awayYards, g.homePassYards, g.awayPassYards,
                g.homeRushYards, g.awayRushYards, g.homeTurnovers, g.awayTurnovers,
                g.homePenalties, g.awayPenalties, g.homePenaltyYards, g.awayPenaltyYards,
                g.homeTimeouts, g.awayTimeouts));
        items.add("r=" + g.hkReturnAvg + "," + g.akReturnAvg + "," + g.hpReturnAvg + "," + g.apReturnAvg);
        items.add("hq=" + csv(g.homeQScore));
        items.add("aq=" + csv(g.awayQScore));
        if (g.weather != null) {
            items.add("w=" + g.weather.name());
        }
        if (g.tdInfo != null) {
            items.add("td=" + g.tdInfo);
        }
        addReturner(items, "hr", g.homeKickReturner);
        addReturner(items, "ar", g.awayKickReturner);
        addLines(items, "hP", g.homePassingStats);
        addLines(items, "aP", g.awayPassingStats);
        addLines(items, "hR", g.homeRushingStats);
        addLines(items, "aR", g.awayRushingStats);
        addLines(items, "hC", g.homeReceivingStats);
        addLines(items, "aC", g.awayReceivingStats);
        addLines(items, "hK", g.homeKickingStats);
        addLines(items, "aK", g.awayKickingStats);
        addLines(items, "hD", g.homeDefenseStats);
        addLines(items, "aD", g.awayDefenseStats);
        items.add("log=" + g.gameEventLog);
        return items;
    }

    private static void applyGame(Game g, String[] p) {
        for (int i = 1; i < p.length; i++) {
            String item = SaveManager.unescapeItem(p[i]);
            int eq = item.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String k = item.substring(0, eq);
            String v = item.substring(eq + 1);
            switch (k) {
                case "s": {
                    int[] s = ints(v);
                    if (s.length >= 14) {
                        g.homeYards = s[0]; g.awayYards = s[1];
                        g.homePassYards = s[2]; g.awayPassYards = s[3];
                        g.homeRushYards = s[4]; g.awayRushYards = s[5];
                        g.homeTurnovers = s[6]; g.awayTurnovers = s[7];
                        g.homePenalties = s[8]; g.awayPenalties = s[9];
                        g.homePenaltyYards = s[10]; g.awayPenaltyYards = s[11];
                        g.homeTimeouts = s[12]; g.awayTimeouts = s[13];
                    }
                    break;
                }
                case "r": {
                    String[] r = v.split(",");
                    if (r.length >= 4) {
                        g.hkReturnAvg = Double.parseDouble(r[0]);
                        g.akReturnAvg = Double.parseDouble(r[1]);
                        g.hpReturnAvg = Double.parseDouble(r[2]);
                        g.apReturnAvg = Double.parseDouble(r[3]);
                    }
                    break;
                }
                case "hq": copyInto(ints(v), g.homeQScore); break;
                case "aq": copyInto(ints(v), g.awayQScore); break;
                case "w":
                    try {
                        g.weather = Weather.valueOf(v);
                    } catch (IllegalArgumentException ignored) {
                        // a weather this build does not know keeps the default
                    }
                    break;
                case "td": g.tdInfo = v; break;
                case "hr": g.homeKickReturner = returner(v); break;
                case "ar": g.awayKickReturner = returner(v); break;
                case "hP": g.homePassingStats = lines(v); break;
                case "aP": g.awayPassingStats = lines(v); break;
                case "hR": g.homeRushingStats = lines(v); break;
                case "aR": g.awayRushingStats = lines(v); break;
                case "hC": g.homeReceivingStats = lines(v); break;
                case "aC": g.awayReceivingStats = lines(v); break;
                case "hK": g.homeKickingStats = lines(v); break;
                case "aK": g.awayKickingStats = lines(v); break;
                case "hD": g.homeDefenseStats = lines(v); break;
                case "aD": g.awayDefenseStats = lines(v); break;
                case "log":
                    g.gameEventLog.setLength(0);
                    g.gameEventLog.append(v);
                    break;
                default:
                    break; // a key from a newer build
            }
        }
    }

    private static void addReturner(List<String> items, String key, PlayerReturner r) {
        if (r == null) {
            return;
        }
        items.add(key + "=" + r.team + "\n" + r.name + "\n" + r.position + "\n" + r.kReturns + "\n" + r.kYards
                + "\n" + r.kTD + "\n" + r.pReturns + "\n" + r.pYards + "\n" + r.pTD + "\n" + r.ratSpeed
                + "\n" + r.startPos + "\n" + r.gameSpeed);
    }

    private static PlayerReturner returner(String v) {
        String[] f = v.split("\n", -1);
        PlayerReturner r = new PlayerReturner(f[0], f[1], f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]),
                Integer.parseInt(f[5]), Integer.parseInt(f[6]), Integer.parseInt(f[7]), Integer.parseInt(f[8]));
        if (f.length >= 12) {
            r.ratSpeed = Integer.parseInt(f[9]);
            r.startPos = Integer.parseInt(f[10]);
            r.gameSpeed = Float.parseFloat(f[11]);
        }
        return r;
    }

    /** A null list is left out; an empty one is written with an empty value. */
    private static void addLines(List<String> items, String key, List<String> lines) {
        if (lines != null) {
            items.add(key + "=" + String.join("\n", lines));
        }
    }

    private static ArrayList<String> lines(String v) {
        ArrayList<String> out = new ArrayList<>();
        if (!v.isEmpty()) {
            for (String s : v.split("\n", -1)) {
                out.add(s);
            }
        }
        return out;
    }

    private static String csv(int... v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    private static int[] ints(String v) {
        if (v.isEmpty()) {
            return new int[0];
        }
        String[] p = v.split(",");
        int[] out = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            out[i] = Integer.parseInt(p[i].trim());
        }
        return out;
    }

    private static void copyInto(int[] from, int[] to) {
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static byte[] deflate(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.length / 4 + 64);
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try (DeflaterOutputStream z = new DeflaterOutputStream(out, d, 1 << 16)) {
            z.write(in);
        } catch (IOException e) {
            throw new IllegalStateException(e); // in-memory streams do not fail
        } finally {
            d.end();
        }
        return out.toByteArray();
    }

    private static byte[] inflate(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.length * 6 + 64);
        try (InflaterInputStream z = new InflaterInputStream(new ByteArrayInputStream(in))) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = z.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    private static final char[] B64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    /** Standard Base64 with padding (java.util.Base64 needs Android API 26; the app supports 24). */
    static String base64(byte[] b) {
        StringBuilder sb = new StringBuilder((b.length + 2) / 3 * 4);
        for (int i = 0; i < b.length; i += 3) {
            int n = (b[i] & 0xff) << 16;
            if (i + 1 < b.length) n |= (b[i + 1] & 0xff) << 8;
            if (i + 2 < b.length) n |= b[i + 2] & 0xff;
            sb.append(B64[(n >> 18) & 63]).append(B64[(n >> 12) & 63]);
            sb.append(i + 1 < b.length ? B64[(n >> 6) & 63] : '=');
            sb.append(i + 2 < b.length ? B64[n & 63] : '=');
        }
        return sb.toString();
    }

    static byte[] unbase64(String s) {
        int[] rev = new int[128];
        java.util.Arrays.fill(rev, -1);
        for (int i = 0; i < B64.length; i++) rev[B64[i]] = i;
        int len = s.length();
        while (len > 0 && s.charAt(len - 1) == '=') len--;
        if ((len & 3) == 1) {
            throw new IllegalArgumentException("Bad Base64 length");
        }
        byte[] out = new byte[len * 3 / 4];
        int o = 0, acc = 0, bits = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            int v = c < 128 ? rev[c] : -1;
            if (v < 0) {
                throw new IllegalArgumentException("Bad Base64 character at " + i);
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) (acc >> bits);
            }
        }
        return out;
    }
}
