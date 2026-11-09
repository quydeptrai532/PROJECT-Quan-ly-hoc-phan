import { useEffect, useMemo, useState } from "react";
import {
  BookMarked,
  CalendarRange,
  GitBranch,
  Info,
  Pencil,
  Plus,
  Search,
  Trash2,
  X,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { formatDate, soBuoiHoc, tietLyThuyet } from "../../lib/format";

const EMPTY_SUBJECT_FORM = { ma: "", ten: "", soTinChi: "3", khoaId: "" };

const EMPTY_COHORT_FORM = {
  ma: "",
  ten: "",
  nienKhoa: "",
  namBatDau: "2024",
  namKetThuc: "2028",
  hocKyBatDauId: "",
  hocKyKetThucId: "",
  hieuLucTu: "",
  hieuLucDen: "",
  dangHieuLuc: true,
  moTa: "",
};

const EMPTY_RELATION_FORM = {
  monHocId: "",
  monLienQuanId: "",
  loaiQuanHe: "TIEN_QUYET",
  dieuKienDat: "5.0",
  yeuCauDat: true,
  maNhomTuChon: "",
  tinChiTuChonYeuCau: "",
  khoaNhapHoc: "",
  hocKyHieuLucTuId: "",
  hocKyHieuLucDenId: "",
  hieuLucTu: "",
  hieuLucDen: "",
  dangHieuLuc: true,
  ghiChu: "",
};

const RELATION_TONE = {
  TIEN_QUYET: "indigo",
  SONG_HANH: "amber",
  TUONG_DUONG: "green",
  THAY_THE: "blue",
  NHOM_TU_CHON: "slate",
};

const RELATION_LABEL_FALLBACK = {
  TIEN_QUYET: "Tiên quyết",
  SONG_HANH: "Song hành",
  TUONG_DUONG: "Tương đương",
  THAY_THE: "Thay thế",
  NHOM_TU_CHON: "Nhóm tự chọn",
};

function formatAuditMeta(row) {
  const actor = row.nguoiCapNhatUsername || row.nguoiTaoUsername;
  const timestamp = row.capNhatLuc || row.taoLuc;
  if (!actor && !timestamp) return "Hệ thống";
  const dateText = timestamp ? new Date(timestamp).toLocaleDateString("vi-VN") : "";
  return [actor || "Hệ thống", dateText].filter(Boolean).join(" · ");
}

export default function SubjectPage({ initialTab = "mon-hoc" }) {
  const confirm = useConfirm();
  const [activeTab, setActiveTab] = useState(initialTab);

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab]);

  const { data: monHocList, loading, error, reload: reloadSubjects } = useApi("/admin/mon-hoc");
  const { data: khoaList } = useApi("/admin/khoa");
  const { data: hocKyList } = useApi("/admin/hoc-ky");
  const { data: loaiQuanHeList } = useApi("/admin/loai-quan-he-mon");
  const {
    data: cohortList,
    loading: cohortLoading,
    error: cohortLoadError,
    reload: reloadCohorts,
  } = useApi("/admin/khoa-hoc");
  const {
    data: relationList,
    loading: relationLoading,
    error: relationLoadError,
    reload: reloadRelations,
  } = useApi("/admin/quan-he-mon");

  const [search, setSearch] = useState("");
  const [selectedKhoaFilter, setSelectedKhoaFilter] = useState("");
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");

  // Subject CRUD state
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_SUBJECT_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  // Per-subject Prerequisite / Relation drawer-modal state
  const [prereqFor, setPrereqFor] = useState(null);
  const [prereqs, setPrereqs] = useState([]);
  const [prereqLoading, setPrereqLoading] = useState(false);
  const [prereqError, setPrereqError] = useState("");
  const [selectedPrereq, setSelectedPrereq] = useState("");
  const [quickLoaiQuanHe, setQuickLoaiQuanHe] = useState("TIEN_QUYET");
  const [quickDieuKienDat, setQuickDieuKienDat] = useState("5.0");
  const [quickKhoaNhapHoc, setQuickKhoaNhapHoc] = useState("");

  // Relation tab filter & modal state
  const [relationSearch, setRelationSearch] = useState("");
  const [filterRelationSubjectId, setFilterRelationSubjectId] = useState("");
  const [filterLoaiQuanHe, setFilterLoaiQuanHe] = useState("");
  const [filterRelationCohort, setFilterRelationCohort] = useState("");
  const [filterRelationSemesterId, setFilterRelationSemesterId] = useState("");
  const [filterRelationActive, setFilterRelationActive] = useState("");

  const [relationModalOpen, setRelationModalOpen] = useState(false);
  const [editingRelation, setEditingRelation] = useState(null);
  const [relationForm, setRelationForm] = useState(EMPTY_RELATION_FORM);
  const [relationFormError, setRelationFormError] = useState("");
  const [savingRelation, setSavingRelation] = useState(false);

  // Cohort tab filter & modal state
  const [cohortSearch, setCohortSearch] = useState("");
  const [filterCohortActive, setFilterCohortActive] = useState("");
  const [cohortModalOpen, setCohortModalOpen] = useState(false);
  const [editingCohort, setEditingCohort] = useState(null);
  const [cohortForm, setCohortForm] = useState(EMPTY_COHORT_FORM);
  const [cohortFormError, setCohortFormError] = useState("");
  const [savingCohort, setSavingCohort] = useState(false);

  const rows = useMemo(() => {
    const list = monHocList || [];
    const term = search.trim().toLowerCase();
    return list.filter((item) => {
      if (selectedKhoaFilter && String(item.khoaId) !== String(selectedKhoaFilter)) {
        return false;
      }
      if (!term) return true;
      return (
        item.ma.toLowerCase().includes(term) ||
        item.ten.toLowerCase().includes(term) ||
        (item.khoaTen || "").toLowerCase().includes(term)
      );
    });
  }, [monHocList, search, selectedKhoaFilter]);

  const filteredRelations = useMemo(() => {
    const list = relationList || [];
    const term = relationSearch.trim().toLowerCase();
    return list.filter((item) => {
      if (
        filterRelationSubjectId &&
        String(item.monHocId) !== String(filterRelationSubjectId) &&
        String(item.monLienQuanId) !== String(filterRelationSubjectId)
      ) {
        return false;
      }
      if (filterLoaiQuanHe && item.loaiQuanHe !== filterLoaiQuanHe) {
        return false;
      }
      if (
        filterRelationCohort &&
        (item.khoaNhapHoc || "").toLowerCase() !== filterRelationCohort.toLowerCase()
      ) {
        return false;
      }
      if (filterRelationSemesterId) {
        const semId = String(filterRelationSemesterId);
        if (
          String(item.hocKyHieuLucTuId || "") !== semId &&
          String(item.hocKyHieuLucDenId || "") !== semId
        ) {
          return false;
        }
      }
      if (filterRelationActive !== "") {
        const expected = filterRelationActive === "true";
        if (Boolean(item.dangHieuLuc) !== expected) {
          return false;
        }
      }
      if (!term) return true;
      return (
        (item.maMonChinh || "").toLowerCase().includes(term) ||
        (item.tenMonChinh || "").toLowerCase().includes(term) ||
        (item.maMon || "").toLowerCase().includes(term) ||
        (item.tenMon || "").toLowerCase().includes(term) ||
        (item.maNhomTuChon || "").toLowerCase().includes(term) ||
        (item.khoaNhapHoc || "").toLowerCase().includes(term) ||
        (item.ghiChu || "").toLowerCase().includes(term)
      );
    });
  }, [
    relationList,
    relationSearch,
    filterRelationSubjectId,
    filterLoaiQuanHe,
    filterRelationCohort,
    filterRelationSemesterId,
    filterRelationActive,
  ]);

  const filteredCohorts = useMemo(() => {
    const list = cohortList || [];
    const term = cohortSearch.trim().toLowerCase();
    return list.filter((item) => {
      if (filterCohortActive !== "") {
        const expected = filterCohortActive === "true";
        if (Boolean(item.dangHieuLuc) !== expected) {
          return false;
        }
      }
      if (!term) return true;
      return (
        item.ma.toLowerCase().includes(term) ||
        item.ten.toLowerCase().includes(term) ||
        (item.nienKhoa || "").toLowerCase().includes(term) ||
        (item.moTa || "").toLowerCase().includes(term)
      );
    });
  }, [cohortList, cohortSearch, filterCohortActive]);

  // --- Subject Handlers ---
  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_SUBJECT_FORM });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    setForm({
      ma: row.ma,
      ten: row.ten,
      soTinChi: String(row.soTinChi),
      khoaId: String(row.khoaId || ""),
    });
    setFormError("");
    setModalOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    try {
      const payload = {
        ma: form.ma.trim(),
        ten: form.ten.trim(),
        soTinChi: Number(form.soTinChi),
        khoaId: form.khoaId ? Number(form.khoaId) : null,
      };
      if (editing) {
        await api.put(`/admin/mon-hoc/${editing.id}`, payload);
        setMessage("Cập nhật môn học thành công");
      } else {
        await api.post("/admin/mon-hoc", payload);
        setMessage("Thêm môn học thành công");
      }
      setModalOpen(false);
      reloadSubjects();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa môn học",
      message: `Bạn có chắc muốn xóa môn ${row.ma} - ${row.ten}?`,
      detail: "Không thể xóa môn học đang được tham chiếu bởi chương trình đào tạo, lớp học phần hoặc quan hệ môn.",
      confirmLabel: "Xóa môn học",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    try {
      await api.delete(`/admin/mon-hoc/${row.id}`);
      setMessage("Đã xóa môn học");
      reloadSubjects();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  // --- Quick Prerequisite / Relation Modal for a Subject ---
  const moTienQuyet = async (row) => {
    setPrereqFor(row);
    setPrereqs([]);
    setSelectedPrereq("");
    setQuickLoaiQuanHe("TIEN_QUYET");
    setQuickDieuKienDat("5.0");
    setQuickKhoaNhapHoc("");
    setPrereqError("");
    setPrereqLoading(true);
    try {
      const { data } = await api.get(`/admin/mon-hoc/${row.id}/tien-quyet`);
      setPrereqs(data);
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    } finally {
      setPrereqLoading(false);
    }
  };

  const themTienQuyet = async () => {
    if (!selectedPrereq) return;
    setPrereqError("");
    try {
      await api.post(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`, {
        monTienQuyetId: Number(selectedPrereq),
        monLienQuanId: Number(selectedPrereq),
        loaiQuanHe: quickLoaiQuanHe,
        dieuKienDat: quickDieuKienDat !== "" ? Number(quickDieuKienDat) : 5.0,
        yeuCauDat: true,
        khoaNhapHoc: quickKhoaNhapHoc.trim() || null,
        dangHieuLuc: true,
      });
      setSelectedPrereq("");
      const { data } = await api.get(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`);
      setPrereqs(data);
      reloadRelations();
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    }
  };

  const xoaTienQuyet = async (lienKet) => {
    const daXacNhan = await confirm({
      title: "Bỏ quan hệ môn",
      message: `Bỏ quan hệ ${lienKet.tenLoaiQuanHe || "tiên quyết"} với môn ${lienKet.maMon} - ${lienKet.tenMon} khỏi ${prereqFor.ma}?`,
      detail: "Hệ thống sẽ từ chối xóa nếu quan hệ này đang được chương trình đào tạo hoặc lớp học phần sử dụng.",
      confirmLabel: "Bỏ môn",
      tone: "warning",
    });
    if (!daXacNhan) return;
    setPrereqError("");
    try {
      await api.delete(`/admin/mon-hoc/${prereqFor.id}/tien-quyet/${lienKet.id}`);
      const { data } = await api.get(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`);
      setPrereqs(data);
      reloadRelations();
    } catch (err) {
      setPrereqError(getErrorMessage(err));
    }
  };

  const luaChonTienQuyet = (monHocList || []).filter(
    (item) =>
      item.id !== prereqFor?.id &&
      !prereqs.some(
        (lienKet) =>
          (lienKet.monLienQuanId || lienKet.monTienQuyetId) === item.id &&
          (lienKet.loaiQuanHe || "TIEN_QUYET") === quickLoaiQuanHe
      )
  );

  // --- Full Subject Relation CRUD Handlers ---
  const openCreateRelation = (defaultSubjectId = "") => {
    setEditingRelation(null);
    setRelationForm({
      ...EMPTY_RELATION_FORM,
      monHocId: defaultSubjectId ? String(defaultSubjectId) : "",
    });
    setRelationFormError("");
    setRelationModalOpen(true);
  };

  const openEditRelation = (row) => {
    setEditingRelation(row);
    setRelationForm({
      monHocId: String(row.monHocId || ""),
      monLienQuanId: String(row.monLienQuanId || row.monTienQuyetId || ""),
      loaiQuanHe: row.loaiQuanHe || "TIEN_QUYET",
      dieuKienDat: row.dieuKienDat != null ? String(row.dieuKienDat) : "5.0",
      yeuCauDat: row.yeuCauDat !== false,
      maNhomTuChon: row.maNhomTuChon || "",
      tinChiTuChonYeuCau: row.tinChiTuChonYeuCau != null ? String(row.tinChiTuChonYeuCau) : "",
      khoaNhapHoc: row.khoaNhapHoc || "",
      hocKyHieuLucTuId: row.hocKyHieuLucTuId ? String(row.hocKyHieuLucTuId) : "",
      hocKyHieuLucDenId: row.hocKyHieuLucDenId ? String(row.hocKyHieuLucDenId) : "",
      hieuLucTu: row.hieuLucTu || "",
      hieuLucDen: row.hieuLucDen || "",
      dangHieuLuc: row.dangHieuLuc !== false,
      ghiChu: row.ghiChu || "",
    });
    setRelationFormError("");
    setRelationModalOpen(true);
  };

  const submitRelation = async (event) => {
    event.preventDefault();
    setSavingRelation(true);
    setRelationFormError("");
    setMessage("");
    try {
      const payload = {
        monHocId: relationForm.monHocId ? Number(relationForm.monHocId) : null,
        monLienQuanId: relationForm.monLienQuanId ? Number(relationForm.monLienQuanId) : null,
        monTienQuyetId: relationForm.monLienQuanId ? Number(relationForm.monLienQuanId) : null,
        loaiQuanHe: relationForm.loaiQuanHe,
        dieuKienDat:
          relationForm.dieuKienDat !== "" && relationForm.dieuKienDat != null
            ? Number(relationForm.dieuKienDat)
            : null,
        yeuCauDat: Boolean(relationForm.yeuCauDat),
        maNhomTuChon: relationForm.maNhomTuChon.trim() || null,
        tinChiTuChonYeuCau:
          relationForm.tinChiTuChonYeuCau !== ""
            ? Number(relationForm.tinChiTuChonYeuCau)
            : null,
        khoaNhapHoc: relationForm.khoaNhapHoc.trim() || null,
        hocKyHieuLucTuId: relationForm.hocKyHieuLucTuId
          ? Number(relationForm.hocKyHieuLucTuId)
          : null,
        hocKyHieuLucDenId: relationForm.hocKyHieuLucDenId
          ? Number(relationForm.hocKyHieuLucDenId)
          : null,
        hieuLucTu: relationForm.hieuLucTu || null,
        hieuLucDen: relationForm.hieuLucDen || null,
        dangHieuLuc: Boolean(relationForm.dangHieuLuc),
        ghiChu: relationForm.ghiChu.trim() || null,
      };
      if (editingRelation) {
        await api.put(`/admin/quan-he-mon/${editingRelation.id}`, payload);
        setMessage("Cập nhật quan hệ môn học thành công");
      } else {
        await api.post("/admin/quan-he-mon", payload);
        setMessage("Thêm quan hệ môn học thành công");
      }
      setRelationModalOpen(false);
      reloadRelations();
      if (prereqFor) {
        const { data } = await api.get(`/admin/mon-hoc/${prereqFor.id}/tien-quyet`);
        setPrereqs(data);
      }
    } catch (err) {
      setRelationFormError(getErrorMessage(err));
    } finally {
      setSavingRelation(false);
    }
  };

  const removeRelation = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa quan hệ môn học",
      message: `Xóa quan hệ ${row.tenLoaiQuanHe || RELATION_LABEL_FALLBACK[row.loaiQuanHe]} giữa ${row.maMonChinh} và ${row.maMon}?`,
      detail:
        "Không thể xóa quan hệ đang được chương trình đào tạo hoặc lớp học phần sử dụng.",
      confirmLabel: "Xóa quan hệ",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    try {
      await api.delete(`/admin/quan-he-mon/${row.id}`);
      setMessage("Đã xóa quan hệ môn học");
      reloadRelations();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  // --- Cohort CRUD Handlers ---
  const openCreateCohort = () => {
    setEditingCohort(null);
    setCohortForm({ ...EMPTY_COHORT_FORM });
    setCohortFormError("");
    setCohortModalOpen(true);
  };

  const openEditCohort = (row) => {
    setEditingCohort(row);
    setCohortForm({
      ma: row.ma || "",
      ten: row.ten || "",
      nienKhoa: row.nienKhoa || "",
      namBatDau: row.namBatDau != null ? String(row.namBatDau) : "",
      namKetThuc: row.namKetThuc != null ? String(row.namKetThuc) : "",
      hocKyBatDauId: row.hocKyBatDauId ? String(row.hocKyBatDauId) : "",
      hocKyKetThucId: row.hocKyKetThucId ? String(row.hocKyKetThucId) : "",
      hieuLucTu: row.hieuLucTu || "",
      hieuLucDen: row.hieuLucDen || "",
      dangHieuLuc: row.dangHieuLuc !== false,
      moTa: row.moTa || "",
    });
    setCohortFormError("");
    setCohortModalOpen(true);
  };

  const submitCohort = async (event) => {
    event.preventDefault();
    setSavingCohort(true);
    setCohortFormError("");
    setMessage("");
    try {
      const payload = {
        ma: cohortForm.ma.trim(),
        ten: cohortForm.ten.trim(),
        nienKhoa: cohortForm.nienKhoa.trim(),
        namBatDau: Number(cohortForm.namBatDau),
        namKetThuc: Number(cohortForm.namKetThuc),
        hocKyBatDauId: cohortForm.hocKyBatDauId ? Number(cohortForm.hocKyBatDauId) : null,
        hocKyKetThucId: cohortForm.hocKyKetThucId ? Number(cohortForm.hocKyKetThucId) : null,
        hieuLucTu: cohortForm.hieuLucTu || null,
        hieuLucDen: cohortForm.hieuLucDen || null,
        dangHieuLuc: Boolean(cohortForm.dangHieuLuc),
        moTa: cohortForm.moTa.trim() || null,
      };
      if (editingCohort) {
        await api.put(`/admin/khoa-hoc/${editingCohort.id}`, payload);
        setMessage("Cập nhật khóa học / niên khóa thành công");
      } else {
        await api.post("/admin/khoa-hoc", payload);
        setMessage("Thêm khóa học / niên khóa thành công");
      }
      setCohortModalOpen(false);
      reloadCohorts();
    } catch (err) {
      setCohortFormError(getErrorMessage(err));
    } finally {
      setSavingCohort(false);
    }
  };

  const removeCohort = async (row) => {
    const daXacNhan = await confirm({
      title: "Xóa khóa học / niên khóa",
      message: `Bạn có chắc muốn xóa khóa ${row.ma} - ${row.ten} (${row.nienKhoa})?`,
      detail:
        "Không thể xóa khóa học đang được chương trình đào tạo, quan hệ môn hoặc sinh viên tham chiếu.",
      confirmLabel: "Xóa khóa học",
      tone: "danger",
    });
    if (!daXacNhan) return;
    setActionError("");
    setMessage("");
    try {
      await api.delete(`/admin/khoa-hoc/${row.id}`);
      setMessage("Đã xóa khóa học / niên khóa");
      reloadCohorts();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        icon={BookMarked}
        title="Môn học, Khóa học & Quan hệ môn"
        description="Quản trị danh mục môn học (1 TC = 15 tiết = 5 buổi), khóa học/niên khóa và quan hệ môn (tiên quyết, song hành, tương đương, thay thế, nhóm tự chọn)."
        actions={
          <div className="flex flex-wrap gap-2">
            {activeTab === "quan-he-mon" && (
              <button
                type="button"
                className="btn-primary"
                onClick={() => openCreateRelation()}
              >
                <Plus size={16} /> Thêm quan hệ môn
              </button>
            )}
            {activeTab === "khoa-hoc" && (
              <button type="button" className="btn-primary" onClick={openCreateCohort}>
                <Plus size={16} /> Thêm khóa học
              </button>
            )}
            <button type="button" className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> Thêm môn học
            </button>
          </div>
        }
      />

      <div className="mb-4 flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        <button
          type="button"
          onClick={() => setActiveTab("mon-hoc")}
          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition ${
            activeTab === "mon-hoc"
              ? "bg-slate-900 text-white shadow-sm"
              : "bg-white text-slate-600 hover:bg-slate-100"
          }`}
        >
          <BookMarked size={16} />
          Danh mục môn học ({(monHocList || []).length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab("quan-he-mon")}
          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition ${
            activeTab === "quan-he-mon"
              ? "bg-slate-900 text-white shadow-sm"
              : "bg-white text-slate-600 hover:bg-slate-100"
          }`}
        >
          <GitBranch size={16} />
          Quan hệ giữa các môn ({(relationList || []).length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab("khoa-hoc")}
          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition ${
            activeTab === "khoa-hoc"
              ? "bg-slate-900 text-white shadow-sm"
              : "bg-white text-slate-600 hover:bg-slate-100"
          }`}
        >
          <CalendarRange size={16} />
          Khóa học / Niên khóa ({(cohortList || []).length})
        </button>
      </div>

      <div className="mb-4 space-y-3">
        {message && (
          <Alert tone="success" onClose={() => setMessage("")}>
            {message}
          </Alert>
        )}
        {actionError && (
          <Alert tone="error" onClose={() => setActionError("")}>
            {actionError}
          </Alert>
        )}
        {error && <Alert tone="error">{error}</Alert>}
        {relationLoadError && activeTab === "quan-he-mon" && (
          <Alert tone="error">{relationLoadError}</Alert>
        )}
        {cohortLoadError && activeTab === "khoa-hoc" && (
          <Alert tone="error">{cohortLoadError}</Alert>
        )}
      </div>

      {/* TAB 1: DANH MUC MON HOC */}
      {activeTab === "mon-hoc" && (
        <div className="card overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center">
              <div className="relative w-full sm:max-w-xs">
                <Search
                  size={16}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  className="input pl-9"
                  placeholder="Tìm theo mã, tên môn, khoa..."
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
              <select
                className="input sm:w-56"
                value={selectedKhoaFilter}
                onChange={(event) => setSelectedKhoaFilter(event.target.value)}
              >
                <option value="">Tất cả khoa</option>
                {(khoaList || []).map((khoa) => (
                  <option key={khoa.id} value={khoa.id}>
                    {khoa.ma} - {khoa.ten}
                  </option>
                ))}
              </select>
            </div>
            <p className="text-sm text-slate-500">{rows.length} môn học</p>
          </div>

          {loading ? (
            <Spinner />
          ) : rows.length === 0 ? (
            <EmptyState
              icon={BookMarked}
              title="Chưa có môn học"
              description="Thêm môn học để mở lớp học phần."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px]">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="th">Mã môn</th>
                    <th className="th">Tên môn học</th>
                    <th className="th text-center">Tín chỉ</th>
                    <th className="th text-center">Tiết lý thuyết</th>
                    <th className="th text-center">Số buổi (3 tiết/buổi)</th>
                    <th className="th">Khoa</th>
                    <th className="th">Tạo / Cập nhật</th>
                    <th className="th text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map((row) => (
                    <tr key={row.id} className="hover:bg-slate-50/60">
                      <td className="td font-medium text-slate-900">{row.ma}</td>
                      <td className="td">{row.ten}</td>
                      <td className="td text-center">{row.soTinChi}</td>
                      <td className="td text-center">{tietLyThuyet(row.soTinChi)}</td>
                      <td className="td text-center">{soBuoiHoc(row.soTinChi)}</td>
                      <td className="td text-slate-500">{row.khoaTen}</td>
                      <td className="td text-xs text-slate-500">{formatAuditMeta(row)}</td>
                      <td className="td">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            className="btn-secondary px-2.5 py-1.5 text-xs"
                            onClick={() => moTienQuyet(row)}
                          >
                            Tiên quyết
                          </button>
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                            onClick={() => openEdit(row)}
                            title="Sửa"
                          >
                            <Pencil size={15} />
                          </button>
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
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: QUAN HE GIUA CAC MON */}
      {activeTab === "quan-he-mon" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50/80 p-4 text-xs text-amber-900">
            <div className="flex items-start gap-2.5">
              <Info size={16} className="mt-0.5 shrink-0 text-amber-700" />
              <div className="space-y-1">
                <p className="font-semibold text-amber-950">
                  Trạng thái quy chế áp dụng cho từng loại quan hệ môn học
                </p>
                <p>
                  • <strong>Tiên quyết (TIEN_QUYET)</strong>: Đã chốt quy chế — hệ thống chặn đăng ký học phần nếu sinh viên chưa đạt môn tiên quyết (điểm tổng kết đã công bố ≥ ngưỡng đạt cấu hình, mặc định 5.0) trong phạm vi khóa/học kỳ/thời gian hiệu lực. Không cho phép tạo chu trình tiên quyết.
                </p>
                <p>
                  • <strong>Song hành, Tương đương, Thay thế, Nhóm tự chọn</strong>: Đã lưu cấu hình có cấu trúc và đối chiếu khi kiểm tra điều kiện đăng ký (<code>GET /api/dang-ky/kiem-tra-quan-he</code>), nhưng <strong>không tự động chặn hoặc tự động quy đổi thay thế</strong> cho đến khi Chủ dự án / Phòng Đào tạo chốt quy chế học vụ trên Jira.
                </p>
              </div>
            </div>
          </div>

          <div className="card overflow-hidden">
            <div className="space-y-3 border-b border-slate-200 p-4">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div className="relative w-full lg:max-w-sm">
                  <Search
                    size={16}
                    className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    className="input pl-9"
                    placeholder="Tìm theo mã môn, tên môn, nhóm tự chọn, khóa..."
                    value={relationSearch}
                    onChange={(event) => setRelationSearch(event.target.value)}
                  />
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm text-slate-500">
                    {filteredRelations.length} quan hệ môn
                  </span>
                  <button
                    type="button"
                    className="btn-primary text-xs"
                    onClick={() => openCreateRelation()}
                  >
                    <Plus size={15} /> Thêm quan hệ môn
                  </button>
                </div>
              </div>

              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
                <select
                  className="input text-xs"
                  value={filterRelationSubjectId}
                  onChange={(event) => setFilterRelationSubjectId(event.target.value)}
                >
                  <option value="">Tất cả môn học</option>
                  {(monHocList || []).map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.ma} - {m.ten}
                    </option>
                  ))}
                </select>
                <select
                  className="input text-xs"
                  value={filterLoaiQuanHe}
                  onChange={(event) => setFilterLoaiQuanHe(event.target.value)}
                >
                  <option value="">Tất cả loại quan hệ</option>
                  {(loaiQuanHeList || []).map((loai) => {
                    const code = loai.loaiQuanHe || loai.ma;
                    const label = loai.tenLoaiQuanHe || loai.ten;
                    return (
                      <option key={code} value={code}>
                        {label} ({code})
                      </option>
                    );
                  })}
                </select>
                <select
                  className="input text-xs"
                  value={filterRelationCohort}
                  onChange={(event) => setFilterRelationCohort(event.target.value)}
                >
                  <option value="">Tất cả khóa học</option>
                  {(cohortList || []).map((c) => (
                    <option key={c.id} value={c.ma}>
                      {c.ma} - {c.ten}
                    </option>
                  ))}
                </select>
                <select
                  className="input text-xs"
                  value={filterRelationSemesterId}
                  onChange={(event) => setFilterRelationSemesterId(event.target.value)}
                >
                  <option value="">Tất cả đợt / học kỳ</option>
                  {(hocKyList || []).map((hk) => (
                    <option key={hk.id} value={hk.id}>
                      {hk.ten} ({hk.namHoc})
                    </option>
                  ))}
                </select>
                <select
                  className="input text-xs"
                  value={filterRelationActive}
                  onChange={(event) => setFilterRelationActive(event.target.value)}
                >
                  <option value="">Tất cả trạng thái</option>
                  <option value="true">Đang hiệu lực</option>
                  <option value="false">Ngừng hiệu lực</option>
                </select>
              </div>
            </div>

            {relationLoading ? (
              <Spinner />
            ) : filteredRelations.length === 0 ? (
              <EmptyState
                icon={GitBranch}
                title="Chưa có quan hệ môn học phù hợp"
                description="Thêm quan hệ tiên quyết, song hành, tương đương, thay thế hoặc nhóm tự chọn giữa các môn."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1080px]">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="th">Môn học chính</th>
                      <th className="th">Loại quan hệ</th>
                      <th className="th">Môn liên quan</th>
                      <th className="th">Điều kiện đạt / Tự chọn</th>
                      <th className="th">Khóa & Thời gian hiệu lực</th>
                      <th className="th">Người tạo / Cập nhật</th>
                      <th className="th text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredRelations.map((rel) => (
                      <tr key={rel.id} className="hover:bg-slate-50/60">
                        <td className="td">
                          <p className="font-medium text-slate-900">{rel.maMonChinh}</p>
                          <p className="text-xs text-slate-500">{rel.tenMonChinh}</p>
                        </td>
                        <td className="td">
                          <div className="flex flex-col items-start gap-1">
                            <Badge tone={RELATION_TONE[rel.loaiQuanHe] || "slate"}>
                              {rel.tenLoaiQuanHe || RELATION_LABEL_FALLBACK[rel.loaiQuanHe]}
                            </Badge>
                            <span className="text-[11px] text-slate-500">
                              {rel.quyCheDaChot ? "Đã chốt quy chế" : "Chờ chốt quy chế"}
                            </span>
                          </div>
                        </td>
                        <td className="td">
                          <p className="font-medium text-slate-900">{rel.maMon}</p>
                          <p className="text-xs text-slate-500">
                            {rel.tenMon} ({rel.soTinChi} TC)
                          </p>
                        </td>
                        <td className="td text-xs text-slate-700">
                          {rel.yeuCauDat ? (
                            <p>Điểm đạt ≥ {rel.dieuKienDat ?? 5.0}</p>
                          ) : (
                            <p className="text-slate-500">Không yêu cầu điểm đạt trước</p>
                          )}
                          {rel.maNhomTuChon && (
                            <p className="mt-0.5 text-indigo-700">
                              Nhóm: <strong>{rel.maNhomTuChon}</strong>
                              {rel.tinChiTuChonYeuCau != null
                                ? ` (${rel.tinChiTuChonYeuCau} TC)`
                                : ""}
                            </p>
                          )}
                          {rel.ghiChu && (
                            <p className="mt-0.5 text-slate-500">Ghi chú: {rel.ghiChu}</p>
                          )}
                        </td>
                        <td className="td text-xs text-slate-600">
                          <div className="space-y-0.5">
                            <p>
                              Khóa: <strong>{rel.khoaNhapHoc || "Tất cả khóa"}</strong>
                            </p>
                            {(rel.hocKyHieuLucTuTen || rel.hocKyHieuLucDenTen) && (
                              <p>
                                Học kỳ: {rel.hocKyHieuLucTuTen || "Đầu"} →{" "}
                                {rel.hocKyHieuLucDenTen || "Về sau"}
                              </p>
                            )}
                            {(rel.hieuLucTu || rel.hieuLucDen) && (
                              <p>
                                Ngày: {formatDate(rel.hieuLucTu) || "—"} →{" "}
                                {formatDate(rel.hieuLucDen) || "—"}
                              </p>
                            )}
                            <Badge tone={rel.dangHieuLuc ? "green" : "red"}>
                              {rel.dangHieuLuc ? "Đang hiệu lực" : "Ngừng hiệu lực"}
                            </Badge>
                          </div>
                        </td>
                        <td className="td text-xs text-slate-500">{formatAuditMeta(rel)}</td>
                        <td className="td">
                          <div className="flex justify-end gap-1">
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                              onClick={() => openEditRelation(rel)}
                              title="Sửa quan hệ"
                            >
                              <Pencil size={15} />
                            </button>
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50"
                              onClick={() => removeRelation(rel)}
                              title="Xóa quan hệ"
                            >
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: KHOA HOC / NIEN KHOA */}
      {activeTab === "khoa-hoc" && (
        <div className="card overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center">
              <div className="relative w-full sm:max-w-xs">
                <Search
                  size={16}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  className="input pl-9"
                  placeholder="Tìm theo mã khóa, tên khóa, niên khóa..."
                  value={cohortSearch}
                  onChange={(event) => setCohortSearch(event.target.value)}
                />
              </div>
              <select
                className="input sm:w-52"
                value={filterCohortActive}
                onChange={(event) => setFilterCohortActive(event.target.value)}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="true">Đang hiệu lực</option>
                <option value="false">Ngừng hiệu lực</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-slate-500">{filteredCohorts.length} khóa học</span>
              <button type="button" className="btn-primary text-xs" onClick={openCreateCohort}>
                <Plus size={15} /> Thêm khóa học
              </button>
            </div>
          </div>

          {cohortLoading ? (
            <Spinner />
          ) : filteredCohorts.length === 0 ? (
            <EmptyState
              icon={CalendarRange}
              title="Chưa có khóa học / niên khóa"
              description="Thêm khóa đào tạo (VD: K2024 · 2024-2028) để gắn với chương trình đào tạo và điều kiện môn học."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px]">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="th">Mã khóa</th>
                    <th className="th">Tên khóa</th>
                    <th className="th">Niên khóa</th>
                    <th className="th">Học kỳ áp dụng</th>
                    <th className="th">Thời gian hiệu lực</th>
                    <th className="th">Người tạo / Cập nhật</th>
                    <th className="th text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredCohorts.map((cohort) => (
                    <tr key={cohort.id} className="hover:bg-slate-50/60">
                      <td className="td font-semibold text-slate-900">{cohort.ma}</td>
                      <td className="td">
                        <p className="font-medium text-slate-800">{cohort.ten}</p>
                        {cohort.moTa && (
                          <p className="text-xs text-slate-500">{cohort.moTa}</p>
                        )}
                      </td>
                      <td className="td text-sm text-slate-700">
                        {cohort.nienKhoa} ({cohort.namBatDau} - {cohort.namKetThuc})
                      </td>
                      <td className="td text-xs text-slate-600">
                        {cohort.hocKyBatDauTen || "—"} → {cohort.hocKyKetThucTen || "—"}
                      </td>
                      <td className="td text-xs text-slate-600">
                        <p>
                          {formatDate(cohort.hieuLucTu) || "—"} →{" "}
                          {formatDate(cohort.hieuLucDen) || "—"}
                        </p>
                        <Badge tone={cohort.dangHieuLuc ? "green" : "red"}>
                          {cohort.dangHieuLuc ? "Đang hiệu lực" : "Ngừng hiệu lực"}
                        </Badge>
                      </td>
                      <td className="td text-xs text-slate-500">{formatAuditMeta(cohort)}</td>
                      <td className="td">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100"
                            onClick={() => openEditCohort(cohort)}
                            title="Sửa khóa học"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50"
                            onClick={() => removeCohort(cohort)}
                            title="Xóa khóa học"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* MODAL 1: THEM / SUA MON HOC */}
      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? "Cập nhật môn học" : "Thêm môn học"}
        footer={
          <>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setModalOpen(false)}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="subject-form"
              className="btn-primary"
              disabled={saving}
            >
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="subject-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2">
              <Alert tone="error">{formError}</Alert>
            </div>
          )}
          <Field label="Mã môn học" required>
            <input
              className="input"
              value={form.ma}
              onChange={(event) => setForm({ ...form, ma: event.target.value })}
              placeholder="VD: INT201"
              required
            />
          </Field>
          <Field
            label="Số tín chỉ"
            required
            hint={`1 tín chỉ = 15 tiết lý thuyết = 5 buổi (3 tiết/buổi) · ${tietLyThuyet(form.soTinChi)} tiết · ${soBuoiHoc(form.soTinChi)} buổi`}
          >
            <input
              className="input"
              type="number"
              min="1"
              max="10"
              value={form.soTinChi}
              onChange={(event) => setForm({ ...form, soTinChi: event.target.value })}
              required
            />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Tên môn học" required>
              <input
                className="input"
                value={form.ten}
                onChange={(event) => setForm({ ...form, ten: event.target.value })}
                required
              />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <Field label="Khoa" required>
              <select
                className="input"
                value={form.khoaId}
                onChange={(event) => setForm({ ...form, khoaId: event.target.value })}
                required
              >
                <option value="">-- Chọn khoa --</option>
                {(khoaList || []).map((khoa) => (
                  <option key={khoa.id} value={khoa.id}>
                    {khoa.ma} - {khoa.ten}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </form>
      </Modal>

      {/* MODAL 2: MON TIEN QUYET & QUAN HE MON CUA 1 MON */}
      <Modal
        open={Boolean(prereqFor)}
        onClose={() => setPrereqFor(null)}
        title="Môn tiên quyết & Quan hệ môn"
        description={
          prereqFor
            ? `${prereqFor.ma} - ${prereqFor.ten}. Quản lý các môn tiên quyết, song hành, tương đương, thay thế hoặc nhóm tự chọn.`
            : ""
        }
        footer={
          <div className="flex w-full items-center justify-between">
            <button
              type="button"
              className="btn-secondary text-xs"
              onClick={() => {
                const subjectId = prereqFor?.id;
                setPrereqFor(null);
                setActiveTab("quan-he-mon");
                openCreateRelation(subjectId);
              }}
            >
              Cấu hình nâng cao (đợt hiệu lực / nhóm tự chọn)
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setPrereqFor(null)}
            >
              Đóng
            </button>
          </div>
        }
      >
        {prereqError && (
          <Alert tone="error" className="mb-3">
            {prereqError}
          </Alert>
        )}
        {prereqLoading ? (
          <Spinner label="Đang tải môn tiên quyết..." />
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {prereqs.length === 0 ? (
                <p className="text-sm text-slate-500">Chưa cấu hình môn tiên quyết / quan hệ môn.</p>
              ) : (
                prereqs.map((lienKet) => (
                  <span
                    key={lienKet.id}
                    className="inline-flex items-center gap-2 rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700"
                  >
                    <span className="font-semibold">
                      [{lienKet.tenLoaiQuanHe || RELATION_LABEL_FALLBACK[lienKet.loaiQuanHe] || "Tiên quyết"}]
                    </span>
                    <span>
                      {lienKet.maMon} - {lienKet.tenMon}
                    </span>
                    {lienKet.dieuKienDat != null && lienKet.yeuCauDat !== false && (
                      <span className="text-xs text-indigo-500">
                        (≥ {lienKet.dieuKienDat})
                      </span>
                    )}
                    {lienKet.khoaNhapHoc && (
                      <span className="text-xs text-indigo-500">
                        · {lienKet.khoaNhapHoc}
                      </span>
                    )}
                    <button
                      type="button"
                      onClick={() => xoaTienQuyet(lienKet)}
                      className="rounded-full p-0.5 hover:bg-indigo-200"
                      title="Bỏ môn tiên quyết"
                    >
                      <X size={13} />
                    </button>
                  </span>
                ))
              )}
            </div>

            <div className="mt-5 space-y-3 rounded-xl border border-slate-200 bg-slate-50/70 p-3">
              <div className="grid gap-2 sm:grid-cols-3">
                <select
                  className="input text-xs"
                  value={quickLoaiQuanHe}
                  onChange={(event) => setQuickLoaiQuanHe(event.target.value)}
                >
                  {(loaiQuanHeList || [
                    { ma: "TIEN_QUYET", ten: "Tiên quyết" },
                    { ma: "SONG_HANH", ten: "Song hành" },
                    { ma: "TUONG_DUONG", ten: "Tương đương" },
                    { ma: "THAY_THE", ten: "Thay thế" },
                    { ma: "NHOM_TU_CHON", ten: "Nhóm tự chọn" },
                  ]).map((loai) => {
                    const code = loai.loaiQuanHe || loai.ma;
                    const label = loai.tenLoaiQuanHe || loai.ten;
                    return (
                      <option key={code} value={code}>
                        Loại: {label}
                      </option>
                    );
                  })}
                </select>
                <input
                  className="input text-xs"
                  type="number"
                  step="0.1"
                  min="0"
                  max="10"
                  value={quickDieuKienDat}
                  onChange={(event) => setQuickDieuKienDat(event.target.value)}
                  placeholder="Điểm đạt (VD: 5.0)"
                />
                <input
                  className="input text-xs"
                  value={quickKhoaNhapHoc}
                  onChange={(event) => setQuickKhoaNhapHoc(event.target.value)}
                  placeholder="Khóa áp dụng (VD: K2024)"
                />
              </div>

              <div className="flex flex-col gap-2 sm:flex-row">
                <select
                  className="input flex-1"
                  value={selectedPrereq}
                  onChange={(event) => setSelectedPrereq(event.target.value)}
                >
                  <option value="">-- Chọn môn tiên quyết --</option>
                  {luaChonTienQuyet.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.ma} - {item.ten}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={themTienQuyet}
                  disabled={!selectedPrereq}
                >
                  <Plus size={16} /> Thêm
                </button>
              </div>
            </div>
            <p className="mt-2 text-xs text-slate-500">
              Hệ thống tự động chặn đăng ký nếu sinh viên chưa đạt tất cả môn tiên quyết đang hiệu lực, đồng thời kiểm tra chống tự tham chiếu và chu trình tiên quyết.
            </p>
          </>
        )}
      </Modal>

      {/* MODAL 3: THEM / SUA QUAN HE MON HOC DAY DU */}
      <Modal
        open={relationModalOpen}
        onClose={() => setRelationModalOpen(false)}
        title={editingRelation ? "Cập nhật quan hệ môn học" : "Thêm quan hệ môn học"}
        footer={
          <>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setRelationModalOpen(false)}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="relation-form"
              className="btn-primary"
              disabled={savingRelation}
            >
              {savingRelation ? "Đang lưu..." : "Lưu quan hệ"}
            </button>
          </>
        }
      >
        <form id="relation-form" className="grid gap-3 sm:grid-cols-2" onSubmit={submitRelation}>
          {relationFormError && (
            <div className="sm:col-span-2">
              <Alert tone="error">{relationFormError}</Alert>
            </div>
          )}
          <Field label="Môn học chính" required>
            <select
              className="input"
              value={relationForm.monHocId}
              onChange={(event) =>
                setRelationForm({ ...relationForm, monHocId: event.target.value })
              }
              required
            >
              <option value="">-- Chọn môn học chính --</option>
              {(monHocList || []).map((m) => (
                <option key={m.id} value={m.id}>
                  {m.ma} - {m.ten}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Môn học liên quan" required>
            <select
              className="input"
              value={relationForm.monLienQuanId}
              onChange={(event) =>
                setRelationForm({ ...relationForm, monLienQuanId: event.target.value })
              }
              required
            >
              <option value="">-- Chọn môn liên quan --</option>
              {(monHocList || [])
                .filter((m) => String(m.id) !== String(relationForm.monHocId))
                .map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.ma} - {m.ten}
                  </option>
                ))}
            </select>
          </Field>
          <Field label="Loại quan hệ" required>
            <select
              className="input"
              value={relationForm.loaiQuanHe}
              onChange={(event) =>
                setRelationForm({ ...relationForm, loaiQuanHe: event.target.value })
              }
              required
            >
              {(loaiQuanHeList || [
                { ma: "TIEN_QUYET", ten: "Tiên quyết" },
                { ma: "SONG_HANH", ten: "Song hành" },
                { ma: "TUONG_DUONG", ten: "Tương đương" },
                { ma: "THAY_THE", ten: "Thay thế" },
                { ma: "NHOM_TU_CHON", ten: "Nhóm tự chọn" },
              ]).map((loai) => {
                const code = loai.loaiQuanHe || loai.ma;
                const label = loai.tenLoaiQuanHe || loai.ten;
                return (
                  <option key={code} value={code}>
                    {label} ({code})
                  </option>
                );
              })}
            </select>
          </Field>
          <Field label="Điểm đạt tối thiểu (thang 10)" hint="Mặc định 5.0 với môn yêu cầu đạt">
            <input
              className="input"
              type="number"
              step="0.1"
              min="0"
              max="10"
              value={relationForm.dieuKienDat}
              onChange={(event) =>
                setRelationForm({ ...relationForm, dieuKienDat: event.target.value })
              }
            />
          </Field>
          {relationForm.loaiQuanHe === "NHOM_TU_CHON" && (
            <>
              <Field label="Mã nhóm tự chọn" required>
                <input
                  className="input"
                  value={relationForm.maNhomTuChon}
                  onChange={(event) =>
                    setRelationForm({ ...relationForm, maNhomTuChon: event.target.value })
                  }
                  placeholder="VD: TC-CNPM-01"
                  required
                />
              </Field>
              <Field label="Số tín chỉ tự chọn yêu cầu">
                <input
                  className="input"
                  type="number"
                  min="1"
                  max="30"
                  value={relationForm.tinChiTuChonYeuCau}
                  onChange={(event) =>
                    setRelationForm({
                      ...relationForm,
                      tinChiTuChonYeuCau: event.target.value,
                    })
                  }
                  placeholder="VD: 6"
                />
              </Field>
            </>
          )}
          <Field label="Khóa nhập học áp dụng" hint="Để trống nếu áp dụng cho mọi khóa">
            <input
              className="input"
              list="cohort-options-relation"
              value={relationForm.khoaNhapHoc}
              onChange={(event) =>
                setRelationForm({ ...relationForm, khoaNhapHoc: event.target.value })
              }
              placeholder="VD: K2024"
            />
            <datalist id="cohort-options-relation">
              {(cohortList || []).map((c) => (
                <option key={c.id} value={c.ma}>
                  {c.ten} ({c.nienKhoa})
                </option>
              ))}
            </datalist>
          </Field>
          <Field label="Trạng thái & Yêu cầu đạt">
            <div className="flex flex-col gap-1.5 pt-1 text-xs text-slate-700">
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={relationForm.yeuCauDat}
                  onChange={(event) =>
                    setRelationForm({ ...relationForm, yeuCauDat: event.target.checked })
                  }
                />
                Yêu cầu đạt điểm tổng kết đã công bố
              </label>
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={relationForm.dangHieuLuc}
                  onChange={(event) =>
                    setRelationForm({ ...relationForm, dangHieuLuc: event.target.checked })
                  }
                />
                Đang có hiệu lực áp dụng
              </label>
            </div>
          </Field>
          <Field label="Học kỳ bắt đầu hiệu lực">
            <select
              className="input"
              value={relationForm.hocKyHieuLucTuId}
              onChange={(event) =>
                setRelationForm({ ...relationForm, hocKyHieuLucTuId: event.target.value })
              }
            >
              <option value="">-- Không giới hạn đầu --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} ({hk.namHoc})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Học kỳ kết thúc hiệu lực">
            <select
              className="input"
              value={relationForm.hocKyHieuLucDenId}
              onChange={(event) =>
                setRelationForm({ ...relationForm, hocKyHieuLucDenId: event.target.value })
              }
            >
              <option value="">-- Không giới hạn cuối --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} ({hk.namHoc})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Hiệu lực từ ngày">
            <input
              className="input"
              type="date"
              value={relationForm.hieuLucTu}
              onChange={(event) =>
                setRelationForm({ ...relationForm, hieuLucTu: event.target.value })
              }
            />
          </Field>
          <Field label="Hiệu lực đến ngày">
            <input
              className="input"
              type="date"
              value={relationForm.hieuLucDen}
              onChange={(event) =>
                setRelationForm({ ...relationForm, hieuLucDen: event.target.value })
              }
            />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Ghi chú quy chế / Điều kiện áp dụng">
              <input
                className="input"
                value={relationForm.ghiChu}
                onChange={(event) =>
                  setRelationForm({ ...relationForm, ghiChu: event.target.value })
                }
                placeholder="VD: Áp dụng từ CTĐT 2024; chờ chốt quy chế học song hành/thay thế..."
              />
            </Field>
          </div>
        </form>
      </Modal>

      {/* MODAL 4: THEM / SUA KHOA HOC / NIEN KHOA */}
      <Modal
        open={cohortModalOpen}
        onClose={() => setCohortModalOpen(false)}
        title={editingCohort ? "Cập nhật khóa học / niên khóa" : "Thêm khóa học / niên khóa"}
        footer={
          <>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setCohortModalOpen(false)}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="cohort-form"
              className="btn-primary"
              disabled={savingCohort}
            >
              {savingCohort ? "Đang lưu..." : "Lưu khóa học"}
            </button>
          </>
        }
      >
        <form id="cohort-form" className="grid gap-3 sm:grid-cols-2" onSubmit={submitCohort}>
          {cohortFormError && (
            <div className="sm:col-span-2">
              <Alert tone="error">{cohortFormError}</Alert>
            </div>
          )}
          <Field label="Mã khóa học" required>
            <input
              className="input"
              value={cohortForm.ma}
              onChange={(event) => setCohortForm({ ...cohortForm, ma: event.target.value })}
              placeholder="VD: K2024"
              required
            />
          </Field>
          <Field label="Tên khóa học" required>
            <input
              className="input"
              value={cohortForm.ten}
              onChange={(event) => setCohortForm({ ...cohortForm, ten: event.target.value })}
              placeholder="VD: Khóa 2024"
              required
            />
          </Field>
          <Field label="Niên khóa" required>
            <input
              className="input"
              value={cohortForm.nienKhoa}
              onChange={(event) =>
                setCohortForm({ ...cohortForm, nienKhoa: event.target.value })
              }
              placeholder="VD: 2024-2028"
              required
            />
          </Field>
          <div className="grid grid-cols-2 gap-2">
            <Field label="Năm bắt đầu" required>
              <input
                className="input"
                type="number"
                min="2000"
                max="2100"
                value={cohortForm.namBatDau}
                onChange={(event) =>
                  setCohortForm({ ...cohortForm, namBatDau: event.target.value })
                }
                required
              />
            </Field>
            <Field label="Năm kết thúc" required>
              <input
                className="input"
                type="number"
                min="2000"
                max="2100"
                value={cohortForm.namKetThuc}
                onChange={(event) =>
                  setCohortForm({ ...cohortForm, namKetThuc: event.target.value })
                }
                required
              />
            </Field>
          </div>
          <Field label="Học kỳ bắt đầu">
            <select
              className="input"
              value={cohortForm.hocKyBatDauId}
              onChange={(event) =>
                setCohortForm({ ...cohortForm, hocKyBatDauId: event.target.value })
              }
            >
              <option value="">-- Chọn học kỳ bắt đầu --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} ({hk.namHoc})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Học kỳ kết thúc">
            <select
              className="input"
              value={cohortForm.hocKyKetThucId}
              onChange={(event) =>
                setCohortForm({ ...cohortForm, hocKyKetThucId: event.target.value })
              }
            >
              <option value="">-- Chọn học kỳ kết thúc --</option>
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} ({hk.namHoc})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Hiệu lực từ ngày">
            <input
              className="input"
              type="date"
              value={cohortForm.hieuLucTu}
              onChange={(event) =>
                setCohortForm({ ...cohortForm, hieuLucTu: event.target.value })
              }
            />
          </Field>
          <Field label="Hiệu lực đến ngày">
            <input
              className="input"
              type="date"
              value={cohortForm.hieuLucDen}
              onChange={(event) =>
                setCohortForm({ ...cohortForm, hieuLucDen: event.target.value })
              }
            />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Mô tả">
              <input
                className="input"
                value={cohortForm.moTa}
                onChange={(event) =>
                  setCohortForm({ ...cohortForm, moTa: event.target.value })
                }
                placeholder="Ghi chú về chuẩn đầu ra, chương trình áp dụng..."
              />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700">
              <input
                type="checkbox"
                checked={cohortForm.dangHieuLuc}
                onChange={(event) =>
                  setCohortForm({ ...cohortForm, dangHieuLuc: event.target.checked })
                }
              />
              Khóa học đang có hiệu lực đào tạo
            </label>
          </div>
        </form>
      </Modal>
    </div>
  );
}
