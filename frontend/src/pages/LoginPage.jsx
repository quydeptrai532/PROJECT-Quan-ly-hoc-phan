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
    <div className="grid min-h-screen bg-[#f3f5f0] lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-[#173f3a] p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full opacity-30 blur-3xl"
          style={{ background: "radial-gradient(circle, #d9ed8a 0%, transparent 70%)" }}
        />
        <div
          className="pointer-events-none absolute -bottom-32 -left-20 h-96 w-96 rounded-full opacity-20 blur-3xl"
          style={{ background: "radial-gradient(circle, #8fb69b 0%, transparent 70%)" }}
        />

        <div className="relative flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-full bg-[#d9ed8a] text-[#173f3a]">
            <GraduationCap size={24} />
          </span>
          <div>
            <p className="text-lg font-semibold">MỘC · CAMPUS</p>
            <p className="text-xs text-[#b4c9bd]">Hệ thống quản lý đào tạo</p>
          </div>
        </div>

        <div className="relative max-w-md">
          <p className="text-sm font-medium uppercase tracking-widest text-[#d9ed8a]">Học kỳ mới</p>
          <h1 className="mt-3 text-3xl font-semibold leading-tight">
            Đăng ký học phần nhanh chóng, quản lý kết quả học tập minh bạch.
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-300">
            Hệ thống hỗ trợ phòng đào tạo quản lý chương trình, mở lớp học phần; sinh viên đăng ký trực
            tuyến; giảng viên nhập điểm và theo dõi lớp được phân công.
          </p>
          <ul className="mt-8 space-y-4 text-sm text-slate-200">
            <li className="flex items-start gap-3">
              <BookOpen size={18} className="mt-0.5 text-[#d9ed8a]" />
              Kiểm tra tự động môn tiên quyết, trùng lịch và sĩ số lớp.
            </li>
            <li className="flex items-start gap-3">
              <ShieldCheck size={18} className="mt-0.5 text-[#d9ed8a]" />
              Phân quyền theo vai trò, xác thực bằng JWT an toàn.
            </li>
            <li className="flex items-start gap-3">
              <GraduationCap size={18} className="mt-0.5 text-[#d9ed8a]" />
              Xem điểm, GPA học kỳ và điểm tích lũy tức thời.
            </li>
          </ul>

          <div className="mt-9 grid grid-cols-3 gap-3">
            {[
              { value: "3", label: "Vai trò" },
              { value: "JWT", label: "Xác thực" },
              { value: "24/7", label: "Truy cập" },
            ].map((item) => (
              <div
                key={item.label}
                className="rounded-xl bg-white/5 px-3 py-3 text-center ring-1 ring-white/10 backdrop-blur"
              >
                <p className="text-lg font-semibold text-white">{item.value}</p>
                <p className="mt-0.5 text-[11px] uppercase tracking-wider text-slate-400">{item.label}</p>
              </div>
            ))}
          </div>
        </div>

        <p className="relative text-xs text-slate-500">
          Đồ án quản lý đào tạo · Spring Boot + ReactJS
        </p>
      </div>

      <div className="flex items-center justify-center px-5 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <span className="grid h-11 w-11 place-items-center rounded-full bg-[#d9ed8a] text-[#173f3a]">
              <GraduationCap size={24} />
            </span>
            <h1 className="mt-4 text-xl font-semibold text-slate-900">Mộc · Campus</h1>
            <p className="text-sm text-slate-500">Hệ thống quản lý đào tạo và đăng ký học phần</p>
          </div>

          <h2 className="text-2xl font-semibold text-slate-900">Đăng nhập</h2>
          <p className="mt-1 text-sm text-slate-500">
            Sử dụng mã người dùng và mật khẩu được phòng đào tạo cấp.
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
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
