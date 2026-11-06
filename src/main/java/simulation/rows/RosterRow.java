package simulation.rows;

/**
 * One line of {@code Team.getRoster()}: {@code role,detail,name,status,rating,awards,progression}.
 * Staff rows carry age in {@code detail}, player rows the class year. A row
 * with no role but a name is a section header ("Quarterbacks"); a row with
 * neither is a spacer.
 */
public record RosterRow(RowKind kind, String role, String detail, String name, String status,
                        String rating, String awards, String progression) {

    public static RosterRow parse(String line) {
        Csv c = new Csv(line, ",");
        String role = c.at(0);
        String name = c.at(2);
        RowKind kind = role.isEmpty() ? (name.isEmpty() ? RowKind.SPACER : RowKind.SECTION) : RowKind.ENTRY;
        return new RosterRow(kind, role, c.at(1), name, c.at(3), c.at(4), c.at(5), c.at(6));
    }
}
