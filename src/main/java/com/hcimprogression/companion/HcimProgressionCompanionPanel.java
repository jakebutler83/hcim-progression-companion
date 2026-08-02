package com.hcimprogression.companion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
    private static final Color GOLD = new Color(247, 201, 72);
    private static final Color BLUE = new Color(93, 173, 226);
    private static final Color MUTED = new Color(165, 165, 165);
    private static final Color CARD_BACKGROUND = new Color(31, 31, 31);
    private static final Color INPUT_BACKGROUND = new Color(24, 24, 24);
    private static final Color DIVIDER = new Color(58, 58, 58);
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
    private final JLabel clanRosterValue = new JLabel("Waiting");
    private final JLabel clanEventsValue = new JLabel("Open Clan Events");
    private final JLabel groupStorageValue = new JLabel("Disabled");
    private final JLabel personalBankValue = new JLabel("Disabled");
    private final JLabel accountSyncStatusValue = new JLabel("Never synced");
    private final JLabel questsUpdatedValue = new JLabel("—");
    private final JLabel tasksUpdatedValue = new JLabel("—");
    private final JLabel accountSyncTimeValue = new JLabel("—");
    private final JLabel collectionCaptureValue = new JLabel("Open clue pages");
    private final JLabel clueCountsValue = new JLabel("—");
    private final JLabel tcgValue = new JLabel("Disabled");
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
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = createVerticalPanel(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(12, 8, 14, 8));

        content.add(createHeader());
        content.add(Box.createVerticalStrut(10));

        JPanel connectionBody = createVerticalPanel(CARD_BACKGROUND);
        connectionBody.add(createRow("Link status", linkValue));
        connectionBody.add(Box.createVerticalStrut(8));

        linkCodeField.setToolTipText("One-time code generated in Website Settings");
        linkCodeField.setBackground(INPUT_BACKGROUND);
        linkCodeField.setForeground(Color.WHITE);
        linkCodeField.setCaretColor(Color.WHITE);
        linkCodeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)
        ));
        linkCodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        linkCodeField.setAlignmentX(LEFT_ALIGNMENT);
        connectionBody.add(linkCodeField);
        connectionBody.add(Box.createVerticalStrut(6));

        styleButton(connectButton, false);
        Runnable submitLink = () -> {
            String code = linkCodeField.getText().trim();
            if (!code.isEmpty())
            {
                linkHandler.accept(code);
            }
        };
        connectButton.addActionListener(e -> submitLink.run());
        linkCodeField.addActionListener(e -> submitLink.run());
        connectionBody.add(connectButton);
        connectionBody.add(Box.createVerticalStrut(6));
        connectionBody.add(createHint("Paste the one-time code from Website Settings."));
        content.add(createCard("WEBSITE CONNECTION", BLUE, connectionBody));
        content.add(Box.createVerticalStrut(8));

        JPanel accountBody = createVerticalPanel(CARD_BACKGROUND);
        configureValueLabel(accountSyncStatusValue, SwingConstants.CENTER);
        accountSyncStatusValue.setFont(accountSyncStatusValue.getFont().deriveFont(Font.BOLD));
        accountSyncStatusValue.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER),
            BorderFactory.createEmptyBorder(7, 5, 7, 5)
        ));
        JPanel accountStatus = new JPanel(new BorderLayout());
        accountStatus.setBackground(CARD_BACKGROUND);
        accountStatus.setAlignmentX(LEFT_ALIGNMENT);
        accountStatus.add(accountSyncStatusValue, BorderLayout.CENTER);
        accountBody.add(accountStatus);
        accountBody.add(Box.createVerticalStrut(7));

        JPanel metrics = new JPanel(new GridLayout(1, 2, 6, 0));
        metrics.setBackground(CARD_BACKGROUND);
        metrics.setAlignmentX(LEFT_ALIGNMENT);
        metrics.add(createMetric("QUESTS", questsUpdatedValue));
        metrics.add(createMetric("TASKS", tasksUpdatedValue));
        accountBody.add(metrics);
        accountBody.add(Box.createVerticalStrut(5));
        accountBody.add(createRow("Last sync", accountSyncTimeValue));
        accountBody.add(createRow("Collection Log", collectionCaptureValue));
        accountBody.add(createRow("Clue totals", clueCountsValue));
        accountBody.add(createRow("OSRS TCG", tcgValue));
        accountBody.add(Box.createVerticalStrut(8));

        styleButton(accountSyncButton, true);
        accountSyncButton.setToolTipText("Upload skills, quests, captured clue totals, and captured Collection Log items");
        accountSyncButton.addActionListener(e -> accountSyncHandler.run());
        accountBody.add(accountSyncButton);
        content.add(createCollapsibleCard("ACCOUNT SYNC", SUCCESS, accountBody, true));
        content.add(Box.createVerticalStrut(8));

        JPanel liveSyncBody = createVerticalPanel(CARD_BACKGROUND);
        liveSyncBody.add(createRow("Location sharing", sharingValue));
        liveSyncBody.add(createRow("Location update", locationSyncValue));
        liveSyncBody.add(createRow("Social presence", socialPresenceValue));
        liveSyncBody.add(createRow("Clan roster", clanRosterValue));
        liveSyncBody.add(createRow("Clan events", clanEventsValue));
        liveSyncBody.add(createRow("Group storage", groupStorageValue));
        liveSyncBody.add(createRow("Personal bank", personalBankValue));
        content.add(createCollapsibleCard("LIVE SYNC", GOLD, liveSyncBody, true));
        content.add(Box.createVerticalStrut(8));

        JPanel characterBody = createVerticalPanel(CARD_BACKGROUND);
        characterBody.add(createRow("Player", playerValue));
        characterBody.add(createRow("World", worldValue));
        characterBody.add(createRow("Map region", regionValue));
        characterBody.add(createRow("Coordinates", coordinatesValue));
        characterBody.add(createRow("Plane", planeValue));
        content.add(createCollapsibleCard("CHARACTER DETAILS", BLUE, characterBody, false));
        content.add(Box.createVerticalStrut(8));

        JPanel socialBody = createVerticalPanel(CARD_BACKGROUND);
        socialBody.add(createRow("Region", socialRegionValue));
        socialBody.add(createRow("Activity", socialActivityValue));
        socialBody.add(createRow("Combat level", socialCombatValue));
        socialBody.add(createRow("Equipment slots", socialGearValue));
        content.add(createCollapsibleCard("SOCIAL DETAILS", new Color(177, 126, 216), socialBody, false));
        content.add(Box.createVerticalStrut(9));
        content.add(createHint("Only features enabled in the plugin settings are shared."));

        add(content, BorderLayout.NORTH);
    }

    private JPanel createHeader()
    {
        JPanel header = createVerticalPanel(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("PROGRESSION PATH");
        title.setForeground(GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);

        JLabel subtitle = new JLabel("Progression Path companion");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(7));

        configureValueLabel(statusValue, SwingConstants.LEFT);
        statusValue.setFont(statusValue.getFont().deriveFont(Font.BOLD, 11f));
        header.add(statusValue);
        return header;
    }

    private JPanel createVerticalPanel(Color background)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel createCard(String titleText, Color accent, JPanel body)
    {
        JPanel card = createVerticalPanel(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, accent),
            BorderFactory.createEmptyBorder(9, 10, 10, 10)
        ));

        JLabel title = new JLabel(titleText);
        title.setForeground(accent);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 10f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(7));
        card.add(body);
        return card;
    }

    private JPanel createCollapsibleCard(String titleText, Color accent, JPanel body, boolean expanded)
    {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BACKGROUND);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, accent));

        JButton toggle = new JButton();
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setForeground(accent);
        toggle.setFont(toggle.getFont().deriveFont(Font.BOLD, 10f));
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setMargin(new Insets(0, 0, 0, 0));
        toggle.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));

        body.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        body.setVisible(expanded);
        updateToggleText(toggle, titleText, expanded);
        toggle.addActionListener(e -> {
            boolean showBody = !body.isVisible();
            body.setVisible(showBody);
            updateToggleText(toggle, titleText, showBody);
            card.revalidate();
            card.repaint();
        });

        card.add(toggle, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void updateToggleText(JButton button, String title, boolean expanded)
    {
        button.setText((expanded ? "\u25BE  " : "\u25B8  ") + title);
        button.setToolTipText((expanded ? "Collapse " : "Expand ") + title.toLowerCase());
    }

    private JPanel createRow(String labelText, JLabel valueLabel)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(CARD_BACKGROUND);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JLabel label = new JLabel(labelText);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(11f));
        configureValueLabel(valueLabel, SwingConstants.RIGHT);
        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JPanel createMetric(String labelText, JLabel valueLabel)
    {
        JPanel metric = createVerticalPanel(INPUT_BACKGROUND);
        metric.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
        configureValueLabel(valueLabel, SwingConstants.CENTER);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 15f));
        metric.add(valueLabel);

        JLabel label = new JLabel(labelText, SwingConstants.CENTER);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 9f));
        label.setAlignmentX(CENTER_ALIGNMENT);
        metric.add(Box.createVerticalStrut(2));
        metric.add(label);
        return metric;
    }

    private JLabel createHint(String text)
    {
        JLabel hint = new JLabel("<html><body style='width: 185px'>" + text + "</body></html>");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setAlignmentX(LEFT_ALIGNMENT);
        return hint;
    }

    private void configureValueLabel(JLabel valueLabel, int alignment)
    {
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        valueLabel.setHorizontalAlignment(alignment);
        valueLabel.setAlignmentX(alignment == SwingConstants.CENTER ? CENTER_ALIGNMENT : LEFT_ALIGNMENT);
        valueLabel.setToolTipText(valueLabel.getText());
        valueLabel.addPropertyChangeListener("text", e -> valueLabel.setToolTipText(String.valueOf(e.getNewValue())));
    }

    private void styleButton(JButton button, boolean primary)
    {
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(6, 10, 6, 10));
        button.setForeground(Color.WHITE);
        button.setBackground(primary ? new Color(53, 116, 73) : new Color(55, 83, 104));
        button.setBorder(BorderFactory.createLineBorder(primary ? new Color(75, 151, 97) : new Color(76, 117, 146)));
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
        showUnlinked(null);
    }

    public void showUnlinked(String playerName)
    {
        connectButton.setEnabled(true);
        linkValue.setText(playerName == null || playerName.isEmpty() ? "Not linked" : "Link " + playerName);
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

    public void showAccountSyncSuccess(int quests, int tasks, int bossKillCounts)
    {
        setAccountSyncing(false);
        accountSyncStatusValue.setText("Synced \u2022 " + bossKillCounts + " boss KCs");
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

    public void showTcgStatus(TcgCollectionSnapshot snapshot, boolean enabled)
    {
        if (!enabled)
        {
            tcgValue.setText("Disabled");
            tcgValue.setForeground(MUTED);
            return;
        }
        if (snapshot == null || !snapshot.isAvailable())
        {
            tcgValue.setText("No save found");
            tcgValue.setForeground(WARNING);
            return;
        }
        tcgValue.setText(snapshot.getUniqueOwned() + " unique / " + snapshot.getTotalCardsOwned() + " cards");
        tcgValue.setToolTipText(String.format("%.1f%% complete • %d foil uniques", snapshot.getCompletionPct(), snapshot.getUniqueFoilOwned()));
        tcgValue.setForeground(SUCCESS);
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


    public void showClanRosterSuccess(int members)
    {
        clanRosterValue.setText(members + " members");
        clanRosterValue.setForeground(SUCCESS);
    }

    public void showClanRosterError(String message)
    {
        clanRosterValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        clanRosterValue.setForeground(ERROR);
    }

    public void showClanRosterDisabled()
    {
        clanRosterValue.setText("Disabled in settings");
        clanRosterValue.setForeground(WARNING);
    }

    public void showClanEventsSyncing(int events)
    {
        clanEventsValue.setText("Syncing " + events + "…");
        clanEventsValue.setForeground(WARNING);
    }

    public void showClanEventsSuccess(int events)
    {
        clanEventsValue.setText(events + " events · " + LocalTime.now().format(TIME_FORMAT));
        clanEventsValue.setForeground(SUCCESS);
        clanEventsValue.setToolTipText("Open Clan Settings → Events again to force a fresh import.");
    }

    public void showClanEventsWaiting()
    {
        clanEventsValue.setText("Open Clan Settings → Events");
        clanEventsValue.setForeground(WARNING);
    }

    public void showClanEventsError(String message)
    {
        clanEventsValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        clanEventsValue.setForeground(ERROR);
    }

    public void showClanEventsDisabled()
    {
        clanEventsValue.setText("Disabled in settings");
        clanEventsValue.setForeground(WARNING);
    }

    public void showGroupStorageWaiting()
    {
        groupStorageValue.setText("Open Group Storage");
        groupStorageValue.setForeground(WARNING);
    }

    public void showGroupStorageSyncing(int occupiedSlots)
    {
        groupStorageValue.setText("Syncing " + occupiedSlots + " slots...");
        groupStorageValue.setForeground(WARNING);
    }

    public void showGroupStorageSuccess(int occupiedSlots)
    {
        groupStorageValue.setText(occupiedSlots + " slots - " + LocalTime.now().format(TIME_FORMAT));
        groupStorageValue.setForeground(SUCCESS);
        groupStorageValue.setToolTipText("The website now has the latest Group Storage snapshot.");
    }

    public void showGroupStorageError(String message)
    {
        groupStorageValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        groupStorageValue.setForeground(ERROR);
    }

    public void showGroupStorageDisabled()
    {
        groupStorageValue.setText("Disabled in settings");
        groupStorageValue.setForeground(WARNING);
    }

    public void showPersonalBankWaiting()
    {
        personalBankValue.setText("Open your bank");
        personalBankValue.setForeground(WARNING);
    }

    public void showPersonalBankSyncing(int occupiedSlots)
    {
        personalBankValue.setText("Syncing " + occupiedSlots + " slots...");
        personalBankValue.setForeground(WARNING);
    }

    public void showPersonalBankSuccess(int occupiedSlots)
    {
        personalBankValue.setText(occupiedSlots + " slots - " + LocalTime.now().format(TIME_FORMAT));
        personalBankValue.setForeground(SUCCESS);
        personalBankValue.setToolTipText("The website now has the latest Personal Bank snapshot.");
    }

    public void showPersonalBankError(String message)
    {
        personalBankValue.setText(message == null || message.isEmpty() ? "Sync failed" : message);
        personalBankValue.setForeground(ERROR);
    }

    public void showPersonalBankDisabled()
    {
        personalBankValue.setText("Disabled in settings");
        personalBankValue.setForeground(WARNING);
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
        clanRosterValue.setText("Waiting for login");
        clanRosterValue.setForeground(WARNING);
        groupStorageValue.setText("Waiting for login");
        groupStorageValue.setForeground(WARNING);
        personalBankValue.setText("Waiting for login");
        personalBankValue.setForeground(WARNING);
        clanEventsValue.setText("Open Clan Settings → Events");
        clanEventsValue.setForeground(WARNING);
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
