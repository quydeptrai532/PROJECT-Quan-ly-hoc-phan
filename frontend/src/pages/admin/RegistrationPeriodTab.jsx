import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Calendar,
  CalendarClock,
  Clock,
  Edit2,
  FastForward,
  Filter,
  Plus,
  RefreshCw,
  Trash2,
  Users,
  XCircle,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Modal, Spinner } from "../../components/ui";
import { formatDateTime } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function RegistrationPeriodTab() {
  const { data: hocKyList, loading: loadingHocKy } = useApi("/hoc-ky");
  const { data: khoaHocList } = useApi("/khoa-hoc");
  const { data: nganhList } = useApi("/nganh");
  const { data: lopHanhChinhList } = useApi("/lop-hanh-chinh");

  const [selectedHocKyId, setSelectedHocKyId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [dotList, setDotList] = useState([]);
  const [loadingDots, setLoadingDots] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Modals state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [giaHanModalOpen, setGiaHanModalOpen] = useState(false);
  const [dongSomModalOpen, setDongSomModalOpen] = useState(false);
  const [moBoSungModalOpen, setMoBoSungModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

  const [selectedDot, setSelectedDot] = useState(null);

  // Form states
  const [form, setForm] = useState({
    tenDot: "",
    batDau: "",
    ketThuc: "",
    khoaHocId: "",
    nganhId: "",
    lopHanhChinhId: "",
    nhomDoiTuong: "",
    thuTuUuTien: 1,
    choPhepDangKy: true,
    ghiChu: "",
    lyDo: "",
  });

  const [giaHanForm, setGiaHanForm] = useState({ thoiGianMoi: "", lyDo: "" });
  const [dongSomForm, setDongSomForm] = useState({ lyDo: "" });
  const [moBoSungForm, setMoBoSungForm] = useState({ tenDot: "", batDau: "", ketThuc: "", lyDo: "" });
  const [deleteForm, setDeleteForm] = useState({ lyDo: "" });
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (!selectedHocKyId && hocKyList?.length) {
      setSelectedHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, selectedHocKyId]);

  const loadDots = async (hocKyId) => {
    if (!hocKyId) return;
    setLoadingDots(true);
    setError("");
    try {
      const res = await api.get("/admin/dot-dang-ky", { params: { hocKyId } });
      setDotList(res.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoadingDots(false);
    }
  };

  useEffect(() => {
    if (selectedHocKyId) {
      loadDots(selectedHocKyId);
    }
  }, [selectedHocKyId]);

  const filteredDots = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return dotList;
    return dotList.filter(
      (d) =>
        (d.tenDot && d.tenDot.toLowerCase().includes(term)) ||
        (d.doiTuongMoTa && d.doiTuongMoTa.toLowerCase().includes(term)) ||
        (d.nhomDoiTuong && d.nhomDoiTuong.toLowerCase().includes(term)) ||
        (d.tenKhoaHoc && d.tenKhoaHoc.toLowerCase().includes(term)) ||
        (d.tenNganh && d.tenNganh.toLowerCase().includes(term))
    );
  }, [dotList, keyword]);

  const handleOpenCreate = () => {
    setForm({
      tenDot: "",
      batDau: "",
      ketThuc: "",
      khoaHocId: "",
      nganhId: "",
      lopHanhChinhId: "",
      nhomDoiTuong: "",
      thuTuUuTien: 1,
      choPhepDangKy: true,
      ghiChu: "",
      lyDo: "",
    });
    setError("");
    setMessage("");
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError("");
    try {
      await api.post("/admin/dot-dang-ky", {
        hocKyId: Number(selectedHocKyId),
        tenDot: form.tenDot.trim(),
        batDau: form.batDau,
        ketThuc: form.ketThuc,
        khoaHocId: form.khoaHocId ? Number(form.khoaHocId) : null,
        nganhId: form.nganhId ? Number(form.nganhId) : null,
        lopHanhChinhId: form.lopHanhChinhId ? Number(form.lopHanhChinhId) : null,
        nhomDoiTuong: form.nhomDoiTuong.trim() || null,
        thuTuUuTien: Number(form.thuTuUuTien) || 1,
        choPhepDangKy: form.choPhepDangKy,
        ghiChu: form.ghiChu.trim() || null,
      });
      setMessage("Đã tạo đợt đăng ký mới thành công!");
      setCreateModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenEdit = (dot) => {
    setSelectedDot(dot);
    setForm({
      tenDot: dot.tenDot || "",
      batDau: dot.batDau ? dot.batDau.slice(0, 16) : "",
      ketThuc: dot.ketThuc ? dot.ketThuc.slice(0, 16) : "",
      khoaHocId: dot.khoaHocId ? String(dot.khoaHocId) : "",
      nganhId: dot.nganhId ? String(dot.nganhId) : "",
      lopHanhChinhId: dot.lopHanhChinhId ? String(dot.lopHanhChinhId) : "",
      nhomDoiTuong: dot.nhomDoiTuong || "",
      thuTuUuTien: dot.thuTuUuTien || 1,
      choPhepDangKy: dot.choPhepDangKy ?? true,
      ghiChu: dot.ghiChu || "",
      lyDo: "",
    });
    setError("");
    setMessage("");
    setEditModalOpen(true);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!form.lyDo.trim()) {
      setError("Vui lòng nhập lý do điều chỉnh đợt đăng ký.");
      return;
    }
    setActionLoading(true);
    setError("");
    try {
      await api.put(`/admin/dot-dang-ky/${selectedDot.id}`, {
        tenDot: form.tenDot.trim(),
        batDau: form.batDau,
        ketThuc: form.ketThuc,
        khoaHocId: form.khoaHocId ? Number(form.khoaHocId) : null,
        nganhId: form.nganhId ? Number(form.nganhId) : null,
        lopHanhChinhId: form.lopHanhChinhId ? Number(form.lopHanhChinhId) : null,
        nhomDoiTuong: form.nhomDoiTuong.trim() || null,
        thuTuUuTien: Number(form.thuTuUuTien) || 1,
        choPhepDangKy: form.choPhepDangKy,
        ghiChu: form.ghiChu.trim() || null,
        lyDo: form.lyDo.trim(),
      });
      setMessage(`Đã cập nhật đợt đăng ký "${form.tenDot}" thành công!`);
      setEditModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenGiaHan = (dot) => {
    setSelectedDot(dot);
    setGiaHanForm({
      thoiGianMoi: dot.ketThuc ? dot.ketThuc.slice(0, 16) : "",
      lyDo: "",
    });
    setError("");
    setMessage("");
    setGiaHanModalOpen(true);
  };

  const handleGiaHanSubmit = async (e) => {
    e.preventDefault();
    if (!giaHanForm.lyDo.trim()) {
      setError("Vui lòng nhập lý do gia hạn đợt đăng ký.");
      return;
    }
    setActionLoading(true);
    setError("");
    try {
      await api.post(`/admin/dot-dang-ky/${selectedDot.id}/gia-han`, {
        thoiGianMoi: giaHanForm.thoiGianMoi,
        lyDo: giaHanForm.lyDo.trim(),
      });
      setMessage(`Đã gia hạn đợt "${selectedDot.tenDot}" thành công!`);
      setGiaHanModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenDongSom = (dot) => {
    setSelectedDot(dot);
    setDongSomForm({ lyDo: "" });
    setError("");
    setMessage("");
    setDongSomModalOpen(true);
  };

  const handleDongSomSubmit = async (e) => {
    e.preventDefault();
    if (!dongSomForm.lyDo.trim()) {
      setError("Vui lòng nhập lý do đóng sớm đợt đăng ký.");
      return;
    }
    setActionLoading(true);
    setError("");
    try {
      await api.post(`/admin/dot-dang-ky/${selectedDot.id}/dong-som`, {
        lyDo: dongSomForm.lyDo.trim(),
      });
      setMessage(`Đã đóng sớm đợt "${selectedDot.tenDot}" thành công!`);
      setDongSomModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenMoBoSung = (dot) => {
    setSelectedDot(dot);
    setMoBoSungForm({
      tenDot: dot.tenDot ? `${dot.tenDot} (Bổ sung)` : "",
      batDau: "",
      ketThuc: "",
      lyDo: "",
    });
    setError("");
    setMessage("");
    setMoBoSungModalOpen(true);
  };

  const handleMoBoSungSubmit = async (e) => {
    e.preventDefault();
    if (!moBoSungForm.lyDo.trim()) {
      setError("Vui lòng nhập lý do mở bổ sung.");
      return;
    }
    setActionLoading(true);
    setError("");
    try {
      await api.post(`/admin/dot-dang-ky/${selectedDot.id}/mo-bo-sung`, {
        tenDot: moBoSungForm.tenDot.trim() || null,
        batDau: moBoSungForm.batDau,
        ketThuc: moBoSungForm.ketThuc,
        lyDo: moBoSungForm.lyDo.trim(),
      });
      setMessage(`Đã mở đợt bổ sung thành công!`);
      setMoBoSungModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenDelete = (dot) => {
    setSelectedDot(dot);
    setDeleteForm({ lyDo: "" });
    setError("");
    setMessage("");
    setDeleteModalOpen(true);
  };

  const handleDeleteSubmit = async (e) => {
    e.preventDefault();
    if (!deleteForm.lyDo.trim()) {
      setError("Vui lòng nhập lý do xóa đợt đăng ký.");
      return;
    }
    setActionLoading(true);
    setError("");
    try {
      await api.delete(`/admin/dot-dang-ky/${selectedDot.id}`, {
        params: { lyDo: deleteForm.lyDo.trim() },
      });
      setMessage(`Đã xóa đợt đăng ký "${selectedDot.tenDot}" thành công!`);
      setDeleteModalOpen(false);
      await loadDots(selectedHocKyId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const renderTrangThaiBadge = (dot) => {
    if (dot.daDongSom) {
      return <Badge tone="red">Đóng sớm</Badge>;
    }
    if (!dot.choPhepDangKy) {
      return <Badge tone="slate">Tạm dừng ĐK</Badge>;
    }
    switch (dot.trangThai) {
      case "DANG_MO":
        return <Badge tone="green">Đang mở</Badge>;
      case "SAP_MO":
        return <Badge tone="amber">Sắp mở</Badge>;
      case "DA_DONG":
        return <Badge tone="slate">Đã đóng</Badge>;
      default:
        return <Badge tone="slate">{dot.trangThai}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Controls & Filter Header */}
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-500">
              Chọn Học kỳ
            </label>
            <select
              className="input w-auto min-w-[220px]"
              value={selectedHocKyId}
              onChange={(e) => setSelectedHocKyId(e.target.value)}
              disabled={loadingHocKy}
            >
              {(hocKyList || []).map((hk) => (
                <option key={hk.id} value={hk.id}>
                  {hk.ten} - {hk.namHoc}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-slate-500">
              Tìm kiếm
            </label>
            <input
              className="input w-auto min-w-[200px]"
              placeholder="Tên đợt, đối tượng..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className="btn btn-secondary flex items-center gap-1.5"
            onClick={() => loadDots(selectedHocKyId)}
            disabled={loadingDots}
          >
            <RefreshCw size={15} className={loadingDots ? "animate-spin" : ""} />
            Làm mới
          </button>
          <button
            type="button"
            className="btn btn-primary flex items-center gap-1.5"
            onClick={handleOpenCreate}
            disabled={!selectedHocKyId}
          >
            <Plus size={16} />
            Thêm đợt đăng ký
          </button>
        </div>
      </div>

      {/* Info banner about timezone & multi-cohort policy */}
      <div className="rounded-xl border border-blue-100 bg-blue-50/70 p-4 text-xs leading-relaxed text-blue-900">
        <p className="font-semibold">Quy định múi giờ và chính sách đối tượng đăng ký:</p>
        <ul className="mt-1 list-inside list-disc space-y-0.5 text-blue-800">
          <li>
            Múi giờ hệ thống áp dụng thống nhất: <strong>Asia/Ho_Chi_Minh (UTC+7)</strong>. Mọi biên thời gian đăng ký
            được kiểm soát chặt chẽ ở backend.
          </li>
          <li>
            Giới hạn đối tượng theo khóa, ngành, lớp hoặc nhóm. Nếu không chọn, đợt áp dụng cho toàn bộ sinh viên.
          </li>
          <li>
            Thứ tự ưu tiên: Sinh viên thuộc nhiều nhóm sẽ áp dụng quy tắc ưu tiên cao nhất theo cấu hình hệ thống. Mọi
            thay đổi đợt/đối tượng đều bắt buộc nhập lý do và lưu vết audit tự động.
          </li>
        </ul>
      </div>

      {/* Alerts */}
      {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
      {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}

      {/* Table */}
      <div className="card overflow-hidden">
        {loadingDots ? (
          <Spinner label="Đang tải danh sách đợt đăng ký..." />
        ) : filteredDots.length === 0 ? (
          <EmptyState
            icon={CalendarClock}
            title="Chưa có đợt đăng ký nào"
            description="Hãy tạo đợt đăng ký đầu tiên để phân luồng đối tượng theo khóa, ngành hoặc lớp."
            action={
              <button type="button" className="btn btn-primary mt-3 flex items-center gap-1.5" onClick={handleOpenCreate}>
                <Plus size={16} /> Thêm đợt ngay
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="th">Tên đợt & Ưu tiên</th>
                  <th className="th">Khoảng thời gian (UTC+7)</th>
                  <th className="th">Đối tượng áp dụng</th>
                  <th className="th">Trạng thái</th>
                  <th className="th">Audit & Điều chỉnh</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredDots.map((dot) => (
                  <tr key={dot.id} className="hover:bg-slate-50/70 transition">
                    <td className="td font-medium">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-900">{dot.tenDot}</span>
                        {dot.daMoBoSung && (
                          <span className="rounded bg-indigo-100 px-1.5 py-0.5 text-[10px] font-medium text-indigo-700">
                            Bổ sung
                          </span>
                        )}
                      </div>
                      <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
                        <span>Ưu tiên: <strong>#{dot.thuTuUuTien}</strong></span>
                        {dot.ghiChu && <span className="text-slate-400">· {dot.ghiChu}</span>}
                      </div>
                    </td>

                    <td className="td text-xs text-slate-700">
                      <div>
                        Bắt đầu: <strong className="text-slate-900">{formatDateTime(dot.batDau)}</strong>
                      </div>
                      <div className="mt-0.5">
                        Kết thúc: <strong className="text-slate-900">{formatDateTime(dot.ketThuc)}</strong>
                      </div>
                    </td>

                    <td className="td text-xs">
                      {dot.khoaHocId || dot.nganhId || dot.lopHanhChinhId || dot.nhomDoiTuong ? (
                        <div className="space-y-1">
                          {dot.tenKhoaHoc && (
                            <div className="inline-block rounded bg-sky-50 px-2 py-0.5 text-sky-800 mr-1">
                              Khóa: {dot.tenKhoaHoc}
                            </div>
                          )}
                          {dot.tenNganh && (
                            <div className="inline-block rounded bg-purple-50 px-2 py-0.5 text-purple-800 mr-1">
                              Ngành: {dot.tenNganh}
                            </div>
                          )}
                          {dot.tenLopHanhChinh && (
                            <div className="inline-block rounded bg-emerald-50 px-2 py-0.5 text-emerald-800 mr-1">
                              Lớp: {dot.tenLopHanhChinh}
                            </div>
                          )}
                          {dot.nhomDoiTuong && (
                            <div className="inline-block rounded bg-amber-50 px-2 py-0.5 text-amber-800">
                              Nhóm: {dot.nhomDoiTuong}
                            </div>
                          )}
                        </div>
                      ) : (
                        <Badge tone="indigo">Tất cả sinh viên</Badge>
                      )}
                    </td>

                    <td className="td">
                      {renderTrangThaiBadge(dot)}
                    </td>

                    <td className="td text-xs text-slate-600">
                      <div>
                        Cập nhật: <strong>{dot.nguoiCapNhatCuoi || dot.nguoiTao || "Hệ thống"}</strong>
                      </div>
                      <div className="text-[11px] text-slate-400">
                        {formatDateTime(dot.capNhatLuc || dot.taoLuc)}
                      </div>
                      {dot.lyDoDieuChinh && (
                        <div className="mt-1 line-clamp-1 italic text-slate-500" title={dot.lyDoDieuChinh}>
                          Lý do: {dot.lyDoDieuChinh}
                        </div>
                      )}
                    </td>

                    <td className="td text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
                          title="Sửa cấu hình đợt"
                          onClick={() => handleOpenEdit(dot)}
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          type="button"
                          className="rounded-lg p-1.5 text-blue-600 hover:bg-blue-50"
                          title="Gia hạn thời gian"
                          onClick={() => handleOpenGiaHan(dot)}
                        >
                          <FastForward size={15} />
                        </button>
                        {!dot.daDongSom && dot.trangThai !== "DA_DONG" && (
                          <button
                            type="button"
                            className="rounded-lg p-1.5 text-amber-600 hover:bg-amber-50"
                            title="Đóng sớm đợt đăng ký"
                            onClick={() => handleOpenDongSom(dot)}
                          >
                            <XCircle size={15} />
                          </button>
                        )}
                        <button
                          type="button"
                          className="rounded-lg p-1.5 text-emerald-600 hover:bg-emerald-50"
                          title="Mở đợt bổ sung"
                          onClick={() => handleOpenMoBoSung(dot)}
                        >
                          <Plus size={15} />
                        </button>
                        <button
                          type="button"
                          className="rounded-lg p-1.5 text-rose-600 hover:bg-rose-50"
                          title="Xóa đợt"
                          onClick={() => handleOpenDelete(dot)}
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

      {/* MODAL 1: TẠO ĐỢT MỚI */}
      <Modal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Thêm đợt đăng ký mới"
        description="Cấu hình khoảng thời gian và đối tượng được phép đăng ký học phần."
        size="lg"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setCreateModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-create-dot"
              className="btn btn-primary"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang lưu..." : "Tạo đợt"}
            </button>
          </>
        }
      >
        <form id="form-create-dot" onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
              Tên đợt đăng ký *
            </label>
            <input
              required
              className="input mt-1 w-full"
              placeholder="VD: Đợt 1 - Sinh viên K2024 Khoa CNTT"
              value={form.tenDot}
              onChange={(e) => setForm({ ...form, tenDot: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thời gian bắt đầu (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={form.batDau}
                onChange={(e) => setForm({ ...form, batDau: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thời gian kết thúc (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={form.ketThuc}
                onChange={(e) => setForm({ ...form, ketThuc: e.target.value })}
              />
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
              Giới hạn đối tượng áp dụng (để trống nếu áp dụng cho toàn bộ)
            </h4>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="block text-xs text-slate-500">Khóa học / Niên khóa</label>
                <select
                  className="input mt-1 w-full"
                  value={form.khoaHocId}
                  onChange={(e) => setForm({ ...form, khoaHocId: e.target.value })}
                >
                  <option value="">-- Tất cả khóa --</option>
                  {(khoaHocList || []).map((k) => (
                    <option key={k.id} value={k.id}>
                      {k.ten} ({k.ma})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-500">Ngành học</label>
                <select
                  className="input mt-1 w-full"
                  value={form.nganhId}
                  onChange={(e) => setForm({ ...form, nganhId: e.target.value })}
                >
                  <option value="">-- Tất cả ngành --</option>
                  {(nganhList || []).map((n) => (
                    <option key={n.id} value={n.id}>
                      {n.ten} ({n.ma})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-500">Lớp hành chính</label>
                <select
                  className="input mt-1 w-full"
                  value={form.lopHanhChinhId}
                  onChange={(e) => setForm({ ...form, lopHanhChinhId: e.target.value })}
                >
                  <option value="">-- Tất cả lớp --</option>
                  {(lopHanhChinhList || []).map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.ten} ({l.ma})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-3">
              <label className="block text-xs text-slate-500">Mã nhóm đối tượng (nếu có)</label>
              <input
                className="input mt-1 w-full"
                placeholder="VD: CHINH_QUY, CHAT_LUONG_CAO, HOAN_HOI_PHI..."
                value={form.nhomDoiTuong}
                onChange={(e) => setForm({ ...form, nhomDoiTuong: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thứ tự ưu tiên (1 = cao nhất)
              </label>
              <input
                type="number"
                min="1"
                className="input mt-1 w-full"
                value={form.thuTuUuTien}
                onChange={(e) => setForm({ ...form, thuTuUuTien: e.target.value })}
              />
            </div>
            <div className="flex items-center pt-6">
              <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 h-4 w-4"
                  checked={form.choPhepDangKy}
                  onChange={(e) => setForm({ ...form, choPhepDangKy: e.target.checked })}
                />
                <span>Cho phép đăng ký học phần trong đợt này</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
              Ghi chú nội bộ
            </label>
            <textarea
              rows={2}
              className="input mt-1 w-full"
              placeholder="Ghi chú thêm về đợt đăng ký này..."
              value={form.ghiChu}
              onChange={(e) => setForm({ ...form, ghiChu: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 2: SỬA ĐỢT & LÝ DO */}
      <Modal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title={`Chỉnh sửa đợt: ${selectedDot?.tenDot}`}
        description="Mọi thay đổi cấu hình đợt đăng ký đều được ghi lại trong nhật ký audit hệ thống."
        size="lg"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setEditModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-edit-dot"
              className="btn btn-primary"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang lưu..." : "Lưu thay đổi"}
            </button>
          </>
        }
      >
        <form id="form-edit-dot" onSubmit={handleEditSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
              Tên đợt đăng ký *
            </label>
            <input
              required
              className="input mt-1 w-full"
              value={form.tenDot}
              onChange={(e) => setForm({ ...form, tenDot: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thời gian bắt đầu (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={form.batDau}
                onChange={(e) => setForm({ ...form, batDau: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thời gian kết thúc (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={form.ketThuc}
                onChange={(e) => setForm({ ...form, ketThuc: e.target.value })}
              />
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
              Đối tượng áp dụng
            </h4>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="block text-xs text-slate-500">Khóa học</label>
                <select
                  className="input mt-1 w-full"
                  value={form.khoaHocId}
                  onChange={(e) => setForm({ ...form, khoaHocId: e.target.value })}
                >
                  <option value="">-- Tất cả khóa --</option>
                  {(khoaHocList || []).map((k) => (
                    <option key={k.id} value={k.id}>
                      {k.ten} ({k.ma})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-500">Ngành học</label>
                <select
                  className="input mt-1 w-full"
                  value={form.nganhId}
                  onChange={(e) => setForm({ ...form, nganhId: e.target.value })}
                >
                  <option value="">-- Tất cả ngành --</option>
                  {(nganhList || []).map((n) => (
                    <option key={n.id} value={n.id}>
                      {n.ten} ({n.ma})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-500">Lớp hành chính</label>
                <select
                  className="input mt-1 w-full"
                  value={form.lopHanhChinhId}
                  onChange={(e) => setForm({ ...form, lopHanhChinhId: e.target.value })}
                >
                  <option value="">-- Tất cả lớp --</option>
                  {(lopHanhChinhList || []).map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.ten} ({l.ma})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-3">
              <label className="block text-xs text-slate-500">Mã nhóm đối tượng</label>
              <input
                className="input mt-1 w-full"
                value={form.nhomDoiTuong}
                onChange={(e) => setForm({ ...form, nhomDoiTuong: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Thứ tự ưu tiên
              </label>
              <input
                type="number"
                min="1"
                className="input mt-1 w-full"
                value={form.thuTuUuTien}
                onChange={(e) => setForm({ ...form, thuTuUuTien: e.target.value })}
              />
            </div>
            <div className="flex items-center pt-6">
              <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 h-4 w-4"
                  checked={form.choPhepDangKy}
                  onChange={(e) => setForm({ ...form, choPhepDangKy: e.target.checked })}
                />
                <span>Cho phép đăng ký học phần</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-rose-700">
              Lý do điều chỉnh (bắt buộc để lưu audit) *
            </label>
            <input
              required
              className="input mt-1 w-full border-rose-300 focus:border-rose-500 focus:ring-rose-500"
              placeholder="VD: Điều chỉnh thời gian mở đăng ký theo thông báo số 45/TB-ĐT..."
              value={form.lyDo}
              onChange={(e) => setForm({ ...form, lyDo: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 3: GIA HẠN */}
      <Modal
        open={giaHanModalOpen}
        onClose={() => setGiaHanModalOpen(false)}
        title={`Gia hạn đợt đăng ký: ${selectedDot?.tenDot}`}
        description="Gia hạn thời gian kết thúc của đợt đăng ký. Thao tác yêu cầu lý do giải trình để lưu audit."
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setGiaHanModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-gia-han"
              className="btn btn-primary"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang xử lý..." : "Xác nhận gia hạn"}
            </button>
          </>
        }
      >
        <form id="form-gia-han" onSubmit={handleGiaHanSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
              Thời gian kết thúc mới (UTC+7) *
            </label>
            <input
              required
              type="datetime-local"
              className="input mt-1 w-full"
              value={giaHanForm.thoiGianMoi}
              onChange={(e) => setGiaHanForm({ ...giaHanForm, thoiGianMoi: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-rose-700">
              Lý do gia hạn (bắt buộc) *
            </label>
            <textarea
              required
              rows={3}
              className="input mt-1 w-full border-rose-300 focus:border-rose-500 focus:ring-rose-500"
              placeholder="VD: Gia hạn thêm 2 ngày do lịch bảo trì cổng đăng ký ngày 08/11..."
              value={giaHanForm.lyDo}
              onChange={(e) => setGiaHanForm({ ...giaHanForm, lyDo: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 4: ĐÓNG SỚM */}
      <Modal
        open={dongSomModalOpen}
        onClose={() => setDongSomModalOpen(false)}
        title={`Đóng sớm đợt đăng ký: ${selectedDot?.tenDot}`}
        description="Đóng ngay lập tức đợt đăng ký hiện tại. Sinh viên sẽ không thể đăng ký thêm. Lịch sử đã đăng ký được bảo toàn nguyên vẹn."
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setDongSomModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-dong-som"
              className="btn btn-primary bg-rose-600 hover:bg-rose-700"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang xử lý..." : "Đóng đợt ngay"}
            </button>
          </>
        }
      >
        <form id="form-dong-som" onSubmit={handleDongSomSubmit} className="space-y-4">
          <div className="rounded-lg bg-rose-50 p-3 text-xs text-rose-800">
            Cảnh báo: Hành động này sẽ cập nhật mốc kết thúc về thời điểm hiện tại và đánh dấu đóng sớm. Mọi đăng ký trước đó vẫn được giữ nguyên.
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-rose-700">
              Lý do đóng sớm (bắt buộc) *
            </label>
            <textarea
              required
              rows={3}
              className="input mt-1 w-full border-rose-300 focus:border-rose-500 focus:ring-rose-500"
              placeholder="VD: Các lớp học phần đã đầy sĩ số, đóng sớm để chốt thời khóa biểu..."
              value={dongSomForm.lyDo}
              onChange={(e) => setDongSomForm({ ...dongSomForm, lyDo: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 5: MỞ BỔ SUNG */}
      <Modal
        open={moBoSungModalOpen}
        onClose={() => setMoBoSungModalOpen(false)}
        title={`Mở đợt bổ sung từ đợt: ${selectedDot?.tenDot}`}
        description="Tạo một đợt đăng ký bổ sung kế thừa đối tượng và thứ tự ưu tiên của đợt gốc."
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setMoBoSungModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-mo-bo-sung"
              className="btn btn-primary"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang xử lý..." : "Mở đợt bổ sung"}
            </button>
          </>
        }
      >
        <form id="form-mo-bo-sung" onSubmit={handleMoBoSungSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
              Tên đợt bổ sung
            </label>
            <input
              className="input mt-1 w-full"
              value={moBoSungForm.tenDot}
              onChange={(e) => setMoBoSungForm({ ...moBoSungForm, tenDot: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Bắt đầu (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={moBoSungForm.batDau}
                onChange={(e) => setMoBoSungForm({ ...moBoSungForm, batDau: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Kết thúc (UTC+7) *
              </label>
              <input
                required
                type="datetime-local"
                className="input mt-1 w-full"
                value={moBoSungForm.ketThuc}
                onChange={(e) => setMoBoSungForm({ ...moBoSungForm, ketThuc: e.target.value })}
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-rose-700">
              Lý do mở bổ sung (bắt buộc) *
            </label>
            <textarea
              required
              rows={3}
              className="input mt-1 w-full border-rose-300 focus:border-rose-500 focus:ring-rose-500"
              placeholder="VD: Mở bổ sung cho sinh viên hoàn thành học phí trễ..."
              value={moBoSungForm.lyDo}
              onChange={(e) => setMoBoSungForm({ ...moBoSungForm, lyDo: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 6: XÓA ĐỢT */}
      <Modal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title={`Xóa đợt đăng ký: ${selectedDot?.tenDot}`}
        description="Hành động này sẽ xóa cấu hình đợt đăng ký và lưu vết audit lý do xóa."
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setDeleteModalOpen(false)}
              disabled={actionLoading}
            >
              Hủy
            </button>
            <button
              type="submit"
              form="form-delete-dot"
              className="btn btn-primary bg-rose-600 hover:bg-rose-700"
              disabled={actionLoading}
            >
              {actionLoading ? "Đang xóa..." : "Xóa đợt"}
            </button>
          </>
        }
      >
        <form id="form-delete-dot" onSubmit={handleDeleteSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-rose-700">
              Lý do xóa đợt (bắt buộc) *
            </label>
            <textarea
              required
              rows={3}
              className="input mt-1 w-full border-rose-300 focus:border-rose-500 focus:ring-rose-500"
              placeholder="VD: Tạo trùng đợt hoặc thay đổi phương án đăng ký..."
              value={deleteForm.lyDo}
              onChange={(e) => setDeleteForm({ ...deleteForm, lyDo: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
}
