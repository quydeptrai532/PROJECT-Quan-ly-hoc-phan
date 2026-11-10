import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Eye,
  FileCheck,
  History,
  RotateCcw,
  Search,
  XCircle,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, Field, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const APPEAL_STATUS_META = {
  MOI_TAO: { label: "Mới tiếp nhận", tone: "amber" },
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

export default function AdminGradeAppealsPage() {
  const { data: hocKyList } = useApi("/admin/hoc-ky");
  const { data: config } = useApi("/admin/phuc-khao/cau-hinh");

  const [hocKyId, setHocKyId] = useState("");
  const [trangThaiFilter, setTrangThaiFilter] = useState("");
  const [keyword, setKeyword] = useState("");

  const [appeals, setAppeals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Detail Modal
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedAppeal, setSelectedAppeal] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // In-Progress Modal
  const [tiepNhanOpen, setTiepNhanOpen] = useState(false);
  const [tiepNhanNote, setTiepNhanNote] = useState("");
  const [tiepNhanSubmitting, setTiepNhanSubmitting] = useState(false);

  // Resolution Modal
  const [resolveOpen, setResolveOpen] = useState(false);
  const [resolveDecision, setResolveDecision] = useState("CHAP_THUAN"); // CHAP_THUAN hoặc TU_CHOI
  const [newDiemQt, setNewDiemQt] = useState("");
  const [newDiemCk, setNewDiemCk] = useState("");
  const [resolveNote, setResolveNote] = useState("");
  const [resolveSubmitting, setResolveSubmitting] = useState(false);
  const [resolveError, setResolveError] = useState("");

  const loadAppeals = async () => {
    setLoading(true);
    setError("");
    try {
      const params = {};
      if (hocKyId) params.hocKyId = hocKyId;
      if (trangThaiFilter) params.trangThai = trangThaiFilter;
      if (keyword.trim()) params.keyword = keyword.trim();

      const { data } = await api.get("/admin/phuc-khao", { params });
      setAppeals(data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAppeals();
  }, [hocKyId, trangThaiFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    loadAppeals();
  };

  const openDetail = async (appeal) => {
    setSelectedAppeal(appeal);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const { data } = await api.get(`/admin/phuc-khao/${appeal.id}`);
      setSelectedAppeal(data);
    } catch (err) {
      console.error("Lỗi tải chi tiết phúc khảo:", err);
    } finally {
      setDetailLoading(false);
    }
  };

  const openTiepNhan = (appeal) => {
    setSelectedAppeal(appeal);
    setTiepNhanNote("Phòng Đào tạo chuyển tổ chuyên môn chấm thẩm định");
    setTiepNhanOpen(true);
  };

  const submitTiepNhan = async (e) => {
    e.preventDefault();
    setTiepNhanSubmitting(true);
    try {
      await api.post(`/admin/phuc-khao/${selectedAppeal.id}/tiepNhan`, {
        ghiChu: tiepNhanNote,
      });
      setMessage(`Đã tiếp nhận hồ sơ ${selectedAppeal.maHoSo} chuyển sang đang xử lý`);
      setTiepNhanOpen(false);
      loadAppeals();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setTiepNhanSubmitting(false);
    }
  };

  const openResolve = (appeal) => {
    setSelectedAppeal(appeal);
    setResolveDecision("CHAP_THUAN");
    setNewDiemQt(appeal.diemQuaTrinhGoc != null ? String(appeal.diemQuaTrinhGoc) : "");
    setNewDiemCk(appeal.diemCuoiKyGoc != null ? String(appeal.diemCuoiKyGoc) : "");
    setResolveNote("");
    setResolveError("");
    setResolveOpen(true);
  };

  const calculatedNewTk = useMemo(() => {
    const qt = parseFloat(newDiemQt);
    const ck = parseFloat(newDiemCk);
    if (isNaN(qt) || isNaN(ck)) return null;
    return Math.round((0.4 * qt + 0.6 * ck) * 10) / 10;
  }, [newDiemQt, newDiemCk]);

  const submitResolve = async (e) => {
    e.preventDefault();
    setResolveSubmitting(true);
    setResolveError("");
    try {
      const payload = {
        trangThai: resolveDecision,
        ketQuaXuLy: resolveNote.trim(),
      };
      if (resolveDecision === "CHAP_THUAN") {
        if (newDiemQt !== "") payload.diemQuaTrinhMoi = parseFloat(newDiemQt);
        if (newDiemCk !== "") payload.diemCuoiKyMoi = parseFloat(newDiemCk);
      }

      await api.post(`/admin/phuc-khao/${selectedAppeal.id}/xu-ly`, payload);
      setMessage(`Đã xử lý hồ sơ ${selectedAppeal.maHoSo} thành công (${resolveDecision === "CHAP_THUAN" ? "Chấp thuận điều chỉnh" : "Từ chối / Giữ nguyên"})`);
      setResolveOpen(false);
      loadAppeals();
    } catch (err) {
      setResolveError(getErrorMessage(err));
    } finally {
      setResolveSubmitting(false);
    }
  };

  const statusCounts = useMemo(() => {
    const counts = { ALL: appeals.length, MOI_TAO: 0, DANG_XU_LY: 0, CHAP_THUAN: 0, TU_CHOI: 0 };
    appeals.forEach((a) => {
      if (counts[a.trangThai] !== undefined) counts[a.trangThai]++;
    });
    return counts;
  }, [appeals]);

  return (
    <div>
      <PageHeader
        icon={FileCheck}
        title="Quản lý hồ sơ phúc khảo điểm"
        description="Tiếp nhận, xử lý thẩm tra và điều chỉnh điểm học phần theo đơn phúc khảo của sinh viên."
      />

      <div className="mb-4 space-y-3">
        {config && (
          <Alert tone="info">
            <span className="font-semibold">Cấu hình tập trung hiện tại:</span> Thời hạn tiếp nhận hồ sơ là{" "}
            <span className="font-bold">{config.submissionWindowDays} ngày</span> kể từ ngày công bố bảng điểm.{" "}
            {config.officialRegulation ? (
              <span className="text-green-700">(Đã có quy chế chính thức)</span>
            ) : (
              <span className="text-amber-700">(Cấu hình dự kiến — chờ Phòng Đào tạo phê duyệt quy chế chính thức)</span>
            )}
            {config.slaNote && <p className="mt-1 text-xs text-slate-600">{config.slaNote}</p>}
          </Alert>
        )}

        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
      </div>

      <div className="card overflow-hidden">
        {/* Filters */}
        <div className="border-b border-slate-200 p-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                !trangThaiFilter ? "bg-[#173f3a] text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              onClick={() => setTrangThaiFilter("")}
            >
              Tất cả ({statusCounts.ALL})
            </button>
            <button
              type="button"
              className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                trangThaiFilter === "MOI_TAO" ? "bg-amber-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              onClick={() => setTrangThaiFilter("MOI_TAO")}
            >
              Mới tiếp nhận {statusCounts.MOI_TAO > 0 && <span className="rounded-full bg-amber-100 px-1.5 text-[10px] text-amber-900">{statusCounts.MOI_TAO}</span>}
            </button>
            <button
              type="button"
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                trangThaiFilter === "DANG_XU_LY" ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              onClick={() => setTrangThaiFilter("DANG_XU_LY")}
            >
              Đang xử lý ({statusCounts.DANG_XU_LY})
            </button>
            <button
              type="button"
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                trangThaiFilter === "CHAP_THUAN" ? "bg-green-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              onClick={() => setTrangThaiFilter("CHAP_THUAN")}
            >
              Chấp thuận ({statusCounts.CHAP_THUAN})
            </button>
            <button
              type="button"
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                trangThaiFilter === "TU_CHOI" ? "bg-rose-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              onClick={() => setTrangThaiFilter("TU_CHOI")}
            >
              Từ chối ({statusCounts.TU_CHOI})
            </button>
          </div>

          <form onSubmit={handleSearch} className="flex flex-wrap items-center gap-3">
            <select
              className="input w-auto min-w-[180px]"
              value={hocKyId}
              onChange={(e) => setHocKyId(e.target.value)}
            >
              <option value="">Tất cả học kỳ</option>
              {(hocKyList || []).map((h) => (
                <option key={h.id} value={h.id}>
                  {h.ten} - {h.namHoc}
                </option>
              ))}
            </select>

            <div className="relative flex-1 min-w-[240px]">
              <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                className="input pl-9"
                placeholder="Tìm mã hồ sơ, MSSV, họ tên, mã lớp..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
            <button type="submit" className="btn-secondary">
              Tìm kiếm
            </button>
          </form>
        </div>

        {loading ? (
          <Spinner label="Đang tải danh sách hồ sơ phúc khảo..." />
        ) : appeals.length === 0 ? (
          <EmptyState
            icon={FileCheck}
            title="Không tìm thấy hồ sơ phúc khảo nào"
            description="Chưa có sinh viên nào gửi yêu cầu phúc khảo hoặc không có kết quả phù hợp với bộ lọc."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="table">
              <thead className="thead">
                <tr>
                  <th className="th">Mã hồ sơ</th>
                  <th className="th">Sinh viên</th>
                  <th className="th">Lớp / Môn học</th>
                  <th className="th text-center">Điểm gốc (QT/CK/TK)</th>
                  <th className="th">Yêu cầu & Mong muốn</th>
                  <th className="th text-center">Trạng thái</th>
                  <th className="th">Hạn nộp</th>
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
                        <p className="font-medium text-slate-800">{item.hoTenSinhVien}</p>
                        <p className="text-xs text-slate-500">{item.maSinhVien}</p>
                      </td>
                      <td className="td">
                        <p className="font-medium text-slate-800">{item.tenMonHoc}</p>
                        <p className="text-xs text-slate-500">
                          {item.maLopHocPhan} · {item.maMonHoc} ({item.soTinChi} TC)
                        </p>
                      </td>
                      <td className="td text-center font-mono">
                        <span className="text-slate-600">{formatDiem(item.diemQuaTrinhGoc)}</span> /{" "}
                        <span className="text-slate-600">{formatDiem(item.diemCuoiKyGoc)}</span> /{" "}
                        <span className="font-bold text-slate-900">{formatDiem(item.diemTongKetGoc)}</span>
                      </td>
                      <td className="td text-xs">
                        <p className="font-semibold text-slate-700">{LOAI_DIEM_LABEL[item.loaiDiem] || item.loaiDiem}</p>
                        {item.diemMongMuon != null && (
                          <p className="text-slate-500">Mong muốn: {formatDiem(item.diemMongMuon)}</p>
                        )}
                      </td>
                      <td className="td text-center">
                        <Badge tone={meta.tone}>{meta.label}</Badge>
                      </td>
                      <td className="td text-xs">
                        {item.hanNop ? (
                          <span className={item.quaHanNop ? "text-rose-600 font-semibold" : "text-slate-600"}>
                            {formatDateTime(item.hanNop)} {item.quaHanNop && "(Đã hết hạn)"}
                          </span>
                        ) : (
                          <span className="text-slate-400">Không giới hạn</span>
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
                              className="rounded-md p-1.5 text-blue-600 hover:bg-blue-50"
                              onClick={() => openTiepNhan(item)}
                              title="Tiếp nhận chuyển xử lý"
                            >
                              <Clock size={15} />
                            </button>
                          )}
                          {(item.trangThai === "MOI_TAO" || item.trangThai === "DANG_XU_LY") && (
                            <button
                              type="button"
                              className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50"
                              onClick={() => openResolve(item)}
                              title="Xử lý kết quả (Chấp thuận / Từ chối)"
                            >
                              <CheckCircle2 size={15} />
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

      {/* Modal Xem chi tiết & Lịch sử */}
      <Modal
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={selectedAppeal ? `Hồ sơ phúc khảo: ${selectedAppeal.maHoSo}` : "Chi tiết hồ sơ"}
        maxWidth="max-w-2xl"
      >
        {detailLoading ? (
          <Spinner label="Đang tải chi tiết hồ sơ..." />
        ) : selectedAppeal ? (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs">
              <div>
                <span className="text-slate-500">Sinh viên:</span>
                <p className="font-semibold text-slate-800">{selectedAppeal.hoTenSinhVien} ({selectedAppeal.maSinhVien})</p>
              </div>
              <div>
                <span className="text-slate-500">Trạng thái:</span>
                <div>
                  <Badge tone={(APPEAL_STATUS_META[selectedAppeal.trangThai] || APPEAL_STATUS_META.MOI_TAO).tone}>
                    {(APPEAL_STATUS_META[selectedAppeal.trangThai] || APPEAL_STATUS_META.MOI_TAO).label}
                  </Badge>
                </div>
              </div>
              <div>
                <span className="text-slate-500">Lớp học phần / Môn học:</span>
                <p className="font-semibold text-slate-800">{selectedAppeal.maLopHocPhan} - {selectedAppeal.tenMonHoc}</p>
              </div>
              <div>
                <span className="text-slate-500">Học kỳ:</span>
                <p className="font-semibold text-slate-800">{selectedAppeal.tenHocKy} ({selectedAppeal.namHoc})</p>
              </div>
              <div>
                <span className="text-slate-500">Điểm gốc:</span>
                <p className="font-mono font-semibold text-slate-800">
                  QT: {formatDiem(selectedAppeal.diemQuaTrinhGoc)} · CK: {formatDiem(selectedAppeal.diemCuoiKyGoc)} · TK: {formatDiem(selectedAppeal.diemTongKetGoc)}
                </p>
              </div>
              <div>
                <span className="text-slate-500">Loại điểm đề nghị:</span>
                <p className="font-semibold text-slate-800">
                  {LOAI_DIEM_LABEL[selectedAppeal.loaiDiem] || selectedAppeal.loaiDiem}
                  {selectedAppeal.diemMongMuon != null && ` (Mong muốn: ${formatDiem(selectedAppeal.diemMongMuon)})`}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-200 p-3.5 text-xs">
              <span className="font-semibold text-slate-700">Lý do phúc khảo của sinh viên:</span>
              <p className="mt-1 whitespace-pre-wrap text-slate-600">{selectedAppeal.lyDo}</p>
            </div>

            {selectedAppeal.ketQuaXuLy && (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-3.5 text-xs">
                <span className="font-semibold text-emerald-900">Kết quả xử lý của Phòng Đào tạo:</span>
                <p className="mt-1 whitespace-pre-wrap text-emerald-800">{selectedAppeal.ketQuaXuLy}</p>
                {selectedAppeal.trangThai === "CHAP_THUAN" && (
                  <p className="mt-2 font-mono font-bold text-emerald-900">
                    Điểm mới sau phúc khảo: QT={formatDiem(selectedAppeal.diemQuaTrinhMoi)} · CK={formatDiem(selectedAppeal.diemCuoiKyMoi)} · TK={formatDiem(selectedAppeal.diemTongKetMoi)}
                  </p>
                )}
                {selectedAppeal.nguoiXuLyUsername && (
                  <p className="mt-1 text-[11px] text-slate-500">
                    Người xử lý: {selectedAppeal.nguoiXuLyUsername} lúc {formatDateTime(selectedAppeal.thoiDiemXuLy)}
                  </p>
                )}
              </div>
            )}

            {/* Timeline Lịch sử */}
            <div>
              <h4 className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <History size={14} /> Lịch sử xử lý ({selectedAppeal.lichSu?.length || 0})
              </h4>
              {selectedAppeal.lichSu?.length ? (
                <div className="space-y-2 border-l-2 border-slate-200 pl-3">
                  {selectedAppeal.lichSu.map((item) => (
                    <div key={item.id} className="text-xs">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-800">{item.hanhDong}</span>
                        <span className="text-[10px] text-slate-400">{formatDateTime(item.thoiDiem)}</span>
                        {item.nguoiThucHienUsername && (
                          <span className="text-[10px] text-slate-500">bởi {item.nguoiThucHienUsername}</span>
                        )}
                      </div>
                      {item.lyDo && <p className="text-slate-600">{item.lyDo}</p>}
                      {item.diemTongKetMoi != null && (
                        <p className="font-mono text-emerald-700">
                          Điểm điều chỉnh: QT={formatDiem(item.diemQuaTrinhMoi)} · CK={formatDiem(item.diemCuoiKyMoi)} · TK={formatDiem(item.diemTongKetMoi)}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-400">Chưa có lịch sử chuyển trạng thái.</p>
              )}
            </div>
          </div>
        ) : null}
      </Modal>

      {/* Modal Tiếp nhận hồ sơ */}
      <Modal
        open={tiepNhanOpen}
        onClose={() => setTiepNhanOpen(false)}
        title="Tiếp nhận hồ sơ phúc khảo"
        maxWidth="max-w-md"
      >
        <form onSubmit={submitTiepNhan} className="space-y-3">
          <p className="text-xs text-slate-600">
            Chuyển hồ sơ <span className="font-bold text-slate-800">{selectedAppeal?.maHoSo}</span> sang trạng thái <span className="font-semibold text-blue-700">Đang xử lý / Chấm lại</span>.
          </p>
          <Field label="Ghi chú tiếp nhận">
            <textarea
              className="input h-20"
              value={tiepNhanNote}
              onChange={(e) => setTiepNhanNote(e.target.value)}
              placeholder="Nhập ghi chú chuyển tổ chấm lại..."
            />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-secondary" onClick={() => setTiepNhanOpen(false)}>
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={tiepNhanSubmitting}>
              {tiepNhanSubmitting ? "Đang lưu..." : "Xác nhận tiếp nhận"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Xử lý kết quả */}
      <Modal
        open={resolveOpen}
        onClose={() => setResolveOpen(false)}
        title={`Xử lý kết quả phúc khảo: ${selectedAppeal?.maHoSo}`}
        maxWidth="max-w-lg"
      >
        <form onSubmit={submitResolve} className="space-y-4">
          {resolveError && <Alert tone="error">{resolveError}</Alert>}

          <Field label="Quyết định xử lý">
            <div className="flex gap-4">
              <label className="flex items-center gap-2 text-xs font-semibold text-emerald-800 cursor-pointer">
                <input
                  type="radio"
                  name="decision"
                  value="CHAP_THUAN"
                  checked={resolveDecision === "CHAP_THUAN"}
                  onChange={() => setResolveDecision("CHAP_THUAN")}
                />
                Chấp thuận điều chỉnh điểm
              </label>
              <label className="flex items-center gap-2 text-xs font-semibold text-rose-800 cursor-pointer">
                <input
                  type="radio"
                  name="decision"
                  value="TU_CHOI"
                  checked={resolveDecision === "TU_CHOI"}
                  onChange={() => setResolveDecision("TU_CHOI")}
                />
                Từ chối / Giữ nguyên điểm gốc
              </label>
            </div>
          </Field>

          {resolveDecision === "CHAP_THUAN" && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/40 p-3 space-y-3">
              <p className="text-xs text-emerald-900 font-semibold">
                Nhập điểm số mới (hệ thống tự động tính điểm tổng kết và cập nhật qua luồng điều chỉnh có audit):
              </p>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Điểm quá trình mới">
                  <input
                    type="number"
                    step="0.25"
                    min="0"
                    max="10"
                    className="input"
                    value={newDiemQt}
                    onChange={(e) => setNewDiemQt(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Điểm cuối kỳ mới">
                  <input
                    type="number"
                    step="0.25"
                    min="0"
                    max="10"
                    className="input"
                    value={newDiemCk}
                    onChange={(e) => setNewDiemCk(e.target.value)}
                    required
                  />
                </Field>
              </div>
              {calculatedNewTk !== null && (
                <div className="text-xs font-semibold text-emerald-800">
                  Điểm tổng kết mới dự kiến: <span className="font-mono text-sm font-bold text-emerald-950">{calculatedNewTk}</span>
                </div>
              )}
            </div>
          )}

          <Field label="Nội dung kết quả / Biên bản chấm lại (bắt buộc)">
            <textarea
              className="input h-24"
              value={resolveNote}
              onChange={(e) => setResolveNote(e.target.value)}
              placeholder="Ghi rõ lý do điều chỉnh hoặc lý do giữ nguyên điểm theo biên bản hội đồng chấm thẩm định..."
              required
            />
          </Field>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className="btn-secondary" onClick={() => setResolveOpen(false)}>
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={resolveSubmitting}>
              {resolveSubmitting ? "Đang cập nhật..." : "Lưu kết quả xử lý"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
