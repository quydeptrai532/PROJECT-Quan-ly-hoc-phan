#!/usr/bin/env bash
# ==============================================================================
# Script giải phóng dung lượng đĩa cho VPS chạy QLDV (Docker Compose + MySQL)
# Mặc định chỉ BÁO CÁO. Truyền --apply để thực thi dọn dẹp.
#
#   ./tools/ops/cleanup-disk.sh            # xem dung lượng, không xóa gì
#   sudo ./tools/ops/cleanup-disk.sh --apply
#
# An toàn: không xóa volume dữ liệu (mysql_data, support_uploads),
# không chạy `docker system prune -a --volumes`.
# ==============================================================================
set -euo pipefail

APPLY=false
[[ "${1:-}" == "--apply" ]] && APPLY=true

run() {
  if [[ "${APPLY}" == "true" ]]; then
    echo "+ $*"
    eval "$@"
  else
    echo "  [dry-run] $*"
  fi
}

section() { printf '\n=== %s ===\n' "$1"; }

section "Dung lượng đĩa tổng quan"
df -h / 2>/dev/null || true

if command -v docker >/dev/null 2>&1; then
  section "Docker: mức sử dụng hiện tại"
  docker system df || true

  section "Dọn build cache (an toàn, chỉ dữ liệu build)"
  run "docker builder prune -af"

  section "Dọn image dangling / không dùng bởi container nào"
  run "docker image prune -f"

  section "Dọn container đã dừng"
  run "docker container prune -f"

  section "Giới hạn log container (giữ 200MB gần nhất mỗi container)"
  if [[ "${APPLY}" == "true" ]]; then
    docker ps -aq | while read -r cid; do
      [[ -z "${cid}" ]] && continue
      logpath="$(docker inspect --format '{{.LogPath}}' "${cid}" 2>/dev/null || true)"
      [[ -z "${logpath}" || ! -f "${logpath}" ]] && continue
      size=$(du -m "${logpath}" | cut -f1)
      if [[ "${size}" -gt 200 ]]; then
        echo "+ truncate ${logpath} (${size}MB)"
        truncate -s 0 "${logpath}"
      fi
    done
  else
    echo "  [dry-run] truncate log container > 200MB"
  fi

  section "Kích thước volume (KHÔNG xóa, chỉ để kiểm tra)"
  docker volume ls --format '{{.Name}}' | while read -r vol; do
    [[ -z "${vol}" ]] && continue
    mnt=$(docker volume inspect --format '{{.Mountpoint}}' "${vol}" 2>/dev/null || true)
    [[ -z "${mnt}" || ! -d "${mnt}" ]] && continue
    printf '  %-40s %s\n' "${vol}" "$(du -sh "${mnt}" 2>/dev/null | cut -f1)"
  done
else
  echo "Không tìm thấy docker, bỏ qua phần Docker."
fi

section "Dọn cache apt"
run "apt-get clean"
run "apt-get autoremove -y --purge"

section "Giới hạn log hệ thống (journald)"
run "journalctl --vacuum-size=200M"

section "Top 15 thư mục/tệp lớn nhất trên / (có thể mất vài giây)"
du -xh --max-depth=2 / 2>/dev/null | sort -rh | head -n 15 || true

printf '\n'
if [[ "${APPLY}" == "true" ]]; then
  echo "Đã dọn xong. Dung lượng sau khi dọn:"
  df -h / 2>/dev/null || true
else
  echo "Chưa xóa gì. Chạy lại với --apply để thực thi:"
  echo "  sudo ./tools/ops/cleanup-disk.sh --apply"
fi

echo "Lưu ý: sao lưu DB trước khi thao tác mạnh (xem docs/BACKUP_AND_RESTORE.md)."
