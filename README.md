# Caerula Arbor Crop Compatibility

A lightweight compatibility add-on for **Caerula Arbor** on Minecraft 1.20.1 (Forge).

It makes six Caerula Arbor plants expose the standard Minecraft crop interfaces
(the vanilla `CropBlock` contract and an `age` property) so that generic crop
harvesters, automation mods, and other standard farming integrations can
recognize them. It does **not** add per-mod patches; mods that already support
vanilla crops should pick these plants up through the normal crop interfaces and
tags.

## What it changes

The following Caerula Arbor plants are exposed as standard crops with an `age`
property (0..2):

- Viviparous Lily
- Nethersea Potato
- Nethersea Wheat
- Ocean Peduncle
- Ocean Cell
- Fake Egg

The add-on preserves the original Caerula Arbor behavior, including growth,
bonemeal, drops, planting items, shapes, collision, lighting, pathfinding, and
trampling.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- Caerula Arbor 0.12.4 or newer

Install on both client and server (it changes block state and server-side
behavior).

## Compatibility

Tested with Caerula Arbor 0.12.5.x. Versions 0.12.4 and newer are allowed to
load, but compatibility with future Caerula Arbor updates cannot be guaranteed
until tested.

This add-on rewrites internal Caerula Arbor crop classes at startup via a Forge
coremod transformer. If a future Caerula Arbor update changes its crop
implementation, the transformer is designed to fail loudly (with a clear error)
rather than silently produce wrong crop behavior. If a new version fails to
load, please report the affected version.

## Credits

Caerula Arbor is created by Apocalypse and contributors.

This project is an unofficial compatibility add-on and is not affiliated with or
endorsed by the Caerula Arbor developers.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).

Caerula Arbor is distributed under GPL-3.0. Because this add-on redistributes
and modifies Caerula Arbor resources (blockstates and loot tables) and derives
from its class structure, it is released under the same license.
