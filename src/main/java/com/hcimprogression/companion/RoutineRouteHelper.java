package com.hcimprogression.companion;

import com.google.gson.Gson;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/** User-driven routine checklist. No scene scans, movement, menus or game input. */
final class RoutineRouteHelper
{
    private final ConfigManager manager;
    private final HcimProgressionCompanionConfig config;
    private final SyncService service;
    private final Gson gson;
    private final ClientThread clientThread;
    private final JPanel panel = SidebarControls.column();
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"Farm run", "Birdhouse run"});
    private final JTextArea items = new JTextArea(6, 18), stops = new JTextArea(7, 18);
    private final JTextArea status = SidebarControls.text("");
    private final JTextArea packing = SidebarControls.text("");
    private final JPanel packingRows = SidebarControls.column();
    private final JButton start = SidebarControls.button("Start route","Start or restart the manual route checklist",true);
    private final JButton next = SidebarControls.button("Next","Next manual route step",false);
    private final JButton back = SidebarControls.button("Back","Previous route step",false);
    private final JButton stop = SidebarControls.button("Stop","Stop this checklist; keep your saved setup",false);
    private Map<String, Long> carried = Collections.emptyMap(); // EDT-owned immutable snapshot.
    private boolean inventoryAvailable;
    private final JButton download = SidebarControls.button("Load route","Load this character's route from the website",false);
    private final JButton upload = SidebarControls.button("Save route","Save this character's route to the website",false);
    private final Set<String> loaded = new HashSet<>();
    private volatile String session = "", name = "", token = "", api = "";
    private volatile long generation;
    private volatile View view;
    private String selectedKind = "farm";
    private volatile String editorSession = "";
    private String editorName = "";
    private RoutineRoutePlan plan = RoutineRoutePlan.defaults("farm");
    private final RoutineRouteRun run = new RoutineRouteRun();
    private boolean busy;

    RoutineRouteHelper(ConfigManager manager, HcimProgressionCompanionConfig config, SyncService service, Gson gson, ClientThread clientThread)
    {
        this.manager = manager; this.config = config; this.service = service; this.gson = gson; this.clientThread = clientThread;
        SidebarControls.stretch(kind); kind.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,30));
        panel.add(kind); SidebarControls.gap(panel); panel.add(status); SidebarControls.gap(panel);
        panel.add(start); SidebarControls.gap(panel); panel.add(SidebarControls.row(back,next,stop)); SidebarControls.gap(panel);
        panel.add(packing); SidebarControls.gap(panel);
        panel.add(SidebarControls.fold("Packing details",SidebarControls.scroll(packingRows,150))); SidebarControls.gap(panel);
        JPanel edit = SidebarControls.column();
        edit.add(SidebarControls.text("Packing list")); edit.add(SidebarControls.editor(items,105)); SidebarControls.gap(edit);
        edit.add(SidebarControls.text("Exact names: Oak logs x 4. Notes stay manual.")); SidebarControls.gap(edit);
        edit.add(SidebarControls.text("Stops · one location | note per line")); edit.add(SidebarControls.editor(stops,125)); SidebarControls.gap(edit);
        JButton local = SidebarControls.button("Save locally","Save this profile's setup without uploading",false); edit.add(local);
        edit.add(SidebarControls.text("Edits apply when you start or restart."));
        panel.add(SidebarControls.fold("Edit route",edit)); SidebarControls.gap(panel);
        JPanel sync = SidebarControls.column(); sync.add(download); SidebarControls.gap(sync); sync.add(upload); SidebarControls.gap(sync);
        sync.add(SidebarControls.text("Needs a linked character and Route setup sync. Load before your first save."));
        panel.add(SidebarControls.fold("Website sync",sync)); SidebarControls.gap(panel);
        panel.add(SidebarControls.fold("How it works",SidebarControls.text("Steps are manual. Check travel unlocks and supply charges yourself. Inventory and worn gear count; notes and bank items do not.")));
        kind.addActionListener(event -> {
            if (!session.isEmpty() && session.equals(editorSession)) saveLocal();
            run.stop(); view = null;
            selectedKind = kind.getSelectedIndex() == 0 ? "farm" : "birdhouse";
            loadLocal(); updateControls();
        });
        local.addActionListener(event -> { if (saveLocal()) message("Saved for this RuneScape profile and character. Website unchanged."); });
        start.addActionListener(event -> {
            if (!config.routineRoutesEnabled()) { message("Enable Local route helper in plugin settings first."); return; }
            if (!saveLocal()) return;
            run.start(plan); updateView();
        });
        next.addActionListener(event -> {
            if (!run.active()) return;
            run.next();
            if (!run.active()) { view = null; updateControls(); message("Route complete. Timers unchanged."); }
            else updateView();
        });
        back.addActionListener(event -> { if (run.active()) { run.back(); updateView(); } });
        stop.addActionListener(event -> { run.stop(); view = null; updateControls(); message("Route stopped. Setup kept."); });
        download.addActionListener(event -> sync(false)); upload.addActionListener(event -> sync(true));
        items.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { updatePacking(); }
            public void removeUpdate(DocumentEvent event) { updatePacking(); }
            public void changedUpdate(DocumentEvent event) { updatePacking(); }
        });
        loadLocal();
    }

    JPanel panel() { return panel; }

    /** Called on the client thread. Stale EDT and network work is generation-guarded. */
    boolean session(String nextName, String nextToken, String nextApi)
    {
        String next = nextName.isEmpty() ? "" : nextName + ":" + nextToken + ":" + nextApi;
        name = nextName; token = nextToken; api = nextApi;
        if (next.equals(session)) return false;
        session = next; long revision = ++generation; view = null;
        SwingUtilities.invokeLater(() -> {
            if (revision != generation) return;
            editorSession = session; editorName = name;
            run.stop(); busy = false; carried = Collections.emptyMap(); inventoryAvailable = false;
            loaded.clear(); kind.setEnabled(true); download.setEnabled(true); upload.setEnabled(true); loadLocal();
        });
        return true;
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
        updatePacking();
        updateControls(); message(session.isEmpty() ? "Log in to use your route." : "Choose a route, then Start when ready.");
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
                    items.setText(plan.items); stops.setText(plan.stops); saveLocal(); run.stop(); view = null; updateControls();
                    message(result == null ? "No website route yet. Edit this starter setup and Save to website." : "Website route loaded. Review packing items, then Start route.");
                }
            }));
        });
    }
    private void updateView()
    {
        if (!run.active() || !session.equals(editorSession)) { view = null; return; }
        view = new View(run.title(), run.text(), activePackingSummary());
        updateControls(); message(run.position() < 0 ? "Check packing, then Next to begin." : run.title() + "\n" + run.text());
    }
    View view() { return config.routineRoutesEnabled() && !session.isEmpty() && session.equals(editorSession) ? view : null; }
    void carriedItems(Map<String, Long> snapshot, boolean available)
    {
        long revision = generation;
        SwingUtilities.invokeLater(() -> {
            if (revision != generation || !session.equals(editorSession)) return;
            carried = snapshot; inventoryAvailable = available; updatePacking();
        });
    }
    private void updatePacking()
    {
        if (!config.livePackingChecksEnabled() || !config.routineRoutesEnabled() || session.isEmpty() || !session.equals(editorSession))
        {
            packing.setText("Packing checks off"); packingRows.removeAll();
            packingRows.add(SidebarControls.text("Enable Local route helper and Live packing checks in settings."));
        }
        else
        {
            PackingChecklist.Result result = PackingChecklist.check(items.getText(), carried, inventoryAvailable);
            packing.setText(result.summary); packingRows.removeAll();
            for (PackingChecklist.Row row : result.rows)
            {
                JTextArea entry = SidebarControls.text(row.item + "\n" + row.status);
                entry.setForeground(row.state == PackingChecklist.State.READY ? new Color(104,211,145)
                    : row.state == PackingChecklist.State.MISSING ? new Color(246,173,85) : SidebarControls.MUTED);
                packingRows.add(entry); SidebarControls.gap(packingRows);
            }
        }
        packingRows.revalidate(); packingRows.repaint();
        if (run.active() && session.equals(editorSession)) view = new View(run.title(), run.text(), activePackingSummary());
    }
    private String activePackingSummary()
    {
        return config.livePackingChecksEnabled() && run.position() < 0
            ? PackingChecklist.check(run.packingItems(), carried, inventoryAvailable).summary : "";
    }
    private void updateControls()
    {
        start.setText(run.active()?"Restart route":"Start route");
        back.setEnabled(run.active()); next.setEnabled(run.active()); stop.setEnabled(run.active());
    }
    private void message(String text)
    {
        status.setText(String.valueOf(text));
    }
    static final class View {
        final String title, text, packing;
        View(String title, String text, String packing) { this.title = title; this.text = text; this.packing = packing; }
    }
}
