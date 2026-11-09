
import { useEffect, useState } from "react";
import {
    AlertTriangle,
    CheckCircle2,
    RefreshCw,
    Settings2,
    Search,
    Eye,
    X,
    ShieldAlert,
    Clock,
} from "lucide-react";

import api, { getErrorMessage } from "../../api/client";

import {
    Alert,
    Badge,
    EmptyState,
    PageHeader,
    Spinner,
} from "../../components/ui";

const severityLabel = {
    LOW: "Thấp",
    MEDIUM: "Trung bình",
    HIGH: "Cao",
    CRITICAL: "Nghiêm trọng",
};

const severityTone = {
    LOW: "sky",
    MEDIUM: "amber",
    HIGH: "red",
    CRITICAL: "red",
};

const ruleLabel = {
    LOGIN_FAILURES: "Đăng nhập thất bại nhiều lần",
    GRADE_BULK: "Thay đổi điểm hàng loạt",
    GRADE_AFTER_HOURS: "Thay đổi điểm ngoài giờ",
    REGISTRATION_REPEAT: "Đăng ký / hủy học phần lặp",
};

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "—";

    return date.toLocaleString("vi-VN");
}

export default function AuditAlertsPage() {
    const [status, setStatus] = useState("OPEN");

    const [page, setPage] = useState(null);
    const [pageNumber, setPageNumber] = useState(0);

    const [rules, setRules] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [busyId, setBusyId] = useState(null);
    const [savingRule, setSavingRule] = useState("");

    const [showRules, setShowRules] = useState(false);

    const [auditDetail, setAuditDetail] = useState(null);
    const [auditLoading, setAuditLoading] = useState(false);

    // Tải danh sách cảnh báo và cấu hình luật
    async function loadData() {
        setLoading(true);
        setError("");

        try {
            const [alertResponse, ruleResponse] = await Promise.all([
                api.get("/admin/audit-alerts", {
                    params: {
                        status: status || undefined,
                        page: pageNumber,
                        size: 10,
                    },
                }),

                api.get("/admin/audit-alerts/rules"),
            ]);

            setPage(alertResponse.data);
            setRules(ruleResponse.data || []);
        } catch (e) {
            setError(getErrorMessage(e));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadData();
    }, [status, pageNumber]);

    // Thay đổi trạng thái lọc
    function changeStatus(value) {
        setStatus(value);
        setPageNumber(0);
        setAuditDetail(null);
        setSuccess("");
    }

    // Đánh dấu cảnh báo đã xử lý
    async function resolveAlert(id) {
        const note = window.prompt(
            "Nhập ghi chú xử lý cảnh báo:",
            ""
        );

        if (note === null) return;

        setBusyId(id);
        setError("");
        setSuccess("");

        try {
            await api.post(
                `/admin/audit-alerts/${id}/resolve`,
                { note }
            );

            setSuccess("Đã xử lý cảnh báo thành công.");

            await loadData();
        } catch (e) {
            setError(getErrorMessage(e));
        } finally {
            setBusyId(null);
        }
    }

    // Xem chi tiết audit gốc
    async function viewAudit(id) {
        setAuditLoading(true);
        setAuditDetail(null);
        setError("");

        try {
            const response = await api.get(
                `/admin/audit-logs/${id}`
            );

            setAuditDetail(response.data);
        } catch (e) {
            setError(
                "Không thể tải audit gốc: " + getErrorMessage(e)
            );
        } finally {
            setAuditLoading(false);
        }
    }

    // Thay đổi giá trị cấu hình luật
    function updateRule(code, field, value) {
        setRules((current) =>
            current.map((rule) =>
                rule.code === code
                    ? { ...rule, [field]: value }
                    : rule
            )
        );
    }

    // Lưu cấu hình luật
    async function saveRule(rule) {
        if (
            !Number.isInteger(Number(rule.threshold)) ||
            Number(rule.threshold) < 1 ||
            Number(rule.threshold) > 10000
        ) {
            setError("Ngưỡng phải nằm trong khoảng 1 đến 10000.");
            return;
        }

        if (
            !Number.isInteger(Number(rule.windowMinutes)) ||
            Number(rule.windowMinutes) < 1 ||
            Number(rule.windowMinutes) > 10080
        ) {
            setError(
                "Cửa sổ thời gian phải nằm trong khoảng 1 đến 10080 phút."
            );
            return;
        }

        setSavingRule(rule.code);
        setError("");
        setSuccess("");

        try {
            await api.put(
                `/admin/audit-alerts/rules/${rule.code}`,
                {
                    ...rule,
                    threshold: Number(rule.threshold),
                    windowMinutes: Number(rule.windowMinutes),
                }
            );

            setSuccess(
                `Đã cập nhật luật ${rule.name} thành công.`
            );

            await loadData();
        } catch (e) {
            setError(getErrorMessage(e));
        } finally {
            setSavingRule("");
        }
    }

    return (
        <div className="space-y-6">

            {/* Tiêu đề */}
            <PageHeader
                icon={ShieldAlert}
                title="Cảnh báo bất thường"
                description="Theo dõi và xử lý các hành vi bất thường được phát hiện từ nhật ký audit của hệ thống."
            />

            {/* Thông báo */}
            {error && (
                <Alert tone="error">
                    {error}
                </Alert>
            )}

            {success && (
                <Alert tone="success">
                    {success}
                </Alert>
            )}

            {/* Bộ lọc và thao tác */}
            <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">

                <div className="flex flex-wrap items-center justify-between gap-3">

                    <div className="flex flex-wrap gap-2">

                        <button
                            type="button"
                            onClick={() => changeStatus("OPEN")}
                            className={`rounded-lg px-4 py-2 text-sm font-medium ${
                                status === "OPEN"
                                    ? "bg-indigo-600 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                            }`}
                        >
                            Chưa xử lý
                        </button>

                        <button
                            type="button"
                            onClick={() => changeStatus("RESOLVED")}
                            className={`rounded-lg px-4 py-2 text-sm font-medium ${
                                status === "RESOLVED"
                                    ? "bg-indigo-600 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                            }`}
                        >
                            Đã xử lý
                        </button>

                        <button
                            type="button"
                            onClick={() => changeStatus("")}
                            className={`rounded-lg px-4 py-2 text-sm font-medium ${
                                status === ""
                                    ? "bg-indigo-600 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                            }`}
                        >
                            Tất cả
                        </button>

                    </div>

                    <div className="flex flex-wrap gap-2">

                        <button
                            type="button"
                            onClick={() => setShowRules(!showRules)}
                            className="btn-secondary"
                        >
                            <Settings2 size={16} />
                            Cấu hình luật
                        </button>

                        <button
                            type="button"
                            onClick={loadData}
                            className="btn-secondary"
                            disabled={loading}
                        >
                            <RefreshCw size={16} />
                            Làm mới
                        </button>

                    </div>

                </div>

            </section>

            {/* Cấu hình luật phát hiện */}
            {showRules && (
                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

                    <div>
                        <h2 className="font-semibold text-slate-900">
                            Cấu hình luật phát hiện bất thường
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Quản trị viên có thể bật/tắt luật và thay đổi
                            ngưỡng phát hiện, cửa sổ thời gian.
                        </p>
                    </div>

                    {rules.length === 0 && !loading && (
                        <p className="text-sm text-slate-500">
                            Chưa có luật cảnh báo.
                        </p>
                    )}

                    {rules.map((rule) => (
                        <div
                            key={rule.code}
                            className="grid gap-4 border-t border-slate-100 pt-4 md:grid-cols-5 md:items-end"
                        >

                            <div>
                                <p className="text-sm font-semibold text-slate-800">
                                    {rule.name}
                                </p>

                                <p className="mt-1 text-xs text-slate-500">
                                    {rule.code}
                                </p>
                            </div>

                            <label className="text-sm text-slate-600">
                                Trạng thái

                                <select
                                    value={String(rule.enabled)}
                                    onChange={(e) =>
                                        updateRule(
                                            rule.code,
                                            "enabled",
                                            e.target.value === "true"
                                        )
                                    }
                                    className="input mt-1 w-full"
                                >
                                    <option value="true">Đang bật</option>
                                    <option value="false">Đang tắt</option>
                                </select>
                            </label>

                            <label className="text-sm text-slate-600">
                                Ngưỡng

                                <input
                                    type="number"
                                    min="1"
                                    max="10000"
                                    value={rule.threshold}
                                    onChange={(e) =>
                                        updateRule(
                                            rule.code,
                                            "threshold",
                                            Number(e.target.value)
                                        )
                                    }
                                    className="input mt-1 w-full"
                                />
                            </label>

                            <label className="text-sm text-slate-600">
                                Cửa sổ (phút)

                                <input
                                    type="number"
                                    min="1"
                                    max="10080"
                                    value={rule.windowMinutes}
                                    onChange={(e) =>
                                        updateRule(
                                            rule.code,
                                            "windowMinutes",
                                            Number(e.target.value)
                                        )
                                    }
                                    className="input mt-1 w-full"
                                />
                            </label>

                            <button
                                type="button"
                                onClick={() => saveRule(rule)}
                                disabled={savingRule === rule.code}
                                className="btn-primary"
                            >
                                {savingRule === rule.code
                                    ? "Đang lưu..."
                                    : "Lưu cấu hình"}
                            </button>

                        </div>
                    ))}

                </section>
            )}

            {/* Danh sách cảnh báo */}
            {loading ? (
                <Spinner label="Đang tải danh sách cảnh báo..." />
            ) : !error && page?.content?.length === 0 ? (

                <div className="rounded-2xl border border-slate-200 bg-white">
                    <EmptyState
                        icon={CheckCircle2}
                        title="Không có cảnh báo"
                        description="Hiện không có cảnh báo nào phù hợp với trạng thái đã chọn."
                    />
                </div>

            ) : !error && page && (

                <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-5 py-4">

                        <div>
                            <h2 className="font-semibold text-slate-900">
                                Danh sách cảnh báo
                            </h2>

                            <p className="mt-1 text-xs text-slate-500">
                                Tổng cộng {page.totalElements} cảnh báo
                            </p>
                        </div>

                        <Badge tone="indigo">
                            Chỉ quản trị viên
                        </Badge>

                    </div>

                    <div className="divide-y divide-slate-100">

                        {page.content.map((alert) => (

                            <article
                                key={alert.id}
                                className="space-y-4 p-5 transition hover:bg-slate-50/50"
                            >

                                {/* Thông tin chính */}
                                <div className="flex flex-wrap items-start justify-between gap-3">

                                    <div className="flex min-w-0 gap-3">

                                        <div className="rounded-xl bg-amber-50 p-3 text-amber-700">
                                            <AlertTriangle size={22} />
                                        </div>

                                        <div className="min-w-0">

                                            <h3 className="font-semibold text-slate-900">
                                                {alert.title}
                                            </h3>

                                            <p className="mt-1 text-xs text-slate-500">
                                                {ruleLabel[alert.ruleCode] || alert.ruleCode}
                                            </p>

                                            <p className="mt-1 text-sm text-slate-600">
                                                Đối tượng:{" "}
                                                <span className="font-medium">
                          {alert.subject}
                        </span>
                                            </p>

                                        </div>

                                    </div>

                                    <div className="flex flex-wrap gap-2">

                                        <Badge tone={severityTone[alert.severity] || "slate"}>
                                            {severityLabel[alert.severity] || alert.severity}
                                        </Badge>

                                        <Badge
                                            tone={
                                                alert.status === "RESOLVED"
                                                    ? "green"
                                                    : "amber"
                                            }
                                        >
                                            {alert.status === "RESOLVED"
                                                ? "Đã xử lý"
                                                : "Chưa xử lý"}
                                        </Badge>

                                    </div>

                                </div>

                                {/* Thời gian */}
                                <div className="grid gap-3 rounded-xl bg-slate-50 p-3 text-xs text-slate-600 md:grid-cols-3">

                                    <div>
                                        <p className="mb-1 flex items-center gap-1 text-slate-400">
                                            <Clock size={13} />
                                            Thời điểm phát hiện
                                        </p>

                                        <p className="font-medium">
                                            {formatDate(alert.occurredAt)}
                                        </p>
                                    </div>

                                    <div>
                                        <p className="mb-1 text-slate-400">
                                            Bắt đầu cửa sổ
                                        </p>

                                        <p className="font-medium">
                                            {formatDate(alert.windowStart)}
                                        </p>
                                    </div>

                                    <div>
                                        <p className="mb-1 text-slate-400">
                                            Kết thúc cửa sổ
                                        </p>

                                        <p className="font-medium">
                                            {formatDate(alert.windowEnd)}
                                        </p>
                                    </div>

                                </div>

                                {/* Audit gốc */}
                                <div className="rounded-xl border border-slate-200 p-3">

                                    <p className="mb-2 text-xs font-semibold text-slate-700">
                                        Nhật ký audit gốc
                                    </p>

                                    <div className="flex flex-wrap gap-2">

                                        {alert.sourceAuditIds?.length > 0 ? (
                                            alert.sourceAuditIds.map((id) => (

                                                <button
                                                    key={id}
                                                    type="button"
                                                    onClick={() => viewAudit(id)}
                                                    className="inline-flex items-center gap-1 rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-medium text-indigo-700 hover:bg-indigo-100"
                                                >
                                                    <Eye size={13} />
                                                    Audit #{id}
                                                </button>

                                            ))
                                        ) : (
                                            <span className="text-xs text-slate-400">
                        Không có dữ liệu audit gốc.
                      </span>
                                        )}

                                    </div>

                                </div>

                                {/* Trạng thái xử lý */}
                                {alert.status === "RESOLVED" ? (

                                    <div className="rounded-lg bg-emerald-50 p-3 text-xs text-emerald-800">

                                        <p className="font-semibold">
                                            <CheckCircle2
                                                size={14}
                                                className="mr-1 inline"
                                            />
                                            Cảnh báo đã được xử lý
                                        </p>

                                        <p className="mt-1">
                                            Người xử lý: {alert.resolvedBy || "—"}
                                        </p>

                                        <p>
                                            Thời gian: {formatDate(alert.resolvedAt)}
                                        </p>

                                        {alert.resolutionNote && (
                                            <p className="mt-1">
                                                Ghi chú: {alert.resolutionNote}
                                            </p>
                                        )}

                                    </div>

                                ) : (

                                    <div className="flex justify-end">

                                        <button
                                            type="button"
                                            onClick={() => resolveAlert(alert.id)}
                                            disabled={busyId === alert.id}
                                            className="btn-primary"
                                        >
                                            <CheckCircle2 size={16} />

                                            {busyId === alert.id
                                                ? "Đang xử lý..."
                                                : "Đánh dấu đã xử lý"}
                                        </button>

                                    </div>

                                )}

                            </article>

                        ))}

                    </div>

                    {/* Phân trang */}
                    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-5 py-4">

                        <p className="text-xs text-slate-500">
                            Trang {page.number + 1} / {Math.max(page.totalPages, 1)}
                            {" · "}
                            {page.totalElements} kết quả
                        </p>

                        <div className="flex gap-2">

                            <button
                                type="button"
                                disabled={page.first || loading}
                                onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
                                className="btn-secondary"
                            >
                                Trang trước
                            </button>

                            <button
                                type="button"
                                disabled={page.last || loading}
                                onClick={() => setPageNumber((p) => p + 1)}
                                className="btn-secondary"
                            >
                                Trang sau
                            </button>

                        </div>

                    </div>

                </section>
            )}

            {/* Modal xem audit gốc */}
            {(auditLoading || auditDetail) && (

                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">

                    <section className="max-h-[85vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-5 shadow-xl">

                        <div className="mb-4 flex items-center justify-between">

                            <h2 className="font-semibold text-slate-900">
                                Chi tiết nhật ký audit gốc
                            </h2>

                            <button
                                type="button"
                                onClick={() => {
                                    setAuditDetail(null);
                                    setAuditLoading(false);
                                }}
                                className="rounded-lg p-2 text-slate-500 hover:bg-slate-100"
                            >
                                <X size={18} />
                            </button>

                        </div>

                        {auditLoading ? (

                            <Spinner label="Đang tải chi tiết audit..." />

                        ) : (

                            <pre className="overflow-x-auto rounded-xl bg-slate-50 p-4 text-xs leading-6 text-slate-700">
                {JSON.stringify(auditDetail, null, 2)}
              </pre>

                        )}

                    </section>

                </div>

            )}

        </div>
    );
}
