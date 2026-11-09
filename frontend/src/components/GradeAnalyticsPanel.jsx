import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Eye,
  FileCheck2,
  Filter,
  HelpCircle,
  Info,
  Layers,
  Lock,
  RefreshCw,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import api, { getErrorMessage } from "../api/client";
import { Alert, Badge, EmptyState, Spinner } from "./ui";
import { formatDiem } from "../lib/format";

const STATUS_BADGE_MAP = {
  NHAP: { label: "Bản nháp (Chưa công bố)", tone: "slate" },
  CHO_DUYET: { label: "Chờ duyệt (Chưa công bố)", tone: "amber" },
  TRA_VE: { label: "Trả về sửa (Chưa công bố)", tone: "rose" },
  DA_DUYET: { label: "Đã duyệt (Chờ công bố)", tone: "blue" },
  DA_CONG_BO: { label: "Đã công bố", tone: "green" },
  DA_KHOA: { label: "Đã khóa (Đã công bố)", tone: "indigo" },
};

const BUCKET_COLORS = ["#f43f5e", "#f59e0b", "#3b82f6", "#6366f1", "#10b981"];

function renderDiem(val) {
  if (val === null || val === undefined) return "—";
  return formatDiem(val);
}

function renderTyLe(val) {
  if (val === null || val === undefined) return "—";
  return `${Number(val).toFixed(2).replace(/\.00$/, "")}%`;
}

/**
 * Hiển thị khối chỉ số + biểu đồ phân bố khoảng điểm cho một tập dữ liệu (Công bố hoặc Tạm tính chưa công bố).
 */
