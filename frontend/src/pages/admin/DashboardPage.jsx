import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BookMarked,
  BookOpen,
  CalendarRange,
  CheckCircle2,
  ClipboardCheck,
  Download,
  GraduationCap,
  HelpCircle,
  Info,
  Layers,
  LayoutDashboard,
  TrendingUp,
  UserCog,
  UserRound,
  XCircle,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import GradeAnalyticsPanel from "../../components/GradeAnalyticsPanel";
import { Alert, Badge, Modal, PageHeader, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../lib/useApi";
import api, { getErrorMessage } from "../../api/client";

const BUCKET_COLORS = ["#f43f5e", "#f59e0b", "#3b82f6", "#6366f1", "#10b981"];
const GPA_COLORS = ["#10b981", "#3b82f6", "#6366f1", "#f59e0b", "#f43f5e"];

async function docThongBaoLoi(err) {
  const data = err?.response?.data;
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text());
      if (parsed?.message) return parsed.message;
    } catch {
      // Dùng thông báo mặc định
    }
  }
  return getErrorMessage(err);
}

function layTenTepTuHeader(headers, macDinh) {
  const disposition = headers?.["content-disposition"] || headers?.["Content-Disposition"];
  if (!disposition) return macDinh;

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;\s]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i);
  if (asciiMatch?.[1]) {
    return asciiMatch[1].trim();
  }
  return macDinh;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const { data: hocKyList } = useApi("/hoc-ky");
  const { data: khoaList } = useApi("/admin/khoa");
  const { data: nganhList } = useApi("/admin/nganh");

  const [hocKyId, setHocKyId] = useState("");
  const [khoaId, setKhoaId] = useState("");
  const [nganhId, setNganhId] = useState("");
  const [exporting, setExporting] = useState(false);
  const [exportSuccess, setExportSuccess] = useState("");
  const [exportError, setExportError] = useState("");
  const [showKpiModal, setShowKpiModal] = useState(false);

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  // Lọc ngành theo khoa đã chọn
  const filteredNganhList = useMemo(() => {
    if (!khoaId) return nganhList || [];
    return (nganhList || []).filter((n) => String(n.khoaId) === String(khoaId));
  }, [nganhList, khoaId]);

  const handleKhoaChange = (newKhoaId) => {
    setKhoaId(newKhoaId);
    if (newKhoaId && nganhId) {
      const match = (nganhList || []).find(
        (n) => String(n.id) === String(nganhId) && String(n.khoaId) === String(newKhoaId)
      );
      if (!match) {
        setNganhId("");
      }
    }
  };

  const { data: tongQuan } = useApi("/admin/thong-ke/tong-quan");

  const kpiParams = useMemo(() => {
    if (!hocKyId) return undefined;
    const p = { hocKyId };
    if (khoaId) p.khoaId = khoaId;
    if (nganhId) p.nganhId = nganhId;
    return p;
  }, [hocKyId, khoaId, nganhId]);

  const {
    data: kpiData,
    loading: kpiLoading,
    error: kpiError,
    reload: reloadKpi,
  } = useApi("/admin/thong-ke/kpi-dashboard", kpiParams);

  const hanhDongParams = hocKyId ? { hocKyId, ...(khoaId ? { khoaId } : {}) } : undefined;
  const { data: hanhDong, loading: hanhDongLoading, error: hanhDongError } =
    useApi("/admin/thong-ke/hanh-dong", hanhDongParams);

  const hocKy = hocKyList?.find((item) => String(item.id) === String(hocKyId));

  const kpi = kpiData?.kpi;
  const tongDangKy = kpi?.tongDangKy ?? 0;
  const tongChiTieu = kpi?.tongChiTieu ?? 0;
  const tiLeLapDay = kpi?.tiLeLapDay ?? 0;
  const conTrong = kpi?.conTrong ?? 0;

  const pieData = [
    { name: "Đã đăng ký", value: tongDangKy },
    { name: "Còn trống", value: conTrong },
  ];

  const chartData = (kpiData?.danhSachMon || []).slice(0, 15).map((item) => ({
    name: item.maMon,
    "Đã đăng ký": item.daDangKy,
    "Chỉ tiêu": item.tongChiTieu,
  }));

  const bucketChartData = (kpiData?.phanBoDiem || []).map((b, idx) => ({
    name: b.kyHieuKhoang,
    tenKhoang: b.tenKhoang,
    soLuong: b.soLuong,
    tyLePhanTram: b.tyLePhanTram,
    color: BUCKET_COLORS[idx % BUCKET_COLORS.length],
  }));

  const gpaChartData = (kpiData?.phanBoGpa || []).map((g, idx) => ({
    name: g.tenLoai,
    kyHieu: g.kyHieuKhoang,
    soLuong: g.soLuong,
    tyLePhanTram: g.tyLePhanTram,
    color: GPA_COLORS[idx % GPA_COLORS.length],
  }));

  const CHART_TOOLTIP = {
    borderRadius: 12,
    border: "1px solid #e2e8f0",
    fontSize: 12,
    boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
  };

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      setExportSuccess("");
      setExportError("");
      const searchParams = new URLSearchParams();
      if (hocKyId) searchParams.set("hocKyId", hocKyId);
      if (khoaId) searchParams.set("khoaId", khoaId);
      if (nganhId) searchParams.set("nganhId", nganhId);

      const res = await api.get(`/admin/thong-ke/export?${searchParams.toString()}`, {
        responseType: "blob",
      });

      const defaultFilename = `bao-cao-thong-ke-dao-tao-${new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19)}.csv`;
      const tenTep = layTenTepTuHeader(res.headers, defaultFilename);
      const blob = new Blob([res.data], { type: "text/csv;charset=utf-8;" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", tenTep);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setExportSuccess(`Đã xuất báo cáo thống kê đào tạo thành công (${tenTep})`);
    } catch (err) {
      setExportError(await docThongBaoLoi(err));
    } finally {
      setExporting(false);
    }
  };

  return (
    <div>
      <div className="mb-4 space-y-2">
        {exportSuccess && <Alert tone="success" onClose={() => setExportSuccess("")}>{exportSuccess}</Alert>}
        {exportError && <Alert tone="error" onClose={() => setExportError("")}>{exportError}</Alert>}
      </div>
      <PageHeader
        icon={LayoutDashboard}
        title="Tổng quan đào tạo"
        description="Thống kê toàn diện chỉ số KPI, đăng ký học phần, điểm đã công bố và tiến độ học tập."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <select
              className="input w-auto min-w-[200px]"
              value={hocKyId}
              onChange={(event) => setHocKyId(event.target.value)}
              aria-label="Chọn học kỳ"
            >
              {(hocKyList || []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} - {item.namHoc}
                </option>
              ))}
            </select>

            <select
              className="input w-auto min-w-[170px]"
              value={khoaId}
              onChange={(event) => handleKhoaChange(event.target.value)}
              aria-label="Lọc theo khoa"
            >
              <option value="">Tất cả khoa</option>
              {(khoaList || []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten}
                </option>
              ))}
            </select>

            <select
              className="input w-auto min-w-[170px]"
              value={nganhId}
              onChange={(event) => setNganhId(event.target.value)}
              aria-label="Lọc theo ngành học"
            >
              <option value="">Tất cả ngành</option>
              {filteredNganhList.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} ({item.ma})
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={handleExportCsv}
              disabled={exporting || kpiLoading}
              className="btn-secondary flex items-center gap-1.5"
              title="Xuất file CSV thống kê chi tiết theo bộ lọc đang xem"
            >
              <Download size={16} />
              {exporting ? "Đang xuất..." : "Xuất CSV"}
            </button>

            <button
              type="button"
              onClick={() => setShowKpiModal(true)}
              className="btn-secondary flex items-center gap-1.5"
              title="Xem định nghĩa, tử số, mẫu số và trạng thái duyệt của từng KPI"
            >
              <BookOpen size={16} />
              Từ điển KPI
            </button>
          </div>
        }
      />

      {/* Banner thông tin quản trị */}
      <div className="dashboard-hero page-enter relative mb-6 overflow-hidden rounded-[24px] border border-[#dce6d5] p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-10 -top-16 h-56 w-56 rounded-full bg-indigo-500/25 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-20 right-32 h-48 w-48 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="relative flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="hero-muted text-[11px] font-semibold uppercase tracking-[0.2em]">
              Bảng điều khiển học vụ
            </p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight">
              Xin chào, {user?.hoTen || user?.username}
            </h2>
            <p className="hero-muted mt-1.5 max-w-xl text-sm leading-relaxed">
              Theo dõi tình hình mở lớp, đăng ký học phần, điểm đã công bố và tiến độ tích lũy tín chỉ trong học kỳ.
            </p>
          </div>
          <div className="flex items-center gap-3 rounded-xl border border-white/80 bg-white/70 px-4 py-3">
            <span className="grid h-10 w-10 place-items-center rounded-lg bg-[#edf3e4] text-[#58734f]">
              <CalendarRange size={20} />
            </span>
            <div>
              <p className="hero-muted text-[10px] font-semibold uppercase tracking-wider">
                Học kỳ hiện tại
              </p>
              <p className="text-sm font-medium">{tongQuan?.hocKyHienTai || "—"}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Thông tin bộ lọc đang áp dụng & Cảnh báo trạng thái điểm */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs text-slate-600 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-semibold text-slate-800">Phạm vi đang lọc:</span>
          <Badge tone="indigo">{kpiData?.boLoc?.tenHocKy || "Tất cả học kỳ"}</Badge>
          {kpiData?.boLoc?.tenKhoa && <Badge tone="sky">{kpiData.boLoc.tenKhoa}</Badge>}
          {kpiData?.boLoc?.tenNganh && <Badge tone="emerald">{kpiData.boLoc.tenNganh}</Badge>}
        </div>
        <div className="flex items-center gap-2 text-slate-500">
          <Badge tone="green">Chỉ tính điểm ĐÃ CÔNG BỐ</Badge>
          <span>· Dữ liệu truy vấn lúc {kpiData?.boLoc?.thoiDiemTruyVan ? new Date(kpiData.boLoc.thoiDiemTruyVan).toLocaleTimeString("vi-VN") : "—"}</span>
        </div>
      </div>

      {kpiError && <Alert tone="error" className="mb-6">{kpiError}</Alert>}

      {/* 5 THẺ CHỈ SỐ KPI CỐT LÕI (QLSV-86) */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard
          label="Lượt đăng ký hiệu lực"
          value={kpi?.tongDangKy ?? (kpiLoading ? "..." : "—")}
          icon={ClipboardCheck}
          tone="indigo"
          description={`Chỉ tiêu mở lớp: ${kpi?.tongChiTieu ?? "—"} chỗ`}
        />
        <StatCard
          label="Tỷ lệ lấp đầy"
          value={kpi?.tiLeLapDay !== undefined ? `${kpi.tiLeLapDay}%` : (kpiLoading ? "..." : "—")}
          icon={Layers}
          tone={tiLeLapDay >= 85 ? "emerald" : tiLeLapDay >= 50 ? "amber" : "rose"}
          description={`Còn trống: ${kpi?.conTrong ?? "—"} chỗ`}
        />
        <StatCard
          label="Tỷ lệ Đạt (>= 5.0)"
          value={kpi?.tyLeDat !== undefined ? `${kpi.tyLeDat}%` : (kpiLoading ? "..." : "—")}
          icon={CheckCircle2}
          tone="emerald"
          description={`Đạt ${kpi?.soSinhVienDat ?? 0} / ${kpi?.soSinhVienCoDiem ?? 0} SV có điểm`}
        />
        <StatCard
          label="Điểm TB / GPA (4)"
          value={
            kpi?.gpa4TrungBinh !== null && kpi?.gpa4TrungBinh !== undefined
              ? `${kpi.gpa4TrungBinh} / 4.0`
              : (kpiLoading ? "..." : "—")
          }
          icon={GraduationCap}
          tone="sky"
          description={`Điểm TB thang 10: ${kpi?.diemTrungBinh ?? "—"}`}
        />
        <StatCard
          label="Tiến độ tích lũy TB"
          value={
            kpi?.tyLeHoanThanhTinChiTb !== null && kpi?.tyLeHoanThanhTinChiTb !== undefined
              ? `${kpi.tyLeHoanThanhTinChiTb}%`
              : (kpiLoading ? "..." : "—")
          }
          icon={TrendingUp}
          tone="amber"
          description={`Xét trên ${kpi?.tongSinhVienXetTienDo ?? 0} sinh viên`}
        />
      </div>

      {/* Danh sách cảnh báo hành động học vụ */}
      <section className="card mb-6 overflow-hidden" aria-labelledby="action-list-title">
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-4">
          <div>
            <h2 id="action-list-title" className="text-base font-semibold text-slate-900">
              Việc cần giáo vụ chú ý
            </h2>
            <p className="mt-1 text-xs text-slate-500">
              Quy tắc: lớp gần đầy từ 90%; ít đăng ký không quá 25%; nhu cầu môn khi đăng ký đạt tổng chỉ tiêu; đợt đóng trong 7 ngày.
            </p>
          </div>
          <p className="text-xs text-slate-500" aria-live="polite">
            {hanhDong?.capNhatLuc ? `Dữ liệu truy vấn lúc ${new Date(hanhDong.capNhatLuc).toLocaleString("vi-VN")}` : ""}
          </p>
        </div>
        {hanhDongError ? (
          <div className="p-5"><Alert tone="error">Không tải được danh sách việc cần chú ý: {hanhDongError}</Alert></div>
        ) : hanhDongLoading ? (
          <Spinner label="Đang tổng hợp đăng ký theo học kỳ..." />
        ) : !hanhDong?.hocKyId ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">Chưa có học kỳ để phân tích.</div>
        ) : hanhDong.canhBao.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">
            Không có mục cần chú ý theo các quy tắc hiển thị cho {hanhDong.tenHocKy}{hanhDong.tenKhoa ? ` · ${hanhDong.tenKhoa}` : ""}.
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {hanhDong.canhBao.map((item, index) => (
              <li key={`${item.loai}-${item.lopHocPhanId || item.monHocId || item.hocKyId}-${index}`} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">{item.tieuDe}</span>
                    <Badge tone={item.loai === "LOP_GAN_DAY" || item.loai === "MON_HET_CHO" ? "amber" : "slate"}>
                      {item.loai === "LOP_GAN_DAY" ? "Gần đầy" : item.loai === "LOP_IT_DANG_KY" ? "Ít đăng ký" : item.loai === "MON_HET_CHO" ? "Đạt chỉ tiêu" : "Sắp đóng"}
                    </Badge>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{item.lyDo}</p>
                  <p className="mt-1 text-xs text-slate-400">{item.mucTinh} · {item.tenHocKy}</p>
                  {(item.maLop || item.maMon) && (
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      {item.maLop ? `Lớp ${item.maLop} · ` : ""}{item.maMon ? `${item.maMon} · ${item.tenMon}` : ""}
                      {item.soLop > 1 ? ` · ${item.soLop} lớp` : ""}
                    </p>
                  )}
                </div>
                <Link to={item.duongDan} className="btn-secondary shrink-0 self-start sm:self-center">
                  Xem dữ liệu nguồn
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* KHỐI BIỂU ĐỒ 1: Đăng ký theo môn & Tỷ lệ lấp đầy */}
      <div className="mb-6 grid gap-6 xl:grid-cols-3">
        <section className="card p-5 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Đăng ký theo môn học</h2>
              <p className="mt-0.5 text-xs text-slate-500">So sánh số lượng đăng ký hiệu lực với chỉ tiêu mở lớp</p>
            </div>
            <Badge tone="indigo">{chartData.length} môn hiển thị</Badge>
          </div>

          {kpiLoading ? (
            <Spinner />
          ) : chartData.length === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">Chưa có dữ liệu trong phạm vi lọc.</div>
          ) : (
            <div className="mt-4 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <RechartsTooltip contentStyle={CHART_TOOLTIP} cursor={{ fill: "rgba(99, 102, 241, 0.06)" }} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="Đã đăng ký" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={30} isAnimationActive={false} />
                  <Bar dataKey="Chỉ tiêu" fill="#cbd5e1" radius={[6, 6, 0, 0]} maxBarSize={30} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="card p-5">
          <div>
            <h2 className="text-sm font-semibold text-slate-800">Tỷ lệ lấp đầy</h2>
            <p className="mt-0.5 text-xs text-slate-500">Toàn bộ lớp trong phạm vi lọc</p>
          </div>

          {kpiLoading ? (
            <Spinner />
          ) : tongChiTieu === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">Chưa có dữ liệu mở lớp.</div>
          ) : (
            <>
              <div className="relative mt-4 h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={64}
                      outerRadius={90}
                      paddingAngle={3}
                      startAngle={90}
                      endAngle={-270}
                      strokeWidth={0}
                      isAnimationActive={false}
                    >
                      <Cell fill="#6366f1" />
                      <Cell fill="#e2e8f0" />
                    </Pie>
                    <RechartsTooltip contentStyle={CHART_TOOLTIP} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <p className="text-2xl font-semibold text-slate-900">{tiLeLapDay}%</p>
                  <p className="text-[11px] uppercase tracking-wider text-slate-400">Đã lấp đầy</p>
                </div>
              </div>
              <div className="mt-2 grid grid-cols-2 gap-3 text-center">
                <div className="rounded-xl bg-indigo-50 px-3 py-2">
                  <p className="text-lg font-semibold text-indigo-700">{tongDangKy}</p>
                  <p className="text-[11px] text-indigo-500">Đã đăng ký</p>
                </div>
                <div className="rounded-xl bg-slate-50 px-3 py-2">
                  <p className="text-lg font-semibold text-slate-700">{conTrong}</p>
                  <p className="text-[11px] text-slate-500">Còn trống</p>
                </div>
              </div>
            </>
          )}
        </section>
      </div>

      {/* KHỐI BIỂU ĐỒ 2: Phân bố khoảng điểm & Phân loại học lực GPA */}
      <div className="mb-6 grid gap-6 xl:grid-cols-2">
        {/* Phân bố 5 khoảng điểm */}
        <section className="card p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Phân bố khoảng điểm (5 khoảng)</h2>
              <p className="mt-0.5 text-xs text-slate-500">Chỉ thống kê trên các lớp có bảng điểm ĐÃ CÔNG BỐ</p>
            </div>
            <Badge tone="emerald">{kpi?.soSinhVienCoDiem ?? 0} SV có điểm</Badge>
          </div>

          {kpiLoading ? (
            <Spinner />
          ) : (kpi?.soSinhVienCoDiem ?? 0) === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">
              Chưa có sinh viên nào có điểm công bố trong phạm vi lọc.
            </div>
          ) : (
            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={bucketChartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <RechartsTooltip contentStyle={CHART_TOOLTIP} />
                  <Bar dataKey="soLuong" name="Số sinh viên" radius={[6, 6, 0, 0]} maxBarSize={36} isAnimationActive={false}>
                    {bucketChartData.map((entry, index) => (
                      <Cell key={`cell-bucket-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        {/* Phân bố GPA */}
        <section className="card p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Phân loại học lực GPA (Hệ 4)</h2>
              <p className="mt-0.5 text-xs text-slate-500">Quy đổi thang 4 theo chuẩn tín chỉ đại học</p>
            </div>
            <Badge tone="indigo">
              GPA TB: {kpi?.gpa4TrungBinh !== null && kpi?.gpa4TrungBinh !== undefined ? kpi.gpa4TrungBinh : "—"}
            </Badge>
          </div>

          {kpiLoading ? (
            <Spinner />
          ) : (kpi?.soSinhVienCoDiem ?? 0) === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">
              Chưa có dữ liệu GPA đã công bố trong phạm vi lọc.
            </div>
          ) : (
            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={gpaChartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <RechartsTooltip contentStyle={CHART_TOOLTIP} />
                  <Bar dataKey="soLuong" name="Số lượng" radius={[6, 6, 0, 0]} maxBarSize={36} isAnimationActive={false}>
                    {gpaChartData.map((entry, index) => (
                      <Cell key={`cell-gpa-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>
        {/* Thao tác nhanh */}
        <section className="card p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Thao tác nhanh</h2>
              <p className="mt-0.5 text-xs text-slate-500">Phím tắt quản trị học vụ</p>
            </div>
            <Badge tone="slate">Lối tắt</Badge>
          </div>
          <div className="mt-4 space-y-2.5">
            <Link
              to="/quan-tri/lop-hoc-phan"
              className="flex items-center justify-between rounded-xl border border-slate-200 px-3.5 py-2.5 text-xs font-medium text-slate-700 transition hover:border-indigo-300 hover:bg-indigo-50/70"
            >
              <span>Mở lớp học phần</span>
              <Layers size={16} className="text-slate-400" />
            </Link>
            <Link
              to="/quan-tri/sinh-vien"
              className="flex items-center justify-between rounded-xl border border-slate-200 px-3.5 py-2.5 text-xs font-medium text-slate-700 transition hover:border-indigo-300 hover:bg-indigo-50/70"
            >
              <span>Quản lý sinh viên</span>
              <UserRound size={16} className="text-slate-400" />
            </Link>
            <Link
              to="/quan-tri/hoc-ky"
              className="flex items-center justify-between rounded-xl border border-slate-200 px-3.5 py-2.5 text-xs font-medium text-slate-700 transition hover:border-indigo-300 hover:bg-indigo-50/70"
            >
              <span>Cấu hình đợt đăng ký</span>
              <ClipboardCheck size={16} className="text-slate-400" />
            </Link>
            <Link
              to="/quan-tri/phuc-khao"
              className="flex items-center justify-between rounded-xl border border-slate-200 px-3.5 py-2.5 text-xs font-medium text-slate-700 transition hover:border-indigo-300 hover:bg-indigo-50/70"
            >
              <span>Xử lý phúc khảo điểm</span>
              <GraduationCap size={16} className="text-slate-400" />
            </Link>
          </div>
        </section>
      </div>

      {/* BẢNG DỮ LIỆU THỐNG KÊ CHI TIẾT THEO MÔN HỌC */}
      <div className="card mb-6 overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-5 py-4">
          <div>
            <h2 className="text-sm font-semibold text-slate-800">Thống kê chi tiết theo môn học và lớp</h2>
            <p className="text-xs text-slate-500">
              Cùng nguồn tính toán với biểu đồ và file CSV export · Chỉ tính điểm đã công bố
            </p>
          </div>
          <Badge tone="indigo">{kpiData?.danhSachMon?.length || 0} môn học</Badge>
        </div>

        {kpiLoading ? (
          <Spinner />
        ) : (kpiData?.danhSachMon || []).length === 0 ? (
          <div className="px-4 py-12 text-center text-sm text-slate-500">
            Không có môn học hoặc lớp nào trong phạm vi lọc đã chọn.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-xs">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="th">Môn học</th>
                  <th className="th text-center">Tín chỉ</th>
                  <th className="th text-center">Số lớp</th>
                  <th className="th text-center">Chỉ tiêu</th>
                  <th className="th text-center">Đã đăng ký</th>
                  <th className="th">Tỷ lệ lấp đầy</th>
                  <th className="th text-center">Số SV có điểm</th>
                  <th className="th text-center">Điểm TB (10)</th>
                  <th className="th text-center">GPA (4)</th>
                  <th className="th text-center">Tỷ lệ Đạt</th>
                  <th className="th text-center">Chưa có điểm</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {kpiData.danhSachMon.map((item) => (
                  <tr key={item.monHocId} className="hover:bg-slate-50/60">
                    <td className="td">
                      <p className="font-medium text-slate-900">{item.tenMon}</p>
                      <p className="text-[11px] text-slate-400">{item.maMon}</p>
                    </td>
                    <td className="td text-center font-medium">{item.soTinChi}</td>
                    <td className="td text-center">{item.soLop}</td>
                    <td className="td text-center">{item.tongChiTieu}</td>
                    <td className="td text-center font-semibold text-slate-800">{item.daDangKy}</td>
                    <td className="td">
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-20 overflow-hidden rounded-full bg-slate-200">
                          <div
                            className={`h-full rounded-full ${
                              item.tiLeLapDay >= 90
                                ? "bg-rose-500"
                                : item.tiLeLapDay >= 60
                                ? "bg-amber-500"
                                : "bg-emerald-500"
                            }`}
                            style={{ width: `${Math.min(100, item.tiLeLapDay)}%` }}
                          />
                        </div>
                        <span className="text-[11px] text-slate-600">{item.tiLeLapDay}%</span>
                      </div>
                    </td>
                    <td className="td text-center">{item.soSinhVienCoDiem}</td>
                    <td className="td text-center font-semibold text-slate-800">
                      {item.diemTrungBinh !== null && item.diemTrungBinh !== undefined ? item.diemTrungBinh : "—"}
                    </td>
                    <td className="td text-center font-semibold text-indigo-700">
                      {item.gpa4 !== null && item.gpa4 !== undefined ? item.gpa4 : "—"}
                    </td>
                    <td className="td text-center">
                      {item.soSinhVienCoDiem > 0 ? (
                        <span className="font-medium text-emerald-700">{item.tyLeDat}%</span>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="td text-center text-slate-400">
                      {item.soChuaCoDiem > 0 ? item.soChuaCoDiem : 0}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* GradeAnalyticsPanel cấp quản trị */}
      <div className="mt-6">
        <GradeAnalyticsPanel
          mode="admin"
          initialHocKyId={hocKyId}
          initialKhoaId={khoaId}
        />
      </div>

      {/* MODAL TỪ ĐIỂN KPI VÀ QUY CHẾ ĐÀO TẠO (QLSV-86) */}
      <Modal
        open={showKpiModal}
        onClose={() => setShowKpiModal(false)}
        title="Từ điển KPI & Quy chuẩn Báo cáo Quản trị"
        description="Định nghĩa chuẩn hóa các chỉ số KPI, công thức, tử số, mẫu số, xử lý thiếu dữ liệu và tình trạng phê duyệt quy chế."
        size="xl"
      >
        <div className="space-y-6 text-xs text-slate-700">
          <div className="rounded-xl border border-sky-200 bg-sky-50/70 p-4 leading-relaxed text-sky-950">
            <div className="flex items-start gap-2">
              <Info size={16} className="mt-0.5 shrink-0 text-sky-600" />
              <div>
                <span className="font-semibold">Nguyên tắc Single Source of Truth:</span> Toàn bộ các thẻ KPI,
                biểu đồ trực quan, bảng dữ liệu và chức năng Xuất CSV đều được tính toán từ cùng một truy vấn và
                công thức chuẩn hóa ở backend. Chỉ các lớp có bảng điểm <span className="font-semibold underline">ĐÃ CÔNG BỐ (hoặc ĐÃ KHÓA)</span> mới
                được đưa vào tính toán Đạt/Trượt, Điểm TB và GPA.
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full border-collapse border border-slate-200 text-left">
              <thead className="bg-slate-100 text-[11px] font-semibold uppercase text-slate-700">
                <tr>
                  <th className="border border-slate-200 p-2.5">Chỉ số KPI</th>
                  <th className="border border-slate-200 p-2.5">Tử số (Numerator)</th>
                  <th className="border border-slate-200 p-2.5">Mẫu số (Denominator)</th>
                  <th className="border border-slate-200 p-2.5">Đơn vị</th>
                  <th className="border border-slate-200 p-2.5">Xử lý thiếu dữ liệu</th>
                  <th className="border border-slate-200 p-2.5">Trạng thái duyệt</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                <tr>
                  <td className="border border-slate-200 p-2.5 font-semibold text-slate-900">
                    Lượt đăng ký hiệu lực
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng bản ghi đăng ký có trạng thái <code>DA_DANG_KY</code>
                  </td>
                  <td className="border border-slate-200 p-2.5 text-slate-400">Không có (Số rời rạc)</td>
                  <td className="border border-slate-200 p-2.5">Lượt</td>
                  <td className="border border-slate-200 p-2.5">Loại bỏ đăng ký <code>DA_HUY</code></td>
                  <td className="border border-slate-200 p-2.5 font-medium text-emerald-700">Đã chốt</td>
                </tr>
                <tr>
                  <td className="border border-slate-200 p-2.5 font-semibold text-slate-900">
                    Tỷ lệ lấp đầy
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng lượt đăng ký hiệu lực (<code>daDangKy</code>)
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng chỉ tiêu mở lớp (<code>siSoToiDa</code>)
                  </td>
                  <td className="border border-slate-200 p-2.5">%</td>
                  <td className="border border-slate-200 p-2.5">Nếu chỉ tiêu = 0, trả về 0.0%</td>
                  <td className="border border-slate-200 p-2.5 font-medium text-emerald-700">Đã chốt</td>
                </tr>
                <tr>
                  <td className="border border-slate-200 p-2.5 font-semibold text-slate-900">
                    Tỷ lệ Đạt / Trượt
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Số SV có điểm tổng kết &gt;= 5.0 (Đạt) hoặc &lt; 5.0 (Trượt)
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    M = Số SV có điểm tổng kết hợp lệ trong [0.0, 10.0] thuộc lớp đã công bố
                  </td>
                  <td className="border border-slate-200 p-2.5">SV & %</td>
                  <td className="border border-slate-200 p-2.5">
                    Chưa có điểm hoặc lớp chưa công bố bị loại khỏi mẫu số M, đưa vào <code>soChuaCoDiem</code>
                  </td>
                  <td className="border border-slate-200 p-2.5 font-medium text-emerald-700">
                    Đã chốt (Ngưỡng 5.0)
                  </td>
                </tr>
                <tr>
                  <td className="border border-slate-200 p-2.5 font-semibold text-slate-900">
                    GPA (Hệ 10 & Hệ 4)
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng tích số (Điểm x Tín chỉ môn) của các môn đã công bố
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng số tín chỉ của các môn có điểm công bố hợp lệ
                  </td>
                  <td className="border border-slate-200 p-2.5">Điểm (Thang 10 / 4)</td>
                  <td className="border border-slate-200 p-2.5">
                    Môn chưa có điểm không đưa vào tử và mẫu. Không trả PII sinh viên.
                  </td>
                  <td className="border border-slate-200 p-2.5 font-medium text-amber-700">
                    Chờ duyệt mốc học lại
                  </td>
                </tr>
                <tr>
                  <td className="border border-slate-200 p-2.5 font-semibold text-slate-900">
                    Tiến độ tích lũy CTĐT
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Số tín chỉ đạt (earned) từ môn thuộc CTĐT có điểm &gt;= 5.0 đã công bố
                  </td>
                  <td className="border border-slate-200 p-2.5">
                    Tổng số tín chỉ yêu cầu của CTĐT (required)
                  </td>
                  <td className="border border-slate-200 p-2.5">Tín chỉ & %</td>
                  <td className="border border-slate-200 p-2.5">
                    Chưa gán CTĐT ghi nhận <code>configured: false</code>; tự chọn chặn trần theo nhóm
                  </td>
                  <td className="border border-slate-200 p-2.5 font-medium text-amber-700">
                    Chờ duyệt mốc năm học
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-xs text-amber-950">
            <h3 className="font-semibold text-amber-900">
              Các nội dung đang chờ Hội đồng Đào tạo / Nhà trường phê duyệt chính thức:
            </h3>
            <ul className="mt-2 list-inside list-disc space-y-1 text-slate-700">
              <li>
                <span className="font-medium text-slate-900">Tính GPA khi học cải thiện / học lại:</span> Nhà trường
                cần ban hành văn bản xác nhận lấy điểm cao nhất giữa các lần học hay lấy điểm của lần học cuối cùng vào
                GPA tích lũy toàn khóa.
              </li>
              <li>
                <span className="font-medium text-slate-900">Quy đổi thang điểm 4 cho D+ (5.0 - 5.4):</span> Hiện hệ thống
                quy đổi là 1.5; cần Hội đồng Đào tạo phê chuẩn bảng quy đổi đồng bộ toàn trường.
              </li>
              <li>
                <span className="font-medium text-slate-900">Tín chỉ tự chọn vượt trần nhóm:</span> Xác nhận việc có tính
                tín chỉ vượt trần vào điểm GPA tích lũy hay chỉ ghi nhận chứng nhận hoàn thành.
              </li>
              <li>
                <span className="font-medium text-slate-900">Khung SLA mốc tích lũy tín chỉ năm học:</span> Ban hành mốc
                tín chỉ tối thiểu sau Năm 1, Năm 2, Năm 3 để hệ thống tự động phân loại sinh viên đúng tiến độ hay cảnh báo học vụ.
              </li>
            </ul>
          </div>
        </div>
      </Modal>
    </div>
  );
}
