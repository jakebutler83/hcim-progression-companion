package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsPresetControllerTest
{
    private static final String[] ACCOUNT = {"automaticAccountSyncEnabled", "weeklyLootTrackingEnabled", "personalBestSyncEnabled",
        "pinnedGoalEnabled", "routineRoutesSyncEnabled", "skillGoalSyncEnabled", "tcgCollectionSyncEnabled"};
    private static final String[] HELPERS = {"progressionPopupsEnabled", "routineRemindersEnabled", "routineRoutesEnabled",
        "livePackingChecksEnabled", "sessionSkillGoalsEnabled", "sessionSummaryEnabled"};
    private static final String[] ALERTS = {"progressionDesktopNotifications", "routineDesktopNotifications", "skillGoalDesktopNotifications"};
    private static final String[] SOCIAL = {"socialPresenceEnabled", "socialClanSyncEnabled", "clanSkillWeekSyncEnabled", "socialClanEventsSyncEnabled"};
    private static final String[] PRIVATE = {"personalBankSyncEnabled", "groupStorageSyncEnabled", "locationSharingEnabled"};

    private static final class Harness
    {
        String profile = "first";
        final Map<String, Map<String, String>> profiles = new HashMap<>();
        final List<String> writes = new ArrayList<>();
        final SettingsPresetController controller = new SettingsPresetController(key -> values().get(key), this::change, () -> profile);
        Map<String, String> values() { return profiles.computeIfAbsent(profile, ignored -> new HashMap<>()); }
        void change(String key, String value)
        {
            if (java.util.Objects.equals(value, values().get(key))) return;
            values().put(key, value);
            writes.add(key);
            // ConfigManager posts events synchronously. Exercise recursive
            // updates rather than relying on a mocked no-event configuration.
            controller.changed(key, value);
        }
        void assertFlags(boolean expected, String... keys)
        {
            for (String key : keys) assertEquals(key, expected, Boolean.parseBoolean(values().get(key)));
        }
        void mode(String selector, SettingsMode mode) { change(selector, mode.name()); }
    }

    @Test
    public void allNewModesAndExistingNetworkFeaturesAreOffByDefault()
    {
        HcimProgressionCompanionConfig config = new HcimProgressionCompanionConfig() {};
        assertEquals(SettingsMode.OFF, config.accountTrackingMode());
        assertEquals(SettingsMode.OFF, config.inGameHelpersMode());
        assertEquals(SettingsMode.OFF, config.desktopAlertsMode());
        assertEquals(SettingsMode.OFF, config.socialSharingMode());
        assertFalse(config.automaticAccountSyncEnabled()); assertFalse(config.weeklyLootTrackingEnabled());
        assertFalse(config.personalBestSyncEnabled()); assertFalse(config.pinnedGoalEnabled());
        assertFalse(config.routineRoutesSyncEnabled()); assertFalse(config.skillGoalSyncEnabled());
        assertFalse(config.tcgCollectionSyncEnabled()); assertFalse(config.socialPresenceEnabled());
        assertFalse(config.socialClanSyncEnabled()); assertFalse(config.clanSkillWeekSyncEnabled());
        assertFalse(config.socialClanEventsSyncEnabled()); assertFalse(config.personalBankSyncEnabled());
        assertFalse(config.groupStorageSyncEnabled()); assertFalse(config.locationSharingEnabled());
        Harness h = new Harness(); h.controller.initialize();
        assertEquals("OFF", h.values().get(SettingsPresetController.ACCOUNT));
        assertEquals(4, h.writes.size());
    }

    @Test
    public void migrationKeepsEveryExistingChoiceWithoutExpandingSharing()
    {
        Harness h = new Harness();
        h.values().put("automaticAccountSyncEnabled", "true");
        h.values().put("weeklyLootTrackingEnabled", "false");
        h.values().put("socialPresenceEnabled", "true");
        h.values().put("socialClanSyncEnabled", "false");
        h.values().put("personalBankSyncEnabled", "true");
        h.values().put("locationSharingEnabled", "false");
        h.values().put("questCompletionPopups", "false");
        h.values().put("apiBaseUrl", "https://example.invalid");
        h.values().put("deviceToken.character", "test-link-value");
        Map<String, String> original = new HashMap<>(h.values());
        h.controller.initialize();
        original.forEach((key, value) -> assertEquals(key, value, h.values().get(key)));
        assertEquals("CUSTOM", h.values().get(SettingsPresetController.ACCOUNT));
        assertEquals("CUSTOM", h.values().get(SettingsPresetController.SOCIAL));
        assertEquals(4, h.writes.size());
        for (String key : h.writes) assertTrue(key.endsWith("Mode"));
    }

    @Test
    public void accountPresetControlsItsFeaturesButNotOptionalSharingOrHelpers()
    {
        Harness h = new Harness(); h.controller.initialize();
        h.mode(SettingsPresetController.ACCOUNT, SettingsMode.ON);
        h.assertFlags(true, ACCOUNT); h.assertFlags(false, PRIVATE); h.assertFlags(false, SOCIAL);
        h.assertFlags(false, HELPERS); h.assertFlags(false, ALERTS);
        assertEquals("ON", h.values().get(SettingsPresetController.ACCOUNT));
        h.mode(SettingsPresetController.ACCOUNT, SettingsMode.OFF);
        h.assertFlags(false, ACCOUNT);
        assertEquals("OFF", h.values().get(SettingsPresetController.ACCOUNT));
    }

    @Test
    public void helperPresetDoesNotEnableUploadsOrUnmuteCustomPopupCategories()
    {
        Harness h = new Harness();
        h.values().put("questCompletionPopups", "false");
        h.values().put("progressionPopupSeconds", "12");
        h.values().put("compactProgressionOverlay", "true");
        h.controller.initialize();
        h.mode(SettingsPresetController.HELPERS, SettingsMode.ON);
        h.assertFlags(true, HELPERS); h.assertFlags(false, ACCOUNT); h.assertFlags(false, PRIVATE);
        h.assertFlags(false, SOCIAL); h.assertFlags(false, ALERTS);
        assertEquals("false", h.values().get("questCompletionPopups"));
        assertEquals("12", h.values().get("progressionPopupSeconds"));
        assertEquals("true", h.values().get("compactProgressionOverlay"));
        h.mode(SettingsPresetController.HELPERS, SettingsMode.OFF);
        h.assertFlags(false, HELPERS);
    }

    @Test
    public void desktopAndSocialPresetsStayWithinTheirConsentBoundaries()
    {
        Harness h = new Harness(); h.controller.initialize();
        h.mode(SettingsPresetController.ALERTS, SettingsMode.ON);
        h.assertFlags(true, ALERTS); h.assertFlags(false, HELPERS); h.assertFlags(false, ACCOUNT);
        h.mode(SettingsPresetController.SOCIAL, SettingsMode.ON);
        h.assertFlags(true, SOCIAL); h.assertFlags(false, PRIVATE); h.assertFlags(false, ACCOUNT);
        h.values().put("locationSharingEnabled", "true");
        h.mode(SettingsPresetController.SOCIAL, SettingsMode.OFF);
        h.assertFlags(false, SOCIAL);
        assertEquals("true", h.values().get("locationSharingEnabled"));
    }

    @Test
    public void detailedEditsUpdateTheSummaryWithoutChangingOtherFlags()
    {
        Harness h = new Harness(); h.controller.initialize();
        h.mode(SettingsPresetController.ACCOUNT, SettingsMode.ON);
        h.change("weeklyLootTrackingEnabled", "false");
        assertEquals("CUSTOM", h.values().get(SettingsPresetController.ACCOUNT));
        assertEquals("true", h.values().get("personalBestSyncEnabled"));
        h.change("weeklyLootTrackingEnabled", "true");
        assertEquals("ON", h.values().get(SettingsPresetController.ACCOUNT));
        int before = h.writes.size();
        h.mode(SettingsPresetController.ACCOUNT, SettingsMode.CUSTOM);
        assertEquals(before + 1, h.writes.size());
        h.assertFlags(true, ACCOUNT);
    }

    @Test
    public void profileSwitchCannotApplyAnOldOnPresetToNewFlags()
    {
        Harness h = new Harness(); h.controller.initialize();
        h.mode(SettingsPresetController.ACCOUNT, SettingsMode.ON);
        h.mode(SettingsPresetController.SOCIAL, SettingsMode.ON);
        h.profile = "second";
        h.values().put(SettingsPresetController.ACCOUNT, "ON");
        h.values().put(SettingsPresetController.SOCIAL, "ON");
        h.controller.changed(SettingsPresetController.ACCOUNT, "ON");
        h.assertFlags(false, ACCOUNT); h.assertFlags(false, SOCIAL); h.assertFlags(false, PRIVATE);
        assertEquals("OFF", h.values().get(SettingsPresetController.ACCOUNT));
        // Remaining profile-load events still contain the stale old selector.
        h.controller.changed(SettingsPresetController.SOCIAL, "ON");
        h.assertFlags(false, SOCIAL);
        h.profile = "first"; h.controller.initialize();
        h.assertFlags(true, ACCOUNT); h.assertFlags(true, SOCIAL);
    }

    @Test
    public void upgradesAndRestartsNeverFillInMissingFutureConsent()
    {
        Harness h = new Harness();
        for (String key : ACCOUNT) h.values().put(key, "true");
        h.values().remove("personalBestSyncEnabled");
        h.values().put(SettingsPresetController.ACCOUNT, "ON");
        h.controller.initialize();
        assertEquals("CUSTOM", h.values().get(SettingsPresetController.ACCOUNT));
        assertNull(h.values().get("personalBestSyncEnabled"));
        h.controller.initialize();
        assertNull(h.values().get("personalBestSyncEnabled"));
    }

    @Test
    public void invalidSelectorsAndUnrelatedKeysNeverEnableFeatures()
    {
        Harness h = new Harness(); h.controller.initialize();
        h.change(SettingsPresetController.ACCOUNT, "UNKNOWN");
        assertEquals("OFF", h.values().get(SettingsPresetController.ACCOUNT));
        h.change("apiBaseUrl", "https://example.invalid");
        h.change("lastTrackedBarrowsChest", "13");
        h.assertFlags(false, ACCOUNT); h.assertFlags(false, HELPERS); h.assertFlags(false, SOCIAL);
        assertFalse(h.controller.updating());
    }

    @Test
    public void quickControlsFitTheSettingsWidthWithoutOpeningRuneScape() throws Exception
    {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            for (String title : new String[]{"Account tracking", "In-game helpers", "Desktop alerts"})
            {
                javax.swing.JLabel label = new javax.swing.JLabel(title);
                label.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
                javax.swing.JComboBox<String> modes = new javax.swing.JComboBox<>(new String[]{"Off", "On", "Custom"});
                modes.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
                javax.swing.JPanel row = new javax.swing.JPanel(new java.awt.BorderLayout());
                row.add(label, java.awt.BorderLayout.CENTER); row.add(modes, java.awt.BorderLayout.EAST);
                for (int width : new int[]{190, 210, 225})
                {
                    row.setSize(width, 24); row.doLayout();
                    assertTrue(title, label.getWidth() >= label.getFontMetrics(label.getFont()).stringWidth(title));
                    assertTrue(title, modes.getX() + modes.getWidth() <= width);
                }
            }
        });
    }
}
