# Changelog

All notable changes to ElytraTweaks are documented here.

## [1.1.0]

### Added
- **In-game settings screen** (Mods list → Elytra Tweaks → Config). Settings are
  saved in `config/elytra_tweaks-client.toml`.
- **Deploy Key Mode:** `Toggle` (press to open, press again to close; default)
  or `Hold` (the elytra stays open only while the key is held).
- **Jump Key Deploy:** choose what the vanilla jump key may do while falling
  (the mod's own deploy key always works, whatever this is set to):
  - `Blocked` (default): never opens the elytra, as in 1.0.5.
  - `Only While Holding Firework`: opens it only while you hold a firework
    rocket in either hand, so normal jumps and jump-crits never deploy it.
  - `Vanilla`: opens it like vanilla.
- **Safety Net** toggle: turn off the reactive wing-folding if it causes the
  one-frame flicker in your modpack (those mods' deploys are then no longer
  stopped).
- **Wind Sound Volume:** 0–100 % slider for the gliding wind sound.
- **Firework Takeoff** key (unbound by default, set it in Controls): from the
  ground it jumps, opens the elytra and fires a rocket in one press. It uses a
  rocket from either hand, or from the hotbar (switching back to your slot
  right after). Because it uses an item automatically, check your server's rules.
- **Flight HUD:** speed, altitude, pitch, firework rocket count and elytra
  durability next to the crosshair while gliding (hidden with F1, can be turned
  off).
- **Auto Swap Chestplate** (off by default): when you use the deploy or takeoff
  key without wearing an elytra, it puts one on from your inventory, and puts
  your chestplate back when you land. It never touches a chestplate with Curse of
  Binding. **Warning:** this moves items in your inventory for you, which some
  servers consider cheating. Check the server's rules before turning it on.
- **Low Durability Warning:** a sound and a red message once per flight when the
  elytra is at or below a configurable percentage (default 10 %).

### Changed
- The default deploy key is now Left Alt on every version (it was G on 26.1.2).
  Existing installs keep whatever key is already saved in their options.

### Fixed
- When the safety net folds a deploy that another mod started on the server, it
  now also stops the gliding on the server. Before, the server kept you gliding
  until you landed, and the elytra kept losing durability even with its wings
  folded on your screen. Deploys that only happened on your side are folded
  without telling the server anything, so it never starts gliding by mistake.
- The deploy key name and its controls category now show up translated on 26.1.2.
- The client-only mixins (`LocalPlayer`, elytra sound) are now registered in the
  client mixin list, so they are no longer applied on a dedicated server, where
  those classes don't exist.

### Known limitations
- Elytras worn in an accessory slot (e.g. with Elytra Slot): the deploy key and
  the Firework Takeoff let the game decide whether you can glide, like vanilla
  does, but the HUD durability line, the Low Durability Warning and Auto Swap
  Chestplate only look at the chest slot (and, on 26.1.2, the other equipment
  slots), so they don't see such an elytra.

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
