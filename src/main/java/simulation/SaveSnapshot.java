package simulation;

import java.nio.charset.StandardCharsets;

/**
 * A save taken on the league thread whose most expensive step, compressing the
 * season's box scores, is left for {@link #finish()}, which may run on any
 * thread (see {@link League#snapshotSave()}).
 *
 * <p>The league is read only while the snapshot is taken: {@link #finish()}
 * touches nothing but the bytes captured then, and returns exactly the bytes
 * {@link League#serializeSave()} would have returned at that moment.
 */
public final class SaveSnapshot {

    private final byte[] save;
    private final byte[] token;
    private final byte[] boxScoreText;
    private final int tokenAt;

    /** A finished save (nothing deferred). */
    SaveSnapshot(byte[] save) {
        this(save, null, null, -1);
    }

    /**
     * @param save         save bytes holding {@code token} once, in place of the box-score payload
     * @param boxScoreText the uncompressed box-score text the payload is made from
     */
    SaveSnapshot(byte[] save, byte[] token, byte[] boxScoreText, int tokenAt) {
        this.save = save;
        this.token = token;
        this.boxScoreText = boxScoreText;
        this.tokenAt = tokenAt;
    }

    /** Placeholder written where the payload goes: ASCII, never escaped, and unique per snapshot. */
    static String newToken() {
        return "CFHCdeferredBoxScores" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /** Index of the only occurrence of {@code token} in {@code data}, or -1 when it is absent or repeated. */
    static int indexOfOnly(byte[] data, byte[] token) {
        int found = -1;
        outer:
        for (int i = 0, last = data.length - token.length; i <= last; i++) {
            if (data[i] != token[0]) {
                continue;
            }
            for (int j = 1; j < token.length; j++) {
                if (data[i + j] != token[j]) {
                    continue outer;
                }
            }
            if (found >= 0) {
                return -1;
            }
            found = i;
        }
        return found;
    }

    /** The save bytes; compresses the box scores first if that was deferred. Safe on any thread. */
    public byte[] finish() {
        if (boxScoreText == null) {
            return save;
        }
        byte[] payload = BoxScoreCodec.payload(boxScoreText).getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[save.length - token.length + payload.length];
        System.arraycopy(save, 0, out, 0, tokenAt);
        System.arraycopy(payload, 0, out, tokenAt, payload.length);
        System.arraycopy(save, tokenAt + token.length, out, tokenAt + payload.length,
                save.length - tokenAt - token.length);
        return out;
    }
}
