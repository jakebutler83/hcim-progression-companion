package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/**
 * Captures collection-log data from pages the player opens in game.
 *
 * The game only constructs item widgets for the currently visible page, so a
 * complete historical import requires opening each Treasure Trails page once.
 */
public class CollectionLogCaptureService
{
    private static final int ACTIVE_TAB_VARBIT = 6905;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9][0-9,]*)");

    private final Map<String, CapturedPage> pages = new LinkedHashMap<>();
    private final Map<String, Integer> clueCounts = new LinkedHashMap<>();

    public synchronized CaptureResult captureCurrentPage(Client client, ItemManager itemManager)
    {
        Widget header = client.getWidget(ComponentID.COLLECTION_LOG_ENTRY_HEADER);
        Widget itemsContainer = client.getWidget(ComponentID.COLLECTION_LOG_ENTRY_ITEMS);
        if (header == null || itemsContainer == null)
        {
            return null;
        }

        Widget[] headerChildren = header.getDynamicChildren();
        if (headerChildren == null || headerChildren.length == 0)
        {
            return null;
        }

        String pageTitle = clean(headerChildren[0].getText());
        if (pageTitle.isEmpty())
        {
            return null;
        }

        String tabName = activeTabName(client);
        List<AccountSnapshot.CollectionLogItemSnapshot> obtainedItems = new ArrayList<>();
        Widget[] itemWidgets = itemsContainer.getDynamicChildren();
        if (itemWidgets != null)
        {
            for (Widget itemWidget : itemWidgets)
            {
                int itemId = itemWidget.getItemId();
                if (itemId <= 0 || itemWidget.getOpacity() != 0)
                {
                    continue;
                }

                ItemComposition composition = itemManager.getItemComposition(itemId);
                String itemName = composition == null ? "Item " + itemId : composition.getMembersName();
                if (itemName == null || itemName.trim().isEmpty())
                {
                    itemName = "Item " + itemId;
                }

                int quantity = Math.max(1, itemWidget.getItemQuantity());
                obtainedItems.add(new AccountSnapshot.CollectionLogItemSnapshot(
                    itemId,
                    itemName,
                    pageTitle,
                    quantity
                ));
            }
        }

        pages.put(pageTitle.toLowerCase(Locale.ENGLISH), new CapturedPage(pageTitle, tabName, obtainedItems));
        captureClueCount(pageTitle, headerChildren);
        return new CaptureResult(pageTitle, obtainedItems.size(), pages.size(), getObtainedItemCount());
    }

    public synchronized void applyTo(AccountSnapshot snapshot)
    {
        snapshot.getClueCounts().putAll(clueCounts);
        for (CapturedPage page : pages.values())
        {
            snapshot.getCollectionLogItems().addAll(page.items);
        }
        snapshot.getCollectionLog().put("capturedPages", pages.size());
        snapshot.getCollectionLog().put("capturedItems", getObtainedItemCount());
    }

    public synchronized int getCapturedPageCount()
    {
        return pages.size();
    }

    public synchronized int getObtainedItemCount()
    {
        int count = 0;
        for (CapturedPage page : pages.values())
        {
            count += page.items.size();
        }
        return count;
    }

    public synchronized Map<String, Integer> getClueCounts()
    {
        return new LinkedHashMap<>(clueCounts);
    }

    private void captureClueCount(String pageTitle, Widget[] headerChildren)
    {
        String tier = clueTier(pageTitle);
        if (tier == null)
        {
            return;
        }

        for (int i = 1; i < headerChildren.length; i++)
        {
            Widget child = headerChildren[i];
            if (child == null)
            {
                continue;
            }
            String text = clean(child.getText());
            String lower = text.toLowerCase(Locale.ENGLISH);
            if (!lower.contains("clue") && !lower.contains("completed"))
            {
                continue;
            }
            Matcher matcher = NUMBER_PATTERN.matcher(text);
            int last = -1;
            while (matcher.find())
            {
                try
                {
                    last = Integer.parseInt(matcher.group(1).replace(",", ""));
                }
                catch (NumberFormatException ignored)
                {
                    // Ignore malformed widget text.
                }
            }
            if (last >= 0)
            {
                clueCounts.put(tier, last);
                return;
            }
        }
    }

    private String activeTabName(Client client)
    {
        Widget tabs = client.getWidget(ComponentID.COLLECTION_LOG_TABS);
        if (tabs == null || tabs.getStaticChildren() == null)
        {
            return "Collection Log";
        }
        int index = client.getVarbitValue(ACTIVE_TAB_VARBIT);
        Widget[] children = tabs.getStaticChildren();
        if (index < 0 || index >= children.length || children[index] == null)
        {
            return "Collection Log";
        }
        String name = clean(children[index].getName());
        return name.isEmpty() ? "Collection Log" : name;
    }

    private String clueTier(String pageTitle)
    {
        String normalized = pageTitle.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("beginner")) return "beginner";
        if (normalized.contains("easy")) return "easy";
        if (normalized.contains("medium")) return "medium";
        if (normalized.contains("hard") && !normalized.contains("shared")) return "hard";
        if (normalized.contains("elite")) return "elite";
        if (normalized.contains("master")) return "master";
        return null;
    }

    private String clean(String value)
    {
        return value == null ? "" : Text.removeTags(value).trim();
    }

    private static final class CapturedPage
    {
        private final String title;
        private final String tab;
        private final List<AccountSnapshot.CollectionLogItemSnapshot> items;

        private CapturedPage(String title, String tab, List<AccountSnapshot.CollectionLogItemSnapshot> items)
        {
            this.title = title;
            this.tab = tab;
            this.items = items;
        }
    }

    public static final class CaptureResult
    {
        private final String pageTitle;
        private final int pageItems;
        private final int totalPages;
        private final int totalItems;

        private CaptureResult(String pageTitle, int pageItems, int totalPages, int totalItems)
        {
            this.pageTitle = pageTitle;
            this.pageItems = pageItems;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
        }

        public String getPageTitle() { return pageTitle; }
        public int getPageItems() { return pageItems; }
        public int getTotalPages() { return totalPages; }
        public int getTotalItems() { return totalItems; }
    }
}
