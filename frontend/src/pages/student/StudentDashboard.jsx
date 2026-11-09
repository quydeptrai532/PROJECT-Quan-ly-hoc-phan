import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  ClipboardList,
  Clock,
  GraduationCap,
  MapPin,
} from "lucide-react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Alert, Badge, EmptyState, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { formatDate, formatDateTime, formatDiem, lichHoc, THU_LABEL } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const CHART_TOOLTIP = {
  borderRadius: 12,
  border: "1px solid #e2e8f0",
  fontSize: 12,
  boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
};

export default function StudentDashboard() {
  const { user } = useAuth();
  const { data: hocKyList } = useApi("/hoc-ky");
  const { data: dangKy, loading: loadingDangKy } = useApi("/dang-ky/me");
  const { data: ketQua, loading: loadingDiem } = useApi("/sinh-vien/me/diem");
  const { data: canhBao, loading: loadingCanhBao, error: loiCanhBao } = useApi("/sinh-vien/me/canh-bao-dashboard");
  const { data: thoiKhoaBieu, loading: loadingTkb } = useApi("/sinh-vien/me/thoi-khoa-bieu");
  const hocKy = hocKyList?.find((item) => item.dangMoDangKy) || hocKyList?.[0];
  const { data: dotHienTai } = useApi(hocKy?.id ? "/dang-ky/dot-hien-tai" : null, hocKy?.id ? { hocKyId: hocKy.id } : undefined);
  const hocPhan = dangKy || [];
  const tongTinChi = hocPhan.reduce((sum, item) => sum + item.soTinChi, 0);

  const homNay = (() => {
    const day = new Date().getDay();
    return day === 0 ? 8 : day + 1;
  })();

  const lichHomNay = useMemo(
    () =>
      (thoiKhoaBieu || [])
        .filter((item) => item.thu === homNay)
        .sort((a, b) => a.tietBatDau - b.tietBatDau),
    [thoiKhoaBieu, homNay]
  );

  const gpaTheoKy = useMemo(
    () =>
      [...(ketQua?.hocKy || [])]
        .filter((item) => item.tinChiCoDiem > 0)
        .sort((a, b) => {
          const dateFor = (id) => hocKyList?.find((ky) => ky.id === id)?.ngayBatDau || "";
          return dateFor(a.hocKyId).localeCompare(dateFor(b.hocKyId));
        })
        .map((item) => ({
          hocKyId: item.hocKyId,
          name: `${item.tenHocKy} ${item.namHoc}`,
          gpa10: item.gpa10,
          gpa4: item.gpa4,
        })),
    [ketQua, hocKyList]
  );

  const monDaCoDiem = useMemo(
    () =>
      (ketQua?.items || [])
        .filter((item) => item.diemTongKet !== null && item.diemTongKet !== undefined)
        .slice(0, 4),
    [ketQua]
  );

  return (
    <div>
      <section className="dashboard-hero relative overflow-hidden rounded-[24px] border border-[#dce6d5] p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-10 -top-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-24 left-24 h-56 w-56 rounded-full bg-cyan-300/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="hero-muted text-[11px] font-semibold uppercase tracking-[0.2em]">
              MỘC CAMPUS · KHÔNG GIAN HỌC TẬP
            </p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">
              Xin chào, {user?.hoTen || user?.username}
            </h2>
            <p className="hero-muted mt-2 max-w-xl text-sm leading-relaxed">
              Bạn đang đăng ký {tongTinChi} tín chỉ trong {hocKy?.ten || "học kỳ hiện tại"}. Theo dõi lịch học,
              tiến độ tín chỉ và kết quả học tập ngay tại đây.
            </p>
            <div className="mt-5 flex flex-wrap gap-2">
              <Link
                to="/dang-ky"
                className="hero-action inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold shadow-sm transition"
              >
                <BookOpen size={16} /> Đăng ký học phần
              </Link>
              <Link
                to="/thoi-khoa-bieu"
                className="inline-flex items-center gap-2 rounded-xl border border-[#cbd8c5] bg-white/60 px-4 py-2.5 text-sm font-medium text-[#34564c] transition hover:bg-white"
              >
                <CalendarDays size={16} /> Thời khóa biểu
              </Link>
            </div>
          </div>

          <div className="w-full max-w-xs rounded-xl border border-white/80 bg-white/70 p-4">
            <p className="hero-muted text-[10px] font-semibold uppercase tracking-wider">
              Học kỳ hiện tại
            </p>
            <p className="mt-1 text-lg font-semibold">{hocKy ? `${hocKy.ten} - ${hocKy.namHoc}` : "—"}</p>
            <div className="mt-3 flex items-center justify-between gap-3 text-xs">
              <span
                className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${
                  hocKy?.dangMoDangKy
                    ? "bg-emerald-300/20 text-emerald-50 ring-emerald-200/30"
                    : "bg-white/10 text-white ring-white/20"
                }`}
              >
                <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
                {hocKy?.dangMoDangKy ? "Đang mở đăng ký" : "Đã đóng đăng ký"}
              </span>
              <span className="hero-muted">
                {hocKy ? `${hocKy.soTinChiToiThieu} - ${hocKy.soTinChiToiDa} TC` : ""}
              </span>
            </div>
            {hocKy && (
              <p className="hero-muted mt-3 text-[11px]">
                Đăng ký đến {formatDate(hocKy.dangKyDen)}
              </p>
            )}
          </div>
        </div>
      </section>

      <div className="mt-4"><Alert tone="warning">Quy chế thay điểm và tính GPA/tín chỉ khi học lại, học cải thiện chưa được cấu hình; GPA cho các môn học nhiều lần chưa phải kết quả chính thức.</Alert></div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Học phần đã đăng ký" value={hocPhan.length} icon={ClipboardList} />
        <StatCard label="Tín chỉ học kỳ" value={tongTinChi} icon={BookOpen} tone="emerald" />
        <StatCard
          label="GPA hệ 4"
          value={(ketQua?.gpa4 || 0).toFixed(2)}
          hint={`Hệ 10: ${(ketQua?.gpa10 || 0).toFixed(2)}`}
          icon={GraduationCap}
          tone="amber"
        />
        <StatCard
          label="Tín chỉ tích lũy"
          value={ketQua?.tinChiTichLuy || 0}
          hint="Chỉ tính môn đạt từ 5.0"
          icon={GraduationCap}
          tone="sky"
        />
      </div>

      <section className="card mt-6 p-5" aria-labelledby="academic-alerts-heading">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 id="academic-alerts-heading" className="text-sm font-semibold text-slate-800">Cảnh báo học vụ</h2>
            <p className="mt-0.5 text-xs text-slate-500">Điểm đã có và các mốc đăng ký sắp mở hoặc sắp đóng</p>
          </div>
          {!loadingCanhBao && !loiCanhBao && <Badge tone="amber">{canhBao?.length || 0} cảnh báo</Badge>}
        </div>
        {loadingCanhBao ? (
          <Spinner />
        ) : loiCanhBao ? (
          <Alert tone="error" className="mt-4">Không thể tải cảnh báo học vụ. Vui lòng thử tải lại trang.</Alert>
        ) : canhBao?.length ? (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {canhBao.map((item, index) => (
              <article key={`${item.loai}-${item.hocKyId}-${index}`} className="rounded-xl border border-amber-200/80 bg-amber-50/60 p-4">
                <h3 className="text-sm font-semibold text-slate-800">{item.tieuDe}</h3>
                <p className="mt-1 text-sm text-slate-600">{item.lyDo}</p>
                <p className="mt-2 text-xs text-slate-500">Học kỳ: {item.tenHocKy} {item.namHoc}</p>
                <Link to={item.lienKet} className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-700">
                  Xem chi tiết <ArrowRight size={13} />
                </Link>
              </article>
            ))}
          </div>
        ) : (
          <p className="mt-4 rounded-xl bg-slate-50 px-4 py-5 text-center text-sm text-slate-500">Hiện không có cảnh báo học vụ.</p>
        )}
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <section className="card p-5 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Xu hướng GPA theo học kỳ</h2>
              <p className="mt-0.5 text-xs text-slate-500">Điểm trung bình hệ 10 của các học kỳ đã có điểm</p>
            </div>
            <Badge tone="indigo">Hệ 10</Badge>
          </div>

          {loadingDiem ? (
            <Spinner />
          ) : gpaTheoKy.length === 0 ? (
            <EmptyState
              icon={GraduationCap}
              title="Chưa có dữ liệu GPA"
              description="Biểu đồ sẽ hiển thị sau khi bạn có điểm tổng kết."
            />
          ) : (
            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={gpaTheoKy} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 11, fill: "#64748b" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    domain={[0, 10]}
                    tick={{ fontSize: 11, fill: "#64748b" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <RechartsTooltip
                    contentStyle={CHART_TOOLTIP}
                    formatter={(value) => [formatDiem(value), "GPA hệ 10"]}
                    labelStyle={{ color: "#334155", fontWeight: 600 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="gpa10"
                    stroke="#6366f1"
                    strokeWidth={3}
                    dot={{ r: 4, fill: "#fff", strokeWidth: 3, stroke: "#6366f1" }}
                    activeDot={{ r: 6 }}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="card p-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Lịch học hôm nay</h2>
              <p className="mt-0.5 text-xs text-slate-500">{THU_LABEL[homNay] || "Hôm nay"}</p>
            </div>
            <Badge tone="sky">{lichHomNay.length} buổi</Badge>
          </div>

          {loadingTkb ? (
            <Spinner />
          ) : lichHomNay.length === 0 ? (
            <div className="mt-6 flex flex-col items-center gap-2 text-center">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-400 ring-1 ring-slate-100">
                <CalendarDays size={22} />
              </span>
              <p className="text-sm font-medium text-slate-600">Hôm nay không có lịch học</p>
              <p className="text-xs text-slate-400">Bạn có thể xem toàn bộ tuần trong mục Thời khóa biểu.</p>
            </div>
          ) : (
            <div className="mt-4 space-y-3">
              {lichHomNay.map((item) => (
                <div key={item.lopHocPhanId} className="rounded-xl border border-slate-200/80 p-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-medium text-slate-800">{item.tenMon}</p>
                    <span className="shrink-0 rounded-lg bg-indigo-50 px-2 py-1 text-[11px] font-semibold text-indigo-700">
                      Tiết {item.tietBatDau}
                    </span>
                  </div>
                  <p className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
                    <Clock size={13} /> Tiết {item.tietBatDau}-{item.tietBatDau + item.soTiet - 1}
                    <span className="text-slate-300">·</span>
                    <MapPin size={13} /> {item.phong}
                  </p>
                  <p className="mt-0.5 text-[11px] text-slate-400">{item.maLop}</p>
                </div>
              ))}
            </div>
          )}

          <Link
            to="/thoi-khoa-bieu"
            className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-indigo-600 hover:text-indigo-700"
          >
            Xem cả tuần <ArrowRight size={13} />
          </Link>
        </section>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <section className="card p-5 xl:col-span-2">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Học phần đang đăng ký</h2>
              <p className="mt-0.5 text-xs text-slate-500">Tổng {tongTinChi} tín chỉ trong học kỳ</p>
            </div>
            <Link to="/hoc-phan-cua-toi" className="text-xs font-medium text-indigo-600 hover:text-indigo-700">
              Xem tất cả
            </Link>
          </div>

          {loadingDangKy ? (
            <Spinner />
          ) : hocPhan.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="Chưa đăng ký học phần"
              description="Chọn lớp phù hợp trong mục Đăng ký học phần."
              action={
                <Link to="/dang-ky" className="btn-primary">
                  Đăng ký ngay
                </Link>
              }
            />
          ) : (
            <div className="mt-4 space-y-2">
              {hocPhan.slice(0, 4).map((item) => (
                <div
                  key={item.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200/80 px-3.5 py-3"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-indigo-100 to-violet-100 text-xs font-semibold text-indigo-700">
                      {item.maMon.slice(0, 2)}
                    </span>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-slate-800">{item.tenMon}</p>
                      <p className="text-xs text-slate-500">
                        {item.maLop} · {item.soTinChi} TC · {item.tenGiangVien}
                      </p>
                    </div>
                  </div>
                  <p className="text-xs text-slate-500">{lichHoc(item.thu, item.tietBatDau, item.soTiet)}</p>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="card p-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Điểm gần đây</h2>
              <p className="mt-0.5 text-xs text-slate-500">Một số học phần đã có điểm</p>
            </div>
            <Link to="/diem" className="text-xs font-medium text-indigo-600 hover:text-indigo-700">
              Chi tiết
            </Link>
          </div>

          {loadingDiem ? (
            <Spinner />
          ) : monDaCoDiem.length === 0 ? (
            <div className="mt-6 text-center text-sm text-slate-500">Chưa có điểm tổng kết.</div>
          ) : (
            <div className="mt-4 space-y-3">
              {monDaCoDiem.map((item) => (
                <div key={item.id} className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm text-slate-700">{item.tenMon}</p>
                    <p className="text-[11px] text-slate-400">{item.maMon}</p>
                  </div>
                  <Badge tone={item.diemTongKet >= 5 ? "green" : "red"}>
                    {formatDiem(item.diemTongKet)}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
