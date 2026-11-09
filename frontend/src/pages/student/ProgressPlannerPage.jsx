import { useEffect, useState } from "react";
import {
  AlertCircle,
  AlertTriangle,
  BookOpenCheck,
  CalendarPlus,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  ExternalLink,
  GraduationCap,
  Info,
  Layers,
  RefreshCw,
  Save,
  Trash2,
  XCircle,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function ProgressPlannerPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [semesters, setSemesters] = useState([]);
  const [selectedSemesterId, setSelectedSemesterId] = useState("");
  const [plannerData, setPlannerData] = useState(null);
  const [plans, setPlans] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState("");
  const [planName, setPlanName] = useState("Kế hoạch học tập kỳ tới");
  const [selectedClassIds, setSelectedClassIds] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const [activeTab, setActiveTab] = useState("MISSING_COURSES"); // MISSING_COURSES | SUGGESTIONS | WARNINGS
  const [showExcluded, setShowExcluded] = useState(false);

  // Tải danh sách học kỳ
  useEffect(() => {
    api.get("/hoc-ky")
      .then((res) => {
        const list = res.data || [];
        setSemesters(list);
        if (list.length > 0 && !selectedSemesterId) {
          const openSem = list.find((s) => s.dangMoDangKy) || list[0];
          setSelectedSemesterId(String(openSem.id));
        }
      })
      .catch((err) => console.error("Không tải được danh sách học kỳ:", err));
  }, []);

  // Tải dữ liệu Academic Planner & Kế hoạch cá nhân
  const loadPlanner = async (semesterId) => {
    setLoading(true);
    setError("");
    try {
      const params = semesterId ? { hocKyId: semesterId } : {};
      const [planRes, userPlansRes] = await Promise.all([
        api.get("/sinh-vien/me/ke-hoach-mon-thieu", { params }),
        api.get("/sinh-vien/me/ke-hoach"),
      ]);
      setPlannerData(planRes.data);
      setPlans(userPlansRes.data || []);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSemesterId) {
      loadPlanner(selectedSemesterId);
    } else {
      loadPlanner(null);
    }
  }, [selectedSemesterId]);

  // Cập nhật khi chọn kế hoạch đã lưu
  useEffect(() => {
    const found = plans.find((p) => String(p.id) === selectedPlanId);
    if (found) {
      setPlanName(found.name);
      setSelectedClassIds(found.items.map((i) => i.classId));
    } else {
      setSelectedClassIds([]);
    }
  }, [selectedPlanId, plans]);

  const toggleClassSelect = (classId) => {
    setSelectedClassIds((old) =>
      old.includes(classId) ? old.filter((x) => x !== classId) : [...old, classId]
    );
  };

  const handleSavePlan = async () => {
    setSaving(true);
    setError("");
    setSuccessMessage("");
    try {
      const payload = { name: planName, classIds: selectedClassIds };
      const { data } = selectedPlanId
        ? await api.put(`/sinh-vien/me/ke-hoach/${selectedPlanId}`, payload)
        : await api.post("/sinh-vien/me/ke-hoach", payload);
      setSuccessMessage("Đã lưu kế hoạch cá nhân thành công.");
      await loadPlanner(selectedSemesterId);
      setSelectedPlanId(String(data.id));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePlan = async () => {
    if (!selectedPlanId) return;
    setError("");
    setSuccessMessage("");
    try {
      await api.delete(`/sinh-vien/me/ke-hoach/${selectedPlanId}`);
      setSelectedPlanId("");
      setSuccessMessage("Đã xóa kế hoạch cá nhân.");
      await loadPlanner(selectedSemesterId);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  const handleSendToCoursePlanner = () => {
    const currentPlan = plans.find((p) => String(p.id) === selectedPlanId);
    if (!currentPlan || !user?.username) return;
    const byTerm = new Map();
    currentPlan.items.forEach((item) => {
      const key = `course-plan:${encodeURIComponent(user.username)}:${item.semesterId}`;
      byTerm.set(key, [...(byTerm.get(key) || []), item.classId]);
    });
    try {
      for (const [key, classIds] of byTerm) {
        localStorage.setItem(key, JSON.stringify([...new Set(classIds)]));
      }
      const first = currentPlan.items[0];
      if (first) navigate(`/ke-hoach-hoc-tap?hocKyId=${first.semesterId}`);
    } catch {
      setError("Không chuyển được kế hoạch sang giỏ Course Planner trên thiết bị này.");
    }
  };

  if (loading && !plannerData) {
    return <Spinner label="Đang đối chiếu chương trình và kế hoạch môn còn thiếu…" />;
  }

  const {
    configured,
    message,
    program,
    summary,
    missingGroups = [],
    upcomingSemester,
    registrationSuggestions = [],
    excludedClasses = [],
    unofferedMissingCourses = [],
    graduationDelayWarnings = [],
    limitationsAndDisclaimers = [],
  } = plannerData || {};

  return (
    <div className="space-y-6">
      <PageHeader
        title="Academic Planner — Kế hoạch môn còn thiếu"
        description="Đối chiếu phiên bản chương trình được gán với kết quả đạt của sinh viên, phân nhóm bắt buộc/tự chọn và gợi ý lớp mở kỳ tới."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <select
              className="input w-auto min-w-[200px]"
              value={selectedSemesterId}
              onChange={(e) => setSelectedSemesterId(e.target.value)}
              aria-label="Chọn học kỳ mục tiêu"
            >
              {semesters.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.ten} - {s.namHoc} {s.dangMoDangKy ? "(Đang mở)" : ""}
                </option>
              ))}
            </select>
            <button
              onClick={() => loadPlanner(selectedSemesterId)}
              className="btn-secondary"
              title="Làm mới dữ liệu"
            >
              <RefreshCw size={16} /> Làm mới
            </button>
          </div>
        }
      />

      {error && <Alert variant="error">{error}</Alert>}
      {successMessage && <Alert variant="success">{successMessage}</Alert>}

      {/* Trạng thái chưa cấu hình chương trình */}
      {!configured ? (
        <section className="card p-6">
          <EmptyState
            icon={BookOpenCheck}
            title="Chưa được gán phiên bản chương trình đào tạo"
            description={
              message ||
              "Hệ thống cần tài khoản sinh viên được gán đúng phiên bản chương trình đào tạo theo ngành và khóa nhập học để tính môn còn thiếu."
            }
          />
          <div className="mt-4 rounded-xl bg-slate-50 p-4 text-xs text-slate-600">
            <p className="font-semibold text-slate-700">Lưu ý nghiệp vụ:</p>
            <ul className="mt-1 list-disc space-y-1 pl-4">
              {limitationsAndDisclaimers.map((note, idx) => (
                <li key={idx}>{note}</li>
              ))}
            </ul>
          </div>
        </section>
      ) : (
        <>
          {/* 1. Thẻ tổng quan chương trình & tiến độ tích lũy */}
          <section className="card p-5">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-4">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Chương trình đào tạo được gán
                </span>
                <h2 className="text-lg font-bold text-slate-800">
                  {program.major} · Khóa {program.cohort}
                </h2>
                <p className="text-sm text-slate-500">
                  Phiên bản chính thức: <span className="font-semibold text-indigo-600">{program.version}</span> (dữ liệu được đối chiếu độc lập theo đúng phiên bản này)
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone="blue">
                  Đã tích lũy: {summary?.totalEarnedCredits ?? 0} / {summary?.totalRequiredCredits ?? 0} TC
                </Badge>
                <Badge tone={summary?.totalMissingCredits === 0 ? "green" : "amber"}>
                  {summary?.completionPercent ?? 0}% hoàn thành
                </Badge>
              </div>
            </div>

            {/* Thanh tiến độ */}
            <div className="mt-4">
              <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-emerald-500 transition-all duration-300"
                  style={{ width: `${summary?.completionPercent ?? 0}%` }}
                />
              </div>
            </div>

            {/* Chỉ tiêu phân tách Bắt buộc & Tự chọn */}
            <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
                <span className="text-xs text-slate-500">Khối Bắt buộc</span>
                <p className="text-sm font-semibold">
                  {summary?.mandatoryEarnedCredits} / {summary?.mandatoryRequiredCredits} TC đạt
                </p>
                <p className="text-xs text-slate-600">
                  Còn thiếu: <strong className="text-red-600">{summary?.mandatoryMissingCredits} TC</strong>
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
                <span className="text-xs text-slate-500">Khối Tự chọn</span>
                <p className="text-sm font-semibold">
                  {summary?.electiveEarnedCredits} / {summary?.electiveRequiredCredits} TC đạt
                </p>
                <p className="text-xs text-slate-600">
                  Còn thiếu: <strong className="text-amber-600">{summary?.electiveMissingCredits} TC</strong>
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
                <span className="text-xs text-slate-500">Học kỳ mục tiêu</span>
                <p className="text-sm font-semibold">
                  {upcomingSemester ? `${upcomingSemester.name} - ${upcomingSemester.academicYear}` : "Chưa chọn"}
                </p>
                <p className="text-xs text-slate-600">
                  Giới hạn: {upcomingSemester?.registeredCredits ?? 0} / {upcomingSemester?.maxCredits ?? 24} TC
                  {upcomingSemester?.isOpenForRegistration ? " · Đang mở" : " · Chưa mở"}
                </p>
              </div>
            </div>
          </section>

          {/* 2. Cảnh báo nguy cơ chậm tốt nghiệp & Lưu ý quy chế */}
          {graduationDelayWarnings.length > 0 && (
            <section className="space-y-3">
              <div className="flex items-center gap-2">
                <AlertTriangle size={18} className="text-amber-600" />
                <h3 className="font-semibold text-slate-800">Cảnh báo nguy cơ chậm tiến độ</h3>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {graduationDelayWarnings.map((warn, i) => (
                  <div
                    key={i}
                    className="flex flex-col justify-between rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-sm"
                  >
                    <div>
                      <div className="flex items-center gap-1.5 font-medium text-amber-900">
                        <AlertCircle size={16} className="shrink-0 text-amber-600" />
                        <span>{warn.title}</span>
                      </div>
                      <p className="mt-2 text-xs text-amber-800 leading-relaxed">{warn.description}</p>
                    </div>
                    <p className="mt-3 border-t border-amber-200/60 pt-2 text-[11px] text-amber-700 italic">
                      Căn cứ: {warn.basis}
                    </p>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Hộp thông tin về giới hạn dữ liệu & quy chế */}
          <div className="rounded-xl border border-blue-200 bg-blue-50/60 p-4 text-xs text-blue-900">
            <div className="flex items-center gap-1.5 font-semibold">
              <Info size={15} className="text-blue-600" />
              <span>Lưu ý nghiệp vụ & Giới hạn dữ liệu hệ thống:</span>
            </div>
            <ul className="mt-1.5 list-disc space-y-1 pl-5 text-blue-800">
              {limitationsAndDisclaimers.map((note, idx) => (
                <li key={idx}>{note}</li>
              ))}
            </ul>
          </div>

          {/* Navigation Tabs */}
          <div className="flex flex-wrap border-b border-slate-200 text-sm font-medium">
            <button
              onClick={() => setActiveTab("MISSING_COURSES")}
              className={`px-4 py-2.5 border-b-2 transition-colors ${
                activeTab === "MISSING_COURSES"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-600 hover:text-slate-900"
              }`}
            >
              Môn còn thiếu theo nhóm ({summary?.totalMissingCredits ?? 0} TC)
            </button>
            <button
              onClick={() => setActiveTab("SUGGESTIONS")}
              className={`px-4 py-2.5 border-b-2 transition-colors ${
                activeTab === "SUGGESTIONS"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-600 hover:text-slate-900"
              }`}
            >
              Gợi ý lớp mở kỳ tới ({registrationSuggestions.length} lớp khả thi)
            </button>
            <button
              onClick={() => setActiveTab("UNOFFERED")}
              className={`px-4 py-2.5 border-b-2 transition-colors ${
                activeTab === "UNOFFERED"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-600 hover:text-slate-900"
              }`}
            >
              Môn không mở trong kỳ ({unofferedMissingCourses.length} môn)
            </button>
          </div>

          {/* TAB 1: Danh sách môn còn thiếu phân nhóm Bắt buộc / Tự chọn */}
          {activeTab === "MISSING_COURSES" && (
            <div className="space-y-5">
              {missingGroups.map((group) => (
                <section key={group.groupId || group.groupName} className="card p-5">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                    <div>
                      <h3 className="font-semibold text-slate-900">{group.groupName}</h3>
                      <span className="text-xs text-slate-500">
                        {group.groupType === "BAT_BUOC" ? "Nhóm môn Bắt buộc" : "Nhóm môn Tự chọn"} ·{" "}
                        {group.earnedCredits} / {group.requiredCredits} TC tích lũy
                      </span>
                    </div>
                    <Badge tone={group.isFulfilled ? "green" : "amber"}>
                      {group.isFulfilled ? "Đã hoàn thành nhóm" : `Còn thiếu ${group.missingCredits} TC`}
                    </Badge>
                  </div>

                  {group.missingCourses.length === 0 ? (
                    <p className="py-2 text-sm text-slate-500 italic">
                      Không còn môn thiếu trong nhóm này.
                    </p>
                  ) : (
                    <div className="divide-y divide-slate-100">
                      {group.missingCourses.map((course) => (
                        <div
                          key={course.subjectId}
                          className="flex flex-wrap items-start justify-between gap-3 py-3"
                        >
                          <div className="space-y-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-medium text-slate-900">
                                {course.subjectCode} · {course.subjectName}
                              </span>
                              <Badge tone="gray">{course.credits} TC</Badge>
                              {course.studyStatus === "HOC_LAI" && (
                                <Badge tone="red">
                                  Cần học lại (điểm lần trước: {course.latestScore ?? "—"})
                                </Badge>
                              )}
                              {course.studyStatus === "DANG_HOC" && (
                                <Badge tone="blue">Đang học kỳ này</Badge>
                              )}
                              {course.studyStatus === "CHUA_HOC" && (
                                <Badge tone="gray">Chưa học</Badge>
                              )}
                              {course.eligible ? (
                                <Badge tone="green">Đủ điều kiện tiên quyết</Badge>
                              ) : (
                                <Badge tone="amber">Chưa đủ tiên quyết</Badge>
                              )}
                            </div>

                            <p className="text-xs text-slate-500">
                              {course.eligibilityReason}
                            </p>

                            {/* Môn song hành nếu có */}
                            {course.coRequisites && course.coRequisites.length > 0 && (
                              <div className="mt-1 flex flex-wrap gap-1">
                                {course.coRequisites.map((cr, idx) => (
                                  <span
                                    key={idx}
                                    className="rounded bg-indigo-50 px-2 py-0.5 text-[11px] text-indigo-700"
                                  >
                                    {cr.note}
                                  </span>
                                ))}
                              </div>
                            )}

                            {/* Lịch sử các lần học trước nếu có */}
                            {course.attempts && course.attempts.length > 0 && (
                              <div className="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                                <span>Lần học trước:</span>
                                {course.attempts.map((att, idx) => (
                                  <span
                                    key={idx}
                                    className={`rounded px-1.5 py-0.5 text-[11px] ${
                                      att.passed
                                        ? "bg-emerald-50 text-emerald-700"
                                        : "bg-red-50 text-red-700"
                                    }`}
                                  >
                                    {att.semesterName} ({att.classCode}): {att.score != null ? att.score : "chưa có điểm"}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              ))}
            </div>
          )}

          {/* TAB 2: Gợi ý lớp mở kỳ tới & Kế hoạch cá nhân */}
          {activeTab === "SUGGESTIONS" && (
            <div className="space-y-6">
              {/* Box gợi ý đăng ký khả thi */}
              <section className="card p-5">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h3 className="font-semibold text-slate-900">Lớp học phần gợi ý đăng ký</h3>
                    <p className="text-xs text-slate-500">
                      Gợi ý chỉ gồm lớp đang mở đăng ký, còn chỗ, đủ điều kiện tiên quyết và không xung đột lịch.
                    </p>
                  </div>
                  <span className="text-xs font-semibold text-indigo-600">
                    {registrationSuggestions.length} lớp phù hợp
                  </span>
                </div>

                {registrationSuggestions.length === 0 ? (
                  <EmptyState
                    icon={BookOpenCheck}
                    title="Không có lớp phù hợp trong kỳ này"
                    description="Các môn còn thiếu có thể chưa mở lớp, đã hết chỗ hoặc chưa đạt môn tiên quyết."
                  />
                ) : (
                  <div className="divide-y divide-slate-100">
                    {registrationSuggestions.map((sug) => (
                      <label
                        key={sug.classId}
                        className="flex cursor-pointer items-start gap-3 py-3 hover:bg-slate-50/50 rounded-lg px-2 -mx-2 transition-colors"
                      >
                        <input
                          type="checkbox"
                          checked={selectedClassIds.includes(sug.classId)}
                          onChange={() => toggleClassSelect(sug.classId)}
                          className="mt-1"
                        />
                        <div className="flex-1 space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <strong className="text-slate-900">
                              {sug.subjectCode} · {sug.subjectName}
                            </strong>
                            <Badge tone="blue">{sug.classCode}</Badge>
                            <Badge tone="gray">{sug.credits} TC</Badge>
                            <Badge tone="green">Còn {sug.remainingSeats} chỗ</Badge>
                          </div>
                          <p className="text-xs text-slate-600">
                            Thứ {sug.dayOfWeek}, tiết {sug.startPeriod}–{sug.startPeriod + sug.periods - 1} · Phòng {sug.room} · GV: {sug.lecturerName}
                          </p>
                          <p className="text-xs text-emerald-700 flex items-center gap-1">
                            <CheckCircle2 size={13} className="shrink-0" />
                            {sug.reason}
                          </p>
                        </div>
                      </label>
                    ))}
                  </div>
                )}
              </section>

              {/* Lớp bị loại trừ (có lý do cụ thể) */}
              {excludedClasses.length > 0 && (
                <section className="card p-5">
                  <button
                    type="button"
                    onClick={() => setShowExcluded(!showExcluded)}
                    className="flex w-full items-center justify-between text-left font-medium text-slate-800"
                  >
                    <span className="flex items-center gap-2">
                      <XCircle size={16} className="text-slate-400" />
                      Lớp học phần bị loại trừ ({excludedClasses.length} lớp)
                    </span>
                    {showExcluded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                  </button>

                  {showExcluded && (
                    <div className="mt-3 divide-y divide-slate-100 border-t border-slate-100 pt-3">
                      {excludedClasses.map((exc, idx) => (
                        <div key={idx} className="py-2.5 text-xs">
                          <div className="flex flex-wrap items-center gap-2">
                            <strong className="text-slate-700">
                              {exc.classCode} · {exc.subjectName}
                            </strong>
                            <span className="text-slate-500">
                              Thứ {exc.dayOfWeek}, tiết {exc.startPeriod}–{exc.startPeriod + exc.periods - 1}
                            </span>
                            <span className="text-slate-400">
                              ({exc.remainingSeats}/{exc.totalSeats} chỗ)
                            </span>
                          </div>
                          <p className="mt-1 text-red-600 font-medium">Lý do: {exc.reason}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              )}

              {/* Box lưu kế hoạch cá nhân & chuyển sang Course Planner */}
              <section className="card p-5">
                <div className="mb-3 flex items-center gap-2">
                  <CalendarPlus size={18} className="text-indigo-600" />
                  <h3 className="font-semibold text-slate-900">Lưu kế hoạch học tập cá nhân</h3>
                </div>
                <p className="mb-4 text-xs text-slate-500">
                  Chọn các lớp gợi ý ở trên để tạo kế hoạch. Lưu kế hoạch không tạo đăng ký chính thức, dùng để quản lý lộ trình học tập cá nhân.
                </p>

                <div className="flex flex-wrap gap-2">
                  <select
                    className="input max-w-xs"
                    value={selectedPlanId}
                    onChange={(e) => setSelectedPlanId(e.target.value)}
                  >
                    <option value="">Kế hoạch mới…</option>
                    {plans.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                  <input
                    className="input max-w-xs"
                    value={planName}
                    onChange={(e) => setPlanName(e.target.value)}
                    placeholder="Tên kế hoạch"
                    aria-label="Tên kế hoạch"
                  />
                  <button
                    className="btn-primary"
                    disabled={saving || !selectedClassIds.length}
                    onClick={handleSavePlan}
                  >
                    <Save size={16} />
                    {saving ? "Đang lưu…" : "Lưu kế hoạch"}
                  </button>
                  {selectedPlanId && (
                    <>
                      <button className="btn-secondary" onClick={handleDeletePlan}>
                        <Trash2 size={16} /> Xóa
                      </button>
                      <button
                        className="btn-secondary flex items-center gap-1.5"
                        onClick={handleSendToCoursePlanner}
                      >
                        <ExternalLink size={15} /> Mở trong Course Planner
                      </button>
                    </>
                  )}
                </div>
              </section>
            </div>
          )}

          {/* TAB 3: Môn không mở trong kỳ */}
          {activeTab === "UNOFFERED" && (
            <section className="card p-5">
              <div className="mb-3">
                <h3 className="font-semibold text-slate-900">Môn học không có lớp mở trong kỳ được chọn</h3>
                <p className="text-xs text-slate-500">
                  Các môn dưới đây thuộc chương trình đào tạo còn thiếu nhưng học kỳ này không mở lớp học phần.
                  Chúng được thể hiện riêng biệt để sinh viên nắm lộ trình và không biến thành gợi ý đăng ký khả thi.
                </p>
              </div>

              {unofferedMissingCourses.length === 0 ? (
                <EmptyState
                  icon={CheckCircle2}
                  title="Tất cả môn còn thiếu đều có lớp mở trong kỳ"
                  description="Sinh viên có thể xem tab Gợi ý lớp mở kỳ tới để lập kế hoạch."
                />
              ) : (
                <div className="divide-y divide-slate-100">
                  {unofferedMissingCourses.map((unoff) => (
                    <div
                      key={unoff.subjectId}
                      className="flex flex-wrap items-center justify-between gap-3 py-3"
                    >
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <strong className="text-slate-800">
                            {unoff.subjectCode} · {unoff.subjectName}
                          </strong>
                          <Badge tone="gray">{unoff.credits} TC</Badge>
                          <Badge tone={unoff.groupType === "BAT_BUOC" ? "blue" : "purple"}>
                            {unoff.groupType === "BAT_BUOC" ? "Bắt buộc" : "Tự chọn"}
                          </Badge>
                          {unoff.eligible ? (
                            <Badge tone="green">Đủ tiên quyết</Badge>
                          ) : (
                            <Badge tone="amber">Chưa đủ tiên quyết</Badge>
                          )}
                        </div>
                        <p className="mt-1 text-xs text-slate-500">{unoff.note}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
