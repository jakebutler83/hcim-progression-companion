# Progression Path Companion beta testing

Thank you for testing Progression Path before its RuneLite Plugin Hub release. This beta runs the public companion source in RuneLite's development client. It does not use a modified RuneLite installer or an unofficial plugin loader.

## What you need

- Windows 10 or 11
- The official RuneLite and Jagex Launcher
- [Eclipse Temurin JDK 11](https://adoptium.net/temurin/releases/?version=11)
- A Progression Path website account
- A complete clone or ZIP extraction of this repository

Do not share your RuneScape password, Jagex login, Progression Path password, device token, or `.runelite/credentials.properties` file with anyone, including the Progression Path team.

## 1. Download the beta

Choose one method:

- Clone `https://github.com/jakebutler83/hcim-progression-companion.git`, or
- On GitHub, choose **Code → Download ZIP**, then extract the entire ZIP before launching it.

Do not run the launcher from inside the ZIP preview. `Launch-Beta-Client.cmd`, `gradlew.bat`, `build.gradle`, and the `src` folder must remain together.

## 2. Prepare a Jagex Account for a development client

RuneLite's official development instructions require a one-time local credential handoff:

1. Confirm the RuneLite launcher is version 2.6.3 or newer.
2. Open **RuneLite (configure)** from the Windows Start menu.
3. Add `--insecure-write-credentials` to **Client arguments** and save.
4. Launch RuneLite once through the Jagex Launcher with the character you want to test.
5. After the game opens, close that normal RuneLite client.

RuneLite writes a temporary local `.runelite/credentials.properties` file so the development client can use the already-authorized Jagex session. This file is equivalent to a login session: never upload, message, or send it to anyone.

Official instructions: [Using Jagex Accounts for RuneLite development](https://github.com/runelite/wiki/blob/master/Using-Jagex-Accounts.md)

## 3. Launch the beta client

Double-click **`Launch-Beta-Client.cmd`**.

The launcher only:

1. Checks that Java 11 is active.
2. Runs the repository's included Gradle wrapper.
3. Starts RuneLite in developer mode with the local companion plugin.

The first launch can take several minutes while Gradle downloads RuneLite development dependencies. Keep the black launcher window open while RuneLite is running; its log is useful if a test fails.

Advanced check: run `Launch-Beta-Client.cmd --check` in Command Prompt to validate the folder and Java version without opening RuneLite.

## 4. Link your RuneScape character

1. Log in to your own account at [Progression Path](https://progressionpath.netlify.app).
2. Select or add the RuneScape character you are testing.
3. Open the website's companion connection controls and generate a one-time link code.
4. In RuneLite, open **Progression Path Companion** in the sidebar.
5. Paste the one-time code and connect.
6. Enable only the sharing features you want to test.
7. Click **Sync Account Now** once and confirm the website shows the correct character.

A one-time sync uploads the current account snapshot. Automatic updates happen only while this development client and the companion are running.

## Suggested beta checks

- The RuneScape name shown in the companion matches the selected website character.
- Skills, quests, diaries, boss KC, clues, and collection-log progress appear under the correct character.
- Personal Bank and Group Storage remain disabled unless deliberately enabled.
- Social presence, world, location, clan roster, and worn gear follow their privacy toggles.
- Automatic snapshots update after the configured cooldown without creating `HTTP 429` errors.
- Logging out clears live presence and performs the final account snapshot when automatic snapshots are enabled.
- If OSRS TCG is installed, owned card names appear after both plugins support the shared PluginMessage API.

Report what you did, what you expected, what happened, and include a screenshot with private codes or tokens hidden.

## Troubleshooting

### The launcher says Java is missing or the wrong version

Install the Windows x64 **JDK 11** build of Eclipse Temurin. Reopen the extracted folder after installation. If several Java versions are installed, configure `JAVA_HOME` and `PATH` to point to Java 11.

### The Jagex account does not log in

Close all RuneLite clients, repeat section 2 with the intended character, and then launch the beta again. Never copy another person's credentials file.

### The companion is not visible

Confirm you launched `Launch-Beta-Client.cmd` from the complete repository rather than normal RuneLite. Check the launcher window for a build or startup error.

### The website shows the wrong character

Log out of the development client, launch the intended character once through the Jagex Launcher, close normal RuneLite, and restart the beta. Generate a new one-time Progression Path link code for that character.

### Account sync is rate limited

Wait for the cooldown shown in the companion. Do not repeatedly press **Sync Account Now**; queued automatic events are coalesced to reduce requests.

## Remove development credentials after testing

When finished:

1. Remove `--insecure-write-credentials` from **RuneLite (configure)**.
2. Delete `.runelite/credentials.properties`, as described by RuneLite's official guide.
3. Use **End sessions** in Jagex Account settings if the session ever needs to be invalidated immediately.

Deleting the repository does not delete website data already uploaded. Use Progression Path's account and privacy controls to manage that data.
