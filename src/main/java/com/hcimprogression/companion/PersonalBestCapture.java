package com.hcimprogression.companion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;

final class PersonalBestCapture
{
    static final String QUEST_GROUP = "hcimprogressionquestpb";
    private static final Pattern TIME = Pattern.compile("(?<![0-9:.])([0-9]{1,2}:)?([0-9]{1,3}):([0-9]{2}(?:\\.[0-9]{1,3})?)(?![0-9:.])");

    static boolean validKey(String key)
    {
        return key != null && key.length() <= 160 && key.matches("[a-z0-9][a-z0-9 ',.():/+\\-]{0,159}")
            && !key.equals("constructor") && !key.equals("prototype");
    }

    static long milliseconds(String seconds)
    {
        try
        {
            if (seconds == null || seconds.length() > 40) return 0L;
            double bounded = Double.parseDouble(seconds);
            if (!Double.isFinite(bounded) || bounded <= 0 || bounded > 86400) return 0L;
            BigDecimal value = new BigDecimal(seconds).multiply(BigDecimal.valueOf(1000));
            long result = value.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
            return result > 0 && result <= 86_400_000L ? result : 0L;
        }
        catch (RuntimeException invalid) { return 0L; }
    }

    static long parseTime(String text)
    {
        Matcher match = TIME.matcher(text == null ? "" : text.replaceAll("<[^>]*>|@[^@]+@", ""));
        if (!match.find()) return 0L;
        int minutes = Integer.parseInt(match.group(2));
        BigDecimal seconds = new BigDecimal(match.group(3));
        if (seconds.compareTo(BigDecimal.valueOf(60)) >= 0 || (match.group(1) != null && minutes >= 60)) return 0L;
        int hours = match.group(1) == null ? 0 : Integer.parseInt(match.group(1).replace(":", ""));
        return milliseconds(seconds.add(BigDecimal.valueOf(hours * 3600L + minutes * 60L)).toPlainString());
    }

    // ConfigManager owns the RS profile. Never read a previous account's profile
    // during a login transition, nor combine seasonal records with Standard PBs.
    static String mode(Client client, ConfigManager config)
    {
        return modeForProfile(config.getRSProfileKey(),client.getAccountHash(),RuneScapeProfileType.getCurrent(client),config.getRSProfiles());
    }

    static String modeForProfile(String key, long hash, RuneScapeProfileType type, List<RuneScapeProfile> profiles)
    {
        if (key == null || hash == -1L || hash == 0L) return "";
        for (RuneScapeProfile profile : profiles)
        {
            if (key.equals(profile.getKey()) && profile.getAccountHash() == hash && profile.getType() == type)
            {
                String mode = profile.getType().name();
                return mode.equals("STANDARD") || mode.equals("QUEST_SPEEDRUNNING") ? mode : "";
            }
        }
        return "";
    }

    static List<PersonalBestRecord> read(ConfigManager config, String mode)
    {
        if (mode.isEmpty()) return Collections.emptyList();
        List<PersonalBestRecord> records = new ArrayList<>();
        readGroup(config, "personalbest", mode, "runelite-personalbest", records);
        if (mode.equals("QUEST_SPEEDRUNNING")) readGroup(config, QUEST_GROUP, mode, "game-quest-speedrun", records);
        return records;
    }

    private static void readGroup(ConfigManager config, String group, String mode, String source, List<PersonalBestRecord> records)
    {
        List<String> keys = new ArrayList<>(config.getRSProfileConfigurationKeys(group, config.getRSProfileKey(), ""));
        Collections.sort(keys);
        for (String raw : keys)
        {
            if (records.size() >= 256) break;
            String key = raw.toLowerCase(Locale.ROOT);
            if (!validKey(key)) continue;
            String value = config.getRSProfileConfiguration(group, raw);
            long time = milliseconds(value);
            if (time > 0) records.add(new PersonalBestRecord(key, mode, source, time));
        }
    }
}
