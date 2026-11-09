import { useState, useEffect, useMemo } from "react";
import {
  DoorOpen,
  Plus,
  Search,
  Pencil,
  Trash2,
  SlidersHorizontal,
  RotateCcw,
  CheckCircle2,
  Wrench,
  Ban,
  Users,
  Building2,
  Tag,
  Info,
  Clock,
  RefreshCw,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner, StatCard } from "../../components/ui";

const EMPTY_FORM = {
  ma: "",
  ten: "",
  toaNha: "",
  tang: "",
  viTri: "",
  sucChua: "50",
  loaiPhong: "Lý thuyết",
  thietBi: "",
  trangThai: "HOAT_DONG",
  ghiChu: "",
};

const EMPTY_QUICK_STATUS = {
  id: null,
  ma: "",
  ten: "",
  trangThai: "HOAT_DONG",
  ghiChu: "",
};

const STATUS_BADGE = {
  HOAT_DONG: { label: "Hoạt động", tone: "green", icon: CheckCircle2 },
  BAO_TRI: { label: "Bảo trì", tone: "amber", icon: Wrench },
  NGUNG_SU_DUNG: { label: "Ngừng sử dụng", tone: "red", icon: Ban },
};

function formatAuditMeta(row) {
  const actor = row.nguoiCapNhatUsername || row.nguoiTaoUsername;
  const timestamp = row.capNhatLuc || row.taoLuc;
  if (!actor && !timestamp) return "Hệ thống";
  const dateText = timestamp ? new Date(timestamp).toLocaleDateString("vi-VN") : "";
  return [actor || "Hệ thống", dateText].filter(Boolean).join(" · ");
}

