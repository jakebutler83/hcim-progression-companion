package com.hcimprogression.companion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

public class SocialClanEventService
{
    private static final int MAX_EVENTS = 100;
    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d-MMM-uuuu")
        .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);

    public SocialClanEventSnapshot createSnapshot(Client client)
    {
        Widget worldWidget = client.getWidget(InterfaceID.ClansEvents.LIST_CONTENTS_WORLD);
        if (worldWidget == null)
        {
            return null;
        }

        Widget[] worlds = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_WORLD);
        Widget[] dates = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_DATE);
        Widget[] times = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_TIME);
        Widget[] durations = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_DURATION);
        Widget[] types = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_TYPE);
        Widget[] activities = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_ACTIVITY);
        Widget[] subTypes = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_SUBTYPE);
        Widget[] ranks = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_RANK_TO_VIEW);
        Widget[] creators = dynamicChildren(client, InterfaceID.ClansEvents.LIST_CONTENTS_CREATOR);

        SocialClanEventSnapshot snapshot = new SocialClanEventSnapshot();
        snapshot.setClanName(clanName(client));
        snapshot.setImportedBy(localPlayerName(client));
        snapshot.setTimestamp(System.currentTimeMillis());

        int count = minLength(worlds, dates, times, activities);
        count = Math.min(count, MAX_EVENTS);
        for (int i = 0; i < count; i++)
        {
            String title = cleanText(activities[i]);
            if (title.isEmpty())
            {
                continue;
            }

            String dateText = valueAfterLabel(cleanText(dates[i]), "Date");
            String timeText = valueAfterLabel(cleanText(times[i]), "Time").replace("UTC", "").trim();
            long startAt = parseStart(dateText, timeText);
            if (startAt <= 0)
            {
                continue;
            }

            int durationDays = clamp(parseFirstInteger(cleanTextAt(durations, i)), 1, 365);
            int world = clamp(parseFirstInteger(cleanTextAt(worlds, i)), 0, 999);
            long endAt = startAt + durationDays * 24L * 60L * 60L * 1000L;

            snapshot.getEvents().add(new SocialClanEventSnapshot.ClanEventSnapshot(
                clean(title, 100),
                clean(valueAfterLabel(cleanTextAt(types, i), "Type"), 60),
                clean(valueAfterLabel(cleanTextAt(subTypes, i), "Sub Type"), 60),
                clean(valueAfterLabel(cleanTextAt(ranks, i), "Rank"), 50),
                clean(valueAfterLabel(cleanTextAt(creators, i), "By"), 50),
                world,
                durationDays,
                startAt,
                endAt
            ));
        }

        return snapshot;
    }

    private Widget[] dynamicChildren(Client client, int componentId)
    {
        Widget widget = client.getWidget(componentId);
        Widget[] children = widget == null ? null : widget.getDynamicChildren();
        return children == null ? new Widget[0] : children;
    }

    private int minLength(Widget[]... arrays)
    {
        int minimum = Integer.MAX_VALUE;
        for (Widget[] array : arrays)
        {
            minimum = Math.min(minimum, array == null ? 0 : array.length);
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
    }

    private long parseStart(String dateText, String timeText)
    {
        try
        {
            LocalDate date = LocalDate.parse(dateText.trim(), DATE_FORMAT);
            LocalTime time = LocalTime.parse(timeText.trim(), TIME_FORMAT);
            return date.atTime(time).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        catch (DateTimeParseException ignored)
        {
            return 0;
        }
    }

    private int parseFirstInteger(String value)
    {
        String digits = String.valueOf(value == null ? "" : value).replaceAll("[^0-9]", " ").trim();
        if (digits.isEmpty())
        {
            return 0;
        }
        String first = digits.split("\\s+")[0];
        try
        {
            return Integer.parseInt(first);
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    private String valueAfterLabel(String value, String label)
    {
        String cleaned = clean(value, 120);
        String prefix = label + ":";
        if (cleaned.regionMatches(true, 0, prefix, 0, prefix.length()))
        {
            return cleaned.substring(prefix.length()).trim();
        }
        return cleaned;
    }


    private String cleanTextAt(Widget[] widgets, int index)
    {
        if (widgets == null || index < 0 || index >= widgets.length)
        {
            return "";
        }
        return cleanText(widgets[index]);
    }

    private String cleanText(Widget widget)
    {
        return widget == null ? "" : clean(Text.removeTags(widget.getText()), 120);
    }

    private String clanName(Client client)
    {
        ClanChannel channel = client.getClanChannel();
        if (channel != null && channel.getName() != null && !channel.getName().trim().isEmpty())
        {
            return clean(channel.getName(), 50);
        }
        ClanSettings settings = client.getClanSettings();
        return settings == null ? "" : clean(settings.getName(), 50);
    }

    private String localPlayerName(Client client)
    {
        Player player = client.getLocalPlayer();
        return player == null ? "" : clean(player.getName(), 20);
    }

    private int clamp(int value, int minimum, int maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String clean(String value, int max)
    {
        String cleaned = value == null ? "" : value.replace('\u00A0', ' ').replaceAll("[\\r\\n\\t]+", " ").trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
