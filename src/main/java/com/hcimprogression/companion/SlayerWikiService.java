package com.hcimprogression.companion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.function.BiConsumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Explicit user lookup: only a public page title, never bank/account data. */
final class SlayerWikiService
{
    private final OkHttpClient http;
    private final Gson gson;
    SlayerWikiService(OkHttpClient http, Gson gson) { this.http = http; this.gson = gson; }
    Call load(String title, BiConsumer<SlayerWikiGuide, String> done)
    {
        HttpUrl url = new HttpUrl.Builder().scheme("https").host(SlayerWikiGuide.HOST).addPathSegment("api.php")
            .addQueryParameter("action", "query").addQueryParameter("titles", SlayerWikiGuide.validTitle(title))
            .addQueryParameter("prop", "revisions").addQueryParameter("rvprop", "content|ids").addQueryParameter("rvslots", "main")
            .addQueryParameter("format", "json").addQueryParameter("redirects", "1").build();
        Call call = http.newCall(new Request.Builder().url(url).header("User-Agent", "ProgressionPath (Slayer preparation; public Wiki lookup)").build());
        call.timeout().timeout(15, java.util.concurrent.TimeUnit.SECONDS);
        call.enqueue(new Callback()
        {
            @Override public void onFailure(Call call, IOException error) { done.accept(null, "Wiki lookup unavailable; your local bank setup is unchanged."); }
            @Override public void onResponse(Call call, Response response)
            {
                try (Response reply = response)
                {
                    if (!reply.isSuccessful() || reply.body() == null || !SlayerWikiGuide.HOST.equals(reply.request().url().host())) throw new IOException();
                    okio.BufferedSource source = reply.body().source();
                    if (source.request(500_001)) throw new IOException();
                    JsonObject json = gson.fromJson(source.getBuffer().readUtf8(), JsonObject.class);
                    JsonObject pages = json.getAsJsonObject("query").getAsJsonObject("pages");
                    JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
                    JsonObject revision = page.getAsJsonArray("revisions").get(0).getAsJsonObject();
                    JsonElement content = revision.getAsJsonObject("slots").getAsJsonObject("main").get("*");
                    done.accept(SlayerWikiGuide.parse(page.get("title").getAsString(), revision.get("revid").getAsLong(), content.getAsString()), null);
                }
                catch (IllegalArgumentException error) { done.accept(null, error.getMessage()); }
                catch (Exception error) { done.accept(null, "No usable monster page returned. Try the exact Wiki monster name, not a task category."); }
            }
        });
        return call;
    }
}
