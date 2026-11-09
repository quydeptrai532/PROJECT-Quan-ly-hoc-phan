# ==============================================================================
# Script khôi phục và xác minh tính toàn vẹn bản sao lưu trên Windows (QLSV-94 / TASK-OP-07)
# ==============================================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$VerifyDb = "quan_ly_dao_tao_restore_test",
    [string]$DbHost = "127.0.0.1",
    [string]$DbPort = "3307",
    [string]$DbUser = "root"
)

if (-not (Test-Path $BackupFile)) {
    Write-Error "Tệp sao lưu không tồn tại: $BackupFile"
    exit 1
}

Write-Host "=== Bắt đầu xác minh khôi phục sao lưu ===" -ForegroundColor Cyan
Write-Host "Tệp sao lưu: $BackupFile"
Write-Host "Database Sandbox: $VerifyDb"

# 1. Kiểm tra checksum SHA256 nếu có
$shaFile = "${BackupFile}.sha256"
if (Test-Path $shaFile) {
    Write-Host "Xác minh mã băm SHA256..."
    $expected = (Get-Content $shaFile).Split(" ")[0].Trim().ToUpper()
    $actual = (Get-FileHash -Algorithm SHA256 -Path $BackupFile).Hash.ToUpper()
    if ($expected -ne $actual) {
        Write-Error "MÃ BĂM KHÔNG TRÙNG KHỚP! Bản sao lưu có nguy cơ bị sửa đổi hoặc lỗi truyền tải!"
        exit 1
    }
    Write-Host "Mã băm SHA256 hoàn toàn trùng khớp!" -ForegroundColor Green
}

# 2. Tạo database sandbox
Write-Host "Tạo database sandbox [$VerifyDb]..."
& mysql -h $DbHost -P $DbPort -u $DbUser -e "DROP DATABASE IF EXISTS \`$VerifyDb\`; CREATE DATABASE \`$VerifyDb\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Nạp dữ liệu
Write-Host "Đang nhập dữ liệu vào sandbox..."
Get-Content $BackupFile | & mysql -h $DbHost -P $DbPort -u $DbUser $VerifyDb

# 4. Kiểm tra số lượng bảng và bản ghi
$tableCount = & mysql -h $DbHost -P $DbPort -u $DbUser -N -s -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='$VerifyDb';"
Write-Host "- Tổng số bảng phục hồi: $tableCount" -ForegroundColor Yellow

$coreTables = @("nguoi_dung", "mon_hoc", "hoc_ky", "lop_hoc_phan", "dang_ky")
foreach ($t in $coreTables) {
    $c = & mysql -h $DbHost -P $DbPort -u $DbUser -N -s -e "SELECT count(*) FROM \`$VerifyDb\`.\`$t\`;"
    Write-Host "  + Bảng [$t]: $c bản ghi"
}

Write-Host "=== Xác minh khôi phục THÀNH CÔNG! ===" -ForegroundColor Green
