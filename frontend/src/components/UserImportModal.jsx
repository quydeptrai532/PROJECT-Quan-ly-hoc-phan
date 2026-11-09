import { useRef, useState } from "react";
import { AlertCircle, CheckCircle2, Download, FileSpreadsheet, Info, Upload, X } from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { Alert, Badge, Modal, Spinner } from "./ui";

export default function UserImportModal({ open, onClose, type = "sinh-vien", onSuccess }) {
  const isStudent = type === "sinh-vien";
  const label = isStudent ? "sinh viên" : "giảng viên";

  const fileInputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [previewData, setPreviewData] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const resetState = () => {
    setFile(null);
    setPreviewData(null);
    setImportResult(null);
    setError("");
    setLoading(false);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleClose = () => {
    resetState();
    onClose();
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await api.get(`/admin/${type}/import/template`, {
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `mau-import-${type}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleFileChange = (e) => {
    const selected = e.target.files?.[0];
    if (selected) {
      setFile(selected);
      setPreviewData(null);
      setImportResult(null);
      setError("");
    }
  };

  const handlePreview = async () => {
    if (!file) {
      setError("Vui lòng chọn một file CSV để xem trước");
      return;
    }
    setLoading(true);
    setError("");
    setPreviewData(null);
    setImportResult(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const { data } = await api.post(`/admin/${type}/import/preview`, formData);
      setPreviewData(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async () => {
    if (!previewData?.previewToken) {
      setError("Không tìm thấy thông tin phiên xem trước hợp lệ");
      return;
    }
    setLoading(true);
    setError("");

    try {
      const { data } = await api.post(`/admin/${type}/import/confirm`, {
        previewToken: previewData.previewToken,
        fileSha256: previewData.fileSha256,
      });
      setImportResult(data);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadErrorReport = async () => {
    const errorRows = importResult?.danhSachKetQua?.filter((r) => !r.hopLe) || previewData?.danhSachDong?.filter((r) => !r.hopLe);
    if (!errorRows || errorRows.length === 0) return;

    try {
      const response = await api.post(`/admin/${type}/import/export-errors`, errorRows, {
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `loi-import-${type}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      // Fallback client-side CSV download
      const rows = [
        "\uFEFFDòng,Mã,Họ tên,Trường lỗi,Giá trị nhập,Nguyên nhân lỗi",
        ...errorRows.flatMap((r) =>
          (r.errors && r.errors.length > 0 ? r.errors : [{ cot: "", giaTriNhap: "", message: "Lỗi dòng" }]).map(
            (e) => `"${r.dong}","${r.ma || ""}","${r.hoTen || ""}","${e.cot || e.field || ""}","${e.giaTriNhap || ""}","${e.message || ""}"`
          )
        ),
      ];
      const blob = new Blob([rows.join("\r\n")], { type: "text/csv;charset=utf-8" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `loi-import-${type}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    }
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      size="xl"
      title={`Import danh sách ${label} từ CSV`}
      description={`Tải lên tệp CSV chứa danh sách ${label}, kiểm tra dữ liệu trước khi ghi nhận vào hệ thống.`}
      footer={
        <div className="flex w-full items-center justify-between">
          <button type="button" className="btn-secondary" onClick={handleDownloadTemplate}>
            <Download size={16} /> Tải file mẫu CSV
          </button>
          <div className="flex items-center gap-2">
            <button type="button" className="btn-secondary" onClick={handleClose}>
              Đóng
            </button>
            {!importResult && previewData && previewData.coTheXacNhan && (
              <button
                type="button"
                className="btn-primary"
                onClick={handleConfirm}
                disabled={loading}
              >
                {loading ? "Đang xử lý..." : `Xác nhận Import (${previewData.soDongHopLe} dòng hợp lệ)`}
              </button>
            )}
            {!importResult && !previewData && (
              <button
                type="button"
                className="btn-primary"
                onClick={handlePreview}
                disabled={!file || loading}
              >
                {loading ? "Đang đọc..." : "Xem trước (Preview)"}
              </button>
            )}
          </div>
        </div>
      }
    >
      <div className="space-y-4">
        {error && (
          <Alert tone="error" onClose={() => setError("")}>
            {error}
          </Alert>
        )}

        {/* STEP 1: Select File */}
        {!importResult && (
          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50/60 p-5 text-center">
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={handleFileChange}
            />
            <div className="flex flex-col items-center justify-center gap-2">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-white text-[#58734f] shadow-sm ring-1 ring-slate-200">
                <FileSpreadsheet size={22} />
              </span>
              <div>
                <p className="text-sm font-medium text-slate-800">
                  {file ? file.name : `Chọn tệp CSV danh sách ${label}`}
                </p>
                <p className="text-xs text-slate-500">
                  {file
                    ? `${(file.size / 1024).toFixed(1)} KB — Bấm "Xem trước" để phân tích dữ liệu`
                    : "Hỗ trợ định dạng .csv chuẩn UTF-8, dung lượng tối đa 5 MB"}
                </p>
              </div>
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  className="btn-secondary text-xs"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={loading}
                >
                  <Upload size={14} /> {file ? "Đổi tệp khác" : "Chọn file CSV"}
                </button>
                {file && !previewData && (
                  <button
                    type="button"
                    className="btn-primary text-xs"
                    onClick={handlePreview}
                    disabled={loading}
                  >
                    Xem trước (Preview)
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {loading && <Spinner label="Hệ thống đang phân tích và validate dữ liệu..." />}

        {/* STEP 2: Preview Details */}
        {!loading && previewData && !importResult && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-xl border border-slate-200 bg-white p-3 text-center shadow-xs">
                <p className="text-xs text-slate-500">Tổng số dòng</p>
                <p className="text-lg font-bold text-slate-800">{previewData.tongSoDong}</p>
              </div>
              <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-3 text-center shadow-xs">
                <p className="text-xs text-emerald-700">Dòng hợp lệ</p>
                <p className="text-lg font-bold text-emerald-800">{previewData.soDongHopLe}</p>
              </div>
              <div className="rounded-xl border border-rose-200 bg-rose-50/50 p-3 text-center shadow-xs">
                <p className="text-xs text-rose-700">Dòng có lỗi</p>
                <p className="text-lg font-bold text-rose-800">{previewData.soDongLoi}</p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-center shadow-xs">
                <p className="text-xs text-slate-500">Chính sách</p>
                <p className="text-xs font-semibold text-slate-700">
                  {previewData.transactionPolicy} / {previewData.idempotencyPolicy}
                </p>
              </div>
            </div>

            <div className="flex items-center justify-between text-xs text-slate-500">
              <p>Danh sách dữ liệu xem trước theo từng dòng:</p>
              {previewData.soDongLoi > 0 && (
                <button
                  type="button"
                  className="flex items-center gap-1 font-medium text-rose-600 hover:underline"
                  onClick={handleDownloadErrorReport}
                >
                  <Download size={13} /> Tải danh sách lỗi ({previewData.soDongLoi} dòng)
                </button>
              )}
            </div>

            <div className="max-h-72 overflow-y-auto rounded-xl border border-slate-200 bg-white shadow-inner">
              <table className="w-full text-left text-xs">
                <thead className="sticky top-0 border-b border-slate-200 bg-slate-50 font-medium text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Dòng</th>
                    <th className="px-3 py-2">Mã</th>
                    <th className="px-3 py-2">Họ tên</th>
                    <th className="px-3 py-2">Email</th>
                    {isStudent ? (
                      <>
                        <th className="px-3 py-2">Khoa</th>
                        <th className="px-3 py-2">Ngành / Lớp</th>
                      </>
                    ) : (
                      <>
                        <th className="px-3 py-2">SĐT</th>
                        <th className="px-3 py-2">Khoa</th>
                      </>
                    )}
                    <th className="px-3 py-2 text-center">Trạng thái</th>
                    <th className="px-3 py-2">Ghi chú / Lỗi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {previewData.danhSachDong?.map((row) => (
                    <tr
                      key={row.dong}
                      className={row.hopLe ? "hover:bg-slate-50" : "bg-rose-50/40 hover:bg-rose-50/70"}
                    >
                      <td className="px-3 py-2 font-mono text-slate-500">{row.dong}</td>
                      <td className="px-3 py-2 font-semibold text-slate-900">{row.ma}</td>
                      <td className="px-3 py-2 text-slate-800">{row.hoTen}</td>
                      <td className="px-3 py-2 text-slate-600">{row.email || "—"}</td>
                      {isStudent ? (
                        <>
                          <td className="px-3 py-2 text-slate-600">
                            {row.khoaTen ? `${row.khoaTen} (#${row.khoaId})` : row.khoaId ? `#${row.khoaId}` : "—"}
                          </td>
                          <td className="px-3 py-2 text-slate-600">
                            {row.nganhTen || row.lopTen ? `${row.nganhTen || "—"} / ${row.lopTen || "—"}` : "—"}
                          </td>
                        </>
                      ) : (
                        <>
                          <td className="px-3 py-2 text-slate-600">{row.soDienThoai || "—"}</td>
                          <td className="px-3 py-2 text-slate-600">
                            {row.khoaTen ? `${row.khoaTen} (#${row.khoaId})` : row.khoaId ? `#${row.khoaId}` : "—"}
                          </td>
                        </>
                      )}
                      <td className="px-3 py-2 text-center">
                        <Badge tone={row.hopLe ? "green" : row.trangThai === "TRUNG_LAP" ? "amber" : "red"}>
                          {row.hopLe ? "Hợp lệ" : row.trangThai === "TRUNG_LAP" ? "Trùng lặp" : "Lỗi"}
                        </Badge>
                      </td>
                      <td className="px-3 py-2 text-slate-600">
                        {row.hopLe ? (
                          <span className="text-emerald-700">Sẵn sàng tạo mới</span>
                        ) : (
                          <ul className="list-inside list-disc space-y-0.5 text-rose-700">
                            {row.errors?.map((err, i) => (
                              <li key={i}>
                                {err.cot && <span className="font-semibold">[{err.cot}] </span>}
                                {err.message}
                              </li>
                            ))}
                          </ul>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center gap-2 rounded-xl bg-amber-50 p-3 text-xs text-amber-900 ring-1 ring-amber-200">
              <Info size={16} className="shrink-0 text-amber-600" />
              <span>
                <strong>Chính sách giao dịch (Transaction Policy):</strong> Hệ thống áp dụng giao dịch theo từng dòng
                (Row-by-Row). Dòng hợp lệ sẽ được tạo tài khoản, dòng lỗi hoặc trùng lặp sẽ bị bỏ qua và ghi nhận chi tiết.
                Mật khẩu mặc định được bảo vệ và không lưu trong bản xem trước.
              </span>
            </div>
          </div>
        )}

        {/* STEP 3: Result Summary */}
        {!loading && importResult && (
          <div className="space-y-4">
            <Alert tone={importResult.soThatBai > 0 ? "warning" : "success"}>
              <p className="font-semibold">
                {`Quá trình import hoàn tất: Đã tạo ${importResult.soTaoMoi} ${label} thành công.`}
              </p>
              {importResult.soThatBai > 0 && (
                <p className="mt-1 text-xs">
                  {`Có ${importResult.soThatBai} dòng không thể nhập do lỗi dữ liệu hoặc tài khoản đã tồn tại.`}
                </p>
              )}
            </Alert>

            {importResult.soThatBai > 0 && (
              <div className="flex justify-end">
                <button
                  type="button"
                  className="btn-secondary text-xs"
                  onClick={handleDownloadErrorReport}
                >
                  <Download size={14} /> Tải file báo lỗi (.csv)
                </button>
              </div>
            )}

            <div className="max-h-64 overflow-y-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-left text-xs">
                <thead className="sticky top-0 border-b border-slate-200 bg-slate-50 font-medium text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Dòng</th>
                    <th className="px-3 py-2">Mã</th>
                    <th className="px-3 py-2">Họ tên</th>
                    <th className="px-3 py-2">Trạng thái</th>
                    <th className="px-3 py-2">Chi tiết</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {importResult.danhSachKetQua?.map((row) => (
                    <tr
                      key={row.dong}
                      className={row.hopLe ? "hover:bg-slate-50" : "bg-rose-50/40 hover:bg-rose-50/70"}
                    >
                      <td className="px-3 py-2 font-mono text-slate-500">{row.dong}</td>
                      <td className="px-3 py-2 font-semibold text-slate-900">{row.ma}</td>
                      <td className="px-3 py-2 text-slate-800">{row.hoTen}</td>
                      <td className="px-3 py-2">
                        <Badge tone={row.hopLe ? "green" : "red"}>
                          {row.hopLe ? "Thành công" : "Thất bại"}
                        </Badge>
                      </td>
                      <td className="px-3 py-2 text-slate-600">
                        {row.hopLe ? (
                          <span className="text-emerald-700">Tạo tài khoản thành công</span>
                        ) : (
                          <ul className="list-inside list-disc text-rose-700">
                            {row.errors?.map((err, i) => (
                              <li key={i}>{err.message}</li>
                            ))}
                          </ul>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                className="btn-secondary text-xs"
                onClick={resetState}
              >
                Import tệp khác
              </button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
