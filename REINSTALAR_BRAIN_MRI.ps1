# Run from the Android project folder after extracting the ZIP there.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$jdk = 'C:\Java\jdk-17.0.20.1+1'
if (-not (Test-Path (Join-Path $jdk 'bin\java.exe'))) { throw "JDK 17 missing: $jdk" }
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:Path = "$jdk\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
Write-Host '--- Connected Android devices ---'
& (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe') devices
if ($LASTEXITCODE -ne 0) { throw 'ADB failed' }
Write-Host '--- Rebuilding and installing Brain MRI ---'
& .\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { throw 'Build or installation failed' }
Write-Host 'Brain MRI installed. Open it from your phone.'
