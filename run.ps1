# run.ps1 — KaspiTracker local development runner
# Usage: .\run.ps1
#
# Loads .env, sets environment variables, and starts the Spring Boot app.
# Requires: Docker Desktop running + docker compose up -d

param(
    [switch]$SkipDocker  # Pass -SkipDocker to skip docker compose up
)

$ErrorActionPreference = "Stop"

# ── Load .env ────────────────────────────────────────────────
if (-not (Test-Path ".env")) {
    Write-Error ".env not found. Copy .env.example to .env and fill in your credentials."
    exit 1
}

Get-Content ".env" | Where-Object { $_ -match "^\s*[^#]" -and $_ -match "=" } | ForEach-Object {
    $line = $_.Trim()
    $eq   = $line.IndexOf("=")
    $key  = $line.Substring(0, $eq).Trim()
    $val  = $line.Substring($eq + 1).Trim()
    [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
    Write-Host "  Loaded: $key"
}

Write-Host ""

# ── Optionally start Docker Compose ──────────────────────────
if (-not $SkipDocker) {
    Write-Host "Starting PostgreSQL (docker compose up -d)..."
    docker compose up -d
    Write-Host ""
}

# ── Run Spring Boot ───────────────────────────────────────────
Write-Host "Starting KaspiTracker (mvn spring-boot:run)..."


$dbHost = [System.Environment]::GetEnvironmentVariable("DB_HOST", "Process")
$dbPort = [System.Environment]::GetEnvironmentVariable("DB_PORT", "Process")
$dbName = [System.Environment]::GetEnvironmentVariable("DB_NAME", "Process")
$dbUser = [System.Environment]::GetEnvironmentVariable("DB_USER", "Process")
$dbPass = [System.Environment]::GetEnvironmentVariable("DB_PASSWORD", "Process")
$srvPort = [System.Environment]::GetEnvironmentVariable("SERVER_PORT", "Process")

# Pass as JVM system properties so Spring's RestartClassLoader sees them
$jvmArgs = "-DDB_HOST=$dbHost -DDB_PORT=$dbPort -DDB_NAME=$dbName -DDB_USER=$dbUser -DDB_PASSWORD=$dbPass -DSERVER_PORT=$srvPort"

mvn spring-boot:run --no-transfer-progress `
    "-Dspring-boot.run.jvmArguments=$jvmArgs"

