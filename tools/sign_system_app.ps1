param(
    [Parameter(Mandatory=$true)]
    [string]$ApkPath,
    [string]$OutPath = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$keysDir = Join-Path (Split-Path -Parent $scriptDir) "keys"
$platformPk8 = Join-Path $keysDir "platform.pk8"
$platformPem = Join-Path $keysDir "platform.x509.pem"

if (-not (Test-Path $ApkPath)) {
    Write-Error "Файл APK не найден: $ApkPath"
    exit 1
}

if ($OutPath -eq "") {
    $dir = [System.IO.Path]::GetDirectoryName($ApkPath)
    $name = [System.IO.Path]::GetFileNameWithoutExtension($ApkPath)
    $ext = [System.IO.Path]::GetExtension($ApkPath)
    $OutPath = Join-Path $dir "$($name)_signed$($ext)"
}

Write-Host "Подписание $ApkPath системным ключом AOSP..." -ForegroundColor Cyan

# Поиск apksigner в системе
$apksigner = (Get-Command apksigner -ErrorAction SilentlyContinue).Source
if (-not $apksigner) {
    # Поиск в стандартных путях Android SDK
    $sdkBuildTools = "$env:LOCALAPPDATA\Android\Sdk\build-tools"
    if (Test-Path $sdkBuildTools) {
        $latest = Get-ChildItem $sdkBuildTools | Sort-Object Name -Descending | Select-Object -First 1
        if ($latest) {
            $candidate = Join-Path $latest.FullName "apksigner.bat"
            if (Test-Path $candidate) { $apksigner = $candidate }
        }
    }
}

if ($apksigner) {
    & $apksigner sign --key $platformPk8 --cert $platformPem --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out $OutPath $ApkPath
    Write-Host "APK успешно подписан: $OutPath" -ForegroundColor Green
} else {
    Write-Warning "apksigner не найден в PATH или Android SDK. Команда для ручного вызова:"
    Write-Host "apksigner sign --key `"$platformPk8`" --cert `"$platformPem`" --out `"$OutPath`" `"$ApkPath`"" -ForegroundColor Yellow
}
