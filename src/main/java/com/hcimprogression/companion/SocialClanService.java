package com.hcimprogression.companion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;

public class SocialClanService
{
    private static final int MAX_MEMBERS = 500;

    public SocialClanSnapshot createSnapshot(Client client)
    {
        ClanChannel channel = client.getClanChannel();
        ClanSettings settings = client.getClanSettings();
        if (channel == null && settings == null)
        {
            return null;
        }

        SocialClanSnapshot snapshot = new SocialClanSnapshot();
        snapshot.setClanName(clean(channel != null ? channel.getName() : settings.getName(), 50));
        snapshot.setTimestamp(System.currentTimeMillis());

        Map<String, ClanChannelMember> onlineMembers = new LinkedHashMap<>();
        if (channel != null && channel.getMembers() != null)
        {
            for (ClanChannelMember member : channel.getMembers())
            {
                if (member == null || member.getName() == null)
                {
                    continue;
                }
                onlineMembers.put(normalize(member.getName()), member);
            }
        }

        Player localPlayer = client.getLocalPlayer();
        String localName = localPlayer == null ? "" : localPlayer.getName();
        if (settings != null)
        {
            ClanMember localMember = settings.findMember(localName);
            snapshot.setPlayerRank(localMember == null ? "Member" : rankTitle(settings, localMember));

            List<ClanMember> members = settings.getMembers();
            if (members != null)
            {
                for (ClanMember member : members)
                {
                    if (member == null || snapshot.getMembers().size() >= MAX_MEMBERS)
                    {
                        break;
                    }
                    String name = clean(member.getName(), 50);
                    if (name.isEmpty())
                    {
                        continue;
                    }
                    ClanChannelMember online = onlineMembers.remove(normalize(name));
                    LocalDate joined = member.getJoinDate();
                    snapshot.getMembers().add(new SocialClanSnapshot.ClanMemberSnapshot(
                        name,
                        rankTitle(settings, member),
                        online == null ? 0 : online.getWorld(),
                        online != null,
                        joined == null ? "" : joined.toString()
                    ));
                }
            }
        }
        else
        {
            snapshot.setPlayerRank("Member");
        }

        // If clan settings have not loaded yet, or the channel contains a member
        // omitted from the settings snapshot, preserve the online channel member.
        for (ClanChannelMember member : onlineMembers.values())
        {
            if (snapshot.getMembers().size() >= MAX_MEMBERS)
            {
                break;
            }
            snapshot.getMembers().add(new SocialClanSnapshot.ClanMemberSnapshot(
                clean(member.getName(), 50),
                rankTitle(settings, member),
                member.getWorld(),
                true,
                ""
            ));
        }

        return snapshot;
    }

    private String rankTitle(ClanSettings settings, ClanMember member)
    {
        return rankTitle(settings, member == null ? null : member.getRank());
    }

    private String rankTitle(ClanSettings settings, ClanChannelMember member)
    {
        return rankTitle(settings, member == null ? null : member.getRank());
    }

    private String rankTitle(ClanSettings settings, net.runelite.api.clan.ClanRank rank)
    {
        if (rank == null)
        {
            return "Member";
        }
        if (settings != null)
        {
            ClanTitle title = settings.titleForRank(rank);
            if (title != null && title.getName() != null && !title.getName().trim().isEmpty())
            {
                return clean(title.getName(), 40);
            }
        }
        String fallback = rank.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
        if (fallback.isEmpty())
        {
            return "Member";
        }
        return Character.toUpperCase(fallback.charAt(0)) + fallback.substring(1);
    }

    private String normalize(String value)
    {
        return String.valueOf(value == null ? "" : value)
            .replace('\u00A0', ' ')
            .trim()
            .toLowerCase(Locale.ENGLISH);
    }

    private String clean(String value, int max)
    {
        String cleaned = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
