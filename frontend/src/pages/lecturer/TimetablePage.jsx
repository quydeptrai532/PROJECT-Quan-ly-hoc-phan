import { useEffect, useState } from "react";
import { CalendarDays } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import AnnouncementPanel from "../../components/AnnouncementPanel";
import { useConfirm } from "../../components/ConfirmDialog";
import TimetableBoard from "../../components/TimetableBoard";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDate } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function LecturerTimetablePage() {
  const confirm = useConfirm();
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [actionMessage, setActionMessage] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data, loading, error } = useApi("/giang-vien/me/thoi-khoa-bieu", params);
  const { data: thongBao, reload: reloadThongBao } = useApi(
    "/giang-vien/me/thong-bao-nghi",
    params
  );
  const hocKy = (hocKyList || []).find((item) => String(item.id) === String(hocKyId));

  const taoThongBao = async (payload) => {
    setBusy(true);
    setActionError("");
    setActionMessage("");
    try {
      await api.post("/giang-vien/me/thong-bao-nghi", payload);
      setActionMessage("Đã đăng thông báo nghỉ học. Buổi học chuyển màu đỏ trên thời khóa biểu.");
      reloadThongBao();
      return true;
    } catch (err) {
      setActionError(getErrorMessage(err));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const xoaThongBao = async (thongBaoItem) => {
    const daXacNhan = await confirm({
      title: "Xoá thông báo nghỉ",
      message: `Xoá thông báo nghỉ ngày ${formatDate(thongBaoItem.ngayNghi)} của lớp ${thongBaoItem.maLop}?`,
      detail: "Buổi học sẽ trở lại bình thường trên thời khóa biểu.",
      confirmLabel: "Xoá thông báo",
      tone: "danger",
    });
    if (!daXacNhan) return;

    setActionError("");
    setActionMessage("");
    try {
      await api.delete(`/giang-vien/me/thong-bao-nghi/${thongBaoItem.id}`);
      setActionMessage("Đã xoá thông báo nghỉ học.");
      reloadThongBao();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        icon={CalendarDays}
        title="Thời khóa biểu giảng dạy"
        description="Lịch giảng dạy theo từng tuần và đăng thông báo nghỉ học cho lớp được phân công."
        actions={
          <select
            className="input w-auto min-w-[220px]"
            value={hocKyId}
            onChange={(event) => setHocKyId(event.target.value)}
          >
            {(hocKyList || []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten} - {item.namHoc}
              </option>
            ))}
          </select>
        }
      />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[330px_minmax(0,1fr)]">
        <AnnouncementPanel
          classes={data || []}
          announcements={thongBao || []}
          hocKy={hocKy}
          editable
          busy={busy}
          error={actionError}
          success={actionMessage}
          onDismissError={() => setActionError("")}
          onDismissSuccess={() => setActionMessage("")}
          onCreate={taoThongBao}
          onDelete={xoaThongBao}
        />

        <div className="card min-w-0 p-4">
          {error && <Alert tone="error" className="mb-4">{error}</Alert>}
          {loading ? (
            <Spinner />
          ) : !data?.length ? (
            <EmptyState
              icon={CalendarDays}
              title="Chưa có lịch giảng dạy"
              description="Lịch sẽ hiển thị khi bạn được phân công lớp học phần."
            />
          ) : (
            <TimetableBoard
              key={hocKyId}
              items={data}
              hocKy={hocKy}
              announcements={thongBao || []}
            />
          )}
        </div>
      </div>
    </div>
  );
}
