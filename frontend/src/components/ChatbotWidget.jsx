import { useEffect, useRef, useState } from "react";
import {
  AlertCircle,
  Bot,
  Calendar,
  CheckCircle2,
  Clock,
  CornerDownLeft,
  HelpCircle,
  Loader2,
  MapPin,
  Maximize2,
  Minimize2,
  RotateCcw,
  Send,
  ShieldAlert,
  Sparkles,
  User,
  Users,
  X,
} from "lucide-react";
import api, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Badge } from "./ui";

const QUICK_SUGGESTIONS = {
  SINH_VIEN: [
    "Tìm các lớp học phần còn chỗ",
    "Xem thời khóa biểu của tôi",
    "Lớp INT201.01 còn bao nhiêu chỗ?",
    "Có lớp nào thay thế không?",
    "Hướng dẫn trợ giúp",
  ],
  GIANG_VIEN: [
    "Xem danh sách lớp tôi phụ trách",
    "Lịch giảng dạy của tôi",
    "Hướng dẫn trợ giúp",
  ],
  QUAN_TRI: [
    "Xem thống kê tổng quan đào tạo",
    "Tra cứu danh sách lớp học phần",
    "Hướng dẫn trợ giúp",
  ],
};

export default function ChatbotWidget({ isOpenControlled, onCloseControlled, defaultExpanded = false }) {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(defaultExpanded);
  const [isMaximized, setIsMaximized] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [confirmingId, setConfirmingId] = useState(null);
  const [status, setStatus] = useState(null);
  const [sessionId] = useState(() => "session-" + Math.random().toString(36).slice(2, 10));
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const activeOpen = isOpenControlled !== undefined ? isOpenControlled : isOpen;
  const handleClose = () => {
    if (onCloseControlled) onCloseControlled();
    else setIsOpen(false);
  };

  useEffect(() => {
    // Lấy thông tin trạng thái provider
    api.get("/chatbot/status")
      .then((res) => setStatus(res.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (activeOpen && messages.length === 0) {
      setMessages([
        {
          id: "welcome",
          sender: "BOT",
          reply: `Xin chào ${user?.hoTen || user?.username}! Tôi là **Trợ lý AI Đăng ký Học phần (QLSV Assistant)**.\n\n` +
            `Dữ liệu lớp học phần, sĩ số và đăng ký được trích xuất trực tiếp từ CSDL thời gian thực với đầy đủ quyền hạn của bạn (${user?.role || "Sinh viên"}).\n` +
            `Tôi có thể hỗ trợ bạn tìm lớp, kiểm tra chỗ trống, xem lịch học và hướng dẫn đăng ký an toàn.`,
          timestamp: new Date().toISOString(),
          actionType: "NONE",
        },
      ]);
    }
  }, [activeOpen, user, messages.length]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const handleSendMessage = async (textToSend) => {
    const query = (textToSend || input).trim();
    if (!query || loading) return;

    const userMsg = {
      id: "user-" + Date.now(),
      sender: "USER",
      reply: query,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInput("");
    setLoading(true);

    try {
      const { data } = await api.post("/chatbot/message", {
        message: query,
        sessionId,
      });

      setMessages((prev) => [...prev, data]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          id: "err-" + Date.now(),
          sender: "BOT",
          reply: `⚠️ Không thể gửi câu hỏi: ${getErrorMessage(err)}`,
          timestamp: new Date().toISOString(),
          actionType: "ERROR",
        },
      ]);
    } finally {
      setLoading(false);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  };

  const handleConfirmRegistration = async (lopHocPhanId) => {
    setConfirmingId(lopHocPhanId);
    setLoading(true);
    try {
      const { data } = await api.post("/chatbot/confirm", {
        lopHocPhanId,
        sessionId,
      });
      setMessages((prev) => [...prev, data]);
      // Phát sự kiện để cập nhật lại danh sách đăng ký nếu đang ở trang đăng ký
      window.dispatchEvent(new Event("notifications:updated"));
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          id: "err-" + Date.now(),
          sender: "BOT",
          reply: `❌ Đăng ký thất bại: ${getErrorMessage(err)}`,
          timestamp: new Date().toISOString(),
          actionType: "REGISTRATION_FAILED",
        },
      ]);
    } finally {
      setConfirmingId(null);
      setLoading(false);
    }
  };

  const handleResetSession = async () => {
    try {
      await api.post("/chatbot/reset", null, { params: { sessionId } });
      setMessages([
        {
          id: "reset-" + Date.now(),
          sender: "BOT",
          reply: "Đã làm mới phiên trò chuyện và giải phóng lịch sử. Bạn có thể bắt đầu câu hỏi mới.",
          timestamp: new Date().toISOString(),
          actionType: "NONE",
        },
      ]);
    } catch (err) {
      console.error("Lỗi khi reset session:", err);
    }
  };

  const suggestions = QUICK_SUGGESTIONS[user?.role] || QUICK_SUGGESTIONS.SINH_VIEN;

  return (
    <>
      {/* Floating Action Button (khi đang đóng) */}
      {!activeOpen && (
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 z-40 flex items-center gap-2.5 rounded-full bg-gradient-to-r from-[#173f3a] to-[#2563eb] px-4 py-3.5 text-white shadow-xl shadow-slate-900/20 transition-all hover:scale-105 hover:shadow-2xl focus:outline-none focus:ring-4 focus:ring-emerald-400/40"
          title="Mở Trợ lý AI Đăng ký Học phần"
          aria-label="Mở Trợ lý AI"
        >
          <div className="relative">
            <Bot size={22} className="animate-pulse" />
            <span className="absolute -right-1 -top-1 flex h-2.5 w-2.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-500" />
            </span>
          </div>
          <span className="text-sm font-semibold tracking-wide">Trợ lý AI Đăng ký</span>
        </button>
      )}

      {/* Chat Window Panel / Drawer */}
      {activeOpen && (
        <div
          className={`fixed z-50 flex flex-col overflow-hidden border border-slate-200/90 bg-white shadow-2xl backdrop-blur-xl transition-all duration-300 ${
            isMaximized
              ? "inset-4 md:inset-8 rounded-2xl"
              : "bottom-4 right-4 h-[620px] max-h-[90vh] w-[420px] max-w-[95vw] rounded-2xl"
          }`}
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-200 bg-gradient-to-r from-[#173f3a] via-[#1b4b45] to-[#1e3a8a] px-4 py-3.5 text-white">
            <div className="flex items-center gap-3">
              <div className="grid h-10 w-10 place-items-center rounded-xl bg-white/10 ring-1 ring-white/20">
                <Sparkles size={20} className="text-amber-300" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h3 className="truncate text-sm font-bold text-white">Trợ lý AI Đăng ký</h3>
                  <span className="rounded-full bg-emerald-500/20 px-2 py-0.5 text-[10px] font-semibold text-emerald-300 ring-1 ring-emerald-400/30">
                    Live
                  </span>
                </div>
                <div className="flex items-center gap-1.5 text-[11px] text-slate-300">
                  <span>
                    {status?.configured
                      ? `Provider: ${status.model}`
                      : "Chế độ: CSDL thời gian thực"}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1 text-slate-300">
              <button
                type="button"
                onClick={handleResetSession}
                className="rounded-lg p-1.5 transition hover:bg-white/10 hover:text-white"
                title="Làm mới cuộc trò chuyện"
              >
                <RotateCcw size={16} />
              </button>
              <button
                type="button"
                onClick={() => setIsMaximized(!isMaximized)}
                className="hidden rounded-lg p-1.5 transition hover:bg-white/10 hover:text-white sm:inline-flex"
                title={isMaximized ? "Thu nhỏ" : "Phóng to"}
              >
                {isMaximized ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
              </button>
              <button
                type="button"
                onClick={handleClose}
                className="rounded-lg p-1.5 transition hover:bg-white/10 hover:text-white"
                title="Đóng cửa sổ"
              >
                <X size={18} />
              </button>
            </div>
          </div>

          {/* External Provider Status Notice (khi chưa cấu hình API key) */}
          {status && !status.configured && (
            <div className="flex items-center justify-between border-b border-amber-200/80 bg-amber-50/90 px-3.5 py-1.5 text-[11px] text-amber-900">
              <span className="flex items-center gap-1.5 truncate">
                <HelpCircle size={13} className="shrink-0 text-amber-700" />
                <span>AI Provider LLM chưa cấu hình API key; trợ lý dùng bộ phân tích quy chuẩn CSDL an toàn.</span>
              </span>
            </div>
          )}

          {/* Messages Container */}
          <div className="flex-1 space-y-4 overflow-y-auto bg-slate-50/70 p-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.sender === "USER" ? "items-end" : "items-start"}`}
              >
                <div className="flex items-end gap-2 max-w-[92%]">
                  {msg.sender === "BOT" && (
                    <div className="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-[#173f3a] text-white">
                      <Bot size={15} />
                    </div>
                  )}

                  <div
                    className={`rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm ${
                      msg.sender === "USER"
                        ? "bg-[#173f3a] text-white rounded-br-xs"
                        : msg.actionType === "SECURITY_BLOCKED"
                        ? "border border-rose-300 bg-rose-50 text-rose-950 rounded-bl-xs"
                        : "border border-slate-200/80 bg-white text-slate-800 rounded-bl-xs"
                    }`}
                  >
                    {/* Header thông tin thời điểm & học kỳ nếu có */}
                    {msg.sender === "BOT" && (msg.tenHocKy || msg.thoiDiemLayDuLieu) && (
                      <div className="mb-2 flex flex-wrap items-center gap-2 border-b border-slate-100 pb-1.5 text-[11px] text-slate-400">
                        {msg.tenHocKy && <span className="font-medium text-emerald-700">📚 {msg.tenHocKy}</span>}
                        {msg.thoiDiemLayDuLieu && (
                          <span className="flex items-center gap-1 text-slate-400">
                            <Clock size={11} /> {msg.thoiDiemLayDuLieu}
                          </span>
                        )}
                      </div>
                    )}

                    <div className="whitespace-pre-line">{msg.reply}</div>

                    {/* Hộp xác nhận đăng ký bắt buộc (CONFIRM_REGISTRATION) */}
                    {msg.pendingClass && msg.requiresConfirmation && (
                      <div className="mt-3.5 rounded-xl border border-amber-300 bg-amber-50/80 p-3.5 text-xs text-amber-950 shadow-xs">
                        <div className="flex items-center gap-2 font-bold text-amber-900">
                          <AlertCircle size={16} className="text-amber-700" />
                          <span>XÁC NHẬN ĐĂNG KÝ HỌC PHẦN</span>
                        </div>
                        <div className="mt-2 space-y-1 text-slate-700">
                          <p>
                            • Lớp: <strong className="text-slate-900">{msg.pendingClass.ma}</strong> - {msg.pendingClass.tenMon} ({msg.pendingClass.soTinChi} tín chỉ)
                          </p>
                          <p>• Giảng viên: {msg.pendingClass.giangVien} | Phòng: {msg.pendingClass.phong}</p>
                          <p>• Lịch học: Thứ {msg.pendingClass.thu}, tiết {msg.pendingClass.tietBatDau} - {msg.pendingClass.tietBatDau + msg.pendingClass.soTiet - 1}</p>
                          <p>
                            • Chỗ còn lại: <strong>{msg.pendingClass.conLai} / {msg.pendingClass.siSoToiDa}</strong>
                          </p>
                        </div>
                        <p className="mt-2 text-[11px] text-amber-800">
                          ⚠️ Hệ thống chưa gọi API ghi danh cho tới khi bạn bấm xác nhận bên dưới.
                        </p>

                        <div className="mt-3 flex items-center gap-2 pt-1">
                          <button
                            type="button"
                            disabled={loading || confirmingId === msg.pendingClass.id}
                            onClick={() => handleConfirmRegistration(msg.pendingClass.id)}
                            className="flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3.5 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-emerald-700 disabled:opacity-50"
                          >
                            {confirmingId === msg.pendingClass.id ? (
                              <Loader2 size={13} className="animate-spin" />
                            ) : (
                              <CheckCircle2 size={13} />
                            )}
                            Xác nhận đăng ký
                          </button>
                          <button
                            type="button"
                            disabled={loading}
                            onClick={() => handleSendMessage("Hủy")}
                            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-50"
                          >
                            Hủy bỏ
                          </button>
                        </div>
                      </div>
                    )}

                    {/* Danh sách thẻ lớp ứng viên (Candidate Classes) */}
                    {msg.candidateClasses && msg.candidateClasses.length > 0 && !msg.requiresConfirmation && (
                      <div className="mt-3 space-y-2">
                        {msg.candidateClasses.slice(0, 5).map((lop, idx) => (
                          <div
                            key={lop.id}
                            className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 shadow-xs"
                          >
                            <div className="min-w-0 flex-1">
                              <p className="font-bold text-[#173f3a]">
                                {idx + 1}. {lop.ma} · {lop.tenMon}
                              </p>
                              <p className="text-[11px] text-slate-500">
                                GV: {lop.giangVien} | T{lop.thu} (tiết {lop.tietBatDau}-{lop.tietBatDau + lop.soTiet - 1}) | P.{lop.phong}
                              </p>
                              <p className="text-[11px] font-medium text-emerald-700">
                                Sĩ số: {lop.siSoHienTai}/{lop.siSoToiDa} (còn {lop.conLai} chỗ)
                              </p>
                            </div>

                            {user?.role === "SINH_VIEN" && (
                              <button
                                type="button"
                                disabled={loading || lop.conLai <= 0}
                                onClick={() => handleSendMessage(`Tôi muốn đăng ký lớp ${lop.ma}`)}
                                className="shrink-0 rounded-lg bg-[#173f3a] px-2.5 py-1.5 text-[11px] font-semibold text-white shadow-xs transition hover:bg-emerald-800 disabled:bg-slate-200 disabled:text-slate-400"
                              >
                                Chọn lớp
                              </button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex items-center gap-2 text-xs text-slate-500">
                <div className="grid h-7 w-7 place-items-center rounded-lg bg-[#173f3a] text-white">
                  <Bot size={15} />
                </div>
                <div className="flex items-center gap-1.5 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-slate-600 shadow-xs">
                  <Loader2 size={14} className="animate-spin text-emerald-600" />
                  <span>Trợ lý đang tra cứu dữ liệu đào tạo...</span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Suggestions */}
          <div className="border-t border-slate-200/60 bg-white/95 px-3 py-2">
            <div className="flex gap-1.5 overflow-x-auto pb-1 text-xs scrollbar-none">
              {suggestions.map((item, idx) => (
                <button
                  key={idx}
                  type="button"
                  disabled={loading}
                  onClick={() => handleSendMessage(item)}
                  className="shrink-0 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-slate-600 transition hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-800 disabled:opacity-40"
                >
                  {item}
                </button>
              ))}
            </div>
          </div>

          {/* Input Form */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex items-center gap-2 border-t border-slate-200 bg-white p-3"
          >
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Hỏi về lớp, lịch, phòng, sĩ số hoặc đăng ký..."
              disabled={loading}
              className="flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-emerald-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500/20 disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-to-r from-[#173f3a] to-emerald-700 text-white shadow-md transition hover:opacity-95 disabled:opacity-40"
              title="Gửi câu hỏi"
            >
              <Send size={16} />
            </button>
          </form>
        </div>
      )}
    </>
  );
}
