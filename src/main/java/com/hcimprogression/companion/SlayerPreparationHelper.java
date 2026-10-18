package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.LinkBrowser;
import okhttp3.Call;

/** User-started, local bank preparation. No game actions and no automatic Wiki requests. */
final class SlayerPreparationHelper
{
    private final Client client;
    private final ClientThread thread;
    private final ConfigManager config;
    private final ItemManager items;
    private final AccountSnapshotService snapshots;
    private final SlayerBankPlanner planner;
    private final SlayerWikiService wiki;
    private final JPanel root, body = SidebarControls.column();
    private final JTextArea taskText = SidebarControls.text("Log in, get a Slayer task, then select Start task.");
    private final JTextArea gearText = SidebarControls.text("Open your normal bank after starting. Worn gear and Group Storage are never used as bank ownership.");
    private final JTextArea wikiText = SidebarControls.text("Load Wiki info to see factual stats for an exact monster variant.");
    private final JTextArea locations = SidebarControls.text("Locations appear after a Wiki lookup. Always follow your assigned area restriction.");
    private final JTextArea status = SidebarControls.text("Local preparation only; no auto-withdraw, equipment changes or combat guidance.");
    private final JTextField title = new JTextField();
    private final JComboBox<String> styles = new JComboBox<>(new String[]{"Melee: standard", "Melee: stab", "Melee: crush", "Ranged", "Magic: single target", "Magic: burst/barrage"});
    private final JComboBox<SlayerWikiGuide.Variant> variants = new JComboBox<>();
    private final JButton highlight = SidebarControls.button("Highlight bank gear", "Visual marks in the normal bank only. No items are moved.", false);
    private volatile Set<Integer> highlighted = Collections.emptySet();
    private volatile long revision;
    private volatile long generation;
    private volatile boolean closed;
    private long lookupAt;
    private String identity = "", assignment = "", task = "", monster = "", area = "", style = "melee";
    private String lastTaskInput = "";
    private int remaining;
    private boolean active, bankKnown, marking;
    private List<SlayerBankPlanner.Owned> bank = Collections.emptyList();
    private SlayerBankPlanner.Plan plan;
    private SlayerWikiGuide guide, displayedGuide;
    private volatile Call lookup;

