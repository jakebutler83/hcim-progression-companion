package com.hcimprogression.companion;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** EDT-only, plain-text Wiki facts. Never renders downloaded HTML. */
final class SlayerWikiPanel
{
    private final JPanel root = SidebarControls.column();
    private final JTextArea state = SidebarControls.text("Start a task, then load its optional Wiki facts. Stats and locations load together.");
    private final JComboBox<SlayerWikiGuide.Variant> variants = new JComboBox<>();
    private final JPanel facts = SidebarControls.column();
    private final JPanel statsBody = SidebarControls.column();
    private final JPanel locationBody = SidebarControls.column();
    private final JTextArea source = SidebarControls.text("");
    private final SidebarControls.Fold stats = SidebarControls.fold("Stats & weaknesses", statsBody);
    private final SidebarControls.Fold locations = SidebarControls.fold("Locations", locationBody);
    private SlayerWikiGuide current;

    SlayerWikiPanel()
    {
        root.add(state); SidebarControls.gap(root);
        SidebarControls.field(variants); variants.setToolTipText("Choose the exact monster level / variant");
        facts.add(SidebarControls.text("Monster variant")); facts.add(variants); SidebarControls.gap(facts);
        facts.add(stats); SidebarControls.gap(facts); facts.add(locations); SidebarControls.gap(facts);
        source.setFont(SidebarControls.FONT.deriveFont(10f)); facts.add(source);
        root.add(facts); facts.setVisible(false);
        variants.addActionListener(event -> renderVariant());
    }
    JPanel panel() { return root; }
    void show(SlayerWikiGuide guide, String message, boolean loading)
    {
        state.setText(message);
        state.setForeground(loading ? SidebarControls.GOLD : guide == null ? SidebarControls.MUTED : new Color(113,206,158));
        if (guide == current) return;
        current = guide; variants.removeAllItems(); statsBody.removeAll(); locationBody.removeAll();
        facts.setVisible(guide != null);
        if (guide != null)
        {
            for (SlayerWikiGuide.Variant variant : guide.variants) variants.addItem(variant);
            // Explicit render as well as the selection event: never depend on
            // a particular Look-and-Feel firing an event for the first item.
            renderVariant();
            locationBody.add(SidebarControls.text("Possible locations; your assignment's area restriction still applies."));
            for (String place : guide.locations.split("\n"))
            {
                SidebarControls.gap(locationBody);
                JTextArea row = SidebarControls.text("• " + place);
                row.setForeground(place.toLowerCase(java.util.Locale.ROOT).contains("wilderness") ? new Color(237,160,115) : Color.LIGHT_GRAY);
                locationBody.add(row);
            }
            SidebarControls.gap(locationBody);
            locationBody.add(SidebarControls.text("Open the Wiki page for access requirements and travel instructions."));
            source.setText("OSRS Wiki contributors · revision " + guide.revision + "\n" + guide.title);
            stats.setExpanded(true); locations.setExpanded(true);
        }
        root.revalidate(); root.repaint();
    }
    private void renderVariant()
    {
        SlayerWikiGuide.Variant selected = (SlayerWikiGuide.Variant) variants.getSelectedItem();
        if (current == null || selected == null) return;
        statsBody.removeAll(); Map<String, String> values = selected.fields;
        String element = values.get("Elemental weakness");
        String percent = values.get("Elemental weakness bonus (%)");
        JTextArea weakness = SidebarControls.heading(element == null ? values.getOrDefault("Weakness", "No explicit weakness listed")
            : element + (percent == null ? " weakness" : " · +" + percent + "% weakness"));
        statsBody.add(weakness); SidebarControls.gap(statsBody);
        JPanel metrics = new JPanel(new GridLayout(0,2,6,6)); metrics.setOpaque(false); SidebarControls.stretch(metrics);
        for (String key : new String[]{"Combat", "HP", "Attack", "Strength", "Defence", "Magic", "Ranged", "Max hit"})
        {
            if (!values.containsKey(key)) continue;
            JPanel cell = SidebarControls.column();
            JTextArea number = SidebarControls.heading(values.get(key)); number.setForeground(Color.WHITE);
            number.setFont(SidebarControls.FONT.deriveFont(Font.BOLD,14f)); cell.add(number);
            cell.add(SidebarControls.text(key)); metrics.add(cell);
        }
        statsBody.add(metrics); SidebarControls.gap(statsBody);
        for (String key : new String[]{"Attack style", "Slayer required"}) if (values.containsKey(key)) statsBody.add(SidebarControls.text(key + ": " + values.get(key)));
        JPanel defence = SidebarControls.column();
        for (Map.Entry<String,String> field : values.entrySet()) if (field.getKey().endsWith("defence")) defence.add(SidebarControls.text(field.getKey() + ": " + field.getValue()));
        defence.add(SidebarControls.text("Defence bonuses are not a DPS ranking. Match the correct level / variant."));
        statsBody.add(SidebarControls.fold("Defence details", defence));
        statsBody.revalidate(); statsBody.repaint();
    }
}
