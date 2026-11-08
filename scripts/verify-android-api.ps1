param(
    [int]$Port = 18080,
    [switch]$UseAdbReverse,
    [string]$Serial = "emulator-5554",
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$repo = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $repo "backend"
$python = Join-Path $backend ".venv/Scripts/python.exe"
if (-not (Test-Path $python)) {
    throw "Instala primero el backend en su entorno virtual: backend/README.md"
}
$tempDirectory = Join-Path ([IO.Path]::GetTempPath()) ("ricetta-android-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempDirectory | Out-Null
$previousSecret = $env:JWT_SECRET_KEY
$previousDatabase = $env:DATABASE_URL
$previousExpiration = $env:ACCESS_TOKEN_EXPIRE_MINUTES
$previousSerial = $env:ANDROID_SERIAL
$server = $null
$reverseAdded = $false
try {
    $env:ANDROID_SERIAL = $Serial
    # Impedir conectar accidentalmente con otro servidor en el mismo puerto.
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Any, $Port)
    $listener.Start()
    $listener.Stop()
    $keyBytes = New-Object byte[] 48
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($keyBytes) } finally { $random.Dispose() }
    $env:JWT_SECRET_KEY = [Convert]::ToBase64String($keyBytes)
    $databasePath = (Join-Path $tempDirectory "integration.sqlite3").Replace("\", "/")
    $env:DATABASE_URL = "sqlite:///$databasePath"
    $env:ACCESS_TOKEN_EXPIRE_MINUTES = "60"
    $bindHost = if ($UseAdbReverse) { "127.0.0.1" } else { "0.0.0.0" }
    $baseUrl = "http://10.0.2.2:$Port/"
    if ($UseAdbReverse) {
        $mappings = & $AdbPath -s $Serial reverse --list
        if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar adb reverse" }
        if ($mappings -match "tcp:$Port\s") { throw "Puerto ya mapeado por adb; elige otro Port" }
        & $AdbPath -s $Serial reverse "tcp:$Port" "tcp:$Port"
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear adb reverse" }
        $reverseAdded = $true
        $baseUrl = "http://127.0.0.1:$Port/"
    }
    $server = Start-Process -FilePath $python -WorkingDirectory $backend -PassThru -WindowStyle Hidden -ArgumentList @(
        "-m", "uvicorn", "app.main:create_app", "--factory", "--host", $bindHost,
        "--port", "$Port", "--no-access-log"
    ) -RedirectStandardOutput (Join-Path $tempDirectory "server-out.log") -RedirectStandardError (Join-Path $tempDirectory "server-error.log")

    $ready = $false
    for ($attempt = 0; $attempt -lt 100; $attempt++) {
        if ($server.HasExited) { throw "Uvicorn finalizó antes de responder." }
        try {
            $schema = Invoke-RestMethod "http://127.0.0.1:$Port/openapi.json" -TimeoutSec 2
            if ($schema.info.title -eq "Ricetta API") { $ready = $true; break }
        } catch { Start-Sleep -Milliseconds 200 }
    }
    if (-not $ready) { throw "FastAPI no respondió dentro del plazo." }
    $docs = Invoke-WebRequest "http://127.0.0.1:$Port/docs" -UseBasicParsing -TimeoutSec 3
    if ($docs.StatusCode -ne 200) { throw "/docs no disponible" }
    Write-Host "FastAPI real y OpenAPI disponibles; base SQLite temporal aislada."
    & (Join-Path $repo "gradlew.bat") "-p" $repo "connectedDebugAndroidTest" "-Pandroid.testInstrumentationRunnerArguments.fastApiBaseUrl=$baseUrl" "--console=plain"
    if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas instrumentadas." }
    Write-Host "Integración Android/FastAPI aprobada."
} finally {
    if ($null -ne $server) {
        if (-not $server.HasExited) {
            Stop-Process -Id $server.Id
            $server.WaitForExit()
        }
        $server.Dispose()
    }
    if ($reverseAdded) { & $AdbPath -s $Serial reverse --remove "tcp:$Port" | Out-Null }
    $env:JWT_SECRET_KEY = $previousSecret
    $env:DATABASE_URL = $previousDatabase
    $env:ACCESS_TOKEN_EXPIRE_MINUTES = $previousExpiration
    $env:ANDROID_SERIAL = $previousSerial
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force
}
