import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  AlertTriangle,
  Ban,
  CheckCircle2,
  ClipboardCheck,
  Download,
  Layers,
  Lock,
  Megaphone,
  Pencil,
  Plus,
  RotateCcw,
  Search,
  Trash2,
  Unlock,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem, lichHoc, soBuoiHoc, tietLyThuyet } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const GRADE_SHEET_STATUS_META = {
  NHAP: { label: "Điểm: Nháp", tone: "slate" },
  CHO_DUYET: { label: "Điểm: Chờ duyệt", tone: "amber" },
  TRA_VE: { label: "Điểm: Trả sửa", tone: "rose" },
  DA_DUYET: { label: "Điểm: Đã duyệt", tone: "blue" },
  DA_CONG_BO: { label: "Điểm: Đã công bố", tone: "green" },
  DA_KHOA: { label: "Điểm: Đã khóa", tone: "indigo" },
};

const ACTION_LABEL = {
  GUI_DUYET: "Giảng viên gửi duyệt",
  TRA_VE: "Trả về chỉnh sửa",
  PHE_DUYET: "Phê duyệt bảng điểm",
  CONG_BO: "Công bố bảng điểm",
  KHOA: "Khóa bảng điểm",
  MO_KHOA: "Mở khóa bảng điểm",
};

const EMPTY_FORM = {
  ma: "",
  monHocId: "",
  hocKyId: "",
  giangVienId: "",
  phong: "",
  thu: "2",
  tietBatDau: "1",
  soTiet: "3",
  siSoToiDa: "40",
};

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

