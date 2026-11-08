import { useRef, useState } from "react";
import {
    AlertTriangle,
    CheckCircle2,
    Download,
    FileSpreadsheet,
    RefreshCw,
    ShieldCheck,
    Upload,
} from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { formatDiem } from "../lib/format";
import { Alert, Badge, Modal } from "./ui";

const ACTION_BADGE = {
    TAO_MOI: { label: "Tạo mới", tone: "green" },
    CAP_NHAT: { label: "Cập nhật", tone: "indigo" },
    LOI: { label: "Lỗi dữ liệu", tone: "red" },
};

const FIELD_LABEL = {
    maSinhVien: "Mã sinh viên",
    hoTen: "Họ và tên",
    diemQuaTrinh: "Điểm quá trình",
    diemCuoiKy: "Điểm cuối kỳ",
};

async function docThongBaoLoiBlob(err) {
    const data = err?.response?.data;
    if (data instanceof Blob) {
        try {
            const parsed = JSON.parse(await data.text());
            if (parsed?.message) return parsed.message;
        } catch {
            // fallback
        }
    }
    return getErrorMessage(err);
}

export default function GradeExcelImportModal({
    open,
    onClose,
    lop,
    disabled,
    onImportSuccess,
}) {
    const fileInputRef = useRef(null);
    const [selectedFile, setSelectedFile] = useState(null);
    const [downloadingTemplate, setDownloadingTemplate] = useState(false);
    const [previewLoading, setPreviewLoading] = useState(false);
    const [confirmLoading, setConfirmLoading] = useState(false);
    const [error, setError] = useState("");
    const [preview, setPreview] = useState(null);
    const [importResult, setImportResult] = useState(null);

    const lopId = lop?.id;
    const maLop = lop?.ma || lopId;

    const resetFlow = () => {
        setSelectedFile(null);
        setPreview(null);
        setImportResult(null);
        setError("");
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const handleClose = () => {
        if (previewLoading || confirmLoading) return;
        resetFlow();
        onClose?.();
    };

    const taiTemplateXlsx = async () => {
        if (!lopId) return;
        setDownloadingTemplate(true);
        setError("");
        try {
            const response = await api.get(
                `/giang-vien/lop-hoc-phan/${lopId}/diem/template-xlsx`,
                { responseType: "blob" }
            );
            const blob = new Blob([response.data], {
                type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = `mau-nhap-diem-${maLop}.xlsx`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            setError(await docThongBaoLoiBlob(err));
        } finally {
            setDownloadingTemplate(false);
        }
    };

    const onSelectFile = (event) => {
        const file = event.target.files?.[0] || null;
        setError("");
        setPreview(null);
        setImportResult(null);

        if (!file) {
            setSelectedFile(null);
            return;
        }

        const lowerName = (file.name || "").trim().toLowerCase();
        if (lowerName.endsWith(".csv")) {
            setSelectedFile(null);
            setError(
                "Không chấp nhận tệp CSV. Chức năng này chỉ nhận tệp Excel định dạng .xlsx theo mẫu chuẩn."
            );
            return;
        }
        if (!lowerName.endsWith(".xlsx")) {
            setSelectedFile(null);
            setError("Định dạng tệp không hợp lệ. Vui lòng chỉ chọn tệp có phần mở rộng .xlsx.");
            return;
        }
        if (file.size > 2 * 1024 * 1024) {
            setSelectedFile(null);
            setError("Kích thước tệp vượt quá giới hạn tối đa cho phép (2 MB).");
            return;
        }

        setSelectedFile(file);
    };

    const thucHienPreview = async () => {
        if (!selectedFile || !lopId) {
            setError("Vui lòng chọn tệp Excel (.xlsx) trước khi xem trước.");
            return;
        }

        setPreviewLoading(true);
        setError("");
        setPreview(null);
        setImportResult(null);

        try {
            const formData = new FormData();
            formData.append("file", selectedFile);
            const { data } = await api.post(
                `/giang-vien/lop-hoc-phan/${lopId}/diem/import-xlsx/preview`,
                formData,
                {
                    headers: { "Content-Type": "multipart/form-data" },
                }
            );
            setPreview(data);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setPreviewLoading(false);
        }
    };

    const xacNhanImport = async () => {
        if (!preview || !preview.coTheXacNhan || !preview.previewToken || !lopId) return;
        setConfirmLoading(true);
        setError("");
        try {
            const { data } = await api.post(
                `/giang-vien/lop-hoc-phan/${lopId}/diem/import-xlsx/confirm`,
                {
                    previewToken: preview.previewToken,
                    fileSha256: preview.fileSha256,
                }
            );
            setImportResult(data);
            setPreview(null);
            onImportSuccess?.(data);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setConfirmLoading(false);
        }
    };

    return (
        <Modal
            open={open}
            onClose={handleClose}
            size="xl"
            title={`Import điểm từ Excel (.xlsx): ${lop?.ma || ""} - ${lop?.tenMon || ""}`}
            description="Tải tệp mẫu .xlsx của lớp, nhập điểm quá trình/cuối kỳ, kiểm tra bản xem trước (Preview) và xác nhận ghi vào bảng điểm."
            footer={
                <div className="flex w-full flex-wrap items-center justify-between gap-2">
                    <button
                        type="button"
                        className="btn-secondary text-xs"
                        onClick={taiTemplateXlsx}
                        disabled={downloadingTemplate}
                    >
                        <Download size={14} />
                        {downloadingTemplate ? "Đang tạo mẫu..." : "Tải template .xlsx chuẩn"}
                    </button>

                    <div className="flex items-center gap-2">
                        {(preview || importResult) && (
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                onClick={resetFlow}
                                disabled={previewLoading || confirmLoading}
                            >
                                <RefreshCw size={14} /> Chọn tệp khác
                            </button>
                        )}
                        {preview && (
                            <button
                                type="button"
                                className="btn-primary text-xs"
                                disabled={
                                    !preview.coTheXacNhan ||
                                    preview.soDongLoi > 0 ||
                                    confirmLoading ||
                                    disabled
                                }
                                onClick={xacNhanImport}
                            >
                                <CheckCircle2 size={14} />
                                {confirmLoading
                                    ? "Đang ghi vào bảng điểm..."
                                    : `Xác nhận Import (${preview.soDongHopLe} sinh viên)`}
                            </button>
                        )}
                        <button
                            type="button"
                            className="btn-secondary text-xs"
                            onClick={handleClose}
                            disabled={previewLoading || confirmLoading}
                        >
                            Đóng
                        </button>
                    </div>
                </div>
            }
        >
            <div className="space-y-4">
                {/* Thông tin quy tắc & template */}
                <div className="rounded-xl border border-slate-200 bg-slate-50/80 p-3.5 text-xs text-slate-600">
                    <div className="flex flex-wrap items-center justify-between gap-2 font-semibold text-slate-800">
                        <span className="inline-flex items-center gap-1.5">
                            <ShieldCheck size={15} className="text-emerald-600" />
                            Quy định định dạng Template & Chính sách Import (QLSV-73)
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                            <Badge tone="indigo" dot={false}>Chỉ nhận .xlsx</Badge>
                            <Badge tone="slate" dot={false}>Tối đa 2 MB · 500 dòng · 1 sheet</Badge>
                            <Badge tone="amber" dot={false}>Transaction: ALL_OR_NOTHING</Badge>
                        </div>
                    </div>
                    <ul className="mt-2 list-disc space-y-1 pl-4 leading-relaxed">
                        <li>
                            <strong>Cấu trúc cột chuẩn (Sheet <code>BangDiem</code>):</strong>{" "}
                            <code>maSinhVien</code> (bắt buộc, duy nhất, thuộc danh sách lớp),{" "}
                            <code>hoTen</code> (thông tin tra cứu),{" "}
                            <code>diemQuaTrinh</code> (0–10, tối đa 2 chữ số thập phân),{" "}
                            <code>diemCuoiKy</code> (0–10, tối đa 2 chữ số thập phân).
                        </li>
                        <li>
                            <strong>An toàn dữ liệu:</strong> Không chấp nhận CSV, .xls cũ hoặc ô chứa công thức Excel (<code>=SUM(...)</code>). Điểm tổng kết được backend tự động tính theo công thức <code>0.4 × quá trình + 0.6 × cuối kỳ</code> (làm tròn 1 chữ số thập phân).
                        </li>
                        <li>
                            <strong>Chính sách giao dịch & cập nhật:</strong> <code>ALL_OR_NOTHING</code> (nếu có bất kỳ dòng lỗi nào, toàn bộ tệp sẽ bị chặn xác nhận để giảng viên chỉnh sửa) và <code>OVERWRITE_EXISTING</code> (cập nhật ghi đè điểm nháp hiện có, không tạo bản ghi trùng khi upload lại).
                        </li>
                    </ul>
                </div>

                {error && (
                    <Alert tone="error" onClose={() => setError("")}>
                        {error}
                    </Alert>
                )}

                {/* Bước 1: Chọn tệp và bấm Xem trước */}
                {!importResult && (
                    <div className="rounded-xl border border-dashed border-slate-300 bg-white p-4">
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div className="flex items-center gap-3">
                                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700">
                                    <FileSpreadsheet size={20} />
                                </span>
                                <div>
                                    <p className="text-sm font-medium text-slate-800">
                                        Chọn tệp bảng điểm Excel (.xlsx)
                                    </p>
                                    <p className="text-xs text-slate-500">
                                        {selectedFile
                                            ? `Đã chọn: ${selectedFile.name} (${(selectedFile.size / 1024).toFixed(1)} KB)`
                                            : "Chỉ chấp nhận tệp .xlsx theo mẫu chuẩn của lớp học phần."}
                                    </p>
                                </div>
                            </div>
                            <div className="flex flex-wrap items-center gap-2">
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    onChange={onSelectFile}
                                    className="block text-xs text-slate-600 file:mr-2 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-slate-700 hover:file:bg-slate-200"
                                />
                                <button
                                    type="button"
                                    className="btn-primary text-xs"
                                    disabled={!selectedFile || previewLoading || disabled}
                                    onClick={thucHienPreview}
                                >
                                    <Upload size={14} />
                                    {previewLoading ? "Đang kiểm tra..." : "Phân tích & Xem trước"}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Bước 2: Hiển thị kết quả Xem trước (Preview) */}
                {preview && (
                    <div className="space-y-4">
                        <Alert tone={preview.soDongLoi > 0 ? "warning" : "info"}>
                            {preview.soDongLoi > 0 ? (
                                <span>
                                    Phát hiện <strong>{preview.soDongLoi}</strong> dòng có lỗi trong tệp{" "}
                                    <strong>{preview.tenTep}</strong> (Sheet <code>{preview.sheetName}</code>). Theo chính sách{" "}
                                    <code>{preview.transactionPolicy}</code>, dữ liệu chưa được ghi vào bảng điểm. Vui lòng kiểm tra bảng lỗi chi tiết theo dòng/cột bên dưới, sửa tệp và tải lên lại.
                                </span>
                            ) : (
                                <span>
                                    Bản xem trước tệp <strong>{preview.tenTep}</strong> hoàn toàn hợp lệ (
                                    <strong>{preview.soDongHopLe}</strong> dòng). Dữ liệu{" "}
                                    <strong>chưa được ghi vào bảng điểm</strong> cho đến khi bạn bấm{" "}
                                    <strong>Xác nhận Import</strong>.
                                </span>
                            )}
                        </Alert>

                        {/* Thống kê tổng quan Preview */}
                        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-slate-500">Tổng số dòng</p>
                                <p className="mt-1 text-xl font-bold text-slate-900">{preview.tongSoDong}</p>
                            </div>
                            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-emerald-700">Dòng hợp lệ</p>
                                <p className="mt-1 text-xl font-bold text-emerald-800">{preview.soDongHopLe}</p>
                            </div>
                            <div className="rounded-xl border border-rose-200 bg-rose-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-rose-700">Dòng lỗi</p>
                                <p className="mt-1 text-xl font-bold text-rose-800">{preview.soDongLoi}</p>
                            </div>
                            <div className="rounded-xl border border-sky-200 bg-sky-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-sky-700">Dự kiến tạo mới</p>
                                <p className="mt-1 text-xl font-bold text-sky-800">{preview.soTaoMoiDuKien}</p>
                            </div>
                            <div className="rounded-xl border border-indigo-200 bg-indigo-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-indigo-700">Dự kiến cập nhật</p>
                                <p className="mt-1 text-xl font-bold text-indigo-800">{preview.soCapNhatDuKien}</p>
                            </div>
                        </div>

                        {/* Bảng chi tiết lỗi theo Sheet / Dòng / Cột */}
                        {Array.isArray(preview.errors) && preview.errors.length > 0 && (
                            <div className="overflow-hidden rounded-xl border border-rose-200 bg-white">
                                <div className="flex items-center gap-2 border-b border-rose-100 bg-rose-50 px-4 py-2.5 text-xs font-semibold text-rose-800">
                                    <AlertTriangle size={15} />
                                    Chi tiết lỗi theo Sheet / Dòng / Cột ({preview.errors.length} lỗi)
                                </div>
                                <div className="max-h-56 overflow-y-auto">
                                    <table className="w-full text-xs">
                                        <thead className="bg-slate-50 text-slate-600">
                                            <tr>
                                                <th className="px-3 py-2 text-left">Sheet</th>
                                                <th className="px-3 py-2 text-center">Dòng</th>
                                                <th className="px-3 py-2 text-left">Cột</th>
                                                <th className="px-3 py-2 text-left">Mã SV</th>
                                                <th className="px-3 py-2 text-left">Giá trị nhập</th>
                                                <th className="px-3 py-2 text-left">Nguyên nhân lỗi</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-rose-100">
                                            {preview.errors.map((err, idx) => (
                                                <tr key={idx} className="bg-rose-50/20">
                                                    <td className="px-3 py-2 font-mono text-slate-700">
                                                        {err.sheet || preview.sheetName}
                                                    </td>
                                                    <td className="px-3 py-2 text-center font-semibold text-rose-700">
                                                        {err.dong ?? "—"}
                                                    </td>
                                                    <td className="px-3 py-2 font-medium text-slate-800">
                                                        {err.cotExcel ? `${err.cotExcel} ` : ""}
                                                        ({FIELD_LABEL[err.cot] || err.cot || err.field})
                                                    </td>
                                                    <td className="px-3 py-2 font-mono text-slate-700">
                                                        {err.maSinhVien || "—"}
                                                    </td>
                                                    <td className="px-3 py-2 font-mono text-rose-700">
                                                        {err.giaTriNhap !== undefined && err.giaTriNhap !== null && err.giaTriNhap !== ""
                                                            ? String(err.giaTriNhap)
                                                            : "(trống)"}
                                                    </td>
                                                    <td className="px-3 py-2 font-medium text-rose-700">
                                                        {err.message}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Bảng dữ liệu xem trước từng dòng */}
                        <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                            <div className="border-b border-slate-200 bg-slate-50 px-4 py-2.5 text-xs font-semibold text-slate-700">
                                Bảng đối chiếu dữ liệu xem trước ({preview.danhSachDong?.length || 0} dòng)
                            </div>
                            <div className="max-h-72 overflow-y-auto">
                                <table className="w-full text-xs">
                                    <thead className="bg-white text-slate-600">
                                        <tr>
                                            <th className="px-3 py-2 text-center">Dòng</th>
                                            <th className="px-3 py-2 text-left">Mã SV</th>
                                            <th className="px-3 py-2 text-left">Họ tên</th>
                                            <th className="px-3 py-2 text-center">Hiện tại (QT / CK / TK)</th>
                                            <th className="px-3 py-2 text-center">Import mới (QT / CK)</th>
                                            <th className="px-3 py-2 text-center">TK dự kiến</th>
                                            <th className="px-3 py-2 text-center">Trạng thái</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {(preview.danhSachDong || []).map((row) => {
                                            const badge = ACTION_BADGE[row.hanhDongDuKien] || ACTION_BADGE.LOI;
                                            return (
                                                <tr
                                                    key={row.dong}
                                                    className={!row.hopLe ? "bg-rose-50/40" : "hover:bg-slate-50"}
                                                >
                                                    <td className="px-3 py-2 text-center font-mono text-slate-500">
                                                        {row.dong}
                                                    </td>
                                                    <td className="px-3 py-2 font-mono font-semibold text-slate-900">
                                                        {row.maSinhVien || "—"}
                                                    </td>
                                                    <td className="px-3 py-2 text-slate-700">
                                                        {row.hoTen || "—"}
                                                    </td>
                                                    <td className="px-3 py-2 text-center text-slate-500">
                                                        {formatDiem(row.diemQuaTrinhHienTai)} /{" "}
                                                        {formatDiem(row.diemCuoiKyHienTai)} /{" "}
                                                        <span className="font-medium">
                                                            {formatDiem(row.diemTongKetHienTai)}
                                                        </span>
                                                    </td>
                                                    <td className="px-3 py-2 text-center font-medium text-slate-900">
                                                        {formatDiem(row.diemQuaTrinhMoi)} /{" "}
                                                        {formatDiem(row.diemCuoiKyMoi)}
                                                    </td>
                                                    <td className="px-3 py-2 text-center font-semibold text-indigo-700">
                                                        {formatDiem(row.diemTongKetDuKien)}
                                                    </td>
                                                    <td className="px-3 py-2 text-center">
                                                        <Badge tone={badge.tone}>{badge.label}</Badge>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}

                {/* Bước 3: Kết quả sau khi xác nhận Import */}
                {importResult && (
                    <div className="space-y-4">
                        <Alert tone="success">
                            Đã xác nhận import điểm thành công từ tệp <strong>{importResult.tenTep}</strong> cho lớp{" "}
                            <strong>{importResult.maLop}</strong> theo chính sách{" "}
                            <code>{importResult.transactionPolicy}</code>.
                        </Alert>

                        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-slate-500">Tổng dòng xử lý</p>
                                <p className="mt-1 text-xl font-bold text-slate-900">{importResult.tongSoDong}</p>
                            </div>
                            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-emerald-700">Đã tạo mới</p>
                                <p className="mt-1 text-xl font-bold text-emerald-800">{importResult.soTaoMoi}</p>
                            </div>
                            <div className="rounded-xl border border-indigo-200 bg-indigo-50/70 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-indigo-700">Đã cập nhật</p>
                                <p className="mt-1 text-xl font-bold text-indigo-800">{importResult.soCapNhat}</p>
                            </div>
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-center">
                                <p className="text-[11px] font-medium uppercase text-slate-500">Thất bại</p>
                                <p className="mt-1 text-xl font-bold text-slate-800">{importResult.soThatBai}</p>
                            </div>
                        </div>

                        <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                            <div className="border-b border-slate-200 bg-slate-50 px-4 py-2.5 text-xs font-semibold text-slate-700">
                                Chi tiết kết quả ghi điểm ({importResult.danhSachKetQua?.length || 0} sinh viên)
                            </div>
                            <div className="max-h-64 overflow-y-auto">
                                <table className="w-full text-xs">
                                    <thead className="bg-white text-slate-600">
                                        <tr>
                                            <th className="px-3 py-2 text-center">Dòng</th>
                                            <th className="px-3 py-2 text-left">Mã SV</th>
                                            <th className="px-3 py-2 text-left">Họ tên</th>
                                            <th className="px-3 py-2 text-center">Trước import (QT / CK / TK)</th>
                                            <th className="px-3 py-2 text-center">Sau import (QT / CK / TK)</th>
                                            <th className="px-3 py-2 text-center">Kết quả</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {(importResult.danhSachKetQua || []).map((item) => {
                                            const badge = ACTION_BADGE[item.hanhDong] || ACTION_BADGE.CAP_NHAT;
                                            return (
                                                <tr key={item.dangKyId || item.dong}>
                                                    <td className="px-3 py-2 text-center font-mono text-slate-500">
                                                        {item.dong}
                                                    </td>
                                                    <td className="px-3 py-2 font-mono font-semibold text-slate-900">
                                                        {item.maSinhVien}
                                                    </td>
                                                    <td className="px-3 py-2 text-slate-700">{item.hoTen || "—"}</td>
                                                    <td className="px-3 py-2 text-center text-slate-500">
                                                        {formatDiem(item.diemQuaTrinhCu)} /{" "}
                                                        {formatDiem(item.diemCuoiKyCu)} /{" "}
                                                        {formatDiem(item.diemTongKetCu)}
                                                    </td>
                                                    <td className="px-3 py-2 text-center font-semibold text-emerald-700">
                                                        {formatDiem(item.diemQuaTrinhMoi)} /{" "}
                                                        {formatDiem(item.diemCuoiKyMoi)} /{" "}
                                                        {formatDiem(item.diemTongKetMoi)}
                                                    </td>
                                                    <td className="px-3 py-2 text-center">
                                                        <Badge tone={badge.tone}>{badge.label}</Badge>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </Modal>
    );
}
