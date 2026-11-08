import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, ClipboardList, Layers, MapPin, Megaphone, Users } from "lucide-react";
import ClassAnnouncementModal from "../../components/ClassAnnouncementModal";
import GradeAnalyticsPanel from "../../components/GradeAnalyticsPanel";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { lichHoc } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function LecturerClassesPage() {
  const { data, loading, error } = useApi("/giang-vien/me/lop-hoc-phan");
  const [hocKyId, setHocKyId] = useState("");
  const [selectedLopForAnnouncement, setSelectedLopForAnnouncement] = useState(null);

  const hocKyList = useMemo(() => {
    const map = new Map();
    (data || []).forEach((item) => {
      if (!map.has(item.hocKyId)) map.set(item.hocKyId, item.tenHocKy);
    });
    return [...map.entries()].map(([id, ten]) => ({ id, ten }));
  }, [data]);

  const danhSach = useMemo(() => {
    if (!hocKyId) return data || [];
    return (data || []).filter((item) => String(item.hocKyId) === String(hocKyId));
  }, [data, hocKyId]);

  return (
    <div>
      <PageHeader
        icon={Layers}
        title="Lớp học phần phụ trách"
        description="Các lớp được phân công và danh sách sinh viên để nhập điểm."
        actions={
          <select className="input w-auto min-w-[200px]" value={hocKyId} onChange={(e) => setHocKyId(e.target.value)}>
            <option value="">Tất cả học kỳ</option>
            {hocKyList.map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten}
              </option>
            ))}
          </select>
        }
      />

      {error && <Alert tone="error" className="mb-4">{error}</Alert>}

      {loading ? (
        <Spinner />
      ) : danhSach.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={Layers}
            title="Chưa được phân công lớp học phần"
            description="Liên hệ phòng đào tạo nếu cần bổ sung lịch giảng dạy."
          />
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {danhSach.map((lop) => (
            <div key={lop.id} className="card flex flex-col p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-indigo-600">{lop.ma}</p>
                  <h2 className="mt-1 font-semibold text-slate-900">{lop.tenMon}</h2>
                  <p className="text-xs text-slate-500">
                    {lop.maMon} · {lop.soTinChi} tín chỉ
                  </p>
                </div>
                <Badge tone="sky">{lop.tenHocKy}</Badge>
              </div>

              <dl className="mt-4 space-y-2 text-sm text-slate-600">
                <div className="flex items-center gap-2">
                  <ClipboardList size={15} className="text-slate-400" />
                  {lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)}
                </div>
                <div className="flex items-center gap-2">
                  <MapPin size={15} className="text-slate-400" />
                  Phòng {lop.phong}
                </div>
                <div className="flex items-center gap-2">
                  <Users size={15} className="text-slate-400" />
                  {lop.siSoHienTai}/{lop.siSoToiDa} sinh viên
                </div>
              </dl>

              <div className="mt-5 space-y-2">
                <Link
                  to={`/lop-cua-toi/${lop.id}/nhap-diem`}
                  className="btn-primary w-full py-2 text-sm justify-center"
                >
                  Nhập điểm & xem danh sách
                </Link>
                <Link
                  to={`/lop-cua-toi/${lop.id}/diem-danh`}
                  className="btn-secondary w-full py-1.5 text-xs justify-center"
                >
                  <CalendarCheck size={14} /> Điểm danh & chuyên cần
                </Link>
                <button
                  type="button"
                  onClick={() => setSelectedLopForAnnouncement(lop)}
                  className="btn-secondary w-full py-1.5 text-xs justify-center"
                >
                  <Megaphone size={14} /> Gửi thông báo
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Thống kê điểm và phân bố trong phạm vi lớp giảng viên phụ trách (QLSV-75 / TASK-LC-10) */}
      <div className="mt-6">
        <GradeAnalyticsPanel mode="lecturer" initialHocKyId={hocKyId} />
      </div>

      {/* Modal gửi thông báo cho lớp (QLSV-68 / TASK-LC-03) */}
      <ClassAnnouncementModal
        open={Boolean(selectedLopForAnnouncement)}
        onClose={() => setSelectedLopForAnnouncement(null)}
        lop={selectedLopForAnnouncement}
      />
    </div>
  );
}
