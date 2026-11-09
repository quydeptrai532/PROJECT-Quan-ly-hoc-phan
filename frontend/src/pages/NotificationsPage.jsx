import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Check, CheckCheck, ChevronLeft, ChevronRight, Clock3, ExternalLink } from "lucide-react";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../components/ui";
import { useApi } from "../lib/useApi";
import api, { getErrorMessage } from "../api/client";

const DATE_FORMAT = new Intl.DateTimeFormat("vi-VN", {
  dateStyle: "medium",
  timeStyle: "short",
});

const EVENT_TYPE_LABELS = {
  COURSE_REGISTRATION_CONFIRMED: "Đăng ký học phần thành công",
  COURSE_REGISTRATION_CANCELLED: "Hủy đăng ký học phần",
  COURSE_SECTION_CANCELLED: "Lớp học phần bị hủy",
  COURSE_SESSION_CANCELLED: "Buổi học tạm nghỉ",
  COURSE_SECTION_SCHEDULE_CHANGED: "Thay đổi lịch học",
  COURSE_SECTION_ASSIGNMENT_CHANGED: "Thay đổi giảng viên",
  REGISTRATION_WINDOW_CLOSING: "Nhắc hạn đăng ký",
  GRADES_PUBLISHED: "Công bố điểm lớp học phần",
  CLASS_ANNOUNCEMENT: "Thông báo lớp học phần",
  CLASS_STUDENT_TRANSFERRED: "Chuyển sinh viên sang lớp khác",
};

export default function NotificationsPage() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const navigate = useNavigate();
  const { data, loading, error, reload } = useApi("/notifications", {
    unreadOnly,
    page,
    size: 20,
  });

  const notifyChanged = () => window.dispatchEvent(new Event("notifications:updated"));

  const markRead = async (notification) => {
    if (notification.read) return true;
    setBusyId(notification.id);
    setActionError("");
    try {
      await api.patch(`/notifications/${notification.id}/read`);
      await reload();
      notifyChanged();
      return true;
    } catch (err) {
      setActionError(getErrorMessage(err));
      return false;
    } finally {
      setBusyId(null);
    }
  };

  const openNotification = async (notification) => {
    if (!(await markRead(notification))) return;
    if (notification.targetUrl?.startsWith("/") && !notification.targetUrl.startsWith("//")) {
      navigate(notification.targetUrl);
    }
  };

  const markAllRead = async () => {
    setActionError("");
    try {
      await api.patch("/notifications/read-all");
      await reload();
      notifyChanged();
    } catch (err) {
      setActionError(getErrorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title="Trung tâm thông báo"
        description="Cập nhật liên quan đến lớp học phần và đợt đăng ký của bạn."
        icon={Bell}
        actions={data?.unreadCount > 0 ? (
          <button type="button" className="btn-secondary" onClick={markAllRead}>
            <CheckCheck size={16} /> Đánh dấu tất cả đã đọc
          </button>
        ) : null}
      />

      <div className="mb-5 flex flex-wrap items-center gap-2">
        <button
          type="button"
          className={unreadOnly ? "btn-secondary" : "btn-primary"}
          onClick={() => { setUnreadOnly(false); setPage(0); }}
          aria-pressed={!unreadOnly}
        >
          Tất cả
        </button>
        <button
          type="button"
          className={unreadOnly ? "btn-primary" : "btn-secondary"}
          onClick={() => { setUnreadOnly(true); setPage(0); }}
          aria-pressed={unreadOnly}
        >
          Chưa đọc {data?.unreadCount > 0 && <span className="ml-1">({data.unreadCount})</span>}
        </button>
      </div>

      {actionError && <Alert tone="error" className="mb-4">Không cập nhật được thông báo: {actionError}</Alert>}
      {error && <Alert tone="error" className="mb-4">Không tải được thông báo: {error}</Alert>}
      {loading ? <Spinner label="Đang tải thông báo..." /> : error ? (
        <button type="button" className="btn-secondary" onClick={reload}>Thử tải lại</button>
      ) : data?.content?.length ? (
        <div className="space-y-3">
          {data.content.map((notification) => (
            <article
              key={notification.id}
              className={`rounded-2xl border p-4 shadow-sm transition sm:p-5 ${
                notification.read ? "border-slate-200 bg-white" : "border-[#cbd9bb] bg-[#f5f8f0]"
              }`}
            >
              <div className="flex items-start gap-3">
                <span className={`mt-0.5 grid h-10 w-10 shrink-0 place-items-center rounded-xl ${
                  notification.read ? "bg-slate-100 text-slate-500" : "bg-[#e5efd8] text-[#54734d]"
                }`}>
                  <Bell size={18} />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-semibold text-slate-900">{notification.title}</h2>
                    {!notification.read && <Badge tone="green">Chưa đọc</Badge>}
                    {notification.priority === "HIGH" && <Badge tone="amber">Ưu tiên cao</Badge>}
                  </div>
                  <p className="mt-1 text-sm leading-relaxed text-slate-600">{notification.message}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-500">
                    <span className="inline-flex items-center gap-1.5">
                      <Clock3 size={13} /> {DATE_FORMAT.format(new Date(notification.createdAt))}
                    </span>
                    <span>
                      {EVENT_TYPE_LABELS[notification.eventType]
                        ? `${EVENT_TYPE_LABELS[notification.eventType]} (${notification.eventType.replaceAll("_", " ")})`
                        : notification.eventType.replaceAll("_", " ")}
                    </span>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <button type="button" className="btn-secondary" onClick={() => openNotification(notification)}>
                      Mở nội dung <ExternalLink size={14} />
                    </button>
                    {!notification.read && (
                      <button type="button" className="btn-ghost" disabled={busyId === notification.id}
                        onClick={() => markRead(notification)}>
                        <Check size={15} /> Đánh dấu đã đọc
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </article>
          ))}
          <div className="flex items-center justify-between border-t border-slate-200 pt-4">
            <span className="text-xs text-slate-500">Trang {data.page + 1} / {Math.max(data.totalPages, 1)}</span>
            <div className="flex gap-2">
              <button type="button" className="btn-secondary" disabled={page <= 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}>
                <ChevronLeft size={16} /> Trước
              </button>
              <button type="button" className="btn-secondary" disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((value) => value + 1)}>
                Tiếp <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <EmptyState
            icon={Bell}
            title={unreadOnly ? "Bạn đã xem hết thông báo mới" : "Chưa có thông báo"}
            description={unreadOnly
              ? "Các cập nhật mới về lớp học phần và đợt đăng ký sẽ xuất hiện ở đây."
              : "Khi có thay đổi liên quan đến lớp học phần hoặc đăng ký, thông báo sẽ xuất hiện ở đây."}
          />
        </div>
      )}
    </div>
  );
}
