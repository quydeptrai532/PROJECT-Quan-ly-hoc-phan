import { useEffect, useState } from "react";
import { History, Search, ShieldCheck } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";

const ACTIONS = {
  COURSE_CREATED: "Tạo môn học",
  COURSE_UPDATED: "Cập nhật môn học",
  COURSE_DELETED: "Xóa môn học",
  PREREQUISITE_ADDED: "Thêm môn tiên quyết",
  PREREQUISITE_REMOVED: "Xóa môn tiên quyết",
  COURSE_SECTION_CREATED: "Tạo lớp học phần",
  COURSE_SECTION_UPDATED: "Cập nhật lớp học phần",
  COURSE_SECTION_CANCELLED: "Hủy lớp học phần",
  COURSE_SECTION_DELETED: "Xóa lớp học phần",
  GRADE_SHEET_SUBMITTED: "Gửi duyệt bảng điểm",
  GRADE_SHEET_RETURNED: "Trả về chỉnh sửa bảng điểm",
  GRADE_SHEET_APPROVED: "Phê duyệt bảng điểm",
  GRADE_SHEET_PUBLISHED: "Công bố bảng điểm",
  GRADE_SHEET_LOCKED: "Khóa bảng điểm",
  GRADE_SHEET_UNLOCKED: "Mở khóa bảng điểm",
  GRADE_SHEET_PUBLICATION_REVOKED: "Thu hồi công bố bảng điểm",
  GRADE_IMPORTED_XLSX: "Import điểm từ Excel (.xlsx)",
  REGISTRATION_WINDOW_CREATED: "Tạo đợt đăng ký",
  REGISTRATION_WINDOW_UPDATED: "Cập nhật đợt đăng ký",
  REGISTRATION_WINDOW_DELETED: "Xóa cấu hình đợt đăng ký",
  STUDENT_ACCOUNT_STATUS_CHANGED: "Đổi trạng thái tài khoản sinh viên",
  LECTURER_ACCOUNT_STATUS_CHANGED: "Đổi trạng thái tài khoản giảng viên",
  STUDENT_ACCOUNT_CREATED: "Tạo tài khoản sinh viên",
  STUDENT_ACCOUNT_UPDATED: "Cập nhật tài khoản sinh viên",
  LECTURER_ACCOUNT_CREATED: "Tạo tài khoản giảng viên",
  LECTURER_ACCOUNT_UPDATED: "Cập nhật tài khoản giảng viên",
  COURSE_REGISTRATION: "Đăng ký học phần",
  COURSE_REGISTRATION_CANCELLED: "Hủy đăng ký học phần",
};

const FIELDS = {
  maLop: "Mã lớp",
  monHocId: "Môn học ID",
  hocKyId: "Học kỳ ID",
  giangVienId: "Giảng viên ID",
  phong: "Phòng",
  thu: "Thứ",
  tietBatDau: "Tiết bắt đầu",
  soTiet: "Số tiết",
  siSoToiDa: "Sĩ số tối đa",
  siSoHienTai: "Sĩ số hiện tại",
  conLai: "Chỗ còn lại",
  tenHocKy: "Học kỳ",
  namHoc: "Năm học",
  dangKyTu: "Mở đăng ký",
  dangKyDen: "Đóng đăng ký",
  soTinChiToiThieu: "Tín chỉ tối thiểu",
  soTinChiToiDa: "Tín chỉ tối đa",
  trangThai: "Trạng thái",
  trangThaiCu: "Trạng thái trước",
  trangThaiMoi: "Trạng thái mới",
  trangThaiBangDiem: "Trạng thái bảng điểm",
  phienBanCongBo: "Phiên bản công bố",
  phienBanBangDiem: "Phiên bản bảng điểm",
  choPhepSuaDiem: "Cho phép sửa điểm",
  hanhDong: "Thao tác chuyển trạng thái",
  lyDo: "Lý do",
  lyDoTraVe: "Lý do trả về",
  lyDoMoKhoa: "Lý do mở khóa",
  nguoiThucHienId: "ID người thực hiện",
  soSinhVienDangHoc: "Số SV đang học",
  soSinhVienDuDieuKien: "Số SV đủ điều kiện",
  dangKyId: "Đăng ký ID",
  lopHocPhanId: "Lớp học phần ID",
  active: "Tài khoản hoạt động",
  maMon: "Mã môn học",
  tenMon: "Tên môn học",
  soTinChi: "Số tín chỉ",
  khoaId: "Khoa ID",
  monTienQuyetId: "Môn tiên quyết ID",
};

const ENTITY_TYPES = ["COURSE", "COURSE_PREREQUISITE", "COURSE_SECTION", "GRADE_SHEET", "SEMESTER", "STUDENT_ACCOUNT", "LECTURER_ACCOUNT", "COURSE_REGISTRATION"];

function displayValue(value) {
  if (typeof value === "boolean") return value ? "Có" : "Không";
  return String(value ?? "—");
}

function DetailChanges({ details }) {
  const before = details?.before || {};
  const after = details?.after || {};
  const keys = [...new Set([...Object.keys(before), ...Object.keys(after)])];
  if (!keys.length && !details?.errorCode) return <span className="text-slate-400">—</span>;

  return (
    <div className="min-w-56 space-y-1 text-xs">
      {keys.map((key) => (
        <div key={key}>
          <span className="font-medium text-slate-600">{FIELDS[key] || key}: </span>
          <span className="text-slate-500">{key in before ? displayValue(before[key]) : "∅"}</span>
          <span className="px-1 text-slate-400">→</span>
          <span className="text-slate-800">{key in after ? displayValue(after[key]) : "∅"}</span>
        </div>
      ))}
      {details?.errorCode && <div className="font-medium text-rose-700">Kết quả lỗi: {details.errorCode}</div>}
    </div>
  );
}

