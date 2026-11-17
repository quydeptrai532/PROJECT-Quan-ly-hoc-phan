import { useEffect, useState } from "react";
import { History } from "lucide-react";
import { Alert, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDateTime } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const PAGE_SIZE = 20;

export default function RegistrationHistoryPage() {
  const { data: semesters } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");
  const [page, setPage] = useState(0);
  useEffect(() => {
    if (!hocKyId && semesters?.length) setHocKyId(String(semesters[0].id));
  }, [semesters, hocKyId]);

  const { data, loading, error } = useApi(
    hocKyId ? "/dang-ky/me/lich-su" : null,
    hocKyId ? { hocKyId, page, size: PAGE_SIZE } : undefined
  );
  const items = data?.content || [];

  return (
    <div>
      <PageHeader icon={History} title="Lịch sử đăng ký" description="Tra cứu các lần đăng ký và hủy học phần theo học kỳ." actions={
        <select className="input w-auto min-w-[220px]" value={hocKyId} onChange={(event) => { setHocKyId(event.target.value); setPage(0); }}>
          {(semesters || []).map((semester) => <option key={semester.id} value={semester.id}>{semester.ten} - {semester.namHoc}</option>)}
        </select>
      } />
      {error && <Alert tone="error">{error}</Alert>}
      <div className="card mt-4 overflow-hidden">
        {loading ? <Spinner /> : items.length === 0 ? (
          <EmptyState icon={History} title="Chưa có lịch sử đăng ký" description="Các lần đăng ký hoặc hủy trong học kỳ được chọn sẽ hiển thị tại đây." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[850px]">
              <thead className="bg-slate-50"><tr>
                <th className="th">Môn học</th><th className="th">Mã lớp</th><th className="th">Thời điểm đăng ký</th>
                <th className="th">Trạng thái</th><th className="th">Thời điểm hủy</th><th className="th">Lý do hủy</th>
              </tr></thead>
              <tbody className="divide-y divide-slate-100">{items.map((item) => (
                <tr key={item.id}>
                  <td className="td"><p className="font-medium">{item.tenMon}</p><p className="text-xs text-slate-500">{item.maMon}</p></td>
                  <td className="td">{item.maLop}</td><td className="td">{formatDateTime(item.thoiGianDangKy)}</td>
                  <td className="td"><span className={`rounded-full px-2 py-1 text-xs font-medium ${item.trangThai === "DA_HUY" ? "bg-rose-50 text-rose-700" : "bg-emerald-50 text-emerald-700"}`}>
                    {item.trangThai === "DA_HUY" ? "Đã hủy" : "Đã đăng ký"}
                  </span></td>
                  <td className="td">{item.thoiGianHuy ? formatDateTime(item.thoiGianHuy) : "—"}</td>
                  <td className="td">{item.lyDoHuy || "—"}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </div>
      {data && data.totalPages > 1 && <div className="mt-4 flex items-center justify-between">
        <p className="text-sm text-slate-500">Trang {data.number + 1}/{data.totalPages} · {data.totalElements} sự kiện</p>
        <div className="flex gap-2"><button className="btn-secondary" disabled={data.first} onClick={() => setPage((current) => current - 1)}>Trước</button>
          <button className="btn-secondary" disabled={data.last} onClick={() => setPage((current) => current + 1)}>Tiếp</button></div>
      </div>}
    </div>
  );
}