function ChiSoVaPhanBoSection({
  chiSo,
  isPreviewUnpublished = false,
  nguongDiemDat = 5.0,
}) {
  if (!chiSo) return null;

  const coDuLieuTinh = (chiSo.soBanGhiDuocTinh || 0) > 0;
  const chartData = (chiSo.phanBoDiem || []).map((b, idx) => ({
    name: b.kyHieuKhoang,
    tenKhoang: b.tenKhoang,
    soLuong: b.soLuong,
    tyLePhanTram: b.tyLePhanTram ?? 0,
    color: BUCKET_COLORS[idx % BUCKET_COLORS.length],
  }));

  return (
    <div
      className={`rounded-2xl border p-4 sm:p-5 ${
        isPreviewUnpublished
          ? "border-amber-300 bg-amber-50/50"
          : "border-slate-200 bg-white"
      }`}
    >
      {isPreviewUnpublished && (
        <div className="mb-4 flex items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-100/80 px-3.5 py-2.5 text-xs text-amber-950">
          <AlertTriangle size={16} className="mt-0.5 shrink-0 text-amber-700" />
          <div>
            <span className="font-semibold">
              Dữ liệu tạm tính theo trạng thái làm việc hiện tại (Chưa công bố):
            </span>{" "}
            {chiSo.nhanPhamVi}. Khối dữ liệu này chỉ hiển thị riêng cho
            giảng viên phụ trách / quản trị để kiểm tra trước khi công bố và{" "}
            <span className="font-semibold underline">
              tuyệt đối không trộn vào thống kê công bố chính thức
            </span>
            .
          </div>
        </div>
      )}

      {/* 5 thẻ chỉ số cốt lõi */}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <div className="rounded-xl border border-slate-200 bg-slate-50/80 p-3.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
            Số SV có điểm (Mẫu số)
          </p>
          <p className="mt-1 text-xl font-bold text-slate-900">
            {chiSo.soBanGhiDuocTinh ?? 0}
            <span className="text-sm font-normal text-slate-500">
              {" "}
              / {chiSo.tongSoDangKyHieuLuc ?? 0} ĐK
            </span>
          </p>
          <p className="mt-1 text-[11px] text-slate-500">
            Chỉ tính SV đủ điểm hợp lệ
          </p>
        </div>

        <div className="rounded-xl border border-indigo-100 bg-indigo-50/60 p-3.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-indigo-700">
            Điểm trung bình
          </p>
          <p className="mt-1 text-xl font-bold text-indigo-950">
            {renderDiem(chiSo.diemTrungBinh)}
          </p>
          <p className="mt-1 text-[11px] text-indigo-600">
            Thang điểm 10 (HALF_UP 2 chữ số)
          </p>
        </div>

        <div className="rounded-xl border border-sky-100 bg-sky-50/60 p-3.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-sky-700">
            Thấp nhất – Cao nhất
          </p>
          <p className="mt-1 text-xl font-bold text-sky-950">
            {renderDiem(chiSo.diemThapNhat)} – {renderDiem(chiSo.diemCaoNhat)}
          </p>
          <p className="mt-1 text-[11px] text-sky-600">
            Biên độ điểm trong mẫu số
          </p>
        </div>

        <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-3.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-emerald-700">
            Đạt (&ge; {nguongDiemDat}) / Trượt (&lt; {nguongDiemDat})
          </p>
          <p className="mt-1 text-xl font-bold text-emerald-950">
            {renderTyLe(chiSo.tyLeDat)}{" "}
            <span className="text-xs font-medium text-rose-600">
              / {renderTyLe(chiSo.tyLeTruot)}
            </span>
          </p>
          <p className="mt-1 text-[11px] text-emerald-700">
            Đạt: <strong>{chiSo.soSinhVienDat ?? 0}</strong> · Trượt:{" "}
            <strong className="text-rose-700">{chiSo.soSinhVienTruot ?? 0}</strong>
          </p>
        </div>

        <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-3.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-amber-800">
            Chưa có điểm (Ngoài mẫu số)
          </p>
          <p className="mt-1 text-xl font-bold text-amber-950">
            {chiSo.soSinhVienChuaCoDiem ?? 0} SV
          </p>
          <p className="mt-1 text-[11px] text-amber-800">
            Chưa đủ điểm: {chiSo.soSinhVienChuaNhapDuDiem ?? 0} · Chưa công bố:{" "}
            {chiSo.soSinhVienChuaCongBoDiem ?? 0}
          </p>
        </div>
      </div>

      {/* Phân bố khoảng điểm */}
      <div className="mt-5 grid gap-5 lg:grid-cols-12">
        <div className="lg:col-span-6">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Biểu đồ phân bố theo khoảng điểm
            </h4>
            <span className="text-xs text-slate-500">
              Tổng phân bố:{" "}
              <strong className="text-slate-800">
                {chiSo.tongSoTrongPhanBo ?? 0}
              </strong>{" "}
              / {chiSo.soBanGhiDuocTinh ?? 0} bản ghi
            </span>
          </div>

          {!coDuLieuTinh ? (
            <div className="mt-3 flex h-52 flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50/60 p-4 text-center text-xs text-slate-500">
              <BarChart3 size={24} className="mb-1.5 text-slate-400" />
              Chưa có bản ghi điểm hợp lệ trong mẫu số để vẽ biểu đồ phân bố.
            </div>
          ) : (
            <div className="mt-3 h-56 rounded-xl border border-slate-100 bg-slate-50/40 p-2">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={chartData}
                  margin={{ top: 8, right: 8, left: -18, bottom: 0 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#e2e8f0"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 11, fill: "#475569" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: "#475569" }}
                    axisLine={false}
                    tickLine={false}
                    allowDecimals={false}
                  />
                  <RechartsTooltip
                    formatter={(value, _name, props) => [
                      `${value} SV (${renderTyLe(props?.payload?.tyLePhanTram)})`,
                      props?.payload?.tenKhoang || "Số lượng",
                    ]}
                  />
                  <Bar
                    dataKey="soLuong"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={38}
                    isAnimationActive={false}
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={entry.name} fill={entry.color || BUCKET_COLORS[index]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="lg:col-span-6">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-700">
            Bảng chi tiết phân bố khoảng điểm (Không chồng lấn)
          </h4>
          <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
            <table className="w-full text-xs">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="px-3 py-2 text-left font-semibold">Ký hiệu</th>
                  <th className="px-3 py-2 text-left font-semibold">Mô tả khoảng</th>
                  <th className="px-3 py-2 text-right font-semibold">Số SV</th>
                  <th className="px-3 py-2 text-right font-semibold">Tỷ lệ</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {(chiSo.phanBoDiem || []).map((b, idx) => (
                  <tr key={b.maKhoang}>
                    <td className="px-3 py-2 font-mono font-semibold text-slate-800">
                      <span
                        className="mr-2 inline-block h-2.5 w-2.5 rounded-full"
                        style={{
                          backgroundColor: BUCKET_COLORS[idx % BUCKET_COLORS.length],
                        }}
                      />
                      {b.kyHieuKhoang}
                    </td>
                    <td className="px-3 py-2 text-slate-600">{b.tenKhoang}</td>
                    <td className="px-3 py-2 text-right font-semibold text-slate-900">
                      {b.soLuong}
                    </td>
                    <td className="px-3 py-2 text-right text-slate-600">
                      {renderTyLe(b.tyLePhanTram)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="border-t border-slate-200 bg-slate-50 font-semibold text-slate-800">
                <tr>
                  <td colSpan={2} className="px-3 py-2">
                    Tổng số bản ghi hợp lệ trong phân bố
                  </td>
                  <td className="px-3 py-2 text-right">
                    {chiSo.tongSoTrongPhanBo ?? 0}
                  </td>
                  <td className="px-3 py-2 text-right">
                    {coDuLieuTinh ? "100%" : "—"}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Panel thống kê điểm lớp học phần và phân bố điểm (QLSV-75 / TASK-LC-10).
 * Hỗ trợ:
 * - mode="admin": Quản trị viên xem và lọc theo Học kỳ, Khoa, Môn học, Lớp học phần.
 * - mode="lecturer": Giảng viên xem và lọc trong phạm vi các lớp mình phụ trách.
 * - singleClassId != null: Nhúng trực tiếp trong trang Nhập điểm của 1 lớp học phần cụ thể.
 */
export default function GradeAnalyticsPanel({
  mode = "lecturer",
  initialHocKyId = "",
  initialKhoaId = "",
  singleClassId = null,
  reloadTrigger = 0,
}) {
  const isSingleClass = Boolean(singleClassId);

  const [hocKyId, setHocKyId] = useState(initialHocKyId ? String(initialHocKyId) : "");
  const [khoaId, setKhoaId] = useState(initialKhoaId ? String(initialKhoaId) : "");
  const [monHocId, setMonHocId] = useState("");
  const [lopHocPhanId, setLopHocPhanId] = useState("");

  const [report, setReport] = useState(null);
  const [singleClassData, setSingleClassData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showUnpublishedClassIds, setShowUnpublishedClassIds] = useState({});
  const [showSingleClassPreview, setShowSingleClassPreview] = useState(false);

  // Đồng bộ khi bộ lọc học kỳ / khoa ở trang cha thay đổi
  useEffect(() => {
    if (initialHocKyId !== undefined && initialHocKyId !== null) {
      setHocKyId(String(initialHocKyId));
      setMonHocId("");
      setLopHocPhanId("");
    }
  }, [initialHocKyId]);

  useEffect(() => {
    if (initialKhoaId !== undefined && initialKhoaId !== null) {
      setKhoaId(String(initialKhoaId));
      setMonHocId("");
      setLopHocPhanId("");
    }
  }, [initialKhoaId]);

  const fetchAnalytics = async () => {
    setLoading(true);
    setError("");
    try {
      if (isSingleClass) {
        const endpoint =
          mode === "admin"
            ? `/admin/lop-hoc-phan/${singleClassId}/thong-ke-diem`
            : `/giang-vien/lop-hoc-phan/${singleClassId}/thong-ke-diem`;
        const { data } = await api.get(endpoint);
        setSingleClassData(data);
      } else {
        const endpoint =
          mode === "admin" ? "/admin/thong-ke/diem-lop" : "/giang-vien/thong-ke-diem";
        const params = {};
        if (hocKyId) params.hocKyId = hocKyId;
        if (mode === "admin" && khoaId) params.khoaId = khoaId;
        if (monHocId) params.monHocId = monHocId;
        if (lopHocPhanId) params.lopHocPhanId = lopHocPhanId;
        const { data } = await api.get(endpoint, { params });
        setReport(data);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSingleClass, singleClassId, mode, hocKyId, khoaId, monHocId, lopHocPhanId, reloadTrigger]);

  const toggleClassPreview = (id) => {
    setShowUnpublishedClassIds((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  const danhSachMon = useMemo(() => report?.danhSachMon || [], [report]);
  const danhSachLop = useMemo(() => report?.danhSachLop || [], [report]);

  if (isSingleClass) {
    if (loading) {
      return (
        <div className="card mb-5 p-5">
          <Spinner label="Đang tải thống kê và phân bố điểm lớp học phần..." />
        </div>
      );
    }
    if (error) {
      return (
        <div className="card mb-5 p-5">
          <Alert tone="error">Không tải được thống kê điểm lớp: {error}</Alert>
        </div>
      );
    }
    if (!singleClassData) return null;

    const statusBadge =
      STATUS_BADGE_MAP[singleClassData.trangThaiBangDiem] || STATUS_BADGE_MAP.NHAP;

    return (
      <section className="card mb-5 overflow-hidden" aria-label="Thống kê điểm lớp học phần">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <BarChart3 size={18} className="text-indigo-600" />
              <h2 className="text-sm font-semibold text-slate-900">
                Thống kê & phân bố điểm lớp {singleClassData.maLop}
              </h2>
              <Badge tone={statusBadge.tone}>{statusBadge.label}</Badge>
              <Badge tone="amber">Cấu hình tập trung (Chờ chủ dự án chốt)</Badge>
            </div>
            <p className="mt-1 text-xs text-slate-500">
              Mẫu số chỉ tính sinh viên có đăng ký hiệu lực và đã có điểm tổng kết hợp lệ;
              sinh viên chưa có điểm công bố <strong>không bị tính thành 0.0</strong>. Ngưỡng đạt hiện hành:{" "}
              <strong>&ge; 5.0</strong>.
            </p>
          </div>
          <div className="flex items-center gap-2">
            {!singleClassData.daCongBo && singleClassData.thongKeTamTinhChuaCongBo && (
              <button
                type="button"
                onClick={() => setShowSingleClassPreview((v) => !v)}
                className="btn-secondary py-1.5 text-xs"
              >
                <Eye size={14} />
                {showSingleClassPreview
                  ? "Ẩn thống kê tạm tính (chưa công bố)"
                  : "Xem tạm tính theo điểm đang nhập"}
              </button>
            )}
            <button
              type="button"
              onClick={fetchAnalytics}
              className="btn-secondary py-1.5 text-xs"
              title="Làm mới thống kê"
            >
              <RefreshCw size={14} /> Làm mới
            </button>
          </div>
        </div>

        <div className="space-y-4 p-5">
          {singleClassData.daCongBo ? (
            <ChiSoVaPhanBoSection
              chiSo={singleClassData.thongKeCongBo}
              isPreviewUnpublished={false}
              nguongDiemDat={5.0}
            />
          ) : (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs text-slate-700">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Lock size={15} className="text-slate-500" />
                  <span>
                    Bảng điểm lớp <strong>{singleClassData.maLop}</strong> đang ở trạng thái{" "}
                    <strong>{statusBadge.label}</strong>. Thống kê chính thức (đã công bố) hiện có{" "}
                    <strong>0 / {singleClassData.thongKeCongBo?.tongSoDangKyHieuLuc ?? 0}</strong>{" "}
                    bản ghi trong mẫu số ({singleClassData.thongKeCongBo?.soSinhVienChuaCongBoDiem ?? 0} SV chờ công bố).
                  </span>
                </div>
                {singleClassData.thongKeTamTinhChuaCongBo && !showSingleClassPreview && (
                  <button
                    type="button"
                    onClick={() => setShowSingleClassPreview(true)}
                    className="font-semibold text-indigo-600 hover:underline"
                  >
                    Hiển thị bản tạm tính cho giảng viên &rarr;
                  </button>
                )}
              </div>
            </div>
          )}

          {!singleClassData.daCongBo &&
            showSingleClassPreview &&
            singleClassData.thongKeTamTinhChuaCongBo && (
              <ChiSoVaPhanBoSection
                chiSo={singleClassData.thongKeTamTinhChuaCongBo}
                isPreviewUnpublished={true}
                nguongDiemDat={5.0}
              />
            )}
        </div>
      </section>
    );
  }

  // Chế độ tổng hợp nhiều lớp / môn học (Quản trị hoặc Giảng viên)
  const nguongDiemDat = report?.cauHinh?.nguongDat ?? 5.0;

  return (
    <section className="card mb-6 overflow-hidden" aria-label="Thống kê điểm lớp và phân bố">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <BarChart3 size={18} className="text-indigo-600" />
            <h2 className="text-base font-semibold text-slate-900">
              Thống kê điểm lớp học phần & phân bố điểm
            </h2>
            <Badge tone="green">Chỉ tính bảng điểm Đã công bố / Đã khóa</Badge>
            {report?.cauHinh && !report.cauHinh.laQuyCheChinhThuc && (
              <Badge tone="amber">Cấu hình tập trung — Chờ chủ dự án chốt</Badge>
            )}
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Tổng hợp không chứa thông tin danh tính sinh viên (PII). Sinh viên chưa có điểm công bố{" "}
            <strong>không bị tính ngầm thành 0.0</strong> mà tách riêng ngoài mẫu số.
          </p>
        </div>

        {/* Bộ lọc môn học & lớp học phần trong phạm vi quyền */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
            <Filter size={14} />
            <span>Lọc:</span>
          </div>
          <select
            className="input w-auto min-w-[200px] py-1.5 text-xs"
            value={monHocId}
            onChange={(e) => {
              setMonHocId(e.target.value);
              setLopHocPhanId("");
            }}
            aria-label="Lọc theo môn học"
          >
            <option value="">Tất cả môn học ({danhSachMon.length})</option>
            {danhSachMon.map((m) => (
              <option key={m.monHocId} value={m.monHocId}>
                {m.maMon} - {m.tenMon}
              </option>
            ))}
          </select>

          <select
            className="input w-auto min-w-[210px] py-1.5 text-xs"
            value={lopHocPhanId}
            onChange={(e) => setLopHocPhanId(e.target.value)}
            aria-label="Lọc theo lớp học phần"
          >
            <option value="">Tất cả lớp học phần ({danhSachLop.length})</option>
            {danhSachLop.map((l) => (
              <option key={l.lopHocPhanId} value={l.lopHocPhanId}>
                {l.maLop} · {l.maMon} ({l.daCongBo ? "Đã công bố" : l.trangThaiBangDiem})
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={fetchAnalytics}
            className="btn-secondary py-1.5 text-xs"
            title="Tải lại dữ liệu thống kê"
          >
            <RefreshCw size={14} /> Làm mới
          </button>
        </div>
      </div>

      {error ? (
        <div className="p-5">
          <Alert tone="error">Không thể tải dữ liệu thống kê điểm: {error}</Alert>
        </div>
      ) : loading ? (
        <Spinner label="Đang tổng hợp thống kê điểm và phân bố..." />
      ) : !report || report.tongSoLopTrongPhamVi === 0 ? (
        <div className="p-6">
          <EmptyState
            icon={Layers}
            title="Không có lớp học phần nào trong phạm vi lọc"
            description="Thử chọn lại học kỳ, môn học hoặc lớp học phần khác để xem thống kê."
          />
        </div>
      ) : (
        <div className="space-y-6 p-5">
          {/* Hộp giải thích định nghĩa mẫu số & quy tắc tạm thời */}
          <div className="rounded-xl border border-indigo-100 bg-indigo-50/50 px-4 py-3 text-xs text-slate-700">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2 font-semibold text-indigo-950">
                <Info size={15} className="text-indigo-600" />
                <span>
                  Phạm vi hiện tại: {report.tongSoLopTrongPhamVi} lớp (
                  <span className="text-emerald-700">
                    {report.soLopDaCongBo} lớp đã công bố
                  </span>{" "}
                  ·{" "}
                  <span className="text-amber-700">
                    {report.soLopChuaCongBo} lớp chưa công bố
                  </span>
                  ) · Ngưỡng đạt hiện hành: &ge; {nguongDiemDat}
                </span>
              </div>
              <span className="text-[11px] text-slate-500">
                Chính sách điểm thiếu:{" "}
                <code className="rounded bg-white px-1.5 py-0.5 font-mono text-indigo-700">
                  {report.cauHinh?.chinhSachDiemThieu}
                </code>
              </span>
            </div>
            <p className="mt-1.5 leading-relaxed text-slate-600">
              {report.cauHinh?.ghiChuCanChot}
            </p>
          </div>

          {/* Tổng hợp chính thức trên các lớp ĐÃ CÔNG BỐ */}
          <div>
            <h3 className="mb-2.5 flex items-center gap-2 text-sm font-semibold text-slate-800">
              <FileCheck2 size={16} className="text-emerald-600" />
              Tổng hợp chính thức (Chỉ tính trên {report.soLopDaCongBo} lớp đã công bố / khóa bảng điểm)
            </h3>
            <ChiSoVaPhanBoSection
              chiSo={report.tongHopCongBo}
              isPreviewUnpublished={false}
              nguongDiemDat={nguongDiemDat}
            />
          </div>

          {/* Bảng thống kê theo môn học (khi có từ 1 môn trở lên) */}
          {(report.theoMon || []).length > 0 && (
            <div>
              <h3 className="mb-2.5 text-sm font-semibold text-slate-800">
                Thống kê tổng hợp theo môn học (Chỉ tính lớp đã công bố)
              </h3>
              <div className="overflow-x-auto rounded-xl border border-slate-200">
                <table className="w-full min-w-[780px] text-xs">
                  <thead className="bg-slate-50 text-slate-600">
                    <tr>
                      <th className="px-3.5 py-2.5 text-left font-semibold">Môn học</th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">
                        Số lớp (Công bố / Tổng)
                      </th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">
                        Mẫu số (Có điểm / Tổng ĐK)
                      </th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">
                        Chưa có điểm công bố
                      </th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">Điểm TB</th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">Min – Max</th>
                      <th className="px-3.5 py-2.5 text-center font-semibold">
                        Tỷ lệ Đạt / Trượt
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {report.theoMon.map((m) => {
                      const tk = m.thongKeCongBo;
                      return (
                        <tr key={m.monHocId} className="hover:bg-slate-50/70">
                          <td className="px-3.5 py-2.5">
                            <p className="font-semibold text-slate-900">{m.tenMon}</p>
                            <p className="text-[11px] text-slate-500">
                              {m.maMon} · {m.soTinChi} TC
                              {m.tenKhoa ? ` · ${m.tenKhoa}` : ""}
                            </p>
                          </td>
                          <td className="px-3.5 py-2.5 text-center">
                            <span className="font-semibold text-emerald-700">
                              {m.soLopDaCongBo}
                            </span>{" "}
                            / {m.tongSoLop} lớp
                          </td>
                          <td className="px-3.5 py-2.5 text-center font-semibold text-slate-800">
                            {tk.soBanGhiDuocTinh} / {tk.tongSoDangKyHieuLuc}
                          </td>
                          <td className="px-3.5 py-2.5 text-center text-amber-700">
                            {tk.soSinhVienChuaCoDiem} SV
                          </td>
                          <td className="px-3.5 py-2.5 text-center font-bold text-indigo-700">
                            {renderDiem(tk.diemTrungBinh)}
                          </td>
                          <td className="px-3.5 py-2.5 text-center text-slate-700">
                            {renderDiem(tk.diemThapNhat)} – {renderDiem(tk.diemCaoNhat)}
                          </td>
                          <td className="px-3.5 py-2.5 text-center">
                            {tk.soBanGhiDuocTinh > 0 ? (
                              <span>
                                <strong className="text-emerald-700">
                                  {renderTyLe(tk.tyLeDat)}
                                </strong>{" "}
                                ({tk.soSinhVienDat}) /{" "}
                                <span className="text-rose-600">
                                  {renderTyLe(tk.tyLeTruot)}
                                </span>{" "}
                                ({tk.soSinhVienTruot})
                              </span>
                            ) : (
                              <span className="text-slate-400">Chưa có điểm công bố</span>
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

          {/* Bảng chi tiết theo từng lớp học phần */}
          <div>
            <h3 className="mb-2.5 text-sm font-semibold text-slate-800">
              Chi tiết theo từng lớp học phần ({report.theoLop?.length || 0} lớp)
            </h3>
            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full min-w-[920px] text-xs">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="px-3.5 py-2.5 text-left font-semibold">Lớp học phần</th>
                    <th className="px-3.5 py-2.5 text-left font-semibold">Trạng thái bảng điểm</th>
                    <th className="px-3.5 py-2.5 text-center font-semibold">
                      Mẫu số công bố (Có điểm / ĐK)
                    </th>
                    <th className="px-3.5 py-2.5 text-center font-semibold">Chưa có điểm</th>
                    <th className="px-3.5 py-2.5 text-center font-semibold">Điểm TB</th>
                    <th className="px-3.5 py-2.5 text-center font-semibold">Min – Max</th>
                    <th className="px-3.5 py-2.5 text-center font-semibold">Đạt / Trượt</th>
                    <th className="px-3.5 py-2.5 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {(report.theoLop || []).map((lop) => {
                    const badge =
                      STATUS_BADGE_MAP[lop.trangThaiBangDiem] || STATUS_BADGE_MAP.NHAP;
                    const tk = lop.thongKeCongBo;
                    const previewOpen = Boolean(showUnpublishedClassIds[lop.lopHocPhanId]);

                    return (
                      <FragmentRow
                        key={lop.lopHocPhanId}
                        lop={lop}
                        badge={badge}
                        tk={tk}
                        previewOpen={previewOpen}
                        onTogglePreview={() => toggleClassPreview(lop.lopHocPhanId)}
                        nguongDiemDat={nguongDiemDat}
                      />
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function FragmentRow({
  lop,
  badge,
  tk,
  previewOpen,
  onTogglePreview,
  nguongDiemDat,
}) {
  return (
    <>
      <tr className="hover:bg-slate-50/70">
        <td className="px-3.5 py-3">
          <div className="font-semibold text-slate-900">
            {lop.maLop} · {lop.tenMon}
          </div>
          <div className="text-[11px] text-slate-500">
            {lop.maMon} ({lop.soTinChi} TC)
            {lop.tenGiangVien ? ` · GV: ${lop.tenGiangVien}` : ""}
            {lop.tenHocKy ? ` · ${lop.tenHocKy}` : ""}
          </div>
        </td>
        <td className="px-3.5 py-3">
          <Badge tone={badge.tone}>{badge.label}</Badge>
        </td>
        <td className="px-3.5 py-3 text-center font-semibold text-slate-800">
          {tk.soBanGhiDuocTinh} / {tk.tongSoDangKyHieuLuc}
        </td>
        <td className="px-3.5 py-3 text-center text-amber-700">
          {tk.soSinhVienChuaCoDiem} SV
        </td>
        <td className="px-3.5 py-3 text-center font-bold text-indigo-700">
          {lop.daCongBo ? renderDiem(tk.diemTrungBinh) : "—"}
        </td>
        <td className="px-3.5 py-3 text-center text-slate-700">
          {lop.daCongBo
            ? `${renderDiem(tk.diemThapNhat)} – ${renderDiem(tk.diemCaoNhat)}`
            : "—"}
        </td>
        <td className="px-3.5 py-3 text-center">
          {lop.daCongBo && tk.soBanGhiDuocTinh > 0 ? (
            <span>
              <strong className="text-emerald-700">{renderTyLe(tk.tyLeDat)}</strong> (
              {tk.soSinhVienDat}) /{" "}
              <span className="text-rose-600">{renderTyLe(tk.tyLeTruot)}</span> (
              {tk.soSinhVienTruot})
            </span>
          ) : (
            <span className="text-slate-400">Chưa công bố</span>
          )}
        </td>
        <td className="px-3.5 py-3 text-right">
          <div className="flex flex-wrap items-center justify-end gap-1.5">
            {!lop.daCongBo && lop.thongKeTamTinhChuaCongBo && (
              <button
                type="button"
                onClick={onTogglePreview}
                className="btn-secondary px-2.5 py-1 text-[11px]"
              >
                <Eye size={13} />
                {previewOpen ? "Ẩn tạm tính" : "Xem tạm tính"}
              </button>
            )}
            {lop.coQuyenDrillDownSinhVien && (
              <Link
                to={lop.duongDanDrillDown || `/lop-cua-toi/${lop.lopHocPhanId}/nhap-diem`}
                className="btn-secondary px-2.5 py-1 text-[11px]"
              >
                Bảng điểm lớp
              </Link>
            )}
          </div>
        </td>
      </tr>
      {!lop.daCongBo && previewOpen && lop.thongKeTamTinhChuaCongBo && (
        <tr className="bg-amber-50/30">
          <td colSpan={8} className="p-4">
            <ChiSoVaPhanBoSection
              chiSo={lop.thongKeTamTinhChuaCongBo}
              isPreviewUnpublished={true}
              nguongDiemDat={nguongDiemDat}
            />
          </td>
        </tr>
      )}
    </>
  );
}
