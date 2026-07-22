# HCIM Progression Companion — Social Presence Phase II

This build adds privacy-aware Social Hub presence syncing to the existing account and group-location companion.

## Synced presence

Approximately every six seconds while logged in, the plugin sends:

- player name
- world
- broad region
- current activity
- combat level
- worn equipment item IDs and names
- optional exact coordinates

The website applies the account's Social Hub privacy settings before storing fields visible to approved friends.

## Plugin settings

### Share Social Hub presence

Enabled by default. Turning this off stops Social Hub presence heartbeats while leaving manual account syncing available.

### Share exact group location

Disabled by default. This continues to power the HCIM live group map. It also permits exact Social Hub coordinates only when exact sharing is separately enabled on the website. Wilderness safety on the website can still suppress the exact pin.

## Existing behavior preserved

- one-time website linking
- account snapshot sync
- quest and skill task updates
- clue and Collection Log capture
- HCIM group live-location sync

## Build verification

A full Gradle dependency download requires internet access. The changed Java sources were also checked with an offline Java 11 API-shape compile to catch syntax and method-call errors.
