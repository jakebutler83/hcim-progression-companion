package com.hcimprogression.companion;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** Standalone Swing facts only. No game client or game input. */
public class SlayerWikiPanelTest
{
    static SlayerWikiGuide greaterDemon()
    {
        // Public fields verified against OSRS Wiki revision 15315063.
        return SlayerWikiGuide.parse("Greater demon",15315063,
            "{{Infobox Monster\n|version1=Level 92\n|version2=Level 100\n|name=Greater demon\n"
            + "|combat1=92\n|combat2=100\n|hitpoints1=87\n|hitpoints2=115\n|att1=76\n|att2=90\n|str1=78\n|str2=70\n"
            + "|def1=81\n|def2=80\n|mage=1\n|range=1\n|max hit1=9\n|max hit2=8\n|attack style=[[Slash]]\n"
            + "|dstab=0\n|dslash=0\n|dcrush=0\n|dmagic=-10\n|dlight=0\n|dstandard=0\n|dheavy=0\n"
            + "|elementalweaknesstype=Water\n|elementalweaknesspercent=40\n}}\n"
            + "{{LocLine\n|location=[[Catacombs of Kourend]]\n}}\n{{LocLine\n|location=[[Chasm of Fire]]\n}}\n");
    }
    @Test public void firstVariantStatsWeaknessAndLocationsDisplayTogetherWithoutAnotherClick() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SlayerWikiPanel view = new SlayerWikiPanel(); view.show(greaterDemon(),"Wiki facts loaded",false);
            String text = text(view.panel());
            assertTrue(text.contains("87")); assertTrue(text.contains("Water · +40%"));
            assertTrue(text.contains("Catacombs of Kourend")); assertTrue(text.contains("Chasm of Fire"));
            assertTrue(text.contains("15315063"));
            JComboBox<?> variants = components(view.panel()).stream().filter(JComboBox.class::isInstance).map(JComboBox.class::cast).findFirst().get();
            variants.setSelectedIndex(1); text = text(view.panel()); assertTrue(text.contains("115")); assertFalse(text.contains("87"));
        });
    }
    @Test public void loadingAndErrorsDoNotLeaveEmptyVariantDropdownOrPreviousMonsterFacts() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SlayerWikiPanel view = new SlayerWikiPanel();
            view.show(greaterDemon(),"Ready",false); view.show(null,"Loading stats and locations…",true);
            assertFalse(text(view.panel()).contains("Catacombs")); assertTrue(text(view.panel()).contains("Loading stats"));
            view.show(null,"Wiki unavailable. Retry.",false); assertTrue(text(view.panel()).contains("Retry"));
            assertFalse(components(view.panel()).stream().filter(JComboBox.class::isInstance).map(JComboBox.class::cast).findFirst().get().isShowing());
        });
    }
    @Test public void populatedCardsAndAllThreePageButtonsFitNarrowWidths() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SlayerWikiPanel view = new SlayerWikiPanel(); view.show(greaterDemon(),"Ready",false);
            for (int width : new int[]{140,170,210})
            {
                layout(view.panel(),width); checkBounds(view.panel());
            }
            CompanionSidebarDeck deck = new CompanionSidebarDeck();
            JPanel small = SidebarControls.column(); small.add(SidebarControls.text("Small"));
            JPanel large = SidebarControls.column(); large.add(SidebarControls.scroll(SidebarControls.text("Large"),700));
            deck.page("Play",small); deck.page("Account",large); deck.page("Details",SidebarControls.column());
            layout(deck,170); int before = deck.getPreferredSize().height;
            deck.showPage("Account"); layout(deck,170); assertTrue(deck.getPreferredSize().height > before + 500);
            deck.showPage("Play"); layout(deck,170); assertTrue(deck.getPreferredSize().height < 200);
            for (Component component : components(deck)) if (component instanceof javax.swing.AbstractButton && component.isVisible())
            {
                javax.swing.AbstractButton button = (javax.swing.AbstractButton)component;
                assertTrue(button.getText(),button.getFontMetrics(button.getFont()).stringWidth(button.getText()) + button.getInsets().left + button.getInsets().right <= button.getWidth());
            }
        });
    }
    static void layout(JPanel root,int width)
    {
        root.setSize(width,2000);
        for (int i=0;i<4;i++) { layoutTree(root); root.setSize(width,root.getPreferredSize().height); }
    }
    private static void layoutTree(Container parent) { parent.doLayout(); for (Component c:parent.getComponents()) if (c instanceof Container) layoutTree((Container)c); }
    private static void checkBounds(Container parent)
    {
        for (Component child:parent.getComponents()) if (child.isVisible())
        {
            assertTrue(child.getClass().getSimpleName(),child.getX() >= 0 && child.getX()+child.getWidth() <= parent.getWidth());
            if (child instanceof JTextArea && !((JTextArea)child).getText().isEmpty())
                assertTrue("Visible facts need a nonzero layout area",child.getWidth() > 0 && child.getHeight() > 0);
            if (child instanceof Container) checkBounds((Container)child);
        }
    }
    private static List<Component> components(Container root)
    {
        List<Component> result = new ArrayList<>();
        for (Component child:root.getComponents()) { result.add(child); if (child instanceof Container) result.addAll(components((Container)child)); }
        return result;
    }
    private static String text(Container root)
    {
        StringBuilder result = new StringBuilder();
        for (Component child:components(root)) if (child instanceof JTextArea && visibleInTree(child,root)) result.append(((JTextArea)child).getText()).append('\n');
        return result.toString();
    }
    private static boolean visibleInTree(Component value,Container root)
    {
        while (value != null && value != root) { if (!value.isVisible()) return false; value = value.getParent(); } return value == root;
    }
}
