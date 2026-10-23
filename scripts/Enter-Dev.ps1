param(
    [string]$ToolsRoot
)

$ErrorActionPreference = 'Stop'
if (-not $ToolsRoot) {
    $projectRoot = Split-Path $PSScriptRoot -Parent
    $localToolsConfig = Join-Path $projectRoot '.local\tools-root.txt'
    if (Test-Path $localToolsConfig) {
        $ToolsRoot = (Get-Content -LiteralPath $localToolsConfig -Raw).Trim()
    }
    if (-not $ToolsRoot) {
        $ToolsRoot = Join-Path $projectRoot '.local\tools'
    }
}
if ([System.IO.Path]::GetPathRoot([System.IO.Path]::GetFullPath($ToolsRoot)) -eq 'C:\') {
    throw 'Leafolio tools must be stored outside the system drive. Pass -ToolsRoot to choose another location.'
}
$env:LEAFOLIO_TOOLS = $ToolsRoot
$env:JAVA_HOME = Join-Path $ToolsRoot 'jdk-17'
$env:ANDROID_HOME = Join-Path $ToolsRoot 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = Join-Path $ToolsRoot 'gradle-home'
$env:ANDROID_USER_HOME = Join-Path $ToolsRoot 'android-user'
$env:ANDROID_EMULATOR_HOME = Join-Path $ToolsRoot 'android-user'
$env:ANDROID_AVD_HOME = Join-Path $ToolsRoot 'avd'
$env:TEMP = Join-Path $ToolsRoot 'temp'
$env:TMP = $env:TEMP
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=$env:TEMP -Duser.home=$ToolsRoot\user-home"
$env:GRADLE_OPTS = "-Dorg.gradle.java.installations.auto-download=false"
foreach ($directory in @($env:GRADLE_USER_HOME, $env:ANDROID_USER_HOME, $env:ANDROID_AVD_HOME, $env:TEMP, "$ToolsRoot\user-home")) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}
$toolPaths = @(
    "$env:JAVA_HOME\bin",
    "$env:ANDROID_HOME\cmdline-tools\latest\bin",
    "$env:ANDROID_HOME\platform-tools",
    "$env:ANDROID_HOME\build-tools\35.0.0",
    "$ToolsRoot\gradle-8.13\bin"
)
$env:PATH = ($toolPaths -join ';') + ';' + $env:PATH