export default function AuditLogPage() {
  const [filters, setFilters] = useState({ from: "", to: "", actor: "", action: "", entityType: "", entityId: "" });
  const [submittedFilters, setSubmittedFilters] = useState(filters);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function loadAuditLogs() {
      setLoading(true);
      setError("");
      const params = { page: pageNumber, size: 25 };
      if (submittedFilters.from) params.from = `${submittedFilters.from}T00:00:00`;
      if (submittedFilters.to) params.to = `${submittedFilters.to}T23:59:59`;
      for (const key of ["actor", "action", "entityType", "entityId"]) {
        if (submittedFilters[key]) params[key] = submittedFilters[key];
      }
      try {
        const response = await api.get("/admin/audit-logs", { params });
        if (active) setPage(response.data);
      } catch (requestError) {
        if (active) setError(getErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }
    loadAuditLogs();
    return () => { active = false; };
  }, [pageNumber, submittedFilters]);

  const changeFilter = (event) => setFilters((current) => ({ ...current, [event.target.name]: event.target.value }));
  const submitFilters = (event) => {
    event.preventDefault();
    setPageNumber(0);
    setSubmittedFilters({ ...filters });
  };
  const clearFilters = () => {
    const cleared = { from: "", to: "", actor: "", action: "", entityType: "", entityId: "" };
    setFilters(cleared);
    setSubmittedFilters(cleared);
    setPageNumber(0);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        icon={ShieldCheck}
        title="Nhật ký hoạt động"
        description="Tra cứu dấu vết các thay đổi quản trị và thao tác đăng ký. Nhật ký chỉ được tạo bởi máy chủ."
      />

      <form onSubmit={submitFilters} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <label className="text-xs font-medium text-slate-600">Từ ngày
            <input name="from" type="date" value={filters.from} onChange={changeFilter} className="input mt-1 w-full" />
          </label>
          <label className="text-xs font-medium text-slate-600">Đến ngày
            <input name="to" type="date" value={filters.to} onChange={changeFilter} className="input mt-1 w-full" />
          </label>
          <label className="text-xs font-medium text-slate-600">Người thực hiện
            <input name="actor" value={filters.actor} onChange={changeFilter} placeholder="Tên đăng nhập" className="input mt-1 w-full" />
          </label>
          <label className="text-xs font-medium text-slate-600">Hành động
            <select name="action" value={filters.action} onChange={changeFilter} className="input mt-1 w-full">
              <option value="">Tất cả</option>
              {Object.entries(ACTIONS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <label className="text-xs font-medium text-slate-600">Loại đối tượng
            <select name="entityType" value={filters.entityType} onChange={changeFilter} className="input mt-1 w-full">
              <option value="">Tất cả</option>
              {ENTITY_TYPES.map((value) => <option key={value} value={value}>{value}</option>)}
            </select>
          </label>
          <label className="text-xs font-medium text-slate-600">ID đối tượng
            <input name="entityId" value={filters.entityId} onChange={changeFilter} placeholder="ID" className="input mt-1 w-full" />
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button type="submit" className="btn-primary"><Search size={16} /> Lọc nhật ký</button>
          <button type="button" className="btn-secondary" onClick={clearFilters}>Xóa bộ lọc</button>
        </div>
      </form>

      {error && <Alert tone="error">Không tải được nhật ký: {error}</Alert>}
      {loading ? <Spinner label="Đang tải nhật ký..." /> : !error && page?.content?.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <EmptyState icon={History} title="Chưa có bản ghi phù hợp" description="Thử nới rộng bộ lọc hoặc kiểm tra khoảng thời gian." />
        </div>
      ) : !error && page && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
            <div>
              <h2 className="font-semibold text-slate-900">Các sự kiện đã ghi</h2>
              <p className="mt-1 text-xs text-slate-500">{page.totalElements} bản ghi · mới nhất trước</p>
            </div>
            <Badge tone="indigo">Chỉ đọc</Badge>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-[1100px] w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Thời điểm</th><th className="px-4 py-3">Người thực hiện</th>
                  <th className="px-4 py-3">Hành động</th><th className="px-4 py-3">Đối tượng</th>
                  <th className="px-4 py-3">Kết quả</th><th className="px-4 py-3">Thay đổi</th>
                  <th className="px-4 py-3">Correlation ID</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {page.content.map((row) => (
                  <tr key={row.id} className="align-top hover:bg-slate-50/70">
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-600">{new Date(row.occurredAt).toLocaleString("vi-VN")}</td>
                    <td className="px-4 py-3"><p className="font-medium text-slate-800">{row.actorUsername}</p><p className="text-xs text-slate-500">{row.actorRole}</p></td>
                    <td className="px-4 py-3 text-slate-700">{ACTIONS[row.action] || row.action}</td>
                    <td className="px-4 py-3"><p className="text-xs text-slate-500">{row.entityType}</p><p className="font-mono text-xs text-slate-700">#{row.entityId || "—"}</p></td>
                    <td className="px-4 py-3"><Badge tone={row.result === "SUCCESS" ? "green" : "red"}>{row.result === "SUCCESS" ? "Thành công" : "Thất bại"}</Badge></td>
                    <td className="px-4 py-3"><DetailChanges details={row.details} /></td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-500">{row.correlationId || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <span className="text-xs text-slate-500">Trang {page.number + 1} / {Math.max(page.totalPages, 1)}</span>
            <div className="flex gap-2">
              <button type="button" className="btn-secondary" disabled={page.first} onClick={() => setPageNumber((current) => Math.max(0, current - 1))}>Trước</button>
              <button type="button" className="btn-secondary" disabled={page.last} onClick={() => setPageNumber((current) => current + 1)}>Sau</button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
