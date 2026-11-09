#!/usr/bin/env bash
# ==============================================================================
# Script sao lưu cơ sở dữ liệu & tệp đính kèm an toàn cho QLDV (QLSV-94 / TASK-OP-07)
# Tuân thủ:
#  - Không hardcode mật khẩu hay secret vào mã nguồn.
#  - Đọc thông tin từ biến môi trường (MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PWD, MYSQL_DATABASE).
#  - Tạo mã băm SHA256 để xác thực tính toàn vẹn khi phục hồi.
# ==============================================================================
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
DB_NAME="${MYSQL_DATABASE:-quan_ly_dao_tao}"
DB_USER="${MYSQL_USER:-root}"
DB_HOST="${MYSQL_HOST:-localhost}"
DB_PORT="${MYSQL_PORT:-3307}"
ATTACHMENTS_DIR="${ATTACHMENTS_DIR:-./backend/data/support-attachments}"

mkdir -p "${BACKUP_DIR}"

DUMP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"
SHA_FILE="${DUMP_FILE}.sha256"
ATTACHMENTS_FILE="${BACKUP_DIR}/attachments_${TIMESTAMP}.tar.gz"

echo "=== [$(date)] Bắt đầu sao lưu hệ thống Quản lý đào tạo ==="
echo "Host: ${DB_HOST}:${DB_PORT} | Database: ${DB_NAME} | User: ${DB_USER}"

# 1. Sao lưu cơ sở dữ liệu MySQL
if [ -n "${CONTAINER_NAME:-}" ]; then
  echo "Thực hiện sao lưu từ Docker container: ${CONTAINER_NAME}..."
  docker exec "${CONTAINER_NAME}" \
    mysqldump -u"${DB_USER}" -p"${MYSQL_PWD:-root}" \
    --single-transaction --quick --routines --triggers --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 "${DB_NAME}" | gzip -c > "${DUMP_FILE}"
else
  echo "Thực hiện sao lưu qua mysqldump client..."
  mysqldump -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
    --single-transaction --quick --routines --triggers --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 "${DB_NAME}" | gzip -c > "${DUMP_FILE}"
fi

# 2. Kiểm tra tính toàn vẹn và tạo checksum SHA-256
if [ ! -s "${DUMP_FILE}" ]; then
  echo "LỖI: Tệp sao lưu rỗng hoặc không được tạo thành công!"
  exit 1
fi
gzip -t "${DUMP_FILE}"
sha256sum "${DUMP_FILE}" > "${SHA_FILE}"
echo "Đã tạo bản sao lưu DB: ${DUMP_FILE} ($(du -h "${DUMP_FILE}" | cut -f1))"
echo "Checksum SHA256: $(cat "${SHA_FILE}")"

# 3. Sao lưu thư mục tệp đính kèm (nếu tồn tại)
if [ -d "${ATTACHMENTS_DIR}" ]; then
  echo "Sao lưu thư mục tệp đính kèm: ${ATTACHMENTS_DIR}..."
  tar -czf "${ATTACHMENTS_FILE}" -C "${ATTACHMENTS_DIR}" .
  sha256sum "${ATTACHMENTS_FILE}" > "${ATTACHMENTS_FILE}.sha256"
  echo "Đã sao lưu tệp đính kèm: ${ATTACHMENTS_FILE}"
fi

echo "=== Sao lưu hoàn tất thành công tại ${BACKUP_DIR} ==="
