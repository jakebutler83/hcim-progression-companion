package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

public class SocialClanEventSnapshot
{
    private String clanName;
    private String importedBy;
    private long timestamp;
    private final List<ClanEventSnapshot> events = new ArrayList<>();

    public String getClanName() { return clanName; }
    public void setClanName(String clanName) { this.clanName = clanName; }
    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<ClanEventSnapshot> getEvents() { return events; }

    public String fingerprint()
    {
        StringBuilder value = new StringBuilder();
        value.append(clanName).append('|');
        for (ClanEventSnapshot event : events)
        {
            value.append(event.getTitle()).append('|')
                .append(event.getStartAt()).append('|')
                .append(event.getDurationDays()).append('|')
                .append(event.getWorld()).append('|')
                .append(event.getType()).append('|')
                .append(event.getSubType()).append('|')
                .append(event.getRequiredRank()).append('|')
                .append(event.getCreatedBy()).append(';');
        }
        return Integer.toHexString(value.toString().hashCode());
    }

    public static class ClanEventSnapshot
    {
        private final String title;
        private final String type;
        private final String subType;
        private final String requiredRank;
        private final String createdBy;
        private final int world;
        private final int durationDays;
        private final long startAt;
        private final long endAt;

        public ClanEventSnapshot(
            String title,
            String type,
            String subType,
            String requiredRank,
            String createdBy,
            int world,
            int durationDays,
            long startAt,
            long endAt)
        {
            this.title = title;
            this.type = type;
            this.subType = subType;
            this.requiredRank = requiredRank;
            this.createdBy = createdBy;
            this.world = world;
            this.durationDays = durationDays;
            this.startAt = startAt;
            this.endAt = endAt;
        }

        public String getTitle() { return title; }
        public String getType() { return type; }
        public String getSubType() { return subType; }
        public String getRequiredRank() { return requiredRank; }
        public String getCreatedBy() { return createdBy; }
        public int getWorld() { return world; }
        public int getDurationDays() { return durationDays; }
        public long getStartAt() { return startAt; }
        public long getEndAt() { return endAt; }
    }
}
