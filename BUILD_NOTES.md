# Build notes (for the maintainer, not for distribution)

This `release/` folder is the clean, publishable snapshot. It was assembled from
the verified `stage/` payload of the tested `1.0.5` JAR.

## Provenance check performed

- Every resource entry (`assets/`, `data/`, `coremods/`, `pack.mcmeta`,
  `META-INF/coremods.json`) and both compiled classes
  (`CaerulaCropBlock.class`, `CaerulaCropCompat.class`) are SHA-256 identical to
  the entries inside the tested JAR
  `caerula_arbor_crop_compat-1.0.5-forge-1.20.1.jar`.
- The only difference between `stage/` and the tested JAR was
  `META-INF/MANIFEST.MF`: the tested JAR carried the auto-generated
  `Created-By` line and lacked `FMLModType: MOD`. Forge treats a missing
  `FMLModType` as `MOD`, so runtime behavior is identical. `build_release.ps1`
  now writes `FMLModType: MOD` explicitly (the correct, intended manifest).

## Metadata corrections applied for publication

- `license` changed from `All Rights Reserved` to `GPL-3.0`. The add-on
  redistributes/modifies Caerula Arbor (GPL-3.0) resources and derives from its
  class structure, so GPL-3.0 is the only clean choice.
- `authors` changed from the placeholder `Local compatibility addon` to
  `qhp-ket`.
- `description` expanded to describe the standard-crop-contract goal rather than
  "converts ... CropBlocks".
- `versionRange="[0.12.4,)"` deliberately kept (loose metadata, conservative
  compatibility claim in README).

## Rebuilding

Run `pwsh ./build_release.ps1`. It repacks `classes/` + `src/main/resources/`
into `dist/caerula_arbor_crop_compat-1.0.5-forge-1.20.1.jar` using
`packaging/manifest.txt`.

## What was intentionally left out

The abandoned Mixin implementation (`CropAgePropertyMixin.java`,
`GrowthProcedureMixin.java`) is NOT part of the shipped JAR (the final mechanism
is the Forge coremod transformer in `coremods/crop_transformer.js`). It is kept
only under `reference/mixin-legacy/` for history and is excluded from the build.
