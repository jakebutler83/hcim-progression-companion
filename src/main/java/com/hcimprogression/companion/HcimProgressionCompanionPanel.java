package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class HcimProgressionCompanionPanel extends PluginPanel
{
    private final JLabel statusValue = new JLabel("Waiting for login");
    private final JLabel sharingValue = new JLabel("Disabled");
    private final JLabel playerValue = new JLabel("—");
    private final JLabel worldValue = new JLabel("—");
    private final JLabel regionValue = new JLabel("—");
    private final JLabel coordinatesValue = new JLabel("—");
    private final JLabel planeValue = new JLabel("—");

    public HcimProgressionCompanionPanel()
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel(
                "HCIM Progression",
                SwingConstants.CENTER
        );

        title.setForeground(new Color(247, 201, 72));
        title.setBorder(
                BorderFactory.createEmptyBorder(4, 0, 12, 0)
        );

        JPanel informationPanel =
                new JPanel(new GridLayout(0, 1, 0, 8));

        informationPanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        informationPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(122, 90, 50)
                        ),
                        BorderFactory.createEmptyBorder(
                                10, 10, 10, 10
                        )
                )
        );

        informationPanel.add(
                createRow("Status", statusValue)
        );

        informationPanel.add(
                createRow("Location sharing", sharingValue)
        );

        informationPanel.add(
                createRow("Player", playerValue)
        );

        informationPanel.add(
                createRow("World", worldValue)
        );

        informationPanel.add(
                createRow("Region ID", regionValue)
        );

        informationPanel.add(
                createRow("Coordinates", coordinatesValue)
        );

        informationPanel.add(
                createRow("Plane", planeValue)
        );

        add(title, BorderLayout.NORTH);
        add(informationPanel, BorderLayout.CENTER);
    }

    private JPanel createRow(
            String labelText,
            JLabel valueLabel)
    {
        JPanel row = new JPanel(
                new BorderLayout(8, 0)
        );

        row.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.LIGHT_GRAY);

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);

        return row;
    }

    public void updatePlayerInformation(
            String player,
            int world,
            int regionId,
            int x,
            int y,
            int plane)
    {
        statusValue.setText("Connected");
        statusValue.setForeground(
                new Color(104, 211, 145)
        );

        playerValue.setText(player);
        worldValue.setText(String.valueOf(world));
        regionValue.setText(String.valueOf(regionId));
        coordinatesValue.setText(x + ", " + y);
        planeValue.setText(String.valueOf(plane));
    }

    public void showLoggedOut()
    {
        statusValue.setText("Waiting for login");
        statusValue.setForeground(
                new Color(246, 173, 85)
        );

        playerValue.setText("—");
        worldValue.setText("—");
        regionValue.setText("—");
        coordinatesValue.setText("—");
        planeValue.setText("—");
    }

    public void showSharingEnabled()
    {
        sharingValue.setText("Enabled");
        sharingValue.setForeground(
                new Color(104, 211, 145)
        );
    }

    public void showSharingDisabled()
    {
        sharingValue.setText("Disabled");
        sharingValue.setForeground(
                new Color(246, 173, 85)
        );
    }
}