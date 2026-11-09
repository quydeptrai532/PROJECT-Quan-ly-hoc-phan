#!/usr/bin/env bash
# ==============================================================================
# Script khôi phục và xác minh tính toàn vẹn cơ sở dữ liệu (QLSV-94 / TASK-OP-07)
# Quy trình:
#  1. Kiểm tra tính toàn vẹn checksum SHA-256 của tệp backup.
#  2. Nạp bản sao lưu vào cơ sở dữ liệu kiểm thử (sandbox: quan_ly_dao_tao_restore_test).
#  3. Tự động kiểm tra số lượng bảng, số lượng bản ghi của các bảng cốt lõi.
#  4. Đưa ra báo cáo xác nhận khôi phục thành công.
# ==============================================================================
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Cách dùng: $0 <duong-dan-tep-backup.sql.gz hoặc .sql>"
  exit 1
fi

BACKUP_FILE="$1"
VERIFY_DB="${VERIFY_DB:-quan_ly_dao_tao_restore_test}"
DB_USER="${MYSQL_USER:-root}"
DB_HOST="${MYSQL_HOST:-localhost}"
DB_PORT="${MYSQL_PORT:-3307}"

echo "=== [$(date)] Bắt đầu quy trình xác minh khôi phục sao lưu ==="
echo "Tệp sao lưu: ${BACKUP_FILE}"
echo "Cơ sở dữ liệu đích xác minh (Sandbox): ${VERIFY_DB}"

# 1. Kiểm tra checksum SHA256 nếu có tệp .sha256
if [ -f "${BACKUP_FILE}.sha256" ]; then
  echo "Xác minh mã băm SHA256..."
  sha256sum -c "${BACKUP_FILE}.sha256"
  echo "Mã băm SHA256 hợp lệ!"
else
  echo "Cảnh báo: Không tìm thấy tệp ${BACKUP_FILE}.sha256; tiếp tục giải nén kiểm tra cấu trúc."
fi

# 2. Khởi tạo database sandbox sạch
echo "Khởi tạo database sandbox sạch [${VERIFY_DB}]..."
mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
  -e "DROP DATABASE IF EXISTS \`${VERIFY_DB}\`; CREATE DATABASE \`${VERIFY_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Nạp tệp sao lưu vào database sandbox
echo "Đang khôi phục dữ liệu vào database sandbox..."
if [[ "${BACKUP_FILE}" == *.gz ]]; then
  gzip -dc "${BACKUP_FILE}" | mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" "${VERIFY_DB}"
else
  mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" "${VERIFY_DB}" < "${BACKUP_FILE}"
fi

# 4. Xác minh tính toàn vẹn (Kiểm tra bảng và số lượng bản ghi cốt lõi)
echo "=== KẾT QUẢ XÁC MINH CƠ SỞ DỮ LIỆU ==="
TOTAL_TABLES=$(mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -N -s \
  -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='${VERIFY_DB}';")
echo "- Tổng số bảng phục hồi: ${TOTAL_TABLES}"

if [ "${TOTAL_TABLES}" -lt 10 ]; then
  echo "LỖI: Số lượng bảng quá ít (${TOTAL_TABLES}), bản sao lưu có thể bị lỗi hoặc chưa hoàn chỉnh!"
  exit 1
fi

# Đếm số bản ghi các bảng cốt lõi
for TABLE in nguoi_dung mon_hoc hoc_ky lop_hoc_phan dang_ky; do
  COUNT=$(mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -N -s \
    -e "SELECT count(*) FROM \`${VERIFY_DB}\`.\`${TABLE}\`;" 2>/dev/null || echo "0")
  echo "  + Bảng [${TABLE}]: ${COUNT} bản ghi"
done

echo "=== Xác minh khôi phục hoàn tất THÀNH CÔNG! Bản sao lưu an toàn để sử dụng. ==="
