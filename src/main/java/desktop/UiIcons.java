package desktop;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small line icons drawn with Java2D on a 16-unit grid.
 *
 * <p>The desktop UI used Unicode symbols and emoji as icons. Those depend on the
 * platform's fonts: sizes and weights varied from row to row, some meant the
 * wrong thing (the Mac Command key for Player Search, a soccer ball for Special
 * Teams), and emoji showed as tofu boxes or unreadable smudges wherever no
 * colour-emoji font is installed. These icons paint the same everywhere and
 * take the host component's foreground colour, so they follow the theme and
 * list selection.
 */
final class UiIcons {

    enum Glyph {
        HOME, RECRUITING, STANDINGS, SCOREBOARD, WHISTLE, PODIUM, BAR_CHART, LINE_CHART, SEARCH,
        HISTORY, NEWS, COACHES, TROPHY, MEDAL, GEAR,
        OFFENSE, DEFENSE, SPECIAL_TEAMS,
        CALENDAR, PIN, HEART, FOOTBALL, CLIPBOARD, BUS, SUN,
        MAIL, MUSIC, VOLUME, MUTED
    }

    private static final float STROKE = 1.5f;

    private UiIcons() {}

    /** Icon painted in the host component's foreground colour. */
    static Icon of(Glyph glyph, int size) {
        return new GlyphIcon(glyph, size, null);
    }

    /** Icon painted in a fixed colour. */
    static Icon of(Glyph glyph, int size, Color color) {
        return new GlyphIcon(glyph, size, color);
    }

    private static final class GlyphIcon implements Icon {
        private final Glyph glyph;
        private final int size;
        private final Color color;

