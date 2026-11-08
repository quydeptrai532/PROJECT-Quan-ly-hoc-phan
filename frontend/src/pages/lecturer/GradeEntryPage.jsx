import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
    AlertTriangle,
    ArrowLeft,
    Calculator,
    ClipboardList,
    Eye,
    FileSpreadsheet,
    History,
    Lock,
    Megaphone,
    Save,
    Search,
    Send,
    Upload,
    Users,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import ExportStudentsButton from "../../components/ExportStudentsButton";
import ClassAnnouncementModal from "../../components/ClassAnnouncementModal";
import GradeExcelImportModal from "../../components/GradeExcelImportModal";
import { Alert, Badge, EmptyState, Modal, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime, formatDiem, lichHoc } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const GRADE_SHEET_STATUS_META = {
    NHAP: { label: "Bản nháp (Cho phép sửa)", tone: "slate" },
    CHO_DUYET: { label: "Chờ Phòng Đào tạo duyệt (Đã khóa sửa)", tone: "amber" },
    TRA_VE: { label: "Trả về chỉnh sửa (Cho phép sửa)", tone: "rose" },
    DA_DUYET: { label: "Đã phê duyệt (Chờ công bố/khóa)", tone: "blue" },
    DA_CONG_BO: { label: "Đã công bố (Đã khóa sửa)", tone: "green" },
    DA_KHOA: { label: "Đã khóa bảng điểm", tone: "indigo" },
};

const ACTION_LABEL = {
    GUI_DUYET: "Giảng viên gửi duyệt",
    TRA_VE: "Trả về chỉnh sửa",
    PHE_DUYET: "Phê duyệt bảng điểm",
    CONG_BO: "Công bố bảng điểm",
    KHOA: "Khóa bảng điểm",
    MO_KHOA: "Mở khóa bảng điểm",
};

function tinhTongKet(quaTrinh, cuoiKy) {
    const qt = Number(quaTrinh);
    const ck = Number(cuoiKy);
    if (quaTrinh === "" || cuoiKy === "" || quaTrinh === null || cuoiKy === null || Number.isNaN(qt) || Number.isNaN(ck)) return null;
    return Math.round((0.4 * qt + 0.6 * ck) * 10) / 10;
}

