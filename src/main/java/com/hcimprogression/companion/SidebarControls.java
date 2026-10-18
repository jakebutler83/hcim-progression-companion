package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Cursor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;
import javax.swing.text.View;

/** Width-constrained sidebar widgets. Never reads or operates the game. */
final class SidebarControls
{
    static final Color MUTED = new Color(164,173,184), GOLD = new Color(235,194,93);
    static final Color SURFACE = new Color(29,33,39), INPUT = new Color(22,25,30), LINE = new Color(54,61,71);
    static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    static JPanel column()
    {
        JPanel result = new JPanel()
        {
            @Override public Dimension getMaximumSize()
            {
                // BoxLayout's empty maximum height is zero. Do not freeze that
                // value before controls are added; nested cards would disappear.
                return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);
            }
        };
        result.setOpaque(false);
        result.setLayout(new BoxLayout(result,BoxLayout.Y_AXIS)); stretch(result); return result;
    }
    static void stretch(JComponent component)
    {
        component.setAlignmentX(0f);
        component.setMinimumSize(new Dimension(0,component.getMinimumSize().height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE,component.getMaximumSize().height));
    }
    static void gap(JPanel panel) { panel.add(Box.createVerticalStrut(6)); }
    static JButton button(String title, String tooltip, boolean primary)
    {
        JButton result = new JButton(title); stretch(result);
        result.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        result.setFont(FONT.deriveFont(11f)); result.setToolTipText(tooltip);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE,30)); result.setPreferredSize(new Dimension(1,30));
        result.setMargin(new Insets(3,4,3,4)); result.setFocusPainted(false);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        result.setForeground(Color.WHITE); result.setBackground(primary ? new Color(45,100,76) : new Color(39,45,54));
        result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? new Color(71,143,109) : LINE),BorderFactory.createEmptyBorder(4,4,4,4)));
        return result;
    }
    static JPanel row(JComponent... controls)
    {
        JPanel result = new JPanel(new GridLayout(1,controls.length,4,0)); result.setOpaque(false); stretch(result);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        for (JComponent control : controls) result.add(control); return result;
    }
    static JTextArea text(String value)
    {
        JTextArea result = new WrappedText(); result.setText(value); result.setEditable(false); result.setFocusable(false);
        result.setLineWrap(true); result.setWrapStyleWord(true); result.setOpaque(false); result.setForeground(MUTED);
        result.setFont(FONT.deriveFont(11f)); result.setBorder(BorderFactory.createEmptyBorder()); stretch(result); return result;
    }
    static JTextArea heading(String value)
    {
        JTextArea result = text(value); result.setFont(FONT.deriveFont(Font.BOLD, 12f)); result.setForeground(GOLD); return result;
    }
    static JPanel card(String title, JComponent body)
    {
        JPanel result = column(); result.setOpaque(true); result.setBackground(SURFACE);
        result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(8,8,8,8)));
        if (!title.isEmpty()) { result.add(heading(title)); gap(result); }
        result.add(body); return result;
    }
    static void field(JComponent field)
    {
        stretch(field); field.setFont(FONT); field.setBackground(INPUT); field.setForeground(Color.WHITE);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        if (field instanceof javax.swing.JTextField)
            field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(4,6,4,6)));
    }
    static JScrollPane editor(JTextArea field, int height)
    {
        field.setRows(0); field.setColumns(0); field.setLineWrap(true); field.setWrapStyleWord(true);
        field.setFont(FONT.deriveFont(11f)); field.setForeground(Color.WHITE); field.setCaretColor(Color.WHITE);
        field.setBackground(new Color(36,36,36)); field.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return scroll(field,height);
    }
    static JScrollPane scroll(JComponent body, int height)
    {
        JScrollPane pane = new JScrollPane(body,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        stretch(pane); pane.setPreferredSize(new Dimension(1,height)); pane.setMinimumSize(new Dimension(0,height));
        pane.setMaximumSize(new Dimension(Integer.MAX_VALUE,height)); pane.setBorder(BorderFactory.createLineBorder(new Color(58,58,58)));
        return pane;
    }
    static Fold fold(String title, JComponent body)
    {
        return new Fold(title, body);
    }
    static final class Fold extends JPanel
    {
        private final JButton toggle;
        private final JComponent body;
        private final JPanel inset = column();
        private final String title;
        Fold(String title, JComponent body)
        {
            super(new BorderLayout()); this.title = title; this.body = body;
            setOpaque(true); setBackground(SURFACE); stretch(this);
            setBorder(BorderFactory.createLineBorder(LINE));
            toggle = button("", "", false); toggle.setHorizontalAlignment(SwingConstants.LEFT);
            toggle.setFont(FONT.deriveFont(Font.BOLD,10f)); toggle.setForeground(GOLD);
            toggle.setName("fold-" + title);
            inset.setBorder(BorderFactory.createEmptyBorder(7,7,8,7)); inset.add(body);
            add(toggle,BorderLayout.NORTH); add(inset,BorderLayout.CENTER);
            toggle.addActionListener(event -> setExpanded(!body.isVisible())); setExpanded(false);
        }
        void setExpanded(boolean value)
        {
            body.setVisible(value); inset.setVisible(value);
            toggle.setText((value ? "▾ " : "▸ ") + title);
            toggle.setToolTipText((value ? "Collapse " : "Expand ") + title); revalidate(); repaint();
        }
    }
    private static final class WrappedText extends JTextArea
    {
        WrappedText()
        {
            // These are labels, not editors. Even a non-focusable JTextArea's
            // default caret scrolls its ancestors when background text changes.
            DefaultCaret stationary = new DefaultCaret()
            {
                @Override protected void adjustVisibility(java.awt.Rectangle bounds)
                {
                    // Display-only updates must never move the user's viewport.
                }
            };
            stationary.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            setCaret(stationary);
        }
        @Override public void setText(String value)
        {
            String next = value == null ? "" : value;
            if (getDocument() != null && next.equals(getText())) return;
            super.setText(next);
        }
        @Override public Dimension getPreferredSize()
        {
            if (getUI() == null) return new Dimension(1,16);
            int width = getWidth() > 0 ? getWidth() : getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 190;
            Insets insets = getInsets(); View root = getUI().getRootView(this);
            root.setSize(Math.max(1,width-insets.left-insets.right),Integer.MAX_VALUE);
            return new Dimension(1,(int)Math.ceil(root.getPreferredSpan(View.Y_AXIS))+insets.top+insets.bottom);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE,getPreferredSize().height); }
    }
}
