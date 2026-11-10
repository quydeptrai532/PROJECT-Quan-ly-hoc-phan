import { useState } from "react";
import { Download } from "lucide-react";
import api, { getErrorMessage } from "../api/client";

async function docThongBaoLoi(err) {
    const data = err?.response?.data;
    if (data instanceof Blob) {
        try {
            const parsed = JSON.parse(await data.text());
            if (parsed?.message) return parsed.message;
        } catch {
            // Không phải JSON, dùng thông báo mặc định bên dưới.
        }
    }
    return getErrorMessage(err);
}

function layTenTepTuHeader(headers, macDinh) {
    const disposition = headers?.["content-disposition"] || headers?.["Content-Disposition"];
    if (!disposition) return macDinh;

    const utf8Match = disposition.match(/filename\*=UTF-8''([^;\s]+)/i);
    if (utf8Match?.[1]) {
        try {
            return decodeURIComponent(utf8Match[1]);
        } catch {
            return utf8Match[1];
        }
    }

    const asciiMatch = disposition.match(/filename="?([^";]+)"?/i);
    if (asciiMatch?.[1]) {
        return asciiMatch[1].trim();
    }
    return macDinh;
}

/** Nút xuất danh sách sinh viên của lớp học phần ra CSV chuẩn hóa và an toàn (QLSV-91 / TASK-OP-04). */
export default function ExportStudentsButton({
    lopId,
    maLop,
    soSinhVien,
    siSo,
    tuKhoa,
    sapXep,
    loading,
    disabled = false,
    onSuccess,
    onError
}) {
    const [exporting, setExporting] = useState(false);
    const count = soSinhVien !== undefined ? soSinhVien : (siSo !== undefined ? siSo : 0);

    const xuatDanhSach = async () => {
        setExporting(true);
        onError?.("");
        onSuccess?.("");
        try {
            const params = {};
            if (tuKhoa && tuKhoa.trim()) params.tuKhoa = tuKhoa.trim();
            if (sapXep && sapXep.trim()) params.sapXep = sapXep.trim();

            const response = await api.get(`/giang-vien/lop-hoc-phan/${lopId}/sinh-vien/csv`, {
                params,
                responseType: "blob"
            });
            const defaultFilename = `danh-sach-sinh-vien-${maLop || lopId}.csv`;
            const tenTep = layTenTepTuHeader(response.headers, defaultFilename);

            const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8" }));
            const link = document.createElement("a");
            link.href = url;
            link.download = tenTep;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            onSuccess?.(`Đã xuất danh sách ${count} sinh viên ra file CSV (${tenTep})`);
        } catch (err) {
            onError?.(await docThongBaoLoi(err));
        } finally {
            setExporting(false);
        }
    };

    return (
        <button
            type="button"
            className="btn-secondary px-3 py-1.5 text-xs"
            disabled={disabled || loading || count === 0 || exporting}
            title={count === 0 ? "Không có sinh viên phù hợp bộ lọc để xuất" : "Tải danh sách sinh viên (CSV, UTF-8)"}
            onClick={xuatDanhSach}
        >
            <Download size={14} />
            {exporting ? "Đang xuất..." : "Xuất CSV"}
        </button>
    );
}
