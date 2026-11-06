package simulation.rows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One line of {@code Team.getRosterStats()}: a name followed by up to six stat
 * columns. Category rows ("Passing,Cmp%,Yrds,...") are SECTION headers whose
 * values are the column titles; a line with no name and no values is a spacer.
 */
public record StatLine(RowKind kind, String name, List<String> values) {

    public static final int COLUMNS = 6;
    private static final java.util.Set<String> CATEGORIES =
            java.util.Set.of("Passing", "Rushing", "Receiving", "Defense", "Kicking");

    public static StatLine parse(String line) {
        Csv c = new Csv(line, ",");
        String name = c.at(0);
        List<String> values = new ArrayList<>(COLUMNS);
        boolean any = false;
        for (int i = 0; i < COLUMNS; i++) {
            String v = c.at(i + 1);
            values.add(v);
            any |= !v.isEmpty();
        }
        RowKind kind;
        if (name.isEmpty() && !any) {
            kind = RowKind.SPACER;
        } else if (CATEGORIES.contains(name) && (values.get(0).isEmpty() || !values.get(0).matches("[-+]?\\d.*"))) {
            kind = RowKind.SECTION;
        } else {
            kind = RowKind.ENTRY;
        }
        return new StatLine(kind, name, Collections.unmodifiableList(values));
    }

    public String value(int i) {
        return i < values.size() ? values.get(i) : "";
    }
}
