# HCIM Progression Companion

HCIM Progression Companion is a RuneLite plugin that connects your RuneLite client to the HCIM Progression website.

It allows your Group Hardcore Ironman team to automatically synchronize progression without manually checking off every task.

## Features

- Automatic quest completion syncing
- One-click account snapshot syncing
- Optional batched automatic account snapshots after skill, quest, diary, or collection-log changes
- Farm-run snapshots and harvest reminders derived from RuneLite Time Tracking's per-profile patch states and farming-tick calibration
- Read-only Kingdom of Miscellania coffer and approval snapshots using RuneLite's built-in kingdom state
- Daily reward availability from RuneLite's Daily Task Indicator sources
- Exact rune-pouch contents for character-specific supply and loadout guidance
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

## Optional in-game progression displays (development)

These features require the matching website/API deployment before end-to-end
testing. They are off by default and do not automate any gameplay.

- **Progression completion pop-ups** displays newly completed quests, diary
  tiers, and supported skill/clue/boss milestones after a successful game-event
  account sync. Enable **Automatic account snapshots** too. Its existing
  quiet-period batching and five-minute upload cooldown still apply, so this is
  not an instant quest-completion replacement. With pop-ups enabled, early skill
  level transitions also queue a sync; default sync behavior is unchanged.
- **Pinned website goal** displays the task selected with **Pin next task in
  RuneLite** on the dashboard's current account goal. **Unpin** removes it. The
  companion privately checks only the linked character's saved task about once
  per minute while logged in. A failed refresh clears the display and backs off.
- **Also notify outside the game** optionally forwards completion messages to
  RuneLite's notification system. Sound, tray, focus and flash behavior follow
  the user's normal RuneLite settings; this option is separate from the overlay.
- **Quest completion pop-ups**, **Diary completion pop-ups**, and **Milestone
  completion pop-ups** are local category filters. They do not enable network
  features by themselves: the master completion option and automatic snapshots
  remain off by default. Muting a category removes its visible/queued pop-ups
  and desktop alerts; turning it back on does not replay muted achievements.
- **RECENT COMPLETIONS**, inside **IN-GAME DISPLAYS**, keeps the last ten proven
  new completions, newest first, with category and local receipt time. Muted
  categories still appear while the master completion option is enabled. This
  is an in-memory session list, not an achievement archive: it is never uploaded
  or persisted. Login backfills/manual refreshes do not populate it. Logout,
  character/profile change, relinking, and changing master display settings
  clear it. Changing only category filters keeps the list intact.
- **Local farm / birdhouse reminders** is a separate, local-only option. It
  reads the current character's saved RuneLite Time Tracking records about every
  ten seconds and announces growing-to-ready transitions. It needs neither a
  website link nor automatic snapshots. Farming deadlines are estimates: check
  the patch rather than treating the reminder as proof of crop health.
- **Also notify for ready routines** optionally adds RuneLite sound/tray alerts.
  Leave it off if Time Tracking already sends these notifications, to avoid
  duplicate desktop alerts. Both local settings are disabled by default.
- The collapsed **IN-GAME DISPLAYS** panel explains which options are active,
  whether completion pop-ups need auto sync, and whether a pinned goal is
  loading, unavailable, absent, or complete. **Preview local reminder** is a
  user-triggered overlay preview; it never modifies real timers or achievements.

Login/refresh establishes a silent baseline. Repeated events are deduplicated,
queues are bounded, and logout, character switching, relinking, or changing the
display settings invalidates pending callbacks. Opening old Collection Log
pages does not create item-completion pop-ups.

Birdhouse snapshots now prefer RuneLite's existing `timetracking` profile records
over the companion's fallback timestamps. Character and RuneScape profile
changes clear in-memory farm/birdhouse caches. Local reminders intentionally
ignore fallback guesses and suppress already-ready timers on login/re-enable.
The record format is documented by the upstream
[RuneLite birdhouse tracker](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/hunter/BirdHouseTracker.java).

### User testing

