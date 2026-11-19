$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Write-Host 'Connected Android devices:'
if (Get-Command adb -ErrorAction SilentlyContinue) { & adb devices }
& "$PSScriptRoot\gradlew.bat" :app:installDebug
if ($LASTEXITCODE -ne 0) { throw 'Build/install failed. See the Gradle output above.' }
Write-Host 'Install finished. Open Brain MRI Lab 15 on your phone.'
