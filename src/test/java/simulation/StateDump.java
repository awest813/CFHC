package simulation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug aid for save determinism: a reflective dump of every engine object reachable
 * from a league (primitives and strings, one per line, by field path). Diffing the
 * dump of a league against the dump of its save reloaded shows each field a save
 * does not carry. Repeated references print as {@code @path} of their first visit.
 */
final class StateDump {

    private final java.io.Writer out;
    private final IdentityHashMap<Object, String> seen = new IdentityHashMap<>();
    private final List<String> skipFields;
    private final java.util.ArrayDeque<Object> pending = new java.util.ArrayDeque<>();

    private StateDump(List<String> skipFields, java.io.Writer out) {
        this.skipFields = skipFields;
        this.out = out;
    }

    static void dump(Object root, List<String> skipFields, java.io.Writer out) {
        StateDump d = new StateDump(skipFields, out);
        d.visit("league", root, 0);
        while (!d.pending.isEmpty()) {
            Object next = d.pending.removeFirst();
            d.visitFields(label(next), next, 0);
        }
    }

    private static boolean engineClass(Class<?> c) {
        String n = c.getName();
        return n.startsWith("simulation.") || n.startsWith("positions.") || n.startsWith("staff.")
                || n.startsWith("recruiting.") || n.startsWith("comparator.");
    }

    /** Stable names for the big entities, so their fields dump under the same path however reached. */
    private static String label(Object o) {
        if (o instanceof Team) {
            return "Team(" + ((Team) o).getName() + ")";
        }
        if (o instanceof positions.Player) {
            positions.Player p = (positions.Player) o;
            return "Player(" + p.position + " " + p.name + " @" + (p.team != null ? p.team.getName() : "-") + ")";
        }
        if (o instanceof staff.Staff) {
            staff.Staff st = (staff.Staff) o;
            return "Staff(" + st.position + " " + st.name + ")";
        }
        if (o instanceof Game) {
            Game g = (Game) o;
            return "Game(" + g.gameName + ":" + (g.homeTeam != null ? g.homeTeam.getName() : "") + "-"
                    + (g.awayTeam != null ? g.awayTeam.getName() : "") + ")";
        }
        return null;
    }

    private void line(String path, Object value) {
        try {
            out.write(path + "=" + value + "\n");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private void visit(String path, Object o, int depth) {
        if (o == null) {
            line(path, "null");
            return;
        }
        Class<?> c = o.getClass();
        if (o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character
                || c.isEnum()) {
            line(path, o);
            return;
        }
        String label = label(o);
        if (label != null) {
            line(path, "@" + label);
            if (seen.put(o, label) == null) {
                pending.add(o);
            }
            return;
        }
        if (depth > 40) {
            line(path, "<deep>");
            return;
        }
        String prev = seen.get(o);
        if (prev != null) {
            line(path, "@" + prev);
            return;
        }
        seen.put(o, path);
        if (c.isArray()) {
            int n = Array.getLength(o);
            line(path + ".length", n);
            for (int i = 0; i < n; i++) {
                visit(path + "[" + i + "]", Array.get(o, i), depth + 1);
            }
            return;
        }
        if (o instanceof Collection) {
            int i = 0;
            line(path + ".size", ((Collection<?>) o).size());
            for (Object e : (Collection<?>) o) {
                visit(path + "[" + i++ + "]", e, depth + 1);
            }
            return;
        }
        if (o instanceof Map) {
            line(path + ".size", ((Map<?, ?>) o).size());
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                Object k = e.getKey();
                String key = (k instanceof String || k instanceof Number || k instanceof Enum) ? String.valueOf(k)
                        : (k == null ? "null" : seen.getOrDefault(k, k.getClass().getSimpleName()));
                visit(path + "{" + key + "}", e.getValue(), depth + 1);
            }
            return;
        }
        if (!engineClass(c)) {
            line(path, "<" + c.getSimpleName() + ">");
            return;
        }
        visitFields(path, o, depth);
    }

    private void visitFields(String path, Object o, int depth) {
        Class<?> c = o.getClass();
        List<Field> fields = new ArrayList<>();
        for (Class<?> k = c; k != null && engineClass(k); k = k.getSuperclass()) {
            List<Field> own = new ArrayList<>();
            for (Field f : k.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                own.add(f);
            }
            fields.addAll(0, own);
        }
        for (Field f : fields) {
            String key = c.getSimpleName() + "." + f.getName();
            if (skipFields.contains(f.getName()) || skipFields.contains(key)) continue;
            try {
                f.setAccessible(true);
                visit(path + "." + f.getName(), f.get(o), depth + 1);
            } catch (ReflectiveOperationException | RuntimeException e) {
                line(path + "." + f.getName(), "<inaccessible>");
            }
        }
    }
}