export default function ClassroomPage() {
  const confirm = useConfirm();

  // Data states
  const [rooms, setRooms] = useState([]);
  const [taxonomy, setTaxonomy] = useState({ loaiPhongList: [], thietBiList: [], trangThaiList: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Filter states
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [minCapacity, setMinCapacity] = useState("");
  const [maxCapacity, setMaxCapacity] = useState("");

  // Modal states
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  // Quick status change modal
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [quickStatus, setQuickStatus] = useState(EMPTY_QUICK_STATUS);
  const [savingStatus, setSavingStatus] = useState(false);

  // Fetch taxonomy and classrooms
  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const [roomsRes, taxRes] = await Promise.all([
        api.get("/admin/phong-hoc"),
        api.get("/admin/phong-hoc/taxonomy"),
      ]);
      setRooms(roomsRes.data || []);
      setTaxonomy(taxRes.data || { loaiPhongList: [], thietBiList: [], trangThaiList: [] });
    } catch (err) {
      setError(getErrorMessage(err, "Không thể tải danh sách phòng học"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Filtered rooms
  const filteredRooms = useMemo(() => {
    return rooms.filter((r) => {
      if (statusFilter && r.trangThai !== statusFilter) return false;
      if (typeFilter && r.loaiPhong !== typeFilter) return false;
      if (minCapacity && r.sucChua < Number(minCapacity)) return false;
      if (maxCapacity && r.sucChua > Number(maxCapacity)) return false;

      if (search.trim()) {
        const q = search.trim().toLowerCase();
        const match =
          (r.ma && r.ma.toLowerCase().includes(q)) ||
          (r.ten && r.ten.toLowerCase().includes(q)) ||
          (r.toaNha && r.toaNha.toLowerCase().includes(q)) ||
          (r.viTri && r.viTri.toLowerCase().includes(q)) ||
          (r.loaiPhong && r.loaiPhong.toLowerCase().includes(q)) ||
          (r.thietBi && r.thietBi.toLowerCase().includes(q));
        if (!match) return false;
      }
      return true;
    });
  }, [rooms, search, statusFilter, typeFilter, minCapacity, maxCapacity]);

  // Statistics
  const stats = useMemo(() => {
    const total = rooms.length;
    const active = rooms.filter((r) => r.trangThai === "HOAT_DONG").length;
    const maintenance = rooms.filter((r) => r.trangThai === "BAO_TRI").length;
    const inactive = rooms.filter((r) => r.trangThai === "NGUNG_SU_DUNG").length;
    const totalCap = rooms.reduce((acc, r) => acc + (r.sucChua || 0), 0);
    return { total, active, maintenance, inactive, totalCap };
  }, [rooms]);

  // Available room types from data & taxonomy
  const availableRoomTypes = useMemo(() => {
    const set = new Set(taxonomy.loaiPhongList || []);
    rooms.forEach((r) => {
      if (r.loaiPhong) set.add(r.loaiPhong);
    });
    return Array.from(set).filter(Boolean);
  }, [taxonomy, rooms]);

  const resetFilters = () => {
    setSearch("");
    setStatusFilter("");
    setTypeFilter("");
    setMinCapacity("");
    setMaxCapacity("");
  };

  const hasActiveFilters = Boolean(
    search.trim() || statusFilter || typeFilter || minCapacity || maxCapacity
  );

  // Create / Edit Handlers
  const openCreateModal = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setFormError("");
    setModalOpen(true);
  };

  const openEditModal = (room) => {
    setEditing(room);
    setForm({
      ma: room.ma || "",
      ten: room.ten || "",
      toaNha: room.toaNha || "",
      tang: room.tang !== null && room.tang !== undefined ? String(room.tang) : "",
      viTri: room.viTri || "",
      sucChua: String(room.sucChua || 50),
      loaiPhong: room.loaiPhong || "",
      thietBi: room.thietBi || "",
      trangThai: room.trangThai || "HOAT_DONG",
      ghiChu: room.ghiChu || "",
    });
    setFormError("");
    setModalOpen(true);
  };

  const saveRoom = async (e) => {
    e.preventDefault();
    setFormError("");
    setSaving(true);
    try {
      const payload = {
        ma: form.ma.trim(),
        ten: form.ten.trim(),
        toaNha: form.toaNha.trim() || null,
        tang: form.tang ? Number(form.tang) : null,
        viTri: form.viTri.trim() || null,
        sucChua: Number(form.sucChua),
        loaiPhong: form.loaiPhong.trim() || null,
        thietBi: form.thietBi.trim() || null,
        trangThai: form.trangThai,
        ghiChu: form.ghiChu.trim() || null,
      };

      if (editing) {
        await api.put(`/admin/phong-hoc/${editing.id}`, payload);
        setSuccessMsg(`Đã cập nhật thông tin phòng học ${payload.ma}`);
      } else {
        await api.post("/admin/phong-hoc", payload);
        setSuccessMsg(`Đã thêm mới phòng học ${payload.ma}`);
      }

      setModalOpen(false);
      await loadData();
    } catch (err) {
      setFormError(getErrorMessage(err, "Có lỗi xảy ra khi lưu phòng học"));
    } finally {
      setSaving(false);
    }
  };

  // Quick status change
  const openQuickStatusModal = (room) => {
    setQuickStatus({
      id: room.id,
      ma: room.ma,
      ten: room.ten,
      trangThai: room.trangThai,
      ghiChu: room.ghiChu || "",
    });
    setStatusModalOpen(true);
  };

  const saveQuickStatus = async (e) => {
    e.preventDefault();
    setSavingStatus(true);
    try {
      await api.patch(`/admin/phong-hoc/${quickStatus.id}/trang-thai`, {
        trangThai: quickStatus.trangThai,
        ghiChu: quickStatus.ghiChu.trim() || null,
      });
      setSuccessMsg(`Đã cập nhật trạng thái phòng ${quickStatus.ma} thành "${STATUS_BADGE[quickStatus.trangThai]?.label}"`);
      setStatusModalOpen(false);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, "Không thể cập nhật trạng thái phòng học"));
    } finally {
      setSavingStatus(false);
    }
  };

  // Delete handler
  const handleDelete = async (room) => {
    if (room.soLopHocPhan > 0) {
      const confirmed = await confirm({
        title: "Không thể xóa cứng phòng học",
        message: `Phòng "${room.ma} - ${room.ten}" hiện đang được liên kết với ${room.soLopHocPhan} lớp học phần trong lịch sử. Hệ thống bảo toàn dữ liệu lớp/lịch học cũ. Bạn có muốn chuyển trạng thái phòng này sang "Ngừng sử dụng" không?`,
        confirmLabel: "Chuyển sang Ngừng sử dụng",
        tone: "amber",
      });
      if (confirmed) {
        try {
          await api.patch(`/admin/phong-hoc/${room.id}/trang-thai`, {
            trangThai: "NGUNG_SU_DUNG",
            ghiChu: "Ngừng sử dụng để bảo toàn lịch sử lớp học phần",
          });
          setSuccessMsg(`Đã chuyển phòng ${room.ma} sang trạng thái Ngừng sử dụng`);
          await loadData();
        } catch (err) {
          setError(getErrorMessage(err, "Không thể chuyển trạng thái phòng"));
        }
      }
      return;
    }

    const confirmed = await confirm({
      title: "Xác nhận xóa phòng học",
      message: `Bạn có chắc chắn muốn xóa phòng học "${room.ma} - ${room.ten}" không? Thao tác này không thể hoàn tác.`,
      confirmLabel: "Xóa phòng",
      tone: "danger",
    });

    if (confirmed) {
      try {
        await api.delete(`/admin/phong-hoc/${room.id}`);
        setSuccessMsg(`Đã xóa phòng học ${room.ma}`);
        await loadData();
      } catch (err) {
        setError(getErrorMessage(err, "Không thể xóa phòng học"));
      }
    }
  };

  // Equipment tags helper
  const renderEquipmentTags = (thietBiStr) => {
    if (!thietBiStr) return <span className="text-slate-400 italic text-xs">Chưa có thiết bị</span>;
    const items = thietBiStr.split(",").map((s) => s.trim()).filter(Boolean);
    return (
      <div className="flex flex-wrap gap-1">
        {items.map((item, idx) => (
          <span
            key={idx}
            className="inline-flex items-center rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-700 font-medium"
          >
            {item}
          </span>
        ))}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        icon={DoorOpen}
        title="Quản lý phòng học"
        eyebrow="Danh mục cơ sở vật chất"
        description="Quản lý danh sách phòng học, sức chứa, phân loại, thiết bị và trạng thái hoạt động phục vụ xếp lịch lớp học phần."
        actions={
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={loadData}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
              title="Tải lại dữ liệu"
            >
              <RefreshCw size={16} />
              <span>Làm mới</span>
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-slate-800"
            >
              <Plus size={16} />
              <span>Thêm phòng học</span>
            </button>
          </div>
        }
      />

      {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
      {successMsg && <Alert tone="success" onClose={() => setSuccessMsg("")}>{successMsg}</Alert>}

      {/* Stats Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <StatCard label="Tổng số phòng" value={stats.total} icon={Building2} tone="indigo" />
        <StatCard label="Đang hoạt động" value={stats.active} icon={CheckCircle2} tone="emerald" />
        <StatCard label="Đang bảo trì" value={stats.maintenance} icon={Wrench} tone="amber" />
        <StatCard label="Ngừng sử dụng" value={stats.inactive} icon={Ban} tone="rose" />
        <StatCard label="Tổng sức chứa" value={stats.totalCap.toLocaleString("vi-VN")} icon={Users} tone="sky" />
      </div>

      {/* Filter and Search Bar */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-12">
          {/* Keyword Search */}
          <div className="relative lg:col-span-4">
            <Search className="pointer-events-none absolute left-3 top-2.5 text-slate-400" size={17} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã, tên phòng, tòa nhà, thiết bị..."
              className="input pl-9 w-full text-sm"
            />
          </div>

          {/* Status Filter */}
          <div className="lg:col-span-2">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="input w-full text-sm"
            >
              <option value="">Trạng thái: Tất cả</option>
              <option value="HOAT_DONG">Đang hoạt động</option>
              <option value="BAO_TRI">Đang bảo trì</option>
              <option value="NGUNG_SU_DUNG">Ngừng sử dụng</option>
            </select>
          </div>

          {/* Room Type Filter */}
          <div className="lg:col-span-2">
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="input w-full text-sm"
            >
              <option value="">Loại phòng: Tất cả</option>
              {availableRoomTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          {/* Min Capacity */}
          <div className="lg:col-span-2">
            <input
              type="number"
              min="1"
              value={minCapacity}
              onChange={(e) => setMinCapacity(e.target.value)}
              placeholder="Sức chứa từ..."
              className="input w-full text-sm"
            />
          </div>

          {/* Max Capacity */}
          <div className="lg:col-span-2 flex items-center gap-2">
            <input
              type="number"
              min="1"
              value={maxCapacity}
              onChange={(e) => setMaxCapacity(e.target.value)}
              placeholder="...đến"
              className="input w-full text-sm"
            />
            {hasActiveFilters && (
              <button
                type="button"
                onClick={resetFilters}
                className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition"
                title="Xóa bộ lọc"
              >
                <RotateCcw size={15} />
              </button>
            )}
          </div>
        </div>

        {/* Quick taxonomy pills */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-slate-100 text-xs text-slate-500">
          <span className="font-medium mr-1 flex items-center gap-1">
            <Tag size={13} /> Phân loại nhanh:
          </span>
          <button
            type="button"
            onClick={() => setTypeFilter("")}
            className={`rounded-lg px-2.5 py-1 text-xs transition ${
              typeFilter === ""
                ? "bg-slate-900 text-white font-medium"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            Tất cả loại
          </button>
          {availableRoomTypes.map((type) => (
            <button
              key={type}
              type="button"
              onClick={() => setTypeFilter(typeFilter === type ? "" : type)}
              className={`rounded-lg px-2.5 py-1 text-xs transition ${
                typeFilter === type
                  ? "bg-indigo-600 text-white font-medium"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* Main Table Content */}
      {loading ? (
        <Spinner label="Đang tải danh sách phòng học..." />
      ) : filteredRooms.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <EmptyState
            icon={DoorOpen}
            title={hasActiveFilters ? "Không tìm thấy phòng học phù hợp" : "Chưa có phòng học nào"}
            description={
              hasActiveFilters
                ? "Thử thay đổi từ khóa tìm kiếm hoặc bỏ các tiêu chí lọc."
                : "Bấm 'Thêm phòng học' để bắt đầu thiết lập danh mục phòng học cho nhà trường."
            }
            action={
              hasActiveFilters ? (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Xóa bộ lọc
                </button>
              ) : (
                <button
                  type="button"
                  onClick={openCreateModal}
                  className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
                >
                  Thêm phòng học đầu tiên
                </button>
              )
            }
          />
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50/75 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-3.5">Mã phòng</th>
                  <th className="px-4 py-3.5">Tên phòng</th>
                  <th className="px-4 py-3.5">Tòa nhà & Vị trí</th>
                  <th className="px-4 py-3.5 text-center">Sức chứa</th>
                  <th className="px-4 py-3.5">Loại phòng</th>
                  <th className="px-4 py-3.5">Trang thiết bị</th>
                  <th className="px-4 py-3.5 text-center">Lớp xếp lịch</th>
                  <th className="px-4 py-3.5 text-center">Trạng thái</th>
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredRooms.map((room) => {
                  const statusCfg = STATUS_BADGE[room.trangThai] || STATUS_BADGE.HOAT_DONG;
                  const StatusIcon = statusCfg.icon;

                  return (
                    <tr key={room.id} className="transition hover:bg-slate-50/80">
                      {/* Mã phòng */}
                      <td className="px-4 py-3.5">
                        <span className="font-mono text-xs font-semibold text-indigo-700 bg-indigo-50 px-2 py-1 rounded-md border border-indigo-100">
                          {room.ma}
                        </span>
                      </td>

                      {/* Tên phòng */}
                      <td className="px-4 py-3.5">
                        <p className="font-medium text-slate-900">{room.ten}</p>
                        {room.ghiChu && (
                          <p className="text-xs text-slate-400 mt-0.5 line-clamp-1" title={room.ghiChu}>
                            {room.ghiChu}
                          </p>
                        )}
                        <p className="text-[11px] text-slate-400 mt-0.5 flex items-center gap-1">
                          <Clock size={11} /> {formatAuditMeta(room)}
                        </p>
                      </td>

                      {/* Tòa nhà & Vị trí */}
                      <td className="px-4 py-3.5 text-slate-600">
                        <div className="flex flex-col text-xs">
                          <span className="font-medium text-slate-800">
                            {room.toaNha ? `Tòa ${room.toaNha}` : "-"}
                            {room.tang ? ` · Tầng ${room.tang}` : ""}
                          </span>
                          {room.viTri && <span className="text-slate-400">{room.viTri}</span>}
                        </div>
                      </td>

                      {/* Sức chứa */}
                      <td className="px-4 py-3.5 text-center">
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-slate-700 bg-slate-100 px-2 py-1 rounded-md">
                          <Users size={13} className="text-slate-400" />
                          {room.sucChua} chỗ
                        </span>
                      </td>

                      {/* Loại phòng */}
                      <td className="px-4 py-3.5">
                        <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700">
                          {room.loaiPhong || "Chung"}
                        </span>
                      </td>

                      {/* Trang thiết bị */}
                      <td className="px-4 py-3.5 max-w-[200px]">
                        {renderEquipmentTags(room.thietBi)}
                      </td>

                      {/* Lớp xếp lịch */}
                      <td className="px-4 py-3.5 text-center">
                        {room.soLopHocPhan > 0 ? (
                          <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10">
                            {room.soLopHocPhan} lớp
                          </span>
                        ) : (
                          <span className="text-xs text-slate-400">-</span>
                        )}
                      </td>

                      {/* Trạng thái */}
                      <td className="px-4 py-3.5 text-center">
                        <button
                          type="button"
                          onClick={() => openQuickStatusModal(room)}
                          className="group inline-flex items-center gap-1"
                          title="Bấm vào để cập nhật trạng thái"
                        >
                          <Badge tone={statusCfg.tone}>
                            <StatusIcon size={12} className="inline mr-1" />
                            {statusCfg.label}
                          </Badge>
                        </button>
                      </td>

                      {/* Thao tác */}
                      <td className="px-4 py-3.5 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openQuickStatusModal(room)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-amber-600 transition"
                            title="Đổi trạng thái phòng học"
                          >
                            <SlidersHorizontal size={16} />
                          </button>
                          <button
                            type="button"
                            onClick={() => openEditModal(room)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-indigo-600 transition"
                            title="Sửa phòng học"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(room)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600 transition"
                            title={room.soLopHocPhan > 0 ? "Ngừng sử dụng phòng (đã có lịch)" : "Xóa phòng học"}
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50/50 px-4 py-3 text-xs text-slate-500">
            <span>
              Hiển thị <strong>{filteredRooms.length}</strong> / <strong>{rooms.length}</strong> phòng học
            </span>
            <span>Chính sách: Không xóa cứng phòng đã gắn lịch; hỗ trợ chuyển trạng thái bảo trì/ngừng sử dụng.</span>
          </div>
        </div>
      )}

      {/* Modal Thêm / Sửa Phòng học */}
      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? `Sửa phòng học: ${editing.ma}` : "Thêm phòng học mới"}
        description="Điền đầy đủ thông tin phòng học bên dưới. Mã phòng được tự động chuẩn hóa chữ hoa và loại bỏ khoảng trắng thừa."
        size="lg"
        footer={
          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={saveRoom}
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
            >
              {saving ? "Đang lưu..." : editing ? "Lưu thay đổi" : "Tạo phòng học"}
            </button>
          </div>
        }
      >
        <form onSubmit={saveRoom} className="space-y-4">
          {formError && <Alert tone="error">{formError}</Alert>}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Mã phòng học" required hint="Mã duy nhất, chuẩn hóa chữ hoa (VD: A101, LAB02, HT01)">
              <input
                type="text"
                value={form.ma}
                onChange={(e) => setForm({ ...form, ma: e.target.value })}
                placeholder="VD: A101"
                className="input w-full font-mono uppercase"
                required
              />
            </Field>

            <Field label="Tên phòng học" required hint="Tên gọi đầy đủ (VD: Giảng đường A101, Phòng máy tính 02)">
              <input
                type="text"
                value={form.ten}
                onChange={(e) => setForm({ ...form, ten: e.target.value })}
                placeholder="VD: Giảng đường A101"
                className="input w-full"
                required
              />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Tòa nhà" hint="VD: Nhà A, Nhà B, Khu C, HT">
              <input
                type="text"
                value={form.toaNha}
                onChange={(e) => setForm({ ...form, toaNha: e.target.value })}
                placeholder="VD: Nhà A"
                className="input w-full"
              />
            </Field>

            <Field label="Tầng" hint="Số nguyên (VD: 1, 2, 3)">
              <input
                type="number"
                value={form.tang}
                onChange={(e) => setForm({ ...form, tang: e.target.value })}
                placeholder="VD: 1"
                className="input w-full"
              />
            </Field>

            <Field label="Sức chứa (chỗ)" required hint="Số nguyên dương (> 0)">
              <input
                type="number"
                min="1"
                value={form.sucChua}
                onChange={(e) => setForm({ ...form, sucChua: e.target.value })}
                placeholder="VD: 60"
                className="input w-full"
                required
              />
            </Field>
          </div>

          <Field label="Vị trí chi tiết" hint="Mô tả cụ thể hướng hành lang hoặc cửa vào">
            <input
              type="text"
              value={form.viTri}
              onChange={(e) => setForm({ ...form, viTri: e.target.value })}
              placeholder="VD: Cánh Tây, gần cầu thang bộ số 1"
              className="input w-full"
            />
          </Field>

          {/* Loai phong with suggestions */}
          <Field label="Loại phòng học" hint="Chọn danh mục gợi ý hoặc tự nhập loại mới nếu chưa có trong danh mục">
            <div className="space-y-2">
              <input
                type="text"
                value={form.loaiPhong}
                onChange={(e) => setForm({ ...form, loaiPhong: e.target.value })}
                placeholder="VD: Lý thuyết, Thực hành máy tính, Hội trường..."
                className="input w-full"
              />
              <div className="flex flex-wrap gap-1 text-xs">
                <span className="text-slate-400 py-0.5">Gợi ý:</span>
                {(taxonomy.loaiPhongList || [
                  "Lý thuyết",
                  "Thực hành máy tính",
                  "Hội trường",
                  "Thí nghiệm",
                  "Đa năng",
                ]).map((tax) => (
                  <button
                    key={tax}
                    type="button"
                    onClick={() => setForm({ ...form, loaiPhong: tax })}
                    className="rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-700 hover:bg-slate-100 transition"
                  >
                    {tax}
                  </button>
                ))}
              </div>
            </div>
          </Field>

          {/* Equipment with suggestions */}
          <Field
            label="Trang thiết bị"
            hint="Nhập danh sách thiết bị ngăn cách bởi dấu phẩy hoặc click chọn từ gợi ý bên dưới"
          >
            <div className="space-y-2">
              <input
                type="text"
                value={form.thietBi}
                onChange={(e) => setForm({ ...form, thietBi: e.target.value })}
                placeholder="VD: Máy chiếu, Micro không dây, Máy lạnh, Bảng thông minh"
                className="input w-full"
              />
              <div className="flex flex-wrap gap-1 text-xs">
                <span className="text-slate-400 py-0.5">Thêm nhanh:</span>
                {(taxonomy.thietBiList || [
                  "Máy chiếu",
                  "Micro không dây",
                  "Máy lạnh",
                  "Bảng thông minh",
                  "Dàn âm thanh",
                  "45 PC Core i7",
                  "Hệ thống thí nghiệm cơ bản",
                ]).map((eq) => (
                  <button
                    key={eq}
                    type="button"
                    onClick={() => {
                      const current = form.thietBi
                        ? form.thietBi.split(",").map((s) => s.trim()).filter(Boolean)
                        : [];
                      if (!current.includes(eq)) {
                        setForm({
                          ...form,
                          thietBi: current.concat(eq).join(", "),
                        });
                      }
                    }}
                    className="rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-700 hover:bg-indigo-50 hover:border-indigo-200 hover:text-indigo-700 transition"
                  >
                    + {eq}
                  </button>
                ))}
              </div>
            </div>
          </Field>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Trạng thái ban đầu" required>
              <select
                value={form.trangThai}
                onChange={(e) => setForm({ ...form, trangThai: e.target.value })}
                className="input w-full"
              >
                <option value="HOAT_DONG">Hoạt động (sẵn sàng xếp lịch)</option>
                <option value="BAO_TRI">Bảo trì (tạm thời không xếp lịch mới)</option>
                <option value="NGUNG_SU_DUNG">Ngừng sử dụng (ngừng hoạt động vĩnh viễn)</option>
              </select>
            </Field>

            <Field label="Ghi chú">
              <input
                type="text"
                value={form.ghiChu}
                onChange={(e) => setForm({ ...form, ghiChu: e.target.value })}
                placeholder="VD: Bảo dưỡng kỳ 2 năm 2026..."
                className="input w-full"
              />
            </Field>
          </div>
        </form>
      </Modal>

      {/* Modal Đổi Trạng thái nhanh */}
      <Modal
        open={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        title={`Cập nhật trạng thái: Phòng ${quickStatus.ma}`}
        description="Chuyển trạng thái phòng học phục vụ công tác xếp lịch hoặc bảo dưỡng thiết bị."
        size="md"
        footer={
          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => setStatusModalOpen(false)}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={saveQuickStatus}
              disabled={savingStatus}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
            >
              {savingStatus ? "Đang lưu..." : "Cập nhật trạng thái"}
            </button>
          </div>
        }
      >
        <form onSubmit={saveQuickStatus} className="space-y-4">
          <Field label="Trạng thái phòng học" required>
            <select
              value={quickStatus.trangThai}
              onChange={(e) => setQuickStatus({ ...quickStatus, trangThai: e.target.value })}
              className="input w-full text-sm"
            >
              <option value="HOAT_DONG">🟢 Hoạt động (Có thể xếp lịch lớp học phần mới)</option>
              <option value="BAO_TRI">🟡 Bảo trì (Chặn xếp lịch lớp học phần mới)</option>
              <option value="NGUNG_SU_DUNG">🔴 Ngừng sử dụng (Bảo toàn lịch sử lớp cũ, chặn xếp mới)</option>
            </select>
          </Field>

          <Field label="Ghi chú lý do thay đổi" hint="Ghi rõ lý do bảo trì hoặc thời gian dự kiến hoàn thành">
            <textarea
              rows={3}
              value={quickStatus.ghiChu}
              onChange={(e) => setQuickStatus({ ...quickStatus, ghiChu: e.target.value })}
              placeholder="VD: Thay thế máy chiếu và bảo trì dàn lạnh đến hết ngày 15/11/2026..."
              className="input w-full text-sm"
            />
          </Field>

          <div className="rounded-xl border border-blue-100 bg-blue-50/60 p-3 text-xs text-blue-900 flex items-start gap-2">
            <Info size={16} className="shrink-0 text-blue-600 mt-0.5" />
            <span>
              Quy tắc hệ thống: Khi phòng ở trạng thái <strong>Bảo trì</strong> hoặc <strong>Ngừng sử dụng</strong>,
              quản trị viên sẽ không thể tạo mới hoặc cập nhật lớp học phần chọn phòng này. Lớp/lịch cũ tiếp tục hiển thị
              bình thường.
            </span>
          </div>
        </form>
      </Modal>
    </div>
  );
}
