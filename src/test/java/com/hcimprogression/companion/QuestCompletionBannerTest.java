package com.hcimprogression.companion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.*;

/** Headless drawing/state tests only. Never starts RuneLite or interacts with the game. */
public class QuestCompletionBannerTest
{
    @Test public void questUsesExactlyOnePresentationAndKeepsItsConfiguredDuration()
    {
        ProgressionFeedback feedback = quest();
        ProgressionFeedback.View view = feedback.view(2000);
        assertTrue(view.questPopup());
        assertEquals(1000, view.popupStarted);
        assertEquals(9000, view.popupUntil);
        assertTrue(QuestCompletionOverlay.visible(view, true, true, true, false));
        assertFalse(QuestCompletionOverlay.visible(view, false, true, true, false));
        assertFalse(QuestCompletionOverlay.visible(view, true, false, true, false));
        assertFalse(QuestCompletionOverlay.visible(view, true, true, false, false));
        assertFalse(QuestCompletionOverlay.visible(view, true, true, true, true));
        assertFalse(feedback.view(9000).questPopup());
        assertEquals("", feedback.tick(9000));
    }

    @Test public void routineDiaryAndMilestoneKeepTheirOriginalPresentation()
    {
        for (String category : Arrays.asList("diary", "task"))
        {
            ProgressionFeedback feedback = new ProgressionFeedback();
            long session = feedback.session("one");
            feedback.baseline(session);
            feedback.accept(session, true, Collections.singletonList(new ProgressionFeedback.Event(category + ":1", "Complete")));
            feedback.tick(1000);
            assertFalse(feedback.view(1100).questPopup());
        }
        ProgressionFeedback feedback = new ProgressionFeedback();
        feedback.session("one");
        feedback.routine("Birdhouses ready");
        feedback.tick(1000);
        assertFalse(feedback.view(1100).questPopup());
    }

    @Test public void queuedQuestsGetFreshAnimationAndNeverReplayOnCharacterSwitch()
    {
        ProgressionFeedback feedback = quest();
        feedback.accept(feedback.session("one"), true,
            Collections.singletonList(new ProgressionFeedback.Event("quest:second", "A second quest")));
        assertEquals("", feedback.tick(8999));
        assertEquals("A second quest", feedback.tick(9000, 3));
        assertEquals(9000, feedback.view(9500).popupStarted);
        assertEquals(12000, feedback.view(9500).popupUntil);
        feedback.session("two");
        assertFalse(feedback.view(9500).questPopup());
        assertEquals("", feedback.tick(9500));
    }

    @Test public void filtersAndPauseClearActiveQuestWithoutReplay()
    {
        ProgressionFeedback feedback = quest();
        feedback.categories(false, true, true);
        assertFalse(feedback.view(1500).questPopup());
        feedback.categories(true, true, true);
        assertFalse(feedback.view(1600).questPopup());
        feedback = quest();
        feedback.muted(true);
        feedback.muted(false);
        assertFalse(feedback.view(1600).questPopup());
        assertEquals("", feedback.tick(9500));
    }

    @Test public void fadeAndCountdownHaveBoundedEndpoints()
    {
        assertEquals(0, QuestCompletionBanner.opacity(1000, 9000, 999), 0);
        assertEquals(0, QuestCompletionBanner.opacity(1000, 9000, 1000), 0);
        assertEquals(0.5, QuestCompletionBanner.opacity(1000, 9000, 1125), 0.001);
        assertEquals(1, QuestCompletionBanner.opacity(1000, 9000, 2000), 0);
        assertEquals(0.5, QuestCompletionBanner.opacity(1000, 9000, 8775), 0.001);
        assertEquals(0, QuestCompletionBanner.opacity(1000, 9000, 9000), 0);
        assertEquals(0, QuestCompletionBanner.opacity(1000, 1000, 1000), 0);
        assertEquals(1, QuestCompletionBanner.remaining(1000, 9000, 0), 0);
        assertEquals(0.5, QuestCompletionBanner.remaining(1000, 9000, 5000), 0);
        assertEquals(0, QuestCompletionBanner.remaining(1000, 9000, 10000), 0);
        assertEquals(0, QuestCompletionBanner.remaining(1000, 1000, 1000), 0);
    }

