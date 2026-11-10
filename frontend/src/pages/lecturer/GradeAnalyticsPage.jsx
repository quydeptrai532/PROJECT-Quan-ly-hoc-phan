import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, BarChart3, BookOpen, FileCheck2, Layers } from "lucide-react";
import GradeAnalyticsPanel from "../../components/GradeAnalyticsPanel";
import { Badge, PageHeader } from "../../components/ui";
import { useApi } from "../../lib/useApi";

const SECTIONS = [
  { id: "tk-tong-hop", label: "Tổng hợp chính thức", icon: FileCheck2 },
  { id: "tk-theo-mon", label: "Thống kê theo môn", icon: BookOpen },
  { id: "tk-theo-lop", label: "Chi tiết theo lớp", icon: Layers },
];

export default function LecturerGradeAnalyticsPage() {
  const { data: lopList } = useApi("/giang-vien/me/lop-hoc-phan");
  const [hocKyId, setHocKyId] = useState(
    () => new URLSearchParams(window.location.search).get("hocKyId") || ""
  );
  const [activeSection, setActiveSection] = useState(SECTIONS[0].id);

  const hocKyList = useMemo(() => {
    const map = new Map();
    (lopList || []).forEach((item) => {
      if (!map.has(item.hocKyId)) map.set(item.hocKyId, item.tenHocKy);
    });
    return [...map.entries()].map(([id, ten]) => ({ id, ten }));
  }, [lopList]);

  useEffect(() => {
    const onScroll = () => {
      let current = SECTIONS[0].id;
      for (const section of SECTIONS) {
        const element = document.getElementById(section.id);
        if (element && element.getBoundingClientRect().top <= 160) {
          current = section.id;
        }
      }
      setActiveSection(current);
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const scrollToSection = (id) => {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <div>
      <PageHeader
        icon={BarChart3}
        eyebrow="Giảng viên"
        title="Thống kê điểm & phân bố điểm"
        description="Tổng hợp không chứa thông tin danh tính sinh viên. Sinh viên chưa có điểm công bố được tách riêng ngoài mẫu số, không bị tính ngầm thành 0.0."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="green">Chỉ tính bảng điểm Đã công bố / Đã khóa</Badge>
            <select
              aria-label="Chọn học kỳ thống kê"
              className="input w-auto min-w-[220px]"
              value={hocKyId}
              onChange={(event) => setHocKyId(event.target.value)}
            >
              <option value="">Tất cả học kỳ</option>
              {hocKyList.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.ten}
                </option>
              ))}
            </select>
          </div>
        }
      />

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_236px]">
        <GradeAnalyticsPanel mode="lecturer" initialHocKyId={hocKyId} hideTitle />

        <aside className="hidden lg:block">
          <nav
            aria-label="Mục lục trang thống kê điểm"
            className="sticky top-24 rounded-2xl border border-slate-200 bg-white p-3 shadow-soft"
          >
            <p className="px-2 pb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-400">
              Trong trang này
            </p>
            <ul className="space-y-1">
              {SECTIONS.map(({ id, label, icon: Icon }) => {
                const active = activeSection === id;
                return (
                  <li key={id}>
                    <button
                      type="button"
                      onClick={() => scrollToSection(id)}
                      aria-current={active ? "true" : undefined}
                      className={`flex w-full items-center gap-2 rounded-xl px-2.5 py-2 text-left text-[13px] transition-colors ${
                        active
                          ? "bg-[#edf3e4] font-semibold text-[#2c4a35]"
                          : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                      }`}
                    >
                      <Icon size={15} className={active ? "text-[#5c7d4f]" : "text-slate-400"} />
                      <span className="truncate">{label}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
            <div className="mt-3 border-t border-slate-100 pt-3">
              <Link
                to="/lop-cua-toi"
                className="flex items-center gap-2 rounded-xl px-2.5 py-2 text-[13px] text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
              >
                <ArrowLeft size={15} className="text-slate-400" />
                Danh sách lớp học phần
              </Link>
            </div>
          </nav>
        </aside>
      </div>
    </div>
  );
}
