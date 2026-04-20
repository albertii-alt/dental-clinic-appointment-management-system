param(
    [string]$AppVersion = "1.0.0",
    [ValidateSet("exe", "msi")]
    [string]$InstallerType = "exe",
    [string]$Vendor = "Dental Clinic Team"
)

$ErrorActionPreference = "Stop"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
Set-Location $repoRoot

Write-Host "[1/5] Checking tools..."
Require-Command "jpackage"

Write-Host "[2/5] Verifying desktop jar exists..."

$distJar = Join-Path $repoRoot "dist\DentalClinicAppointment_ManagementSystem.jar"
$distLib = Join-Path $repoRoot "dist\lib"

if (-not (Test-Path $distJar)) {
    throw "Build output not found: $distJar"
}
if (-not (Test-Path $distLib)) {
    throw "Build library folder not found: $distLib"
}

$workRoot = Join-Path $repoRoot "build\windows-installer"
$inputDir = Join-Path $workRoot "input"
$outputDir = Join-Path $workRoot "output"

Write-Host "[3/5] Preparing installer input files..."
if (Test-Path $workRoot) {
    Remove-Item -Recurse -Force $workRoot
}
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

Copy-Item $distJar $inputDir
Copy-Item $distLib (Join-Path $inputDir "lib") -Recurse

$backendJar = Join-Path $repoRoot "backend-spring\target\backend-spring-0.1.0.jar"
if (-not (Test-Path $backendJar)) {
    throw "Spring backend JAR not found: $backendJar. Run: cd backend-spring && mvn clean package -DskipTests"
}
$backendInputDir = Join-Path $inputDir "backend"
New-Item -ItemType Directory -Force -Path $backendInputDir | Out-Null
Copy-Item $backendJar $backendInputDir

Write-Host "[4/5] Creating Windows installer ($InstallerType)..."
$jpackageArgs = @(
    "--type", $InstallerType,
    "--name", "DentalClinicSystem",
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--dest", $outputDir,
    "--input", $inputDir,
    "--main-jar", "DentalClinicAppointment_ManagementSystem.jar",
    "--main-class", "com.dentalclinic.main.Main",
    "--win-shortcut",
    "--win-menu",
    "--win-dir-chooser",
    "--java-options", "-Dfile.encoding=UTF-8"
)

& jpackage @jpackageArgs

Write-Host "[5/5] Done. Installer created in: $outputDir"
Get-ChildItem $outputDir | Select-Object Name, Length, LastWriteTime
