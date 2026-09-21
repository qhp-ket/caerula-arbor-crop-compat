# Changelog

## 1.0.9

- Make the optional Harvest With Ease compatibility transformer fail soft when
  its `getAge(BlockState)` structure changes.
- Require the patched HWE helper to remain static before using its parameter
  slot; incompatible versions now log a warning and keep their original logic.

## 1.0.8

- Always register the optional Harvest With Ease class target. Forge simply
  never visits that transformer when HWE is absent, avoiding the prior
  too-early `Java.type()` availability check.
- Log a clear INFO message after the HWE `HarvestUtils#getAge(BlockState)`
  bridge is applied.

## 1.0.7

- Derive `CropBlock#getMaxAge()` from native `blockstate` possible values.
- Require the transformed `blockstate` IntegerProperty to include zero, while
  allowing its upper bound to evolve with Caerula Arbor.
- Add an optional, bridge-only patch for Harvest With Ease 9.4.0's `getAge`
  helper; it never adds a second `age` property.

## 1.0.6

- Keeps Caerula Arbor's native `blockstate` (0..2) schema; it no longer renames
  it to `age`.
- `getAgeProperty()` now exposes native `blockstate`, with `getMaxAge() == 2`.
- Removes procedure patches and copied Caerula Arbor loot/blockstate resources.
- Migrates the project to a Java 17 ForgeGradle build.

## 1.0.5

Initial public release.

- Adds standard `CropBlock` compatibility to six Caerula Arbor plants.
- Exposes the standard `age` crop property (0..2).
- Adds standard crop and seed tags for generic automation compatibility.
- Preserves the original Caerula Arbor growth, bonemeal, drops, and planting
  behavior.
- Does not patch individual harvesting or technology mods.
- Requires Caerula Arbor 0.12.4 or newer (tested on 0.12.5.x).
