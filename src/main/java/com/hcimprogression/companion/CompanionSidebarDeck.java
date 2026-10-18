package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Only the selected page participates in layout; no giant blank tab heights. */
final class CompanionSidebarDeck extends JPanel
{
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel navigation = new JPanel(new GridLayout(1,0,4,0));
    private final ButtonGroup group = new ButtonGroup();
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();
    CompanionSidebarDeck()
    {
        super(new BorderLayout(0,10)); setOpaque(false); SidebarControls.stretch(this);
        content.setOpaque(false); navigation.setOpaque(false);
        add(navigation,BorderLayout.NORTH); add(content,BorderLayout.CENTER);
    }
    void page(String name, JPanel page)
    {
        pages.put(name,page);
        JToggleButton button = new JToggleButton(name);
        button.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI()); button.setContentAreaFilled(false); button.setOpaque(true);
        button.setBackground(SidebarControls.SURFACE); button.setForeground(Color.LIGHT_GRAY);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setName("page-" + name); button.setFont(SidebarControls.FONT.deriveFont(11f)); button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(1,32)); button.setToolTipText("Show " + name);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SidebarControls.LINE),BorderFactory.createEmptyBorder(6,2,6,2)));
        group.add(button); buttons.put(name,button); navigation.add(button);
        button.addActionListener(event -> showPage(name));
        if (pages.size() == 1) showPage(name);
    }
    void showPage(String name)
    {
        if (!pages.containsKey(name)) return;
        content.removeAll(); content.add(pages.get(name), BorderLayout.NORTH);
        buttons.forEach((key, button) -> {
            boolean selected = key.equals(name); button.setSelected(selected);
            button.setBackground(selected ? new Color(81,66,35) : SidebarControls.SURFACE);
            button.setForeground(selected ? SidebarControls.GOLD : Color.LIGHT_GRAY);
        });
        revalidate(); repaint();
    }
}
