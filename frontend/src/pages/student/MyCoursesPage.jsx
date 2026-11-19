import { useEffect, useState } from "react";
import { ClipboardList, Trash2 } from "lucide-react";
import api, { getErrorMessage } from "../../api/client";
import { Alert, EmptyState, Modal, PageHeader, StatCard, Spinner } from "../../components/ui";
import { formatDateTime, lichHoc, soBuoiHoc, tietLyThuyet } from "../../lib/format";
import { useApi } from "../../lib/useApi";

export default function MyCoursesPage() {
  const { data: hocKyList } = useApi("/hoc-ky");
  const [hocKyId, setHocKyId] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [dropTarget, setDropTarget] = useState(null);
  const [dropReason, setDropReason] = useState("");

  useEffect(() => {
    if (!hocKyId && hocKyList?.length) {
      setHocKyId(String(hocKyList[0].id));
    }
  }, [hocKyList, hocKyId]);

  const params = hocKyId ? { hocKyId } : undefined;
  const { data, loading, error: loadError, reload } = useApi("/dang-ky/me", params);

  const hocKy = hocKyList?.find((item) => String(item.id) === String(hocKyId));
  const items = data || [];
  const tongTinChi = items.reduce((sum, item) => sum + item.soTinChi, 0);

  const openDropModal = (item) => {
    setDropTarget(item);
    setDropReason("");
  };

  const confirmDrop = async () => {
    if (!dropTarget) return;
    const item = dropTarget;
    setBusyId(item.id);
    setError("");
    setMessage("");
    try {
      await api.delete(`/dang-ky/${item.id}`, {
        params: dropReason.trim() ? { lyDo: dropReason.trim() } : {},
      });
      setMessage(`Đã hủy đăng ký lớp ${item.maLop}`);
      setDropTarget(null);
      setDropReason("");
      await reload();
      window.dispatchEvent(new Event("notifications:updated"));
    } catch (err) {
      setError(getErrorMessage(err));
      setDropTarget(null);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <PageHeader
        icon={ClipboardList}
        title="Học phần của tôi"
        description="Danh sách học phần đã đăng ký trong học kỳ và tổng số tín chỉ."
        actions={
          <select
            className="input w-auto min-w-[220px]"
            value={hocKyId}
            onChange={(event) => {
              setHocKyId(event.target.value);
              setMessage("");
              setError("");
            }}
          >
            {(hocKyList || []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.ten} - {item.namHoc}
              </option>
            ))}
          </select>
        }
      />

      <div className="mb-5 grid gap-4 sm:grid-cols-3">
        <StatCard label="Số học phần" value={items.length} icon={ClipboardList} />
        <StatCard
          label="Tín chỉ đã đăng ký"
          value={tongTinChi}
          hint={hocKy ? `Quy định: ${hocKy.soTinChiToiThieu} - ${hocKy.soTinChiToiDa} tín chỉ` : ""}
          icon={ClipboardList}
          tone="emerald"
        />
        <StatCard
          label="Trạng thái đợt đăng ký"
          value={hocKy?.dangMoDangKy ? "Đang mở" : "Đã đóng"}
          hint={hocKy ? `${hocKy.ten} - ${hocKy.namHoc}` : ""}
          icon={ClipboardList}
          tone={hocKy?.dangMoDangKy ? "emerald" : "rose"}
        />
      </div>

      <div className="mb-4 space-y-3">
        {message && <Alert tone="success" onClose={() => setMessage("")}>{message}</Alert>}
        {error && <Alert tone="error" onClose={() => setError("")}>{error}</Alert>}
        {loadError && <Alert tone="error">{loadError}</Alert>}
      </div>

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner />
        ) : items.length === 0 ? (
          <EmptyState
            icon={ClipboardList}
            title="Chưa đăng ký học phần nào"
            description="Chuyển sang trang Đăng ký học phần để chọn lớp."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead className="bg-slate-50">
                <tr>
                  <th className="th">Lớp</th>
                  <th className="th">Môn học</th>
                  <th className="th">Giảng viên</th>
                  <th className="th">Lịch học</th>
                  <th className="th">Thời gian đăng ký</th>
                  <th className="th text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60">
                    <td className="td font-medium text-slate-900">{item.maLop}</td>
                    <td className="td">
                      <p className="font-medium text-slate-800">{item.tenMon}</p>
                      <p className="text-xs text-slate-500">
                        {item.maMon} · {item.soTinChi} tín chỉ · {tietLyThuyet(item.soTinChi)} tiết ·{" "}
                        {soBuoiHoc(item.soTinChi)} buổi
                      </p>
                    </td>
                    <td className="td">{item.tenGiangVien}</td>
                    <td className="td">
                      <p>{lichHoc(item.thu, item.tietBatDau, item.soTiet)}</p>
                      <p className="text-xs text-slate-500">{item.phong}</p>
                    </td>
                    <td className="td text-slate-500">{formatDateTime(item.thoiGianDangKy)}</td>
                    <td className="td text-right">
                      {hocKy?.dangMoDangKy ? (
                        <button
                          type="button"
                          className="btn-ghost px-2.5 py-1.5 text-xs text-rose-600 hover:bg-rose-50"
                          disabled={busyId === item.id}
                          onClick={() => openDropModal(item)}
                        >
                          <Trash2 size={14} />
                          {busyId === item.id ? "Đang hủy..." : "Hủy"}
                        </button>
                      ) : (
                        <span className="text-xs text-slate-400">Đã đóng</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {tongTinChi < (hocKy?.soTinChiToiThieu || 0) && items.length > 0 && (
        <p className="mt-3 text-xs text-amber-600">
          Bạn đang đăng ký dưới số tín chỉ tối thiểu ({hocKy.soTinChiToiThieu}) của học kỳ.
        </p>
      )}

      <Modal
        open={Boolean(dropTarget)}
        onClose={() => !busyId && setDropTarget(null)}
        size="sm"
        title="Hủy đăng ký học phần"
        footer={
          <>
            <button
              type="button"
              className="btn-secondary"
              disabled={Boolean(busyId)}
              onClick={() => setDropTarget(null)}
            >
              Giữ lại
            </button>
            <button
              type="button"
              className="btn-danger"
              disabled={Boolean(busyId)}
              onClick={confirmDrop}
            >
              {busyId ? "Đang hủy..." : "Hủy đăng ký"}
            </button>
          </>
        }
      >
        {dropTarget && (
          <div className="space-y-4">
            <p className="text-sm font-medium text-slate-800">
              Bạn có chắc muốn hủy đăng ký lớp {dropTarget.maLop} - {dropTarget.tenMon}?
            </p>
            <p className="text-xs leading-relaxed text-slate-500">
              Chỗ đã đăng ký sẽ được hoàn lại cho lớp học phần. Bạn có thể đăng ký lại nếu còn trong thời gian cho phép.
            </p>
            <div>
              <label className="label" htmlFor="drop-reason">
                Lý do hủy đăng ký (không bắt buộc)
              </label>
              <input
                id="drop-reason"
                className="input"
                maxLength={255}
                placeholder="VD: Đổi sang lớp khác hoặc điều chỉnh kế hoạch..."
                value={dropReason}
                onChange={(event) => setDropReason(event.target.value)}
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
