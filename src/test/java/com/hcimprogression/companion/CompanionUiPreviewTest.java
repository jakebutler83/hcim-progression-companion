package com.hcimprogression.companion;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.RuneLite;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen plugin widgets only. Never starts, reads or controls a game client. */
public class CompanionUiPreviewTest
{
    @Test public void renderStandaloneSidebarAndLoadedWikiFacts() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            HcimProgressionCompanionConfig config=new HcimProgressionCompanionConfig(){};
            SlayerPreparationHelper slayer=new SlayerPreparationHelper(null,null,null,null,null,null,null);
            HcimProgressionCompanionPanel main=new HcimProgressionCompanionPanel(code->{},()->{},()->{},
                new RoutineRouteHelper(null,config,null,null,null).panel(),new SessionGoalHelper(null,null,config,null).panel(),slayer.panel());
            main.showLinked("Example adventurer");
            expandSlayer(main);
            BufferedImage shell=render(main);
            SlayerWikiPanel facts=new SlayerWikiPanel(); facts.show(SlayerWikiPanelTest.greaterDemon(),"Wiki facts loaded · choose the exact variant",false);
            JPanel card=SidebarControls.card("Monster intelligence",facts.panel());
            BufferedImage loaded=render(card);
            assertEquals(240,shell.getWidth()); assertTrue(loaded.getHeight()>200);
            if ("1".equals(System.getenv("PROGRESSION_PATH_UI_PREVIEW")))
            {
                File folder=new File(RuneLite.RUNELITE_DIR,"progression-path-previews");
                try
                {
                    java.nio.file.Files.createDirectories(folder.toPath());
                    ImageIO.write(shell,"png",new File(folder,"slayer-sidebar-play.png"));
                    ImageIO.write(loaded,"png",new File(folder,"slayer-wiki-facts.png"));
                }
                catch(java.io.IOException error) { throw new IllegalStateException(error); }
            }
        });
    }
    private static BufferedImage render(JPanel panel)
    {
        SlayerWikiPanelTest.layout(panel,240);
        BufferedImage image=new BufferedImage(240,Math.max(1,panel.getHeight()),BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics(); graphics.setColor(new Color(23,26,31)); graphics.fillRect(0,0,image.getWidth(),image.getHeight());
        panel.printAll(graphics); graphics.dispose(); return image;
    }
    private static void expandSlayer(Container parent)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JButton && "fold-Slayer preparation".equals(child.getName())) ((JButton)child).doClick(0);
            if(child instanceof Container) expandSlayer((Container)child);
        }
    }
}
