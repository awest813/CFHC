package simulation.rows;

/** The engine's row strings are comma-separated with padded blanks; missing fields read as "". */
final class Csv {
    private final String[] f;

    Csv(String line, String separator) {
        f = (line == null ? "" : line).split(separator, -1);
    }

    String at(int i) {
        return i < f.length ? f[i].trim() : "";
    }

    int size() {
        return f.length;
    }
}
