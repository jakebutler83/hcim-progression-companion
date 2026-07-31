# HCIM Progression Companion

HCIM Progression Companion is a RuneLite plugin that connects your RuneLite client to the HCIM Progression website.

It allows your Group Hardcore Ironman team to automatically synchronize progression without manually checking off every task.

## Features

- Automatic quest completion syncing
- One-click account snapshot syncing
- Skill level progression updates
- Opt-in Group Storage snapshots for the private website Group Bank
- Live player location updates
- Privacy-aware Social Hub presence for approved friends
- Live world, broad region, activity, combat level, and worn equipment
- Private RuneScape clan roster, rank, online-world, and in-game clan-event syncing
- Secure per-player connection using a one-time website link code
- Supports multiple HCIM groups independently

## Setup

1. Install the plugin from the RuneLite Plugin Hub.
2. Log into your HCIM Progression account.
3. Open the **Group** page.
4. Generate a one-time companion link code.
5. Enter the code in the RuneLite panel.
6. Click **Sync Account Now** to upload current skills and completed quests.
7. Leave **Share Social Hub presence** enabled to appear to approved friends.
8. Leave **Sync clan roster** enabled to populate the Clan Hall.
9. Leave **Sync clan events** enabled, then open **Clan Settings → Events** in game whenever you want to import or refresh the clan calendar.
10. Enable **Sync Group Storage**, then open the shared storage in game whenever you want to refresh the website Group Bank.
11. Enable exact location sharing only if you want the HCIM live map and optional exact Social Hub pins.

Once connected, supported progression updates will automatically appear on your group's dashboard.

## Privacy

The plugin communicates only with the configured Progression Path service using a private linked-device token.

Social presence is visible only to approved Progression Path friends and is filtered by the website privacy controls. Exact location is disabled by default, and Wilderness safety can suppress exact coordinates.

Clan event syncing is read-only: it imports the visible in-game calendar into Progression Path but never creates or edits RuneScape clan events.

Group Storage syncing is read-only and disabled by default. RuneLite uploads a private last-seen item snapshot only while the shared storage interface is open; it never deposits, withdraws, or changes any item.

No gameplay automation is performed.

No account passwords or RuneLite credentials are transmitted.

## Website

https://progressionpath.netlify.app

## Support

Report bugs or request features through the project's GitHub Issues page.
