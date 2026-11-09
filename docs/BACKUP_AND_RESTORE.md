# QUY TRÌNH SAO LƯU VÀ KHÔI PHỤC CƠ SỞ DỮ LIỆU (BACKUP & RESTORE)

> **Mã công việc:** QLSV-94 — TASK-OP-07 (Work Package D)  
> **Áp dụng cho:** Hệ thống Quản lý Đào tạo & Đăng ký Học phần  
> **Cơ sở dữ liệu thực tế:** MySQL 8.4 Community Server (`quan_ly_dao_tao`) triển khai qua Docker Compose container `qldt-mysql` / host port `3307`.  
> **Lưu trữ tệp:** Volume `support_uploads` gắn tại `/app/data/support-attachments`.

---

## 1. Nguyên tắc An toàn & Bảo mật

1. **Không đưa secret/mật khẩu vào kho mã nguồn (Git):** Mọi lệnh, script và cấu hình chỉ tham chiếu biến môi trường (`$MYSQL_PWD`, `$MYSQL_ROOT_PASSWORD`, `.my.cnf` phân quyền `0600`).
2. **Bảo toàn dữ liệu nhất quán (Transactional Consistency):** Sử dụng cờ `--single-transaction` khi sao lưu bảng InnoDB để không khóa bảng khi hệ thống đang phục vụ người dùng.
3. **Mã hóa và xác thực tính toàn vẹn:** Mỗi bản sao lưu phải được băm SHA-256 (`.sha256`) và nén mã hóa trước khi đưa ra bộ nhớ ngoài hoặc cloud storage.
4. **Quy tắc 3-2-1:** Duy trì ít nhất 3 bản sao chép, trên 2 phương tiện lưu trữ khác nhau, với ít nhất 1 bản ngoài cơ sở dữ liệu chính (offsite).
5. **Chính sách lưu trữ (Retention Policy):**
   - Bản sao lưu theo giờ: Giữ 24 giờ gần nhất.
   - Bản sao lưu hàng ngày (Daily): Giữ 30 ngày.
   - Bản sao lưu hàng tháng (Monthly): Giữ 12 tháng.

---

## 2. Quy trình & Lệnh Sao lưu Thực tế (Backup Procedures)

### 2.1. Sao lưu Cơ sở dữ liệu qua Docker Container (Khuyến nghị cho Môi trường Triển khai)

```bash
# 1. Đặt thư mục và biến môi trường (không echo mật khẩu ra màn hình)
export BACKUP_DIR="./backups/$(date +%Y%m%d)"
mkdir -p "${BACKUP_DIR}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DUMP_FILE="${BACKUP_DIR}/quan_ly_dao_tao_${TIMESTAMP}.sql.gz"

# 2. Thực thi mysqldump từ bên trong container qldt-mysql
docker exec qldt-mysql mysqldump \
  -u root -p"${MYSQL_ROOT_PASSWORD:-root}" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  quan_ly_dao_tao | gzip -c > "${DUMP_FILE}"

# 3. Tạo mã băm xác thực tính toàn vẹn SHA-256
sha256sum "${DUMP_FILE}" > "${DUMP_FILE}.sha256"

# 4. Kiểm tra tệp nén không bị hỏng
gzip -t "${DUMP_FILE}"
echo "Sao lưu cơ sở dữ liệu thành công: ${DUMP_FILE}"
```

### 2.2. Sao lưu qua MySQL Client ngoài Host (Cổng 3307)

```bash
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-root}"
mysqldump -h 127.0.0.1 -P 3307 -u root \
  --single-transaction --quick --routines --triggers --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  quan_ly_dao_tao | gzip -c > "${DUMP_FILE}"
unset MYSQL_PWD
```

### 2.3. Sao lưu Thư mục Tệp đính kèm (Support Attachments)

```bash
ATTACHMENT_BACKUP="${BACKUP_DIR}/attachments_${TIMESTAMP}.tar.gz"
tar -czf "${ATTACHMENT_BACKUP}" -C ./backend/data/support-attachments .
sha256sum "${ATTACHMENT_BACKUP}" > "${ATTACHMENT_BACKUP}.sha256"
```

---

## 3. Quy trình Khôi phục & Xác minh (Restore & Verification)

> **CẢNH BÁO QUAN TRỌNG:** Không bao giờ khôi phục trực tiếp vào cơ sở dữ liệu sản xuất (Production) mà không kiểm thử trước trên môi trường độc lập (Sandbox/Staging).