    @Test public void pixelWrappingFitsLongQuestNamesAtNarrowAndNormalWidths()
    {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setFont(FontManager.getRunescapeBoldFont());
            FontMetrics metrics = g.getFontMetrics();
            for (int width : new int[]{124, 144, 244})
                for (String title : new String[]{"Tale of the Righteous", "Recipe for Disaster: Freeing the Lumbridge Guide",
                    "W".repeat(100), "A quest with a \uD83C\uDF1F and another long title"})
                {
                    List<String> lines = QuestCompletionBanner.wrap(title, metrics, width);
                    assertFalse(lines.isEmpty());
                    for (String line : lines) assertTrue(line, metrics.stringWidth(line) <= width);
                    assertEquals(title.replace(" ", ""), String.join("", lines).replace(" ", ""));
                }
        }
        finally { g.dispose(); }
    }

    @Test public void drawingIsBoundedStableThroughFadeAndPreservesCallerGraphics()
    {
        QuestCompletionBanner banner = new QuestCompletionBanner();
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setColor(Color.MAGENTA);
            g.setClip(new Rectangle(0, 0, 400, 400));
            g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
            Object font = g.getFont(), stroke = g.getStroke(), composite = g.getComposite(), clip = g.getClipBounds();
            for (int width : new int[]{220, 240, 340})
            {
                String title = "Recipe for Disaster: Freeing the Lumbridge Guide";
                Dimension start = banner.render(g, title, width, 1000, 9000, 1000);
                Dimension middle = banner.render(g, title, width, 1000, 9000, 2000);
                assertEquals(start, middle);
                assertEquals(width, middle.width);
                assertTrue(middle.height >= 94 && middle.height < 200);
                FontMetrics titleMetrics = g.getFontMetrics(FontManager.getRunescapeBoldFont());
                int titleRows = QuestCompletionBanner.wrap(title, titleMetrics, width - 96).size();
                int titleBottom = 35 + titleRows * titleMetrics.getHeight();
                int footerTop = middle.height - 18 - g.getFontMetrics(FontManager.getRunescapeSmallFont()).getAscent();
                assertTrue("Long titles must not touch the footer", footerTop >= titleBottom + 6);
                assertEquals(font, g.getFont());
                assertEquals(stroke, g.getStroke());
                assertEquals(composite, g.getComposite());
                assertEquals(clip, g.getClipBounds());
                assertEquals(Color.MAGENTA, g.getColor());
            }
            for (int y = 0; y < 400; y++) for (int x = 340; x < 400; x++) assertEquals(0, image.getRGB(x, y));
        }
        finally { g.dispose(); }
    }

    @Test public void optionalArtworkPreviewNeverNeedsAGameClient() throws Exception
    {
        if (!"1".equals(System.getenv("PROGRESSION_PATH_QUEST_PREVIEW"))) return;
        BufferedImage image = new BufferedImage(740, 420, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setColor(new Color(25, 28, 28));
            g.fillRect(0, 0, 740, 420);
            String[] titles = {"Tale of the Righteous", "Recipe for Disaster: Freeing the Lumbridge Guide", "Song of the Elves"};
            for (int i = 0; i < titles.length; i++)
            {
                Graphics2D sample = (Graphics2D) g.create(20, 14 + i * 132, 700, 130);
                try
                {
                    new QuestCompletionBanner().render(sample, titles[i], 340, 1000, 9000, 2000);
                    sample.translate(375, 0);
                    new QuestCompletionBanner().render(sample, titles[i], 240, 1000, 9000, 2000);
                }
                finally { sample.dispose(); }
            }
        }
        finally { g.dispose(); }
        File directory = new File(RuneLite.RUNELITE_DIR, "progression-path-qa");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        assertTrue(ImageIO.write(image, "png", new File(directory, "quest-completion-preview.png")));
        BufferedImage single = new BufferedImage(380, 130, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sample = single.createGraphics();
        try
        {
            sample.translate(20, 12);
            new QuestCompletionBanner().render(sample, "Tale of the Righteous", 340, 1000, 9000, 2000);
        }
        finally { sample.dispose(); }
        assertTrue(ImageIO.write(single, "png", new File(directory, "quest-completion-banner.png")));
    }

    private static ProgressionFeedback quest()
    {
        ProgressionFeedback feedback = new ProgressionFeedback();
        long session = feedback.session("one");
        feedback.baseline(session);
        feedback.accept(session, true, Collections.singletonList(new ProgressionFeedback.Event("quest:first", "Tale of the Righteous")));
        feedback.tick(1000);
        return feedback;
    }
}