        GlyphIcon(Glyph glyph, int size, Color color) {
            this.glyph = glyph;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.scale(size / 16.0, size / 16.0);
                Color paint = color != null ? color : (c != null ? c.getForeground() : Color.GRAY);
                if (c != null && !c.isEnabled()) {
                    paint = new Color(paint.getRed(), paint.getGreen(), paint.getBlue(), 110);
                }
                g2.setColor(paint);
                g2.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(glyph, g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static void draw(Glyph glyph, Graphics2D g) {
        switch (glyph) {
            case HOME -> {
                g.draw(poly(2, 8, 8, 2.5, 14, 8));
                g.draw(poly(3.5, 7, 3.5, 13.5, 12.5, 13.5, 12.5, 7));
                g.draw(poly(6.5, 13.5, 6.5, 10, 9.5, 10, 9.5, 13.5));
            }
            case RECRUITING -> {
                person(g, 6, 5, 2.4, 1.5, 10.5);
                g.draw(line(13, 5.5, 13, 10.5));
                g.draw(line(10.5, 8, 15.5, 8));
            }
            case STANDINGS -> {
                for (double y : new double[]{4, 8, 12}) {
                    g.fill(dot(3, y, 1.1));
                    g.draw(line(6, y, 14, y));
                }
            }
            case SCOREBOARD -> {
                g.draw(new RoundRectangle2D.Double(1.5, 3, 13, 8.5, 2.5, 2.5));
                g.draw(line(8, 5, 8, 9.5));
                g.fill(new Rectangle2D.Double(3.8, 5.3, 2.4, 3.9));
                g.fill(new Rectangle2D.Double(9.8, 5.3, 2.4, 3.9));
                g.draw(line(5, 11.5, 5, 14.5));
                g.draw(line(11, 11.5, 11, 14.5));
            }
            case WHISTLE -> {
                g.draw(new Ellipse2D.Double(2, 6.5, 7.5, 7.5));
                g.draw(poly(5.75, 6.5, 14.5, 6.5, 14.5, 9.5, 9.3, 9.5));
                g.fill(dot(5.75, 10.25, 1.2));
            }
            case PODIUM -> {
                g.draw(closed(1.5, 14.5, 1.5, 9, 5.5, 9, 5.5, 5, 10.5, 5, 10.5, 10.5, 14.5, 10.5, 14.5, 14.5));
                g.draw(line(8, 7.5, 8, 11.5));
                g.draw(line(5.5, 9, 5.5, 14.5));
                g.draw(line(10.5, 10.5, 10.5, 14.5));
            }
            case BAR_CHART -> {
                g.draw(line(1.5, 14.5, 14.5, 14.5));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line(4, 12.5, 4, 9));
                g.draw(line(8, 12.5, 8, 6));
                g.draw(line(12, 12.5, 12, 3));
            }
            case LINE_CHART -> {
                g.draw(poly(2, 2, 2, 14, 14.5, 14));
                g.draw(poly(4.5, 11, 7.5, 7.5, 10, 9.5, 14, 4));
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(2, 2, 8.5, 8.5));
                g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line(9.6, 9.6, 14, 14));
            }
            case HISTORY -> {
                // Counter-clockwise arc with its arrowhead on the left, plus clock hands.
                g.draw(new Arc2D.Double(2.75, 2.75, 11.5, 11.5, 210, 300, Arc2D.OPEN));
                g.draw(poly(5.4, 5.8, 2.9, 6.7, 2.4, 4.1));
                g.draw(poly(8.5, 5.5, 8.5, 8.5, 10.75, 10));
            }
            case NEWS -> {
                g.draw(new RoundRectangle2D.Double(2, 2.5, 12, 11, 1.5, 1.5));
                g.fill(new Rectangle2D.Double(4, 5, 4, 3));
                g.draw(line(9.5, 5.5, 12, 5.5));
                g.draw(line(9.5, 7.5, 12, 7.5));
                g.draw(line(4, 10, 12, 10));
                g.draw(line(4, 11.9, 10, 11.9));
            }
            case COACHES -> {
                person(g, 6, 5.2, 2.2, 1.5, 10.5);
                g.draw(new Ellipse2D.Double(9.2, 2.6, 3.8, 3.8));
                Path2D back = new Path2D.Double();
                back.moveTo(11.6, 8.6);
                back.curveTo(13.4, 8.8, 14.5, 10.3, 14.5, 13.2);
                g.draw(back);
            }
            case TROPHY -> {
                Path2D cup = new Path2D.Double();
                cup.moveTo(4.5, 2.5);
                cup.lineTo(11.5, 2.5);
                cup.lineTo(11.5, 6);
                cup.curveTo(11.5, 8.3, 9.9, 10, 8, 10);
                cup.curveTo(6.1, 10, 4.5, 8.3, 4.5, 6);
                cup.closePath();
                g.draw(cup);
                Path2D handles = new Path2D.Double();
                handles.moveTo(4.5, 4);
                handles.curveTo(2, 4, 2, 7.6, 4.9, 7.4);
                handles.moveTo(11.5, 4);
                handles.curveTo(14, 4, 14, 7.6, 11.1, 7.4);
                g.draw(handles);
                g.draw(line(8, 10, 8, 12.5));
                g.draw(line(5.5, 13.5, 10.5, 13.5));
            }
            case MEDAL -> {
                g.draw(poly(4.5, 1.5, 6.8, 7.2));
                g.draw(poly(11.5, 1.5, 9.2, 7.2));
                g.draw(new Ellipse2D.Double(4.5, 7, 7, 7));
                g.fill(dot(8, 10.5, 1.3));
            }
            case GEAR -> {
                double c = 8;
                g.draw(new Ellipse2D.Double(c - 4.2, c - 4.2, 8.4, 8.4));
                g.draw(new Ellipse2D.Double(c - 1.7, c - 1.7, 3.4, 3.4));
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45 + 22.5);
                    g.draw(line(c + 4.2 * Math.cos(a), c + 4.2 * Math.sin(a),
                            c + 6.4 * Math.cos(a), c + 6.4 * Math.sin(a)));
                }
            }
            case OFFENSE -> {
                g.draw(line(2.5, 13.5, 13, 3));
                g.draw(poly(7.5, 3, 13, 3, 13, 8.5));
            }
            case DEFENSE -> {
                Path2D shield = new Path2D.Double();
                shield.moveTo(8, 1.5);
                shield.lineTo(13.5, 3.5);
                shield.lineTo(13.5, 7.5);
                shield.curveTo(13.5, 11, 11, 13.3, 8, 14.5);
                shield.curveTo(5, 13.3, 2.5, 11, 2.5, 7.5);
                shield.lineTo(2.5, 3.5);
                shield.closePath();
                g.draw(shield);
            }
            case SPECIAL_TEAMS -> {
                // Goalposts: the kicking game.
                g.draw(poly(3, 1.5, 3, 7.5, 13, 7.5, 13, 1.5));
                g.draw(line(8, 7.5, 8, 14.5));
            }
            case CALENDAR -> {
                g.draw(new RoundRectangle2D.Double(2, 3, 12, 11, 2, 2));
                g.draw(line(2, 6.5, 14, 6.5));
                g.draw(line(5, 1.5, 5, 4.5));
                g.draw(line(11, 1.5, 11, 4.5));
                for (double[] p : new double[][]{{5, 9}, {8, 9}, {11, 9}, {5, 11.5}, {8, 11.5}}) {
                    g.fill(dot(p[0], p[1], 0.85));
                }
            }
            case PIN -> {
                Path2D pin = new Path2D.Double();
                pin.moveTo(8, 14.5);
                pin.curveTo(8, 14.5, 3, 9.5, 3, 6);
                pin.curveTo(3, 3.2, 5.2, 1.5, 8, 1.5);
                pin.curveTo(10.8, 1.5, 13, 3.2, 13, 6);
                pin.curveTo(13, 9.5, 8, 14.5, 8, 14.5);
                pin.closePath();
                g.draw(pin);
                g.draw(new Ellipse2D.Double(6.2, 4.2, 3.6, 3.6));
            }
            case HEART -> {
                Path2D heart = new Path2D.Double();
                heart.moveTo(8, 13.5);
                heart.curveTo(8, 13.5, 2, 9.8, 2, 5.8);
                heart.curveTo(2, 3.8, 3.5, 2.5, 5.2, 2.5);
                heart.curveTo(6.5, 2.5, 7.5, 3.3, 8, 4.3);
                heart.curveTo(8.5, 3.3, 9.5, 2.5, 10.8, 2.5);
                heart.curveTo(12.5, 2.5, 14, 3.8, 14, 5.8);
                heart.curveTo(14, 9.8, 8, 13.5, 8, 13.5);
                heart.closePath();
                g.draw(heart);
            }
            case FOOTBALL -> {
                Path2D ball = new Path2D.Double();
                ball.moveTo(2, 14);
                ball.curveTo(2, 7.5, 7.5, 2, 14, 2);
                ball.curveTo(14, 8.5, 8.5, 14, 2, 14);
                ball.closePath();
                g.draw(ball);
                g.draw(line(5.8, 10.2, 10.2, 5.8));
                for (double t : new double[]{6.9, 8, 9.1}) {
                    g.draw(line(t - 0.9, (16 - t) - 0.9, t + 0.9, (16 - t) + 0.9));
                }
            }
            case CLIPBOARD -> {
                g.draw(new RoundRectangle2D.Double(3, 2.5, 10, 12, 1.8, 1.8));
                g.fill(new RoundRectangle2D.Double(6, 1.3, 4, 2.6, 1.2, 1.2));
                g.draw(line(5.5, 7, 10.5, 7));
                g.draw(line(5.5, 9.5, 10.5, 9.5));
                g.draw(line(5.5, 12, 8.5, 12));
            }
            case BUS -> {
                g.draw(new RoundRectangle2D.Double(2, 2, 12, 10.5, 3, 3));
                g.draw(line(2, 7, 14, 7));
                g.draw(line(8, 2, 8, 7));
                g.fill(dot(4.8, 9.8, 0.9));
                g.fill(dot(11.2, 9.8, 0.9));
                g.draw(line(4.5, 12.5, 4.5, 14.5));
                g.draw(line(11.5, 12.5, 11.5, 14.5));
            }
            case SUN -> {
                g.draw(new Ellipse2D.Double(5, 5, 6, 6));
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    g.draw(line(8 + 5 * Math.cos(a), 8 + 5 * Math.sin(a),
                            8 + 6.6 * Math.cos(a), 8 + 6.6 * Math.sin(a)));
                }
            }
            case MAIL -> {
                g.draw(new RoundRectangle2D.Double(1.5, 3.5, 13, 9, 1.8, 1.8));
                g.draw(poly(2, 4.5, 8, 9, 14, 4.5));
            }
            case MUSIC -> {
                g.draw(line(5.5, 11.5, 5.5, 3.6));
                g.draw(line(12.5, 10, 12.5, 2.1));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line(5.5, 3.6, 12.5, 2.1));
                g.fill(new Ellipse2D.Double(2.2, 10.2, 3.6, 2.8));
                g.fill(new Ellipse2D.Double(9.2, 8.7, 3.6, 2.8));
            }
            case VOLUME -> {
                speaker(g);
                g.draw(new Arc2D.Double(7.5, 5.5, 5, 5, -50, 100, Arc2D.OPEN));
                g.draw(new Arc2D.Double(5.5, 3, 9.5, 10, -50, 100, Arc2D.OPEN));
            }
            case MUTED -> {
                speaker(g);
                g.draw(line(11, 6, 14.5, 9.5));
                g.draw(line(14.5, 6, 11, 9.5));
            }
        }
    }

    /** Head and shoulders centred on {@code headX}. */
    private static void person(Graphics2D g, double headX, double headY, double headR,
                               double left, double right) {
        g.draw(new Ellipse2D.Double(headX - headR, headY - headR, headR * 2, headR * 2));
        Path2D body = new Path2D.Double();
        double top = headY + headR + 1.6;
        body.moveTo(left, 14);
        body.curveTo(left, top + 1.5, left + 2, top, headX, top);
        body.curveTo(right - 2, top, right, top + 1.5, right, 14);
        g.draw(body);
    }

    private static void speaker(Graphics2D g) {
        g.draw(closed(2, 6, 5, 6, 8.5, 3, 8.5, 13, 5, 10, 2, 10));
    }

    private static Shape line(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    private static Shape dot(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    private static Path2D closed(double... xy) {
        Path2D p = poly(xy);
        p.closePath();
        return p;
    }
}
