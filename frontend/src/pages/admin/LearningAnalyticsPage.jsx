import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Award,
  BarChart3,
  BookOpen,
  CalendarRange,
  CheckCircle2,
  Download,
  FileCheck2,
  Filter,
  GraduationCap,
  HelpCircle,
  Info,
  Layers,
  LineChart as LineChartIcon,
  RefreshCw,
  TrendingUp,
  Users,
  XCircle,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner, StatCard } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { formatDiem } from "../../lib/format";

const BUCKET_COLORS = ["#f43f5e", "#f59e0b", "#3b82f6", "#6366f1", "#10b981"];

function renderDiem(val) {
  if (val === null || val === undefined) return "—";
  return formatDiem(val);
}

function renderTyLe(val) {
  if (val === null || val === undefined) return "—";
  return `${Number(val).toFixed(2).replace(/\.00$/, "")}%`;
}

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

export default function LearningAnalyticsPage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const { data: khoaList } = useApi("/admin/khoa");
  const { data: nganhList } = useApi("/admin/nganh");

  const [hocKyId, setHocKyId] = useState("");
  const [khoaId, setKhoaId] = useState("");
  const [nganhId, setNganhId] = useState("");
  const [monHocId, setMonHocId] = useState("");

  const [exporting, setExporting] = useState(false);
  const [exportSuccess, setExportSuccess] = useState("");
  const [exportError, setExportError] = useState("");

  // Chọn học kỳ mặc định
  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  // Lọc ngành theo khoa
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
      if (!match) setNganhId("");
    }
    setMonHocId("");
  };

  const analyticsParams = useMemo(() => {
    const p = {};
    if (hocKyId) p.hocKyId = hocKyId;
    if (khoaId) p.khoaId = khoaId;
    if (nganhId) p.nganhId = nganhId;
    if (monHocId) p.monHocId = monHocId;
    return p;
  }, [hocKyId, khoaId, nganhId, monHocId]);

  const {
    data: report,
    loading,
    error,
    reload: reloadAnalytics,
  } = useApi("/admin/learning-analytics", analyticsParams);

  const kpi = report?.kpi;
  const phanBoDiem = report?.phanBoDiem || [];
  const xuHuong = report?.xuHuongTheoHocKy || [];
  const soSanh = report?.soSanhCungMon;
  const danhSachMon = report?.danhSachMonHoc || [];

  // Dữ liệu biểu đồ phân bố điểm
  const phanBoChartData = useMemo(() => {
    return phanBoDiem.map((b, idx) => ({
      name: b.kyHieuKhoang,
      tenKhoang: b.tenKhoang,
      soLuong: b.soLuong,
      tyLePhanTram: b.tyLePhanTram,
      color: BUCKET_COLORS[idx % BUCKET_COLORS.length],
      laKhoangDat: b.laKhoangDat,
    }));
  }, [phanBoDiem]);

  // Dữ liệu biểu đồ xu hướng GPA qua các học kỳ
  const xuHuongChartData = useMemo(() => {
    return (xuHuong || []).map((x) => ({
      name: `${x.tenHocKy} (${x.namHoc})`,
      "Điểm TB (10)": x.diemTrungBinh !== null ? x.diemTrungBinh : null,
      "GPA (4)": x.gpaThang4 !== null ? x.gpaThang4 : null,
      "Tỷ lệ Đạt (%)": x.coDiemCongBo ? x.tyLeDat : null,
      mauSo: x.mauSoDuocTinh,
      coDiem: x.coDiemCongBo,
      ghiChu: x.ghiChu,
    }));
  }, [xuHuong]);

  // Dữ liệu biểu đồ so sánh các lớp cùng môn (chỉ lấy lớp đủ điều kiện)
  const soSanhChartData = useMemo(() => {
    if (!soSanh?.danhSachLop) return [];
    return soSanh.danhSachLop
      .filter((l) => l.duDieuKienSoSanhChinhThuc)
      .map((l) => ({
        name: l.maLop,
        giangVien: l.tenGiangVien,
        "Điểm TB": l.diemTrungBinh,
        "Tỷ lệ Đạt (%)": l.tyLeDat,
        mauSo: l.mauSo,
        duSoMau: l.duSoMauToiThieu,
      }));
  }, [soSanh]);

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      setExportSuccess("");
      setExportError("");

      const searchParams = new URLSearchParams();
      if (hocKyId) searchParams.set("hocKyId", hocKyId);
      if (khoaId) searchParams.set("khoaId", khoaId);
      if (nganhId) searchParams.set("nganhId", nganhId);
      if (monHocId) searchParams.set("monHocId", monHocId);

      const res = await api.get(`/admin/learning-analytics/xuat-csv?${searchParams.toString()}`, {
        responseType: "blob",
      });

      const defaultFilename = `learning-analytics-${new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19)}.csv`;
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
      setExportSuccess(`Đã xuất báo cáo phân tích học tập thành công (${tenTep})`);
    } catch (err) {
      setExportError(await docThongBaoLoi(err));
    } finally {
      setExporting(false);
    }
  };

  const CHART_TOOLTIP = {
    borderRadius: 12,
    border: "1px solid #e2e8f0",
    fontSize: 12,
    boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
  };

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        {exportSuccess && (
          <Alert tone="success" onClose={() => setExportSuccess("")}>
            {exportSuccess}
          </Alert>
        )}
        {exportError && (
          <Alert tone="error" onClose={() => setExportError("")}>
            {exportError}
          </Alert>
        )}
      </div>

      <PageHeader
        icon={BarChart3}
        title="Phân tích học tập (Learning Analytics)"
        description="Báo cáo phân bố điểm, tỷ lệ đạt/trượt, xu hướng GPA qua các học kỳ và so sánh khách quan các lớp cùng môn."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <select
              className="input w-auto min-w-[190px]"
              value={hocKyId}
              onChange={(e) => setHocKyId(e.target.value)}
              aria-label="Chọn học kỳ"
            >
              {(hocKyList || []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} - {item.namHoc}
                </option>
              ))}
            </select>

            <select
              className="input w-auto min-w-[160px]"
              value={khoaId}
              onChange={(e) => handleKhoaChange(e.target.value)}
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
              className="input w-auto min-w-[160px]"
              value={nganhId}
              onChange={(e) => setNganhId(e.target.value)}
              aria-label="Lọc theo ngành"
            >
              <option value="">Tất cả ngành</option>
              {filteredNganhList.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten} ({item.ma})
                </option>
              ))}
            </select>

            <select
              className="input w-auto min-w-[190px]"
              value={monHocId}
              onChange={(e) => setMonHocId(e.target.value)}
              aria-label="Lọc môn học để so sánh lớp"
            >
              <option value="">Tất cả môn học</option>
              {danhSachMon.map((item) => (
                <option key={item.monHocId} value={item.monHocId}>
                  {item.maMon} - {item.tenMon} ({item.soLopTrongPhamVi} lớp)
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={handleExportCsv}
              disabled={exporting || loading}
              className="btn-secondary flex items-center gap-1.5"
              title="Xuất file CSV đúng theo bộ lọc đang xem"
            >
              <Download size={16} />
              {exporting ? "Đang xuất..." : "Xuất CSV"}
            </button>

            <button
              type="button"
              onClick={reloadAnalytics}
              disabled={loading}
              className="btn-secondary flex items-center gap-1.5"
              title="Làm mới số liệu"
            >
              <RefreshCw size={16} />
            </button>
          </div>
        }
      />

      {/* Cảnh báo phi nhân quả (Causality disclaimer) */}
      <div className="flex items-start gap-3 rounded-2xl border border-amber-300 bg-amber-50/80 p-4 text-xs text-amber-950 shadow-sm">
        <AlertTriangle size={18} className="mt-0.5 shrink-0 text-amber-700" />
        <div className="leading-relaxed">
          <strong className="font-semibold uppercase tracking-wider text-amber-900">
            Nguyên tắc phân tích học tập khách quan:
          </strong>{" "}
          Toàn bộ các chỉ số hiển thị thuần túy mô tả thống kê phân bố điểm thực tế đã công bố theo quy chế đào tạo.{" "}
          <strong className="underline">Tuyệt đối không suy diễn kết luận nhân quả</strong> về năng lực giảng dạy của
          giảng viên, trình độ học tập của sinh viên hay độ khó của đề thi từ dữ liệu mô tả này mà không qua nghiên cứu kiểm
          soát các biến ngoại lai (thành phần sinh viên, hình thức thi cử, điều kiện giảng dạy, thời khóa biểu...).
        </div>
      </div>

      {loading ? (
        <div className="card p-12">
          <Spinner label="Đang tải dữ liệu phân tích học tập..." />
        </div>
      ) : error ? (
        <div className="card p-8">
          <Alert tone="error">
            Không tải được dữ liệu phân tích học tập: {error}
            <div className="mt-3">
              <button
                type="button"
                onClick={reloadAnalytics}
                className="btn-secondary text-xs"
              >
                Thử lại
              </button>
            </div>
          </Alert>
        </div>
      ) : !kpi || kpi.tongSoLop === 0 ? (
        <div className="card p-8">
          <EmptyState
            icon={Layers}
            title="Không tìm thấy dữ liệu lớp học phần"
            description="Không có lớp học phần nào phù hợp với bộ lọc hiện tại. Thử chọn lại học kỳ, khoa hoặc môn học khác."
          />
        </div>
      ) : (
        <>
          {/* Thẻ KPI cốt lõi */}
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <StatCard
              icon={Users}
              label="Số SV có điểm (M)"
              value={kpi.mauSoDuocTinh}
              note={`/ ${kpi.tongSoDangKyHieuLuc} lượt ĐK (${kpi.soLopDaCongBo}/${kpi.tongSoLop} lớp công bố)`}
              tone="indigo"
            />
            <StatCard
              icon={GraduationCap}
              label="Điểm trung bình (10)"
              value={renderDiem(kpi.diemTrungBinh)}
              note={
                kpi.mauSoDuocTinh > 0
                  ? `Min: ${renderDiem(kpi.diemThapNhat)} · Max: ${renderDiem(kpi.diemCaoNhat)}`
                  : "Chưa có điểm công bố"
              }
              tone="sky"
            />
            <StatCard
              icon={Award}
              label="GPA tương đương (4)"
              value={renderDiem(kpi.gpaThang4)}
              note="Quy đổi theo thang điểm 4 chuẩn quy chế"
              tone="indigo"
            />
            <StatCard
              icon={CheckCircle2}
              label="Tỷ lệ Đạt (≥ 5.0)"
              value={renderTyLe(kpi.tyLeDat)}
              note={`${kpi.soSinhVienDat} Đạt · ${kpi.soSinhVienTruot} Trượt (< 5.0)`}
              tone="green"
            />
            <StatCard
              icon={XCircle}
              label="Chưa có điểm công bố"
              value={kpi.soSinhVienChuaCoDiem}
              note="Loại khỏi mẫu số; không tự tính thành 0.0"
              tone="amber"
            />
          </div>

          {/* Khối Biểu đồ Phân bố điểm & Xu hướng GPA */}
          <div className="grid gap-6 lg:grid-cols-2">
            {/* 1. Biểu đồ Phân bố điểm */}
            <div className="card flex flex-col p-5">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div>
                  <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <BarChart3 size={17} className="text-indigo-600" />
                    Phân bố điểm tổng kết (M = {kpi.mauSoDuocTinh} sinh viên)
                  </h3>
                  <p className="mt-0.5 text-xs text-slate-500">
                    5 khoảng điểm chuẩn theo TASK-AD-11, không chồng lấn và không bỏ sót
                  </p>
                </div>
                <Badge tone="indigo">Ngưỡng đạt ≥ 5.0</Badge>
              </div>

              {kpi.mauSoDuocTinh === 0 ? (
                <div className="grid flex-1 place-items-center py-12 text-center text-xs text-slate-500">
                  Chưa có sinh viên nào có điểm công bố trong phạm vi lọc này.
                </div>
              ) : (
                <div className="h-72 w-full pt-2">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={phanBoChartData}
                      margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis
                        dataKey="name"
                        tick={{ fontSize: 11, fill: "#64748b" }}
                        axisLine={false}
                        tickLine={false}
                        interval={0}
                      />
                      <YAxis
                        tick={{ fontSize: 11, fill: "#64748b" }}
                        axisLine={false}
                        tickLine={false}
                        allowDecimals={false}
                      />
                      <RechartsTooltip
                        contentStyle={CHART_TOOLTIP}
                        formatter={(val, name, entry) => [
                          `${val} SV (${entry.payload.tyLePhanTram}%) - ${
                            entry.payload.laKhoangDat ? "ĐẠT" : "CHƯA ĐẠT"
                          }`,
                          entry.payload.tenKhoang,
                        ]}
                      />
                      <Bar dataKey="soLuong" radius={[6, 6, 0, 0]}>
                        {phanBoChartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* 2. Biểu đồ Xu hướng GPA qua các học kỳ */}
            <div className="card flex flex-col p-5">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div>
                  <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <TrendingUp size={17} className="text-emerald-600" />
                    Xu hướng GPA qua các học kỳ
                  </h3>
                  <p className="mt-0.5 text-xs text-slate-500">
                    Trình tự thời gian các học kỳ; kỳ chưa công bố điểm hiển thị trạng thái rỗng
                  </p>
                </div>
                <Badge tone="green">{xuHuongChartData.length} học kỳ</Badge>
              </div>

              {xuHuongChartData.length === 0 ? (
                <div className="grid flex-1 place-items-center py-12 text-center text-xs text-slate-500">
                  Không có dữ liệu chuỗi thời gian học kỳ.
                </div>
              ) : (
                <div className="h-72 w-full pt-2">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                      data={xuHuongChartData}
                      margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis
                        dataKey="name"
                        tick={{ fontSize: 10, fill: "#64748b" }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 11, fill: "#64748b" }}
                        axisLine={false}
                        tickLine={false}
                        domain={[0, 10]}
                      />
                      <RechartsTooltip
                        contentStyle={CHART_TOOLTIP}
                        formatter={(val, name, entry) => {
                          if (val === null || val === undefined) return ["Chưa công bố điểm", name];
                          if (name === "Tỷ lệ Đạt (%)") return [`${val}%`, name];
                          return [renderDiem(val), name];
                        }}
                      />
                      <Legend
                        wrapperStyle={{ fontSize: 11, paddingTop: 10 }}
                        iconType="circle"
                      />
                      <Line
                        type="monotone"
                        dataKey="Điểm TB (10)"
                        stroke="#6366f1"
                        strokeWidth={2.5}
                        dot={{ r: 4, fill: "#6366f1" }}
                        connectNulls={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="GPA (4)"
                        stroke="#10b981"
                        strokeWidth={2}
                        dot={{ r: 3, fill: "#10b981" }}
                        connectNulls={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>
          </div>

          {/* Khối So sánh các lớp cùng môn */}
          <section className="card overflow-hidden" aria-label="So sánh các lớp cùng môn">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <Layers size={18} className="text-indigo-600" />
                  <h3 className="text-sm font-semibold text-slate-900">
                    So sánh các lớp cùng môn học:{" "}
                    <span className="text-indigo-700">
                      {soSanh ? `${soSanh.maMon} - ${soSanh.tenMon}` : "Vui lòng chọn môn học ở bộ lọc phía trên"}
                    </span>
                  </h3>
                  {soSanh && (
                    <Badge tone={soSanh.coTheSoSanh ? "green" : "amber"}>
                      {soSanh.soLopDuDieuKienSoSanh}/{soSanh.tongSoLop} lớp đủ điều kiện
                    </Badge>
                  )}
                </div>
                <p className="mt-1 text-xs text-slate-500">
                  {soSanh?.thongDiepSoSanh ||
                    "Chọn một môn học cụ thể để xem đối chiếu giữa các lớp học phần cùng môn trong học kỳ."}
                </p>
              </div>
            </div>

            {!soSanh ? (
              <div className="p-8">
                <EmptyState
                  icon={BookOpen}
                  title="Chưa chọn môn học để so sánh lớp"
                  description="Sử dụng bộ lọc môn học ở góc trên bên phải để chọn môn học cần so sánh giữa các lớp học phần."
                />
              </div>
            ) : soSanh.danhSachLop.length === 0 ? (
              <div className="p-8">
                <EmptyState
                  icon={Layers}
                  title="Không có lớp học phần nào của môn học trong kỳ này"
                  description="Thử chọn học kỳ khác hoặc môn học khác để xem dữ liệu so sánh."
                />
              </div>
            ) : (
              <div className="space-y-6 p-5">
                {/* Biểu đồ so sánh Điểm TB và Tỷ lệ Đạt giữa các lớp đủ điều kiện */}
                {soSanhChartData.length > 0 && (
                  <div>
                    <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-600">
                      Biểu đồ so sánh Điểm TB và Tỷ lệ Đạt (Chỉ lớp đã công bố điểm)
                    </h4>
                    <div className="h-64 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart
                          data={soSanhChartData}
                          margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                          <XAxis
                            dataKey="name"
                            tick={{ fontSize: 11, fill: "#64748b" }}
                            axisLine={false}
                            tickLine={false}
                          />
                          <YAxis
                            tick={{ fontSize: 11, fill: "#64748b" }}
                            axisLine={false}
                            tickLine={false}
                            domain={[0, 10]}
                          />
                          <RechartsTooltip
                            contentStyle={CHART_TOOLTIP}
                            formatter={(val, name, entry) => [
                              name === "Tỷ lệ Đạt (%)" ? `${val}%` : renderDiem(val),
                              `${name} (${entry.payload.giangVien}, M = ${entry.payload.mauSo})`,
                            ]}
                          />
                          <Legend wrapperStyle={{ fontSize: 11 }} />
                          <Bar dataKey="Điểm TB" fill="#6366f1" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}

                {/* Bảng chi tiết từng lớp */}
                <div className="overflow-x-auto rounded-xl border border-slate-200">
                  <table className="w-full min-w-[960px] text-xs">
                    <thead className="bg-slate-50 text-slate-600">
                      <tr>
                        <th className="px-3.5 py-2.5 text-left font-semibold">Mã lớp</th>
                        <th className="px-3.5 py-2.5 text-left font-semibold">Giảng viên</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Sĩ số (ĐK/Chỉ tiêu)</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Mẫu số M (Có điểm)</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Chưa có điểm</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Trạng thái điểm</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Điểm TB</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Min – Max</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">GPA (4)</th>
                        <th className="px-3.5 py-2.5 text-center font-semibold">Đạt / Trượt</th>
                        <th className="px-3.5 py-2.5 text-left font-semibold">Điều kiện so sánh</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {soSanh.danhSachLop.map((lop) => {
                        const hopLe = lop.duDieuKienSoSanhChinhThuc && lop.duSoMauToiThieu;
                        return (
                          <tr
                            key={lop.lopHocPhanId}
                            className={`hover:bg-slate-50/80 ${
                              !lop.daCongBo ? "bg-amber-50/30" : ""
                            }`}
                          >
                            <td className="px-3.5 py-2.5 font-semibold text-slate-900">
                              {lop.maLop}
                            </td>
                            <td className="px-3.5 py-2.5 text-slate-700">
                              {lop.tenGiangVien}
                            </td>
                            <td className="px-3.5 py-2.5 text-center">
                              {lop.daDangKy} / {lop.siSoToiDa}
                            </td>
                            <td className="px-3.5 py-2.5 text-center font-bold text-indigo-700">
                              {lop.mauSo}
                            </td>
                            <td className="px-3.5 py-2.5 text-center text-amber-700">
                              {lop.soSinhVienChuaCoDiem}
                            </td>
                            <td className="px-3.5 py-2.5 text-center">
                              <Badge tone={lop.daCongBo ? "green" : "amber"}>
                                {lop.moTaTrangThai}
                              </Badge>
                            </td>
                            <td className="px-3.5 py-2.5 text-center font-bold text-slate-900">
                              {lop.daCongBo ? renderDiem(lop.diemTrungBinh) : "—"}
                            </td>
                            <td className="px-3.5 py-2.5 text-center text-slate-600">
                              {lop.daCongBo && lop.diemThapNhat !== null
                                ? `${renderDiem(lop.diemThapNhat)} – ${renderDiem(lop.diemCaoNhat)}`
                                : "—"}
                            </td>
                            <td className="px-3.5 py-2.5 text-center font-semibold text-emerald-700">
                              {lop.daCongBo ? renderDiem(lop.gpaThang4) : "—"}
                            </td>
                            <td className="px-3.5 py-2.5 text-center">
                              {lop.daCongBo && lop.mauSo > 0 ? (
                                <span>
                                  <strong className="text-emerald-700">
                                    {renderTyLe(lop.tyLeDat)}
                                  </strong>{" "}
                                  ({lop.soSinhVienDat}) /{" "}
                                  <span className="text-rose-600">
                                    {renderTyLe(lop.tyLeTruot)}
                                  </span>{" "}
                                  ({lop.soSinhVienTruot})
                                </span>
                              ) : (
                                <span className="text-slate-400">—</span>
                              )}
                            </td>
                            <td className="px-3.5 py-2.5 text-left">
                              <span
                                className={`inline-flex items-center gap-1 text-[11px] ${
                                  hopLe
                                    ? "text-emerald-700"
                                    : !lop.daCongBo
                                    ? "text-amber-800"
                                    : "text-rose-700"
                                }`}
                              >
                                {hopLe ? (
                                  <CheckCircle2 size={13} className="shrink-0 text-emerald-600" />
                                ) : (
                                  <AlertTriangle size={13} className="shrink-0 text-amber-600" />
                                )}
                                {lop.ghiChuDieuKien}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
