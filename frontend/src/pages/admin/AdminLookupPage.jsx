import { useEffect, useState } from "react";
import { Search, UserSearch } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";

const KETQUA = { DAT: ["Đạt", "green"], CHUA_DAT: ["Chưa đạt", "red"], CHUA_CO_DIEM: ["Chưa có điểm", "slate"] };
const LOP = { DA_HUY: ["Đã hủy", "red"], DA_KET_THUC: ["Đã kết thúc", "slate"], DANG_MO: ["Đang mở", "green"] };

function Pager({ page, onChange }) {
    if (!page || page.totalPages <= 1) return null;
    return (
        <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm text-slate-600">
            <span>Trang {page.page + 1}/{page.totalPages} · {page.totalElements} bản ghi</span>
            <div className="flex gap-2">
                <button className="btn-secondary" disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>Trước</button>
                <button className="btn-secondary" disabled={page.page + 1 >= page.totalPages} onClick={() => onChange(page.page + 1)}>Sau</button>
            </div>
        </div>
    );
}

export default function AdminLookupPage() {
    const [kind, setKind] = useState("sinh-vien");
    const [keyword, setKeyword] = useState("");
    const [submitted, setSubmitted] = useState("");
    const [pageNo, setPageNo] = useState(0);
    const [list, setList] = useState(null);
    const [listLoading, setListLoading] = useState(false);
    const [error, setError] = useState("");

    const [selected, setSelected] = useState(null);
    const [mucDich, setMucDich] = useState("");
    const [hocKyId, setHocKyId] = useState("");
    const [trangThai, setTrangThai] = useState("");
    const [detailPage, setDetailPage] = useState(0);
    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const [semesters, setSemesters] = useState([]);
    const [request, setRequest] = useState(null); // tham số đã xác nhận, chỉ gọi API khi bấm "Xem"

    useEffect(() => {
        api.get("/admin/hoc-ky").then((r) => setSemesters(r.data || [])).catch(() => setSemesters([]));
    }, []);

    useEffect(() => {
        let active = true;
        setListLoading(true);
        setError("");
        api.get(`/admin/tra-cuu/${kind}`, { params: { keyword: submitted || undefined, page: pageNo, size: 15 } })
            .then((r) => active && setList(r.data))
            .catch((e) => active && setError(getErrorMessage(e)))
            .finally(() => active && setListLoading(false));
        return () => { active = false; };
    }, [kind, submitted, pageNo]);

    useEffect(() => {
        if (!request) return undefined;
        let active = true;
        setDetailLoading(true);
        setDetailError("");
        const params = { mucDich: request.mucDich, page: detailPage, size: 20 };
        if (request.hocKyId) params.hocKyId = request.hocKyId;
        if (kind === "sinh-vien" && request.trangThai) params.trangThai = request.trangThai;
        api.get(`/admin/tra-cuu/${kind}/${request.id}/lich-su`, { params })
            .then((r) => active && setDetail(r.data))
            .catch((e) => { if (active) { setDetail(null); setDetailError(getErrorMessage(e)); } })
            .finally(() => active && setDetailLoading(false));
        return () => { active = false; };
    }, [request, detailPage, kind]);

    const switchKind = (next) => {
        setKind(next); setKeyword(""); setSubmitted(""); setPageNo(0);
        setSelected(null); setDetail(null); setRequest(null); setDetailError("");
    };
    const pick = (row) => {
        setSelected(row); setDetail(null); setRequest(null); setDetailError(""); setDetailPage(0);
    };
    const view = (event) => {
        event.preventDefault();
        if (mucDich.trim().length < 5) {
            setDetailError("Vui lòng nhập mục đích nghiệp vụ (ít nhất 5 ký tự). Thao tác xem được ghi nhật ký.");
            return;
        }
        setDetailPage(0);
        setRequest({ id: selected.id, mucDich: mucDich.trim(), hocKyId, trangThai });
    };

    const rows = detail ? (kind === "sinh-vien" ? detail.lichSu : detail.lopGiangDay) : null;

    return (
        <div className="space-y-6">
            <PageHeader icon={UserSearch} title="Tra cứu lịch sử học tập & giảng dạy"
                        description="Tra cứu hồ sơ sinh viên/giảng viên theo mã hoặc tên. Mỗi lần xem chi tiết đều được ghi audit kèm mục đích nghiệp vụ." />

            <div className="flex gap-2">
                {[["sinh-vien", "Sinh viên"], ["giang-vien", "Giảng viên"]].map(([k, label]) => (
                    <button key={k} onClick={() => switchKind(k)} className={kind === k ? "btn-primary" : "btn-secondary"}>{label}</button>
                ))}
            </div>

            <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); setPageNo(0); setSubmitted(keyword.trim()); }}>
                <input className="input max-w-sm" placeholder="Nhập mã hoặc họ tên" value={keyword} maxLength={100}
                       onChange={(e) => setKeyword(e.target.value)} />
                <button className="btn-primary" type="submit"><Search size={16} /> Tìm</button>
            </form>

            {error && <Alert tone="error">{error}</Alert>}

            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
                <div className="card overflow-hidden">
                    {listLoading ? <Spinner /> : !list || list.items.length === 0 ? (
                        <EmptyState icon={Search} title="Không có kết quả" description="Thử mã hoặc tên khác." />
                    ) : (
                        <>
                            <ul className="divide-y divide-slate-100">
                                {list.items.map((row) => (
                                    <li key={row.id}>
                                        <button onClick={() => pick(row)}
                                                className={`w-full px-4 py-3 text-left hover:bg-slate-50 ${selected?.id === row.id ? "bg-slate-50" : ""}`}>
                                            <p className="font-medium text-slate-800">{row.hoTen}</p>
                                            <p className="text-xs text-slate-500">{row.ma} · {row.khoa || "—"}{!row.dangHoatDong && " · Đã khóa"}</p>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                            <Pager page={list} onChange={setPageNo} />
                        </>
                    )}
                </div>

                <div className="card overflow-hidden">
                    {!selected ? (
                        <EmptyState icon={UserSearch} title="Chọn một hồ sơ" description="Chọn sinh viên/giảng viên ở danh sách bên trái." />
                    ) : (
                        <div className="space-y-4 p-4">
                            <p className="font-semibold text-slate-800">{selected.hoTen} <span className="text-slate-500">({selected.ma})</span></p>
                            <form className="grid gap-2 sm:grid-cols-2" onSubmit={view}>
                                <input className="input sm:col-span-2" placeholder="Mục đích nghiệp vụ (bắt buộc, ví dụ: Xác minh hồ sơ xét tốt nghiệp)"
                                       value={mucDich} maxLength={200} onChange={(e) => setMucDich(e.target.value)} />
                                <select className="input" value={hocKyId} onChange={(e) => setHocKyId(e.target.value)}>
                                    <option value="">Tất cả học kỳ</option>
                                    {semesters.map((s) => <option key={s.id} value={s.id}>{s.ten} · {s.namHoc}</option>)}
                                </select>
                                {kind === "sinh-vien" && (
                                    <select className="input" value={trangThai} onChange={(e) => setTrangThai(e.target.value)}>
                                        <option value="">Mọi kết quả</option>
                                        <option value="DAT">Đạt</option>
                                        <option value="CHUA_DAT">Chưa đạt</option>
                                        <option value="CHUA_CO_DIEM">Chưa có điểm</option>
                                    </select>
                                )}
                                <button className="btn-primary sm:col-span-2" type="submit">Xem lịch sử (ghi audit)</button>
                            </form>

                            {detailError && <Alert tone="error">{detailError}</Alert>}
                            {detailLoading && <Spinner />}
                            {!detailLoading && rows && rows.items.length === 0 && (
                                <EmptyState title="Chưa có dữ liệu lịch sử" description="Không có bản ghi phù hợp bộ lọc." />
                            )}
                            {!detailLoading && rows && rows.items.length > 0 && (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead className="text-xs uppercase text-slate-500">
                                        {kind === "sinh-vien" ? (
                                            <tr><th className="p-2">Học kỳ</th><th className="p-2">Môn</th><th className="p-2">Lớp</th><th className="p-2">Điểm</th><th className="p-2">Trạng thái</th></tr>
                                        ) : (
                                            <tr><th className="p-2">Học kỳ</th><th className="p-2">Lớp</th><th className="p-2">Môn</th><th className="p-2">Sĩ số</th><th className="p-2">Trạng thái</th></tr>
                                        )}
                                        </thead>
                                        <tbody className="divide-y divide-slate-100">
                                        {rows.items.map((r) => kind === "sinh-vien" ? (
                                            <tr key={r.dangKyId}>
                                                <td className="p-2">{r.tenHocKy}</td>
                                                <td className="p-2">{r.maMon} – {r.tenMon}</td>
                                                <td className="p-2">{r.maLop}{r.lopDaHuy && <Badge tone="red">Lớp đã hủy</Badge>}</td>
                                                <td className="p-2">{r.diemTongKet ?? "—"}</td>
                                                <td className="p-2">
                                                    {r.trangThaiDangKy === "DA_HUY" ? <Badge tone="red">Đã hủy đăng ký</Badge>
                                                        : <Badge tone={KETQUA[r.trangThaiKetQua][1]}>{KETQUA[r.trangThaiKetQua][0]}</Badge>}
                                                </td>
                                            </tr>
                                        ) : (
                                            <tr key={r.lopHocPhanId}>
                                                <td className="p-2">{r.tenHocKy}</td>
                                                <td className="p-2">{r.maLop}</td>
                                                <td className="p-2">{r.maMon} – {r.tenMon}</td>
                                                <td className="p-2">{r.soSinhVienDangHoc}/{r.siSoToiDa}</td>
                                                <td className="p-2"><Badge tone={LOP[r.trangThai][1]}>{LOP[r.trangThai][0]}</Badge></td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                    <Pager page={rows} onChange={setDetailPage} />
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
