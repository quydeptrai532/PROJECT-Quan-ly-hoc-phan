import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Eye,
  FileCheck,
  History,
  Plus,
  RotateCcw,
  XCircle,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const APPEAL_STATUS_META = {
  MOI_TAO: { label: "Mới tiếp nhận / Chờ xử lý", tone: "amber" },
  DANG_XU_LY: { label: "Đang xử lý / Chấm lại", tone: "blue" },
  CHAP_THUAN: { label: "Chấp thuận điều chỉnh", tone: "green" },
  TU_CHOI: { label: "Từ chối / Giữ nguyên", tone: "rose" },
  DA_HUY: { label: "Đã hủy", tone: "slate" },
};

const LOAI_DIEM_LABEL = {
  DIEM_QUA_TRINH: "Điểm quá trình",
  DIEM_CUOI_KY: "Điểm cuối kỳ",
  CA_HAI: "Cả hai điểm",
};

export default function GradeAppealsPage() {
  const [searchParams] = useSearchParams();
  const preselectedDangKyId = searchParams.get("dangKyId");

  const [appeals, setAppeals] = useState([]);
  const [catalog, setCatalog] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Create Modal
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedDangKyId, setSelectedDangKyId] = useState("");
  const [loaiDiem, setLoaiDiem] = useState("DIEM_CUOI_KY");
  const [diemMongMuon, setDiemMongMuon] = useState("");
  const [lyDo, setLyDo] = useState("");
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState("");

  // Cancel Modal
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancellingAppeal, setCancellingAppeal] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [cancelSubmitting, setCancelSubmitting] = useState(false);

  // Detail Modal
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedAppeal, setSelectedAppeal] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const [resAppeals, resCatalog] = await Promise.all([
        api.get("/sinh-vien/phuc-khao"),
        api.get("/sinh-vien/phuc-khao/danh-muc"),
      ]);
      setAppeals(resAppeals.data || []);
      setCatalog(resCatalog.data || null);

      if (preselectedDangKyId && resCatalog.data?.danhSachDiem) {
        const found = resCatalog.data.danhSachDiem.find(
          (d) => String(d.dangKyId) === String(preselectedDangKyId)
        );
        if (found) {
          setSelectedDangKyId(String(found.dangKyId));
          setCreateOpen(true);
        }
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateModal = () => {
    setSelectedDangKyId("");
    setLoaiDiem("DIEM_CUOI_KY");
    setDiemMongMuon("");
    setLyDo("");
    setCreateError("");
    setCreateOpen(true);
  };

  const selectedDiemItem = (catalog?.danhSachDiem || []).find(
    (item) => String(item.dangKyId) === String(selectedDangKyId)
  );

  const submitCreate = async (e) => {
    e.preventDefault();
    setCreateSubmitting(true);
    setCreateError("");
    try {
      const payload = {
        dangKyId: Number(selectedDangKyId),
        loaiDiem,
        lyDo: lyDo.trim(),
      };
      if (diemMongMuon !== "") {
        payload.diemMongMuon = parseFloat(diemMongMuon);
      }

      await api.post("/sinh-vien/phuc-khao", payload);
      setMessage("Nộp hồ sơ phúc khảo điểm thành công. Phòng Đào tạo sẽ tiếp nhận và phản hồi kết quả.");
      setCreateOpen(false);
      loadData();
    } catch (err) {
      setCreateError(getErrorMessage(err));
    } finally {
      setCreateSubmitting(false);
    }
  };

  const openCancelModal = (appeal) => {
    setCancellingAppeal(appeal);
    setCancelReason("Tôi muốn rút hồ sơ phúc khảo môn này");
    setCancelOpen(true);
  };

  const submitCancel = async (e) => {
    e.preventDefault();
    setCancelSubmitting(true);
    try {
      await api.post(`/sinh-vien/phuc-khao/${cancellingAppeal.id}/huy`, {
        lyDo: cancelReason,
      });
      setMessage(`Đã hủy hồ sơ phúc khảo ${cancellingAppeal.maHoSo}`);
      setCancelOpen(false);
      loadData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCancelSubmitting(false);
    }
  };

  const openDetail = async (appeal) => {
    setSelectedAppeal(appeal);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const { data } = await api.get(`/sinh-vien/phuc-khao/${appeal.id}`);
      setSelectedAppeal(data);
    } catch (err) {
      console.error("Lỗi tải chi tiết:", err);
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <div>
      <PageHeader
        icon={FileCheck}
        title="Phúc khảo điểm học phần"
        description="Nộp hồ sơ đề nghị xem xét lại điểm số, theo dõi tiến độ thẩm tra và nhận thông báo kết quả."
        actions={
          <button type="button" className="btn-primary" onClick={openCreateModal}>
            <Plus size={16} /> Nộp hồ sơ phúc khảo
          </button>
        }
      />

      <div className="mb-4 space-y-3">
        {catalog && (
          <Alert tone="info">
            <span className="font-semibold">Quy định tiếp nhận:</span> Thời hạn nộp hồ sơ phúc khảo là{" "}
            <span className="font-bold">{catalog.submissionWindowDays} ngày</span> kể từ ngày công bố điểm chính thức.{" "}
            {catalog.slaNote && <span className="block mt-1 text-xs text-slate-600">{catalog.slaNote}</span>}
          </Alert>
        )}

        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        <div className="border-b border-slate-200 p-4">
          <h3 className="text-sm font-semibold text-slate-800">
            Danh sách hồ sơ phúc khảo của bạn ({appeals.length})
          </h3>
        </div>

        {loading ? (
          <Spinner label="Đang tải danh sách hồ sơ..." />
        ) : appeals.length === 0 ? (
          <EmptyState
            icon={FileCheck}
            title="Bạn chưa có hồ sơ phúc khảo nào"
            description="Nếu phát hiện sai sót trong điểm quá trình hoặc điểm thi cuối kỳ của môn học đã công bố, bạn có thể nộp đơn phúc khảo trong thời hạn quy định."
            action={
              <button type="button" className="btn-primary" onClick={openCreateModal}>
                <Plus size={16} /> Nộp hồ sơ ngay
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="table">
              <thead className="thead">
                <tr>
                  <th className="th">Mã hồ sơ</th>
                  <th className="th">Môn học / Lớp</th>
                  <th className="th text-center">Điểm gốc (QT/CK/TK)</th>
                  <th className="th">Loại điểm</th>
                  <th className="th text-center">Trạng thái</th>
                  <th className="th">Ngày nộp</th>
                  <th className="th">Kết quả xử lý</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="tbody">
                {appeals.map((item) => {
                  const meta = APPEAL_STATUS_META[item.trangThai] || APPEAL_STATUS_META.MOI_TAO;
                  return (
                    <tr key={item.id}>
                      <td className="td font-mono font-semibold text-slate-800">{item.maHoSo}</td>
                      <td className="td">
                        <p className="font-medium text-slate-800">{item.tenMonHoc}</p>
                        <p className="text-xs text-slate-500">{item.maLopHocPhan} · {item.maMonHoc}</p>
                      </td>
                      <td className="td text-center font-mono">
                        <span className="text-slate-600">{formatDiem(item.diemQuaTrinhGoc)}</span> /{" "}
                        <span className="text-slate-600">{formatDiem(item.diemCuoiKyGoc)}</span> /{" "}
                        <span className="font-bold text-slate-900">{formatDiem(item.diemTongKetGoc)}</span>
                      </td>
                      <td className="td text-xs">
                        <span className="font-medium text-slate-700">{LOAI_DIEM_LABEL[item.loaiDiem] || item.loaiDiem}</span>
                        {item.diemMongMuon != null && (
                          <span className="block text-slate-400">Mong muốn: {formatDiem(item.diemMongMuon)}</span>
                        )}
                      </td>
                      <td className="td text-center">
                        <Badge tone={meta.tone}>{meta.label}</Badge>
                      </td>
                      <td className="td text-xs text-slate-500">{formatDateTime(item.createdAt)}</td>
                      <td className="td text-xs">
                        {item.trangThai === "CHAP_THUAN" ? (
                          <div className="font-semibold text-emerald-700">
                            <span>Điểm mới: TK {formatDiem(item.diemTongKetMoi)}</span>
                            {item.ketQuaXuLy && <p className="text-[11px] text-slate-600 font-normal truncate max-w-xs">{item.ketQuaXuLy}</p>}
                          </div>
                        ) : item.trangThai === "TU_CHOI" ? (
                          <div className="text-rose-700">
                            <span>Giữ nguyên điểm gốc</span>
                            {item.ketQuaXuLy && <p className="text-[11px] text-slate-600 truncate max-w-xs">{item.ketQuaXuLy}</p>}
                          </div>
                        ) : (
                          <span className="text-slate-400">Đang chờ xử lý...</span>
                        )}
                      </td>
                      <td className="td text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            className="rounded-md p-1.5 text-slate-600 hover:bg-slate-100"
                            onClick={() => openDetail(item)}
                            title="Xem chi tiết & lịch sử"
                          >
                            <Eye size={15} />
                          </button>
                          {item.trangThai === "MOI_TAO" && (
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-rose-600 hover:bg-rose-50"
                              onClick={() => openCancelModal(item)}
                              title="Rút hồ sơ phúc khảo"
                            >
                              <XCircle size={15} />
                            </button>
                          )}
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

      {/* Modal Nộp hồ sơ phúc khảo */}
      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Nộp hồ sơ phúc khảo điểm học phần"
        maxWidth="max-w-lg"
      >
        <form onSubmit={submitCreate} className="space-y-4">
          {createError && <Alert tone="error">{createError}</Alert>}

          <Field label="Chọn môn học đã công bố điểm">
            <select
              className="input"
              value={selectedDangKyId}
              onChange={(e) => setSelectedDangKyId(e.target.value)}
              required
            >
              <option value="">-- Chọn học phần --</option>
              {(catalog?.danhSachDiem || []).map((d) => (
                <option
                  key={d.dangKyId}
                  value={d.dangKyId}
                  disabled={d.quaHanNop || d.daCoDonDangXuLy}
                >
                  {d.maLop} - {d.tenMon} (TK: {formatDiem(d.diemTongKet)}){" "}
                  {d.daCoDonDangXuLy ? "[Đang xử lý]" : d.quaHanNop ? "[Hết hạn]" : ""}
                </option>
              ))}
            </select>
          </Field>

          {selectedDiemItem && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs space-y-1.5">
              <div className="flex justify-between">
                <span className="text-slate-500">Môn học:</span>
                <span className="font-semibold text-slate-800">{selectedDiemItem.tenMon} ({selectedDiemItem.maMon})</span>
              </div>
              <div className="flex justify-between font-mono">
                <span className="text-slate-500">Điểm hiện tại:</span>
                <span>
                  QT: <span className="font-bold">{formatDiem(selectedDiemItem.diemQuaTrinh)}</span> ·
                  CK: <span className="font-bold">{formatDiem(selectedDiemItem.diemCuoiKy)}</span> ·
                  TK: <span className="font-bold text-emerald-800">{formatDiem(selectedDiemItem.diemTongKet)}</span>
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Hạn nộp phúc khảo:</span>
                <span className={selectedDiemItem.quaHanNop ? "font-bold text-rose-600" : "font-medium text-slate-700"}>
                  {selectedDiemItem.hanNop ? formatDateTime(selectedDiemItem.hanNop) : "Không giới hạn"}
                  {selectedDiemItem.quaHanNop && " (Đã quá hạn)"}
                </span>
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <Field label="Loại điểm cần phúc khảo">
              <select
                className="input"
                value={loaiDiem}
                onChange={(e) => setLoaiDiem(e.target.value)}
                required
              >
                <option value="DIEM_CUOI_KY">Điểm thi cuối kỳ</option>
                <option value="DIEM_QUA_TRINH">Điểm quá trình</option>
                <option value="CA_HAI">Cả hai điểm</option>
              </select>
            </Field>

            <Field label="Điểm mong muốn (tùy chọn)">
              <input
                type="number"
                step="0.25"
                min="0"
                max="10"
                className="input"
                placeholder="VD: 8.5"
                value={diemMongMuon}
                onChange={(e) => setDiemMongMuon(e.target.value)}
              />
            </Field>
          </div>

          <Field label="Lý do đề nghị phúc khảo (tối thiểu 10 ký tự)">
            <textarea
              className="input h-24"
              placeholder="Trình bày rõ lý do đề nghị phúc khảo (ví dụ: đối chiếu đáp án câu 3, sót điểm thành phần...)"
              value={lyDo}
              onChange={(e) => setLyDo(e.target.value)}
              required
            />
          </Field>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-secondary" onClick={() => setCreateOpen(false)}>
              Hủy
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={createSubmitting || selectedDiemItem?.quaHanNop || selectedDiemItem?.daCoDonDangXuLy}
            >
              {createSubmitting ? "Đang gửi..." : "Gửi hồ sơ phúc khảo"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Hủy hồ sơ */}
      <Modal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        title="Rút hồ sơ phúc khảo"
        maxWidth="max-w-md"
      >
        <form onSubmit={submitCancel} className="space-y-3">
          <p className="text-xs text-slate-600">
            Bạn có chắc chắn muốn hủy hồ sơ phúc khảo <span className="font-bold text-slate-800">{cancellingAppeal?.maHoSo}</span> không?
          </p>
          <Field label="Lý do rút hồ sơ">
            <input
              type="text"
              className="input"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              required
            />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-secondary" onClick={() => setCancelOpen(false)}>
              Quay lại
            </button>
            <button type="submit" className="btn-primary text-rose-700 bg-rose-50 hover:bg-rose-100 border-rose-200" disabled={cancelSubmitting}>
              {cancelSubmitting ? "Đang hủy..." : "Xác nhận hủy đơn"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Xem chi tiết & Lịch sử */}
      <Modal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={selectedAppeal ? `Chi tiết hồ sơ: ${selectedAppeal.maHoSo}` : "Chi tiết hồ sơ"}
        maxWidth="max-w-lg"
      >
        {detailLoading ? (
          <Spinner label="Đang tải chi tiết..." />
        ) : selectedAppeal ? (
          <div className="space-y-3">
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Trạng thái:</span>
                <Badge tone={(APPEAL_STATUS_META[selectedAppeal.trangThai] || APPEAL_STATUS_META.MOI_TAO).tone}>
                  {(APPEAL_STATUS_META[selectedAppeal.trangThai] || APPEAL_STATUS_META.MOI_TAO).label}
                </Badge>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Môn học / Lớp:</span>
                <span className="font-semibold text-slate-800">{selectedAppeal.tenMonHoc} ({selectedAppeal.maLopHocPhan})</span>
              </div>
              <div className="flex justify-between font-mono">
                <span className="text-slate-500">Điểm gốc:</span>
                <span>
                  QT: {formatDiem(selectedAppeal.diemQuaTrinhGoc)} ·
                  CK: {formatDiem(selectedAppeal.diemCuoiKyGoc)} ·
                  TK: <span className="font-bold">{formatDiem(selectedAppeal.diemTongKetGoc)}</span>
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Loại điểm đề nghị:</span>
                <span className="font-medium text-slate-800">{LOAI_DIEM_LABEL[selectedAppeal.loaiDiem] || selectedAppeal.loaiDiem}</span>
              </div>
              {selectedAppeal.diemMongMuon != null && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Điểm mong muốn:</span>
                  <span className="font-medium text-slate-800">{formatDiem(selectedAppeal.diemMongMuon)}</span>
                </div>
              )}
            </div>

            <div className="rounded-xl border border-slate-200 p-3 text-xs">
              <span className="font-semibold text-slate-700">Lý do của bạn:</span>
              <p className="mt-1 whitespace-pre-wrap text-slate-600">{selectedAppeal.lyDo}</p>
            </div>

            {selectedAppeal.ketQuaXuLy && (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-3 text-xs">
                <span className="font-semibold text-emerald-900">Kết quả từ Phòng Đào tạo:</span>
                <p className="mt-1 whitespace-pre-wrap text-emerald-800">{selectedAppeal.ketQuaXuLy}</p>
                {selectedAppeal.trangThai === "CHAP_THUAN" && (
                  <p className="mt-2 font-mono font-bold text-emerald-900">
                    Điểm mới: QT={formatDiem(selectedAppeal.diemQuaTrinhMoi)} · CK={formatDiem(selectedAppeal.diemCuoiKyMoi)} · TK={formatDiem(selectedAppeal.diemTongKetMoi)}
                  </p>
                )}
              </div>
            )}

            <div>
              <h4 className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <History size={14} /> Lịch sử xử lý
              </h4>
              {selectedAppeal.lichSu?.length ? (
                <div className="space-y-2 border-l-2 border-slate-200 pl-3">
                  {selectedAppeal.lichSu.map((item) => (
                    <div key={item.id} className="text-xs">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-800">{item.hanhDong}</span>
                        <span className="text-[10px] text-slate-400">{formatDateTime(item.thoiDiem)}</span>
                      </div>
                      {item.lyDo && <p className="text-slate-600">{item.lyDo}</p>}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-400">Chưa có lịch sử.</p>
              )}
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
