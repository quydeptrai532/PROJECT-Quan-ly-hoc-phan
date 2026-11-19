import { Fragment, useMemo } from "react";
import { SUBJECT_COLORS, THU_LABEL, addDays, formatDayMonth, isSameDay, toDateKey } from "../lib/format";

const DAYS = [2, 3, 4, 5, 6, 7, 8];
const PERIODS = Array.from({ length: 12 }, (_, index) => index + 1);

function ngayCuaThu(weekStart, thu) {
  if (!weekStart) return null;
  return addDays(weekStart, thu - 2);
}

export function TimetableLegend({ items }) {
  const monHoc = useMemo(() => [...new Set((items || []).map((item) => item.tenMon))], [items]);
  const mauTheoMon = (tenMon) =>
    SUBJECT_COLORS[Math.max(0, monHoc.indexOf(tenMon)) % SUBJECT_COLORS.length];

  if (monHoc.length === 0) return null;

  return (
    <div className="flex flex-wrap gap-2">
      {monHoc.map((tenMon) => (
        <span
          key={tenMon}
          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs ${mauTheoMon(tenMon)}`}
        >
          <span className="h-1.5 w-1.5 rounded-full bg-current opacity-60" />
          {tenMon}
        </span>
      ))}
    </div>
  );
}

export default function TimetableGrid({ items, weekStart, announcements = [] }) {
  const today = new Date();
  const monHoc = useMemo(() => [...new Set((items || []).map((item) => item.tenMon))], [items]);
  const mauTheoMon = (tenMon) =>
    SUBJECT_COLORS[Math.max(0, monHoc.indexOf(tenMon)) % SUBJECT_COLORS.length];

  const thongBaoTheoBuoi = useMemo(() => {
    const map = new Map();
    (announcements || []).forEach((thongBao) => {
      map.set(`${thongBao.lopHocPhanId}|${String(thongBao.ngayNghi).slice(0, 10)}`, thongBao);
    });
    return map;
  }, [announcements]);

  return (
    <div className="overflow-x-auto">
      <div className="min-w-[680px]">
        <div
          className="grid"
          style={{
            gridTemplateColumns: "48px repeat(7, minmax(0, 1fr))",
            gridTemplateRows: "auto repeat(12, minmax(54px, auto))",
          }}
        >
          <div className="border-b border-slate-200 px-2 py-2 text-center text-[11px] font-semibold uppercase text-slate-400">
            Tiết
          </div>
          {DAYS.map((day) => {
            const date = ngayCuaThu(weekStart, day);
            const laHomNay = date && isSameDay(date, today);
            return (
              <div
                key={day}
                className={`border-b border-l border-slate-200 px-2 py-2 text-center text-xs font-semibold ${
                  laHomNay ? "bg-indigo-50 text-indigo-700" : "text-slate-600"
                }`}
                style={{ gridColumn: day, gridRow: 1 }}
              >
                <span>{THU_LABEL[day]}</span>
                {date && (
                  <span className="ml-1 text-[11px] font-normal text-slate-400">
                    ({formatDayMonth(date)})
                  </span>
                )}
                {laHomNay && (
                  <span className="ml-1 inline-block h-1.5 w-1.5 rounded-full bg-indigo-500 align-middle" />
                )}
              </div>
            );
          })}

          {PERIODS.map((period) => (
            <Fragment key={period}>
              <div
                className="border-b border-slate-100 py-1 text-center text-[11px] text-slate-400"
                style={{ gridColumn: 1, gridRow: period + 1 }}
              >
                {period}
              </div>
              {DAYS.map((day) => {
                const date = ngayCuaThu(weekStart, day);
                const laHomNay = date && isSameDay(date, today);
                return (
                  <div
                    key={`${day}-${period}`}
                    className={`border-b border-l border-slate-100 ${laHomNay ? "bg-indigo-50/40" : ""}`}
                    style={{ gridColumn: day, gridRow: period + 1 }}
                  />
                );
              })}
            </Fragment>
          ))}

          {(items || []).map((item) => {
            const ngay = ngayCuaThu(weekStart, item.thu);
            const nghi = ngay
              ? thongBaoTheoBuoi.get(`${item.lopHocPhanId}|${toDateKey(ngay)}`)
              : null;
            return (
              <div
                key={item.lopHocPhanId}
                className={`z-10 m-0.5 overflow-hidden rounded-lg border border-l-4 px-2 py-1.5 text-xs shadow-sm transition-shadow duration-200 hover:shadow-md ${
                  nghi ? "border-rose-400 bg-rose-100 text-rose-900" : mauTheoMon(item.tenMon)
                }`}
                style={{
                  gridColumn: item.thu,
                  gridRow: `${item.tietBatDau + 1} / span ${item.soTiet}`,
                }}
              >
                {nghi ? (
                  <>
                    <p className="font-semibold leading-snug">Nghỉ buổi hôm đó</p>
                    <p className="mt-0.5 line-clamp-2 opacity-80">{item.tenMon}</p>
                    <p className="opacity-70">Lớp {item.maLop}</p>
                    {nghi.lyDo && <p className="line-clamp-2 opacity-70">Lý do: {nghi.lyDo}</p>}
                  </>
                ) : (
                  <>
                    <p className="line-clamp-2 font-semibold leading-snug">{item.tenMon}</p>
                    <p className="mt-0.5 opacity-80">Phòng {item.phong}</p>
                    <p className="opacity-70">
                      Tiết {item.tietBatDau}-{item.tietBatDau + item.soTiet - 1}
                    </p>
                    <p className="opacity-70">{item.maLop}</p>
                  </>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
