import { useState } from "react";
import {
  Bot,
  Calendar,
  CheckCircle2,
  Clock,
  HelpCircle,
  Lightbulb,
  MessageSquare,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import ChatbotWidget from "../components/ChatbotWidget";
import { Badge, PageHeader } from "../components/ui";
import { useAuth } from "../context/AuthContext";

export default function ChatbotPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      <PageHeader
        icon={Bot}
        title="Trợ lý AI Đăng ký Học phần"
        subtitle="Hệ thống trợ lý hội thoại tiếng Việt tư vấn lớp học, kiểm tra sĩ số, thời khóa biểu và hỗ trợ đăng ký an toàn"
        badge={
          <Badge tone="green" dot>
            Trực tuyến
          </Badge>
        }
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Cột hướng dẫn & bảo đảm an toàn */}
        <div className="space-y-4 lg:col-span-1">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs">
            <h3 className="flex items-center gap-2 text-sm font-bold text-slate-900">
              <ShieldCheck size={18} className="text-emerald-600" />
              Nguyên tắc an toàn dữ liệu
            </h3>
            <ul className="mt-3 space-y-2.5 text-xs text-slate-600">
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="mt-0.5 shrink-0 text-emerald-500" />
                <span>
                  <strong>Dữ liệu thời gian thực:</strong> Tất cả sĩ số, phòng học, lịch và giảng viên lấy trực tiếp từ CSDL đào tạo.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="mt-0.5 shrink-0 text-emerald-500" />
                <span>
                  <strong>Xác nhận rõ ràng:</strong> Hệ thống tuyệt đối không tự ý đăng ký lớp mà luôn yêu cầu bạn xác nhận rõ ràng trước khi ghi danh.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="mt-0.5 shrink-0 text-emerald-500" />
                <span>
                  <strong>Bảo vệ phân quyền:</strong> Tự động chặn các yêu cầu trái quyền, không làm lộ điểm hoặc dữ liệu của người khác.
                </span>
              </li>
            </ul>
          </div>

          <div className="rounded-2xl border border-slate-200/80 bg-gradient-to-br from-emerald-50/50 to-sky-50/50 p-5 shadow-xs">
            <h3 className="flex items-center gap-2 text-sm font-bold text-[#173f3a]">
              <Lightbulb size={18} className="text-amber-500" />
              Mẹo đặt câu hỏi hiệu quả
            </h3>
            <div className="mt-3 space-y-2 text-xs text-slate-600">
              <div className="rounded-xl border border-slate-200/60 bg-white p-2.5">
                <p className="font-semibold text-slate-800">1. Tra cứu lớp còn chỗ</p>
                <p className="italic text-slate-500">"Tìm các lớp môn Lập trình Java còn chỗ học thứ 4"</p>
              </div>
              <div className="rounded-xl border border-slate-200/60 bg-white p-2.5">
                <p className="font-semibold text-slate-800">2. Xem lịch học cá nhân</p>
                <p className="italic text-slate-500">"Xem thời khóa biểu của tôi"</p>
              </div>
              <div className="rounded-xl border border-slate-200/60 bg-white p-2.5">
                <p className="font-semibold text-slate-800">3. Đăng ký học phần</p>
                <p className="italic text-slate-500">"Tôi muốn đăng ký lớp INT201.01"</p>
              </div>
              <div className="rounded-xl border border-slate-200/60 bg-white p-2.5">
                <p className="font-semibold text-slate-800">4. Tìm lớp thay thế khi đầy</p>
                <p className="italic text-slate-500">"Lớp INT201.01 đầy rồi, có lớp nào thay thế không?"</p>
              </div>
            </div>
          </div>
        </div>

        {/* Khung chat toàn màn hình */}
        <div className="relative min-h-[640px] lg:col-span-2">
          <ChatbotWidget isOpenControlled={true} onCloseControlled={() => {}} defaultExpanded={true} />
        </div>
      </div>
    </div>
  );
}
