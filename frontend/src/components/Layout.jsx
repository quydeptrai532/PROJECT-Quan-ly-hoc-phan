import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  BookMarked,
  BookOpen,
  BookOpenCheck,
  Bell,
  Building2,
  CalendarCheck,
  CalendarDays,
  CalendarRange,
  ClipboardList,
  ChartNoAxesCombined,
  LibraryBig,
  GraduationCap,
  History,
  Layers,
  LayoutDashboard,
  LifeBuoy,
  LogOut,
  Menu,
  Network,
  ScrollText,
  UserCog,
  UserRound,
  Users,
  X,
} from "lucide-react";
import { ROLE_LABEL, useAuth } from "../context/AuthContext";
import api from "../api/client";

const NAV = {
  SINH_VIEN: [
    { to: "/tong-quan", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/dang-ky", label: "Đăng ký học phần", icon: BookOpen },
    { to: "/dang-ky-hoc-lai", label: "Học lại / cải thiện", icon: BookOpenCheck },
    { to: "/ke-hoach-hoc-tap", label: "Kế hoạch học tập", icon: CalendarRange },
    { to: "/hoc-phan-cua-toi", label: "Học phần của tôi", icon: ClipboardList },
    { to: "/lich-su-dang-ky", label: "Lịch sử đăng ký", icon: History },
    { to: "/thoi-khoa-bieu", label: "Thời khóa biểu", icon: CalendarDays },
    { to: "/diem", label: "Điểm & kết quả", icon: GraduationCap },
    { to: "/chuyen-can-cua-toi", label: "Chuyên cần", icon: CalendarCheck },
    { to: "/lich-su-hoc-tap", label: "Lịch sử học tập", icon: ScrollText },
    { to: "/tien-do", label: "Tiến độ học tập", icon: ChartNoAxesCombined },
    { to: "/yeu-cau-ho-tro", label: "Hỗ trợ & khiếu nại", icon: LifeBuoy },
    { to: "/thong-bao", label: "Thông báo", icon: Bell },
  ],
  GIANG_VIEN: [
    { to: "/tong-quan", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/lop-cua-toi", label: "Lớp học phần", icon: Layers },
    { to: "/lop-cua-toi/thoi-khoa-bieu", label: "Thời khóa biểu", icon: CalendarDays },
    { to: "/thong-bao", label: "Thông báo", icon: Bell },
  ],
  QUAN_TRI: [
    { to: "/quan-tri", label: "Tổng quan", icon: LayoutDashboard },
    { to: "/quan-tri/hoc-ky", label: "Học kỳ", icon: CalendarRange },
    { to: "/quan-tri/khoa", label: "Khoa", icon: Building2 },
    { to: "/quan-tri/nganh", label: "Ngành", icon: Network },
    { to: "/quan-tri/lop-hanh-chinh", label: "Lớp hành chính", icon: Users },
    { to: "/quan-tri/mon-hoc", label: "Môn học", icon: BookMarked },
    { to: "/quan-tri/quan-he-mon", label: "Quan hệ môn học", icon: Network },
    { to: "/quan-tri/khoa-hoc", label: "Khóa học / Niên khóa", icon: CalendarRange },
    { to: "/quan-tri/lop-hoc-phan", label: "Lớp học phần", icon: Layers },
    { to: "/quan-tri/sinh-vien", label: "Sinh viên", icon: UserRound },
    { to: "/quan-tri/chuong-trinh-dao-tao", label: "Chương trình đào tạo", icon: LibraryBig },
    { to: "/quan-tri/giang-vien", label: "Giảng viên", icon: UserCog },
    { to: "/quan-tri/yeu-cau-ho-tro", label: "Yêu cầu hỗ trợ", icon: LifeBuoy },
    { to: "/quan-tri/nhat-ky-hoat-dong", label: "Nhật ký hoạt động", icon: History },
      { to: "/quan-tri/canh-bao-audit", label: "Cảnh báo Audit", icon: Bell },
  ],
};

const SECTION_LABEL = {
  SINH_VIEN: "Không gian học tập",
  GIANG_VIEN: "Không gian giảng dạy",
  QUAN_TRI: "Quản trị hệ thống",
};

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!["SINH_VIEN", "GIANG_VIEN"].includes(user?.role)) {
      setUnreadCount(0);
      return undefined;
    }
    const loadUnreadCount = () => {
      api.get("/notifications", { params: { unreadOnly: true, page: 0, size: 1 } })
        .then(({ data }) => setUnreadCount(data.unreadCount || 0))
        .catch(() => setUnreadCount(0));
    };
    loadUnreadCount();
    window.addEventListener("notifications:updated", loadUnreadCount);
    return () => window.removeEventListener("notifications:updated", loadUnreadCount);
  }, [user?.role, location.pathname]);

  const items = NAV[user?.role] || [];
  const allItems = Object.values(NAV).flat();
  const exact = allItems.find((item) => item.to === location.pathname);
  const title = exact
    ? exact.label
    : location.pathname.endsWith("/nhap-diem")
    ? "Nhập điểm"
    : location.pathname.endsWith("/diem-danh")
    ? "Điểm danh & Chuyên cần"
    : "Hồ sơ cá nhân";

  const homNay = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (
    <div className="min-h-screen">
      {open && (
        <div
          className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-sm lg:hidden"
          onClick={() => setOpen(false)}
        />
      )}

      <aside
        className={`app-sidebar fixed inset-y-0 left-0 z-40 flex w-64 flex-col overflow-hidden transition-transform duration-300 lg:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="relative flex h-[76px] shrink-0 items-center gap-3 border-b border-white/10 px-5">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-[#d9ed8a] text-[#173f3a]">
            <GraduationCap size={21} />
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-bold tracking-wide text-white">MỘC · CAMPUS</p>
            <p className="truncate text-[10px] uppercase tracking-[.15em] text-[#a7bdb0]">Cổng sinh viên</p>
          </div>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="ml-auto rounded-lg p-1.5 text-slate-400 hover:bg-white/10 hover:text-white lg:hidden"
            aria-label="Đóng menu"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="relative flex-1 space-y-1 overflow-y-auto px-3 py-4">
          <p className="px-3 pb-3 text-[10px] font-semibold uppercase tracking-[0.16em] text-[#91aa9c]">
            {SECTION_LABEL[user?.role] || "Menu"}
          </p>
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/quan-tri" || item.to === "/lop-cua-toi"}
              className={({ isActive }) =>
                `nav-link group relative flex items-center gap-3 rounded-xl px-3 py-3 text-[13px] transition-all duration-200 ${
                  isActive
                    ? "nav-link-active font-semibold"
                    : ""
                }`
              }
            >
              <item.icon size={18} className="shrink-0" />
              <span className="truncate">{item.label}</span>
              {item.to === "/thong-bao" && unreadCount > 0 && (
                <span className="ml-auto rounded-full bg-[#d9ed8a] px-2 py-0.5 text-[10px] font-bold text-[#173f3a]">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="relative space-y-2 border-t border-white/10 p-4">
          <NavLink
            to="/ho-so"
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-colors ${
                isActive ? "bg-white/10 text-white" : "text-[#cfddd5] hover:bg-white/5 hover:text-white"
              }`
            }
          >
            <UserRound size={18} className="shrink-0" />
            Hồ sơ cá nhân
          </NavLink>
          <div className="rounded-xl p-2">
            <div className="flex items-center gap-2.5">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[#e7b397] text-sm font-semibold text-[#40352d]">
                {(user?.hoTen || user?.username || "?").slice(0, 1).toUpperCase()}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-white">{user?.hoTen || user?.username}</p>
                <p className="truncate text-[11px] text-slate-400">
                  {user?.ma} · {ROLE_LABEL[user?.role]}
                </p>
              </div>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-white"
                title="Đăng xuất"
              >
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="app-header sticky top-0 z-20 border-b backdrop-blur-xl">
          <div className="flex h-[68px] items-center gap-3 px-4 sm:px-8 lg:px-10">
            <button
              type="button"
              onClick={() => setOpen(true)}
              className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 lg:hidden"
              aria-label="Mở menu"
            >
              <Menu size={20} />
            </button>
            <div className="min-w-0 flex-1">
              <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-[#78917e]">
                Mộc Campus / {ROLE_LABEL[user?.role]}
              </p>
              <h1 className="page-heading truncate text-base font-semibold">{title}</h1>
            </div>

            <span className="hidden items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-500 shadow-sm xl:inline-flex">
              <CalendarDays size={14} className="text-indigo-500" />
              <span className="capitalize">{homNay}</span>
            </span>

            <div className="flex items-center gap-3">
              <div className="hidden text-right sm:block">
                <p className="max-w-[220px] truncate text-sm font-medium text-slate-800">
                  {user?.hoTen || user?.username}
                </p>
                <p className="text-[11px] text-slate-500">
                  {user?.ma} · {ROLE_LABEL[user?.role]}
                </p>
              </div>
              <span className="grid h-9 w-9 place-items-center rounded-full bg-[#d9ed8a] text-sm font-semibold text-[#173f3a]">
                {(user?.hoTen || user?.username || "?").slice(0, 1).toUpperCase()}
              </span>
            </div>
          </div>
        </header>

        <main className="app-main relative mx-auto w-full px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          <div key={location.pathname} className="page-enter">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
