$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Enter-Dev.ps1"
$projectRoot = Split-Path $PSScriptRoot -Parent
& gradle.bat -p "$projectRoot\tools\android-smoke" --no-daemon --console=plain assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Android environment smoke build failed.' }
& adb.exe version
if ($LASTEXITCODE -ne 0) { throw 'ADB verification failed.' }
Get-Item "$projectRoot\tools\android-smoke\app\build\outputs\apk\debug\app-debug.apk" |
    Select-Object FullName, Length

