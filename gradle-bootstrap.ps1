param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GradleArguments)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$version = '8.9'
$dist = Join-Path $PSScriptRoot ".tools\gradle-$version\bin\gradle.bat"
$zip = Join-Path $PSScriptRoot ".tools\gradle-$version-bin.zip"
$expectedSha = 'd725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab'

if (Test-Path 'C:\Java\jdk-17.0.20+1\bin\java.exe') {
    $env:JAVA_HOME = 'C:\Java\jdk-17.0.20+1'
}
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
}
# Java prints version information to stderr; avoid PowerShell 5.1 NativeCommandError.
$priorPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try { $javaVersion = (& java -version 2>&1 | Out-String) }
finally { $ErrorActionPreference = $priorPreference }
if ($LASTEXITCODE -ne 0) { throw 'Java not found. Install JDK 17 and set JAVA_HOME.' }
Write-Host $javaVersion.Trim()

if (-not (Test-Path 'local.properties')) {
    $sdk = $env:ANDROID_SDK_ROOT
    if (-not $sdk) { $sdk = $env:ANDROID_HOME }
    if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    if (Test-Path $sdk) {
        $sdkForProperties = $sdk.Replace('\','/')
        "sdk.dir=$sdkForProperties" | Set-Content -Path 'local.properties' -Encoding ascii
        Write-Host "Android SDK: $sdk"
    } else {
        Write-Warning 'Android SDK not found automatically. Set ANDROID_SDK_ROOT or create local.properties.'
    }
}
if (-not (Test-Path $dist)) {
    New-Item -ItemType Directory -Force (Split-Path $zip) | Out-Null
    if (-not (Test-Path $zip)) {
        Write-Host 'Downloading official Gradle 8.9 (one time)...'
        Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $zip
    }
    $actual = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expectedSha) {
        Remove-Item $zip -Force
        throw 'Gradle checksum mismatch. Download discarded.'
    }
    Write-Host 'Gradle SHA-256 verified. Extracting...'
    Expand-Archive -Path $zip -DestinationPath (Split-Path $zip) -Force
}
& $dist @GradleArguments
exit $LASTEXITCODE
