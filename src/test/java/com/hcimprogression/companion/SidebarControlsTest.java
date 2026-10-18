package com.hcimprogression.companion;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** Standalone Swing layout only. Never opens or interacts with RuneScape. */
public class SidebarControlsTest
{
    @Test public void hintsWrapAtTheAvailableWidthInsteadOfShiftingRight() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea text = SidebarControls.text("A long packing instruction with a travel note that needs several lines in a narrow sidebar.");
            text.setSize(210,100);int wide=text.getPreferredSize().height;
            text.setSize(110,100);assertTrue(text.getPreferredSize().height>wide);assertEquals(0f,text.getAlignmentX(),0f);
            text.setText("<img> untrusted & text");assertEquals("<img> untrusted & text",text.getText());
        });
    }
    @Test public void allThreeNavigationButtonsFitAndFoldsKeepDetailsHidden() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JPanel body=SidebarControls.column();body.add(SidebarControls.text("Very long note for manual review"));
            JPanel fold=SidebarControls.fold("Packing details",body);assertFalse(body.isVisible());
            JButton toggle=(JButton)fold.getComponent(0);toggle.doClick(0);assertTrue(body.isVisible());toggle.doClick(0);assertFalse(body.isVisible());
            JPanel row=SidebarControls.row(SidebarControls.button("Back","",false),SidebarControls.button("Next","",false),SidebarControls.button("Stop","",false));
            for(int width:new int[]{170,190,210}) {row.setSize(width,30);row.doLayout();for(Component item:row.getComponents()) assertTrue(item.getX()+item.getWidth()<=width);}
        });
    }
    @Test public void helperControlsStayInsideNarrowSidebarWithoutLaunchingAClient() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            HcimProgressionCompanionConfig config=new HcimProgressionCompanionConfig(){};
            RoutineRouteHelper routes=new RoutineRouteHelper(null,config,null,null,null);
            SessionGoalHelper goals=new SessionGoalHelper(null,null,config,null);
            SlayerPreparationHelper slayer=new SlayerPreparationHelper(null,null,null,null,null,null,null);
            for(JPanel root:new JPanel[]{routes.panel(),goals.panel(),slayer.panel()}) for(boolean expanded:new boolean[]{false,true}) {
                if(expanded)expandFolds(root);
                for(int width:new int[]{170,190,210}) {
                    root.setSize(width,3000);layout(root);root.setSize(width,root.getPreferredSize().height);layout(root);
                    for(Component item:root.getComponents()) if(item.isVisible()) {assertTrue(item.getX()>=0);assertTrue(item.getX()+item.getWidth()<=width);}
                    checkButtons(root);
                }
            }
        });
    }
    private static void layout(Container parent) {parent.doLayout();for(Component item:parent.getComponents())if(item instanceof Container)layout((Container)item);}
    private static void expandFolds(Container parent) {
        for(Component item:parent.getComponents()) {
            if(item instanceof JButton && item.getName()!=null && item.getName().startsWith("fold-"))((JButton)item).doClick(0);
            if(item instanceof Container)expandFolds((Container)item);
        }
    }
    private static void checkButtons(Container parent) {
        for(Component item:parent.getComponents())if(item.isVisible()) {
            if(item instanceof JButton) {JButton button=(JButton)item;if(!button.getText().isEmpty())assertTrue(button.getText(),button.getFontMetrics(button.getFont()).stringWidth(button.getText())+button.getInsets().left+button.getInsets().right<=button.getWidth());}
            if(item instanceof Container)checkButtons((Container)item);
        }
    }
}
