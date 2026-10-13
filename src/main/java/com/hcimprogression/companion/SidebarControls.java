package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.View;

/** Width-constrained sidebar widgets. Never reads or operates the game. */
final class SidebarControls
{
    static final Color MUTED = new Color(165,165,165), GOLD = new Color(235,194,93);
    static JPanel column()
    {
        JPanel result = new JPanel(); result.setOpaque(false);
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
        result.setFont(result.getFont().deriveFont(11f)); result.setToolTipText(tooltip);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE,30)); result.setPreferredSize(new Dimension(1,30));
        result.setMargin(new Insets(3,4,3,4)); result.setFocusPainted(false);
        result.setForeground(Color.WHITE); result.setBackground(primary ? new Color(53,116,73) : new Color(48,48,48));
        result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(67,67,67)),BorderFactory.createEmptyBorder(4,4,4,4)));
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
        result.setFont(result.getFont().deriveFont(11f)); result.setBorder(BorderFactory.createEmptyBorder()); stretch(result); return result;
    }
    static JScrollPane editor(JTextArea field, int height)
    {
        field.setRows(0); field.setColumns(0); field.setLineWrap(true); field.setWrapStyleWord(true);
        field.setFont(field.getFont().deriveFont(11f)); field.setForeground(Color.WHITE); field.setCaretColor(Color.WHITE);
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
    static JPanel fold(String title, JComponent body)
    {
        JPanel result = new JPanel(new BorderLayout(0,6)); result.setOpaque(false); stretch(result);
        JButton toggle = button("▸ " + title,"Expand " + title,false); toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setName("fold-"+title); body.setVisible(false);
        toggle.addActionListener(event -> {
            body.setVisible(!body.isVisible()); toggle.setText((body.isVisible()?"▾ ":"▸ ")+title);
            toggle.setToolTipText((body.isVisible()?"Collapse ":"Expand ")+title); result.revalidate(); result.repaint();
        });
        result.add(toggle,BorderLayout.NORTH); result.add(body,BorderLayout.CENTER); return result;
    }
    private static final class WrappedText extends JTextArea
    {
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