Launch the development client with `gradlew.bat run`. Local reminder tests
(steps 6–7) do not need any deployment. Website/API/Firebase deployment is
required for the linked goal/completion tests (steps 1–5). For Jagex Accounts, first follow
[RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
Only the user may operate/log into RuneScape; do not automate in-game tests.

1. Link the correct character and enable both display settings plus automatic
   snapshots. Login/manual sync must not announce old achievements.
2. Pin the dashboard's next task. Verify it appears within about one minute;
   unpin it and verify the overlay disappears on the next refresh.
3. Complete a new quest, diary tier, or supported milestone. Allow the normal
   account-sync cooldown, then verify one eight-second completion message.
   Repeat manual sync and reconnect; that completion must not replay.
4. Switch between two characters on one website login, including switching
   while a goal request is in flight. Neither goal nor pop-up may cross over.
5. Disable the displays, log out, and briefly lose network access. No stale
   goal should remain visible or pop-ups continue playing after disabling.
6. Local reminders can be tested **before any deployment**. Enable Time
   Tracking and **Local farm / birdhouse reminders**, open **IN-GAME DISPLAYS**,
   then click **Preview local reminder**. Verify the compact reminder disappears
   after eight seconds and does not change a real timer. A website link is not
   required; desktop sound is separately opt-in.
7. Observe a growing birdhouse or planted patch, wait for its recorded deadline,
   and verify one reminder. Already-ready timers at login must be silent.
   Replant/reseed for another cycle, and switch RuneScape characters/profiles;
   reminders must never follow the previous character.
8. With completion feedback enabled, mute one category in plugin settings and
   complete a new achievement in that category. After its automatic sync, verify
   it appears once in **RECENT COMPLETIONS** but produces no pop-up or desktop
   alert. Other categories and routine reminders should still work. Re-enable
   the category: no old message should replay. Switch characters/log out and
   confirm the recent list clears. Linked testing requires the matching API.

Automated tests/compilation do not establish that these behaviors work in-game.

## Personal task pins and routine routes

- Website Journey and Home checklist tasks have a small pin button, including
  minigame goals. Boss Hub cards have separate activity pins. A boss activity is
  a focus label, not an invented completion counter or combat helper.
- Enable **Local route helper** and open the sidebar's **ROUTE HELPER** card.
  Farm and birdhouse setups have editable packing lists and ordered stops.
  Review your quests, levels, teleports and supplies before starting. **Next**,
  **Back**, **Stop** and **Start / restart route** are manual checklist controls.
  No game inputs, pathfinding, scene indicators or crop-timer changes occur.
- Local setups are saved per RuneScape profile and character. Separately enable
  **Route setup sync** (third-party opt-in), link the character, and choose
  **Load from website** before **Save to website**. A stale revision is rejected
  rather than overwriting another device's edits. Network errors retain local
  edits. These routes live privately under the permanent website character ID.
- On the website, edit the matching route in Farm Runs or Birdhouse Tracker.
  Starter birdhouse logs use synced Hunter/Crafting levels when available;
  first-run clockworks and repeat-run reuse are distinguished. Travel methods
  are suggestions requiring actual unlocks, not proof of access.

Manual acceptance test: pin a different Journey task and a boss activity; verify
the correct in-game goal and unpin. Edit one farm setup locally, start it, use
Back/Next and Stop, then restart the four-stop birdhouse route. Load/edit/upload
and check the website reflects this character's route. Edit it on the website
and try a stale plugin upload: it must reject while retaining local edits. Log
out and switch characters/RS profiles during a request; no old route or response
may appear on the new character. All new options default off. Public Plugin Hub
release still requires user in-game confirmation and maintainer review.

## Local packing checks, skill goals and session controls (development)

These additions are entirely local and do not turn on account snapshots, bank
uploads or route sync. **Live packing checks**, **Local skill goals**, **Local
session summary**, and skill-goal desktop notifications default off.

- Enable **Local route helper** and **Live packing checks**. In **ROUTE HELPER**,
  use exact in-game names and quantities, for example `Hammer x 1`, `Oak logs x 4`,
  and `Barley seed x 40`. Simple legacy tool lines such as `Hammer` also work.
  Checks update after inventory/equipment changes and combine carried stacks
  with worn gear; noted items, placeholders and bank contents do not count.
  Duplicate list entries consume the remaining quantity rather than counting
  the same items twice. Conditional instructions and generic ingredients stay
  **Review manually**. Names/charge variants must match exactly; this is not a
  charge, teleport-unlock or farming-access validator. Missing inventory data
  stays **Waiting**, never falsely **Ready**. No items are moved or highlighted.
- Packing results in the sidebar apply to the editable list. Starting a route
  freezes its checklist: the overlay's packing summary uses that running copy.
  Edited instructions apply on the next Start/restart. Missing supplies do not
  block Next or automatically advance the route.
- Enable **Local skill goals**, then open **SESSION GOALS & STATS**. Pick any
  skill, choose **Gain XP this session** or **Reach a level**, enter the XP gain
  or real target level (2–99), then **Start skill goal**. Live XP events update
  the bar; boosted levels do not prove completion. Targets already reached or
  above 200M XP are rejected. Only one local skill goal is active at a time;
  completion is announced once. **Clear skill goal** removes it. Optional
  desktop alerts follow RuneLite's notification preferences.
- **Local session summary** records XP, real levels and completed local skill
  goals since enabling it this login. **Restart session summary** establishes a
  fresh baseline without replacing the active goal. This summary does not yet
  count loot, boss kills or website completions. Local goals and statistics reset
  on logout, character/profile switch or plugin shutdown. Nothing is uploaded
  unless the separate Skill goal website sync option is enabled (see below).
  World hopping normally preserves the session while logged in.
- **Pop-up duration (seconds)** sets new completion/routine/local-goal notices
  to 3–20 seconds (default 8). **Compact progression display** reduces overlay
  width and hides supplementary instructions, keeping goal/route information.
  In **SESSION GOALS & STATS**, **Mute pop-ups for 15 minutes** suppresses this
  plugin's pop-ups and desktop alerts, but not its goal/route displays or other
  plugins' notifications. **Unmute pop-ups** ends it early. Muted notifications
  are discarded, not replayed; recent website completions still populate their
  session list. The mute lasts across account changes in this running plugin,
  but is not persisted across a plugin/client restart.

Manual acceptance before public release:

1. Enable the local options without enabling website sync. With `Hammer x 1`
   and `Oak logs x 4`, add/remove carried items: ready/missing must change within
   a game tick. Notes should stay manual; noted logs must not count. A worn
   exact-name item should count. Verify duplicate requirements are not reused.
2. Start a small Mining XP goal, gain XP in another skill (no goal progress),
   then train Mining: bar, remaining XP and one completion alert should update.
   Test a level target, an already-achieved level, invalid input, Clear, and
   replacing a completed goal. No website connection is required.
3. Check session XP/levels, restart the summary (goal unchanged), and verify
   the counters restart from current XP. Hop worlds, then log out and switch
   accounts/RS profiles: no previous character's goal, packing or statistics
   should appear. Disable/re-enable the local options; no old completion should
   replay. Toggling the summary starts a new baseline.
4. Test compact mode and 3-/20-second durations. Mute, complete a small skill
   goal and preview a local routine reminder: no pop-up/desktop alert. Unmute
   must not replay either. Goal state remains visible. Other plugins' alerts
   are intentionally unaffected. Confirm normal notifications resume for a
   new goal/reminder.

Automated checks currently pass; these new additions still require the user's
live in-game confirmation. No Plugin Hub release or iOS upload is triggered.

## Compact sidebar and optional Journey skill-goal sync (development)

The new helper cards share a left-aligned, width-constrained layout with plain
wrapped text instead of fixed-width HTML labels. **ROUTE HELPER** keeps route
selection, Start/Restart, Back/Next/Stop and a short packing summary up front.
**Packing details**, **Edit route**, **Website sync** and **How it works** expand
only when needed. Editors and packing details have bounded vertical scroll
areas and no horizontal scrolling. **Load route** / **Save route**, inside
Website sync, are the existing website download/upload actions with shorter
labels and full tooltips; saved routes/config keys are unchanged.

**SESSION GOALS & STATS** keeps the skill target, Start/Clear and progress up
front, with expandable Session summary, Notifications and Website sync cards.
Buttons are shorter: **Restart summary**, **Pause 15 min**, **Resume alerts**.

**Skill goal website sync** is a separate third-party opt-in and defaults off.
Local goals/statistics/packing still require no connection or uploads. With
this option enabled and this character linked, newly started skill goals save
to the character's private Journey tasks. Progress is coalesced about every two
minutes; starts, completions, replacing/clearing an active goal send sooner.
**Send current goal** can retry the current goal after enabling/linking. Late
callbacks and queued records are discarded on logout/profile/character/link
changes. No per-XP-drop network requests are made.

Website records use permanent character IDs, not shared login slots, and never
modify the group's shared custom-task array. Journey has an **Active tasks**
panel above the roadmap and a RuneLite skill-goal history inside the existing
custom-task section. Completed/stopped goals leave the active list. Stored
website tasks remain after the local session resets on logout; progress is
last-synced, not a guarantee of live tracking when the companion is closed.
**Stop tracking on website** removes a task from active focus without changing
the local goal. Stopped/completed records cannot be revived by delayed uploads.
Account deletion recursively deletes these private goal records.

Release requirements: deploy the matching API, Firestore rules and website
before testing the new sync end-to-end. Until then the opt-in sync may report
unavailable and retains the local goal. Public Plugin Hub submission and iOS
upload remain held until the user requests release.

Manual checks: expand each compact section; verify long packing notes wrap
without shifting right and all Back/Next/Stop buttons fit. Save/load a route
to confirm existing setups remain intact. Enable Skill goal website sync, start
a small XP/level goal, verify it appears for this character in both Journey
locations, complete it, and confirm it leaves Active tasks. Stop another goal
on the website and gain more XP: it must stay stopped there. Switch accounts
during an upload; the new character must never inherit the old goal.

## Plugin Hub releases

Pushing source changes to `main` updates this repository but does not immediately update the public Plugin Hub build. Publish a GitHub release, or manually run the `Submit Plugin Hub release` workflow, when a tested companion version is ready. The workflow opens or refreshes a Plugin Hub pull request for RuneLite maintainer review, so ordinary development commits do not bypass the public release process.

One-time setup: add a companion repository Actions secret named `PLUGIN_HUB_TOKEN`. Use a fine-grained GitHub token limited to the `jakebutler83/plugin-hub` repository with **Contents: Read and write** permission. Until this secret exists, the workflow exits safely without changing either repository.

No gameplay automation is performed.

No account passwords or RuneLite credentials are transmitted.

## Website

https://progressionpath.netlify.app

## Support

Report bugs or request features through the project's GitHub Issues page.
