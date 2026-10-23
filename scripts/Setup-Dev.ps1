param(
    [string]$ToolsRoot,
    [string]$SourceJdk
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
if (-not $SourceJdk -and $env:JAVA_HOME) {
    $SourceJdk = $env:JAVA_HOME
}
if (-not $SourceJdk) {
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javac) {
        $SourceJdk = Split-Path (Split-Path $javac.Source -Parent) -Parent
    }
}
. "$PSScriptRoot\Enter-Dev.ps1" -ToolsRoot $ToolsRoot
$downloads = Join-Path $ToolsRoot 'downloads'
New-Item -ItemType Directory -Path $downloads -Force | Out-Null

function Get-Archive([string]$Url, [string]$Destination) {
    if (!(Test-Path $Destination)) {
        & curl.exe --fail --location --connect-timeout 15 --max-time 180 --retry 2 --silent --show-error --output "$Destination.partial" $Url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $Url" }
        Move-Item -LiteralPath "$Destination.partial" -Destination $Destination -Force
    }
}

if (!(Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    if (!$SourceJdk -or !(Test-Path "$SourceJdk\bin\javac.exe")) {
        throw 'JDK 17 was not found. Install JDK 17 or pass -SourceJdk with its directory.'
    }
    Copy-Item -LiteralPath $SourceJdk -Destination $env:JAVA_HOME -Recurse
}

$sdkZip = Join-Path $downloads 'commandlinetools-win-13114758_latest.zip'
if (!(Test-Path "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Get-Archive 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' $sdkZip
    $sdkStage = Join-Path $ToolsRoot 'sdk-stage'
    Expand-Archive -LiteralPath $sdkZip -DestinationPath $sdkStage -Force
    New-Item -ItemType Directory -Path "$env:ANDROID_HOME\cmdline-tools" -Force | Out-Null
    Move-Item -LiteralPath "$sdkStage\cmdline-tools" -Destination "$env:ANDROID_HOME\cmdline-tools\latest"
}

$gradleZip = Join-Path $downloads 'gradle-8.13-bin.zip'
if (!(Test-Path "$ToolsRoot\gradle-8.13\bin\gradle.bat")) {
    try {
        Get-Archive 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' $gradleZip
    } catch {
        Get-Archive 'https://mirrors.huaweicloud.com/gradle/gradle-8.13-bin.zip' $gradleZip
    }
    $checksum = (& curl.exe --fail --location --retry 3 --silent --show-error 'https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256')
    if ($LASTEXITCODE -ne 0) { throw 'Failed to retrieve Gradle checksum.' }
    if ((Get-FileHash -LiteralPath $gradleZip -Algorithm SHA256).Hash -ne $checksum.Trim()) {
        throw 'Gradle archive checksum mismatch.'
    }
    Expand-Archive -LiteralPath $gradleZip -DestinationPath $ToolsRoot -Force
}

# SDK licenses are accepted for the requested Android development environment.
1..100 | ForEach-Object { 'y' } | & sdkmanager.bat "--sdk_root=$env:ANDROID_HOME" --licenses
if ($LASTEXITCODE -ne 0) { throw 'Android SDK license setup failed.' }
& sdkmanager.bat "--sdk_root=$env:ANDROID_HOME" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK package installation failed.' }
& java.exe -version
& gradle.bat --version
if ($LASTEXITCODE -ne 0) { throw 'Gradle verification failed.' }

