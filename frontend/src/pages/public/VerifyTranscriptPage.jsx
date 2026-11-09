import { useEffect, useState } from "react";
import { useLocation, useParams, Link } from "react-router-dom";
import {
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Search,
  ShieldCheck,
  Building2,
  GraduationCap,
  Calendar,
  Award,
  Hash,
  ExternalLink,
  Printer,
  RefreshCw,
  FileText,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";

export default function VerifyTranscriptPage() {
  const { code: routeCode } = useParams();
  const location = useLocation();

  const queryParams = new URLSearchParams(location.search);
  const initialCode =
    routeCode ||
    queryParams.get("ma") ||
    queryParams.get("code") ||
    "";

  const [inputCode, setInputCode] = useState(initialCode);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");
  const [isRateLimited, setIsRateLimited] = useState(false);

  const performVerification = async (codeToVerify) => {
    const trimmed = (codeToVerify || "").trim();
    if (!trimmed) {
      setErrorMsg("Vui lòng nhập mã xác minh bảng điểm.");
      setResult(null);
      return;
    }

    setLoading(true);
    setErrorMsg("");
    setIsRateLimited(false);
    setResult(null);

    try {
      const response = await api.get(
        `/public/bang-diem/xac-thuc/${encodeURIComponent(trimmed)}`
      );
      setResult(response.data);
    } catch (err) {
      const status = err?.response?.status;
      if (status === 429) {
        setIsRateLimited(true);
        setErrorMsg(
          "Bạn đã gửi quá nhiều yêu cầu xác thực trong thời gian ngắn. Vui lòng đợi 1 phút trước khi thử lại."
        );
      } else if (status === 404 || status === 400) {
        setErrorMsg(
          "Mã xác minh không hợp lệ hoặc không tìm thấy trên hệ thống chứng thực của nhà trường. Vui lòng kiểm tra lại ký tự hoặc quét lại mã QR trên bảng điểm chính thức."
        );
      } else {
        setErrorMsg(getErrorMessage(err));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initialCode) {
      setInputCode(initialCode);
      performVerification(initialCode);
    }
  }, [initialCode]);

  const handleSubmit = (e) => {
    e.preventDefault();
    performVerification(inputCode);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 via-white to-slate-100 text-slate-800">
      {/* Header bar */}
      <header className="border-b border-slate-200 bg-white/90 backdrop-blur sticky top-0 z-20">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-100">
              <ShieldCheck size={22} />
            </div>
            <div>
              <h1 className="text-base sm:text-lg font-bold text-slate-900 leading-tight">
                Cổng Xác Minh Bảng Điểm Điện Tử
              </h1>
              <p className="text-xs text-slate-500">
                Hệ thống Quản lý Đào tạo & Khảo thí Đại học
              </p>
            </div>
          </div>
          <Link
            to="/login"
            className="text-xs font-medium text-indigo-600 hover:text-indigo-800 transition px-3 py-1.5 rounded-lg border border-indigo-200 hover:border-indigo-300"
          >
            Đăng nhập hệ thống &rarr;
          </Link>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 py-8">
        {/* Verification Form Card */}
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
          <div className="mb-4">
            <h2 className="text-base font-semibold text-slate-800">
              Tra cứu tính xác thực bảng điểm
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Nhập mã định danh xác minh (dạng <code className="bg-slate-100 px-1 py-0.5 rounded text-indigo-700">VT-XXXX-XXXX-...</code>) được in dưới góc trang hoặc quét từ mã QR trên bảng điểm PDF.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <input
                type="text"
                value={inputCode}
                onChange={(e) => setInputCode(e.target.value.toUpperCase())}
                placeholder="Nhập mã xác minh (VT-...)"
                className="w-full rounded-xl border border-slate-300 px-4 py-2.5 pl-10 text-sm font-mono tracking-wider focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100"
                disabled={loading}
              />
              <Search
                size={18}
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
              />
            </div>
            <button
              type="submit"
              disabled={loading || !inputCode.trim()}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 disabled:opacity-50 transition"
            >
              {loading ? (
                <>
                  <RefreshCw size={16} className="animate-spin" />
                  Đang kiểm tra...
                </>
              ) : (
                <>
                  <ShieldCheck size={16} />
                  Xác thực ngay
                </>
              )}
            </button>
          </form>
        </section>

        {/* Error / Rate limit Notice */}
        {errorMsg && (
          <div className={`rounded-2xl border p-5 mb-6 shadow-sm ${
            isRateLimited
              ? "bg-amber-50 border-amber-200 text-amber-900"
              : "bg-rose-50 border-rose-200 text-rose-900"
          }`}>
            <div className="flex items-start gap-3">
              <span className={`p-2 rounded-xl shrink-0 ${isRateLimited ? "bg-amber-100 text-amber-700" : "bg-rose-100 text-rose-700"}`}>
                <AlertTriangle size={20} />
              </span>
              <div>
                <h3 className="text-sm font-semibold mb-1">
                  {isRateLimited ? "Giới hạn tốc độ tra cứu" : "Không thể xác minh bảng điểm"}
                </h3>
                <p className="text-xs leading-relaxed opacity-90">{errorMsg}</p>
                <p className="text-[11px] text-slate-500 mt-2">
                  * Nhằm đảm bảo an toàn thông tin và quyền riêng tư, hệ thống áp dụng cơ chế chống quét mã tự động và không cung cấp dữ liệu cá nhân cho các truy vấn không hợp lệ.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Verification Result */}
        {result && (
          <div className="space-y-6">
            {/* Status 1: HIEU_LUC (Valid) */}
            {result.hopLe && result.trangThai === "HIEU_LUC" && (
              <div className="bg-white rounded-2xl border border-emerald-200 shadow-sm overflow-hidden">
                <div className="bg-emerald-600 text-white px-6 py-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <CheckCircle2 size={24} className="text-emerald-200" />
                    <div>
                      <h3 className="font-bold text-base">BẢNG ĐIỂM HỢP LỆ VÀ CÓ HIỆU LỰC</h3>
                      <p className="text-xs text-emerald-100">
                        Chứng thực điện tử chính thức từ cơ sở dữ liệu nhà trường
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handlePrint}
                    className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-xs font-medium transition"
                  >
                    <Printer size={14} />
                    In chứng nhận
                  </button>
                </div>

                <div className="p-6 space-y-6">
                  {/* Student Info */}
                  <div>
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                      <GraduationCap size={15} />
                      Thông tin sinh viên & Đào tạo
                    </h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100">
                      <div>
                        <span className="text-xs text-slate-500 block">Họ và tên</span>
                        <span className="text-sm font-semibold text-slate-900">{result.hoTen}</span>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 block">Mã số sinh viên</span>
                        <span className="text-sm font-semibold text-slate-900">{result.maSinhVien}</span>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 block">Khoa</span>
                        <span className="text-sm text-slate-700">{result.tenKhoa || "—"}</span>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 block">Ngành đào tạo</span>
                        <span className="text-sm text-slate-700">{result.tenNganh || "—"}</span>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 block">Lớp sinh hoạt</span>
                        <span className="text-sm text-slate-700">{result.tenLop || "—"}</span>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 block">Khóa học / Niên khóa</span>
                        <span className="text-sm text-slate-700">{result.nienKhoa || "—"}</span>
                      </div>
                    </div>
                  </div>

                  {/* Certified Metrics */}
                  <div>
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                      <Award size={15} />
                      Kết quả học tập đã được chứng thực
                    </h4>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
                        <span className="text-xs text-slate-500 block">Phạm vi</span>
                        <span className="text-sm font-semibold text-slate-800 mt-1 block">
                          {result.hocKyId ? `${result.tenHocKy} (${result.namHoc})` : "Toàn khóa"}
                        </span>
                      </div>
                      <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
                        <span className="text-xs text-slate-500 block">Số môn đã công bố</span>
                        <span className="text-base font-bold text-slate-900 mt-1 block">
                          {result.soMonDaCongBo}
                        </span>
                      </div>
                      <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
                        <span className="text-xs text-slate-500 block">Tín chỉ tích lũy</span>
                        <span className="text-base font-bold text-indigo-600 mt-1 block">
                          {result.tongTinChiTichLuy}
                        </span>
                      </div>
                      <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
                        <span className="text-xs text-slate-500 block">GPA (Thang 4 / 10)</span>
                        <span className="text-base font-bold text-emerald-600 mt-1 block">
                          {result.gpa4 !== null ? result.gpa4.toFixed(2) : "—"} / {result.gpa10 !== null ? result.gpa10.toFixed(2) : "—"}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Metadata & Integrity Check */}
                  <div className="border-t border-slate-100 pt-4">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                      <Hash size={15} />
                      Thông tin phát hành & Toàn vẹn dữ liệu
                    </h4>
                    <div className="space-y-2 text-xs text-slate-600 bg-slate-50/70 p-3.5 rounded-xl border border-slate-100">
                      <div className="flex justify-between flex-wrap gap-1">
                        <span className="text-slate-500">Phiên bản phát hành:</span>
                        <span className="font-semibold text-slate-800">Phiên bản v{result.phienBan}</span>
                      </div>
                      <div className="flex justify-between flex-wrap gap-1">
                        <span className="text-slate-500">Thời điểm phát hành:</span>
                        <span className="font-medium text-slate-800">{result.thoiDiemPhatHanhFormatted || result.thoiDiemPhatHanh}</span>
                      </div>
                      <div className="flex justify-between flex-wrap gap-1">
                        <span className="text-slate-500">Mã băm toàn vẹn (SHA-256 Checksum):</span>
                        <span className="font-mono text-[11px] text-slate-700 break-all">{result.checksumSha256}</span>
                      </div>
                    </div>
                  </div>

                  {/* Privacy Policy Banner */}
                  <div className="rounded-xl border border-sky-100 bg-sky-50/60 p-3.5 text-xs text-sky-900 leading-relaxed">
                    <div className="flex items-start gap-2">
                      <FileText size={16} className="text-sky-600 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-semibold">Chính sách bảo mật điểm số:</span> Căn cứ quy định bảo vệ dữ liệu học tập và quyền riêng tư cá nhân, trang xác minh công khai chỉ hiển thị các chỉ số tổng hợp đã được thẩm định. Điểm chi tiết từng học phần chỉ cung cấp cho người học hoặc cơ quan được ủy quyền hợp pháp.
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Status 2: DA_THU_HOI (Revoked) */}
            {result.trangThai === "DA_THU_HOI" && (
              <div className="bg-white rounded-2xl border border-rose-300 shadow-sm overflow-hidden">
                <div className="bg-rose-600 text-white px-6 py-4 flex items-center gap-3">
                  <XCircle size={24} className="text-rose-200" />
                  <div>
                    <h3 className="font-bold text-base">BẢNG ĐIỂM ĐÃ BỊ THU HỒI</h3>
                    <p className="text-xs text-rose-100">
                      Văn bản này không còn giá trị chứng thực học tập
                    </p>
                  </div>
                </div>
                <div className="p-6 space-y-4">
                  <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-900 text-sm">
                    <p className="font-semibold mb-1">Cảnh báo vô hiệu lực:</p>
                    <p className="text-xs leading-relaxed">
                      Bảng điểm gắn với mã xác minh này đã bị Phòng Đào tạo thu hồi và huỷ bỏ giá trị sử dụng.
                    </p>
                    {result.thongDiep && (
                      <p className="text-xs mt-2 italic bg-white/70 p-2 rounded border border-rose-200">
                        Lý do: {result.thongDiep}
                      </p>
                    )}
                  </div>
                  <div className="text-xs text-slate-500 leading-relaxed">
                    * Vì lý do bảo mật và bảo vệ quyền riêng tư cá nhân, hệ thống không tiết lộ danh tính người học của các văn bản đã bị thu hồi. Vui lòng liên hệ Phòng Đào tạo nếu cần hỗ trợ.
                  </div>
                </div>
              </div>
            )}

            {/* Status 3: DA_THAY_THE (Replaced / Superseded) */}
            {result.trangThai === "DA_THAY_THE" && (
              <div className="bg-white rounded-2xl border border-amber-300 shadow-sm overflow-hidden">
                <div className="bg-amber-600 text-white px-6 py-4 flex items-center gap-3">
                  <AlertTriangle size={24} className="text-amber-200" />
                  <div>
                    <h3 className="font-bold text-base">BẢNG ĐIỂM ĐÃ HẾT HIỆU LỰC (ĐÃ ĐƯỢC CẤP LẠI)</h3>
                    <p className="text-xs text-amber-100">
                      Phiên bản này đã được thay thế bởi phiên bản mới hơn
                    </p>
                  </div>
                </div>
                <div className="p-6 space-y-4">
                  <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-sm">
                    <p className="font-semibold mb-1">Phiên bản đã thay đổi:</p>
                    <p className="text-xs leading-relaxed">
                      Bản ghi bảng điểm này đã được nhà trường cấp lại phiên bản mới hơn (do điều chỉnh học phần, bổ sung điểm hoặc cấp lại theo yêu cầu). Bản in này không còn hiệu lực hiện hành.
                    </p>
                  </div>
                  <div className="text-xs text-slate-500 leading-relaxed">
                    * Vui lòng sử dụng bản in mới nhất kèm mã xác minh của phiên bản thay thế để đối soát.
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="mt-16 border-t border-slate-200 bg-white py-6 text-center text-xs text-slate-500">
        <p className="max-w-2xl mx-auto px-4">
          Hệ thống Chứng thực Bảng điểm Điện tử tuân thủ quy định chữ ký số và bảo mật dữ liệu học tập. Mọi hành vi giả mạo hoặc làm sai lệch nội dung bảng điểm sẽ bị xử lý theo quy định của pháp luật.
        </p>
      </footer>
    </div>
  );
}