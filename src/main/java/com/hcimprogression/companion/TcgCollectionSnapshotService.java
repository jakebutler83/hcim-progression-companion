package com.hcimprogression.companion;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

/**
 * Exchanges only the public, read-only messages exposed by OSRS TCG.
 *
 * <p>The string constants are intentionally copied from the TCG message API.
 * Plugin Hub plugins have separate classloaders and must not import one
 * another's implementation classes.</p>
 */
public class TcgCollectionSnapshotService
{
    private static final String NAMESPACE = "osrstcg";
    private static final String QUERY = "query-owned-names";
    private static final String REPLY = "owned-names";
    private static final String CHANGED = "owned-names-changed";
    private static final String KEY_OWNED_NAMES = "ownedNames";
    private static final int CARD_POOL = 6376;
    private static final int MAX_NAME_LENGTH = 80;

    private final EventBus eventBus;
    private volatile TcgCollectionSnapshot latestSnapshot = TcgCollectionSnapshot.unavailable();

    public TcgCollectionSnapshotService(EventBus eventBus)
    {
        this.eventBus = eventBus;
    }

    /** Requests a fresh snapshot from OSRS TCG, if that plugin is running. */
    public void requestSnapshot()
    {
        eventBus.post(new PluginMessage(NAMESPACE, QUERY));
    }

    /**
     * Accepts OSRS TCG replies and collection-change broadcasts.
     *
     * @return true when the event contained a valid TCG collection snapshot
     */
    public boolean handle(PluginMessage event)
    {
        if (event == null || !NAMESPACE.equals(event.getNamespace())
            || (!REPLY.equals(event.getName()) && !CHANGED.equals(event.getName())))
        {
            return false;
        }

        TcgCollectionSnapshot parsed = parse(event.getData());
        if (parsed == null)
        {
            return false;
        }
        latestSnapshot = parsed;
        return true;
    }

    public TcgCollectionSnapshot getLatestSnapshot()
    {
        return latestSnapshot;
    }

    private TcgCollectionSnapshot parse(Map<String, Object> data)
    {
        if (data == null || !(data.get(KEY_OWNED_NAMES) instanceof Iterable))
        {
            return null;
        }

        TcgCollectionSnapshot snapshot = new TcgCollectionSnapshot();
        Set<String> uniqueNames = new HashSet<>();
        for (Object value : (Iterable<?>) data.get(KEY_OWNED_NAMES))
        {
            if (!(value instanceof String))
            {
                continue;
            }
            String name = ((String) value).trim();
            String normalized = name.toLowerCase(java.util.Locale.ROOT);
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || !uniqueNames.add(normalized))
            {
                continue;
            }
            snapshot.getCards().add(new TcgCollectionSnapshot.CardSnapshot(name, false, 1));
        }

        int owned = snapshot.getCards().size();
        snapshot.setAvailable(true);
        snapshot.setSchemaVersion(1);
        snapshot.setTotalCardsOwned(owned);
        snapshot.setUniqueOwned(owned);
        snapshot.setTotalCardPool(CARD_POOL);
        snapshot.setCompletionPct(owned * 100d / CARD_POOL);
        snapshot.setUpdatedAt(System.currentTimeMillis());
        return snapshot;
    }
}
