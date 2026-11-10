import { useEffect, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  BookOpen,
  CalendarCheck,
  TrendingDown,
  GraduationCap,
  Info,
  ShieldCheck,
  LifeBuoy,
  Eye,
  X,
  FileText,
} from "lucide-react";
import { Link } from "react-router-dom";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";

const RULE_ICONS = {
  DIEM_QUA_TRINH: BookOpen,
  DIEM_DANH: CalendarCheck,
  GPA_GIAM: TrendingDown,
  TIEN_DO_TIN_CHI: GraduationCap,
};

const SEVERITY_BADGE = {
  THAP: "bg-blue-50 text-blue-700 border-blue-200",
  TRUNG_BINH: "bg-amber-50 text-amber-700 border-amber-200",
  CANH_BAO: "bg-orange-50 text-orange-700 border-orange-200",
  CAO: "bg-rose-50 text-rose-700 border-rose-200",
  NGHIEM_TRONG: "bg-red-50 text-red-800 border-red-300 font-semibold",
};

function formatDate(val) {
  if (!val) return "—";
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("vi-VN");
}

export default function StudentAcademicAlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedAlert, setSelectedAlert] = useState(null);

  useEffect(() => {
    fetchMyAlerts();
  }, []);

  const fetchMyAlerts = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await api.get("/api/sinh-vien/me/canh-bao-hoc-tap", {
        params: { page: 0, size: 50 },
      });
      setAlerts(res.data.content || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const activeAlerts = alerts.filter((a) => a.active);
  const resolvedAlerts = alerts.filter((a) => !a.active);

  const parseEvidence = (jsonStr) => {
    try {
      return JSON.parse(jsonStr || "{}");
    } catch {
      return {};
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto px-4 sm:px-6 py-6">
      <PageHeader
        title="Cảnh báo học tập sớm của bạn"
        description="Thông tin nhắc nhở học vụ và phát hiện sớm nguy cơ kết quả môn học / chuyên cần để bạn chủ động cải thiện."
      />

      {/* Safety & Advisory Notice */}
      <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-emerald-900 flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
        <div className="text-sm space-y-1">
          <p className="font-semibold text-emerald-800">
            Thông điệp đồng hành từ Phòng Đào tạo:
          </p>
          <p className="text-emerald-700 leading-relaxed">
            Các cảnh báo tại đây mang tính chất <strong>nhắc nhở và hỗ trợ học tập sớm</strong> dựa trên quy tắc tính toán
            minh bạch (điểm quá trình, số buổi vắng, tiến độ tín chỉ). Cảnh báo{" "}
            <strong>tuyệt đối không tự ý thay đổi trạng thái sinh viên</strong> và{" "}
            <strong>không làm ảnh hưởng đến quyền đăng nhập hay tài khoản của bạn</strong>. Khi dữ liệu điểm hoặc chuyên cần
            của bạn cải thiện, hệ thống sẽ tự động giải tỏa cảnh báo.
          </p>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError("")} />}

      {loading ? (
        <div className="py-16 text-center">
          <Spinner />
          <p className="mt-2 text-sm text-slate-500">Đang tải cảnh báo học vụ của bạn...</p>
        </div>
      ) : activeAlerts.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            icon={CheckCircle2}
            title="Tình hình học tập tốt!"
            description="Bạn hiện không có cảnh báo học tập nào đang kích hoạt. Hãy tiếp tục duy trì kết quả chuyên cần và điểm số nhé!"
          />

          {resolvedAlerts.length > 0 && (
            <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
              <h4 className="text-sm font-semibold text-slate-700">Lịch sử cảnh báo đã được giải tỏa ({resolvedAlerts.length})</h4>
              <div className="space-y-2">
                {resolvedAlerts.map((a) => (
                  <div key={a.id} className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs flex items-center justify-between">
                    <div>
                      <span className="font-medium text-slate-800">{a.tenLuat}</span>
                      <span className="text-slate-500 ml-2">({a.tenMonHoc || a.nguCanh})</span>
                    </div>
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-teal-50 text-teal-700 border border-teal-200">
                      Đã giải tỏa
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          <h3 className="text-base font-semibold text-slate-900 flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-orange-500" />
            Các cảnh báo cần lưu ý ({activeAlerts.length})
          </h3>

          <div className="space-y-3">
            {activeAlerts.map((alert) => {
              const Icon = RULE_ICONS[alert.loaiLuat] || AlertTriangle;
              return (
                <div
                  key={alert.id}
                  className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-3 hover:border-indigo-200 transition"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-lg bg-orange-100 text-orange-700">
                        <Icon className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="font-semibold text-slate-900 text-sm">{alert.tenLuat}</h4>
                        <div className="text-xs text-slate-500">
                          {alert.tenMonHoc ? `${alert.tenMonHoc} (${alert.maLopHocPhan})` : alert.nguCanh}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className={`px-2.5 py-0.5 rounded text-xs border ${SEVERITY_BADGE[alert.mucDo]}`}>
                        {alert.tenMucDo}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-50 text-red-700 border border-red-200">
                        {alert.tenTrangThai}
                      </span>
                    </div>
                  </div>

                  <p className="text-sm text-slate-700 leading-relaxed font-medium bg-orange-50/50 p-3 rounded-lg border border-orange-100">
                    {alert.lyDo}
                  </p>

                  <div className="flex flex-wrap items-center justify-between text-xs text-slate-500 pt-1 border-t border-slate-100">
                    <span>Thời điểm ghi nhận: {formatDate(alert.ngayTao)}</span>

                    <div className="flex items-center gap-2 mt-2 sm:mt-0">
                      <button
                        onClick={() => setSelectedAlert(alert)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        Xem cách tính & Dẫn chứng
                      </button>

                      <Link
                        to="/yeu-cau-ho-tro"
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-indigo-50 hover:bg-indigo-100 text-indigo-700 transition"
                      >
                        <LifeBuoy className="w-3.5 h-3.5" />
                        Cần tư vấn học vụ
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full shadow-2xl border border-slate-200 max-h-[85vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <div className="flex items-center gap-2">
                <Info className="w-5 h-5 text-indigo-600" />
                <h3 className="font-semibold text-slate-900 text-base">Giải thích cách tính cảnh báo</h3>
              </div>
              <button
                onClick={() => setSelectedAlert(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200/60 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4 overflow-y-auto flex-1 text-sm">
              <div>
                <h4 className="text-xs font-semibold text-slate-600 uppercase mb-1">Môn học / Nội dung:</h4>
                <div className="font-medium text-slate-900">{selectedAlert.tenMonHoc || selectedAlert.tenLuat}</div>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-slate-600 uppercase mb-1">Công thức & Cách tính:</h4>
                <div className="p-3 bg-slate-900 text-slate-100 rounded-lg text-xs font-mono leading-relaxed">
                  {selectedAlert.congThucGiaiThich}
                </div>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-slate-600 uppercase mb-1">Dữ liệu dẫn chứng:</h4>
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-xs overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <tbody>
                      {Object.entries(parseEvidence(selectedAlert.duLieuDanChung)).map(([k, v]) => (
                        <tr key={k} className="border-b border-slate-200/60 last:border-0">
                          <td className="py-1 pr-3 font-mono text-slate-500 font-semibold">{k}</td>
                          <td className="py-1 font-mono text-slate-900">{String(v)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="p-3 bg-indigo-50 border border-indigo-200 rounded-lg text-xs text-indigo-900">
                <strong>Lời khuyên cải thiện:</strong> Bạn có thể liên hệ trực tiếp với Giảng viên phụ trách lớp học phần
                hoặc Cố vấn học tập khoa để được hỗ trợ kế hoạch học tập, ôn tập cải thiện điểm quá trình hoặc khắc phục chuyên cần.
              </div>
            </div>

            <div className="px-6 py-3 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={() => setSelectedAlert(null)}
                className="px-4 py-2 rounded-lg text-sm font-medium bg-slate-800 text-white hover:bg-slate-900 transition"
              >
                Đã hiểu
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
