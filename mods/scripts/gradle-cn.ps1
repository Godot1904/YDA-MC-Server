param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'
$GradleVersion = '8.10.2'
$MirrorBase = $env:GRADLE_DIST_MIRROR
if ([string]::IsNullOrWhiteSpace($MirrorBase)) {
    $MirrorBase = 'https://mirrors.cloud.tencent.com/gradle'
}

$CacheDir = Join-Path $env:TEMP 'yudream-gradle'
$ZipPath = Join-Path $CacheDir "gradle-$GradleVersion-bin.zip"
$DistDir = Join-Path $CacheDir "gradle-$GradleVersion"
$GradleBat = Join-Path $DistDir 'bin\gradle.bat'

New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null

if (!(Test-Path $GradleBat)) {
    if (Test-Path $ZipPath) {
        Remove-Item -LiteralPath $ZipPath -Force
    }
    $DownloadUrl = "$MirrorBase/gradle-$GradleVersion-bin.zip"
    Write-Host "Downloading Gradle $GradleVersion from $DownloadUrl"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -UseBasicParsing $DownloadUrl -OutFile $ZipPath
    Expand-Archive -Force $ZipPath $CacheDir
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $env:JAVA_HOME = 'C:\Users\SiberianHusky\.jdks\ms-21.0.10'
}

& $GradleBat @GradleArgs
exit $LASTEXITCODE
