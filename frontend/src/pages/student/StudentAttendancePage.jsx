import { useMemo, useState } from "react";
import {
  AlertTriangle,
  CalendarCheck,
  CalendarDays,
  CheckCircle,
  HelpCircle,
  Info,
  UserCheck,
  XCircle,
} from "lucide-react";
import { Alert, Badge, EmptyState, PageHeader, Spinner, StatCard } from "../../components/ui";
import { useApi } from "../../lib/useApi";

export default function StudentAttendancePage() {
  const { data: hocKyList, loading: loadingHocKy } = useApi("/common/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");

  const params = useMemo(() => (hocKyId ? { hocKyId } : {}), [hocKyId]);
  const { data: reportData, loading, error, reload } = useApi("/sinh-vien/me/chuyen-can", params);
  const { data: alertsData } = useApi("/sinh-vien/me/canh-bao-diem-danh");
  const activeAlerts = useMemo(() => {
    return (alertsData || []).filter((a) => a.trangThai === "DANG_CANH_BAO");
  }, [alertsData]);

  const danhSach = reportData || [];

  // Computed summary
  const summary = useMemo(() => {
    let tongMon = danhSach.length;
    let tongBuoi = 0;
    let coMat = 0;
    let vang = 0;
    let muon = 0;
    let coPhep = 0;

    danhSach.forEach((item) => {
      tongBuoi += item.tongSoBuoi || 0;
      coMat += item.soCoMat || 0;
      vang += item.soVang || 0;
      muon += item.soMuon || 0;
      coPhep += item.soCoPhep || 0;
    });

    const tyLeChung = tongBuoi > 0 ? ((coMat + coPhep) / tongBuoi) * 100 : 100;

    return {
      tongMon,
      tongBuoi,
      coMat,
      vang,
      muon,
      coPhep,
      tyLeChung: Math.min(100, Math.max(0, Math.round(tyLeChung * 10) / 10)),
    };
  }, [danhSach]);

  return (
    <div className="space-y-6">
      <PageHeader
        icon={CalendarCheck}
        title="Chuyên cần học tập"
        description="Theo dõi số buổi học, tình trạng điểm danh và tỷ lệ chuyên cần theo từng môn học."
        actions={
          <div className="flex items-center gap-2">
            <select
              className="input w-auto min-w-[200px]"
              value={hocKyId}
              onChange={(e) => setHocKyId(e.target.value)}
              disabled={loadingHocKy}
            >
              <option value="">Tất cả học kỳ</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} ({hk.namHoc})
                </option>
              ))}
            </select>
          </div>
        }
      />

      {error && <Alert tone="error">{error}</Alert>}

      {activeAlerts.length > 0 && (
        <Alert tone="warning">
          <div className="font-semibold text-amber-950 flex items-center gap-1.5">
            <AlertTriangle size={16} /> Cảnh báo nghỉ quá ngưỡng ({activeAlerts.length})
          </div>
          <div className="mt-2 space-y-1.5 text-xs text-amber-900 leading-relaxed">
            {activeAlerts.map((a) => (
              <div key={a.id} className="p-2.5 bg-amber-100/70 rounded-lg border border-amber-200">
                <span className="font-semibold text-amber-950">{a.maLop} — {a.tenMon}:</span> {a.thongDiep}
              </div>
            ))}
          </div>
        </Alert>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Môn học đang theo dõi"
          value={summary.tongMon}
          hint="Học phần có lịch điểm danh"
          icon={CalendarDays}
          tone="indigo"
        />
        <StatCard
          label="Tổng số buổi đã diễn ra"
          value={summary.tongBuoi}
          hint={`Có mặt: ${summary.coMat} · Phép: ${summary.coPhep}`}
          icon={UserCheck}
          tone="sky"
        />
        <StatCard
          label="Số buổi vắng không phép"
          value={summary.vang}
          hint={`Muộn: ${summary.muon} buổi`}
          icon={XCircle}
          tone={summary.vang > 0 ? "rose" : "emerald"}
        />
        <StatCard
          label="Tỷ lệ chuyên cần chung"
          value={`${summary.tyLeChung.toFixed(1)}%`}
          hint={summary.tyLeChung >= 80 ? "Đạt chuẩn tham gia học tập" : "Cần lưu ý nguy cơ cấm thi"}
          icon={CheckCircle}
          tone={summary.tyLeChung >= 80 ? "emerald" : "amber"}
        />
      </div>

      {/* Policy Notice Box */}
      {danhSach.length > 0 && danhSach[0]?.ghiChuQuyChe && (
        <Alert tone="info">
          <div className="flex items-center gap-2 font-semibold text-sky-950">
            <Info size={16} /> Lưu ý về cách tính tỷ lệ chuyên cần
          </div>
          <p className="mt-1 text-xs text-sky-900 leading-relaxed">
            {danhSach[0].ghiChuQuyChe}
          </p>
        </Alert>
      )}

      {/* Course Attendance Table */}
      {loading ? (
        <Spinner label="Đang tải dữ liệu chuyên cần..." />
      ) : danhSach.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={CalendarCheck}
            title="Chưa có dữ liệu điểm danh"
            description="Bạn chưa đăng ký môn học nào trong học kỳ này hoặc giảng viên chưa tạo buổi điểm danh."
          />
        </div>
      ) : (
        <div className="card p-5 space-y-4">
          <h2 className="text-base font-semibold text-slate-800">
            Chi tiết chuyên cần theo từng lớp học phần
          </h2>

          <div className="overflow-x-auto rounded-xl border border-slate-200">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-600">
                <tr>
                  <th className="px-4 py-3">Mã lớp</th>
                  <th className="px-4 py-3">Tên môn học</th>
                  <th className="px-3 py-3 text-center">Tín chỉ</th>
                  <th className="px-3 py-3 text-center">Tổng buổi</th>
                  <th className="px-3 py-3 text-center text-emerald-700">Có mặt</th>
                  <th className="px-3 py-3 text-center text-rose-700">Vắng</th>
                  <th className="px-3 py-3 text-center text-amber-700">Muộn</th>
                  <th className="px-3 py-3 text-center text-sky-700">Có phép</th>
                  <th className="px-4 py-3 text-center">Tỷ lệ có mặt</th>
                  <th className="px-4 py-3 text-center">Tỷ lệ vắng</th>
                  <th className="px-4 py-3 text-center">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {danhSach.map((item) => {
                  const isCritical = item.tyLeCoMat < 70;
                  const isWarning = item.tyLeCoMat < 80;
                  return (
                    <tr key={item.lopHocPhanId} className="hover:bg-slate-50/70 transition">
                      <td className="px-4 py-3 font-semibold text-indigo-700">{item.maLop}</td>
                      <td className="px-4 py-3 font-medium text-slate-800">{item.tenMon}</td>
                      <td className="px-3 py-3 text-center text-slate-600">{item.soTinChi}</td>
                      <td className="px-3 py-3 text-center font-semibold text-slate-700">{item.tongSoBuoi}</td>
                      <td className="px-3 py-3 text-center font-bold text-emerald-600">{item.soCoMat}</td>
                      <td className="px-3 py-3 text-center font-bold text-rose-600">{item.soVang}</td>
                      <td className="px-3 py-3 text-center font-bold text-amber-600">{item.soMuon}</td>
                      <td className="px-3 py-3 text-center font-bold text-sky-600">{item.soCoPhep}</td>
                      <td className="px-4 py-3 text-center font-bold">
                        <span className={isCritical ? "text-rose-600" : isWarning ? "text-amber-600" : "text-emerald-700"}>
                          {item.tyLeCoMat.toFixed(1)}%
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center text-xs text-slate-500">
                        {item.tyLeVang.toFixed(1)}%
                      </td>
                      <td className="px-4 py-3 text-center">
                        {isCritical ? (
                          <Badge tone="red">Nguy cơ cấm thi</Badge>
                        ) : isWarning ? (
                          <Badge tone="amber">Cảnh báo</Badge>
                        ) : (
                          <Badge tone="green">Đạt</Badge>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
