param([switch]$Install)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Enter-Dev.ps1"
$projectRoot = Split-Path $PSScriptRoot -Parent
& gradle.bat -p $projectRoot --no-daemon --console=plain assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Leafolio debug APK build failed.' }

$apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if ($Install) {
    & adb.exe install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw 'APK built successfully, but Android rejected installation. Check the phone USB install permission.'
    }
}
Get-Item $apk | Select-Object FullName, Length