export default function CourseClassPage() {
  const [searchParams] = useSearchParams();
  const confirm = useConfirm();
  const { data: hocKyList } = useApi("/admin/hoc-ky");
  const { data: monHocList } = useApi("/admin/mon-hoc");
  const { data: giangVienList } = useApi("/admin/giang-vien");
  const { data: activeRooms } = useApi("/admin/phong-hoc/hoat-dong");

  const [hocKyId, setHocKyId] = useState(() => searchParams.get("hocKyId") || "");
  const [keyword, setKeyword] = useState(() => searchParams.get("keyword") || "");
  const [giangVienFilter, setGiangVienFilter] = useState("");
  const [gradeStatusFilter, setGradeStatusFilter] = useState("");
  const [message, setMessage] = useState("");
  const [warning, setWarning] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");
  const [exportingClassId, setExportingClassId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [conflictLoading, setConflictLoading] = useState(false);
  const [conflictResult, setConflictResult] = useState(null);

  // Grade sheet review / reason modal state (QLSV-74)
  const [reasonModalOpen, setReasonModalOpen] = useState(false);
  const [reasonAction, setReasonAction] = useState(null); // { row, mode: 'TRA_VE' | 'MO_KHOA' }
  const [reasonText, setReasonText] = useState("");
  const [reasonError, setReasonError] = useState("");
  const [reasonSubmitting, setReasonSubmitting] = useState(false);

  const [gradeDetailOpen, setGradeDetailOpen] = useState(false);
  const [gradeDetailRow, setGradeDetailRow] = useState(null);
  const [gradeDetailStatus, setGradeDetailStatus] = useState(null);
  const [gradeDetailStudents, setGradeDetailStudents] = useState([]);
  const [gradeDetailLoading, setGradeDetailLoading] = useState(false);
  const [gradeDetailError, setGradeDetailError] = useState("");

  // Impact survey & cancellation modal (QLSV-79)
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelSurveyRow, setCancelSurveyRow] = useState(null);
  const [cancelSurveyData, setCancelSurveyData] = useState(null);
  const [cancelSurveyLoading, setCancelSurveyLoading] = useState(false);
  const [cancelSurveyError, setCancelSurveyError] = useState("");
  const [cancelReason, setCancelReason] = useState("");
  const [cancelling, setCancelling] = useState(false);

  // Student transfer modal (QLSV-79)
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [transferSourceRow, setTransferSourceRow] = useState(null);
  const [transferTargetClasses, setTransferTargetClasses] = useState([]);
  const [transferTargetClassId, setTransferTargetClassId] = useState("");
  const [transferStudents, setTransferStudents] = useState([]);
  const [transferSelectedStudentIds, setTransferSelectedStudentIds] = useState([]);
  const [transferCheckResults, setTransferCheckResults] = useState(null);
  const [transferChecking, setTransferChecking] = useState(false);
  const [transferReason, setTransferReason] = useState("");
  const [transferSubmitting, setTransferSubmitting] = useState(false);
  const [transferError, setTransferError] = useState("");

  // Transfer history modal (QLSV-79)
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [historyRow, setHistoryRow] = useState(null);
  const [historyList, setHistoryList] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");


  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data: lopList, loading, error, reload } = useApi("/admin/lop-hoc-phan", params);

  const pendingApprovalCount = useMemo(() => {
    return (lopList || []).filter((l) => l.trangThaiBangDiem === "CHO_DUYET").length;
  }, [lopList]);

  const rows = useMemo(() => {
    let list = lopList || [];
    if (giangVienFilter) {
      list = list.filter((lop) => String(lop.giangVienId) === String(giangVienFilter));
    }
    if (gradeStatusFilter) {
      list = list.filter((lop) => (lop.trangThaiBangDiem || "NHAP") === gradeStatusFilter);
    }
    const term = keyword.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (lop) =>
        lop.ma.toLowerCase().includes(term) ||
        lop.tenMon.toLowerCase().includes(term) ||
        lop.maMon.toLowerCase().includes(term) ||
        lop.tenGiangVien.toLowerCase().includes(term)
    );
  }, [lopList, keyword, giangVienFilter, gradeStatusFilter]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM, hocKyId: hocKyId || "" });
    setFormError("");
    setConflictResult(null);
    setModalOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    setForm({
      ma: row.ma,
      monHocId: String(row.monHocId),
      hocKyId: String(row.hocKyId),
      giangVienId: String(row.giangVienId),
      phong: row.phong,
      thu: String(row.thu),
      tietBatDau: String(row.tietBatDau),
      soTiet: String(row.soTiet),
      siSoToiDa: String(row.siSoToiDa),
    });
    setFormError("");
    setConflictResult(null);
    setModalOpen(true);
  };

  // Tự động kiểm tra xung đột thời khóa biểu với cùng thuật toán backend (QLSV-78)
  useEffect(() => {
    if (!modalOpen) {
      setConflictResult(null);
      return;
    }

    const hocKyIdNum = Number(form.hocKyId);
    const giangVienIdNum = Number(form.giangVienId);
    const thuNum = Number(form.thu);
    const tietBatDauNum = Number(form.tietBatDau);
    const soTietNum = Number(form.soTiet);
    const phongTrim = form.phong ? form.phong.trim() : "";

    if (
      !hocKyIdNum ||
      !giangVienIdNum ||
      !phongTrim ||
      !thuNum ||
      !tietBatDauNum ||
      !soTietNum ||
      tietBatDauNum < 1 ||
      tietBatDauNum > 12 ||
      soTietNum < 1 ||
      soTietNum > 6 ||
      tietBatDauNum + soTietNum - 1 > 12
    ) {
      setConflictResult(null);
      return;
    }

    const timer = setTimeout(async () => {
      setConflictLoading(true);
      try {
        const { data } = await api.post("/admin/lop-hoc-phan/kiem-tra-xung-dot", {
          lopHocPhanId: editing ? editing.id : null,
          hocKyId: hocKyIdNum,
          monHocId: form.monHocId ? Number(form.monHocId) : null,
          giangVienId: giangVienIdNum,
          phong: phongTrim,
          thu: thuNum,
          tietBatDau: tietBatDauNum,
          soTiet: soTietNum,
        });
        setConflictResult(data);
      } catch (err) {
        console.error("Lỗi khi kiểm tra xung đột lịch học:", err);
      } finally {
        setConflictLoading(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [
    modalOpen,
    editing,
    form.hocKyId,
    form.giangVienId,
    form.phong,
    form.thu,
    form.tietBatDau,
    form.soTiet,
    form.monHocId,
  ]);

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    setWarning("");
    setActionError("");
    try {
      const tietBatDau = Number(form.tietBatDau);
      const soTiet = Number(form.soTiet);
      if (tietBatDau + soTiet - 1 > 12) {
        throw new Error("Lịch học vượt quá tiết 12");
      }
      const payload = {
        ma: form.ma.trim(),
        monHocId: Number(form.monHocId),
        hocKyId: Number(form.hocKyId),
        giangVienId: Number(form.giangVienId),
        phong: form.phong.trim(),
        thu: Number(form.thu),
        tietBatDau,
        soTiet,
        siSoToiDa: Number(form.siSoToiDa),
      };
      const { data } = editing
        ? await api.put(`/admin/lop-hoc-phan/${editing.id}`, payload)
        : await api.post("/admin/lop-hoc-phan", payload);

      setModalOpen(false);
      reload();
      if (data.canhBao?.length) {
        setWarning(`${editing ? "Đã cập nhật" : "Đã tạo"} lớp ${data.lopHocPhan.ma}. Cảnh báo:\n- ${data.canhBao.join("\n- ")}`);
      } else {
        setMessage(`${editing ? "Cập nhật" : "Tạo"} lớp học phần ${data.lopHocPhan.ma} thành công`);
      }
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa lớp học phần",
      message: `Bạn có chắc muốn xóa lớp ${row.ma} - ${row.tenMon}?`,
      detail: "Chỉ có thể xóa lớp chưa có sinh viên đăng ký. Thao tác này không thể hoàn tác.",
      confirmLabel: "Xóa lớp",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    setWarning("");
    try {
      await api.delete(`/admin/lop-hoc-phan/${row.id}`);
      setMessage(`Đã xóa lớp ${row.ma}`);
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  const huyLop = async (row) => {
    const daXacNhan = await confirm({
      title: "Hủy lớp học phần",
      message: `Bạn có chắc muốn hủy lớp ${row.ma} - ${row.tenMon}?`,
      detail: "Tất cả đăng ký đang hiệu lực của lớp sẽ chuyển sang Đã hủy và gửi thông báo tự động tới sinh viên bị ảnh hưởng.",
      confirmLabel: "Hủy lớp học phần",
      tone: "danger",
    });
    if (!daXacNhan) return;
    const lyDo = window.prompt("Lý do hủy lớp học phần (không bắt buộc):") ?? "";
    setActionError("");
    setMessage("");
    setWarning("");
    try {
      const { data } = await api.post(`/admin/lop-hoc-phan/${row.id}/huy`, {
        lyDo: lyDo.trim() || null,
      });
      setMessage(`Đã hủy lớp ${data.maLop} (${data.soDangKyBiHuy} đăng ký được chuyển sang Đã hủy và thông báo tới sinh viên)`);
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  const loadGradeDetail = async (row) => {
    setGradeDetailOpen(true);
    setGradeDetailRow(row);
    setGradeDetailLoading(true);
    setGradeDetailError("");
    try {
      const [statusRes, studentsRes] = await Promise.all([
        api.get(`/admin/lop-hoc-phan/${row.id}/trang-thai-bang-diem`),
        api.get(`/admin/lop-hoc-phan/${row.id}/sinh-vien`),
      ]);
      setGradeDetailStatus(statusRes.data);
      setGradeDetailStudents(studentsRes.data || []);
    } catch (err) {
      setGradeDetailError(getErrorMessage(err));
    } finally {
      setGradeDetailLoading(false);
    }
  };

  const thucHienBangDiem = async (row, endpoint, successText, payload = undefined) => {
    setActionError("");
    setMessage("");
    setWarning("");
    try {
      const { data } = await api.post(`/admin/lop-hoc-phan/${row.id}/${endpoint}`, payload);
      setMessage(successText);
      reload();
      if (gradeDetailOpen && gradeDetailRow?.id === row.id) {
        setGradeDetailStatus(data);
      }
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  const openReasonModal = (row, mode) => {
    setReasonAction({ row, mode });
    setReasonText("");
    setReasonError("");
    setReasonModalOpen(true);
  };

  const submitReasonAction = async (event) => {
    event.preventDefault();
    if (!reasonAction) return;
    const cleanReason = reasonText.trim();
    if (!cleanReason) {
      setReasonError(
        reasonAction.mode === "TRA_VE"
          ? "Vui lòng nhập lý do trả bảng điểm về chỉnh sửa"
          : "Vui lòng nhập lý do bắt buộc để mở khóa bảng điểm"
      );
      return;
    }
    setReasonSubmitting(true);
    setReasonError("");
    setActionError("");
    setMessage("");
    try {
      const endpoint = reasonAction.mode === "TRA_VE" ? "tra-ve-diem" : "mo-khoa-diem";
      const { data } = await api.post(`/admin/lop-hoc-phan/${reasonAction.row.id}/${endpoint}`, {
        lyDo: cleanReason,
      });
      setReasonModalOpen(false);
      setMessage(
        reasonAction.mode === "TRA_VE"
          ? `Đã trả bảng điểm lớp ${reasonAction.row.ma} về cho giảng viên chỉnh sửa`
          : `Đã mở khóa bảng điểm lớp ${reasonAction.row.ma} để chỉnh sửa`
      );
      reload();
      if (gradeDetailOpen && gradeDetailRow?.id === reasonAction.row.id) {
        setGradeDetailStatus(data);
      }
    } catch (err) {
      setReasonError(getErrorMessage(err));
    } finally {
      setReasonSubmitting(false);
    }
  };

  const taiCsv = async (row) => {
    setActionError("");
    setActionSuccess("");
    setExportingClassId(row.id);
    try {
      const response = await api.get(`/admin/lop-hoc-phan/${row.id}/csv`, { responseType: "blob" });
      const defaultFilename = `danh-sach-sinh-vien-${row.ma}.csv`;
      const tenTep = layTenTepTuHeader(response.headers, defaultFilename);
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = tenTep;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setActionSuccess(`Đã xuất danh sách lớp ${row.ma} thành công (${tenTep})`);
    } catch (err) {
      setActionError(await docThongBaoLoi(err));
    } finally {
      setExportingClassId(null);
    }
  };

  return (
    <div>
      <PageHeader
        icon={Layers}
        title="Lớp học phần & Duyệt bảng điểm"
        description="Mở lớp theo học kỳ, phân công giảng viên và thực hiện phê duyệt, trả sửa, công bố, khóa/mở khóa bảng điểm."
        actions={
          <div className="flex flex-wrap items-center gap-2">
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
            <button type="button" className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> Mở lớp
            </button>
          </div>
        }
      />

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {warning && <Alert tone="warning" onClose={() => setWarning("")}>{warning}</Alert>}
        {actionSuccess && <Alert tone="success" onClose={() => setActionSuccess("")}>{actionSuccess}</Alert>}
        {actionError && <Alert tone="error" onClose={() => setActionError("")}>{actionError}</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        {/* Quick Grade Sheet Approval Filters */}
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-200 bg-slate-50/70 px-4 py-2.5">
          <span className="text-xs font-semibold text-slate-500 mr-1">Trạng thái điểm:</span>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              !gradeStatusFilter ? "bg-[#173f3a] text-white" : "bg-white border border-slate-200 text-slate-600 hover:bg-slate-100"
            }`}
            onClick={() => setGradeStatusFilter("")}
          >
            Tất cả ({(lopList || []).length})
          </button>
          <button
            type="button"
            className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "CHO_DUYET"
                ? "bg-amber-600 text-white"
                : "bg-white border border-slate-200 text-amber-700 hover:bg-amber-50"
            }`}
            onClick={() => setGradeStatusFilter("CHO_DUYET")}
          >
            Chờ duyệt
            {pendingApprovalCount > 0 && (
              <span className={`rounded-full px-1.5 text-[10px] font-bold ${
                gradeStatusFilter === "CHO_DUYET" ? "bg-white text-amber-800" : "bg-amber-100 text-amber-900"
              }`}>
                {pendingApprovalCount}
              </span>
            )}
          </button>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "DA_DUYET" ? "bg-blue-600 text-white" : "bg-white border border-slate-200 text-blue-700 hover:bg-blue-50"
            }`}
            onClick={() => setGradeStatusFilter("DA_DUYET")}
          >
            Đã duyệt
          </button>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "DA_CONG_BO" ? "bg-green-600 text-white" : "bg-white border border-slate-200 text-green-700 hover:bg-green-50"
            }`}
            onClick={() => setGradeStatusFilter("DA_CONG_BO")}
          >
            Đã công bố
          </button>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "DA_KHOA" ? "bg-indigo-600 text-white" : "bg-white border border-slate-200 text-indigo-700 hover:bg-indigo-50"
            }`}
            onClick={() => setGradeStatusFilter("DA_KHOA")}
          >
            Đã khóa
          </button>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "TRA_VE" ? "bg-rose-600 text-white" : "bg-white border border-slate-200 text-rose-700 hover:bg-rose-50"
            }`}
            onClick={() => setGradeStatusFilter("TRA_VE")}
          >
            Trả sửa
          </button>
          <button
            type="button"
            className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
              gradeStatusFilter === "NHAP" ? "bg-slate-600 text-white" : "bg-white border border-slate-200 text-slate-600 hover:bg-slate-100"
            }`}
            onClick={() => setGradeStatusFilter("NHAP")}
          >
            Bản nháp
          </button>
        </div>

        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-3 w-full sm:max-w-2xl">
            <div className="relative w-full sm:max-w-xs">
              <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                className="input pl-9"
                placeholder="Tìm lớp, môn học, giảng viên..."
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
            </div>
            <select
              className="input w-auto min-w-[200px]"
              value={giangVienFilter}
              onChange={(e) => setGiangVienFilter(e.target.value)}
            >
              <option value="">Tất cả giảng viên</option>
              {(giangVienList || []).map((gv) => (
                <option key={gv.id} value={gv.id}>
                  {gv.hoTen} ({gv.ma})
                </option>
              ))}
            </select>
          </div>
          <p className="text-sm text-slate-500 shrink-0">{rows.length} lớp học phần</p>
        </div>

        {loading ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState icon={Layers} title="Chưa có lớp học phần" description="Bấm Mở lớp để tạo lớp cho học kỳ này." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1100px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã lớp</th>
                  <th className="th">Môn học</th>
                  <th className="th">Giảng viên</th>
                  <th className="th">Lịch học</th>
                  <th className="th">Phòng</th>
                  <th className="th text-center">Sĩ số</th>
                  <th className="th text-center">Bảng điểm</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => {
                  const statusMeta = GRADE_SHEET_STATUS_META[row.trangThaiBangDiem] || GRADE_SHEET_STATUS_META.NHAP;
                  return (
                    <tr key={row.id} className="hover:bg-slate-50/60">
                      <td className="td font-medium text-slate-900">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span>{row.ma}</span>
                          {row.daHuy && (
                            <Badge tone="rose" title={`Đã hủy lúc ${formatDateTime(row.thoiGianHuy)} - Lý do: ${row.lyDoHuy || "Không ghi"}`}>
                              Đã hủy
                            </Badge>
                          )}
                        </div>
                        {row.daHuy && row.lyDoHuy && (
                          <p className="text-[11px] text-rose-600 truncate max-w-[160px]" title={row.lyDoHuy}>
                            Lý do: {row.lyDoHuy}
                          </p>
                        )}
                      </td>
                      <td className="td">
                        <p className="font-medium text-slate-800">{row.tenMon}</p>
                        <p className="text-xs text-slate-500">
                          {row.maMon} · {row.soTinChi} TC · {tietLyThuyet(row.soTinChi)} tiết LT ·{" "}
                          {soBuoiHoc(row.soTinChi)} buổi
                        </p>
                      </td>
                      <td className="td">{row.tenGiangVien}</td>
                      <td className="td">{lichHoc(row.thu, row.tietBatDau, row.soTiet)}</td>
                      <td className="td">{row.phong}</td>
                      <td className="td text-center">
                        {row.siSoHienTai}/{row.siSoToiDa}
                      </td>
                      <td className="td text-center">
                        <div className="inline-flex flex-col items-center gap-1">
                          <Badge tone={statusMeta.tone}>{statusMeta.label}</Badge>
                          {row.lyDoTraVeBangDiem && row.trangThaiBangDiem === "TRA_VE" && (
                            <span className="max-w-[180px] truncate text-[10px] text-rose-600" title={row.lyDoTraVeBangDiem}>
                              Lý do: {row.lyDoTraVeBangDiem}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="td">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-indigo-600 hover:bg-indigo-50"
                            onClick={() => loadGradeDetail(row)}
                            title="Xem bảng điểm & lịch sử phê duyệt"
                          >
                            <ClipboardCheck size={15} />
                          </button>
                          {row.trangThaiBangDiem === "CHO_DUYET" && row.siSoHienTai > 0 && (
                            <>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-sky-600 hover:bg-sky-50"
                                onClick={() => thucHienBangDiem(row, "phe-duyet-diem", `Đã phê duyệt bảng điểm lớp ${row.ma}`)}
                                title="Phê duyệt bảng điểm (CHO_DUYET → DA_DUYET)"
                              >
                                <CheckCircle2 size={15} />
                              </button>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-rose-600 hover:bg-rose-50"
                                onClick={() => openReasonModal(row, "TRA_VE")}
                                title="Trả bảng điểm về chỉnh sửa kèm lý do"
                              >
                                <RotateCcw size={15} />
                              </button>
                            </>
                          )}
                          {row.trangThaiBangDiem === "DA_DUYET" && (
                            <>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50"
                                onClick={() => thucHienBangDiem(row, "cong-bo-diem", `Đã công bố bảng điểm lớp ${row.ma} và thông báo tới sinh viên`)}
                                title="Công bố bảng điểm (DA_DUYET → DA_CONG_BO)"
                              >
                                <Megaphone size={15} />
                              </button>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-indigo-600 hover:bg-indigo-50"
                                onClick={() => thucHienBangDiem(row, "khoa-diem", `Đã công bố và khóa bảng điểm lớp ${row.ma}`)}
                                title="Công bố & khóa bảng điểm (DA_DUYET → DA_KHOA)"
                              >
                                <Lock size={15} />
                              </button>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-rose-600 hover:bg-rose-50"
                                onClick={() => openReasonModal(row, "TRA_VE")}
                                title="Trả bảng điểm về chỉnh sửa kèm lý do"
                              >
                                <RotateCcw size={15} />
                              </button>
                            </>
                          )}
                          {row.trangThaiBangDiem === "DA_CONG_BO" && (
                            <>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-indigo-600 hover:bg-indigo-50"
                                onClick={() => thucHienBangDiem(row, "khoa-diem", `Đã khóa chốt bảng điểm lớp ${row.ma}`)}
                                title="Khóa chốt bảng điểm (DA_CONG_BO → DA_KHOA)"
                              >
                                <Lock size={15} />
                              </button>
                              <button
                                type="button"
                                className="rounded-md p-1.5 text-amber-600 hover:bg-amber-50"
                                onClick={() => openReasonModal(row, "MO_KHOA")}
                                title="Mở khóa bảng điểm (bắt buộc nhập lý do)"
                              >
                                <Unlock size={15} />
                              </button>
                            </>
                          )}
                          {row.trangThaiBangDiem === "DA_KHOA" && (
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-amber-600 hover:bg-amber-50"
                              onClick={() => openReasonModal(row, "MO_KHOA")}
                              title="Mở khóa bảng điểm (bắt buộc nhập lý do)"
                            >
                              <Unlock size={15} />
                            </button>
                          )}
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 disabled:opacity-50"
                            disabled={exportingClassId === row.id}
                            onClick={() => taiCsv(row)}
                            title={exportingClassId === row.id ? "Đang xuất CSV..." : "Xuất danh sách CSV"}
                          >
                            <Download size={15} className={exportingClassId === row.id ? "animate-pulse text-indigo-600" : ""} />
                          </button>
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                            onClick={() => openEdit(row)}
                            title="Sửa"
                          >
                            <Pencil size={15} />
                          </button>
                          {row.siSoHienTai > 0 && (
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-amber-600 hover:bg-amber-50"
                              onClick={() => huyLop(row)}
                              title="Hủy lớp học phần"
                            >
                              <Ban size={15} />
                            </button>
                          )}
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50"
                            onClick={() => remove(row)}
                            title="Xóa"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal nhập lý do bắt buộc khi Trả sửa hoặc Mở khóa (QLSV-74) */}
      <Modal
        open={reasonModalOpen}
        onClose={() => setReasonModalOpen(false)}
        title={
          reasonAction?.mode === "TRA_VE"
            ? `Trả bảng điểm về chỉnh sửa: ${reasonAction?.row?.ma}`
            : `Mở khóa bảng điểm: ${reasonAction?.row?.ma}`
        }
        description={
          reasonAction?.mode === "TRA_VE"
            ? "Bảng điểm sẽ chuyển về trạng thái Trả về chỉnh sửa (TRA_VE) để giảng viên phụ trách điều chỉnh và gửi duyệt lại."
            : "Thao tác mở khóa bảng điểm yêu cầu quyền đặc biệt và bắt buộc ghi rõ lý do để lưu vết kiểm toán (audit)."
        }
        size="md"
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setReasonModalOpen(false)}>
              Hủy
            </button>
            <button type="submit" form="grade-sheet-reason-form" className="btn-primary" disabled={reasonSubmitting}>
              {reasonSubmitting
                ? "Đang xử lý..."
                : reasonAction?.mode === "TRA_VE"
                  ? "Xác nhận trả sửa"
                  : "Xác nhận mở khóa"}
            </button>
          </>
        }
      >
        <form id="grade-sheet-reason-form" className="space-y-4" onSubmit={submitReasonAction}>
          {reasonError && <Alert tone="error">{reasonError}</Alert>}
          <Field
            label={reasonAction?.mode === "TRA_VE" ? "Lý do trả về chỉnh sửa" : "Lý do mở khóa bảng điểm"}
            required
            hint="Tối đa 500 ký tự. Lý do này sẽ được lưu vào lịch sử trạng thái và nhật ký kiểm toán."
          >
            <textarea
              className="input min-h-[100px] py-2"
              maxLength={500}
              value={reasonText}
              onChange={(e) => setReasonText(e.target.value)}
              placeholder={
                reasonAction?.mode === "TRA_VE"
                  ? "Nhập rõ nội dung sai sót hoặc yêu cầu rà soát lại điểm cho giảng viên..."
                  : "Nhập căn cứ/quyết định mở khóa bảng điểm để điều chỉnh..."
              }
              required
            />
          </Field>
        </form>
      </Modal>

      {/* Modal xem chi tiết bảng điểm & lịch sử duyệt dành cho Phòng Đào tạo / Quản trị (QLSV-74) */}
      <Modal
        open={gradeDetailOpen}
        onClose={() => setGradeDetailOpen(false)}
        title={gradeDetailRow ? `Kiểm duyệt bảng điểm: ${gradeDetailRow.ma} - ${gradeDetailRow.tenMon}` : "Kiểm duyệt bảng điểm"}
        description="Kiểm tra bảng điểm lớp học phần, thực hiện duyệt/trả sửa/công bố/khóa/mở khóa và tra cứu lịch sử chuyển trạng thái."
        size="lg"
      >
        {gradeDetailLoading ? (
          <Spinner label="Đang tải thông tin bảng điểm..." />
        ) : gradeDetailError ? (
          <Alert tone="error">{gradeDetailError}</Alert>
        ) : gradeDetailStatus ? (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3.5">
              <div className="space-y-1 text-xs">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-slate-800">Trạng thái hiện tại:</span>
                  <Badge tone={(GRADE_SHEET_STATUS_META[gradeDetailStatus.trangThaiBangDiem] || GRADE_SHEET_STATUS_META.NHAP).tone}>
                    {(GRADE_SHEET_STATUS_META[gradeDetailStatus.trangThaiBangDiem] || GRADE_SHEET_STATUS_META.NHAP).label}
                  </Badge>
                </div>
                <p className="text-slate-600">
                  Sinh viên đủ điểm: <span className="font-semibold">{gradeDetailStatus.soSinhVienDuDieuKien}/{gradeDetailStatus.soSinhVienDangHoc}</span> ·
                  Phiên bản công bố: <span className="font-semibold">v{gradeDetailStatus.phienBanCongBo}</span>
                </p>
                {gradeDetailStatus.lyDoTraVe && (
                  <p className="text-rose-700">
                    <span className="font-semibold">Lý do trả về gần nhất:</span> {gradeDetailStatus.lyDoTraVe}
                  </p>
                )}
                {gradeDetailStatus.lyDoMoKhoa && (
                  <p className="text-amber-800">
                    <span className="font-semibold">Lý do mở khóa gần nhất:</span> {gradeDetailStatus.lyDoMoKhoa}
                  </p>
                )}
              </div>

              {gradeDetailRow && (
                <div className="flex flex-wrap items-center gap-2">
                  {gradeDetailStatus.trangThaiBangDiem === "CHO_DUYET" && (
                    <>
                      <button
                        type="button"
                        className="btn-primary text-xs"
                        disabled={gradeDetailStatus.soSinhVienDuDieuKien < gradeDetailStatus.soSinhVienDangHoc || gradeDetailStatus.soSinhVienDangHoc === 0}
                        onClick={() => thucHienBangDiem(gradeDetailRow, "phe-duyet-diem", `Đã phê duyệt bảng điểm lớp ${gradeDetailRow.ma}`)}
                      >
                        <CheckCircle2 size={14} /> Phê duyệt
                      </button>
                      <button
                        type="button"
                        className="btn-secondary text-xs text-rose-700"
                        onClick={() => openReasonModal(gradeDetailRow, "TRA_VE")}
                      >
                        <RotateCcw size={14} /> Trả về chỉnh sửa
                      </button>
                    </>
                  )}
                  {gradeDetailStatus.trangThaiBangDiem === "DA_DUYET" && (
                    <>
                      <button
                        type="button"
                        className="btn-primary text-xs"
                        onClick={() => thucHienBangDiem(gradeDetailRow, "cong-bo-diem", `Đã công bố bảng điểm lớp ${gradeDetailRow.ma}`)}
                      >
                        <Megaphone size={14} /> Công bố điểm
                      </button>
                      <button
                        type="button"
                        className="btn-secondary text-xs"
                        onClick={() => thucHienBangDiem(gradeDetailRow, "khoa-diem", `Đã công bố và khóa bảng điểm lớp ${gradeDetailRow.ma}`)}
                      >
                        <Lock size={14} /> Công bố & Khóa
                      </button>
                      <button
                        type="button"
                        className="btn-secondary text-xs text-rose-700"
                        onClick={() => openReasonModal(gradeDetailRow, "TRA_VE")}
                      >
                        <RotateCcw size={14} /> Trả sửa
                      </button>
                    </>
                  )}
                  {gradeDetailStatus.trangThaiBangDiem === "DA_CONG_BO" && (
                    <>
                      <button
                        type="button"
                        className="btn-primary text-xs"
                        onClick={() => thucHienBangDiem(gradeDetailRow, "khoa-diem", `Đã khóa bảng điểm lớp ${gradeDetailRow.ma}`)}
                      >
                        <Lock size={14} /> Khóa bảng điểm
                      </button>
                      <button
                        type="button"
                        className="btn-secondary text-xs text-amber-700"
                        onClick={() => openReasonModal(gradeDetailRow, "MO_KHOA")}
                      >
                        <Unlock size={14} /> Mở khóa kèm lý do
                      </button>
                    </>
                  )}
                  {gradeDetailStatus.trangThaiBangDiem === "DA_KHOA" && (
                    <button
                      type="button"
                      className="btn-secondary text-xs text-amber-700"
                      onClick={() => openReasonModal(gradeDetailRow, "MO_KHOA")}
                    >
                      <Unlock size={14} /> Mở khóa kèm lý do
                    </button>
                  )}
                </div>
              )}
            </div>

            <div>
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
                Bảng điểm sinh viên ({gradeDetailStudents.length})
              </h4>
              {gradeDetailStudents.length === 0 ? (
                <p className="text-xs text-slate-500">Lớp chưa có sinh viên đăng ký đang theo học.</p>
              ) : (
                <div className="max-h-56 overflow-y-auto rounded-lg border border-slate-200">
                  <table className="w-full text-xs">
                    <thead className="bg-slate-50 sticky top-0">
                      <tr>
                        <th className="px-3 py-2 text-left font-medium text-slate-600">Mã SV</th>
                        <th className="px-3 py-2 text-left font-medium text-slate-600">Họ tên</th>
                        <th className="px-3 py-2 text-center font-medium text-slate-600">Quá trình</th>
                        <th className="px-3 py-2 text-center font-medium text-slate-600">Cuối kỳ</th>
                        <th className="px-3 py-2 text-center font-medium text-slate-600">Tổng kết</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {gradeDetailStudents.map((sv) => (
                        <tr key={sv.dangKyId}>
                          <td className="px-3 py-1.5 font-mono text-slate-800">{sv.maSinhVien}</td>
                          <td className="px-3 py-1.5 text-slate-800">{sv.hoTen}</td>
                          <td className="px-3 py-1.5 text-center">{formatDiem(sv.diemQuaTrinh)}</td>
                          <td className="px-3 py-1.5 text-center">{formatDiem(sv.diemCuoiKy)}</td>
                          <td className="px-3 py-1.5 text-center font-semibold text-slate-900">{formatDiem(sv.diemTongKet)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div>
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
                Lịch sử chuyển trạng thái ({gradeDetailStatus.lichSu?.length || 0})
              </h4>
              {!gradeDetailStatus.lichSu || gradeDetailStatus.lichSu.length === 0 ? (
                <p className="text-xs text-slate-500">Chưa có lịch sử chuyển trạng thái.</p>
              ) : (
                <div className="max-h-48 overflow-y-auto divide-y divide-slate-100 rounded-lg border border-slate-200 px-3">
                  {gradeDetailStatus.lichSu.map((item) => (
                    <div key={item.id} className="py-2.5 text-xs space-y-1">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="font-semibold text-slate-800">
                          {ACTION_LABEL[item.hanhDong] || item.hanhDong}:{" "}
                          <span className="font-mono text-slate-600">{item.trangThaiTruoc}</span> →{" "}
                          <span className="font-mono text-indigo-700">{item.trangThaiSau}</span>
                        </span>
                        <span className="text-slate-500">{formatDateTime(item.thoiDiem)}</span>
                      </div>
                      <p className="text-slate-600">
                        Người thực hiện: <span className="font-medium">{item.nguoiThucHienUsername || `User #${item.nguoiThucHienId || "—"}`}</span> ({item.nguoiThucHienRole || "—"})
                      </p>
                      {item.lyDo && (
                        <p className="rounded bg-slate-50 px-2 py-1 text-slate-700 border border-slate-200">
                          <span className="font-medium">Lý do:</span> {item.lyDo}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? "Cập nhật lớp học phần" : "Mở lớp học phần"}
        size="lg"
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Hủy</button>
            <button
              type="submit"
              form="course-class-form"
              className="btn-primary"
              disabled={saving || (conflictResult?.coXungDot && !conflictResult?.choPhepLuu)}
              title={conflictResult?.coXungDot && !conflictResult?.choPhepLuu ? "Bị chặn do xung đột lịch học" : undefined}
            >
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="course-class-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2"><Alert tone="error">{formError}</Alert></div>
          )}

          {conflictLoading && (
            <div className="sm:col-span-2 flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2.5 text-xs text-slate-600">
              <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent" />
              <span>Đang đối chiếu lịch giảng viên và phòng học...</span>
            </div>
          )}

          {conflictResult?.coXungDot && (
            <div className="sm:col-span-2 rounded-xl border border-rose-200 bg-rose-50/70 p-4 text-xs space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-rose-100 text-rose-700 font-bold">
                    !
                  </span>
                  <span className="font-semibold text-rose-900 text-sm">
                    Phát hiện {conflictResult.soLuongXungDot} xung đột lịch học
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  {conflictResult.xungDotGiangVien && (
                    <Badge tone="rose">Trùng lịch giảng viên</Badge>
                  )}
                  {conflictResult.xungDotPhong && (
                    <Badge tone="amber">Trùng phòng học</Badge>
                  )}
                </div>
              </div>

              <div className="space-y-1.5 divide-y divide-rose-100 rounded-lg bg-white/90 p-2.5 border border-rose-100 text-slate-700">
                {conflictResult.danhSachXungDot.map((item, idx) => (
                  <div key={idx} className={idx > 0 ? "pt-1.5" : ""}>
                    <p className="font-medium text-slate-800">
                      • {item.moTa}
                    </p>
                    <p className="text-[11px] text-slate-500 pl-2">
                      Lớp đối chiếu: <span className="font-mono text-indigo-700">{item.maLopLienQuan}</span> ({item.tenMonLienQuan || "—"}) · Phòng: {item.phong} · {item.thu === 8 ? "Chủ nhật" : `Thứ ${item.thu}`}, tiết {item.tietBatDau}-{item.tietKetThuc}
                    </p>
                  </div>
                ))}
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2 pt-1 text-[11px] text-rose-800">
                <p>
                  <span className="font-semibold">Chính sách áp dụng:</span>{" "}
                  {conflictResult.chinhSachEnforcement === "BLOCK_ON_CONFLICT"
                    ? "Chặn lưu khi phát hiện xung đột"
                    : conflictResult.chinhSachEnforcement}
                  {!conflictResult.chinhSachDaChot && (
                    <span className="italic ml-1 text-slate-600">(Chờ chủ dự án phê duyệt quy định chính thức)</span>
                  )}
                </p>
                {!conflictResult.choPhepLuu && (
                  <span className="rounded bg-rose-200/80 px-2 py-0.5 font-semibold text-rose-950">
                    Không cho phép lưu
                  </span>
                )}
              </div>
            </div>
          )}
          <Field label="Mã lớp học phần" required>
            <input
              className="input"
              value={form.ma}
              onChange={(event) => setForm({ ...form, ma: event.target.value })}
              placeholder="VD: INT201.01"
              required
            />
          </Field>
          <Field label="Môn học" required>
            <select
              className="input"
              value={form.monHocId}
              onChange={(event) => setForm({ ...form, monHocId: event.target.value })}
              required
            >
              <option value="">-- Chọn môn --</option>
              {(monHocList || []).map((mon) => (
                <option key={mon.id} value={mon.id}>
                  {mon.ma} - {mon.ten} ({mon.soTinChi} TC · {tietLyThuyet(mon.soTinChi)} tiết)
                </option>
              ))}
            </select>
          </Field>
          <Field label="Học kỳ" required>
            <select
              className="input"
              value={form.hocKyId}
              onChange={(event) => setForm({ ...form, hocKyId: event.target.value })}
              required
            >
              <option value="">-- Chọn học kỳ --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} - {hk.namHoc}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Giảng viên" required>
            <select
              className="input"
              value={form.giangVienId}
              onChange={(event) => setForm({ ...form, giangVienId: event.target.value })}
              required
            >
              <option value="">-- Chọn giảng viên --</option>
              {(giangVienList || []).map((gv) => (
                <option key={gv.id} value={gv.id}>
                  {gv.ma} - {gv.hoTen}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Phòng học" required hint="Chỉ chọn hoặc nhập phòng đang hoạt động">
            <input
              className="input font-mono uppercase"
              list="ds-phong-hoat-dong"
              value={form.phong}
              onChange={(event) => setForm({ ...form, phong: event.target.value.toUpperCase() })}
              placeholder="VD: A101"
              required
            />
            <datalist id="ds-phong-hoat-dong">
              {(activeRooms || []).map((ph) => (
                <option key={ph.id} value={ph.ma}>
                  {ph.ma} - {ph.ten} ({ph.sucChua} chỗ{ph.loaiPhong ? ", " + ph.loaiPhong : ""})
                </option>
              ))}
            </datalist>
          </Field>
          <Field label="Sĩ số tối đa" required>
            <input
              className="input"
              type="number"
              min="1"
              value={form.siSoToiDa}
              onChange={(event) => setForm({ ...form, siSoToiDa: event.target.value })}
              required
            />
          </Field>
          <Field label="Thứ học" required>
            <select
              className="input"
              value={form.thu}
              onChange={(event) => setForm({ ...form, thu: event.target.value })}
            >
              {[2, 3, 4, 5, 6, 7].map((thu) => (
                <option key={thu} value={thu}>Thứ {thu}</option>
              ))}
            </select>
          </Field>
          <Field label="Tiết bắt đầu" required>
            <input
              className="input"
              type="number"
              min="1"
              max="12"
              value={form.tietBatDau}
              onChange={(event) => setForm({ ...form, tietBatDau: event.target.value })}
              required
            />
          </Field>
          <Field label="Số tiết" required hint="Tiết bắt đầu + số tiết không vượt quá 12">
            <input
              className="input"
              type="number"
              min="1"
              max="6"
              value={form.soTiet}
              onChange={(event) => setForm({ ...form, soTiet: event.target.value })}
              required
            />
          </Field>
        </form>
      </Modal>
    </div>
  );
}
