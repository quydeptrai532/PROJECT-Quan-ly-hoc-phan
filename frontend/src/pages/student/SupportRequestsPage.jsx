import { useCallback, useEffect, useState } from "react";
import {
  CheckCircle2,
  Clock,
  Download,
  Eye,
  FileText,
  History,
  LifeBuoy,
  MessageSquare,
  Paperclip,
  Plus,
  Search,
  Send,
  Trash2,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem } from "../../lib/format";

const PAGE_SIZE = 10;

export const PRIORITY_TONE = {
  THAP: "slate",
  TRUNG_BINH: "sky",
  CAO: "amber",
  KHAN_CAP: "red",
};

export const STATUS_TONE = {
  MOI_TAO: "sky",
  DANG_XU_LY: "indigo",
  CHO_PHAN_HOI_SINH_VIEN: "amber",
  DA_GIAI_QUYET: "green",
  TU_CHOI: "red",
  DA_DONG: "slate",
};

export function formatFileSize(bytes) {
  const num = Number(bytes) || 0;
  if (num < 1024) return `${num} B`;
  if (num < 1024 * 1024) return `${(num / 1024).toFixed(1)} KB`;
  return `${(num / (1024 * 1024)).toFixed(2)} MB`;
}

export async function downloadAttachmentFile(ticketId, attachment, onError) {
  try {
    const response = await api.get(
      `/yeu-cau-ho-tro/${ticketId}/tep-dinh-kem/${attachment.id}`,
      { responseType: "blob" }
    );
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = blobUrl;
    link.setAttribute("download", attachment.tenTepGoc || `tep-dinh-kem-${attachment.id}`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
  } catch (err) {
    if (onError) onError(getErrorMessage(err));
  }
}

export default function SupportRequestsPage() {
  const [catalog, setCatalog] = useState(null);
  const [filters, setFilters] = useState({ trangThai: "", loaiYeuCau: "", keyword: "" });
  const [appliedFilters, setAppliedFilters] = useState({ trangThai: "", loaiYeuCau: "", keyword: "" });
  const [pageNumber, setPageNumber] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Create modal state
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({
    loaiYeuCau: "",
    tieuDe: "",
    noiDung: "",
    monHocId: "",
    hocKyId: "",
    diemId: "",
  });
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Detail modal state
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [replySubmitting, setReplySubmitting] = useState(false);

  // Additional evidence submission state
  const [additionalFiles, setAdditionalFiles] = useState([]);
  const [additionalNote, setAdditionalNote] = useState("");
  const [additionalSubmitting, setAdditionalSubmitting] = useState(false);
  const [additionalError, setAdditionalError] = useState("");

  useEffect(() => {
    let active = true;
    api
      .get("/yeu-cau-ho-tro/danh-muc")
      .then(({ data }) => {
        if (!active) return;
        setCatalog(data);
        if (data?.loaiYeuCau?.length) {
          setForm((prev) => ({ ...prev, loaiYeuCau: prev.loaiYeuCau || data.loaiYeuCau[0].ma }));
        }
      })
      .catch((err) => {
        if (active) setError(getErrorMessage(err));
      });
    return () => {
      active = false;
    };
  }, []);

  const loadList = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = { page: pageNumber, size: PAGE_SIZE };
      if (appliedFilters.trangThai) params.trangThai = appliedFilters.trangThai;
      if (appliedFilters.loaiYeuCau) params.loaiYeuCau = appliedFilters.loaiYeuCau;
      if (appliedFilters.keyword) params.keyword = appliedFilters.keyword;
      const { data } = await api.get("/yeu-cau-ho-tro/me", { params });
      setPageData(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [pageNumber, appliedFilters]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const openCreateModal = () => {
    setFormError("");
    setSelectedFiles([]);
    setForm({
      loaiYeuCau: catalog?.loaiYeuCau?.[0]?.ma || "HOC_PHAN",
      tieuDe: "",
      noiDung: "",
      monHocId: "",
      hocKyId: "",
      diemId: "",
    });
    setCreateOpen(true);
  };

  const handleSelectDiem = (diemIdValue) => {
    if (!diemIdValue) {
      setForm((prev) => ({ ...prev, diemId: "" }));
      return;
    }
    const found = (catalog?.diemCuaToi || []).find((d) => String(d.id) === String(diemIdValue));
    setForm((prev) => ({
      ...prev,
      diemId: diemIdValue,
      monHocId: found?.monHocId ? String(found.monHocId) : prev.monHocId,
      hocKyId: found?.hocKyId ? String(found.hocKyId) : prev.hocKyId,
    }));
  };

  const handleFileChange = (event) => {
    setFormError("");
    const incoming = Array.from(event.target.files || []);
    if (!incoming.length) return;

    const maxFiles = catalog?.chinhSachTep?.maxFilesPerRequest || 3;
    const maxBytes = catalog?.chinhSachTep?.maxFileSizeBytes || 5 * 1024 * 1024;
    const allowedExts = (catalog?.chinhSachTep?.allowedExtensions || ["pdf", "png", "jpg", "jpeg"]).map((e) =>
      e.toLowerCase()
    );

    const merged = [...selectedFiles, ...incoming];
    if (merged.length > maxFiles) {
      setFormError(`Chỉ được đính kèm tối đa ${maxFiles} tệp cho mỗi phiếu.`);
      event.target.value = "";
      return;
    }

    for (const file of incoming) {
      const parts = file.name.split(".");
      const ext = parts.length > 1 ? parts[parts.length - 1].toLowerCase() : "";
      if (!allowedExts.includes(ext)) {
        setFormError(
          `Tệp "${file.name}" không đúng định dạng cho phép (${allowedExts.join(", ").toUpperCase()}).`
        );
        event.target.value = "";
        return;
      }
      if (file.size <= 0) {
        setFormError(`Tệp "${file.name}" đang rỗng.`);
        event.target.value = "";
        return;
      }
      if (file.size > maxBytes) {
        setFormError(
          `Tệp "${file.name}" vượt quá dung lượng tối đa ${formatFileSize(maxBytes)}.`
        );
        event.target.value = "";
        return;
      }
    }

    setSelectedFiles(merged);
    event.target.value = "";
  };

  const removeSelectedFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleCreateSubmit = async (event) => {
    event.preventDefault();
    setFormError("");
    setSuccessMsg("");

    const tieuDeTrim = form.tieuDe.trim();
    const noiDungTrim = form.noiDung.trim();
    if (!form.loaiYeuCau) {
      setFormError("Vui lòng chọn loại yêu cầu.");
      return;
    }
    if (tieuDeTrim.length < 5 || tieuDeTrim.length > 200) {
      setFormError("Tiêu đề phải có độ dài từ 5 đến 200 ký tự.");
      return;
    }
    if (noiDungTrim.length < 10 || noiDungTrim.length > 4000) {
      setFormError("Nội dung chi tiết phải có độ dài từ 10 đến 4000 ký tự.");
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        loaiYeuCau: form.loaiYeuCau,
        tieuDe: tieuDeTrim,
        noiDung: noiDungTrim,
        monHocId: form.monHocId ? Number(form.monHocId) : null,
        hocKyId: form.hocKyId ? Number(form.hocKyId) : null,
        diemId: form.diemId ? Number(form.diemId) : null,
      };

      let response;
      if (selectedFiles.length > 0) {
        const formData = new FormData();
        formData.append(
          "payload",
          new Blob([JSON.stringify(payload)], { type: "application/json" })
        );
        selectedFiles.forEach((file) => formData.append("files", file));
        response = await api.post("/yeu-cau-ho-tro", formData);
      } else {
        response = await api.post("/yeu-cau-ho-tro", payload);
      }

      setCreateOpen(false);
      setSuccessMsg(`Đã gửi phiếu hỗ trợ ${response.data.maPhieu} thành công.`);
      setPageNumber(0);
      await loadList();
      setSelectedTicket(response.data);
      setDetailOpen(true);
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const openTicketDetail = async (ticketId) => {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailError("");
    setReplyText("");
    try {
      const { data } = await api.get(`/yeu-cau-ho-tro/${ticketId}`);
      setSelectedTicket(data);
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setDetailLoading(false);
    }
  };

  const handleReplySubmit = async (event) => {
    event.preventDefault();
    if (!selectedTicket) return;
    const trimmed = replyText.trim();
    if (!trimmed) {
      setDetailError("Vui lòng nhập nội dung phản hồi.");
      return;
    }
    setReplySubmitting(true);
    setDetailError("");
    try {
      await api.post(`/yeu-cau-ho-tro/${selectedTicket.id}/trao-doi`, {
        noiDung: trimmed,
      });
      setReplyText("");
      const { data } = await api.get(`/yeu-cau-ho-tro/${selectedTicket.id}`);
      setSelectedTicket(data);
      loadList();
    } catch (err) {
      setDetailError(getErrorMessage(err));
    } finally {
      setReplySubmitting(false);
    }
  };

  const applyFilterSubmit = (event) => {
    event.preventDefault();
    setPageNumber(0);
    setAppliedFilters({ ...filters });
  };

  const resetFilters = () => {
    const empty = { trangThai: "", loaiYeuCau: "", keyword: "" };
    setFilters(empty);
    setAppliedFilters(empty);
    setPageNumber(0);
  };

  const items = pageData?.content || [];
  const policy = catalog?.chinhSachTep;

  return (
    <div className="space-y-6">
      <PageHeader
        icon={LifeBuoy}
        eyebrow="Hỗ trợ học vụ"
        title="Yêu cầu hỗ trợ & khiếu nại"
        description="Gửi yêu cầu hỗ trợ hoặc khiếu nại về môn học, kết quả điểm, học kỳ kèm minh chứng và theo dõi tiến độ xử lý."
        actions={
          <button type="button" className="btn-primary" onClick={openCreateModal}>
            <Plus size={16} /> Tạo phiếu hỗ trợ
          </button>
        }
      />

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

      <form
        onSubmit={applyFilterSubmit}
        className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"
      >
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
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
            Tìm theo mã phiếu hoặc tiêu đề
            <div className="mt-1 flex gap-2">
              <input
                type="text"
                className="input flex-1"
                placeholder="Nhập mã phiếu (VD: HT-...) hoặc từ khóa tiêu đề"
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

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Đang tải danh sách phiếu hỗ trợ..." />
        ) : items.length === 0 ? (
          <EmptyState
            icon={LifeBuoy}
            title="Chưa có phiếu hỗ trợ nào"
            description="Bạn có thể gửi phiếu yêu cầu hỗ trợ hoặc khiếu nại về học phần, điểm số hoặc học kỳ khi cần giải đáp."
            action={
              <button type="button" className="btn-primary" onClick={openCreateModal}>
                <Plus size={16} /> Gửi phiếu hỗ trợ đầu tiên
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[880px] text-left text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Mã phiếu</th>
                  <th className="th">Loại yêu cầu</th>
                  <th className="th">Tiêu đề & Đối tượng liên quan</th>
                  <th className="th">Trạng thái</th>
                  <th className="th">Ngày gửi</th>
                  <th className="th">Cập nhật</th>
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
                      <Badge tone="indigo" dot={false}>
                        {item.tenLoaiYeuCau}
                      </Badge>
                    </td>
                    <td className="td max-w-md">
                      <p className="font-medium text-slate-900">{item.tieuDe}</p>
                      <div className="mt-1 flex flex-wrap gap-2 text-xs text-slate-500">
                        {item.monHoc && (
                          <span>
                            Môn: {item.monHoc.ma} - {item.monHoc.ten}
                          </span>
                        )}
                        {item.hocKy && (
                          <span>
                            · {item.hocKy.ten} ({item.hocKy.namHoc})
                          </span>
                        )}
                        {item.diem && (
                          <span>
                            · Lớp {item.diem.maLop} (Điểm TK: {formatDiem(item.diem.diemTongKet)})
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="td">
                      <Badge tone={STATUS_TONE[item.trangThai] || "slate"}>
                        {item.tenTrangThai}
                      </Badge>
                    </td>
                    <td className="td whitespace-nowrap text-xs text-slate-600">
                      {formatDateTime(item.createdAt)}
                    </td>
                    <td className="td whitespace-nowrap text-xs text-slate-600">
                      {formatDateTime(item.updatedAt)}
                    </td>
                    <td className="td text-right">
                      <button
                        type="button"
                        className="btn-secondary py-1.5 text-xs"
                        onClick={() => openTicketDetail(item.id)}
                      >
                        <Eye size={14} /> Chi tiết
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

      {/* Create Ticket Modal */}
      <Modal
        open={createOpen}
        onClose={() => !submitting && setCreateOpen(false)}
        title="Tạo phiếu hỗ trợ / khiếu nại"
        description="Điền đầy đủ thông tin bên dưới. Lưu ý: Nội dung gốc của phiếu không thể chỉnh sửa sau khi gửi."
        size="lg"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          {formError && <Alert tone="error">{formError}</Alert>}

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Loại yêu cầu" required>
              <select
                className="input w-full"
                value={form.loaiYeuCau}
                onChange={(e) => setForm((prev) => ({ ...prev, loaiYeuCau: e.target.value }))}
                required
              >
                {(catalog?.loaiYeuCau || []).map((item) => (
                  <option key={item.ma} value={item.ma}>
                    {item.ten}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Học kỳ liên quan (tùy chọn)">
              <select
                className="input w-full"
                value={form.hocKyId}
                onChange={(e) => setForm((prev) => ({ ...prev, hocKyId: e.target.value }))}
              >
                <option value="">-- Không chọn --</option>
                {(catalog?.hocKy || []).map((hk) => (
                  <option key={hk.id} value={hk.id}>
                    {hk.ten} - {hk.namHoc}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Môn học liên quan (tùy chọn)">
              <select
                className="input w-full"
                value={form.monHocId}
                onChange={(e) => setForm((prev) => ({ ...prev, monHocId: e.target.value }))}
              >
                <option value="">-- Không chọn --</option>
                {(catalog?.monHoc || []).map((mh) => (
                  <option key={mh.id} value={mh.id}>
                    {mh.ma} - {mh.ten} ({mh.soTinChi} TC)
                  </option>
                ))}
              </select>
            </Field>

            <Field
              label="Bản ghi điểm liên quan (tùy chọn)"
              hint="Chọn khi bạn cần phúc khảo hoặc khiếu nại điểm của lớp học phần đã có điểm."
            >
              <select
                className="input w-full"
                value={form.diemId}
                onChange={(e) => handleSelectDiem(e.target.value)}
              >
                <option value="">-- Không chọn --</option>
                {(catalog?.diemCuaToi || []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.maMon} ({d.maLop}) · {d.tenHocKy} {d.namHoc} · Điểm TK: {formatDiem(d.diemTongKet)}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <Field
            label="Tiêu đề phiếu"
            required
            hint={`Từ 5 đến 200 ký tự (${form.tieuDe.trim().length}/200)`}
          >
            <input
              type="text"
              className="input w-full"
              maxLength={200}
              placeholder="Ví dụ: Đề nghị kiểm tra lại điểm cuối kỳ môn Cấu trúc dữ liệu"
              value={form.tieuDe}
              onChange={(e) => setForm((prev) => ({ ...prev, tieuDe: e.target.value }))}
              required
            />
          </Field>

          <Field
            label="Nội dung chi tiết"
            required
            hint={`Từ 10 đến 4000 ký tự (${form.noiDung.trim().length}/4000). Nội dung gốc sẽ được khóa sau khi gửi.`}
          >
            <textarea
              rows={5}
              className="input w-full"
              maxLength={4000}
              placeholder="Mô tả rõ vấn đề, mã lớp học phần, thời điểm phát sinh và nguyện vọng hỗ trợ..."
              value={form.noiDung}
              onChange={(e) => setForm((prev) => ({ ...prev, noiDung: e.target.value }))}
              required
            />
          </Field>

          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50/70 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-sm font-medium text-slate-800">
                  Tệp minh chứng đính kèm (tùy chọn)
                </p>
                <p className="mt-0.5 text-xs text-slate-500">
                  Tối đa {policy?.maxFilesPerRequest || 3} tệp · Mỗi tệp không quá{" "}
                  {formatFileSize(policy?.maxFileSizeBytes || 5 * 1024 * 1024)} · Định dạng:{" "}
                  {(policy?.allowedExtensions || ["pdf", "png", "jpg", "jpeg"])
                    .join(", ")
                    .toUpperCase()}
                </p>
              </div>
              <label className="btn-secondary cursor-pointer text-xs">
                <Paperclip size={14} /> Chọn tệp
                <input
                  type="file"
                  multiple
                  accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                  className="hidden"
                  onChange={handleFileChange}
                />
              </label>
            </div>

            {selectedFiles.length > 0 && (
              <ul className="mt-3 divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
                {selectedFiles.map((file, index) => (
                  <li
                    key={`${file.name}-${index}`}
                    className="flex items-center justify-between gap-3 px-3 py-2 text-xs"
                  >
                    <div className="flex min-w-0 items-center gap-2">
                      <FileText size={14} className="shrink-0 text-slate-500" />
                      <span className="truncate font-medium text-slate-700">{file.name}</span>
                      <span className="shrink-0 text-slate-400">({formatFileSize(file.size)})</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeSelectedFile(index)}
                      className="rounded p-1 text-rose-600 hover:bg-rose-50"
                      title="Gỡ tệp"
                    >
                      <Trash2 size={14} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              className="btn-secondary"
              disabled={submitting}
              onClick={() => setCreateOpen(false)}
            >
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={submitting}>
              <Send size={15} /> {submitting ? "Đang gửi phiếu..." : "Gửi phiếu hỗ trợ"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Ticket Detail Modal */}
      <Modal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={
          selectedTicket
            ? `Chi tiết phiếu ${selectedTicket.maPhieu}`
            : "Chi tiết phiếu hỗ trợ"
        }
        description={
          selectedTicket
            ? `${selectedTicket.tenLoaiYeuCau} · Gửi lúc ${formatDateTime(selectedTicket.createdAt)}`
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

              {/* Summary & Status Bar */}
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge tone={STATUS_TONE[selectedTicket.trangThai] || "slate"}>
                    {selectedTicket.tenTrangThai}
                  </Badge>
                  <Badge tone="indigo" dot={false}>
                    {selectedTicket.tenLoaiYeuCau}
                  </Badge>
                  {selectedTicket.nguoiXuLyUsername && (
                    <span className="text-xs text-slate-600">
                      Cán bộ phụ trách: <strong>{selectedTicket.nguoiXuLyUsername}</strong>
                    </span>
                  )}
                </div>
                <span className="text-xs text-slate-500">
                  Cập nhật gần nhất: {formatDateTime(selectedTicket.updatedAt)}
                </span>
              </div>

              {/* Original Immutable Content */}
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                  <h3 className="text-sm font-semibold text-slate-900">
                    {selectedTicket.tieuDe}
                  </h3>
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">
                    Nội dung gốc (không chỉnh sửa)
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
                        {selectedTicket.monHoc.ma} - {selectedTicket.monHoc.ten} (
                        {selectedTicket.monHoc.soTinChi} TC)
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


              {/* Evidence Request from Staff Banner */}
              {selectedTicket.yeuCauBoSungMinhChung && (
                <div className="rounded-xl border border-amber-300 bg-amber-50/90 p-4 shadow-sm">
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
                    <div className="space-y-1 flex-1">
                      <h4 className="text-sm font-semibold text-amber-950">
                        Yêu cầu bổ sung minh chứng từ Phòng Đào tạo
                      </h4>
                      <p className="text-xs text-amber-800 leading-relaxed whitespace-pre-line">
                        {selectedTicket.noiDungYeuCauMinhChung}
                      </p>
                    </div>
                  </div>

                  <form onSubmit={handleUploadAdditionalEvidence} className="mt-4 space-y-3 border-t border-amber-200 pt-3">
                    {additionalError && <Alert tone="error">{additionalError}</Alert>}
                    <Field label="Nội dung giải trình bổ sung (tùy chọn)">
                      <textarea
                        rows={2}
                        maxLength={1000}
                        className="input w-full text-xs"
                        placeholder="Nhập phản hồi hoặc mô tả tài liệu bổ sung..."
                        value={additionalNote}
                        onChange={(e) => setAdditionalNote(e.target.value)}
                      />
                    </Field>

                    <div>
                      <label className="block text-xs font-medium text-slate-700">
                        Chọn tệp đính kèm bổ sung ({catalog?.chinhSachTep?.allowedExtensions?.join(", ").toUpperCase() || "PDF, PNG, JPG"})
                      </label>
                      <input
                        type="file"
                        multiple
                        className="input mt-1 w-full text-xs"
                        onChange={handleAdditionalFileChange}
                      />
                    </div>

                    {additionalFiles.length > 0 && (
                      <div className="space-y-1">
                        {additionalFiles.map((f, idx) => (
                          <div key={idx} className="flex items-center justify-between rounded bg-white px-2.5 py-1 text-xs border border-amber-200">
                            <span className="truncate">{f.name} ({formatFileSize(f.size)})</span>
                            <button
                              type="button"
                              className="text-rose-600 hover:text-rose-800 font-medium ml-2"
                              onClick={() => setAdditionalFiles((prev) => prev.filter((_, i) => i !== idx))}
                            >
                              Xóa
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    <div className="flex justify-end pt-1">
                      <button type="submit" className="btn-primary text-xs" disabled={additionalSubmitting}>
                        {additionalSubmitting ? "Đang gửi minh chứng..." : "Gửi minh chứng bổ sung"}
                      </button>
                    </div>
                  </form>
                </div>
              )}
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
                    Trao đổi trên phiếu ({selectedTicket.traoDoi?.length || 0})
                  </h4>

                  {!selectedTicket.traoDoi?.length ? (
                    <p className="py-4 text-center text-xs text-slate-500">
                      Chưa có trao đổi bổ sung nào trên phiếu này.
                    </p>
                  ) : (
                    <div className="max-h-72 space-y-3 overflow-y-auto pr-1">
                      {selectedTicket.traoDoi.map((msg) => {
                        const isAdmin = msg.vaiTroNguoiGui === "QUAN_TRI";
                        return (
                          <div
                            key={msg.id}
                            className={`rounded-xl border p-3 text-sm ${
                              isAdmin
                                ? "border-indigo-100 bg-indigo-50/60"
                                : "border-slate-200 bg-slate-50/70"
                            }`}
                          >
                            <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                              <span className="font-semibold text-slate-800">
                                {msg.tenHienThiNguoiGui}
                              </span>
                              <div className="flex items-center gap-2">
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
                        placeholder="Nhập nội dung phản hồi hoặc bổ sung thông tin..."
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        required
                      />
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-400">
                          {replyText.trim().length}/2000 ký tự
                        </span>
                        <button
                          type="submit"
                          className="btn-primary py-1.5 text-xs"
                          disabled={replySubmitting}
                        >
                          <Send size={14} />{" "}
                          {replySubmitting ? "Đang gửi..." : "Gửi trao đổi"}
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

                {/* Status History Timeline */}
                <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 lg:col-span-2">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    <History size={16} className="text-slate-500" />
                    Lịch sử trạng thái ({selectedTicket.lichSuTrangThai?.length || 0})
                  </h4>

                  <ol className="relative ml-2 border-l border-slate-200 space-y-4">
                    {(selectedTicket.lichSuTrangThai || []).map((hist) => (
                      <li key={hist.id} className="ml-4 text-xs">
                        <span className="absolute -left-1.5 mt-1 h-3 w-3 rounded-full border border-white bg-emerald-600" />
                        <div className="flex flex-wrap items-center gap-1.5">
                          {hist.tenTrangThaiCu && (
                            <>
                              <span className="text-slate-500">{hist.tenTrangThaiCu}</span>
                              <span className="text-slate-400">→</span>
                            </>
                          )}
                          <Badge tone={STATUS_TONE[hist.trangThaiMoi] || "slate"} dot={false}>
                            {hist.tenTrangThaiMoi}
                          </Badge>
                        </div>
                        <p className="mt-1 text-slate-600">
                          Thực hiện bởi: <strong>{hist.tenNguoiThucHien}</strong> (
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
    </div>
  );
}
