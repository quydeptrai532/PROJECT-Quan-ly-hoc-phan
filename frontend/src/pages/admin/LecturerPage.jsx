import { useMemo, useState } from "react";
import { Download, Lock, LockOpen, Pencil, Plus, Search, Upload, UserCog } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import UserImportModal from "../../components/UserImportModal";
import { useApi } from "../../lib/useApi";

const EMPTY_FORM = {
  ma: "",
  hoTen: "",
  email: "",
  soDienThoai: "",
  khoaId: "",
  username: "",
  matKhau: "",
};

export default function LecturerPage() {
  const confirm = useConfirm();
  const { data: giangVienList, loading, error, reload } = useApi("/admin/giang-vien");
  const { data: khoaList } = useApi("/admin/khoa");

  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [busyId, setBusyId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const rows = useMemo(() => {
    const list = giangVienList || [];
    const term = search.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (item) =>
        item.ma.toLowerCase().includes(term) ||
        item.hoTen.toLowerCase().includes(term) ||
        (item.email || "").toLowerCase().includes(term) ||
        item.username.toLowerCase().includes(term)
    );
  }, [giangVienList, search]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (row) => {
    setEditing(row);
    setForm({
      ma: row.ma,
      hoTen: row.hoTen,
      email: row.email || "",
      soDienThoai: row.soDienThoai || "",
      khoaId: row.khoaId ? String(row.khoaId) : "",
      username: row.username,
      matKhau: "",
    });
    setFormError("");
    setModalOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    setMessage("");
    setActionError("");
    try {
      const payload = {
        ma: form.ma.trim(),
        hoTen: form.hoTen.trim(),
        email: form.email.trim() || null,
        soDienThoai: form.soDienThoai.trim() || null,
        khoaId: form.khoaId ? Number(form.khoaId) : null,
        username: form.username.trim(),
        matKhau: form.matKhau || null,
      };
      if (editing) {
        await api.put(`/admin/giang-vien/${editing.id}`, payload);
        setMessage("Cập nhật giảng viên thành công");
      } else {
        await api.post("/admin/giang-vien", payload);
        setMessage("Thêm giảng viên thành công");
      }
      setModalOpen(false);
      reload();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const doiTrangThai = async (row) => {
    const action = row.trangThai ? "khóa" : "mở khóa";
    const daXacNhan = await confirm({
      title: row.trangThai ? "Khóa tài khoản" : "Mở khóa tài khoản",
      message: `Bạn có chắc muốn ${action} tài khoản ${row.username} của ${row.hoTen}?`,
      detail: row.trangThai
        ? "Giảng viên sẽ không thể đăng nhập cho đến khi tài khoản được mở khóa lại."
        : "Giảng viên có thể đăng nhập và sử dụng hệ thống ngay sau khi mở khóa.",
      confirmLabel: row.trangThai ? "Khóa tài khoản" : "Mở khóa",
      tone: row.trangThai ? "security" : "primary",
    });
    if (!daXacNhan) return;
    setBusyId(row.id);
    setActionError("");
    setMessage("");
    try {
      await api.patch(`/admin/giang-vien/${row.id}/trang-thai`, { trangThai: !row.trangThai });
      setMessage(`Đã ${action} tài khoản ${row.username}`);
      reload();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  const taiFileMau = async () => {
    try {
      const response = await api.get("/admin/giang-vien/import/template", { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = "mau-import-giang-vien.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      const csv = "\uFEFFma,hoTen,email,soDienThoai,khoaId\nGV010,Nguyen Van B,b@lecturer.edu.vn,0912345678,1\n";
      const url = window.URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = "mau-import-giang-vien.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    }
  };

  return (
    <div>
      <PageHeader
        icon={UserCog}
        title="Giảng viên"
        description="Quản lý hồ sơ giảng viên và trạng thái tài khoản giảng dạy."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <button type="button" className="btn-secondary" onClick={taiFileMau}>
              <Download size={16} /> File mẫu
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setImportModalOpen(true)}
            >
              <Upload size={16} /> Nhập CSV (Xem trước & Kiểm tra)
            </button>
            <button type="button" className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> Thêm giảng viên
            </button>
          </div>
        }
      />

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {actionError && <Alert tone="error" onClose={() => setActionError("")}>{actionError}</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-sm">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Tìm theo mã, họ tên, email..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <p className="text-sm text-slate-500">{rows.length} giảng viên</p>
        </div>

        {loading ? (
          <Spinner />
        ) : rows.length === 0 ? (
          <EmptyState icon={UserCog} title="Chưa có giảng viên" description="Thêm giảng viên để phân công lớp học phần." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[880px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã GV</th>
                  <th className="th">Họ tên</th>
                  <th className="th">Liên hệ</th>
                  <th className="th">Khoa</th>
                  <th className="th">Tài khoản</th>
                  <th className="th text-center">Trạng thái</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-slate-50/60">
                    <td className="td font-medium text-slate-900">{row.ma}</td>
                    <td className="td">
                      <div className="flex items-center gap-2.5">
                        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-emerald-100 to-teal-100 text-xs font-semibold text-emerald-700">
                          {row.hoTen.slice(0, 1).toUpperCase()}
                        </span>
                        <span className="font-medium text-slate-800">{row.hoTen}</span>
                      </div>
                    </td>
                    <td className="td">
                      <p>{row.email || "—"}</p>
                      <p className="text-xs text-slate-500">{row.soDienThoai || "—"}</p>
                    </td>
                    <td className="td">{row.khoaTen || "—"}</td>
                    <td className="td font-mono text-xs">{row.username}</td>
                    <td className="td text-center">
                      <Badge tone={row.trangThai ? "green" : "red"}>
                        {row.trangThai ? "Hoạt động" : "Đã khóa"}
                      </Badge>
                    </td>
                    <td className="td">
                      <div className="flex justify-end gap-1">
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
                          className={`rounded-md p-1.5 ${
                            row.trangThai ? "text-amber-600 hover:bg-amber-50" : "text-emerald-600 hover:bg-emerald-50"
                          }`}
                          onClick={() => doiTrangThai(row)}
                          disabled={busyId === row.id}
                          title={row.trangThai ? "Khóa tài khoản" : "Mở khóa tài khoản"}
                        >
                          {row.trangThai ? <Lock size={15} /> : <LockOpen size={15} />}
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

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? "Cập nhật giảng viên" : "Thêm giảng viên"}
        size="lg"
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Hủy</button>
            <button type="submit" form="lecturer-form" className="btn-primary" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </button>
          </>
        }
      >
        <form id="lecturer-form" className="grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {formError && (
            <div className="sm:col-span-2"><Alert tone="error">{formError}</Alert></div>
          )}
          <Field label="Mã giảng viên" required>
            <input
              className="input"
              value={form.ma}
              onChange={(event) => setForm({ ...form, ma: event.target.value })}
              placeholder="VD: GV04"
              required
            />
          </Field>
          <Field label="Họ tên" required>
            <input
              className="input"
              value={form.hoTen}
              onChange={(event) => setForm({ ...form, hoTen: event.target.value })}
              required
            />
          </Field>
          <Field label="Email">
            <input
              className="input"
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
            />
          </Field>
          <Field label="Số điện thoại">
            <input
              className="input"
              value={form.soDienThoai}
              onChange={(event) => setForm({ ...form, soDienThoai: event.target.value })}
            />
          </Field>
          <Field label="Khoa">
            <select
              className="input"
              value={form.khoaId}
              onChange={(event) => setForm({ ...form, khoaId: event.target.value })}
            >
              <option value="">-- Chọn khoa --</option>
              {(khoaList || []).map((khoa) => (
                <option key={khoa.id} value={khoa.id}>{khoa.ma} - {khoa.ten}</option>
              ))}
            </select>
          </Field>
          <Field label="Tên đăng nhập" required>
            <input
              className="input"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              disabled={Boolean(editing)}
              required
            />
          </Field>
          <div className="sm:col-span-2">
            <Field
              label={editing ? "Mật khẩu mới" : "Mật khẩu"}
              hint={editing ? "Để trống nếu không đổi mật khẩu." : "Để trống để dùng mật khẩu mặc định 123456."}
            >
              <input
                className="input"
                type="password"
                value={form.matKhau}
                onChange={(event) => setForm({ ...form, matKhau: event.target.value })}
                minLength={6}
              />
            </Field>
          </div>
        </form>
      </Modal>

      <UserImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        type="giang-vien"
        onSuccess={reload}
      />
    </div>
  );
}
