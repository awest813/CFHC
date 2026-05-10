package simulation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Small stream helpers that avoid {@code java.nio.file} (Android minSdk 24). */
public final class IoStreams {

    private static final int BUFFER = 8192;

    private IoStreams() {
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFFER];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
    }

    public static void copyToFile(InputStream in, File dest) throws IOException {
        try (FileOutputStream out = new FileOutputStream(dest)) {
            copy(in, out);
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copy(in, buffer);
        return buffer.toByteArray();
    }
}