export default function GradeEntryPage() {
    const { id } = useParams();
    const { data: lop, loading: loadingLop, error: lopError, reload: reloadLop } = useApi(`/lop-hoc-phan/${id}`);
    const {
        data: trangThaiDetail,
        reload: reloadTrangThai,
    } = useApi(`/giang-vien/lop-hoc-phan/${id}/trang-thai-bang-diem`);
    const {
        data: sinhVienList,
        loading: loadingSv,
        error: svError,
        reload: reloadSv,
    } = useApi(`/giang-vien/lop-hoc-phan/${id}/sinh-vien`);

    const [rows, setRows] = useState([]);
    const [edits, setEdits] = useState({});
    const [fieldErrors, setFieldErrors] = useState({});
    const [busyId, setBusyId] = useState(null);
    const [workflowBusy, setWorkflowBusy] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [searchQuery, setSearchQuery] = useState("");

    const [profileModalOpen, setProfileModalOpen] = useState(false);
    const [announcementModalOpen, setAnnouncementModalOpen] = useState(false);
    const [importModalOpen, setImportModalOpen] = useState(false);
    const [historyModalOpen, setHistoryModalOpen] = useState(false);
    const [templateDownloading, setTemplateDownloading] = useState(false);
    const [profileLoading, setProfileLoading] = useState(false);
    const [profileError, setProfileError] = useState("");
    const [studentProfile, setStudentProfile] = useState(null);

    useEffect(() => {
        setRows(sinhVienList || []);
    }, [sinhVienList]);

    const filteredRows = useMemo(() => {
        if (!searchQuery.trim()) return rows;
        const q = searchQuery.trim().toLowerCase();
        return rows.filter(
            (row) =>
                (row.maSinhVien && row.maSinhVien.toLowerCase().includes(q)) ||
                (row.hoTen && row.hoTen.toLowerCase().includes(q)) ||
                (row.email && row.email.toLowerCase().includes(q))
        );
    }, [rows, searchQuery]);

    const soDaNhap = useMemo(
        () => rows.filter((row) => row.diemTongKet !== null && row.diemTongKet !== undefined).length,
        [rows]
    );

    const trangThaiBangDiem = trangThaiDetail?.trangThaiBangDiem || lop?.trangThaiBangDiem || "NHAP";
    const statusMeta = GRADE_SHEET_STATUS_META[trangThaiBangDiem] || GRADE_SHEET_STATUS_META.NHAP;
    const choPhepSuaDiem = trangThaiBangDiem === "NHAP" || trangThaiBangDiem === "TRA_VE";
    const biKhoaSua = !choPhepSuaDiem;
    const duDieuKienDuyet = rows.length > 0 && soDaNhap === rows.length && Object.keys(edits).length === 0;
    const soLuongThayDoi = Object.keys(edits).length;
    const lyDoTraVe = trangThaiDetail?.lyDoTraVe || lop?.lyDoTraVeBangDiem;
    const lichSu = trangThaiDetail?.lichSu || [];

    const reloadAll = async () => {
        await Promise.all([reloadLop(), reloadTrangThai(), reloadSv()]);
    };

    const onChange = (dangKyId, field, value) => {
        if (biKhoaSua) return;
        setEdits((current) => ({
            ...current,
            [dangKyId]: {
                ...(current[dangKyId] || {
                    diemQuaTrinh: rows.find((row) => row.dangKyId === dangKyId)?.diemQuaTrinh ?? "",
                    diemCuoiKy: rows.find((row) => row.dangKyId === dangKyId)?.diemCuoiKy ?? "",
                }),
                [field]: value,
            },
        }));

        if (fieldErrors[dangKyId]?.[field]) {
            setFieldErrors((current) => {
                const next = { ...current };
                if (next[dangKyId]) {
                    const sub = { ...next[dangKyId] };
                    delete sub[field];
                    if (Object.keys(sub).length === 0) {
                        delete next[dangKyId];
                    } else {
                        next[dangKyId] = sub;
                    }
                }
                return next;
            });
        }
    };

    const getValue = (row, field) => {
        if (edits[row.dangKyId]?.[field] !== undefined) {
            return edits[row.dangKyId][field];
        }
        const value = row[field];
        return value === null || value === undefined ? "" : value;
    };

    const parseErrorsToFieldMap = (resErrors, defaultDangKyId) => {
        const nextErr = {};
        if (Array.isArray(resErrors)) {
            for (const item of resErrors) {
                const targetId = item.dangKyId || defaultDangKyId;
                const targetCol = item.cot || item.field || "general";
                if (targetId) {
                    if (!nextErr[targetId]) nextErr[targetId] = {};
                    nextErr[targetId][targetCol] = item.message;
                }
            }
        }
        return nextErr;
    };

    const luuDiem = async (row) => {
        const quaTrinh = getValue(row, "diemQuaTrinh");
        const cuoiKy = getValue(row, "diemCuoiKy");

        const clientErrors = {};
        if (quaTrinh !== "" && quaTrinh !== null && quaTrinh !== undefined) {
            const qt = Number(quaTrinh);
            if (Number.isNaN(qt) || qt < 0 || qt > 10) {
                clientErrors.diemQuaTrinh = "Điểm phải từ 0 đến 10";
            }
        }
        if (cuoiKy !== "" && cuoiKy !== null && cuoiKy !== undefined) {
            const ck = Number(cuoiKy);
            if (Number.isNaN(ck) || ck < 0 || ck > 10) {
                clientErrors.diemCuoiKy = "Điểm phải từ 0 đến 10";
            }
        }
        if (Object.keys(clientErrors).length > 0) {
            setFieldErrors((current) => ({
                ...current,
                [row.dangKyId]: { ...(current[row.dangKyId] || {}), ...clientErrors },
            }));
            setError(`Dữ liệu điểm của sinh viên ${row.maSinhVien} không hợp lệ (phải từ 0 đến 10)`);
            return;
        }

        setBusyId(row.dangKyId);
        setError("");
        setMessage("");
        try {
            const payload = {
                diemQuaTrinh: quaTrinh === "" ? null : Number(quaTrinh),
                diemCuoiKy: cuoiKy === "" ? null : Number(cuoiKy),
            };
            const { data } = await api.put(`/diem/${row.dangKyId}`, payload);
            setRows((current) =>
                current.map((item) => (item.dangKyId === row.dangKyId ? { ...item, ...data } : item))
            );
            setEdits((current) => {
                const next = { ...current };
                delete next[row.dangKyId];
                return next;
            });
            setFieldErrors((current) => {
                const next = { ...current };
                delete next[row.dangKyId];
                return next;
            });
            setMessage(`Đã lưu điểm cho ${row.maSinhVien} - ${row.hoTen}`);
            await Promise.all([reloadLop(), reloadTrangThai()]);
        } catch (err) {
            const responseData = err.response?.data;
            if (responseData?.errors && Array.isArray(responseData.errors) && responseData.errors.length > 0) {
                const mapped = parseErrorsToFieldMap(responseData.errors, row.dangKyId);
                setFieldErrors((current) => ({ ...current, ...mapped }));
                setError(responseData.message || "Dữ liệu điểm không hợp lệ");
            } else {
                setError(getErrorMessage(err));
            }
        } finally {
            setBusyId(null);
        }
    };

    const luuTatCa = async () => {
        if (soLuongThayDoi === 0 || biKhoaSua) return;
        setWorkflowBusy(true);
        setError("");
        setMessage("");

        const clientErrors = {};
        const danhSachPayload = [];
        for (const [dangKyIdStr, val] of Object.entries(edits)) {
            const dkId = Number(dangKyIdStr);
            const qtVal = val.diemQuaTrinh;
            const ckVal = val.diemCuoiKy;

            const errs = {};
            if (qtVal !== "" && qtVal !== null && qtVal !== undefined) {
                const qt = Number(qtVal);
                if (Number.isNaN(qt) || qt < 0 || qt > 10) errs.diemQuaTrinh = "Điểm phải từ 0 đến 10";
            }
            if (ckVal !== "" && ckVal !== null && ckVal !== undefined) {
                const ck = Number(ckVal);
                if (Number.isNaN(ck) || ck < 0 || ck > 10) errs.diemCuoiKy = "Điểm phải từ 0 đến 10";
            }
            if (Object.keys(errs).length > 0) {
                clientErrors[dkId] = errs;
            }
            danhSachPayload.push({
                dangKyId: dkId,
                diemQuaTrinh: qtVal === "" ? null : Number(qtVal),
                diemCuoiKy: ckVal === "" ? null : Number(ckVal),
            });
        }

        if (Object.keys(clientErrors).length > 0) {
            setFieldErrors((current) => ({ ...current, ...clientErrors }));
            setError("Có một số ô điểm nhập ngoài miền giá trị hợp lệ (0-10). Vui lòng kiểm tra lại");
            setWorkflowBusy(false);
            return;
        }

        try {
            const { data } = await api.put(`/giang-vien/lop-hoc-phan/${id}/diem`, { danhSach: danhSachPayload });
            setRows((current) => {
                const updatedMap = new Map((data || []).map((item) => [item.dangKyId, item]));
                return current.map((row) => (updatedMap.has(row.dangKyId) ? { ...row, ...updatedMap.get(row.dangKyId) } : row));
            });
            setEdits({});
            setFieldErrors({});
            setMessage(`Đã lưu thành công ${danhSachPayload.length} điểm sinh viên trong một thao tác`);
            await Promise.all([reloadLop(), reloadTrangThai()]);
        } catch (err) {
            const responseData = err.response?.data;
            if (responseData?.errors && Array.isArray(responseData.errors) && responseData.errors.length > 0) {
                const mapped = parseErrorsToFieldMap(responseData.errors, null);
                setFieldErrors((current) => ({ ...current, ...mapped }));
                setError(responseData.message || "Dữ liệu cập nhật có điểm không hợp lệ; toàn bộ giao dịch đã được rollback.");
            } else {
                setError(getErrorMessage(err));
            }
        } finally {
            setWorkflowBusy(false);
        }
    };

    const guiDuyetBangDiem = async () => {
        setWorkflowBusy(true);
        setError("");
        setMessage("");
        try {
            await api.post(`/giang-vien/lop-hoc-phan/${id}/gui-duyet-diem`);
            setMessage(
                trangThaiBangDiem === "TRA_VE"
                    ? "Đã gửi duyệt lại bảng điểm sau khi chỉnh sửa. Bảng điểm hiện tạm khóa chỉnh sửa và chờ Phòng Đào tạo phê duyệt."
                    : "Đã gửi duyệt bảng điểm lên Phòng Đào tạo. Bảng điểm hiện tạm khóa chỉnh sửa và chưa phát thông báo tới sinh viên."
            );
            await Promise.all([reloadLop(), reloadTrangThai()]);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setWorkflowBusy(false);
        }
    };

    const xemHoSoSinhVien = async (sinhVienId) => {
        setProfileModalOpen(true);
        setProfileLoading(true);
        setProfileError("");
        setStudentProfile(null);
        try {
            const { data } = await api.get(`/giang-vien/lop-hoc-phan/${id}/sinh-vien/${sinhVienId}`);
            setStudentProfile(data);
        } catch (err) {
            setProfileError(getErrorMessage(err));
        } finally {
            setProfileLoading(false);
        }
    };

    const taiTemplateExcel = async () => {
        if (!lop?.id) return;
        setTemplateDownloading(true);
        setError("");
        setMessage("");
        try {
            const response = await api.get(`/giang-vien/lop-hoc-phan/${lop.id}/diem/template-xlsx`, {
                responseType: "blob",
            });
            const url = window.URL.createObjectURL(
                new Blob([response.data], {
                    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                })
            );
            const link = document.createElement("a");
            link.href = url;
            link.download = `mau-nhap-diem-${lop.ma || lop.id}.xlsx`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            setMessage(`Đã tải xuống tệp mẫu Excel (.xlsx) cho lớp ${lop.ma}`);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setTemplateDownloading(false);
        }
    };

    return (
        <div>
            <Link
                to="/lop-cua-toi"
                className="mb-4 inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-800"
            >
                <ArrowLeft size={14} /> Quay lại danh sách lớp
            </Link>

            <PageHeader
                icon={ClipboardList}
                title={lop ? `Nhập điểm: ${lop.ma} - ${lop.tenMon}` : "Nhập điểm lớp học phần"}
                description={
                    lop
                        ? `${lop.tenHocKy} · Phòng ${lop.phong} · ${lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · Sĩ số ${lop.siSoHienTai}/${lop.siSoToiDa}`
                        : "Quản lý điểm thành phần và quy trình gửi duyệt bảng điểm"
                }
                actions={
                    lop && (
                        <div className="flex flex-wrap items-center gap-2">
                            <Badge tone={statusMeta.tone}>{statusMeta.label}</Badge>
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                onClick={() => setHistoryModalOpen(true)}
                                title="Xem lịch sử chuyển trạng thái và phê duyệt bảng điểm"
                            >
                                <History size={14} /> Lịch sử duyệt ({lichSu.length})
                            </button>
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                onClick={taiTemplateExcel}
                                disabled={templateDownloading || loadingSv}
                                title="Tải tệp mẫu Excel (.xlsx) chứa danh sách sinh viên và cột điểm hợp lệ"
                            >
                                <FileSpreadsheet size={14} />
                                {templateDownloading ? "Đang tải mẫu..." : "Tải mẫu .xlsx"}
                            </button>
                            <button
                                type="button"
                                className="btn-primary text-xs"
                                onClick={() => setImportModalOpen(true)}
                                disabled={biKhoaSua || loadingSv}
                                title={
                                    biKhoaSua
                                        ? `Bảng điểm đang ở trạng thái ${trangThaiBangDiem}, không thể import điểm`
                                        : "Import điểm từ tệp Excel (.xlsx) có xem trước và kiểm tra lỗi"
                                }
                            >
                                <Upload size={14} /> Import điểm (.xlsx)
                            </button>
                            <button
                                type="button"
                                className="btn-secondary text-xs"
                                onClick={() => setAnnouncementModalOpen(true)}
                                title="Gửi thông báo đến sinh viên trong lớp học phần"
                            >
                                <Megaphone size={14} /> Gửi thông báo
                            </button>
                            <ExportStudentsButton
                                lopId={lop.id}
                                maLop={lop.ma}
                                soSinhVien={rows.length}
                                siSo={rows.length}
                                disabled={loadingSv || Boolean(svError)}
                                loading={loadingSv}
                                onSuccess={(text) => {
                                    setError("");
                                    setMessage(text);
                                }}
                                onError={(text) => {
                                    setMessage("");
                                    setError(text);
                                }}
                            />
                        </div>
                    )
                }
            />

            {(lopError || svError) && <Alert tone="error" className="mb-4">{lopError || svError}</Alert>}
            <div className="mb-4 space-y-3">
                {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
                {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
            </div>

            {trangThaiBangDiem === "TRA_VE" && (
                <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs text-rose-800 shadow-sm">
                    <div className="flex items-start gap-2.5">
                        <AlertTriangle size={16} className="mt-0.5 shrink-0 text-rose-600" />
                        <div className="space-y-1">
                            <p className="font-semibold text-rose-900">
                                Bảng điểm đã được Phòng Đào tạo trả về yêu cầu chỉnh sửa
                            </p>
                            {lyDoTraVe && (
                                <p>
                                    <span className="font-medium">Lý do trả về / mở khóa:</span> {lyDoTraVe}
                                </p>
                            )}
                            {trangThaiDetail?.traVeLuc && (
                                <p className="text-[11px] text-rose-700">
                                    Thời điểm trả về: {formatDateTime(trangThaiDetail.traVeLuc)}
                                </p>
                            )}
                            <p className="text-rose-700">
                                Giảng viên phụ trách có thể chỉnh sửa trực tiếp hoặc import lại tệp .xlsx, sau đó bấm{" "}
                                <span className="font-semibold">Gửi duyệt lại</span>.
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {biKhoaSua && (
                <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50/70 px-4 py-3 text-xs text-amber-900 shadow-sm">
                    <div className="flex items-start gap-2.5">
                        <Lock size={15} className="mt-0.5 shrink-0 text-amber-600" />
                        <div className="space-y-1">
                            <p className="font-semibold">
                                Bảng điểm hiện đang khóa chỉnh sửa ({statusMeta.label})
                            </p>
                            <p className="text-amber-800">
                                {trangThaiBangDiem === "CHO_DUYET" &&
                                    "Bảng điểm đang chờ Phòng Đào tạo phê duyệt hoặc trả về chỉnh sửa. Giảng viên không thể sửa hoặc import điểm trong lúc chờ duyệt."}
                                {trangThaiBangDiem === "DA_DUYET" &&
                                    "Bảng điểm đã được Phòng Đào tạo phê duyệt và đang chờ công bố/khóa chính thức. Sinh viên chưa xem được điểm cho tới khi công bố."}
                                {(trangThaiBangDiem === "DA_CONG_BO" || trangThaiBangDiem === "DA_KHOA") &&
                                    "Bảng điểm đã được công bố/khóa chính thức và hiển thị cho sinh viên. Mọi thao tác nhập/sửa/import điểm đều bị khóa cho tới khi người có quyền đặc biệt mở khóa kèm lý do bắt buộc."}
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {lop && (
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
                    <div className="text-xs text-slate-600">
                        <span className="font-semibold text-slate-800">Quy trình duyệt & khóa bảng điểm:</span>{" "}
                        Nháp (`NHAP`) → Gửi duyệt (`CHO_DUYET`) → Trả chỉnh sửa (`TRA_VE`) hoặc Đã duyệt (`DA_DUYET`) → Đã công bố / Đã khóa (`DA_CONG_BO` / `DA_KHOA`).
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                        {choPhepSuaDiem && (
                            <button
                                type="button"
                                className="btn-primary text-xs"
                                disabled={workflowBusy || !duDieuKienDuyet}
                                onClick={guiDuyetBangDiem}
                                title={
                                    !duDieuKienDuyet
                                        ? "Cần nhập đủ điểm cho toàn bộ sinh viên đang học và lưu hết các thay đổi trước khi gửi duyệt"
                                        : "Gửi bảng điểm lên Phòng Đào tạo để phê duyệt"
                                }
                            >
                                <Send size={14} />
                                {trangThaiBangDiem === "TRA_VE" ? "Gửi duyệt lại" : "Gửi duyệt bảng điểm"}
                            </button>
                        )}
                    </div>
                </div>
            )}

            <div className="card overflow-hidden">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
                    <div className="flex items-center gap-3">
                        <p className="text-sm font-medium text-slate-700">Danh sách sinh viên</p>
                        <div className="relative">
                            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
                            <input
                                type="text"
                                className="input h-8 w-48 pl-8 text-xs sm:w-64"
                                placeholder="Lọc theo mã hoặc tên SV..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-3">
                        {soLuongThayDoi > 0 && choPhepSuaDiem && (
                            <button
                                type="button"
                                className="btn-primary text-xs"
                                disabled={workflowBusy}
                                onClick={luuTatCa}
                            >
                                <Save size={14} /> Lưu tất cả thay đổi ({soLuongThayDoi})
                            </button>
                        )}
                        <p className="text-xs text-slate-500">
                            Đã nhập đủ điểm: {soDaNhap}/{rows.length} · Điểm tổng kết = 40% quá trình + 60% cuối kỳ
                        </p>
                    </div>
                </div>

                {loadingSv ? (
                    <Spinner />
                ) : rows.length === 0 ? (
                    <EmptyState
                        icon={Users}
                        title="Lớp chưa có sinh viên đăng ký"
                        description="Sinh viên cần đăng ký lớp trước khi giảng viên nhập điểm."
                    />
                ) : filteredRows.length === 0 ? (
                    <div className="p-8 text-center text-sm text-slate-500">
                        Không tìm thấy sinh viên khớp với từ khóa "{searchQuery}".
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[900px]">
                            <thead className="bg-white">
                            <tr>
                                <th className="th w-12 text-center">STT</th>
                                <th className="th">Mã SV</th>
                                <th className="th">Họ tên</th>
                                <th className="th">Email</th>
                                <th className="th w-28 text-center">Hồ sơ</th>
                                <th className="th w-36 text-center">Quá trình</th>
                                <th className="th w-36 text-center">Cuối kỳ</th>
                                <th className="th w-28 text-center">Tổng kết</th>
                                <th className="th w-28 text-right">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                            {filteredRows.map((row, index) => {
                                const quaTrinh = getValue(row, "diemQuaTrinh");
                                const cuoiKy = getValue(row, "diemCuoiKy");
                                const tongKet = tinhTongKet(quaTrinh, cuoiKy);
                                const daThayDoi =
                                    String(quaTrinh) !== String(row.diemQuaTrinh ?? "") ||
                                    String(cuoiKy) !== String(row.diemCuoiKy ?? "");
                                const rowErrors = fieldErrors[row.dangKyId] || {};
                                const coLoi = Boolean(rowErrors.diemQuaTrinh || rowErrors.diemCuoiKy || rowErrors.general);
                                return (
                                    <tr key={row.dangKyId} className={`hover:bg-slate-50/60 ${coLoi ? "bg-rose-50/30" : ""}`}>
                                        <td className="td text-center text-slate-500">{index + 1}</td>
                                        <td className="td font-medium text-slate-900">
                                            <button
                                                type="button"
                                                onClick={() => xemHoSoSinhVien(row.sinhVienId)}
                                                className="text-indigo-600 hover:underline font-mono"
                                                title="Xem hồ sơ học tập tối thiểu"
                                            >
                                                {row.maSinhVien}
                                            </button>
                                        </td>
                                        <td className="td">
                                            <div>
                                                <span>{row.hoTen}</span>
                                                {rowErrors.general && (
                                                    <p className="mt-0.5 text-[11px] text-rose-600">{rowErrors.general}</p>
                                                )}
                                            </div>
                                        </td>
                                        <td className="td text-slate-500">{row.email || "—"}</td>
                                        <td className="td text-center">
                                            <button
                                                type="button"
                                                onClick={() => xemHoSoSinhVien(row.sinhVienId)}
                                                className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-indigo-700 bg-indigo-50 hover:bg-indigo-100"
                                                title="Xem hồ sơ đào tạo"
                                            >
                                                <Eye size={13} /> Chi tiết
                                            </button>
                                        </td>
                                        <td className="td">
                                            <div className="flex flex-col items-center">
                                                <input
                                                    className={`input w-24 text-center ${rowErrors.diemQuaTrinh ? "!border-rose-500 !ring-rose-500 bg-rose-50/50" : ""}`}
                                                    type="number"
                                                    min="0"
                                                    max="10"
                                                    step="0.1"
                                                    disabled={biKhoaSua}
                                                    value={quaTrinh}
                                                    onChange={(event) => onChange(row.dangKyId, "diemQuaTrinh", event.target.value)}
                                                />
                                                {rowErrors.diemQuaTrinh && (
                                                    <span className="mt-1 max-w-[130px] text-center text-[10px] font-medium leading-tight text-rose-600">
                                                        {rowErrors.diemQuaTrinh}
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="td">
                                            <div className="flex flex-col items-center">
                                                <input
                                                    className={`input w-24 text-center ${rowErrors.diemCuoiKy ? "!border-rose-500 !ring-rose-500 bg-rose-50/50" : ""}`}
                                                    type="number"
                                                    min="0"
                                                    max="10"
                                                    step="0.1"
                                                    disabled={biKhoaSua}
                                                    value={cuoiKy}
                                                    onChange={(event) => onChange(row.dangKyId, "diemCuoiKy", event.target.value)}
                                                />
                                                {rowErrors.diemCuoiKy && (
                                                    <span className="mt-1 max-w-[130px] text-center text-[10px] font-medium leading-tight text-rose-600">
                                                        {rowErrors.diemCuoiKy}
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="td text-center">
                                            <span className="inline-flex items-center gap-1 font-semibold text-slate-900">
                                                <Calculator size={14} className="text-slate-400" />
                                                {tongKet !== null ? formatDiem(tongKet) : formatDiem(row.diemTongKet)}
                                            </span>
                                        </td>
                                        <td className="td text-right">
                                            <button
                                                type="button"
                                                className={daThayDoi ? "btn-primary px-3 py-1.5 text-xs" : "btn-secondary px-3 py-1.5 text-xs"}
                                                disabled={biKhoaSua || busyId === row.dangKyId || !daThayDoi}
                                                onClick={() => luuDiem(row)}
                                            >
                                                <Save size={14} />
                                                {busyId === row.dangKyId ? "Đang lưu..." : "Lưu"}
                                            </button>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <p className="mt-3 text-xs text-slate-500">
                Điểm hợp lệ từ 0 đến 10 (tối đa 2 chữ số thập phân). Điểm tổng kết = 0.4 × quá trình + 0.6 × cuối kỳ (làm tròn 1 chữ số thập phân). Giảng viên chỉ được nhập/sửa/import điểm khi bảng điểm ở trạng thái Nháp (`NHAP`) hoặc Trả về chỉnh sửa (`TRA_VE`).
            </p>

            {/* Modal lịch sử chuyển trạng thái bảng điểm (QLSV-74) */}
            <Modal
                open={historyModalOpen}
                onClose={() => setHistoryModalOpen(false)}
                title={lop ? `Lịch sử phê duyệt & khóa bảng điểm: ${lop.ma}` : "Lịch sử bảng điểm"}
                description="Mọi lần gửi duyệt, trả sửa, phê duyệt, công bố, khóa và mở khóa đều được ghi nhận kèm người thực hiện, thời điểm và lý do."
                size="lg"
            >
                {lichSu.length === 0 ? (
                    <p className="py-6 text-center text-sm text-slate-500">
                        Chưa có lịch sử chuyển trạng thái nào cho bảng điểm lớp này.
                    </p>
                ) : (
                    <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
                        {lichSu.map((item) => (
                            <div key={item.id} className="py-3 text-xs space-y-1">
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                    <span className="font-semibold text-slate-800">
                                        {ACTION_LABEL[item.hanhDong] || item.hanhDong}:{" "}
                                        <span className="font-mono text-slate-600">{item.trangThaiTruoc}</span> →{" "}
                                        <span className="font-mono text-indigo-700">{item.trangThaiSau}</span>
                                    </span>
                                    <span className="text-slate-500">{formatDateTime(item.thoiDiem)}</span>
                                </div>
                                <p className="text-slate-600">
                                    Người thực hiện:{" "}
                                    <span className="font-medium text-slate-800">
                                        {item.nguoiThucHienUsername || `User #${item.nguoiThucHienId || "—"}`}
                                    </span>{" "}
                                    ({item.nguoiThucHienRole || "—"})
                                </p>
                                {item.lyDo && (
                                    <p className="rounded-md bg-slate-50 px-2.5 py-1.5 text-slate-700 border border-slate-200">
                                        <span className="font-medium">Lý do:</span> {item.lyDo}
                                    </p>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </Modal>

            {/* Modal xem hồ sơ học tập tối thiểu sinh viên (QLSV-67 / TASK-LC-02) */}
            <Modal
                open={profileModalOpen}
                onClose={() => setProfileModalOpen(false)}
                title={
                    studentProfile
                        ? `Hồ sơ học tập: ${studentProfile.hoTen} (${studentProfile.ma})`
                        : "Hồ sơ học tập sinh viên"
                }
                description="Thông tin học vụ tối thiểu phục vụ công tác giảng dạy."
                size="md"
            >
                {profileLoading ? (
                    <Spinner label="Đang tải hồ sơ đào tạo sinh viên..." />
                ) : profileError ? (
                    <Alert tone="error">{profileError}</Alert>
                ) : studentProfile ? (
                    <div className="space-y-4">
                        <dl className="grid grid-cols-1 gap-x-4 gap-y-3 sm:grid-cols-2 text-sm">
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Mã sinh viên</dt>
                                <dd className="mt-1 font-semibold text-slate-900">{studentProfile.ma}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Họ và tên</dt>
                                <dd className="mt-1 font-semibold text-slate-900">{studentProfile.hoTen}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Email học tập</dt>
                                <dd className="mt-1 text-slate-700">{studentProfile.email || "—"}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Khoa</dt>
                                <dd className="mt-1 text-slate-700">{studentProfile.khoaMa} · {studentProfile.khoaTen}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Ngành học</dt>
                                <dd className="mt-1 text-slate-700">{studentProfile.nganhMa} · {studentProfile.nganhTen}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Khóa nhập học</dt>
                                <dd className="mt-1 text-slate-700">{studentProfile.khoaNhapHoc}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Lớp hành chính</dt>
                                <dd className="mt-1 text-slate-700">{studentProfile.lopMa} · {studentProfile.lopTen}</dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3">
                                <dt className="text-xs font-medium text-slate-500">Chương trình đào tạo</dt>
                                <dd className="mt-1 text-slate-700">
                                    {studentProfile.chuongTrinhKhoaNhapHoc !== "Chưa cập nhật"
                                        ? `Khóa ${studentProfile.chuongTrinhKhoaNhapHoc} · Phiên bản ${studentProfile.chuongTrinhPhienBan}`
                                        : "Chưa cập nhật"}
                                </dd>
                            </div>
                            <div className="rounded-lg bg-slate-50 p-3 sm:col-span-2">
                                <dt className="text-xs font-medium text-slate-500">Trạng thái học tập</dt>
                                <dd className="mt-1 text-slate-700 font-medium">{studentProfile.trangThaiHocTap}</dd>
                            </div>
                        </dl>

                        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-500">
                            Chính sách bảo mật: Hồ sơ chỉ hiển thị các trường dữ liệu tối thiểu phục vụ giảng dạy. Tuyệt đối không hiển thị thông tin riêng tư như số điện thoại cá nhân, mật khẩu hoặc CCCD.
                        </div>
                    </div>
                ) : null}
            </Modal>
            {/* Modal gửi thông báo cho lớp (QLSV-68 / TASK-LC-03) */}
            <ClassAnnouncementModal
                open={announcementModalOpen}
                onClose={() => setAnnouncementModalOpen(false)}
                lop={lop}
            />
            {/* Modal Import điểm từ Excel .xlsx (QLSV-73 / TASK-LC-08) */}
            <GradeExcelImportModal
                open={importModalOpen}
                onClose={() => setImportModalOpen(false)}
                lop={lop}
                disabled={biKhoaSua}
                onImportSuccess={async (res) => {
                    setEdits({});
                    setFieldErrors({});
                    setError("");
                    setMessage(
                        `Đã import điểm thành công từ tệp ${res.tenTep}: tạo mới ${res.soTaoMoi}, cập nhật ${res.soCapNhat}, thất bại ${res.soThatBai}.`
                    );
                    await reloadAll();
                }}
            />
        </div>
    );
}
