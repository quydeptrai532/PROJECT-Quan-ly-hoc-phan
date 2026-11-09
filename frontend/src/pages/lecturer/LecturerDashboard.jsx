import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertCircle,
  ArrowRight,
  Bell,
  CalendarDays,
  CheckCircle2,
  Clock,
  ExternalLink,
  Layers,
  MapPin,
  Users,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Alert, Badge, EmptyState, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { formatDateTime, lichHoc, THU_LABEL } from "../../lib/format";
import { useApi } from "../../lib/useApi";

const CHART_TOOLTIP = {
  borderRadius: 12,
  border: "1px solid #e2e8f0",
  fontSize: 12,
  boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
};

const GRADE_SHEET_STATUS_BADGE = {
  NHAP: { label: "Bản nháp", tone: "slate" },
  CHO_DUYET: { label: "Chờ duyệt", tone: "amber" },
  TRA_VE: { label: "Trả về chỉnh sửa", tone: "red" },
  DA_DUYET: { label: "Đã duyệt", tone: "sky" },
  DA_CONG_BO: { label: "Đã công bố", tone: "emerald" },
  DA_KHOA: { label: "Đã khóa", tone: "indigo" },
};

const TASK_BADGE = {
  NHAP_DIEM: { label: "Cần nhập điểm", tone: "amber" },
  GUI_DUYET: { label: "Chờ gửi duyệt", tone: "indigo" },
  TRA_VE: { label: "Trả về chỉnh sửa", tone: "red" },
  CHO_DUYET: { label: "Đang chờ duyệt", tone: "sky" },
  CHO_CONG_BO: { label: "Chờ công bố", tone: "emerald" },
};

