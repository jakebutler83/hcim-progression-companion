package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

public class SocialClanSnapshot
{
    private String clanName;
    private String playerRank;
    private long timestamp;
    private final List<ClanMemberSnapshot> members = new ArrayList<>();

    public String getClanName() { return clanName; }
    public void setClanName(String clanName) { this.clanName = clanName; }
    public String getPlayerRank() { return playerRank; }
    public void setPlayerRank(String playerRank) { this.playerRank = playerRank; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<ClanMemberSnapshot> getMembers() { return members; }

    public static class ClanMemberSnapshot
    {
        private final String name;
        private final String rank;
        private final int world;
        private final boolean online;
        private final String joinDate;

        public ClanMemberSnapshot(String name, String rank, int world, boolean online, String joinDate)
        {
            this.name = name;
            this.rank = rank;
            this.world = world;
            this.online = online;
            this.joinDate = joinDate;
        }

        public String getName() { return name; }
        public String getRank() { return rank; }
        public int getWorld() { return world; }
        public boolean isOnline() { return online; }
        public String getJoinDate() { return joinDate; }
    }
}
