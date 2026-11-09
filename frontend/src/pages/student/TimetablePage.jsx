import { useEffect, useState } from "react";
import { CalendarDays } from "lucide-react";
import AnnouncementPanel from "../../components/AnnouncementPanel";
import TimetableBoard from "../../components/TimetableBoard";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";

export default function TimetablePage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data, loading, error } = useApi("/sinh-vien/me/thoi-khoa-bieu", params);
  const { data: thongBao } = useApi("/sinh-vien/me/thong-bao-nghi", params);
  const hocKy = (hocKyList || []).find((item) => String(item.id) === String(hocKyId));

  return (
    <div>
      <PageHeader
        icon={CalendarDays}
        title="Thời khóa biểu"
        description="Thời khóa biểu theo từng tuần và các thông báo nghỉ học của giáo viên."
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
          announcements={thongBao || []}
          hocKy={hocKy}
          editable={false}
        />

        <div className="card min-w-0 p-4">
          {error && <Alert tone="error" className="mb-4">{error}</Alert>}
          {loading ? (
            <Spinner />
          ) : !data?.length ? (
            <EmptyState
              icon={CalendarDays}
              title="Chưa có lịch học"
              description="Đăng ký học phần để thời khóa biểu hiển thị tại đây."
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
