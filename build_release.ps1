#!/usr/bin/env pwsh
# Reproducible packaging script for Caerula Arbor Crop Compatibility.
#
# It packs the verified compiled classes + resources into the distributable JAR,
# writing the correct Forge manifest (FMLModType: MOD). The JAR produced here is
# byte-for-byte equivalent in payload to the individually tested 1.0.5 build
# (only the auto-generated manifest lines differ, which do not affect behavior).
#
# Layout expected (relative to this script):
#   classes/                 compiled .class files (io/github/caerulacropcompat/*)
#   src/main/resources/      META-INF/, coremods/, assets/, data/, pack.mcmeta
#   packaging/manifest.txt   the manifest template used for the JAR
#
# Usage:  pwsh ./build_release.ps1

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $root 'classes'
$res     = Join-Path $root 'src\main\resources'
$dist    = Join-Path $root 'dist'
$manifest = Join-Path $root 'packaging\manifest.txt'
$version  = '1.0.5'
$jarName  = "caerula_arbor_crop_compat-$version-forge-1.20.1.jar"
$outJar   = Join-Path $dist $jarName

if (-not (Test-Path -LiteralPath $classes)) { throw "Missing classes dir: $classes" }
if (-not (Test-Path -LiteralPath $res))     { throw "Missing resources dir: $res" }
if (-not (Test-Path -LiteralPath $manifest)){ throw "Missing manifest: $manifest" }

New-Item -ItemType Directory -Path $dist -Force | Out-Null
if (Test-Path -LiteralPath $outJar) { Remove-Item -LiteralPath $outJar -Force }

# Stage classes + resources together, then jar the combined tree so paths are clean.
$stage = Join-Path $env:TEMP ("cacc_pack_" + [System.Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage -Force | Out-Null
try {
    # NOTE: use -Path (not -LiteralPath) so the trailing wildcard is expanded.
    Copy-Item -Path (Join-Path $classes '*') -Destination $stage -Recurse -Force
    Copy-Item -Path (Join-Path $res '*')     -Destination $stage -Recurse -Force

    & jar --create --file $outJar --manifest $manifest -C $stage .
    if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }

    Write-Host "Built: $outJar"
    $len = (Get-Item -LiteralPath $outJar).Length
    Write-Host "Size:  $len bytes"
}
finally {
    Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}
