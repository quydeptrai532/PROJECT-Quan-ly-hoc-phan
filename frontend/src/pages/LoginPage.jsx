import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BookOpen, Eye, EyeOff, GraduationCap, Loader2, Lock, ShieldCheck, User } from "lucide-react";
import { getErrorMessage } from "../api/client";
import { Alert } from "../components/ui";
import { homePath, useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user) {
      navigate(homePath(user.role), { replace: true });
    }
  }, [user, navigate]);

  useEffect(() => {
    const stored = sessionStorage.getItem("authNotice");
    const replaced = new URLSearchParams(window.location.search).get("reason") === "session_replaced";
    if (stored) {
      setNotice(stored);
      sessionStorage.removeItem("authNotice");
    } else if (replaced) {
      setNotice("Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại.");
    }
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const profile = await login(username.trim(), password);
      navigate(homePath(profile.role), { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid min-h-screen bg-[#f7f6f2] lg:grid-cols-12">
      {/* Left Architectural Showcase */}
      <div className="relative hidden overflow-hidden bg-[#0b1120] p-12 text-white lg:col-span-6 lg:flex lg:flex-col lg:justify-between xl:col-span-7">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.14]"
          style={{
            backgroundImage:
              "linear-gradient(to right, rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.08) 1px, transparent 1px)",
            backgroundSize: "48px 48px",
          }}
        />
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-[420px] w-[420px] rounded-full opacity-35 blur-3xl"
          style={{ background: "radial-gradient(circle, #f59e0b 0%, transparent 70%)" }}
        />
        <div
          className="pointer-events-none absolute -bottom-32 -left-24 h-[420px] w-[420px] rounded-full opacity-25 blur-3xl"
          style={{ background: "radial-gradient(circle, #06b6d4 0%, transparent 70%)" }}
        />

        <div className="relative flex items-center gap-3.5">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-amber-400 to-amber-500 text-[#0b1120] shadow-lg shadow-amber-500/25">
            <GraduationCap size={23} strokeWidth={2.2} />
          </span>
          <div>
            <p className="font-display text-lg font-extrabold tracking-wider text-white">
              AETHERIA <span className="text-amber-400">·</span> EDU
            </p>
            <p className="font-mono text-[11px] uppercase tracking-[0.16em] text-slate-400">
              Academic Atelier Portal
            </p>
          </div>
        </div>

        <div className="relative max-w-xl">
          <span className="inline-flex items-center gap-2 rounded-full border border-amber-400/30 bg-amber-400/10 px-3.5 py-1 font-mono text-[11px] font-semibold uppercase tracking-[0.18em] text-amber-300">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
            Học kỳ mới · Trực tuyến
          </span>
          <h1 className="font-display mt-5 text-4xl font-extrabold leading-[1.15] tracking-tight text-white">
            Đăng ký học phần chuẩn xác,{" "}
            <span className="bg-gradient-to-r from-amber-300 via-amber-400 to-cyan-300 bg-clip-text text-transparent">
              quản trị học vụ thông minh.
            </span>
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-300">
            Nền tảng đào tạo số thế hệ mới: kiểm tra điều kiện tiên quyết tức thì, đồng bộ thời khóa biểu
            thời gian thực và trợ lý AI cá nhân hóa theo từng tài khoản.
          </p>

          <ul className="mt-8 space-y-3.5 text-sm text-slate-200">
            <li className="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 backdrop-blur-sm">
              <BookOpen size={18} className="mt-0.5 shrink-0 text-amber-400" />
              <span>Kiểm tra tự động môn tiên quyết, trùng lịch, giới hạn tín chỉ và sĩ số lớp.</span>
            </li>
            <li className="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 backdrop-blur-sm">
              <ShieldCheck size={18} className="mt-0.5 shrink-0 text-cyan-400" />
              <span>Phân quyền chặt chẽ theo vai trò, xác thực JWT phiên đơn an toàn tuyệt đối.</span>
            </li>
            <li className="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 backdrop-blur-sm">
              <GraduationCap size={18} className="mt-0.5 shrink-0 text-amber-400" />
              <span>Theo dõi GPA học kỳ, tiến độ học tập và bảng điểm xác thực điện tử.</span>
            </li>
          </ul>

          <div className="mt-8 grid grid-cols-3 gap-3">
            {[
              { value: "3", label: "Vai trò" },
              { value: "JWT", label: "Bảo mật" },
              { value: "AI 24/7", label: "Trợ lý số" },
            ].map((item) => (
              <div
                key={item.label}
                className="rounded-2xl border border-white/10 bg-white/[0.04] px-3 py-3.5 text-center backdrop-blur"
              >
                <p className="font-display text-lg font-bold text-white">{item.value}</p>
                <p className="font-mono mt-0.5 text-[10px] uppercase tracking-wider text-slate-400">{item.label}</p>
              </div>
            ))}
          </div>
        </div>

        <p className="font-mono relative text-[11px] uppercase tracking-widest text-slate-500">
          Aetheria Academic System · Spring Boot + ReactJS
        </p>
      </div>

      {/* Right Porcelain Login Card */}
      <div className="flex items-center justify-center px-5 py-12 lg:col-span-6 xl:col-span-5">
        <div className="w-full max-w-md rounded-[26px] border border-slate-900/10 bg-white p-8 shadow-atelier sm:p-10">
          <div className="mb-7 lg:hidden">
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-amber-400 to-amber-500 text-[#0b1120] shadow-md">
              <GraduationCap size={23} />
            </span>
            <h1 className="font-display mt-4 text-xl font-extrabold tracking-wide text-slate-900">
              AETHERIA · EDU
            </h1>
            <p className="text-sm text-slate-500">Hệ thống quản lý đào tạo và đăng ký học phần</p>
          </div>

          <p className="font-mono text-[11px] font-semibold uppercase tracking-[0.18em] text-amber-700">
            Cổng xác thực
          </p>
          <h2 className="font-display mt-1 text-2xl font-bold text-slate-950">Đăng nhập hệ thống</h2>
          <p className="mt-1.5 text-sm text-slate-500">
            Sử dụng mã người dùng và mật khẩu được phòng đào tạo cấp.
          </p>

          <form className="mt-7 space-y-4" onSubmit={handleSubmit}>
            {notice && <Alert tone="warning">{notice}</Alert>}
            {error && <Alert tone="error">{error}</Alert>}

            <div>
              <label className="label" htmlFor="username">
                Tên đăng nhập
              </label>
              <div className="relative">
                <User size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  id="username"
                  className="input pl-10"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="VD: sv01"
                  autoComplete="username"
                  required
                />
              </div>
            </div>

            <div>
              <label className="label" htmlFor="password">
                Mật khẩu
              </label>
              <div className="relative">
                <Lock size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  id="password"
                  className="input px-10"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Nhập mật khẩu"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <button type="submit" className="btn-primary w-full py-2.5" disabled={submitting}>
              {submitting && <Loader2 size={16} className="animate-spin" />}
              {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
