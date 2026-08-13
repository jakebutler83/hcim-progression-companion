package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import org.junit.Assert;
import org.junit.Test;

public class TcgCollectionSnapshotServiceTest
{
    @Test
    public void requestsAndConsumesOwnedNamesThroughPluginMessages()
    {
        EventBus eventBus = new EventBus();
        QueryCapture capture = new QueryCapture();
        eventBus.register(capture);
        TcgCollectionSnapshotService service = new TcgCollectionSnapshotService(eventBus);

        service.requestSnapshot();
        Assert.assertNotNull(capture.query);
        Assert.assertEquals("osrstcg", capture.query.getNamespace());
        Assert.assertEquals("query-owned-names", capture.query.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("ownedNames", Arrays.asList("Dragon scimitar", "Abyssal whip", "dragon SCIMITAR", ""));
        Assert.assertTrue(service.handle(new PluginMessage("osrstcg", "owned-names", data)));

        TcgCollectionSnapshot snapshot = service.getLatestSnapshot();
        Assert.assertTrue(snapshot.isAvailable());
        Assert.assertEquals(2, snapshot.getUniqueOwned());
        Assert.assertEquals(2, snapshot.getTotalCardsOwned());
        Assert.assertEquals(2, snapshot.getCards().size());
    }

    @Test
    public void ignoresUnrelatedPluginMessages()
    {
        TcgCollectionSnapshotService service = new TcgCollectionSnapshotService(new EventBus());
        Assert.assertFalse(service.handle(new PluginMessage("some-other-plugin", "owned-names")));
        Assert.assertFalse(service.getLatestSnapshot().isAvailable());
    }

    private static class QueryCapture
    {
        private PluginMessage query;

        @Subscribe
        public void onPluginMessage(PluginMessage event)
        {
            if ("osrstcg".equals(event.getNamespace()) && "query-owned-names".equals(event.getName()))
            {
                query = event;
            }
        }
    }
}
