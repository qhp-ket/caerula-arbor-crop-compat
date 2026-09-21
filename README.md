# Caerula Arbor Crop Compatibility

A lightweight compatibility add-on for **Caerula Arbor** on Minecraft 1.20.1 (Forge).

It makes six Caerula Arbor plants expose the standard Minecraft crop interfaces
(the vanilla `CropBlock` contract) so that generic crop
harvesters, automation mods, and other standard farming integrations can
recognize them. It primarily relies on standard crop interfaces and tags, with
a small optional compatibility patch for Harvest With Ease 9.4.0, whose crop
age lookup assumes a property literally named `age`. Other mods that already
support vanilla crops should pick these plants up through the normal crop
interfaces and tags.

## What it changes

The following Caerula Arbor plants are exposed as standard crops. Their original
`blockstate` property remains the sole maturity property (0..N); the bridge's
`getAgeProperty()` returns that exact property and `getMaxAge()` derives its
maximum from the property's possible values.

- Viviparous Lily
- Nethersea Potato
- Nethersea Wheat
- Ocean Peduncle
- Ocean Cell
- Fake Egg

The add-on preserves Caerula Arbor's growth, bonemeal, drops, planting items,
shapes, collision, lighting, pathfinding, and trampling behavior. It does not
copy or override Caerula Arbor loot tables or blockstate/model JSON.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- Caerula Arbor 0.12.4 or newer

Install on both client and server (it changes block state and server-side
behavior).

## Compatibility

Tested with Caerula Arbor 0.12.6. Versions 0.12.4 and newer are allowed to
load, but compatibility with future Caerula Arbor updates cannot be guaranteed
until tested.

This add-on rewrites six internal Caerula Arbor crop classes at startup via a
Forge coremod transformer. It checks each original superclass and constructor
call before replacing `Block` with the compatibility `CropBlock` bridge. If a
future Caerula Arbor update changes that structure, loading fails loudly rather
than silently producing incompatible behavior.

## Building

Use Java 17 and run `gradlew.bat build` on Windows (or `./gradlew build` on
Unix). Put a compatible Caerula Arbor JAR in `libs/` before building. The JAR is
a compile/runtime development dependency only and is not included in the output.

## Credits

Caerula Arbor is created by Apocalypse and contributors.

This project is an unofficial compatibility add-on and is not affiliated with or
endorsed by the Caerula Arbor developers.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).

Caerula Arbor is distributed under GPL-3.0. This project is released under the
same license.
