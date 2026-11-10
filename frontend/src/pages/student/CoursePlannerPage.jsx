import { useEffect, useMemo, useState } from "react";
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  MapPin,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { useConfirm } from "../../components/ConfirmDialog";
import TimetableGrid from "../../components/TimetableGrid";
import { Alert, Badge, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { formatDate, lichHoc, startOfWeek } from "../../lib/format";
import { findPlanConflicts } from "../../lib/coursePlanner";
import { useApi } from "../../lib/useApi";
import { useAuth } from "../../context/AuthContext";

function readPlan(key) {
  try {
    const value = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(value)
      ? [...new Set(value.map(Number).filter((id) => Number.isFinite(id) && id > 0))]
      : [];
  } catch {
    return [];
  }
}

export default function CoursePlannerPage() {
  const { user } = useAuth();
  const confirm = useConfirm();
  const { data: hocKyList, loading: termsLoading, error: termsError } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState(() => new URLSearchParams(window.location.search).get("hocKyId") || "");
  const [keyword, setKeyword] = useState("");
  const [cart, setCart] = useState({ key: "", ids: [] });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [storageError, setStorageError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      const current = hocKyList.find((term) => term.dangMoDangKy) || hocKyList[0];
      setHocKyId(String(current.id));
    }
  }, [hocKyList, hocKyId]);

  const planKey = hocKyId && user?.username
    ? `course-plan:${encodeURIComponent(user.username)}:${hocKyId}`
    : "";

  useEffect(() => {
    if (!planKey) return;
    setCart({ key: planKey, ids: readPlan(planKey) });
    setStorageError("");
  }, [planKey]);

  const params = hocKyId ? { hocKyId } : undefined;
  const {
    data: classList,
    loading: classesLoading,
    error: classesError,
    reload: reloadClasses,
  } = useApi("/lop-hoc-phan", params);
  const { data: myRegs, loading: registrationsLoading, reload: reloadRegistrations } =
    useApi("/dang-ky/me", params);

  const hocKy = hocKyList?.find((term) => String(term.id) === String(hocKyId));
  const cartIds = cart.key === planKey ? cart.ids : [];
  const selectedIds = useMemo(() => new Set(cartIds), [cartIds]);
  const currentClasses = useMemo(
    () => (classList || []).filter((item) => String(item.hocKyId) === String(hocKyId)),
    [classList, hocKyId]
  );
  const classesById = useMemo(
    () => new Map(currentClasses.map((item) => [item.id, item])),
    [currentClasses]
  );
  const plannedClasses = cartIds.map((id) => classesById.get(id)).filter(Boolean);
  const missingIds = cartIds.filter((id) => !classesById.has(id));
  const registeredClasses = (myRegs || []).map((item) => ({
    ...item,
    id: item.lopHocPhanId,
    ma: item.maLop,
  }));
  const conflicts = findPlanConflicts(plannedClasses, registeredClasses);
  const registeredClassIds = new Set(registeredClasses.map((item) => item.lopHocPhanId));
  const plannedCredits = plannedClasses.reduce((total, item) => total + item.soTinChi, 0);
  const registeredCredits = registeredClasses.reduce((total, item) => total + item.soTinChi, 0);
  const projectedCredits = plannedCredits + registeredCredits;
  const fullClasses = plannedClasses.filter((item) => item.conLai <= 0);
  const overCreditLimit = Boolean(hocKy && projectedCredits > hocKy.soTinChiToiDa);
  const hasBlockers = Boolean(
    !hocKy?.dangMoDangKy ||
    missingIds.length ||
    fullClasses.length ||
    conflicts.length ||
    overCreditLimit ||
    plannedClasses.some((item) => registeredClassIds.has(item.id))
  );

  const filteredClasses = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return currentClasses;
    return currentClasses.filter((item) =>
      [item.ma, item.maMon, item.tenMon, item.tenGiangVien]
        .some((value) => String(value || "").toLowerCase().includes(term))
    );
  }, [currentClasses, keyword]);

  const updateCart = (ids) => {
    setError("");
    setSuccess("");
    setCart({ key: planKey, ids });
    try {
      localStorage.setItem(planKey, JSON.stringify(ids));
      setStorageError("");
    } catch {
      setStorageError("Không lưu được giỏ trên thiết bị này. Giỏ chỉ còn trong phiên hiện tại.");
    }
  };

  const addToCart = (courseClass) => updateCart([...cartIds, courseClass.id]);
  const removeFromCart = (courseClassId) =>
    updateCart(cartIds.filter((id) => id !== courseClassId));

  const confirmRegistration = async () => {
    if (!plannedClasses.length || hasBlockers || submitting) return;
    const accepted = await confirm({
      tone: "primary",
      title: "Xác nhận đăng ký học phần",
      message: `Đăng ký chính thức ${plannedClasses.length} lớp với tổng ${plannedCredits} tín chỉ?`,
      detail: "Backend sẽ kiểm tra lại tiên quyết, thời gian đăng ký, trùng môn, lịch và chỗ trống trong cùng một giao dịch.",
      confirmLabel: "Đăng ký các lớp này",
      cancelLabel: "Tiếp tục xem kế hoạch",
    });
    if (!accepted) return;

    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const { data } = await api.post("/dang-ky/xac-nhan-ke-hoach", {
        lopHocPhanIds: plannedClasses.map((item) => item.id),
      });
      const registeredIds = new Set((data || []).map((item) => item.lopHocPhanId));
      const remaining = cartIds.filter((id) => !registeredIds.has(id));
      updateCart(remaining);
      setSuccess(`Đã đăng ký chính thức ${data.length} lớp học phần.`);
      await Promise.all([reloadClasses(), reloadRegistrations()]);
      window.dispatchEvent(new Event("notifications:updated"));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      await Promise.all([reloadClasses(), reloadRegistrations()]);
    } finally {
      setSubmitting(false);
    }
  };

  const calendarItems = plannedClasses.map((item) => ({
    ...item,
    lopHocPhanId: item.id,
    maLop: item.ma,
  }));

  return (
    <div>
      <PageHeader
        icon={CalendarDays}
        title="Kế hoạch học tập"
        description="Thử ghép lớp vào giỏ dự kiến, xem lịch tuần và xác nhận đăng ký khi đã sẵn sàng."
        actions={
          <select
            aria-label="Học kỳ kế hoạch"
            className="input w-auto min-w-[220px]"
            value={hocKyId}
            onChange={(event) => {
              setHocKyId(event.target.value);
              setError("");
              setSuccess("");
            }}
          >
            {(hocKyList || []).map((term) => (
              <option key={term.id} value={term.id}>
                {term.ten} - {term.namHoc}
              </option>
            ))}
          </select>
        }
      />

      {termsError && <Alert tone="error" className="mb-4">{termsError}</Alert>}
      {classesError && <Alert tone="error" className="mb-4">{classesError}</Alert>}
      {error && <Alert tone="error" className="mb-4">{error}</Alert>}
      {success && <Alert tone="success" className="mb-4">{success}</Alert>}
      {storageError && <Alert tone="warning" className="mb-4">{storageError}</Alert>}

      {hocKy && (
        <div className="mb-5 flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-soft">
          <Badge tone={hocKy.dangMoDangKy ? "green" : "red"}>
            {hocKy.dangMoDangKy ? "Đang mở đăng ký" : "Đã đóng đăng ký"}
          </Badge>
          <span className="text-slate-600">
            {formatDate(hocKy.dangKyTu)} - {formatDate(hocKy.dangKyDen)}
          </span>
          <span className="text-slate-600">
            Giới hạn {hocKy.soTinChiToiThieu}-{hocKy.soTinChiToiDa} tín chỉ
          </span>
          <span className="ml-auto text-slate-500">Đã đăng ký: {registeredCredits} tín chỉ</span>
        </div>
      )}

      {termsLoading || (hocKyId && (classesLoading || registrationsLoading)) ? (
        <Spinner />
      ) : !hocKyList?.length ? (
        <EmptyState icon={CalendarDays} title="Chưa có học kỳ" description="Học kỳ sẽ xuất hiện khi được mở trong hệ thống." />
      ) : (
        <div className="grid items-start gap-5 2xl:grid-cols-[minmax(0,1fr)_minmax(320px,0.5fr)]">
          <section className="card overflow-hidden">
            <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="font-semibold text-slate-900">Lớp học phần</h2>
                <p className="mt-1 text-xs text-slate-500">Chọn lớp để đưa vào giỏ dự kiến.</p>
              </div>
              <div className="relative w-full sm:max-w-xs">
                <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  aria-label="Tìm lớp học phần"
                  className="input pl-9"
                  placeholder="Mã lớp, môn học, giảng viên..."
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                />
              </div>
            </div>

            {!filteredClasses.length ? (
              <EmptyState icon={Search} title="Không tìm thấy lớp phù hợp" description="Thử từ khóa khác hoặc chọn học kỳ khác." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[720px]">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="th">Lớp / môn</th>
                      <th className="th">Lịch</th>
                      <th className="th">Chỗ</th>
                      <th className="th text-right">Giỏ dự kiến</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredClasses.map((item) => {
                      const inCart = selectedIds.has(item.id);
                      const registered = registeredClassIds.has(item.id);
                      const full = item.conLai <= 0;
                      return (
                        <tr key={item.id}>
                          <td className="td">
                            <p className="font-medium text-slate-900">{item.ma}</p>
                            <p className="text-sm text-slate-700">{item.tenMon}</p>
                            <p className="text-xs text-slate-500">{item.maMon} · {item.soTinChi} tín chỉ · {item.tenGiangVien}</p>
                          </td>
                          <td className="td text-sm">
                            <p>{lichHoc(item.thu, item.tietBatDau, item.soTiet)}</p>
                            <p className="mt-1 flex items-center gap-1 text-xs text-slate-500"><MapPin size={12} />{item.phong}</p>
                          </td>
                          <td className="td text-sm">
                            {full ? <Badge tone="red">Hết chỗ</Badge> : <span>{item.siSoHienTai}/{item.siSoToiDa}</span>}
                            {!hocKy?.dangMoDangKy && <p className="mt-1 text-xs text-slate-500">Đợt đã đóng</p>}
                          </td>
                          <td className="td text-right">
                            {registered ? (
                              <Badge tone="green">Đã đăng ký</Badge>
                            ) : inCart ? (
                              <Badge tone="indigo">Trong giỏ</Badge>
                            ) : (
                              <button type="button" className="btn-secondary px-3 py-1.5 text-xs" onClick={() => addToCart(item)}>
                                <Plus size={14} /> Thêm
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <div className="space-y-5">
            <section className="card p-4 sm:p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="font-semibold text-slate-900">Giỏ dự kiến</h2>
                  <p className="mt-1 text-xs text-slate-500">Lưu riêng theo tài khoản và học kỳ trên thiết bị này.</p>
                </div>
                <Badge tone="indigo">{plannedClasses.length} lớp · {plannedCredits} tín chỉ</Badge>
              </div>

              {!plannedClasses.length ? (
                <div className="mt-4">
                  <EmptyState icon={CalendarDays} title="Giỏ đang trống" description="Thêm lớp từ danh sách để xem lịch dự kiến." />
                </div>
              ) : (
                <div className="mt-4 divide-y divide-slate-100">
                  {plannedClasses.map((item) => (
                    <div key={item.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
                      <div className="min-w-0 flex-1">
                        <p className="font-medium text-slate-900">{item.ma} · {item.tenMon}</p>
                        <p className="mt-1 flex items-center gap-1 text-xs text-slate-600"><Clock3 size={13} />{lichHoc(item.thu, item.tietBatDau, item.soTiet)}</p>
                        <p className="mt-1 text-xs text-slate-500">Phòng {item.phong} · {item.soTinChi} tín chỉ</p>
                        {item.conLai <= 0 && <p className="mt-1 text-xs font-medium text-rose-700">Lớp đã hết chỗ; cần chọn lớp khác.</p>}
                      </div>
                      <button
                        type="button"
                        aria-label={`Bỏ ${item.ma} khỏi giỏ`}
                        className="rounded-lg p-2 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                        onClick={() => removeFromCart(item.id)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="mt-4 rounded-xl bg-slate-50 p-3 text-sm">
                <div className="flex justify-between text-slate-600"><span>Đã đăng ký</span><span>{registeredCredits} tín chỉ</span></div>
                <div className="mt-1 flex justify-between font-semibold text-slate-900"><span>Sau khi đăng ký kế hoạch</span><span>{projectedCredits}/{hocKy?.soTinChiToiDa || 0} tín chỉ</span></div>
              </div>

              <div className="mt-3 space-y-2">
                {plannedCredits + registeredCredits < (hocKy?.soTinChiToiThieu || 0) && plannedClasses.length > 0 && (
                  <Alert tone="warning">Tổng tín chỉ dự kiến thấp hơn mức tối thiểu {hocKy.soTinChiToiThieu}; đây là cảnh báo để bạn cân nhắc.</Alert>
                )}
                {overCreditLimit && <Alert tone="error">Tổng tín chỉ dự kiến vượt giới hạn {hocKy.soTinChiToiDa}.</Alert>}
                {conflicts.map((item) => (
                  <Alert key={item.key} tone="error">
                    {item.type === "schedule"
                      ? `Trùng lịch: ${item.first.ma} và ${item.second.ma} (${item.day}, tiết ${item.start}-${item.end}; ${item.secondSource}).`
                      : `Trùng môn: ${item.first.ma} và ${item.second.ma} cùng thuộc môn ${item.first.tenMon} (${item.secondSource}).`}
                  </Alert>
                ))}
                {fullClasses.map((item) => <Alert key={`full-${item.id}`} tone="error">Lớp {item.ma} đã hết chỗ.</Alert>)}
                {missingIds.map((id) => (
                  <Alert key={`missing-${id}`} tone="error">
                    Lớp #{id} không còn trong học kỳ này. <button type="button" className="font-semibold underline" onClick={() => removeFromCart(id)}>Bỏ khỏi giỏ</button>
                  </Alert>
                ))}
                {!hocKy?.dangMoDangKy && plannedClasses.length > 0 && <Alert tone="warning">Đợt đăng ký đã đóng; kế hoạch vẫn được lưu nhưng chưa thể xác nhận.</Alert>}
              </div>

              <button
                type="button"
                className="btn-primary mt-4 w-full justify-center"
                disabled={!plannedClasses.length || hasBlockers || submitting}
                onClick={confirmRegistration}
              >
                <CheckCircle2 size={16} />
                {submitting ? "Đang kiểm tra và đăng ký..." : `Xác nhận đăng ký (${plannedClasses.length})`}
              </button>
              <p className="mt-2 text-center text-xs text-slate-500">Thêm hoặc bỏ lớp chỉ sửa kế hoạch trên thiết bị, chưa tạo đăng ký thật.</p>
            </section>

            <section className="card overflow-hidden p-4 sm:p-5">
              <div className="mb-4">
                <h2 className="font-semibold text-slate-900">Thời khóa biểu dự kiến</h2>
                <p className="mt-1 text-xs text-slate-500">
                  Lịch lặp hằng tuần trong học kỳ · tuần bắt đầu {formatDate(hocKy?.ngayBatDau)}. Dữ liệu hiện có thứ, tiết và phòng; chưa có giờ đồng hồ cho từng tiết.
                </p>
              </div>
              {calendarItems.length ? (
                <TimetableGrid items={calendarItems} weekStart={startOfWeek(hocKy?.ngayBatDau)} />
              ) : (
                <EmptyState icon={CalendarDays} title="Chưa có lịch dự kiến" description="Thêm lớp vào giỏ để xem ngày học, tiết học và phòng." />
              )}
            </section>
          </div>
        </div>
      )}
    </div>
  );
}