export default function LecturerDashboard() {
  const { user } = useAuth();
  const { data: hocKyList } = useApi("/hoc-ky");
  const [selectedHocKyId, setSelectedHocKyId] = useState("");

  const params = useMemo(
    () => (selectedHocKyId ? { hocKyId: selectedHocKyId } : undefined),
    [selectedHocKyId]
  );

  const {
    data: dashboardData,
    loading: loadingDashboard,
    error: dashboardError,
  } = useApi("/giang-vien/me/dashboard", params);

  const { data: notificationData, loading: loadingNotifications } = useApi("/notifications", {
    size: 5,
  });

  const hocKy = dashboardData?.hocKy;
  const tongQuan = dashboardData?.tongQuan || {
    soLop: 0,
    tongSinhVien: 0,
    tongSucChua: 0,
    soBuoiMoiTuan: 0,
    soBangDiemNhap: 0,
    soBangDiemChoDuyet: 0,
    soBangDiemDaDuyet: 0,
    soBangDiemDaCongBo: 0,
  };
  const lopPhuTrach = dashboardData?.lopPhuTrach || [];
  const lichDay = dashboardData?.lichDay || [];
  const congViec = dashboardData?.congViec || [];
  const notifications = notificationData?.content || [];
  const unreadCount = notificationData?.unreadCount ?? 0;

  const homNay = (() => {
    const day = new Date().getDay();
    return day === 0 ? 8 : day + 1;
  })();

  const lichHomNay = useMemo(
    () =>
      lichDay
        .filter((item) => item.thu === homNay)
        .sort((a, b) => a.tietBatDau - b.tietBatDau),
    [lichDay, homNay]
  );

  const chartData = useMemo(
    () =>
      lopPhuTrach.map((item) => ({
        name: item.ma,
        "Đã đăng ký": item.siSoHienTai,
        "Sĩ số tối đa": item.siSoToiDa,
      })),
    [lopPhuTrach]
  );

  if (loadingDashboard && !dashboardData) {
    return <Spinner label="Đang tải dữ liệu dashboard giảng viên..." />;
  }

  return (
    <div className="space-y-6">
      {dashboardError && <Alert tone="error">{dashboardError}</Alert>}

      {/* Hero Header */}
      <section className="dashboard-hero relative overflow-hidden rounded-[24px] border border-[#dce6d5] p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-10 -top-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-24 left-24 h-56 w-56 rounded-full bg-emerald-300/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="hero-muted text-[11px] font-semibold uppercase tracking-[0.2em]">
                Không gian giảng dạy
              </span>
              {hocKy && (
                <Badge tone="emerald">
                  {hocKy.ten} · {hocKy.namHoc}
                </Badge>
              )}
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
              Xin chào, {user?.hoTen || user?.username}
            </h1>
            <p className="hero-muted max-w-xl text-sm leading-relaxed">
              Bạn đang phụ trách {tongQuan.soLop} lớp học phần với {tongQuan.tongSinhVien} sinh viên
              trong {hocKy ? `${hocKy.ten} (${hocKy.namHoc})` : "học kỳ hiện tại"}.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-3">
              <Link
                to="/lop-cua-toi"
                className="hero-action inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold shadow-sm transition"
              >
                <Layers size={16} /> Lớp học phần
              </Link>
              <Link
                to="/lop-cua-toi/thoi-khoa-bieu"
                className="inline-flex items-center gap-2 rounded-xl border border-[#cbd8c5] bg-white/70 px-4 py-2 text-sm font-medium text-[#34564c] transition hover:bg-white"
              >
                <CalendarDays size={16} /> Lịch giảng dạy
              </Link>
              <Link
                to="/thong-bao"
                className="inline-flex items-center gap-2 rounded-xl border border-[#cbd8c5] bg-white/70 px-4 py-2 text-sm font-medium text-[#34564c] transition hover:bg-white"
              >
                <Bell size={16} /> Thông báo {unreadCount > 0 && `(${unreadCount})`}
              </Link>
            </div>
          </div>

          {/* Semester Selector & Today quick badge */}
          <div className="flex flex-col gap-3 rounded-2xl border border-white/80 bg-white/80 p-4 shadow-sm sm:min-w-[280px]">
            <div>
              <label htmlFor="dashboard-semester" className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                Chọn học kỳ xem
              </label>
              <select
                id="dashboard-semester"
                className="input mt-1 w-full bg-white text-xs font-medium"
                value={selectedHocKyId}
                onChange={(e) => setSelectedHocKyId(e.target.value)}
              >
                <option value="">Học kỳ hiện tại {hocKy ? `(${hocKy.ten})` : ""}</option>
                {(hocKyList || []).map((ky) => (
                  <option key={ky.id} value={ky.id}>
                    {ky.ten} · {ky.namHoc}
                  </option>
                ))}
              </select>
            </div>

            <div className="rounded-xl border border-emerald-100 bg-emerald-50/70 p-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-emerald-800">
                Lịch dạy hôm nay · {THU_LABEL[homNay] || "Hôm nay"}
              </p>
              <p className="mt-1 text-base font-bold text-emerald-950">
                {lichHomNay.length > 0 ? `${lichHomNay.length} buổi giảng dạy` : "Không có lịch dạy"}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Stat Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Lớp phụ trách" value={tongQuan.soLop} icon={Layers} />
        <StatCard
          label="Sinh viên"
          value={tongQuan.tongSinhVien}
          hint={tongQuan.tongSucChua > 0 ? `Sức chứa ${tongQuan.tongSucChua}` : ""}
          icon={Users}
          tone="emerald"
        />
        <StatCard
          label="Buổi dạy / tuần"
          value={tongQuan.soBuoiMoiTuan}
          icon={CalendarDays}
          tone="amber"
        />
        <StatCard
          label="Việc cần xử lý"
          value={congViec.length}
          hint={congViec.length > 0 ? "Bảng điểm nháp / chờ duyệt" : "Tất cả đã hoàn tất"}
          icon={AlertCircle}
          tone={congViec.length > 0 ? "rose" : "sky"}
        />
      </div>

      {/* Main Grid: Tasks & Notifications */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Pending tasks section */}
        <section className="card p-5" aria-labelledby="tasks-heading">
          <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-amber-50 text-amber-600">
                <AlertCircle size={18} />
              </span>
              <div>
                <h2 id="tasks-heading" className="text-sm font-semibold text-slate-800">
                  Công việc cần xử lý
                </h2>
                <p className="text-xs text-slate-500">
                  Theo dõi bảng điểm nháp, trả về chỉnh sửa, thiếu điểm hoặc đang chờ duyệt
                </p>
              </div>
            </div>
            <Badge tone={congViec.length > 0 ? "amber" : "emerald"}>
              {congViec.length} việc
            </Badge>
          </div>

          <div className="mt-4 space-y-3">
            {congViec.length === 0 ? (
              <div className="rounded-xl border border-dashed border-emerald-200 bg-emerald-50/40 p-6 text-center">
                <div className="mx-auto grid h-10 w-10 place-items-center rounded-full bg-emerald-100 text-emerald-600">
                  <CheckCircle2 size={20} />
                </div>
                <p className="mt-2 text-sm font-medium text-emerald-900">
                  Không có công việc tồn đọng
                </p>
                <p className="mt-1 text-xs text-emerald-700">
                  Tất cả các lớp học phần trong kỳ đã được nhập điểm đầy đủ hoặc đã hoàn tất công bố.
                </p>
              </div>
            ) : (
              congViec.map((task, idx) => {
                const meta = TASK_BADGE[task.loai] || { label: task.loai, tone: "slate" };
                return (
                  <div
                    key={`${task.lopHocPhanId}-${idx}`}
                    className="flex flex-col justify-between gap-3 rounded-xl border border-slate-200/90 bg-white p-3.5 shadow-sm sm:flex-row sm:items-center hover:border-indigo-200"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge tone={meta.tone}>{meta.label}</Badge>
                        <span className="text-xs font-semibold text-slate-900">
                          {task.maLop} · {task.tenMon}
                        </span>
                      </div>
                      <p className="text-xs font-medium text-slate-700">{task.tieuDe}</p>
                      <p className="text-xs text-slate-500">{task.moTa}</p>
                    </div>
                    <Link
                      to={task.lienKet}
                      className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-1.5 text-xs font-medium text-indigo-700 transition hover:bg-indigo-100"
                    >
                      Xử lý ngay <ArrowRight size={13} />
                    </Link>
                  </div>
                );
              })
            )}
          </div>
        </section>

        {/* Notifications section */}
        <section className="card p-5" aria-labelledby="notifications-heading">
          <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-sky-50 text-sky-600">
                <Bell size={18} />
              </span>
              <div>
                <h2 id="notifications-heading" className="text-sm font-semibold text-slate-800">
                  Thông báo hệ thống
                </h2>
                <p className="text-xs text-slate-500">Các cập nhật và thông báo mới gửi tới bạn</p>
              </div>
            </div>
            <Link
              to="/thong-bao"
              className="inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-700"
            >
              Tất cả <ExternalLink size={12} />
            </Link>
          </div>

          <div className="mt-4 space-y-3">
            {loadingNotifications ? (
              <Spinner label="Đang tải thông báo..." />
            ) : notifications.length === 0 ? (
              <div className="rounded-xl border border-dashed border-slate-200 p-6 text-center text-xs text-slate-400">
                Chưa có thông báo nào.
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={`rounded-xl border p-3 transition ${
                    item.read
                      ? "border-slate-200/70 bg-slate-50/50"
                      : "border-sky-200 bg-sky-50/40"
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-1.5">
                      {!item.read && (
                        <span className="h-2 w-2 rounded-full bg-sky-500" title="Chưa đọc" />
                      )}
                      <p className="text-xs font-semibold text-slate-800">{item.title}</p>
                    </div>
                    <span className="shrink-0 text-[11px] text-slate-400">
                      {formatDateTime(item.createdAt)}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-600 line-clamp-2">{item.message}</p>
                  {item.targetUrl && (
                    <Link
                      to={item.targetUrl}
                      className="mt-2 inline-flex items-center gap-1 text-[11px] font-medium text-indigo-600 hover:text-indigo-700"
                    >
                      Xem chi tiết <ArrowRight size={11} />
                    </Link>
                  )}
                </div>
              ))
            )}
          </div>
        </section>
      </div>

      {/* Chart & Today's Schedule */}
      <div className="grid gap-6 xl:grid-cols-3">
        {/* Class Enrollment Chart */}
        <section className="card p-5 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Sĩ số lớp học phần</h2>
              <p className="mt-0.5 text-xs text-slate-500">
                So sánh số sinh viên đã đăng ký và sĩ số tối đa theo từng lớp
              </p>
            </div>
            <Badge tone="emerald">{lopPhuTrach.length} lớp</Badge>
          </div>

          {lopPhuTrach.length === 0 ? (
            <EmptyState icon={Layers} title="Chưa có lớp học phần trong học kỳ này" />
          ) : (
            <div className="mt-4 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 11, fill: "#64748b" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: "#64748b" }}
                    axisLine={false}
                    tickLine={false}
                    allowDecimals={false}
                  />
                  <RechartsTooltip
                    contentStyle={CHART_TOOLTIP}
                    cursor={{ fill: "rgba(99, 102, 241, 0.06)" }}
                  />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar
                    dataKey="Đã đăng ký"
                    fill="#10b981"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={34}
                    isAnimationActive={false}
                  />
                  <Bar
                    dataKey="Sĩ số tối đa"
                    fill="#cbd5e1"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={34}
                    isAnimationActive={false}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        {/* Today's Schedule */}
        <section className="card p-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Lịch dạy hôm nay</h2>
              <p className="mt-0.5 text-xs text-slate-500">{THU_LABEL[homNay] || "Hôm nay"}</p>
            </div>
            <Badge tone="sky">{lichHomNay.length} buổi</Badge>
          </div>

          {lichHomNay.length === 0 ? (
            <div className="mt-6 flex flex-col items-center gap-2 text-center">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-400 ring-1 ring-slate-100">
                <CalendarDays size={22} />
              </span>
              <p className="text-sm font-medium text-slate-600">Hôm nay không có lịch dạy</p>
              <p className="text-xs text-slate-400">Xem lịch cả tuần trong mục Thời khóa biểu.</p>
            </div>
          ) : (
            <div className="mt-4 space-y-3">
              {lichHomNay.map((item) => (
                <div key={item.lopHocPhanId} className="rounded-xl border border-slate-200/80 p-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-medium text-slate-800">{item.tenMon}</p>
                    <span className="shrink-0 rounded-lg bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700">
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
            to="/lop-cua-toi/thoi-khoa-bieu"
            className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-emerald-600 hover:text-emerald-700"
          >
            Xem cả tuần <ArrowRight size={13} />
          </Link>
        </section>
      </div>

      {/* Classes Grid with Grade Status & Student progress */}
      <section className="card p-5">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-semibold text-slate-800">Lớp đang phụ trách</h2>
            <p className="mt-0.5 text-xs text-slate-500">
              Chi tiết các lớp giảng dạy, trạng thái bảng điểm và tiến độ nhập điểm
            </p>
          </div>
          <Link
            to="/lop-cua-toi"
            className="text-xs font-medium text-indigo-600 hover:text-indigo-700"
          >
            Xem tất cả ({lopPhuTrach.length})
          </Link>
        </div>

        {lopPhuTrach.length === 0 ? (
          <div className="mt-4">
            <EmptyState icon={Layers} title="Chưa được phân công lớp học phần nào trong kỳ này" />
          </div>
        ) : (
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {lopPhuTrach.map((lop) => {
              const percent = lop.siSoToiDa
                ? Math.min(100, (lop.siSoHienTai / lop.siSoToiDa) * 100)
                : 0;
              const statusMeta =
                GRADE_SHEET_STATUS_BADGE[lop.trangThaiBangDiem] ||
                GRADE_SHEET_STATUS_BADGE.NHAP;

              return (
                <div
                  key={lop.id}
                  className="rounded-xl border border-slate-200/80 bg-white p-4 transition hover:border-indigo-200 hover:shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-800">{lop.tenMon}</p>
                      <p className="text-xs text-slate-500">
                        {lop.ma} · {lop.soTinChi} tín chỉ
                      </p>
                    </div>
                    <Badge tone={statusMeta.tone}>{statusMeta.label}</Badge>
                  </div>

                  <p className="mt-2 text-xs text-slate-600">
                    {lichHoc(lop.thu, lop.tietBatDau, lop.soTiet)} · Phòng {lop.phong}
                  </p>

                  <div className="mt-3 flex items-center gap-2">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-200">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-teal-500"
                        style={{ width: `${percent}%` }}
                      />
                    </div>
                    <span className="text-xs text-slate-600">
                      {lop.siSoHienTai}/{lop.siSoToiDa} SV
                    </span>
                  </div>

                  <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-2 text-xs">
                    <span className="text-slate-500">
                      Đã nhập điểm:{" "}
                      <strong className="font-semibold text-slate-800">
                        {lop.soDaCoDiem}/{lop.soDangHoc}
                      </strong>
                    </span>
                    <Link
                      to={`/lop-cua-toi/${lop.id}/nhap-diem`}
                      className="font-medium text-indigo-600 hover:text-indigo-700"
                    >
                      Nhập / Xem điểm →
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
