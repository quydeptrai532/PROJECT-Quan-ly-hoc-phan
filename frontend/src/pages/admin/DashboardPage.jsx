import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BookMarked,
  CalendarRange,
  ClipboardCheck,
  Layers,
  LayoutDashboard,
  UserCog,
  UserRound,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import GradeAnalyticsPanel from "../../components/GradeAnalyticsPanel";
import { Alert, Badge, PageHeader, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useApi } from "../../lib/useApi";

export default function DashboardPage() {
  const { user } = useAuth();
  const { data: hocKyList } = useApi("/hoc-ky");
  const { data: khoaList } = useApi("/admin/khoa");
  const [hocKyId, setHocKyId] = useState("");
  const [khoaId, setKhoaId] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const { data: tongQuan } = useApi("/admin/thong-ke/tong-quan");
  const params = hocKyId ? { hocKyId, ...(khoaId ? { khoaId } : {}) } : undefined;
  const { data: theoMon, loading, error } = useApi("/admin/thong-ke/theo-mon", params);
  const hanhDongParams = hocKyId ? { hocKyId, ...(khoaId ? { khoaId } : {}) } : undefined;
  const { data: hanhDong, loading: hanhDongLoading, error: hanhDongError } =
    useApi("/admin/thong-ke/hanh-dong", hanhDongParams);

  const hocKy = hocKyList?.find((item) => String(item.id) === String(hocKyId));
  const tongDangKy = useMemo(
    () => (theoMon || []).reduce((sum, item) => sum + item.daDangKy, 0),
    [theoMon]
  );
  const tongChiTieu = useMemo(
    () => (theoMon || []).reduce((sum, item) => sum + item.tongChiTieu, 0),
    [theoMon]
  );
  const tiLeLapDay = tongChiTieu ? Math.round((tongDangKy / tongChiTieu) * 1000) / 10 : 0;
  const conTrong = Math.max(0, tongChiTieu - tongDangKy);
  const pieData = [
    { name: "Đã đăng ký", value: tongDangKy },
    { name: "Còn trống", value: conTrong },
  ];
  const chartData = (theoMon || []).map((item) => ({
    name: item.maMon,
    "Đã đăng ký": item.daDangKy,
    "Chỉ tiêu": item.tongChiTieu,
  }));

  const CHART_TOOLTIP = {
    borderRadius: 12,
    border: "1px solid #e2e8f0",
    fontSize: 12,
    boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
  };

  return (
    <div>
      <PageHeader
        icon={LayoutDashboard}
        title="Tổng quan đào tạo"
        description="Thống kê nhanh tình hình đăng ký học phần và quy mô đào tạo."
        actions={
          <div className="flex flex-wrap gap-2">
          <select
            className="input w-auto min-w-[220px]"
            value={hocKyId}
            onChange={(event) => setHocKyId(event.target.value)}
          >
            {(hocKyList || []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten} - {item.namHoc}
              </option>
            ))}
          </select>
          <select
            className="input w-auto min-w-[180px]"
            value={khoaId}
            onChange={(event) => setKhoaId(event.target.value)}
            aria-label="Lọc theo khoa"
          >
            <option value="">Tất cả khoa</option>
            {(khoaList || []).map((item) => (
              <option key={item.id} value={item.id}>{item.ten}</option>
            ))}
          </select>
          </div>
        }
      />

      <div className="dashboard-hero page-enter relative mb-6 overflow-hidden rounded-[24px] border border-[#dce6d5] p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-10 -top-16 h-56 w-56 rounded-full bg-indigo-500/25 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-20 right-32 h-48 w-48 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="relative flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="hero-muted text-[11px] font-semibold uppercase tracking-[0.2em]">
              Bảng điều khiển học vụ
            </p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight">
              Xin chào, {user?.hoTen || user?.username}
            </h2>
            <p className="hero-muted mt-1.5 max-w-xl text-sm leading-relaxed">
              Theo dõi tình hình mở lớp, đăng ký học phần và kết quả vận hành trong học kỳ hiện tại.
            </p>
          </div>
          <div className="flex items-center gap-3 rounded-xl border border-white/80 bg-white/70 px-4 py-3">
            <span className="grid h-10 w-10 place-items-center rounded-lg bg-[#edf3e4] text-[#58734f]">
              <CalendarRange size={20} />
            </span>
            <div>
              <p className="hero-muted text-[10px] font-semibold uppercase tracking-wider">
                Học kỳ hiện tại
              </p>
              <p className="text-sm font-medium">{tongQuan?.hocKyHienTai || "—"}</p>
            </div>
          </div>
        </div>
      </div>

      <section className="card mb-6 overflow-hidden" aria-labelledby="action-list-title">
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-4">
          <div>
            <h2 id="action-list-title" className="text-base font-semibold text-slate-900">Việc cần giáo vụ chú ý</h2>
            <p className="mt-1 text-xs text-slate-500">
              Quy tắc: lớp gần đầy từ 90%; ít đăng ký không quá 25%; nhu cầu môn khi đăng ký đạt tổng chỉ tiêu; đợt đóng trong 7 ngày.
            </p>
          </div>
          <p className="text-xs text-slate-500" aria-live="polite">
            {hanhDong?.capNhatLuc ? `Dữ liệu truy vấn lúc ${new Date(hanhDong.capNhatLuc).toLocaleString("vi-VN")}` : ""}
          </p>
        </div>
        {hanhDongError ? (
          <div className="p-5"><Alert tone="error">Không tải được danh sách việc cần chú ý: {hanhDongError}</Alert></div>
        ) : hanhDongLoading ? (
          <Spinner label="Đang tổng hợp đăng ký theo học kỳ..." />
        ) : !hanhDong?.hocKyId ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">Chưa có học kỳ để phân tích.</div>
        ) : hanhDong.canhBao.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">
            Không có mục cần chú ý theo các quy tắc hiển thị cho {hanhDong.tenHocKy}{hanhDong.tenKhoa ? ` · ${hanhDong.tenKhoa}` : ""}.
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {hanhDong.canhBao.map((item, index) => (
              <li key={`${item.loai}-${item.lopHocPhanId || item.monHocId || item.hocKyId}-${index}`} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">{item.tieuDe}</span>
                    <Badge tone={item.loai === "LOP_GAN_DAY" || item.loai === "MON_HET_CHO" ? "amber" : "slate"}>
                      {item.loai === "LOP_GAN_DAY" ? "Gần đầy" : item.loai === "LOP_IT_DANG_KY" ? "Ít đăng ký" : item.loai === "MON_HET_CHO" ? "Đạt chỉ tiêu" : "Sắp đóng"}
                    </Badge>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{item.lyDo}</p>
                  <p className="mt-1 text-xs text-slate-400">{item.mucTinh} · {item.tenHocKy}</p>
                  {(item.maLop || item.maMon) && (
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      {item.maLop ? `Lớp ${item.maLop} · ` : ""}{item.maMon ? `${item.maMon} · ${item.tenMon}` : ""}
                      {item.soLop > 1 ? ` · ${item.soLop} lớp` : ""}
                    </p>
                  )}
                </div>
                <Link to={item.duongDan} className="btn-secondary shrink-0 self-start sm:self-center">
                  Xem dữ liệu nguồn
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard label="Sinh viên" value={tongQuan?.soSinhVien ?? "—"} icon={UserRound} />
        <StatCard label="Giảng viên" value={tongQuan?.soGiangVien ?? "—"} icon={UserCog} tone="emerald" />
        <StatCard label="Môn học" value={tongQuan?.soMonHoc ?? "—"} icon={BookMarked} tone="amber" />
        <StatCard label="Lớp học phần" value={tongQuan?.soLopHocPhan ?? "—"} icon={Layers} tone="sky" />
        <StatCard label="Lượt đăng ký" value={tongQuan?.soDangKy ?? "—"} icon={ClipboardCheck} tone="rose" />
      </div>

      <div className="mb-6 grid gap-6 xl:grid-cols-3">
        <section className="card p-5 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Đăng ký theo môn học</h2>
              <p className="mt-0.5 text-xs text-slate-500">So sánh số lượng đăng ký với chỉ tiêu mở lớp</p>
            </div>
            <Badge tone="indigo">{theoMon?.length || 0} môn</Badge>
          </div>

          {loading ? (
            <Spinner />
          ) : chartData.length === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">Chưa có dữ liệu trong học kỳ này.</div>
          ) : (
            <div className="mt-4 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <RechartsTooltip contentStyle={CHART_TOOLTIP} cursor={{ fill: "rgba(99, 102, 241, 0.06)" }} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="Đã đăng ký" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={30} isAnimationActive={false} />
                  <Bar dataKey="Chỉ tiêu" fill="#cbd5e1" radius={[6, 6, 0, 0]} maxBarSize={30} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="card p-5">
          <div>
            <h2 className="text-sm font-semibold text-slate-800">Tỷ lệ lấp đầy</h2>
            <p className="mt-0.5 text-xs text-slate-500">Toàn bộ lớp trong học kỳ</p>
          </div>

          {loading ? (
            <Spinner />
          ) : tongChiTieu === 0 ? (
            <div className="py-12 text-center text-sm text-slate-500">Chưa có dữ liệu.</div>
          ) : (
            <>
              <div className="relative mt-4 h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={64}
                      outerRadius={90}
                      paddingAngle={3}
                      startAngle={90}
                      endAngle={-270}
                      strokeWidth={0}
                      isAnimationActive={false}
                    >
                      <Cell fill="#6366f1" />
                      <Cell fill="#e2e8f0" />
                    </Pie>
                    <RechartsTooltip contentStyle={CHART_TOOLTIP} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <p className="text-2xl font-semibold text-slate-900">{tiLeLapDay}%</p>
                  <p className="text-[11px] uppercase tracking-wider text-slate-400">Đã lấp đầy</p>
                </div>
              </div>
              <div className="mt-2 grid grid-cols-2 gap-3 text-center">
                <div className="rounded-xl bg-indigo-50 px-3 py-2">
                  <p className="text-lg font-semibold text-indigo-700">{tongDangKy}</p>
                  <p className="text-[11px] text-indigo-500">Đã đăng ký</p>
                </div>
                <div className="rounded-xl bg-slate-50 px-3 py-2">
                  <p className="text-lg font-semibold text-slate-700">{conTrong}</p>
                  <p className="text-[11px] text-slate-500">Còn trống</p>
                </div>
              </div>
            </>
          )}
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="card overflow-hidden xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Thống kê theo môn học</h2>
              <p className="text-xs text-slate-500">
                {hocKy ? `${hocKy.ten} - ${hocKy.namHoc}` : "Chọn học kỳ"} · {tongDangKy} lượt đăng ký
              </p>
            </div>
            <Badge tone="indigo">{theoMon?.length || 0} môn</Badge>
          </div>

          {error && <Alert tone="error" className="m-4">{error}</Alert>}
          {loading ? (
            <Spinner />
          ) : (theoMon || []).length === 0 ? (
            <div className="px-4 py-10 text-center text-sm text-slate-500">
              Chưa có lớp học phần trong học kỳ này.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px]">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="th">Môn học</th>
                    <th className="th text-center">Số lớp</th>
                    <th className="th text-center">Chỉ tiêu</th>
                    <th className="th text-center">Đã đăng ký</th>
                    <th className="th">Tỷ lệ lấp đầy</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {theoMon.map((item) => {
                    const tone =
                      item.tiLeLapDay >= 90
                        ? "from-rose-500 to-pink-500"
                        : item.tiLeLapDay >= 60
                        ? "from-amber-400 to-orange-500"
                        : "from-emerald-400 to-teal-500";
                    return (
                      <tr key={item.monHocId} className="hover:bg-slate-50/60">
                        <td className="td">
                          <p className="font-medium text-slate-800">{item.tenMon}</p>
                          <p className="text-xs text-slate-500">{item.maMon}</p>
                        </td>
                        <td className="td text-center">{item.soLop}</td>
                        <td className="td text-center">{item.tongChiTieu}</td>
                        <td className="td text-center">{item.daDangKy}</td>
                        <td className="td">
                          <div className="flex items-center gap-2">
                            <div className="h-1.5 w-28 overflow-hidden rounded-full bg-slate-200">
                              <div className={`h-full rounded-full bg-gradient-to-r ${tone}`} style={{ width: `${Math.min(100, item.tiLeLapDay)}%` }} />
                            </div>
                            <span className="text-xs text-slate-600">{item.tiLeLapDay}%</span>
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

        <div className="space-y-4">
          <div className="card p-5">
            <h2 className="text-sm font-semibold text-slate-800">Học kỳ hiện tại</h2>
            <p className="mt-1 text-sm text-slate-600">{tongQuan?.hocKyHienTai || "—"}</p>
            {hocKy && (
              <div className="mt-3">
                <Badge tone={hocKy.dangMoDangKy ? "green" : "slate"}>
                  {hocKy.dangMoDangKy ? "Đang mở đăng ký" : "Đã đóng đăng ký"}
                </Badge>
                <p className="mt-2 text-xs text-slate-500">
                  Giới hạn {hocKy.soTinChiToiThieu} - {hocKy.soTinChiToiDa} tín chỉ
                </p>
              </div>
            )}
          </div>

          <div className="card p-5">
            <h2 className="text-sm font-semibold text-slate-800">Thao tác nhanh</h2>
            <div className="mt-3 space-y-2">
              <Link to="/quan-tri/lop-hoc-phan" className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:border-indigo-300 hover:bg-indigo-50">
                Mở lớp học phần
                <Layers size={16} className="text-slate-400" />
              </Link>
              <Link to="/quan-tri/sinh-vien" className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:border-indigo-300 hover:bg-indigo-50">
                Quản lý sinh viên
                <UserRound size={16} className="text-slate-400" />
              </Link>
              <Link to="/quan-tri/hoc-ky" className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:border-indigo-300 hover:bg-indigo-50">
                Cấu hình đợt đăng ký
                <ClipboardCheck size={16} className="text-slate-400" />
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Thống kê điểm lớp học phần và phân bố điểm (QLSV-75 / TASK-LC-10) */}
      <div className="mt-6">
        <GradeAnalyticsPanel
          mode="admin"
          initialHocKyId={hocKyId}
          initialKhoaId={khoaId}
        />
      </div>
    </div>
  );
}