### 3.1. Bước 1: Xác thực mã băm SHA-256 & Tính toàn vẹn tệp nén

```bash
# Kiểm tra checksum
sha256sum -c "${DUMP_FILE}.sha256"

# Kiểm tra tệp nén gzip
gzip -t "${DUMP_FILE}"
```

### 3.2. Bước 2: Khôi phục vào Cơ sở dữ liệu Sandbox Kiểm thử

```bash
# Tạo cơ sở dữ liệu sandbox tạm
docker exec -i qldt-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD:-root}" -e \
  "DROP DATABASE IF EXISTS quan_ly_dao_tao_verify; CREATE DATABASE quan_ly_dao_tao_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Nạp dữ liệu bản sao lưu vào sandbox
gzip -dc "${DUMP_FILE}" | docker exec -i qldt-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD:-root}" quan_ly_dao_tao_verify
```

### 3.3. Bước 3: Kiểm tra Xác minh Tự động (Verification Checks)

1. **Kiểm tra tổng số bảng:**
   ```sql
   SELECT count(*) AS total_tables 
   FROM information_schema.tables 
   WHERE table_schema = 'quan_ly_dao_tao_verify';
   ```
   *Yêu cầu:* Số bảng tối thiểu >= 25 bảng (bao gồm người dùng, sinh viên, học kỳ, môn học, lớp học phần, đăng ký, bảng điểm, tác vụ nền...).

2. **Kiểm tra số lượng bản ghi của các bảng trọng yếu:**
   ```sql
   SELECT 'nguoi_dung' AS table_name, count(*) AS count FROM quan_ly_dao_tao_verify.nguoi_dung
   UNION ALL
   SELECT 'mon_hoc', count(*) FROM quan_ly_dao_tao_verify.mon_hoc
   UNION ALL
   SELECT 'hoc_ky', count(*) FROM quan_ly_dao_tao_verify.hoc_ky
   UNION ALL
   SELECT 'lop_hoc_phan', count(*) FROM quan_ly_dao_tao_verify.lop_hoc_phan
   UNION ALL
   SELECT 'dang_ky', count(*) FROM quan_ly_dao_tao_verify.dang_ky;
   ```

3. **Kiểm tra toàn vẹn ràng buộc khóa ngoại (Foreign Keys):**
   ```sql
   -- Kiểm tra không có sinh viên mồ côi (không tồn tại trong bảng người dùng)
   SELECT count(*) FROM quan_ly_dao_tao_verify.sinh_vien sv 
   LEFT JOIN quan_ly_dao_tao_verify.nguoi_dung u ON sv.nguoi_dung_id = u.id 
   WHERE u.id IS NULL;
   -- Kết quả mong đợi: 0
   ```

4. **Kiểm tra dữ liệu mã hóa mật khẩu & UTF-8:**
   ```sql
   -- Xác nhận mật khẩu lưu dạng bcrypt hash ($2a$ hoặc $2b$)
   SELECT username, substring(password, 1, 4) AS hash_prefix 
   FROM quan_ly_dao_tao_verify.nguoi_dung LIMIT 3;
   ```

### 3.4. Bước 4: Khôi phục chính thức vào Sản xuất (Production Rollout)

Khi đã xác nhận bản sao lưu an toàn tại Sandbox:
1. Thông báo bảo trì và tạm dừng dịch vụ backend (`docker compose stop backend`).
2. Thực hiện sao lưu khẩn cấp dữ liệu hiện tại làm bản đối chứng `pre_restore_backup`.
3. Nạp dữ liệu đã xác thực vào `quan_ly_dao_tao`.
4. Giải nén thư mục tệp đính kèm vào volume `support_uploads`.
5. Khởi động lại dịch vụ backend (`docker compose start backend`).
6. Gọi endpoint kiểm tra sức khỏe `GET /api/health` để xác nhận hệ thống sẵn sàng.

---

## 4. Công cụ Tự động hóa Đi kèm

Hệ thống đã chuẩn hóa các script thực thi nhanh tại thư mục `tools/ops/`:
- `tools/ops/backup.sh`: Tự động sao lưu, nén gzip và tạo mã băm SHA-256 trên môi trường Linux/Docker.
- `tools/ops/backup.ps1`: Tự động sao lưu và tạo SHA-256 trên Windows PowerShell.
- `tools/ops/restore-verify.sh`: Tự động nạp vào DB sandbox và in báo cáo số lượng bảng/bản ghi.
- `tools/ops/restore-verify.ps1`: Tự động nạp và kiểm tra tính hợp lệ bản sao lưu trên Windows.