    SlayerPreparationHelper(Client client, ClientThread thread, ConfigManager config, ItemManager items,
        AccountSnapshotService snapshots, SlayerBankPlanner planner, SlayerWikiService wiki)
    {
        this.client = client; this.thread = thread; this.config = config; this.items = items;
        this.snapshots = snapshots; this.planner = planner; this.wiki = wiki;
        body.add(taskText); SidebarControls.gap(body);
        JButton start = SidebarControls.button("Start task", "Read your actual in-game assignment and prepare locally", true);
        JButton stop = SidebarControls.button("Stop", "Clear this session's bank data, highlights and pending lookup", false);
        body.add(SidebarControls.row(start, stop)); SidebarControls.gap(body);
        SidebarControls.stretch(styles); styles.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 28)); body.add(styles);
        JButton refresh = SidebarControls.button("Refresh bank setup", "Apply the monster name below and recalculate from your bank; won't inspect worn gear", false);
        body.add(refresh); body.add(highlight); SidebarControls.gap(body);
        body.add(SidebarControls.fold("Bank loadout", SidebarControls.scroll(gearText, 200)));
        SidebarControls.gap(body); body.add(SidebarControls.text("Monster / Wiki page (editable)"));
        SidebarControls.stretch(title); title.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 28)); body.add(title);
        JButton load = SidebarControls.button("Load Wiki info", "Sends only the public monster page title to OSRS Wiki after confirmation", false);
        JButton open = SidebarControls.button("Open Wiki page", "Open the exact monster page in your browser", false);
        body.add(load); body.add(open); SidebarControls.gap(body);
        SidebarControls.stretch(variants); variants.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 28)); body.add(variants);
        body.add(SidebarControls.fold("Stats & weaknesses", SidebarControls.scroll(wikiText, 200)));
        SidebarControls.gap(body); body.add(SidebarControls.fold("Locations / travel", SidebarControls.scroll(locations, 140)));
        SidebarControls.gap(body); body.add(status);
        root = SidebarControls.fold("SLAYER PREPARATION", body);
        start.addActionListener(event -> { String chosen = style(); thread.invokeLater(() -> start(chosen)); });
        stop.addActionListener(event -> thread.invokeLater(() -> reset("Stopped. Local bank data and highlights cleared.")));
        refresh.addActionListener(event -> { String chosen = style(), selected = title.getText(); thread.invokeLater(() -> {
            if (!sameCharacter()) { reset("Select Start task for this character first."); return; }
            try
            {
                String next = SlayerWikiGuide.validTitle(selected);
                if (!monster.equals(next)) { generation++; cancelLookup(); guide = null; monster = next; }
            }
            catch (IllegalArgumentException error) { publish(error.getMessage()); return; }
            style = chosen; readBank(client.getItemContainer(InventoryID.BANK)); recalculate();
        }); });
        styles.addActionListener(event -> { String chosen = style(); thread.invokeLater(() -> { if (sameCharacter()) { style = chosen; recalculate(); } }); });
        highlight.addActionListener(event -> thread.invokeLater(() -> {
            if (!sameCharacter()) { reset("Select Start task for this character first."); return; }
            marking = !marking && plan != null && !plan.blocked;
            highlighted = marking ? plan.highlights() : Collections.emptySet();
            publish(marking ? "Gold marks show the chosen bank items. Withdraw them yourself; refresh only when you want a new setup."
                : plan != null && plan.blocked ? "Resolve the missing equipment or supported-weapon warning before highlighting." : "Bank highlights cleared.");
        }));
        load.addActionListener(event -> {
            String page = title.getText();
            if (JOptionPane.showConfirmDialog(body, "Load public monster facts from oldschool.runescape.wiki?\nOnly the page title is submitted; bank items, account name and tokens are not sent.\nThis feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", "Wiki lookup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                thread.invokeLater(() -> load(page));
        });
        open.addActionListener(event -> { try { LinkBrowser.browse(SlayerWikiGuide.url(title.getText())); } catch (IllegalArgumentException error) { status.setText(error.getMessage()); } });
        variants.addActionListener(event -> {
            SlayerWikiGuide.Variant value = (SlayerWikiGuide.Variant) variants.getSelectedItem();
            if (value != null && displayedGuide != null) wikiText.setText(value.stats + "\n\nOSRS Wiki · revision " + displayedGuide.revision + "\nChoose the correct variant; requirements and locations may differ.");
        });
    }
    JPanel panel() { return root; }
    Set<Integer> highlights() { return highlighted; }
    private String style() { return new String[]{"melee", "stab", "crush", "ranged", "magic", "burst"}[Math.max(0, styles.getSelectedIndex())]; }
    private String identity() { return config.getRSProfileKey() + ":" + client.getAccountHash(); }
    private boolean sameCharacter() { return !closed && active && client.getGameState() == GameState.LOGGED_IN && identity.equals(identity()); }
    void tick()
    {
        if (closed) return;
        if (client.getGameState() != GameState.LOGGED_IN) { if (active || !identity.isEmpty()) reset("Log in and select Start task."); return; }
        if (!identity.equals(identity())) { reset("Select Start task for this character."); identity = identity(); }
        // Read the DB names only when the game's assignment inputs change,
        // not once per frame or repeatedly for an unchanged task.
        String inputs = client.getVarpValue(VarPlayerID.SLAYER_TARGET) + ":" + client.getVarpValue(VarPlayerID.SLAYER_COUNT)
            + ":" + client.getVarpValue(VarPlayerID.SLAYER_AREA) + ":" + client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
        if (inputs.equals(lastTaskInput)) return;
        SlayerSnapshot current = snapshots.readSlayer(client);
        String nextArea = assignedArea();
        String nextAssignment = current.getTargetId() + ":" + current.getCurrentTask() + ":" + nextArea;
        if (active && (!assignment.equals(nextAssignment) || current.getRemaining() <= 0))
        {
            reset("Assignment changed or completed. Select Start task again.");
            identity = identity();
        }
        lastTaskInput = inputs;
        boolean changed = !task.equals(current.getCurrentTask()) || remaining != current.getRemaining() || !area.equals(nextArea);
        task = current.getCurrentTask(); remaining = current.getRemaining(); area = nextArea;
        if (changed) publish("");
    }
    private void start(String chosen)
    {
        if (closed) return;
        tick();
        if (client.getGameState() != GameState.LOGGED_IN || remaining <= 0) { publish("Get a Slayer assignment before starting."); return; }
        generation++; cancelLookup(); guide = null; active = true; style = chosen;
        assignment = client.getVarpValue(VarPlayerID.SLAYER_TARGET) + ":" + task + ":" + area;
        bankKnown = false; bank = Collections.emptyList(); plan = null; marking = false; highlighted = Collections.emptySet();
        monster = SlayerWikiGuide.monsterTitle(task);
        String page = monster; long expected = generation;
        SwingUtilities.invokeLater(() -> { if (generation == expected) title.setText(page); });
        readBank(client.getItemContainer(InventoryID.BANK)); recalculate();
    }
    void bankChanged(ItemContainer container)
    {
        if (!sameCharacter()) return;
        readBank(container);
        // Freeze the chosen packing list while withdrawing, rather than
        // suggesting alternative weapons each time an item leaves the bank.
        if (plan == null && bankKnown) recalculate();
    }
    private void readBank(ItemContainer container)
    {
        net.runelite.api.widgets.Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        if (!sameCharacter() || container == null || bankItems == null || bankItems.isHidden()) return;
        List<SlayerBankPlanner.Owned> next = new ArrayList<>();
        for (Item item : container.getItems()) if (item != null && item.getId() > 0 && item.getQuantity() > 0)
        {
            ItemComposition definition = items.getItemComposition(item.getId());
            if (definition.getPlaceholderTemplateId() != -1 || definition.getNote() != -1) continue;
            next.add(new SlayerBankPlanner.Owned(item.getId(), definition.getName(), item.getQuantity()));
        }
        bank = next; bankKnown = true;
    }
    private void recalculate()
    {
        marking = false; highlighted = Collections.emptySet();
        if (!bankKnown) { publish("Open the normal bank to prepare a bank-owned setup. No bank upload setting is needed."); return; }
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (Skill skill : Skill.values()) if (skill != Skill.OVERALL) levels.put(skill.getName(), client.getRealSkillLevel(skill));
        plan = planner.plan(monster, style, bank, levels);
        publish("Setup uses this session's real bank read; it is frozen while you withdraw. Gear rules reviewed " + planner.reviewed() + ".");
    }
    private void load(String page)
    {
        if (!sameCharacter()) { publish("Start this character's task first."); return; }
        try { page = SlayerWikiGuide.validTitle(page); } catch (IllegalArgumentException error) { publish(error.getMessage()); return; }
        if (System.currentTimeMillis() - lookupAt < 5000) { publish("Please wait a few seconds between Wiki lookups."); return; }
        lookupAt = System.currentTimeMillis(); cancelLookup(); guide = null;
        monster = page;
        if (bankKnown) recalculate();
        long expected = ++generation; String owner = identity;
        publish("Loading the public Wiki page…");
        lookup = wiki.load(page, (result, error) -> thread.invokeLater(() -> {
            if (!sameCharacter() || generation != expected || !identity.equals(owner)) return;
            lookup = null; guide = result;
            publish(error == null ? "Wiki facts loaded. Select the exact variant; lower defence is not automatically the best DPS." : error);
        }));
        if (closed || generation != expected) cancelLookup();
    }
    private String assignedArea()
    {
        int id = client.getVarpValue(VarPlayerID.SLAYER_AREA);
        if (id <= 0) return "No specific area restriction reported";
        try
        {
            List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, id);
            if (!rows.isEmpty()) return String.valueOf(client.getDBTableField(rows.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0)[0]);
        }
        catch (RuntimeException ignored) { }
        return "Area restriction could not be read; check your in-game assignment";
    }
    void reset(String message)
    {
        generation++; cancelLookup(); active = false; identity = ""; assignment = ""; bankKnown = false;
        lastTaskInput = "";
        task = ""; monster = ""; area = ""; remaining = 0;
        bank = Collections.emptyList(); plan = null; guide = null; marking = false; highlighted = Collections.emptySet();
        long expected = generation;
        SwingUtilities.invokeLater(() -> { if (generation == expected) title.setText(""); });
        publish(message);
    }
    void close()
    {
        // Disable immediately even if an old sidebar action is queued on ClientThread.
        closed = true; highlighted = Collections.emptySet(); generation++;
        cancelLookup();
        thread.invokeLater(() -> reset("Plugin stopped. Local Slayer data cleared."));
    }
    private void cancelLookup() { Call pending = lookup; lookup = null; if (pending != null) pending.cancel(); }
    private void publish(String message)
    {
        long version = ++revision;
        String current = task + " · " + remaining + " remaining\n" + area + (active ? "\nPreparation active" : "\nPreparation stopped");
        StringBuilder equipment = new StringBuilder();
        if (plan != null)
        {
            equipment.append("Preparing for: ").append(monster).append('\n').append('\n');
            for (Map.Entry<String, SlayerBankPlanner.Owned> slot : plan.gear.entrySet()) equipment.append(slot.getKey()).append(": ").append(slot.getValue().name).append('\n');
            equipment.append('\n').append(String.join("\n\n", plan.warnings));
        }
        else equipment.append("Open your normal bank after selecting Start task. Currently worn equipment, group storage and placeholders are not used.");
        String gear = equipment.toString(); boolean marked = marking; SlayerWikiGuide snapshot = guide;
        SwingUtilities.invokeLater(() -> {
            if (version != revision) return;
            taskText.setText(current); gearText.setText(gear); highlight.setText(marked ? "Clear highlights" : "Highlight bank gear");
            if (!message.isEmpty()) status.setText(message);
            if (snapshot != displayedGuide)
            {
                displayedGuide = snapshot; variants.removeAllItems();
                if (snapshot != null)
                {
                    for (SlayerWikiGuide.Variant variant : snapshot.variants) variants.addItem(variant);
                    locations.setText(snapshot.locations + "\n\nLocations are options, not proof of task-area access.\nOSRS Wiki · revision " + snapshot.revision + "\n" + snapshot.url);
                }
                else { wikiText.setText("Load Wiki info for this monster; choose the correct variant."); locations.setText("Always follow your assigned area restriction. Use Open Wiki page for detailed travel instructions."); }
            }
            body.revalidate(); body.repaint();
        });
    }
}
