import { useEffect, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Search,
  Eye,
  X,
  ShieldAlert,
  Clock,
  BookOpen,
  CalendarCheck,
  TrendingDown,
  GraduationCap,
  Info,
  Filter,
  Check,
  FileText,
  SlidersHorizontal,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";

const SEVERITY_LABEL = {
  THAP: "Thấp",
  TRUNG_BINH: "Trung bình",
  CANH_BAO: "Cảnh báo",
  CAO: "Cao",
  NGHIEM_TRONG: "Nghiêm trọng",
};

const SEVERITY_COLOR = {
  THAP: "bg-blue-50 text-blue-700 border-blue-200",
  TRUNG_BINH: "bg-amber-50 text-amber-700 border-amber-200",
  CANH_BAO: "bg-orange-50 text-orange-700 border-orange-200",
  CAO: "bg-rose-50 text-rose-700 border-rose-200",
  NGHIEM_TRONG: "bg-red-100 text-red-800 border-red-300 font-semibold",
};

const RULE_TYPE_ICONS = {
  DIEM_QUA_TRINH: BookOpen,
  DIEM_DANH: CalendarCheck,
  GPA_GIAM: TrendingDown,
  TIEN_DO_TIN_CHI: GraduationCap,
};

const STATUS_LABEL = {
  DANG_CANH_BAO: "Đang cảnh báo",
  DANG_XU_LY: "Đang xử lý",
  DA_XU_LY: "Đã xử lý",
  DA_GIAI_TOA: "Đã giải tỏa",
  BO_QUA: "Bỏ qua / Miễn trừ",
};

const STATUS_COLOR = {
  DANG_CANH_BAO: "bg-red-50 text-red-700 border-red-200",
  DANG_XU_LY: "bg-yellow-50 text-yellow-700 border-yellow-200",
  DA_XU_LY: "bg-green-50 text-green-700 border-green-200",
  DA_GIAI_TOA: "bg-teal-50 text-teal-700 border-teal-200",
  BO_QUA: "bg-slate-100 text-slate-700 border-slate-200",
};

function formatDate(val) {
  if (!val) return "—";
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("vi-VN");
}

export default function AdminEarlyWarningPage() {
  const [tab, setTab] = useState("alerts"); // 'alerts' | 'config'
  const [statusFilter, setStatusFilter] = useState("");
  const [ruleFilter, setRuleFilter] = useState("");
  const [severityFilter, setSeverityFilter] = useState("");
  const [keyword, setKeyword] = useState("");
  const [pageNumber, setPageNumber] = useState(0);

  const [alertsPage, setAlertsPage] = useState(null);
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [selectedAlert, setSelectedAlert] = useState(null);
  const [actionStatus, setActionStatus] = useState("DANG_XU_LY");
  const [actionNote, setActionNote] = useState("");
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    fetchAlerts();
    fetchConfig();
  }, [pageNumber, statusFilter, ruleFilter, severityFilter]);

  const fetchAlerts = async () => {
    try {
      setLoading(true);
      setError("");
      const params = {
        page: pageNumber,
        size: 15,
      };
      if (statusFilter) params.trangThai = statusFilter;
      if (ruleFilter) params.loaiLuat = ruleFilter;
      if (severityFilter) params.mucDo = severityFilter;
      if (keyword.trim()) params.keyword = keyword.trim();

      const res = await api.get("/api/admin/canh-bao-hoc-tap", { params });
      setAlertsPage(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const fetchConfig = async () => {
    try {
      const res = await api.get("/api/admin/canh-bao-hoc-tap/cau-hinh");
      setConfig(res.data);
    } catch (err) {
      console.error("Lỗi lấy cấu hình:", err);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPageNumber(0);
    fetchAlerts();
  };

  const handleScan = async () => {
    if (!window.confirm("Kích hoạt quét toàn trường để đánh giá và cập nhật cảnh báo sớm?")) return;
    try {
      setScanning(true);
      setError("");
      setSuccess("");
      const res = await api.post("/api/admin/canh-bao-hoc-tap/quet-du-lieu");
      setSuccess(res.data.thongBao || "Quét hoàn tất thành công!");
      fetchAlerts();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setScanning(false);
    }
  };

  const handleUpdateStatus = async (e) => {
    e.preventDefault();
    if (!selectedAlert) return;
    try {
      setUpdating(true);
      const res = await api.patch(`/api/admin/canh-bao-hoc-tap/${selectedAlert.id}/xu-ly`, {
        trangThai: actionStatus,
        ghiChu: actionNote,
      });
      setSelectedAlert(res.data);
      setSuccess(`Đã cập nhật trạng thái cảnh báo #${selectedAlert.id} sang ${STATUS_LABEL[actionStatus] || actionStatus}`);
      fetchAlerts();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setUpdating(false);
    }
  };

  const parseEvidence = (jsonStr) => {
    try {
      return JSON.parse(jsonStr || "{}");
    } catch {
      return {};
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 py-6">
      <PageHeader
        title="Cảnh báo học tập sớm (Early Warning)"
        description="Hệ thống giám sát và phát hiện sớm nguy cơ học tập theo các quy tắc minh bạch, giải thích được. Không sử dụng mô hình dự đoán hộp đen."
        actions={
          <div className="flex items-center gap-2">
            <button
              onClick={handleScan}
              disabled={scanning}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium text-sm hover:bg-indigo-700 disabled:opacity-50 transition shadow-sm"
            >
              <RefreshCw className={`w-4 h-4 ${scanning ? "animate-spin" : ""}`} />
              {scanning ? "Đang quét dữ liệu..." : "Quét cảnh báo toàn trường"}
            </button>
          </div>
        }
      />

      {/* Safety & Regulation Notice Banner */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-amber-900 flex items-start gap-3">
        <Info className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="text-sm space-y-1">
          <p className="font-semibold text-amber-800">
            Nguyên tắc minh bạch & Chính sách an toàn học vụ (QLSV-97 / TASK-OP-10):
          </p>
          <p className="text-amber-700 leading-relaxed">
            Hệ thống cảnh báo sớm hoạt động hoàn toàn theo quy tắc tính toán có thể giải thích. Cảnh báo{" "}
            <strong>KHÔNG tự ý thay đổi trạng thái học vụ</strong>, <strong>KHÔNG khóa tài khoản sinh viên</strong> và{" "}
            <strong>KHÔNG sửa điểm số hoặc hủy đăng ký học phần</strong>. Các ngưỡng kích hoạt hiện đang áp dụng cấu hình
            tập trung dự kiến và đang chờ Hội đồng Đào tạo phê duyệt chính thức.
          </p>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError("")} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess("")} />}

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => setTab("alerts")}
          className={`px-5 py-3 font-medium text-sm border-b-2 flex items-center gap-2 transition ${
            tab === "alerts"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          <ShieldAlert className="w-4 h-4" />
          Danh sách cảnh báo
          {alertsPage && alertsPage.totalElements > 0 && (
            <span className="ml-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
              {alertsPage.totalElements}
            </span>
          )}
        </button>
        <button
          onClick={() => setTab("config")}
          className={`px-5 py-3 font-medium text-sm border-b-2 flex items-center gap-2 transition ${
            tab === "config"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          <SlidersHorizontal className="w-4 h-4" />
          Quy tắc minh bạch & Ngưỡng chờ chốt
          <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">
            Chờ duyệt
          </span>
        </button>
      </div>

      {tab === "alerts" ? (
        <div className="space-y-4">
          {/* Filters Bar */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm space-y-3">
            <form onSubmit={handleSearchSubmit} className="flex flex-wrap gap-3 items-center">
              <div className="relative flex-1 min-w-[220px]">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Tìm theo mã SV, họ tên, tên luật hoặc lý do..."
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <select
                value={ruleFilter}
                onChange={(e) => {
                  setRuleFilter(e.target.value);
                  setPageNumber(0);
                }}
                className="text-sm border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Tất cả nhóm luật</option>
                <option value="DIEM_QUA_TRINH">Điểm quá trình thấp</option>
                <option value="DIEM_DANH">Vắng chuyên cần quá ngưỡng</option>
                <option value="GPA_GIAM">GPA giảm liên tiếp</option>
                <option value="TIEN_DO_TIN_CHI">Chậm tiến độ tín chỉ</option>
              </select>

              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPageNumber(0);
                }}
                className="text-sm border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="DANG_CANH_BAO">Đang cảnh báo</option>
                <option value="DANG_XU_LY">Đang xử lý</option>
                <option value="DA_XU_LY">Đã xử lý</option>
                <option value="DA_GIAI_TOA">Đã giải tỏa (tự động)</option>
                <option value="BO_QUA">Bỏ qua / Miễn trừ</option>
              </select>

              <select
                value={severityFilter}
                onChange={(e) => {
                  setSeverityFilter(e.target.value);
                  setPageNumber(0);
                }}
                className="text-sm border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Tất cả mức độ</option>
                <option value="NGHIEM_TRONG">Nghiêm trọng</option>
                <option value="CAO">Cao</option>
                <option value="CANH_BAO">Cảnh báo</option>
                <option value="TRUNG_BINH">Trung bình</option>
                <option value="THAP">Thấp</option>
              </select>

              <button
                type="submit"
                className="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm font-medium hover:bg-slate-900 transition flex items-center gap-1.5"
              >
                <Filter className="w-4 h-4" />
                Lọc
              </button>
            </form>
          </div>

          {/* Alerts Table */}
          {loading ? (
            <div className="py-16 text-center">
              <Spinner />
              <p className="mt-2 text-sm text-slate-500">Đang tải danh sách cảnh báo...</p>
            </div>
          ) : !alertsPage || alertsPage.content.length === 0 ? (
            <EmptyState
              icon={CheckCircle2}
              title="Không có cảnh báo học tập phù hợp"
              description="Hiện tại không có sinh viên nào vi phạm các ngưỡng cảnh báo sớm theo bộ lọc hiện tại."
            />
          ) : (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 text-xs uppercase font-semibold">
                      <th className="py-3 px-4">Sinh viên</th>
                      <th className="py-3 px-4">Nhóm luật</th>
                      <th className="py-3 px-4">Lý do kích hoạt</th>
                      <th className="py-3 px-4">Mức độ</th>
                      <th className="py-3 px-4">Trạng thái</th>
                      <th className="py-3 px-4">Quyền xem SV</th>
                      <th className="py-3 px-4">Ngày tạo</th>
                      <th className="py-3 px-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {alertsPage.content.map((alert) => {
                      const Icon = RULE_TYPE_ICONS[alert.loaiLuat] || AlertTriangle;
                      return (
                        <tr key={alert.id} className="hover:bg-slate-50/80 transition">
                          <td className="py-3 px-4">
                            <div className="font-medium text-slate-900">{alert.hoTenSinhVien}</div>
                            <div className="text-xs text-slate-500 font-mono">
                              {alert.maSinhVien}
                              {alert.lopHanhChinh ? ` • ${alert.lopHanhChinh}` : ""}
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex items-center gap-1.5">
                              <Icon className="w-4 h-4 text-indigo-500 shrink-0" />
                              <span className="font-medium text-slate-800 text-xs">{alert.tenLuat}</span>
                            </div>
                            {alert.tenMonHoc && (
                              <div className="text-xs text-slate-500 truncate max-w-[180px] mt-0.5">
                                {alert.tenMonHoc} ({alert.maLopHocPhan})
                              </div>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <p className="text-slate-700 text-xs line-clamp-2 max-w-[280px]">
                              {alert.lyDo}
                            </p>
                          </td>
                          <td className="py-3 px-4">
                            <span
                              className={`inline-block px-2 py-0.5 rounded text-xs border ${
                                SEVERITY_COLOR[alert.mucDo] || "bg-slate-100 text-slate-700 border-slate-200"
                              }`}
                            >
                              {alert.tenMucDo}
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            <span
                              className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                                STATUS_COLOR[alert.trangThai] || "bg-slate-100 text-slate-700 border-slate-200"
                              }`}
                            >
                              {alert.tenTrangThai}
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            {alert.choPhepSinhVienXem ? (
                              <span className="text-xs font-medium text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                                Cho phép xem
                              </span>
                            ) : (
                              <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                                Ẩn với SV
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-4 text-xs text-slate-500 whitespace-nowrap">
                            {formatDate(alert.ngayTao)}
                          </td>
                          <td className="py-3 px-4 text-right">
                            <button
                              onClick={() => {
                                setSelectedAlert(alert);
                                setActionStatus(alert.trangThai === "DANG_CANH_BAO" ? "DANG_XU_LY" : alert.trangThai);
                                setActionNote(alert.ghiChuXuLy || "");
                              }}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
                            >
                              <Eye className="w-3.5 h-3.5" />
                              Chi tiết
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {alertsPage.totalPages > 1 && (
                <div className="p-4 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
                  <div>
                    Hiển thị trang {alertsPage.number + 1} / {alertsPage.totalPages} ({alertsPage.totalElements} bản ghi)
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
                      disabled={alertsPage.first}
                      className="px-3 py-1.5 rounded border border-slate-300 disabled:opacity-40 hover:bg-slate-50"
                    >
                      Trước
                    </button>
                    <button
                      onClick={() => setPageNumber((p) => Math.min(alertsPage.totalPages - 1, p + 1))}
                      disabled={alertsPage.last}
                      className="px-3 py-1.5 rounded border border-slate-300 disabled:opacity-40 hover:bg-slate-50"
                    >
                      Sau
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* Configuration Tab */
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm space-y-4">
            <h3 className="text-base font-semibold text-slate-900 flex items-center gap-2">
              <SlidersHorizontal className="w-5 h-5 text-indigo-600" />
              Cấu hình tập trung 4 Nhóm Luật Cảnh Báo Sớm
            </h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Mỗi luật dưới đây được tính toán minh bạch trực tiếp từ cơ sở dữ liệu quan hệ, cung cấp công thức dẫn chứng
              cụ thể và giải trình rõ ràng cho cố vấn học tập và người học.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              {config?.danhSachLuat?.map((r) => {
                const Icon = RULE_TYPE_ICONS[r.loaiLuat] || FileText;
                return (
                  <div
                    key={r.maLuat}
                    className="border border-slate-200 rounded-xl p-5 space-y-3 bg-slate-50/50 hover:bg-slate-50 transition"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <div className="p-2 rounded-lg bg-indigo-100 text-indigo-700">
                          <Icon className="w-5 h-5" />
                        </div>
                        <div>
                          <h4 className="font-semibold text-slate-900 text-sm">{r.tenLuat}</h4>
                          <span className="text-xs font-mono text-slate-500">{r.maLuat}</span>
                        </div>
                      </div>
                      <span className="px-2 py-0.5 rounded text-xs border font-medium bg-amber-50 text-amber-800 border-amber-200">
                        Chờ xác nhận
                      </span>
                    </div>

                    <p className="text-xs text-slate-600 leading-relaxed">{r.moTa}</p>

                    <div className="space-y-1.5 text-xs">
                      <div className="flex items-start gap-1.5">
                        <span className="font-medium text-slate-700 w-28 shrink-0">Công thức kích hoạt:</span>
                        <code className="bg-white px-2 py-0.5 rounded border border-slate-200 text-indigo-700 font-mono">
                          {r.congThucTinh}
                        </code>
                      </div>
                      <div className="flex items-start gap-1.5">
                        <span className="font-medium text-slate-700 w-28 shrink-0">Nguồn dẫn chứng:</span>
                        <span className="text-slate-600">{r.nguonDuLieu}</span>
                      </div>
                      <div className="flex items-start gap-1.5">
                        <span className="font-medium text-slate-700 w-28 shrink-0">Quyền xem SV:</span>
                        <span
                          className={`font-medium ${
                            r.choPhepSinhVienXem ? "text-emerald-600" : "text-slate-500"
                          }`}
                        >
                          {r.choPhepSinhVienXem ? "Cho phép sinh viên xem" : "Ẩn với sinh viên (chỉ Cố vấn/Admin)"}
                        </span>
                      </div>
                    </div>

                    <div className="text-xs bg-amber-50 border border-amber-200/80 rounded-lg p-2.5 text-amber-800 leading-relaxed">
                      <strong>Cần Hội đồng Đào tạo xác nhận:</strong> {r.ghiChuQuyChe}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Pending Confirmations Checklist */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm space-y-3">
            <h3 className="text-base font-semibold text-slate-900 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
              Danh mục các tham số cần Chủ dự án & Hội đồng Đào tạo phê duyệt
            </h3>
            <ul className="space-y-2 text-sm text-slate-700">
              {config?.giaTriChoXacNhan?.map((item, idx) => (
                <li key={idx} className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
                    {idx + 1}
                  </span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {/* Alert Detail & Action Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl border border-slate-200 max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <div className="flex items-center gap-2.5">
                <ShieldAlert className="w-5 h-5 text-indigo-600" />
                <div>
                  <h3 className="font-semibold text-slate-900 text-base">
                    Chi tiết cảnh báo #{selectedAlert.id}
                  </h3>
                  <div className="text-xs text-slate-500 font-mono">{selectedAlert.maLuat}</div>
                </div>
              </div>
              <button
                onClick={() => setSelectedAlert(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200/60 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-5 overflow-y-auto flex-1">
              {/* Student & Course Overview */}
              <div className="grid grid-cols-2 gap-3 p-3.5 bg-slate-50 rounded-xl border border-slate-200 text-xs">
                <div>
                  <span className="text-slate-500">Sinh viên:</span>
                  <div className="font-semibold text-slate-900 text-sm mt-0.5">
                    {selectedAlert.hoTenSinhVien} ({selectedAlert.maSinhVien})
                  </div>
                  {selectedAlert.lopHanhChinh && (
                    <div className="text-slate-600 mt-0.5">Lớp HC: {selectedAlert.lopHanhChinh}</div>
                  )}
                </div>
                <div>
                  <span className="text-slate-500">Môn học / Lớp HP:</span>
                  <div className="font-semibold text-slate-900 text-sm mt-0.5">
                    {selectedAlert.tenMonHoc || "Chương trình toàn khóa"}
                  </div>
                  {selectedAlert.maLopHocPhan && (
                    <div className="text-slate-600 mt-0.5 font-mono">Lớp: {selectedAlert.maLopHocPhan}</div>
                  )}
                </div>
              </div>

              {/* Status & Severity */}
              <div className="flex flex-wrap gap-2 items-center text-xs">
                <span className="font-medium text-slate-600">Mức độ:</span>
                <span className={`px-2.5 py-1 rounded border ${SEVERITY_COLOR[selectedAlert.mucDo]}`}>
                  {selectedAlert.tenMucDo}
                </span>

                <span className="font-medium text-slate-600 ml-3">Trạng thái:</span>
                <span className={`px-2.5 py-1 rounded-full border ${STATUS_COLOR[selectedAlert.trangThai]}`}>
                  {selectedAlert.tenTrangThai}
                </span>

                <span className="font-medium text-slate-600 ml-3">Hiển thị SV:</span>
                <span className="font-medium text-slate-700">
                  {selectedAlert.choPhepSinhVienXem ? "Cho phép xem" : "Ẩn với sinh viên"}
                </span>
              </div>

              {/* Reason */}
              <div className="space-y-1">
                <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Lý do kích hoạt cảnh báo:
                </h4>
                <div className="p-3 bg-red-50/70 border border-red-200 rounded-lg text-sm text-red-900 leading-relaxed font-medium">
                  {selectedAlert.lyDo}
                </div>
              </div>

              {/* Explainability Calculation Breakdown */}
              <div className="space-y-1">
                <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Công thức & Cách tính minh bạch:
                </h4>
                <div className="p-3.5 bg-slate-900 text-slate-100 rounded-lg text-xs leading-relaxed font-mono">
                  {selectedAlert.congThucGiaiThich}
                </div>
              </div>

              {/* Evidence Snapshot */}
              <div className="space-y-1">
                <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Dữ liệu dẫn chứng thực tế (Snapshot):
                </h4>
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-xs overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <tbody>
                      {Object.entries(parseEvidence(selectedAlert.duLieuDanChung)).map(([key, val]) => (
                        <tr key={key} className="border-b border-slate-200/60 last:border-0">
                          <td className="py-1 pr-3 font-mono text-slate-500 font-semibold w-40">{key}</td>
                          <td className="py-1 font-mono text-slate-900">{String(val)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Resolution History / Reason */}
              {(selectedAlert.nguoiXuLy || selectedAlert.nguyenNhanGiaiToa) && (
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1.5">
                  <div className="font-semibold text-slate-800">Lịch sử xử lý / Giải tỏa:</div>
                  {selectedAlert.nguoiXuLy && (
                    <div className="text-slate-600">
                      Người xử lý: <strong>{selectedAlert.nguoiXuLy}</strong> vào lúc {formatDate(selectedAlert.ngayXuLy)}
                    </div>
                  )}
                  {selectedAlert.ghiChuXuLy && (
                    <div className="text-slate-700 italic">Ghi chú: "{selectedAlert.ghiChuXuLy}"</div>
                  )}
                  {selectedAlert.nguyenNhanGiaiToa && (
                    <div className="text-teal-700">
                      <strong>Tự động giải tỏa:</strong> {selectedAlert.nguyenNhanGiaiToa}
                    </div>
                  )}
                </div>
              )}

              {/* Status Update Action Form */}
              <form onSubmit={handleUpdateStatus} className="border-t border-slate-200 pt-4 space-y-3">
                <h4 className="text-xs font-semibold text-slate-800 uppercase tracking-wider">
                  Cập nhật trạng thái xử lý cảnh báo:
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">
                      Chọn trạng thái mới:
                    </label>
                    <select
                      value={actionStatus}
                      onChange={(e) => setActionStatus(e.target.value)}
                      className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    >
                      <option value="DANG_XU_LY">Đang xử lý (đang tư vấn)</option>
                      <option value="DA_XU_LY">Đã xử lý (đã hoàn thành tư vấn)</option>
                      <option value="BO_QUA">Bỏ qua / Miễn trừ</option>
                      <option value="DANG_CANH_BAO">Mở lại cảnh báo</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">
                      Ghi chú xử lý / Tư vấn học vụ:
                    </label>
                    <input
                      type="text"
                      placeholder="Nhập ghi chú can thiệp..."
                      value={actionNote}
                      onChange={(e) => setActionNote(e.target.value)}
                      className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setSelectedAlert(null)}
                    className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 transition"
                  >
                    Đóng
                  </button>
                  <button
                    type="submit"
                    disabled={updating}
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition"
                  >
                    {updating ? "Đang lưu..." : "Lưu trạng thái"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
