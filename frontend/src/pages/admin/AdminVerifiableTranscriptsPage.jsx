import { useState, useEffect } from "react";
import {
  ShieldCheck,
  Search,
  Filter,
  FileDown,
  AlertTriangle,
  RotateCcw,
  Ban,
  CheckCircle2,
  XCircle,
  Eye,
  Info,
  Calendar,
  User,
  Hash,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, PageHeader, EmptyState, Spinner } from "../../components/ui";

const TRANG_THAI_OPTIONS = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "HIEU_LUC", label: "Hiệu lực" },
  { value: "DA_THU_HOI", label: "Đã thu hồi" },
  { value: "DA_THAY_THE", label: "Đã thay thế" },
];

function TrangThaiBadge({ trangThai }) {
  switch (trangThai) {
    case "HIEU_LUC":
      return <Badge tone="green">Hiệu lực</Badge>;
    case "DA_THU_HOI":
      return <Badge tone="red">Đã thu hồi</Badge>;
    case "DA_THAY_THE":
      return <Badge tone="amber">Đã thay thế</Badge>;
    default:
      return <Badge tone="slate">{trangThai}</Badge>;
  }
}

export default function AdminVerifiableTranscriptsPage() {
  const [transcripts, setTranscripts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [maSinhVien, setMaSinhVien] = useState("");
  const [trangThai, setTrangThai] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [detailModal, setDetailModal] = useState(null);
  const [actionModal, setActionModal] = useState(null); // { type: 'thu_hoi' | 'cap_lai', item }
  const [actionReason, setActionReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const fetchTranscripts = async () => {
    setLoading(true);
    setError("");
    try {
      const params = {
        page,
        size: 15,
      };
      if (maSinhVien.trim()) params.maSinhVien = maSinhVien.trim();
      if (trangThai) params.trangThai = trangThai;

      const res = await api.get("/admin/bang-diem-xac-thuc", { params });
      setTranscripts(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTranscripts();
  }, [page, trangThai]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchTranscripts();
  };

  const handleDownloadPdf = async (item) => {
    try {
      const res = await api.get(`/admin/bang-diem-xac-thuc/${item.id}/pdf`, {
        responseType: "blob",
      });
      const blob = new Blob([res.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `bang-diem-${item.maSinhVien}-v${item.phienBan}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError("Không thể tải tệp PDF: " + getErrorMessage(err));
    }
  };

  const handleViewDetail = async (item) => {
    try {
      const res = await api.get(`/admin/bang-diem-xac-thuc/${item.id}`);
      setDetailModal(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleExecuteAction = async () => {
    if (!actionReason.trim()) {
      setError("Vui lòng nhập lý do thực hiện thao tác");
      return;
    }
    setActionLoading(true);
    setError("");
    setSuccess("");
    try {
      if (actionModal.type === "thu_hoi") {
        await api.post(`/admin/bang-diem-xac-thuc/${actionModal.item.id}/thu-hoi`, {
          lyDo: actionReason.trim(),
        });
        setSuccess(`Đã thu hồi bảng điểm điện tử v${actionModal.item.phienBan} của SV ${actionModal.item.maSinhVien}`);
      } else if (actionModal.type === "cap_lai") {
        await api.post(`/admin/bang-diem-xac-thuc/${actionModal.item.id}/cap-lai`, {
          lyDo: actionReason.trim(),
        });
        setSuccess(`Đã cấp lại bảng điểm mới cho SV ${actionModal.item.maSinhVien}`);
      }
      setActionModal(null);
      setActionReason("");
      fetchTranscripts();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quản lý Bảng điểm Điện tử Xác thực"
        subtitle="Quản lý việc phát hành, thu hồi, cấp lại và kiểm tra tính toàn vẹn của bảng điểm có mã xác minh điện tử"
      />

      {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
      {success && <Alert tone="success" onClose={() => setSuccess("")}>{success}</Alert>}

      {/* Filter Card */}
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <input
              type="text"
              value={maSinhVien}
              onChange={(e) => setMaSinhVien(e.target.value)}
              placeholder="Tìm theo mã sinh viên..."
              className="w-full rounded-xl border border-slate-300 px-3 py-2 pl-9 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-200"
            />
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          </div>

          <select
            value={trangThai}
            onChange={(e) => {
              setTrangThai(e.target.value);
              setPage(0);
            }}
            className="rounded-xl border border-slate-300 px-3 py-2 text-sm text-slate-700 focus:border-indigo-500 focus:outline-none"
          >
            {TRANG_THAI_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          <button
            type="submit"
            className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 transition"
          >
            <Filter size={15} />
            Lọc kết quả
          </button>
        </form>
      </section>

      {/* Table */}
      <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400">
            <Spinner size={32} className="mx-auto mb-2 text-indigo-600" />
            <p className="text-sm">Đang tải danh sách bảng điểm...</p>
          </div>
        ) : transcripts.length === 0 ? (
          <div className="p-8">
            <EmptyState
              title="Chưa có bảng điểm điện tử nào"
              description="Chưa có bảng điểm điện tử xác thực nào được cấp phát phù hợp với bộ lọc hiện tại."
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3.5">Mã SV / Họ tên</th>
                  <th className="px-4 py-3.5">Phiên bản</th>
                  <th className="px-4 py-3.5">Phạm vi</th>
                  <th className="px-4 py-3.5 text-center">Số môn / Tín chỉ</th>
                  <th className="px-4 py-3.5 text-center">GPA (10/4)</th>
                  <th className="px-4 py-3.5">Thời điểm cấp</th>
                  <th className="px-4 py-3.5 text-center">Trạng thái</th>
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {transcripts.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-800">{item.hoTen}</p>
                      <p className="text-xs text-slate-500 font-mono">{item.maSinhVien}</p>
                    </td>
                    <td className="px-4 py-3">
                      <span className="font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded text-xs">
                        v{item.phienBan}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">
                      {item.hocKyId ? `${item.tenHocKy} (${item.namHoc})` : "Toàn khóa"}
                    </td>
                    <td className="px-4 py-3 text-center text-xs">
                      <span className="font-semibold text-slate-800">{item.soMonDaCongBo} môn</span>
                      <span className="text-slate-400 block">{item.tongTinChiTichLuy} TC</span>
                    </td>
                    <td className="px-4 py-3 text-center text-xs">
                      <span className="font-bold text-slate-800">
                        {item.gpa10 !== null ? item.gpa10.toFixed(2) : "—"}
                      </span>
                      <span className="text-slate-500 block">
                        ({item.gpa4 !== null ? item.gpa4.toFixed(2) : "—"})
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500">
                      {item.thoiDiemPhatHanhFormatted || item.thoiDiemPhatHanh}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <TrangThaiBadge trangThai={item.trangThai} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="inline-flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() => handleViewDetail(item)}
                          title="Xem chi tiết snapshot"
                          className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition"
                        >
                          <Eye size={16} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDownloadPdf(item)}
                          title="Tải tệp PDF xác thực"
                          className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition"
                        >
                          <FileDown size={16} />
                        </button>

                        {item.trangThai === "HIEU_LUC" && (
                          <>
                            <button
                              type="button"
                              onClick={() => {
                                setActionModal({ type: "cap_lai", item });
                                setActionReason("");
                              }}
                              title="Cấp lại phiên bản mới"
                              className="p-1.5 text-amber-600 hover:bg-amber-50 rounded-lg transition"
                            >
                              <RotateCcw size={16} />
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setActionModal({ type: "thu_hoi", item });
                                setActionReason("");
                              }}
                              title="Thu hồi bảng điểm"
                              className="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg transition"
                            >
                              <Ban size={16} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="border-t border-slate-200 px-4 py-3 flex items-center justify-between">
            <button
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="text-xs font-medium text-slate-600 disabled:opacity-40 hover:text-indigo-600 px-2 py-1 rounded"
            >
              &larr; Trang trước
            </button>
            <span className="text-xs text-slate-500">
              Trang {page + 1} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="text-xs font-medium text-slate-600 disabled:opacity-40 hover:text-indigo-600 px-2 py-1 rounded"
            >
              Trang sau &rarr;
            </button>
          </div>
        )}
      </section>

      {/* Detail Modal */}
      {detailModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-xl border border-slate-200 space-y-5">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <ShieldCheck className="text-indigo-600" size={20} />
                Chi tiết Bảng điểm Xác thực v{detailModal.phienBan} - {detailModal.maSinhVien}
              </h3>
              <button
                onClick={() => setDetailModal(null)}
                className="text-slate-400 hover:text-slate-600 rounded-lg p-1 text-sm"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                <div>
                  <span className="text-slate-500 block">Mã xác minh (Đã che):</span>
                  <span className="font-mono font-semibold text-slate-800">{detailModal.maXacMinhMasked}</span>
                </div>
                <div>
                  <span className="text-slate-500 block">Trạng thái:</span>
                  <TrangThaiBadge trangThai={detailModal.trangThai} />
                </div>
                <div>
                  <span className="text-slate-500 block">Thời điểm phát hành:</span>
                  <span className="text-slate-800 font-medium">{detailModal.thoiDiemPhatHanhFormatted || detailModal.thoiDiemPhatHanh}</span>
                </div>
                <div>
                  <span className="text-slate-500 block">Cán bộ phát hành:</span>
                  <span className="text-slate-800 font-medium">{detailModal.nguoiPhatHanh || "Tự động/Sinh viên"}</span>
                </div>
              </div>

              {detailModal.trangThai === "DA_THU_HOI" && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900">
                  <p className="font-semibold">Thông tin thu hồi:</p>
                  <p className="mt-1">Thời gian: {detailModal.thoiDiemThuHoi}</p>
                  <p>Cán bộ thực hiện: {detailModal.nguoiThuHoi}</p>
                  <p className="italic">Lý do: {detailModal.lyDoThuHoi}</p>
                </div>
              )}

              {detailModal.banGhiGocId && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900">
                  <p className="font-semibold">Liên kết phiên bản gốc:</p>
                  <p className="mt-1">Cấp lại thay thế cho bản ghi ID: #{detailModal.banGhiGocId}</p>
                  {detailModal.lyDoCapLai && <p className="italic">Lý do cấp lại: {detailModal.lyDoCapLai}</p>}
                </div>
              )}

              {detailModal.banGhiThayTheId && (
                <div className="p-3 bg-slate-100 border border-slate-200 rounded-xl text-slate-800">
                  <p className="font-semibold">Phiên bản thay thế kế tiếp:</p>
                  <p className="mt-1">Đã được thay thế bởi bản ghi ID: #{detailModal.banGhiThayTheId}</p>
                </div>
              )}

              <div>
                <span className="text-slate-500 block mb-1">Mã băm SHA-256 Checksum của snapshot bất biến:</span>
                <span className="font-mono text-[11px] bg-slate-100 p-2 rounded block break-all text-slate-700">
                  {detailModal.checksumSha256}
                </span>
              </div>
            </div>

            <div className="flex justify-end pt-3 border-t">
              <button
                type="button"
                onClick={() => setDetailModal(null)}
                className="px-4 py-2 text-xs font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Action Dialog (Thu hồi / Cấp lại) */}
      {actionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              {actionModal.type === "thu_hoi" ? (
                <>
                  <Ban className="text-rose-600" size={18} />
                  Thu hồi Bảng điểm Điện tử
                </>
              ) : (
                <>
                  <RotateCcw className="text-amber-600" size={18} />
                  Cấp lại Bảng điểm Điện tử
                </>
              )}
            </h3>

            <p className="text-xs text-slate-600 leading-relaxed">
              {actionModal.type === "thu_hoi"
                ? `Bạn đang thực hiện thu hồi bảng điểm v${actionModal.item.phienBan} của sinh viên ${actionModal.item.maSinhVien}. Sau khi thu hồi, mã xác minh sẽ bị vô hiệu hóa.`
                : `Hệ thống sẽ vô hiệu hóa phiên bản hiện tại (v${actionModal.item.phienBan}) và phát hành phiên bản mới (v${actionModal.item.phienBan + 1}) với mã xác minh ngẫu nhiên mới.`}
            </p>

            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">
                Lý do thực hiện <span className="text-rose-500">*</span>:
              </label>
              <textarea
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                placeholder="Nhập lý do chi tiết..."
                rows={3}
                className="w-full rounded-xl border border-slate-300 p-2.5 text-xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-200"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                disabled={actionLoading}
                onClick={() => setActionModal(null)}
                className="px-4 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={actionLoading || !actionReason.trim()}
                onClick={handleExecuteAction}
                className={`px-4 py-2 text-xs font-medium text-white rounded-xl shadow-sm transition ${
                  actionModal.type === "thu_hoi"
                    ? "bg-rose-600 hover:bg-rose-700"
                    : "bg-amber-600 hover:bg-amber-700"
                } disabled:opacity-50`}
              >
                {actionLoading ? "Đang xử lý..." : "Xác nhận"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}