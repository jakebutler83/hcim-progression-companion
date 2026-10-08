package com.hcimprogression.companion;

import com.google.gson.Gson;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SyncServiceTest
{
    @Test
    public void invalidApiUrlReturnsAnErrorInsteadOfLeavingLinkPending()
    {
        SyncService service = new SyncService(new OkHttpClient(), new Gson());
        AtomicReference<SyncService.LinkResult> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        service.exchangeCode("http://invalid.example", "ABCD1234", (linked, message) -> {
            result.set(linked);
            error.set(message);
        });

        assertNull(result.get());
        assertEquals("API URL must begin with https://", error.get());
    }
}
