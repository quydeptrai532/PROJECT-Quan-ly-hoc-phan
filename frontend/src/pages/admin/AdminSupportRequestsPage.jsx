import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  Calendar,
  CheckCircle2,
  Clock,
  Download,
  Eye,
  FileQuestion,
  History,
  LifeBuoy,
  Lock,
  MessageSquare,
  Paperclip,
  Search,
  Send,
  ShieldAlert,
  SlidersHorizontal,
  UserCheck,
  Users,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem } from "../../lib/format";
import {
  downloadAttachmentFile,
  formatFileSize,
  STATUS_TONE,
} from "../student/SupportRequestsPage";

const PAGE_SIZE = 15;

export const PRIORITY_TONE = {
  THAP: "slate",
  TRUNG_BINH: "sky",
  CAO: "amber",
  KHAN_CAP: "red",
};

export const SLA_TONE = {
  ON_TIME: "green",
  NEARING_DUE: "amber",
  OVERDUE: "red",
  PAUSED: "sky",
  RESOLVED_ON_TIME: "green",
  RESOLVED_OVERDUE: "red",
};

export default function AdminSupportRequestsPage() {
  const [catalog, setCatalog] = useState(null);
  const [queueSummary, setQueueSummary] = useState(null);
  const [handlers, setHandlers] = useState([]);

  // Queue Scope & Filters
  const [scope, setScope] = useState("ALL");
  const [filters, setFilters] = useState({
    trangThai: "",
    loaiYeuCau: "",
    mucUuTien: "",
    keyword: "",
  });
  const [appliedFilters, setAppliedFilters] = useState({
    trangThai: "",
    loaiYeuCau: "",
    mucUuTien: "",
    keyword: "",
  });

  const [pageNumber, setPageNumber] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Detail modal state
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [selectedTicket, setSelectedTicket] = useState(null);

  // Status update form state
  const [nextStatus, setNextStatus] = useState("");
  const [statusNote, setStatusNote] = useState("");
  const [statusUpdating, setStatusUpdating] = useState(false);

  // Reply form state
  const [replyText, setReplyText] = useState("");
  const [isInternalNote, setIsInternalNote] = useState(false);
  const [replySubmitting, setReplySubmitting] = useState(false);

  // Sub-action modals
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignUserId, setAssignUserId] = useState("");
  const [assignNote, setAssignNote] = useState("");
  const [assignSubmitting, setAssignSubmitting] = useState(false);

  const [triageOpen, setTriageOpen] = useState(false);
  const [triagePriority, setTriagePriority] = useState("");
  const [triageCategory, setTriageCategory] = useState("");
  const [triageDeadline, setTriageDeadline] = useState("");
  const [triageNote, setTriageNote] = useState("");
  const [triageSubmitting, setTriageSubmitting] = useState(false);

  const [evidenceOpen, setEvidenceOpen] = useState(false);
  const [evidenceNote, setEvidenceNote] = useState("");
  const [evidenceDeadline, setEvidenceDeadline] = useState("");
  const [evidenceSubmitting, setEvidenceSubmitting] = useState(false);

  // Load catalog, summary and handlers
  const loadMetadata = useCallback(async () => {
    try {
      const [catRes, sumRes, hndRes] = await Promise.all([
        api.get("/yeu-cau-ho-tro/danh-muc"),
        api.get("/admin/yeu-cau-ho-tro/queue/summary"),
        api.get("/admin/yeu-cau-ho-tro/handlers"),
      ]);
      setCatalog(catRes.data);
      setQueueSummary(sumRes.data);
      setHandlers(hndRes.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    loadMetadata();
  }, [loadMetadata]);

  // Load Queue list
  const loadList = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = {
        scope,
        page: pageNumber,
        size: PAGE_SIZE,
      };
      if (appliedFilters.trangThai) params.trangThai = appliedFilters.trangThai;
      if (appliedFilters.loaiYeuCau) params.loaiYeuCau = appliedFilters.loaiYeuCau;
      if (appliedFilters.mucUuTien) params.mucUuTien = appliedFilters.mucUuTien;
      if (appliedFilters.keyword) params.keyword = appliedFilters.keyword;

      const { data } = await api.get("/admin/yeu-cau-ho-tro/queue", { params });
      setPageData(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [scope, pageNumber, appliedFilters]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const statusNameMap = (catalog?.trangThai || []).reduce((acc, cur) => {
    acc[cur.ma] = cur.ten;
    return acc;
  }, {});

  const priorityNameMap = (catalog?.mucUuTien || []).reduce((acc, cur) => {
    acc[cur.ma] = cur.ten;
    return acc;
  }, {
    THAP: "Thấp",
    TRUNG_BINH: "Trung bình",
    CAO: "Cao",
    KHAN_CAP: "Khẩn cấp",
  });

  const openTicketDetail = async (ticketId) => {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError("");
    setReplyText("");
    setIsInternalNote(false);
    setStatusNote("");
    try {
      const { data } = await api.get(`/admin/yeu-cau-ho-tro/${ticketId}`);
      setSelectedTicket(data);
      setNextStatus(data.trangThaiTiepTheoHopLe?.[0] || "");
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setDetailLoading(false);
    }
  };

  const reloadSelectedTicket = async (ticketId) => {
    try {
      const { data } = await api.get(`/admin/yeu-cau-ho-tro/${ticketId}`);
      setSelectedTicket(data);
      setNextStatus(data.trangThaiTiepTheoHopLe?.[0] || "");
      loadList();
      api.get("/admin/yeu-cau-ho-tro/queue/summary").then((res) => setQueueSummary(res.data));
    } catch (err) {
      setDetailError(getErrorMessage(err));
    }
  };

  const handleStatusUpdate = async (event) => {
    event.preventDefault();
    if (!selectedTicket || !nextStatus) return;
    setStatusUpdating(true);
    setDetailError("");
    setSuccessMsg("");
    try {
      const { data } = await api.patch(`/admin/yeu-cau-ho-tro/${selectedTicket.id}/trang-thai`, {
        trangThai: nextStatus,
        ghiChu: statusNote.trim() || null,
      });
      setSelectedTicket(data);
      setNextStatus(data.trangThaiTiepTheoHopLe?.[0] || "");
      setStatusNote("");
      setSuccessMsg(`Đã cập nhật trạng thái phiếu ${data.maPhieu} sang "${data.tenTrangThai}".`);
      loadList();
      api.get("/admin/yeu-cau-ho-tro/queue/summary").then((res) => setQueueSummary(res.data));
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleReplySubmit = async (event) => {
    event.preventDefault();
    if (!selectedTicket) return;
    const trimmed = replyText.trim();
    if (!trimmed) {
      setDetailError("Vui lòng nhập nội dung trao đổi.");
      return;
    }
    setReplySubmitting(true);
    setDetailError("");
    try {
      await api.post(`/yeu-cau-ho-tro/${selectedTicket.id}/trao-doi`, {
        noiDung: trimmed,
        isNoiBo: isInternalNote,
      });
      setReplyText("");
      setIsInternalNote(false);
      reloadSelectedTicket(selectedTicket.id);
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setReplySubmitting(false);
    }
  };

  // Open Assign Modal
  const openAssignModal = () => {
    if (!selectedTicket) return;
    setAssignUserId(selectedTicket.nguoiXuLyId ? String(selectedTicket.nguoiXuLyId) : "");
    setAssignNote("");
    setAssignOpen(true);
  };

  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    setAssignSubmitting(true);
    setDetailError("");
    try {
      await api.patch(`/admin/yeu-cau-ho-tro/${selectedTicket.id}/phan-cong`, {
        handlerUserId: assignUserId ? Number(assignUserId) : null,
        ghiChu: assignNote.trim() || (assignUserId ? "Phân công người xử lý" : "Hủy phân công"),
      });
      setAssignOpen(false);
      setSuccessMsg(`Đã cập nhật phân công xử lý phiếu ${selectedTicket.maPhieu}.`);
      reloadSelectedTicket(selectedTicket.id);
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setAssignSubmitting(false);
    }
  };

  // Open Triage Modal
  const openTriageModal = () => {
    if (!selectedTicket) return;
    setTriagePriority(selectedTicket.mucUuTien || "TRUNG_BINH");
    setTriageCategory(selectedTicket.loaiYeuCau || "");
    setTriageDeadline(selectedTicket.hanXuLy ? selectedTicket.hanXuLy.substring(0, 16) : "");
    setTriageNote("");
    setTriageOpen(true);
  };

  const handleTriageSubmit = async (e) => {
    e.preventDefault();
    if (!triageNote.trim()) {
      setDetailError("Vui lòng nhập lý do điều chỉnh phân loại/ưu tiên/hạn xử lý.");
      return;
    }
    setTriageSubmitting(true);
    setDetailError("");
    try {
      await api.patch(`/admin/yeu-cau-ho-tro/${selectedTicket.id}/triage`, {
        mucUuTien: triagePriority,
        loaiYeuCau: triageCategory,
        hanXuLy: triageDeadline ? triageDeadline + ":00" : null,
        ghiChu: triageNote.trim(),
      });
      setTriageOpen(false);
      setSuccessMsg(`Đã cập nhật phân loại và hạn xử lý phiếu ${selectedTicket.maPhieu}.`);
      reloadSelectedTicket(selectedTicket.id);
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setTriageSubmitting(false);
    }
  };

  // Open Evidence Request Modal
  const openEvidenceModal = () => {
    if (!selectedTicket) return;
    setEvidenceNote("");
    setEvidenceDeadline("");
    setEvidenceOpen(true);
  };

  const handleEvidenceSubmit = async (e) => {
    e.preventDefault();
    if (!evidenceNote.trim()) {
      setDetailError("Vui lòng nhập nội dung minh chứng yêu cầu bổ sung.");
      return;
    }
    setEvidenceSubmitting(true);
    setDetailError("");
    try {
      await api.post(`/admin/yeu-cau-ho-tro/${selectedTicket.id}/yeu-cau-minh-chung`, {
        noiDung: evidenceNote.trim(),
        hanBoSung: evidenceDeadline ? evidenceDeadline + ":00" : null,
      });
      setEvidenceOpen(false);
      setSuccessMsg(`Đã chuyển phiếu ${selectedTicket.maPhieu} sang chờ sinh viên bổ sung minh chứng.`);
      reloadSelectedTicket(selectedTicket.id);
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setEvidenceSubmitting(false);
    }
  };

  const applyFilterSubmit = (event) => {
    event.preventDefault();
    setPageNumber(0);
    setAppliedFilters({ ...filters });
  };

  const resetFilters = () => {
    const empty = { trangThai: "", loaiYeuCau: "", mucUuTien: "", keyword: "" };
    setFilters(empty);
    setAppliedFilters(empty);
    setPageNumber(0);
  };

  const items = pageData?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        icon={LifeBuoy}
        eyebrow="Quản trị học vụ"
        title="Hàng đợi xử lý phiếu hỗ trợ sinh viên"
        description="Quản lý hàng đợi tiếp nhận, phân công cán bộ, điều chỉnh ưu tiên, theo dõi SLA giờ làm việc và xử lý yêu cầu hỗ trợ sinh viên."
      />

      {/* SLA Policy Pending Decision Notice */}
      {queueSummary && !queueSummary.officialSlaEnabled && (
        <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50/80 p-4 text-xs text-amber-900">
          <ShieldAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
          <div className="space-y-1">
            <p className="font-semibold text-amber-950">
              Lưu ý chính sách SLA: {queueSummary.slaNote}
            </p>
            <p className="text-amber-800 leading-relaxed">
              {queueSummary.workingCalendarNote} (Các mốc thời gian và trạng thái SLA hiện được hiển thị để theo dõi và chuẩn hóa quy trình, chưa áp dụng chế tài phạt chính thức).
            </p>
          </div>
        </div>
      )}

      {successMsg && (
        <Alert tone="success" onClose={() => setSuccessMsg("")}>
          {successMsg}
        </Alert>
      )}
      {error && (
        <Alert tone="error" onClose={() => setError("")}>
          {error}
        </Alert>
      )}

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-sm">
          <span className="text-xs font-medium text-slate-500">Tổng phiếu chờ/xử lý</span>
          <p className="mt-1 text-2xl font-bold text-slate-900">{queueSummary?.totalInQueue ?? 0}</p>
        </div>
        <div className="rounded-xl border border-blue-200 bg-blue-50/50 p-3.5 shadow-sm">
          <span className="text-xs font-medium text-blue-700">Được phân công tôi</span>
          <p className="mt-1 text-2xl font-bold text-blue-800">{queueSummary?.myAssignedCount ?? 0}</p>
        </div>
        <div className="rounded-xl border border-amber-200 bg-amber-50/50 p-3.5 shadow-sm">
          <span className="text-xs font-medium text-amber-700">Chưa phân công</span>
          <p className="mt-1 text-2xl font-bold text-amber-800">{queueSummary?.unassignedCount ?? 0}</p>
        </div>
        <div className="rounded-xl border border-rose-200 bg-rose-50/50 p-3.5 shadow-sm">
          <span className="text-xs font-medium text-rose-700">Quá hạn SLA</span>
          <p className="mt-1 text-2xl font-bold text-rose-800">{queueSummary?.overdueCount ?? 0}</p>
        </div>
        <div className="rounded-xl border border-orange-200 bg-orange-50/50 p-3.5 shadow-sm">
          <span className="text-xs font-medium text-orange-700">Sắp đến hạn (&lt;4h)</span>
          <p className="mt-1 text-2xl font-bold text-orange-800">{queueSummary?.nearingDueCount ?? 0}</p>
        </div>
        <div className="rounded-xl border border-purple-200 bg-purple-50/50 p-3.5 shadow-sm">
          <span className="text-xs font-medium text-purple-700">Chờ SV bổ sung</span>
          <p className="mt-1 text-2xl font-bold text-purple-800">{queueSummary?.awaitingStudentCount ?? 0}</p>
        </div>
      </div>

      {/* Scope Navigation Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => { setScope("ALL"); setPageNumber(0); }}
          className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            scope === "ALL"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-600 hover:text-slate-900"
          }`}
        >
          <LifeBuoy size={16} />
          Tất cả hàng đợi
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
            {queueSummary?.totalInQueue ?? 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => { setScope("MY_ASSIGNED"); setPageNumber(0); }}
          className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            scope === "MY_ASSIGNED"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-600 hover:text-slate-900"
          }`}
        >
          <UserCheck size={16} />
          Của tôi
          <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-800">
            {queueSummary?.myAssignedCount ?? 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => { setScope("UNASSIGNED"); setPageNumber(0); }}
          className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            scope === "UNASSIGNED"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-600 hover:text-slate-900"
          }`}
        >
          <Users size={16} />
          Chưa phân công
          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-800">
            {queueSummary?.unassignedCount ?? 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => { setScope("OVERDUE"); setPageNumber(0); }}
          className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            scope === "OVERDUE"
              ? "border-indigo-600 text-indigo-600"
              : "border-transparent text-slate-600 hover:text-slate-900"
          }`}
        >
          <AlertTriangle size={16} />
          Quá hạn SLA
          <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs text-rose-800">
            {queueSummary?.overdueCount ?? 0}
          </span>
        </button>
      </div>

      {/* Filter Form */}
      <form
        onSubmit={applyFilterSubmit}
        className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"
      >
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <label className="text-xs font-medium text-slate-600">
            Loại yêu cầu
            <select
              className="input mt-1 w-full"
              value={filters.loaiYeuCau}
              onChange={(e) => setFilters((prev) => ({ ...prev, loaiYeuCau: e.target.value }))}
            >
              <option value="">Tất cả loại yêu cầu</option>
              {(catalog?.loaiYeuCau || []).map((item) => (
                <option key={item.ma} value={item.ma}>
                  {item.ten}
                </option>
              ))}
            </select>
          </label>

          <label className="text-xs font-medium text-slate-600">
            Mức ưu tiên
            <select
              className="input mt-1 w-full"
              value={filters.mucUuTien}
              onChange={(e) => setFilters((prev) => ({ ...prev, mucUuTien: e.target.value }))}
            >
              <option value="">Tất cả mức ưu tiên</option>
              <option value="THAP">Thấp</option>
              <option value="TRUNG_BINH">Trung bình</option>
              <option value="CAO">Cao</option>
              <option value="KHAN_CAP">Khẩn cấp</option>
            </select>
          </label>

          <label className="text-xs font-medium text-slate-600">
            Trạng thái
            <select
              className="input mt-1 w-full"
              value={filters.trangThai}
              onChange={(e) => setFilters((prev) => ({ ...prev, trangThai: e.target.value }))}
            >
              <option value="">Tất cả trạng thái</option>
              {(catalog?.trangThai || []).map((item) => (
                <option key={item.ma} value={item.ma}>
                  {item.ten}
                </option>
              ))}
            </select>
          </label>

          <label className="text-xs font-medium text-slate-600 sm:col-span-2">
            Tìm theo mã phiếu, tiêu đề, mã SV hoặc họ tên SV
            <div className="mt-1 flex gap-2">
              <input
                type="text"
                className="input flex-1"
                placeholder="Nhập mã phiếu, mã SV, tên SV hoặc tiêu đề..."
                value={filters.keyword}
                onChange={(e) => setFilters((prev) => ({ ...prev, keyword: e.target.value }))}
              />
              <button type="submit" className="btn-primary shrink-0">
                <Search size={16} /> Lọc
              </button>
              <button type="button" className="btn-secondary shrink-0" onClick={resetFilters}>
                Đặt lại
              </button>
            </div>
          </label>
        </div>
      </form>

      {/* Queue Table */}
      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Đang tải danh sách hàng đợi..." />
        ) : items.length === 0 ? (
          <EmptyState
            icon={LifeBuoy}
            title="Không có phiếu hỗ trợ nào trong hàng đợi"
            description="Các phiếu mới từ sinh viên hoặc phiếu trong phạm vi được chọn sẽ hiển thị tại đây."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1050px] text-left text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã phiếu</th>
                  <th className="th">Sinh viên</th>
                  <th className="th">Loại & Ưu tiên</th>
                  <th className="th">Tiêu đề</th>
                  <th className="th">Trạng thái & SLA</th>
                  <th className="th">Người xử lý</th>
                  <th className="th">Hạn xử lý</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/70">
                    <td className="td font-mono text-xs font-semibold text-slate-700">
                      {item.maPhieu}
                    </td>
                    <td className="td">
                      <p className="font-medium text-slate-900">{item.hoTenSinhVien}</p>
                      <p className="text-xs text-slate-500">{item.maSinhVien}</p>
                    </td>
                    <td className="td space-y-1">
                      <div>
                        <Badge tone="indigo" dot={false}>
                          {item.tenLoaiYeuCau}
                        </Badge>
                      </div>
                      <div>
                        <Badge tone={PRIORITY_TONE[item.mucUuTien] || "slate"} dot={false}>
                          {item.tenMucUuTien || priorityNameMap[item.mucUuTien] || item.mucUuTien}
                        </Badge>
                      </div>
                    </td>
                    <td className="td max-w-sm">
                      <p className="font-medium text-slate-900">{item.tieuDe}</p>
                      <div className="mt-1 flex flex-wrap gap-2 text-xs text-slate-500">
                        {item.monHoc && <span>Môn: {item.monHoc.ma}</span>}
                        {item.hocKy && <span>· {item.hocKy.ten}</span>}
                        {item.diem && <span>· Lớp {item.diem.maLop}</span>}
                        {item.yeuCauBoSungMinhChung && (
                          <span className="font-semibold text-amber-600">· Đang chờ SV bổ sung MC</span>
                        )}
                      </div>
                    </td>
                    <td className="td space-y-1">
                      <div>
                        <Badge tone={STATUS_TONE[item.trangThai] || "slate"}>
                          {item.tenTrangThai}
                        </Badge>
                      </div>
                      {item.sla && (
                        <div>
                          <Badge tone={SLA_TONE[item.sla.slaStatus] || "slate"} dot={false}>
                            {item.sla.slaStatusDescription}
                          </Badge>
                        </div>
                      )}
                    </td>
                    <td className="td text-xs">
                      {item.nguoiXuLyUsername ? (
                        <span className="font-medium text-slate-900">{item.nguoiXuLyUsername}</span>
                      ) : (
                        <span className="italic text-amber-600 font-medium">Chưa phân công</span>
                      )}
                    </td>
                    <td className="td whitespace-nowrap text-xs text-slate-600">
                      {item.hanXuLy ? formatDateTime(item.hanXuLy) : "—"}
                    </td>
                    <td className="td text-right">
                      <button
                        type="button"
                        className="btn-secondary py-1.5 text-xs"
                        onClick={() => openTicketDetail(item.id)}
                      >
                        <Eye size={14} /> Xử lý
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {pageData && pageData.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <span className="text-xs text-slate-500">
              Trang {pageData.number + 1} / {pageData.totalPages} · Tổng {pageData.totalElements} phiếu
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-secondary"
                disabled={pageData.first}
                onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
              >
                Trước
              </button>
              <button
                type="button"
                className="btn-secondary"
                disabled={pageData.last}
                onClick={() => setPageNumber((p) => p + 1)}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Admin Ticket Detail & Processing Modal */}
      <Modal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={
          selectedTicket
            ? `Phiếu ${selectedTicket.maPhieu} — ${selectedTicket.hoTenSinhVien} (${selectedTicket.maSinhVien})`
            : "Xử lý phiếu hỗ trợ"
        }
        description={
          selectedTicket
            ? `${selectedTicket.tenLoaiYeuCau} · Ưu tiên: ${selectedTicket.tenMucUuTien || selectedTicket.mucUuTien} · Gửi lúc ${formatDateTime(selectedTicket.createdAt)}`
            : ""
        }
        size="xl"
      >
        {detailLoading ? (
          <Spinner label="Đang tải chi tiết phiếu hỗ trợ..." />
        ) : detailError && !selectedTicket ? (
          <Alert tone="error">{detailError}</Alert>
        ) : (
          selectedTicket && (
            <div className="space-y-6">
              {detailError && (
                <Alert tone="error" onClose={() => setDetailError("")}>
                  {detailError}
                </Alert>
              )}

              {/* Action Toolbar */}
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-indigo-100 bg-indigo-50/50 p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={openAssignModal}
                    className="btn-secondary text-xs"
                  >
                    <UserCheck size={14} /> Phân công xử lý
                  </button>
                  <button
                    type="button"
                    onClick={openTriageModal}
                    className="btn-secondary text-xs"
                  >
                    <SlidersHorizontal size={14} /> Điều chỉnh ưu tiên & hạn (Triage)
                  </button>
                  {selectedTicket.trangThai !== "DA_GIAI_QUYET" && selectedTicket.trangThai !== "DA_DONG" && (
                    <button
                      type="button"
                      onClick={openEvidenceModal}
                      className="btn-secondary text-xs text-amber-700 hover:text-amber-800"
                    >
                      <FileQuestion size={14} /> Yêu cầu bổ sung minh chứng
                    </button>
                  )}
                </div>

                <div className="flex items-center gap-2 text-xs">
                  <span className="text-slate-500">Người xử lý:</span>
                  <span className="font-semibold text-slate-900">
                    {selectedTicket.nguoiXuLyUsername || "Chưa phân công"}
                  </span>
                </div>
              </div>

              {/* SLA & Time Metrics Card */}
              {selectedTicket.sla && (
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-2.5">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-slate-700">Tiến độ SLA làm việc:</span>
                      <Badge tone={SLA_TONE[selectedTicket.sla.slaStatus] || "slate"}>
                        {selectedTicket.sla.slaStatusDescription}
                      </Badge>
                    </div>
                    <span className="text-xs text-slate-500">
                      Hạn xử lý: <strong>{selectedTicket.hanXuLy ? formatDateTime(selectedTicket.hanXuLy) : "Chưa xác định"}</strong>
                    </span>
                  </div>

                  <div className="mt-3 grid gap-3 text-xs sm:grid-cols-4">
                    <div>
                      <span className="text-slate-500">Giờ làm việc đã dùng:</span>
                      <p className="font-semibold text-slate-800">
                        {Math.floor(selectedTicket.sla.workingMinutesSpent / 60)}h {selectedTicket.sla.workingMinutesSpent % 60}m
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-500">Giới hạn SLA quy định:</span>
                      <p className="font-semibold text-slate-800">
                        {Math.floor(selectedTicket.sla.slaLimitWorkingMinutes / 60)}h ({selectedTicket.sla.slaLimitWorkingMinutes}m)
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-500">Thời gian còn lại:</span>
                      <p className={`font-semibold ${selectedTicket.sla.isOverdue ? "text-rose-600" : "text-emerald-700"}`}>
                        {selectedTicket.sla.isOverdue
                          ? `Quá hạn ${Math.floor(selectedTicket.sla.overdueWorkingMinutes / 60)}h ${selectedTicket.sla.overdueWorkingMinutes % 60}m`
                          : `${Math.floor(selectedTicket.sla.workingMinutesRemaining / 60)}h ${selectedTicket.sla.workingMinutesRemaining % 60}m`}
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-500">Tạm dừng chờ SV:</span>
                      <p className="font-semibold text-slate-800">
                        {Math.floor(selectedTicket.sla.pausedWorkingMinutes / 60)}h {selectedTicket.sla.pausedWorkingMinutes % 60}m
                      </p>
                    </div>
                  </div>

                  {selectedTicket.sla.thoiGianCacTrangThai?.length > 0 && (
                    <div className="mt-3 border-t border-slate-200 pt-2.5">
                      <span className="text-[11px] font-medium text-slate-500">Phân bổ thời gian theo trạng thái:</span>
                      <div className="mt-1 flex flex-wrap gap-2">
                        {selectedTicket.sla.thoiGianCacTrangThai.map((item, idx) => (
                          <span
                            key={idx}
                            className={`rounded px-2 py-0.5 text-[11px] ${
                              item.dangChay ? "bg-indigo-100 text-indigo-800 font-semibold" : "bg-slate-200/70 text-slate-700"
                            }`}
                          >
                            {item.tenTrangThai}: {Math.floor(item.workingMinutes / 60)}h {item.workingMinutes % 60}m {item.dangChay && "(đang chạy)"}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Status Update Panel */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-slate-600">Trạng thái hiện tại:</span>
                    <Badge tone={STATUS_TONE[selectedTicket.trangThai] || "slate"}>
                      {selectedTicket.tenTrangThai}
                    </Badge>
                  </div>
                  {selectedTicket.yeuCauBoSungMinhChung && (
                    <span className="rounded bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800">
                      Đang yêu cầu sinh viên bổ sung tài liệu
                    </span>
                  )}
                </div>

                {selectedTicket.trangThaiTiepTheoHopLe?.length > 0 && (
                  <form
                    onSubmit={handleStatusUpdate}
                    className="mt-3 grid gap-3 border-t border-slate-100 pt-3 sm:grid-cols-12"
                  >
                    <div className="sm:col-span-4">
                      <Field label="Chuyển sang trạng thái" required>
                        <select
                          className="input w-full text-sm"
                          value={nextStatus}
                          onChange={(e) => setNextStatus(e.target.value)}
                          required
                        >
                          {selectedTicket.trangThaiTiepTheoHopLe.map((code) => (
                            <option key={code} value={code}>
                              {statusNameMap[code] || code}
                            </option>
                          ))}
                        </select>
                      </Field>
                    </div>
                    <div className="sm:col-span-6">
                      <Field label="Lý do / Ghi chú chuyển trạng thái">
                        <input
                          type="text"
                          maxLength={500}
                          className="input w-full text-sm"
                          placeholder="Nhập lý do chuyển trạng thái hoặc kết quả xử lý..."
                          value={statusNote}
                          onChange={(e) => setStatusNote(e.target.value)}
                        />
                      </Field>
                    </div>
                    <div className="flex items-end sm:col-span-2">
                      <button
                        type="submit"
                        className="btn-primary w-full justify-center py-2 text-xs"
                        disabled={statusUpdating}
                      >
                        <CheckCircle2 size={14} />{" "}
                        {statusUpdating ? "Đang lưu..." : "Cập nhật"}
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {/* Original Immutable Content */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                  <h3 className="text-sm font-semibold text-slate-900">
                    {selectedTicket.tieuDe}
                  </h3>
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">
                    Nội dung gốc sinh viên gửi
                  </span>
                </div>
                <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">
                  {selectedTicket.noiDungGoc}
                </p>

                {(selectedTicket.monHoc || selectedTicket.hocKy || selectedTicket.diem) && (
                  <div className="mt-4 grid gap-2 rounded-lg bg-slate-50 p-3 text-xs text-slate-700 sm:grid-cols-3">
                    {selectedTicket.monHoc && (
                      <div>
                        <span className="font-semibold text-slate-500">Môn học: </span>
                        {selectedTicket.monHoc.ma} - {selectedTicket.monHoc.ten}
                      </div>
                    )}
                    {selectedTicket.hocKy && (
                      <div>
                        <span className="font-semibold text-slate-500">Học kỳ: </span>
                        {selectedTicket.hocKy.ten} ({selectedTicket.hocKy.namHoc})
                      </div>
                    )}
                    {selectedTicket.diem && (
                      <div>
                        <span className="font-semibold text-slate-500">Bản ghi điểm: </span>
                        Lớp {selectedTicket.diem.maLop} · QT:{" "}
                        {formatDiem(selectedTicket.diem.diemQuaTrinh)} · CK:{" "}
                        {formatDiem(selectedTicket.diem.diemCuoiKy)} · TK:{" "}
                        {formatDiem(selectedTicket.diem.diemTongKet)}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Attachments */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <Paperclip size={16} className="text-slate-500" />
                  Tệp minh chứng đính kèm ({selectedTicket.tepDinhKem?.length || 0})
                </h4>
                {!selectedTicket.tepDinhKem?.length ? (
                  <p className="mt-2 text-xs text-slate-500">Không có tệp đính kèm.</p>
                ) : (
                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                    {selectedTicket.tepDinhKem.map((att) => (
                      <div
                        key={att.id}
                        className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2 text-xs"
                      >
                        <div className="min-w-0">
                          <p className="truncate font-medium text-slate-800">{att.tenTepGoc}</p>
                          <p className="text-slate-400">
                            {formatFileSize(att.kichThuoc)} · {att.contentType}
                          </p>
                        </div>
                        <button
                          type="button"
                          className="btn-secondary shrink-0 py-1 text-xs"
                          onClick={() =>
                            downloadAttachmentFile(selectedTicket.id, att, setDetailError)
                          }
                        >
                          <Download size={13} /> Tải xuống
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Two columns: Discussion Thread & Status History */}
              <div className="grid gap-6 lg:grid-cols-5">
                {/* Discussion Thread */}
                <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 lg:col-span-3">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <MessageSquare size={16} className="text-slate-500" />
                    Trao đổi & Ghi chú trên phiếu ({selectedTicket.traoDoi?.length || 0})
                  </h4>

                  {!selectedTicket.traoDoi?.length ? (
                    <p className="py-4 text-center text-xs text-slate-500">
                      Chưa có trao đổi nào trên phiếu này.
                    </p>
                  ) : (
                    <div className="max-h-80 space-y-3 overflow-y-auto pr-1">
                      {selectedTicket.traoDoi.map((msg) => {
                        const isAdmin = msg.vaiTroNguoiGui === "QUAN_TRI";
                        const isInternal = msg.isNoiBo || !msg.congKhai;
                        return (
                          <div
                            key={msg.id}
                            className={`rounded-xl border p-3 text-sm ${
                              isInternal
                                ? "border-amber-200 bg-amber-50/60"
                                : isAdmin
                                ? "border-indigo-100 bg-indigo-50/60"
                                : "border-slate-200 bg-slate-50/70"
                            }`}
                          >
                            <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                              <span className="font-semibold text-slate-800">
                                {msg.tenHienThiNguoiGui}
                              </span>
                              <div className="flex items-center gap-2">
                                {isInternal && (
                                  <Badge tone="red" dot={false}>
                                    <Lock size={10} className="inline mr-1" />
                                    Nội bộ (SV không thấy)
                                  </Badge>
                                )}
                                <Badge tone={isAdmin ? "indigo" : "green"} dot={false}>
                                  {isAdmin ? "Phòng Đào tạo" : "Sinh viên"}
                                </Badge>
                                <span className="text-slate-400">
                                  {formatDateTime(msg.createdAt)}
                                </span>
                              </div>
                            </div>
                            <p className="mt-2 whitespace-pre-line text-slate-700">
                              {msg.noiDung}
                            </p>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {selectedTicket.coThePhanHoi ? (
                    <form onSubmit={handleReplySubmit} className="space-y-2 border-t border-slate-100 pt-3">
                      <textarea
                        rows={3}
                        maxLength={2000}
                        className="input w-full text-sm"
                        placeholder="Nhập nội dung phản hồi cho sinh viên hoặc ghi chú xử lý..."
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        required
                      />
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <label className="flex items-center gap-2 text-xs font-medium text-slate-700">
                          <input
                            type="checkbox"
                            className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                            checked={isInternalNote}
                            onChange={(e) => setIsInternalNote(e.target.checked)}
                          />
                          Ghi chú nội bộ (Chỉ cán bộ thấy, không hiển thị cho sinh viên)
                        </label>

                        <button
                          type="submit"
                          className="btn-primary py-1.5 text-xs"
                          disabled={replySubmitting}
                        >
                          <Send size={14} />{" "}
                          {replySubmitting ? "Đang gửi..." : isInternalNote ? "Lưu ghi chú nội bộ" : "Gửi phản hồi"}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <Alert tone="info">
                      Phiếu đang ở trạng thái <strong>{selectedTicket.tenTrangThai}</strong> nên đã
                      khóa tính năng gửi thêm phản hồi.
                    </Alert>
                  )}
                </div>

                {/* Status & Audit History */}
                <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 lg:col-span-2">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <History size={16} className="text-slate-500" />
                    Lịch sử thay đổi & trạng thái ({selectedTicket.lichSuTrangThai?.length || 0})
                  </h4>

                  <ol className="relative ml-2 border-l border-slate-200 space-y-4 max-h-96 overflow-y-auto pr-1">
                    {(selectedTicket.lichSuTrangThai || []).map((hist) => (
                      <li key={hist.id} className="ml-4 text-xs">
                        <span className="absolute -left-1.5 mt-1 h-3 w-3 rounded-full border border-white bg-indigo-600" />
                        <div className="flex flex-wrap items-center gap-1.5">
                          {hist.loaiThayDoi && (
                            <span className="font-semibold text-slate-900">
                              [{hist.loaiThayDoi}]
                            </span>
                          )}
                          {hist.tenTrangThaiCu && (
                            <>
                              <span className="text-slate-500">{hist.tenTrangThaiCu}</span>
                              <span className="text-slate-400">→</span>
                            </>
                          )}
                          <Badge tone={STATUS_TONE[hist.trangThaiMoi] || "slate"} dot={false}>
                            {hist.tenTrangThaiMoi || hist.giaTriMoi || "Thay đổi"}
                          </Badge>
                        </div>
                        {hist.giaTriCu && hist.giaTriMoi && hist.loaiThayDoi !== "TRANG_THAI" && (
                          <p className="mt-0.5 text-slate-500">
                            {hist.giaTriCu} → <strong>{hist.giaTriMoi}</strong>
                          </p>
                        )}
                        <p className="mt-1 text-slate-600">
                          Bởi: <strong>{hist.tenNguoiThucHien}</strong> (
                          {hist.vaiTroNguoiThucHien === "QUAN_TRI" ? "Quản trị" : "Sinh viên"})
                        </p>
                        <p className="mt-0.5 flex items-center gap-1 text-slate-400">
                          <Clock size={12} /> {formatDateTime(hist.thoiDiem)}
                        </p>
                        {hist.ghiChu && (
                          <p className="mt-1 rounded bg-slate-50 p-2 text-slate-700">
                            {hist.ghiChu}
                          </p>
                        )}
                      </li>
                    ))}
                  </ol>
                </div>
              </div>
            </div>
          )
        )}
      </Modal>

      {/* Assign Modal */}
      <Modal
        open={assignOpen}
        onClose={() => !assignSubmitting && setAssignOpen(false)}
        title={`Phân công người xử lý phiếu ${selectedTicket?.maPhieu}`}
        description="Chọn cán bộ quản trị tiếp nhận giải quyết yêu cầu của sinh viên."
      >
        <form onSubmit={handleAssignSubmit} className="space-y-4">
          <Field label="Cán bộ xử lý" required>
            <select
              className="input w-full"
              value={assignUserId}
              onChange={(e) => setAssignUserId(e.target.value)}
            >
              <option value="">-- Chưa phân công --</option>
              {handlers.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.hoTen} ({h.username}) - {h.email}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Lý do / Ghi chú phân công" required>
            <input
              type="text"
              maxLength={255}
              className="input w-full"
              placeholder="VD: Phân công chuyên viên phụ trách học phần..."
              value={assignNote}
              onChange={(e) => setAssignNote(e.target.value)}
              required
            />
          </Field>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              className="btn-secondary"
              disabled={assignSubmitting}
              onClick={() => setAssignOpen(false)}
            >
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={assignSubmitting}>
              {assignSubmitting ? "Đang lưu..." : "Lưu phân công"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Triage Modal */}
      <Modal
        open={triageOpen}
        onClose={() => !triageSubmitting && setTriageOpen(false)}
        title={`Điều chỉnh phân loại, mức ưu tiên & hạn xử lý: ${selectedTicket?.maPhieu}`}
        description="Cập nhật mức độ ưu tiên nghiệp vụ và ấn định hạn chót xử lý theo lịch làm việc."
      >
        <form onSubmit={handleTriageSubmit} className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Mức ưu tiên" required>
              <select
                className="input w-full"
                value={triagePriority}
                onChange={(e) => setTriagePriority(e.target.value)}
                required
              >
                <option value="THAP">Thấp</option>
                <option value="TRUNG_BINH">Trung bình</option>
                <option value="CAO">Cao</option>
                <option value="KHAN_CAP">Khẩn cấp</option>
              </select>
            </Field>

            <Field label="Loại yêu cầu">
              <select
                className="input w-full"
                value={triageCategory}
                onChange={(e) => setTriageCategory(e.target.value)}
              >
                {(catalog?.loaiYeuCau || []).map((c) => (
                  <option key={c.ma} value={c.ma}>
                    {c.ten}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <Field label="Hạn xử lý (SLA deadline)">
            <input
              type="datetime-local"
              className="input w-full"
              value={triageDeadline}
              onChange={(e) => setTriageDeadline(e.target.value)}
            />
          </Field>

          <Field label="Lý do điều chỉnh (bắt buộc lưu lịch sử)" required>
            <input
              type="text"
              maxLength={255}
              className="input w-full"
              placeholder="VD: Nâng ưu tiên do sắp hết hạn đăng ký đợt bổ sung..."
              value={triageNote}
              onChange={(e) => setTriageNote(e.target.value)}
              required
            />
          </Field>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              className="btn-secondary"
              disabled={triageSubmitting}
              onClick={() => setTriageOpen(false)}
            >
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={triageSubmitting}>
              {triageSubmitting ? "Đang lưu..." : "Lưu điều chỉnh"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Request Evidence Modal */}
      <Modal
        open={evidenceOpen}
        onClose={() => !evidenceSubmitting && setEvidenceOpen(false)}
        title={`Yêu cầu bổ sung minh chứng: ${selectedTicket?.maPhieu}`}
        description="Yêu cầu sinh viên cung cấp thêm giấy tờ hoặc minh chứng xác thực. Đồng hồ SLA xử lý sẽ tạm dừng."
      >
        <form onSubmit={handleEvidenceSubmit} className="space-y-4">
          <Field label="Nội dung yêu cầu sinh viên cung cấp" required>
            <textarea
              rows={4}
              maxLength={1000}
              className="input w-full"
              placeholder="Vui lòng cung cấp đơn xin mở lớp hoặc giấy xác nhận có chữ ký giảng viên..."
              value={evidenceNote}
              onChange={(e) => setEvidenceNote(e.target.value)}
              required
            />
          </Field>

          <Field label="Hạn sinh viên bổ sung (tùy chọn)">
            <input
              type="datetime-local"
              className="input w-full"
              value={evidenceDeadline}
              onChange={(e) => setEvidenceDeadline(e.target.value)}
            />
          </Field>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              className="btn-secondary"
              disabled={evidenceSubmitting}
              onClick={() => setEvidenceOpen(false)}
            >
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={evidenceSubmitting}>
              {evidenceSubmitting ? "Đang gửi..." : "Gửi yêu cầu bổ sung"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
