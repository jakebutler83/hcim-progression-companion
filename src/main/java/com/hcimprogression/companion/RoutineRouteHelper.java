package com.hcimprogression.companion;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;

/** User-driven routine checklist. No scene scans, movement, menus or game input. */
final class RoutineRouteHelper
{
    private final ConfigManager manager;
    private final HcimProgressionCompanionConfig config;
    private final SyncService service;
    private final Gson gson;
    private final ClientThread clientThread;
    private final JPanel panel = new JPanel();
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"Farm run", "Birdhouse run"});
    private final JTextArea items = new JTextArea(6, 18), stops = new JTextArea(7, 18);
    private final JLabel status = new JLabel();
    private final JButton download = new JButton("Load from website"), upload = new JButton("Save to website");
    private final Set<String> loaded = new HashSet<>();
    private volatile String session = "", name = "", token = "", api = "";
    private volatile long generation;
    private volatile View view;
    private String selectedKind = "farm";
    private String editorSession = "", editorName = "";
    private RoutineRoutePlan plan = RoutineRoutePlan.defaults("farm");
    private final RoutineRouteRun run = new RoutineRouteRun();
    private boolean busy;

    RoutineRouteHelper(ConfigManager manager, HcimProgressionCompanionConfig config, SyncService service, Gson gson, ClientThread clientThread)
    {
        this.manager = manager; this.config = config; this.service = service; this.gson = gson; this.clientThread = clientThread;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.add(kind);
        panel.add(new JLabel("Packing list · one item per line")); panel.add(area(items));
        panel.add(new JLabel("Stops · location | travel note")); panel.add(area(stops));
        JButton local = new JButton("Save locally"), start = new JButton("Start / restart route");
        JButton next = new JButton("Next"), back = new JButton("Back"), stop = new JButton("Stop");
        JPanel navigation = new JPanel(); navigation.add(back); navigation.add(next); navigation.add(stop);
        panel.add(local); panel.add(start); panel.add(navigation); panel.add(download); panel.add(upload); panel.add(status);
        kind.addActionListener(event -> {
            if (!session.isEmpty() && session.equals(editorSession)) saveLocal();
            run.stop(); view = null;
            selectedKind = kind.getSelectedIndex() == 0 ? "farm" : "birdhouse";
            loadLocal();
        });
        local.addActionListener(event -> { if (saveLocal()) message("Saved for this RuneScape profile and character. Website unchanged."); });
        start.addActionListener(event -> {
            if (!config.routineRoutesEnabled()) { message("Enable Local route helper in plugin settings first."); return; }
            if (!saveLocal()) return;
            run.start(plan); updateView(); message("Check the packing list. Next begins the first stop; steps are manual.");
        });
        next.addActionListener(event -> {
            if (!run.active()) return;
            run.next();
            if (!run.active()) { view = null; message("Route checklist complete. No farming timers or game actions were changed."); }
            else updateView();
        });
        back.addActionListener(event -> { if (run.active()) { run.back(); updateView(); } });
        stop.addActionListener(event -> { run.stop(); view = null; message("Route stopped. Your setup is still saved locally."); });
        download.addActionListener(event -> sync(false)); upload.addActionListener(event -> sync(true));
        loadLocal();
    }

    private JScrollPane area(JTextArea field)
    {
        field.setLineWrap(true); field.setWrapStyleWord(true);
        field.setBackground(ColorScheme.DARK_GRAY_COLOR); field.setForeground(Color.WHITE);
        JScrollPane pane = new JScrollPane(field); pane.setMaximumSize(new Dimension(230, 150)); return pane;
    }
    JPanel panel() { return panel; }

    /** Called on the client thread. Stale EDT and network work is generation-guarded. */
    void session(String nextName, String nextToken, String nextApi)
    {
        String next = nextName.isEmpty() ? "" : nextName + ":" + nextToken + ":" + nextApi;
        name = nextName; token = nextToken; api = nextApi;
        if (next.equals(session)) return;
        session = next; long revision = ++generation; view = null;
        SwingUtilities.invokeLater(() -> {
            if (revision != generation) return;
            editorSession = session; editorName = name;
            run.stop(); busy = false; loaded.clear(); kind.setEnabled(true); download.setEnabled(true); upload.setEnabled(true); loadLocal();
        });
    }
    void reset() { session("", "", ""); }
    private String key() { return "route_" + editorName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "_") + "_" + selectedKind; }

    private void loadLocal()
    {
        plan = RoutineRoutePlan.defaults(selectedKind);
        if (!session.isEmpty()) {
            try {
                RoutineRoutePlan saved = gson.fromJson(manager.getRSProfileConfiguration("hcimprogression.routes", key()), RoutineRoutePlan.class);
                if (saved != null && selectedKind.equals(saved.kind) && saved.valid()) plan = saved;
            } catch (RuntimeException ignored) { /* Keep safe local defaults for malformed legacy data. */ }
        }
        items.setText(session.isEmpty() ? "" : plan.items); stops.setText(session.isEmpty() ? "" : plan.stops);
        items.setEnabled(!session.isEmpty()); stops.setEnabled(!session.isEmpty());
        message(session.isEmpty() ? "Log into RuneScape to edit a local route." : "Review levels, quests and travel unlocks. Suggestions do not verify access. Local editing needs no website connection.");
    }
    private boolean saveLocal()
    {
        if (session.isEmpty() || !session.equals(editorSession)) { message("Character changed. Wait for this character’s route to load."); return false; }
        RoutineRoutePlan next = new RoutineRoutePlan(selectedKind, items.getText(), stops.getText(), plan.revision);
        if (!next.valid()) { message("Use 1–24 stops and up to 32 items, at most 160 characters per line."); return false; }
        plan = next; manager.setRSProfileConfiguration("hcimprogression.routes", key(), gson.toJson(plan)); return true;
    }
    private void sync(boolean save)
    {
        if (busy) return;
        if (!config.routineRoutesSyncEnabled() || session.isEmpty() || !session.equals(editorSession) || token.isEmpty()) { message("Link this character and enable Route setup sync in settings. Local routes work without it."); return; }
        if (save && !loaded.contains(selectedKind)) { message("Load from website once before uploading, so newer saved edits are not overwritten."); return; }
        if (save && !saveLocal()) return;
        if (!save && (!items.getText().equals(plan.items) || !stops.getText().equals(plan.stops))
            && JOptionPane.showConfirmDialog(panel, "Load the website route and replace unsaved editor changes?", "Load route", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        long revision = generation; String expectedSession = session, selected = selectedKind;
        RoutineRoutePlan sending = new RoutineRoutePlan(selected, plan.items, plan.stops, plan.revision);
        String expectedName = name, expectedToken = token, expectedApi = api;
        String originalItems = items.getText(), originalStops = stops.getText();
        busy = true; kind.setEnabled(false); download.setEnabled(false); upload.setEnabled(false); message(save ? "Saving route to website…" : "Loading website route…");
        clientThread.invokeLater(() -> {
            if (revision != generation || !expectedSession.equals(session) || !config.routineRoutesSyncEnabled()) {
                SwingUtilities.invokeLater(() -> { if (revision == generation) { busy = false; kind.setEnabled(true); download.setEnabled(true); upload.setEnabled(true); message("Route sync cancelled."); } }); return;
            }
            service.syncRoutineRoute(expectedApi, expectedToken, expectedName, selected, save ? sending : null, (result, error) -> SwingUtilities.invokeLater(() -> {
                if (revision != generation || !expectedSession.equals(session)) return;
                busy = false; kind.setEnabled(true); download.setEnabled(true); upload.setEnabled(true);
                if (!config.routineRoutesSyncEnabled()) { message("Route sync disabled. Local route kept."); return; }
                if (error != null) { message(error); return; }
                loaded.add(selected);
                if (!selected.equals(selectedKind)) { message("Route synced. Select that route again to review it."); return; }
                if (save) { plan.revision = result.revision; saveLocal(); message("Saved to this character’s website route."); }
                else {
                    if (!originalItems.equals(items.getText()) || !originalStops.equals(stops.getText())) {
                        loaded.remove(selected); message("You edited during loading. Those changes were kept; load again when ready."); return;
                    }
                    plan = result == null ? RoutineRoutePlan.defaults(selected) : result;
                    items.setText(plan.items); stops.setText(plan.stops); saveLocal(); run.stop(); view = null;
                    message(result == null ? "No website route yet. Edit this starter setup and Save to website." : "Website route loaded. Review packing items, then Start route.");
                }
            }));
        });
    }
    private void updateView()
    {
        if (!run.active()) { view = null; return; }
        view = new View(run.title(), run.text());
        message(run.position() < 0 ? "Check the packing list above, then Next." : run.text() + " — Next after you finish this stop.");
    }
    View view() { return config.routineRoutesEnabled() && !session.isEmpty() ? view : null; }
    private void message(String text)
    {
        String clean = String.valueOf(text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        status.setText("<html><body style='width: 185px'>" + clean + "</body></html>");
    }
    static final class View { final String title, text; View(String title, String text) {this.title = title; this.text = text;} }
}
