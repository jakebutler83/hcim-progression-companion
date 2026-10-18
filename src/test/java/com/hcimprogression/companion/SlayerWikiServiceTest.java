package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerWikiServiceTest
{
    private JsonObject response(boolean versionTwo)
    {
        JsonObject content = new JsonObject(); content.addProperty(versionTwo ? "content" : "*", "{{Infobox Monster\n|name=Greater demon\n|hitpoints=87\n}}\n{{LocLine\n|location=[[Chasm of Fire]]\n}}\n");
        JsonObject slots=new JsonObject(); slots.add("main",content);
        JsonObject revision=new JsonObject(); revision.addProperty("revid",123); revision.add("slots",slots);
        JsonArray revisions=new JsonArray(); revisions.add(revision);
        JsonObject page=new JsonObject(); page.addProperty("title","Greater demon"); page.add("revisions",revisions);
        JsonObject query=new JsonObject();
        if (versionTwo) { JsonArray pages=new JsonArray(); pages.add(page); query.add("pages",pages); }
        else { JsonObject pages=new JsonObject(); pages.add("45",page); query.add("pages",pages); }
        JsonObject result=new JsonObject(); result.add("query",query); return result;
    }
    @Test public void readsBothMediaWikiRevisionPayloadFormats()
    {
        for(boolean versionTwo:new boolean[]{false,true})
        {
            SlayerWikiGuide result=SlayerWikiService.parseResponse(response(versionTwo));
            assertTrue(result.variants.get(0).stats.contains("HP: 87")); assertEquals("Chasm of Fire",result.locations);
        }
    }
    @Test public void missingAndApiErrorResponsesAreExplicitRatherThanEmptySuccess()
    {
        Gson gson=Guice.createInjector().getInstance(Gson.class);
        for(String raw:new String[]{"{}","{\"error\":{\"code\":\"badtitle\"}}","{\"query\":{\"pages\":{}}}","{\"query\":{\"pages\":{\"-1\":{\"title\":\"Missing\",\"missing\":\"\"}}}}"})
            try { SlayerWikiService.parseResponse(gson.fromJson(raw,JsonObject.class)); fail("Should reject missing facts"); } catch(IllegalArgumentException expected) { assertFalse(expected.getMessage().isEmpty()); }
    }
    @Test public void optionalLivePublicLookupReturnsRealGreaterDemonStatsAndLocations() throws Exception
    {
        // Explicit development-only network smoke check. Normal CI is offline.
        Assume.assumeTrue("1".equals(System.getenv("PROGRESSION_PATH_LIVE_WIKI_CHECK")));
        com.google.inject.Injector injector=Guice.createInjector();
        SlayerWikiService service=new SlayerWikiService(injector.getInstance(OkHttpClient.class),injector.getInstance(Gson.class));
        CompletableFuture<SlayerWikiGuide> complete=new CompletableFuture<>();
        okhttp3.Call call=service.load("Greater demon",(guide,error)->{
            if(error != null) complete.completeExceptionally(new IllegalStateException(error)); else complete.complete(guide);
        });
        try
        {
            SlayerWikiGuide guide=complete.get(20,TimeUnit.SECONDS);
            assertTrue(guide.variants.size() >= 5); assertTrue(guide.variants.get(0).fields.containsKey("HP"));
            assertTrue(guide.variants.get(0).fields.containsKey("Elemental weakness"));
            assertTrue(guide.locations.contains("Catacombs of Kourend")); assertTrue(guide.revision > 0);
        }
        finally { call.cancel(); }
    }
}
