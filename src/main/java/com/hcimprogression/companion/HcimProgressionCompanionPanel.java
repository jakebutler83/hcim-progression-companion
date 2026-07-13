package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class HcimProgressionCompanionPanel extends PluginPanel
{
    private final JLabel statusValue = new JLabel("Waiting for login");
    private final JLabel sharingValue = new JLabel("Disabled");
    private final JLabel linkValue = new JLabel("Not linked");
    private final JLabel syncValue = new JLabel("Never");
    private final JLabel playerValue = new JLabel("—");
    private final JLabel worldValue = new JLabel("—");
    private final JLabel regionValue = new JLabel("—");
    private final JLabel coordinatesValue = new JLabel("—");
    private final JLabel planeValue = new JLabel("—");
    private final JTextField linkCodeField = new JTextField();
    private final JButton connectButton = new JButton("Connect");

    public HcimProgressionCompanionPanel(Consumer<String> linkHandler)
    {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("HCIM Progression", SwingConstants.CENTER);
        title.setForeground(new Color(247, 201, 72));
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel connectionPanel = new JPanel(new BorderLayout(6, 6));
        connectionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        connectionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(68, 130, 180)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JLabel connectionTitle = new JLabel("Website connection");
        connectionTitle.setForeground(new Color(125, 184, 219));
        linkCodeField.setToolTipText("One-time code generated in Website Settings");
        connectButton.addActionListener(e -> {
            String code = linkCodeField.getText().trim();
            if (!code.isEmpty()) linkHandler.accept(code);
        });
        connectionPanel.add(connectionTitle, BorderLayout.NORTH);
        connectionPanel.add(linkCodeField, BorderLayout.CENTER);
        connectionPanel.add(connectButton, BorderLayout.EAST);

        JPanel informationPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        informationPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        informationPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(122, 90, 50)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        informationPanel.add(createRow("Game status", statusValue));
        informationPanel.add(createRow("Website link", linkValue));
        informationPanel.add(createRow("Location sharing", sharingValue));
        informationPanel.add(createRow("Last sync", syncValue));
        informationPanel.add(createRow("Player", playerValue));
        informationPanel.add(createRow("World", worldValue));
        informationPanel.add(createRow("Region ID", regionValue));
        informationPanel.add(createRow("Coordinates", coordinatesValue));
        informationPanel.add(createRow("Plane", planeValue));

        content.add(connectionPanel, BorderLayout.NORTH);
        content.add(informationPanel, BorderLayout.CENTER);
        add(title, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createRow(String labelText, JLabel valueLabel)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.LIGHT_GRAY);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    public void setLinking(boolean linking)
    {
        connectButton.setEnabled(!linking);
        linkValue.setText(linking ? "Linking…" : linkValue.getText());
        linkValue.setForeground(new Color(246, 173, 85));
    }

    public void showLinked(String displayName)
    {
        connectButton.setEnabled(true);
        linkCodeField.setText("");
        linkValue.setText(displayName == null || displayName.isEmpty() ? "Linked" : displayName);
        linkValue.setForeground(new Color(104, 211, 145));
    }

    public void showLinkError(String message)
    {
        connectButton.setEnabled(true);
        linkValue.setText(message == null || message.isEmpty() ? "Link failed" : message);
        linkValue.setForeground(new Color(229, 107, 99));
    }

    public void showUnlinked()
    {
        connectButton.setEnabled(true);
        linkValue.setText("Not linked");
        linkValue.setForeground(new Color(246, 173, 85));
    }

    public void showSyncSuccess()
    {
        syncValue.setText("Just now");
        syncValue.setForeground(new Color(104, 211, 145));
    }

    public void showSyncError(String message)
    {
        syncValue.setText(message == null || message.isEmpty() ? "Failed" : message);
        syncValue.setForeground(new Color(229, 107, 99));
    }

    public void updatePlayerInformation(String player, int world, int regionId, int x, int y, int plane)
    {
        statusValue.setText("Connected");
        statusValue.setForeground(new Color(104, 211, 145));
        playerValue.setText(player);
        worldValue.setText(String.valueOf(world));
        regionValue.setText(String.valueOf(regionId));
        coordinatesValue.setText(x + ", " + y);
        planeValue.setText(String.valueOf(plane));
    }

    public void showLoggedOut()
    {
        statusValue.setText("Waiting for login");
        statusValue.setForeground(new Color(246, 173, 85));
        playerValue.setText("—");
        worldValue.setText("—");
        regionValue.setText("—");
        coordinatesValue.setText("—");
        planeValue.setText("—");
    }

    public void showSharingEnabled()
    {
        sharingValue.setText("Enabled");
        sharingValue.setForeground(new Color(104, 211, 145));
    }

    public void showSharingDisabled()
    {
        sharingValue.setText("Disabled");
        sharingValue.setForeground(new Color(246, 173, 85));
    }
}
