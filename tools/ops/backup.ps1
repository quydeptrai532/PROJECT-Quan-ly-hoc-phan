# ==============================================================================
# Script sao lưu cơ sở dữ liệu MySQL và tệp đính kèm an toàn trên Windows (QLSV-94 / TASK-OP-07)
# Đọc cấu hình từ biến môi trường, không hardcode mật khẩu hay secret vào mã nguồn.
# ==============================================================================
param(
    [string]$BackupDir = "./backups",
    [string]$DbName = $env:MYSQL_DATABASE,
    [string]$DbHost = $env:MYSQL_HOST,
    [string]$DbPort = $env:MYSQL_PORT,
    [string]$DbUser = $env:MYSQL_USER,
    [string]$ContainerName = $env:CONTAINER_NAME
)

if (-not $DbName) { $DbName = "quan_ly_dao_tao" }
if (-not $DbHost) { $DbHost = "127.0.0.1" }
if (-not $DbPort) { $DbPort = "3307" }
if (-not $DbUser) { $DbUser = "root" }

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

$dumpFile = Join-Path $BackupDir "${DbName}_${timestamp}.sql"
$shaFile = "${dumpFile}.sha256"

Write-Host "=== Bắt đầu sao lưu hệ thống Quản lý đào tạo ($timestamp) ===" -ForegroundColor Cyan
Write-Host "Database: $DbName | User: $DbUser | Host: $DbHost:$DbPort"

if ($ContainerName) {
    Write-Host "Sao lưu qua Docker container [$ContainerName]..."
    $pass = if ($env:MYSQL_PWD) { $env:MYSQL_PWD } else { "root" }
    docker exec $ContainerName mysqldump -u$DbUser -p$pass `
        --single-transaction --quick --routines --triggers --set-gtid-purged=OFF `
        --default-character-set=utf8mb4 $DbName | Out-File -FilePath $dumpFile -Encoding utf8
} else {
    Write-Host "Sao lưu qua lệnh mysqldump cục bộ..."
    & mysqldump -h $DbHost -P $DbPort -u $DbUser `
        --single-transaction --quick --routines --triggers --set-gtid-purged=OFF `
        --default-character-set=utf8mb4 $DbName | Out-File -FilePath $dumpFile -Encoding utf8
}

if (-not (Test-Path $dumpFile) -or (Get-Item $dumpFile).Length -eq 0) {
    Write-Error "Lỗi: Tệp sao lưu không tồn tại hoặc rỗng!"
    exit 1
}

$hash = (Get-FileHash -Algorithm SHA256 -Path $dumpFile).Hash
"$hash  $dumpFile" | Out-File -FilePath $shaFile -Encoding utf8

Write-Host "Sao lưu cơ sở dữ liệu thành công: $dumpFile" -ForegroundColor Green
Write-Host "Checksum SHA256: $hash" -ForegroundColor Green
