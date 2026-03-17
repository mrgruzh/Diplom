Param(
    [ValidateSet("high", "small")]
    [string]$Preset = "high",
    [string]$ModelUrl = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ModelUrl)) {
    if ($Preset -eq "high") {
        $ModelUrl = "https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip"
    } else {
        $ModelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip"
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$assetsModelDir = Join-Path $repoRoot "app\src\main\assets\model-ru"

$fileName = Split-Path $ModelUrl -Leaf
if ([string]::IsNullOrWhiteSpace($fileName)) {
    throw "Cannot resolve file name from URL: $ModelUrl"
}

$tmpZip = Join-Path $env:TEMP $fileName
$tmpExtract = Join-Path $env:TEMP ([System.IO.Path]::GetFileNameWithoutExtension($fileName))

Write-Host "Repository root: $repoRoot"
Write-Host "Model target:    $assetsModelDir"
Write-Host "Model URL:       $ModelUrl"

if (Test-Path $tmpExtract) {
    Remove-Item $tmpExtract -Recurse -Force
}

New-Item -Path $assetsModelDir -ItemType Directory -Force | Out-Null

Write-Host "Downloading model..."
Invoke-WebRequest -Uri $ModelUrl -OutFile $tmpZip

Write-Host "Extracting archive..."
Expand-Archive -Path $tmpZip -DestinationPath $tmpExtract -Force

$modelRoot = Get-ChildItem -Path $tmpExtract -Directory | Select-Object -First 1
if (-not $modelRoot) {
    throw "Model archive was extracted, but no model folder was found."
}

Write-Host "Copying model files into assets/model-ru..."
Copy-Item -Path (Join-Path $modelRoot.FullName "*") -Destination $assetsModelDir -Recurse -Force

$required = @("am", "conf", "graph", "ivector")
$missing = @()
foreach ($name in $required) {
    if (-not (Test-Path (Join-Path $assetsModelDir $name))) {
        $missing += $name
    }
}

if ($missing.Count -gt 0) {
    throw "Model seems incomplete. Missing folders: $($missing -join ', ')"
}

$uuidFile = Join-Path $assetsModelDir "uuid"
if (-not (Test-Path $uuidFile)) {
    $uuid = "model-ru-$Preset-" + (Get-Date -Format "yyyyMMddHHmmss")
    Set-Content -Path $uuidFile -Value $uuid -Encoding Ascii
}

Write-Host "Done. Vosk model installed successfully."
Write-Host "Now run: Build -> Rebuild Project"
