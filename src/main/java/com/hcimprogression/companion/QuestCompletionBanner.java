package com.hcimprogression.companion;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.FontManager;

/** Original vector artwork. No network, sprites, game access, or per-frame layout rebuilds. */
final class QuestCompletionBanner
{
    static final int MIN_WIDTH = 220;
    private static final int TEXT_X = 76;
    private static final int RIGHT_PAD = 20;
    private static final Color GOLD = new Color(230, 191, 105);
    private static final Color GREEN = new Color(138, 229, 167);
    private static final Font TITLE = FontManager.getRunescapeBoldFont();
    private static final Font LABEL = FontManager.getRunescapeSmallFont();
    private String cachedTitle = "";
    private int cachedWidth;
    private List<String> lines = Collections.emptyList();

    Dimension render(Graphics2D graphics, String title, int width, long started, long until, long now)
    {
        if (width < MIN_WIDTH) throw new IllegalArgumentException("Quest banner is too narrow");
        // A private graphics context preserves the caller's font, clip, composite and stroke.
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(TITLE);
            FontMetrics metrics = g.getFontMetrics();
            if (!cachedTitle.equals(title) || cachedWidth != width)
            {
                cachedTitle = title;
                cachedWidth = width;
                lines = wrap(title, metrics, width - TEXT_X - RIGHT_PAD);
            }
            int lineHeight = metrics.getHeight();
            int height = Math.max(94, 76 + lines.size() * lineHeight);
            g.setComposite(AlphaComposite.SrcOver.derive(opacity(started, until, now)));
            g.setColor(new Color(0, 0, 0, 95));
            g.fillRoundRect(3, 8, width - 6, height - 8, 12, 12);
            g.setPaint(new GradientPaint(0, 6, new Color(35, 47, 38, 245), width, height,
                new Color(15, 20, 19, 245)));
            g.fillRoundRect(1, 4, width - 4, height - 9, 10, 10);
            g.setColor(new Color(161, 129, 65));
            g.drawRoundRect(1, 4, width - 4, height - 9, 10, 10);
            g.setColor(new Color(230, 191, 105, 40));
            g.drawRoundRect(5, 8, width - 12, height - 17, 7, 7);
            g.setColor(GOLD);
            g.drawLine(16, 4, 46, 4);
            g.drawLine(width - 48, 4, width - 18, 4);

            crest(g, height / 2);
            g.setFont(LABEL);
            shadowText(g, "QUEST COMPLETE", TEXT_X, 26, GOLD);
            g.setFont(TITLE);
            int baseline = 35 + metrics.getAscent();
            for (String line : lines)
            {
                shadowText(g, line, TEXT_X, baseline, new Color(247, 239, 214));
                baseline += lineHeight;
            }
            g.setFont(LABEL);
            shadowText(g, "PROGRESSION PATH", TEXT_X, height - 18, new Color(149, 170, 145));
            g.setColor(new Color(71, 79, 59));
            g.fillRect(TEXT_X, height - 12, width - TEXT_X - RIGHT_PAD, 2);
            g.setColor(GOLD);
            g.fillRect(TEXT_X, height - 12,
                (int) Math.round((width - TEXT_X - RIGHT_PAD) * remaining(started, until, now)), 2);
            return new Dimension(width, height);
        }
        finally
        {
            g.dispose();
        }
    }

    private static void shadowText(Graphics2D g, String text, int x, int y, Color color)
    {
        g.setColor(new Color(0, 0, 0, 190));
        g.drawString(text, x + 1, y + 1);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static void crest(Graphics2D g, int y)
    {
        g.setPaint(new GradientPaint(16, y - 24, new Color(62, 86, 53), 62, y + 24, new Color(17, 35, 28)));
        g.fillOval(15, y - 25, 48, 48);
        g.setColor(GOLD);
        g.drawOval(15, y - 25, 48, 48);
        g.setColor(new Color(230, 191, 105, 65));
        g.drawOval(19, y - 21, 40, 40);
        // A small parchment and emerald check: intentionally independent of game assets.
        g.setColor(new Color(215, 199, 157));
        g.fillRoundRect(30, y - 15, 18, 24, 3, 3);
        g.setColor(new Color(116, 94, 56));
        g.drawLine(34, y - 9, 43, y - 9);
        g.drawLine(34, y - 4, 43, y - 4);
        Path2D check = new Path2D.Float();
        check.moveTo(35, y + 6);
        check.lineTo(40, y + 11);
        check.lineTo(51, y - 1);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(16, 43, 28));
        g.draw(check);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(GREEN);
        g.draw(check);
        g.setStroke(new BasicStroke(1));
    }

    static float opacity(long started, long until, long now)
    {
        if (until <= started || now < started || now >= until) return 0;
        return (float) Math.max(0, Math.min(1, Math.min((now - started) / 250.0, (until - now) / 450.0)));
    }

    static double remaining(long started, long until, long now)
    {
        if (until <= started) return 0;
        return Math.max(0, Math.min(1, (double) (until - now) / (until - started)));
    }

    /** Pixel-measured wrapping, including unusually long words; titles are never truncated. */
    static List<String> wrap(String title, FontMetrics metrics, int width)
    {
        List<String> output = new ArrayList<>();
        String text = ProgressionFeedback.clean(title);
        while (!text.isEmpty())
        {
            if (metrics.stringWidth(text) <= width)
            {
                output.add(text);
                break;
            }
            int end = 0;
            int space = -1;
            for (int offset = 0; offset < text.length();)
            {
                int next = offset + Character.charCount(text.codePointAt(offset));
                if (metrics.stringWidth(text.substring(0, next)) > width) break;
                if (text.charAt(offset) == ' ') space = offset;
                end = next;
                offset = next;
            }
            if (space > 0) end = space;
            if (end == 0) end = Character.charCount(text.codePointAt(0));
            output.add(text.substring(0, end));
            text = text.substring(end).trim();
        }
        return output;
    }
}
