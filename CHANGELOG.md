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

### Notes
- Vanilla deploy is now blocked at the source rather than being undone after
  the fact.
- Available for Minecraft 1.21.1 (NeoForge 21.1) and 26.1.2 (NeoForge 26.1).
- Licensed under MIT.
