package desktop;

import javax.swing.DefaultListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class NewsPanel implements LeagueScreen {

    @Override
    public String title() {
        return "News";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultListModel<String> headlineModel = new DefaultListModel<>();

        JTextArea storyArea = new JTextArea("Select a headline to read the full story.");
        storyArea.setEditable(false);
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleTextContent(storyArea);

        // Track the viewport width so rows wrap instead of running off the pane
        // (headlines were cut mid-word behind a horizontal scrollbar).
        JList<String> headlineList = new JList<>(headlineModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        headlineList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleListShell(headlineList);
        headlineList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                String fg = isSelected ? "rgb(255,255,255)" : DesktopTheme.cssRgb(DesktopTheme.textPrimary());
                int wrap = Math.max(120, list.getWidth() - 20);
                l.setText("<html><body style='width:" + wrap + "px;color:" + fg + ";'>"
                        + DesktopTheme.escapeForHtml(value.toString()) + "</body></html>");
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(DesktopTheme.buildScreenHeader("News", "Review weekly headlines and league storylines."), BorderLayout.NORTH);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Prev Week");
        JButton nextBtn = new JButton("Next Week \u25B6");
        JButton latestBtn = new JButton("Latest");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        weekLabel.setForeground(DesktopTheme.textPrimary());

        final int[] newsWeek = { Math.max(0, ctx.league().currentWeek) };

        Runnable loadNewsForWeek = () -> {
            headlineModel.clear();
            weekLabel.setText("Week " + newsWeek[0] + " News");
            List<List<String>> stories = ctx.league().getNewsStories();
            if (stories != null && newsWeek[0] >= 0 && newsWeek[0] < stories.size()) {
                List<String> weekStories = stories.get(newsWeek[0]);
                if (weekStories != null) {
                    for (String s : weekStories) {
                        String[] parts = s.split(">");
                        headlineModel.addElement(parts[0].trim());
                    }
                }
            }
            if (headlineModel.isEmpty()) {
                headlineModel.addElement("No news for week " + newsWeek[0] + ".");
                storyArea.setText("No stories were generated for this week.");
            } else {
                storyArea.setText("Select a headline to read the full story.");
            }
            prevBtn.setEnabled(newsWeek[0] > 0);
            nextBtn.setEnabled(newsWeek[0] < ctx.league().currentWeek);
        };

        prevBtn.addActionListener(e -> {
            if (newsWeek[0] > 0) { newsWeek[0]--; loadNewsForWeek.run(); }
        });
        nextBtn.addActionListener(e -> {
            if (newsWeek[0] < ctx.league().currentWeek) { newsWeek[0]++; loadNewsForWeek.run(); }
        });
        latestBtn.addActionListener(e -> {
            newsWeek[0] = Math.max(0, ctx.league().currentWeek);
            loadNewsForWeek.run();
        });

        navPanel.add(prevBtn);
        navPanel.add(weekLabel);
        navPanel.add(nextBtn);
        navPanel.add(latestBtn);
        DesktopTheme.styleToolbar(navPanel);
        topPanel.add(navPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        headlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = headlineList.getSelectedIndex();
                if (idx >= 0) {
                    String story = lookupStoryForWeek(headlineList.getSelectedValue(), newsWeek[0], ctx);
                    storyArea.setText(story != null ? story : headlineList.getSelectedValue());
                    storyArea.setCaretPosition(0);
                }
            }
        });

        // JList caches row heights; re-measure the wrapped rows on resize.
        headlineList.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                headlineList.setFixedCellHeight(10);
                headlineList.setFixedCellHeight(-1);
            }
        });
        JScrollPane headScroll = new JScrollPane(headlineList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        headScroll.setBorder(DesktopTheme.titledBorder("Headlines"));
        headScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        headScroll.setOpaque(true);
        JScrollPane storyScroll = new JScrollPane(storyArea);
        storyScroll.setBorder(DesktopTheme.titledBorder("Story"));
        storyScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        storyScroll.setOpaque(true);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, headScroll, storyScroll);
        split.setDividerLocation(440);
        split.setResizeWeight(0.4);
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
        panel.add(split, BorderLayout.CENTER);

        loadNewsForWeek.run();
        return panel;
    }

    private static String lookupStoryForWeek(String headline, int week, LeagueScreenContext ctx) {
        List<List<String>> stories = ctx.league().getNewsStories();
        if (stories == null || week < 0 || week >= stories.size()) return null;
        List<String> weekStories = stories.get(week);
        if (weekStories == null) return null;
        for (String story : weekStories) {
            String[] parts = story.split(">");
            if (parts.length >= 2 && headline.contains(parts[0].trim())) {
                return parts[1].trim();
            }
            if (parts.length >= 1 && headline.startsWith(parts[0].trim())) {
                return parts.length >= 2 ? parts[1].trim() : parts[0].trim();
            }
        }
        return null;
    }
}