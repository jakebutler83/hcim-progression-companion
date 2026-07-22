package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    private static final Color SUCCESS = new Color(104, 211, 145);
    private static final Color WARNING = new Color(246, 173, 85);
    private static final Color ERROR = new Color(229, 107, 99);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final JLabel statusValue = new JLabel("Waiting for login");
    private final JLabel sharingValue = new JLabel("Disabled");
    private final JLabel linkValue = new JLabel("Not linked");
    private final JLabel locationSyncValue = new JLabel("Never");
    private final JLabel socialPresenceValue = new JLabel("Waiting");
    private final JLabel socialRegionValue = new JLabel("—");
    private final JLabel socialActivityValue = new JLabel("—");
    private final JLabel socialCombatValue = new JLabel("—");
    private final JLabel socialGearValue = new JLabel("—");
    private final JLabel accountSyncStatusValue = new JLabel("Never synced");
    private final JLabel questsUpdatedValue = new JLabel("—");
    private final JLabel tasksUpdatedValue = new JLabel("—");
    private final JLabel accountSyncTimeValue = new JLabel("—");
    private final JLabel collectionCaptureValue = new JLabel("Open clue pages");
    private final JLabel clueCountsValue = new JLabel("—");
    private final JLabel playerValue = new JLabel("—");
    private final JLabel worldValue = new JLabel("—");
    private final JLabel regionValue = new JLabel("—");
    private final JLabel coordinatesValue = new JLabel("—");
    private final JLabel planeValue = new JLabel("—");
    private final JTextField linkCodeField = new JTextField();
    private final JButton connectButton = new JButton("Connect");
    private final JButton accountSyncButton = new JButton("Sync Account Now");

    public HcimProgressionCompanionPanel(Consumer<String> linkHandler, Runnable accountSyncHandler)
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
            if (!code.isEmpty())
            {
                linkHandler.accept(code);
            }
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
        informationPanel.add(createRow("Location sync", locationSyncValue));
        informationPanel.add(createRow("Social Hub presence", socialPresenceValue));
        informationPanel.add(createRow("Social region", socialRegionValue));
        informationPanel.add(createRow("Current activity", socialActivityValue));
        informationPanel.add(createRow("Combat level", socialCombatValue));
        informationPanel.add(createRow("Worn gear slots", socialGearValue));
        informationPanel.add(createRow("Player", playerValue));
        informationPanel.add(createRow("World", worldValue));
        informationPanel.add(createRow("Region ID", regionValue));
        informationPanel.add(createRow("Coordinates", coordinatesValue));
        informationPanel.add(createRow("Plane", planeValue));

        JPanel accountSyncPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        accountSyncPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        accountSyncPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 145, 92)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JLabel accountSyncTitle = new JLabel("Account sync results");
        accountSyncTitle.setForeground(new Color(133, 214, 151));
        accountSyncPanel.add(accountSyncTitle);
        accountSyncPanel.add(createRow("Status", accountSyncStatusValue));
        accountSyncPanel.add(createRow("Quests processed", questsUpdatedValue));
        accountSyncPanel.add(createRow("Skill tasks processed", tasksUpdatedValue));
        accountSyncPanel.add(createRow("Last account sync", accountSyncTimeValue));
        accountSyncPanel.add(createRow("Collection Log", collectionCaptureValue));
        accountSyncPanel.add(createRow("Clue totals", clueCountsValue));

        accountSyncButton.setToolTipText("Upload skills, quests, captured clue totals, and captured Collection Log items");
        accountSyncButton.addActionListener(e -> accountSyncHandler.run());
        accountSyncPanel.add(accountSyncButton);

        content.add(connectionPanel, BorderLayout.NORTH);
        content.add(informationPanel, BorderLayout.CENTER);
        content.add(accountSyncPanel, BorderLayout.SOUTH);
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
        linkValue.setForeground(WARNING);
    }

    public void showLinked(String displayName)
    {
        connectButton.setEnabled(true);
        linkCodeField.setText("");
        linkValue.setText(displayName == null || displayName.isEmpty() ? "Linked" : displayName);
        linkValue.setForeground(SUCCESS);
    }

    public void showLinkError(String message)
    {
        connectButton.setEnabled(true);
        linkValue.setText(message == null || message.isEmpty() ? "Link failed" : message);
        linkValue.setForeground(ERROR);
    }

    public void showUnlinked()
    {
        connectButton.setEnabled(true);
        linkValue.setText("Not linked");
        linkValue.setForeground(WARNING);
    }

    public void setAccountSyncing(boolean syncing)
    {
        accountSyncButton.setEnabled(!syncing);
        accountSyncButton.setText(syncing ? "Syncing…" : "Sync Account Now");
        if (syncing)
        {
            accountSyncStatusValue.setText("Reading account…");
            accountSyncStatusValue.setForeground(WARNING);
        }
    }

    public void showAccountSyncSuccess(int quests, int tasks)
    {
        setAccountSyncing(false);
        accountSyncStatusValue.setText("Synced successfully");
        accountSyncStatusValue.setForeground(SUCCESS);
        questsUpdatedValue.setText(String.valueOf(quests));
        questsUpdatedValue.setForeground(SUCCESS);
        tasksUpdatedValue.setText(String.valueOf(tasks));
        tasksUpdatedValue.setForeground(SUCCESS);
        accountSyncTimeValue.setText(LocalTime.now().format(TIME_FORMAT));
        accountSyncTimeValue.setForeground(SUCCESS);
    }

    public void showAccountSyncError(String message)
    {
        setAccountSyncing(false);
        accountSyncStatusValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        accountSyncStatusValue.setForeground(ERROR);
    }

    public void showCollectionLogCapture(String pageTitle, int pageItems, int totalPages, int totalItems, Map<String, Integer> clueCounts)
    {
        collectionCaptureValue.setText(totalPages + " pages / " + totalItems + " items");
        collectionCaptureValue.setToolTipText("Last captured: " + pageTitle + " (" + pageItems + " obtained items)");
        collectionCaptureValue.setForeground(SUCCESS);

        StringBuilder counts = new StringBuilder();
        String[] tiers = {"beginner", "easy", "medium", "hard", "elite", "master"};
        for (String tier : tiers)
        {
            Integer value = clueCounts.get(tier);
            if (value == null) continue;
            if (counts.length() > 0) counts.append(" | ");
            counts.append(Character.toUpperCase(tier.charAt(0))).append(':').append(value);
        }
        clueCountsValue.setText(counts.length() == 0 ? "Open each clue page" : counts.toString());
        clueCountsValue.setToolTipText(counts.toString());
        clueCountsValue.setForeground(counts.length() == 0 ? WARNING : SUCCESS);
    }


    public void showSocialPresenceSyncing(String region, String activity, int combatLevel, int gearSlots)
    {
        socialPresenceValue.setText("Syncing…");
        socialPresenceValue.setForeground(WARNING);
        socialRegionValue.setText(region == null || region.isEmpty() ? "Gielinor" : region);
        socialActivityValue.setText(activity == null || activity.isEmpty() ? "Exploring" : activity);
        socialCombatValue.setText(String.valueOf(combatLevel));
        socialGearValue.setText(String.valueOf(gearSlots));
    }

    public void showSocialPresenceSuccess()
    {
        socialPresenceValue.setText("Live");
        socialPresenceValue.setForeground(SUCCESS);
    }

    public void showSocialPresenceError(String message)
    {
        socialPresenceValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        socialPresenceValue.setForeground(ERROR);
    }

    public void showSocialPresenceDisabled()
    {
        socialPresenceValue.setText("Disabled in settings");
        socialPresenceValue.setForeground(WARNING);
    }

    public void showSyncSuccess()
    {
        locationSyncValue.setText("Just now");
        locationSyncValue.setForeground(SUCCESS);
    }

    public void showSyncError(String message)
    {
        locationSyncValue.setText(message == null || message.isEmpty() ? "Failed" : message);
        locationSyncValue.setForeground(ERROR);
    }

    public void updatePlayerInformation(String player, int world, int regionId, int x, int y, int plane)
    {
        statusValue.setText("Connected");
        statusValue.setForeground(SUCCESS);
        playerValue.setText(player);
        worldValue.setText(String.valueOf(world));
        regionValue.setText(String.valueOf(regionId));
        coordinatesValue.setText(x + ", " + y);
        planeValue.setText(String.valueOf(plane));
    }

    public void showLoggedOut()
    {
        statusValue.setText("Waiting for login");
        statusValue.setForeground(WARNING);
        playerValue.setText("—");
        worldValue.setText("—");
        regionValue.setText("—");
        coordinatesValue.setText("—");
        planeValue.setText("—");
        socialPresenceValue.setText("Waiting for login");
        socialPresenceValue.setForeground(WARNING);
        socialRegionValue.setText("—");
        socialActivityValue.setText("—");
        socialCombatValue.setText("—");
        socialGearValue.setText("—");
    }

    public void showSharingEnabled()
    {
        sharingValue.setText("Enabled");
        sharingValue.setForeground(SUCCESS);
    }

    public void showSharingDisabled()
    {
        sharingValue.setText("Disabled");
        sharingValue.setForeground(WARNING);
    }
}
