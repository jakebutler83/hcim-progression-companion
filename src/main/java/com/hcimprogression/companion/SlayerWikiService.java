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
                    if (!reply.isSuccessful()) throw new IllegalArgumentException("Wiki returned HTTP " + reply.code() + ". Open the Wiki page or try again later.");
                    if (reply.body() == null || !SlayerWikiGuide.HOST.equals(reply.request().url().host())) throw new IOException();
                    okio.BufferedSource source = reply.body().source();
                    if (source.request(500_001)) throw new IllegalArgumentException("Wiki response exceeded the safe size limit. Use Open Wiki page.");
                    JsonObject json = gson.fromJson(source.getBuffer().readUtf8(), JsonObject.class);
                    done.accept(parseResponse(json), null);
                }
                catch (IllegalArgumentException error) { done.accept(null, error.getMessage()); }
                catch (Exception error) { done.accept(null, "Wiki returned an unreadable response. Open the Wiki page or retry the exact monster name."); }
            }
        });
        return call;
    }
    static SlayerWikiGuide parseResponse(JsonObject json)
    {
        if (json == null || json.has("error") || !json.has("query")) throw new IllegalArgumentException("Wiki could not return this page. Try the exact monster name.");
        JsonElement pages = json.getAsJsonObject("query").get("pages");
        if (pages == null || pages.isJsonNull()) throw new IllegalArgumentException("Wiki returned no monster page.");
        JsonObject page;
        if (pages.isJsonArray())
        {
            if (pages.getAsJsonArray().size() == 0) throw new IllegalArgumentException("Wiki returned no monster page.");
            page = pages.getAsJsonArray().get(0).getAsJsonObject();
        }
        else
        {
            if (pages.getAsJsonObject().entrySet().isEmpty()) throw new IllegalArgumentException("Wiki returned no monster page.");
            page = pages.getAsJsonObject().entrySet().iterator().next().getValue().getAsJsonObject();
        }
        if (page.has("missing") || !page.has("revisions") || page.getAsJsonArray("revisions").size() == 0)
            throw new IllegalArgumentException("No Wiki page found. Use an exact monster name, not a Slayer category.");
        JsonObject revision = page.getAsJsonArray("revisions").get(0).getAsJsonObject();
        JsonObject main = revision.getAsJsonObject("slots").getAsJsonObject("main");
        JsonElement content = main.has("*") ? main.get("*") : main.get("content");
        if (content == null || content.isJsonNull()) throw new IllegalArgumentException("Wiki page content was unavailable.");
        return SlayerWikiGuide.parse(page.get("title").getAsString(), revision.get("revid").getAsLong(), content.getAsString());
    }
}
