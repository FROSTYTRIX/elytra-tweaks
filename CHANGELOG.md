# Changelog

All notable changes to ElytraTweaks are documented here.

## [1.0.5]

### Changed
- The elytra no longer deploys from the vanilla jump/spacebar key. It now
  deploys **only** from the mod's dedicated deploy key (default: Left Alt).
  This prevents accidental deploys and fixes conflicts with movement mods that
  use the jump key, such as ParCool.

### Fixed
- Stopping flight mid-air now takes a **single press** of the deploy key.
  Previously it could require two presses because the client and server
  fall-flying state drifted out of phase — deploy now syncs both sides via the
  fall-flying packet, so a single stop press always works.

### How the spacebar block works
- Two layers, for modpack robustness:
  1. **Proactive:** the client-side `tryToStartFallFlying` is cancelled for any
     attempt that the mod's own deploy key did not initiate.
  2. **Reactive safety net:** if the player ends up gliding without having asked
     to (via an unusual deploy path from another mod), the wings are folded
     client-side (no stop packet, so it never traps you in the crawling hitbox).

### Known limitation
- In very large modpacks, a mod may open the elytra through a path the proactive
  layer can't see (e.g. a server-side deploy synced back to the client). The
  reactive net still stops it, but because it acts a tick later you may see a
  brief one-frame "flicker" of the wings. Flight is never actually sustained.

### Other
- License changed to MIT.
