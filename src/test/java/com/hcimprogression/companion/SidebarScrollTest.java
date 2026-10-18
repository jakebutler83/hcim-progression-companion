package com.hcimprogression.companion;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen Swing only: no RuneScape window, inputs or account data. */
public class SidebarScrollTest
{
    @Test public void readOnlyLiveUpdatesDoNotDragTheOuterSidebarToTheirCaret() throws Exception
    {
        AtomicInteger requestedScrolls = new AtomicInteger();
        JPanel[] content = new JPanel[1]; JScrollPane[] scroll = new JScrollPane[1]; JTextArea[] status = new JTextArea[1];
        SwingUtilities.invokeAndWait(() -> {
            content[0] = new JPanel()
            {
                @Override public void scrollRectToVisible(Rectangle bounds) { requestedScrolls.incrementAndGet(); super.scrollRectToVisible(bounds); }
            };
            content[0].setLayout(new BoxLayout(content[0],BoxLayout.Y_AXIS));
            content[0].add(Box.createRigidArea(new Dimension(210,1000)));
            status[0] = SidebarControls.text("Waiting for a task update"); content[0].add(status[0]);
            content[0].add(Box.createRigidArea(new Dimension(210,500)));
            scroll[0] = new JScrollPane(content[0]); scroll[0].setSize(240,220); scroll[0].doLayout();
            content[0].setSize(210,1530); content[0].doLayout();
            BufferedImage image=new BufferedImage(240,220,BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=image.createGraphics(); scroll[0].printAll(graphics); graphics.dispose();
        });
        flushEdt();
        SwingUtilities.invokeAndWait(() -> {
            scroll[0].getViewport().setViewPosition(new Point(0,120)); requestedScrolls.set(0);
            status[0].setText("Wiki facts loaded. Select the exact variant; requirements and locations may differ.");
        });
        flushEdt();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Live status must not request caret-driven scrolling",0,requestedScrolls.get());
            assertEquals(120,scroll[0].getViewport().getViewPosition().y);
            scroll[0].getViewport().setViewPosition(new Point(0,500));
            assertEquals("Manual scrolling is not locked",500,scroll[0].getViewport().getViewPosition().y);
        });
    }
    @Test public void displayTextDoesNotTrackDocumentEditsButEditablePackingListsStillDo() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea display=SidebarControls.text("Read-only stats");
            assertEquals(DefaultCaret.NEVER_UPDATE,((DefaultCaret)display.getCaret()).getUpdatePolicy());
            JTextArea editor=new JTextArea(); SidebarControls.editor(editor,100);
            assertEquals(DefaultCaret.UPDATE_WHEN_ON_EDT,((DefaultCaret)editor.getCaret()).getUpdatePolicy());
        });
    }
    @Test public void repeatedUnchangedStatusDoesNotReplaceTheDocument() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea display=SidebarControls.text("Wiki facts loaded"); AtomicInteger changes=new AtomicInteger();
            display.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
            {
                public void insertUpdate(javax.swing.event.DocumentEvent event) { changes.incrementAndGet(); }
                public void removeUpdate(javax.swing.event.DocumentEvent event) { changes.incrementAndGet(); }
                public void changedUpdate(javax.swing.event.DocumentEvent event) { changes.incrementAndGet(); }
            });
            display.setText("Wiki facts loaded"); assertEquals(0,changes.get());
            display.setText("Task count changed"); assertTrue(changes.get() > 0);
        });
    }
    private static void flushEdt() throws Exception { SwingUtilities.invokeAndWait(() -> {}); SwingUtilities.invokeAndWait(() -> {}); }
}
