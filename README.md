# HCIM Progression Companion

HCIM Progression Companion is a RuneLite plugin that connects your RuneLite client to the HCIM Progression website.

It allows your Group Hardcore Ironman team to automatically synchronize progression without manually checking off every task.

## Features

- Automatic quest completion syncing
- One-click account snapshot syncing
- Optional batched automatic account snapshots after skill, quest, diary, or collection-log changes
- Skill level progression updates
- Official hiscore boss KC syncing for the website Luck Tracker
- Brutus KC support through RuneLite's current official hiscore catalog
- Opt-in Group Storage snapshots for the private website Group Bank
- Change-driven live player location updates with a five-minute heartbeat
- Privacy-aware Social Hub presence for approved friends
- Live world, broad region, activity, combat level, and worn equipment
- Private RuneScape clan roster, rank, online-world, and in-game clan-event syncing, with roster uploads only when the roster changes and no more than once every five minutes
- Secure per-player connection using a one-time website link code
- Supports multiple HCIM groups independently

## Setup

1. Install the plugin from the RuneLite Plugin Hub.
2. Log into your HCIM Progression account.
3. Open the **Group** page.
4. Generate a one-time companion link code.
5. Enter the code in the RuneLite panel.
6. Click **Sync Account Now** to upload current skills and completed quests.
7. Enable **Share Social Hub presence** to appear to approved friends.
8. Enable **Sync clan roster** to populate the Clan Hall.
9. Enable **Sync clan events**, then open **Clan Settings → Events** in game whenever you want to import or refresh the clan calendar.
10. Enable **Track NPC loot value** if you want estimated loot gains in Weekly Snapshot. The total is sent only when you manually press **Sync Account**.
11. Enable **Sync Group Storage**, then open the shared storage in game whenever you want to refresh the website Group Bank.
12. Enable exact location sharing only if you want the HCIM live map and optional exact Social Hub pins.
13. Enable **Automatic account snapshots** if you want the companion to queue full progression uploads automatically. Changes are coalesced during a short quiet period, rate-limited to one full snapshot every five minutes, and periodically refreshed while logged in.

Once connected, supported progression updates will automatically appear on your group's dashboard.

### Pre-release beta testers

Until the Plugin Hub submission is approved, testers can run the public source through RuneLite's development client. See [BETA_TESTING.md](BETA_TESTING.md) and double-click `Launch-Beta-Client.cmd` after completing the Jagex Account development setup. Never share `.runelite/credentials.properties`, passwords, device tokens, or one-time link codes.

Live location and Social Hub presence share one request, refresh every five minutes, and update earlier after meaningful world, region, activity, equipment, or sharing changes. Randomized timing prevents large groups of clients from syncing together, and network failures use exponential retry backoff. Automatic account snapshots are opt-in, coalesce skill/quest/diary/collection-log changes, and keep the manual Account Sync button available for an immediate refresh. Hiscore-backed boss KC and clue totals still follow the official hiscore update timing.

## Privacy

The plugin communicates only with the configured Progression Path service using a private linked-device token.

The production API is hosted at `https://hcim-companion-api-973141269474.us-central1.run.app`. The previous Netlify function URL remains a rollback option during the v10.29.0 transition.

Social presence is visible only to approved Progression Path friends and is filtered by the website privacy controls. Exact location is disabled by default, and Wilderness safety can suppress exact coordinates.

Clan event syncing is read-only: it imports the visible in-game calendar into Progression Path but never creates or edits RuneScape clan events.

Group Storage syncing is read-only and disabled by default. RuneLite uploads a private last-seen item snapshot only while the shared storage interface is open; it never deposits, withdraws, or changes any item.

Weekly loot tracking is also disabled by default. When enabled, the companion records the current Grand Exchange value of NPC loot locally. Progression Path receives only cumulative value and loot-pile totals during a manual Account Sync, then calculates the gain since the previous compatible sync. It does not sell, alch, drop, or otherwise interact with any item.

## Plugin Hub releases

Pushing source changes to `main` updates this repository but does not immediately update the public Plugin Hub build. Publish a GitHub release, or manually run the `Submit Plugin Hub release` workflow, when a tested companion version is ready. The workflow opens or refreshes a Plugin Hub pull request for RuneLite maintainer review, so ordinary development commits do not bypass the public release process.

One-time setup: add a companion repository Actions secret named `PLUGIN_HUB_TOKEN`. Use a fine-grained GitHub token limited to the `jakebutler83/plugin-hub` repository with **Contents: Read and write** permission. Until this secret exists, the workflow exits safely without changing either repository.

No gameplay automation is performed.

No account passwords or RuneLite credentials are transmitted.

## Website

https://progressionpath.netlify.app

## Support

Report bugs or request features through the project's GitHub Issues page.
