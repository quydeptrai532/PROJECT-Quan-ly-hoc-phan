import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Award, BookOpen, FileCheck, GraduationCap, ScrollText, TrendingUp } from "lucide-react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import ExportTranscriptPdfButton from "../../components/ExportTranscriptPdfButton";
import { Alert, Badge, EmptyState, PageHeader, StatCard, Spinner } from "../../components/ui";
import { formatDiem } from "../../lib/format";
import { useApi } from "../../lib/useApi";

function ketQuaBadge(diemTongKet) {
  if (diemTongKet === null || diemTongKet === undefined) {
    return <Badge tone="slate">Chưa có điểm</Badge>;
  }
  if (diemTongKet >= 5) return <Badge tone="green">Đạt</Badge>;
  return <Badge tone="red">Không đạt</Badge>;
}

export default function GradesPage() {
  const { data, loading, error } = useApi("/sinh-vien/me/diem");
  const [exportSuccess, setExportSuccess] = useState("");
  const [exportError, setExportError] = useState("");

  const groups = useMemo(() => {
    if (!data?.items) return [];
    const map = new Map();
    data.items.forEach((item) => {
      if (!map.has(item.hocKyId)) {
        map.set(item.hocKyId, {
          hocKyId: item.hocKyId,
          tenHocKy: item.tenHocKy,
          namHoc: item.namHoc,
          items: [],
        });
      }
      map.get(item.hocKyId).items.push(item);
    });
    return [...map.values()];
  }, [data]);

  const tongKetTheoKy = useMemo(() => {
    const map = {};
    (data?.hocKy || []).forEach((item) => {
      map[item.hocKyId] = item;
    });
    return map;
  }, [data]);

  const gpaChart = useMemo(
    () =>
      [...(data?.hocKy || [])]
        .filter((item) => item.tinChiCoDiem > 0)
        .reverse()
        .map((item) => ({ name: `${item.tenHocKy} ${item.namHoc}`, gpa10: item.gpa10 })),
    [data]
  );

  if (loading) return <Spinner />;
  if (error) return <Alert tone="error">{error}</Alert>;

  return (
    <div>
      <PageHeader
        icon={GraduationCap}
        title="Điểm & kết quả học tập"
        description="Điểm thành phần, điểm tổng kết, GPA học kỳ và điểm tích lũy."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Link to="/phuc-khao-diem" className="btn-secondary">
              <FileCheck size={16} />
              Phúc khảo điểm
            </Link>
            <Link to="/lich-su-hoc-tap" className="btn-secondary">
              <ScrollText size={16} />
              Lịch sử học tập & môn chưa đạt
            </Link>
            <ExportTranscriptPdfButton
              onSuccess={setExportSuccess}
              onError={setExportError}
            />
          </div>
        }
      />

      {exportSuccess && (
        <div className="mb-4">
          <Alert tone="success">{exportSuccess}</Alert>
        </div>
      )}
      {exportError && (
        <div className="mb-4">
          <Alert tone="error">{exportError}</Alert>
        </div>
      )}

      <div className="mb-4"><Alert tone="warning">Quy chế thay điểm và tính GPA/tín chỉ khi học lại, học cải thiện chưa được cấu hình. Các điểm từng lần học được lưu riêng; GPA đang hiển thị chưa phải kết quả chính thức cho môn có nhiều lần học.</Alert></div>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="GPA hệ 10" value={data?.gpa10?.toFixed(2) || "0.00"} icon={TrendingUp} />
        <StatCard label="GPA hệ 4" value={data?.gpa4?.toFixed(2) || "0.00"} icon={Award} tone="emerald" />
        <StatCard
          label="Tín chỉ tích lũy"
          value={data?.tinChiTichLuy || 0}
          hint="Chỉ tính môn đạt (từ 5.0)"
          icon={GraduationCap}
          tone="amber"
        />
        <StatCard
          label="Tổng tín chỉ đã đăng ký"
          value={data?.tongTinChi || 0}
          icon={BookOpen}
          tone="sky"
        />
      </div>

      {gpaChart.length > 0 && (
        <section className="card mb-6 p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">Xu hướng GPA</h2>
              <p className="mt-0.5 text-xs text-slate-500">Điểm trung bình hệ 10 qua từng học kỳ</p>
            </div>
            <Badge tone="indigo">Mốc đạt: 5.0</Badge>
          </div>
          <div className="mt-4 h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={gpaChart} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                <YAxis domain={[0, 10]} tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                <RechartsTooltip
                  contentStyle={{
                    borderRadius: 12,
                    border: "1px solid #e2e8f0",
                    fontSize: 12,
                    boxShadow: "0 18px 40px -20px rgba(15, 23, 42, 0.35)",
                  }}
                  formatter={(value) => [formatDiem(value), "GPA hệ 10"]}
                />
                <ReferenceLine y={5} stroke="#f59e0b" strokeDasharray="5 5" />
                <Line
                  type="monotone"
                  dataKey="gpa10"
                  stroke="#6366f1"
                  strokeWidth={3}
                  dot={{ r: 4, fill: "#fff", strokeWidth: 3, stroke: "#6366f1" }}
                  activeDot={{ r: 6 }}
                  isAnimationActive={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      )}

      {!groups.length ? (
        <div className="card">
          <EmptyState
            icon={GraduationCap}
            title="Chưa có dữ liệu điểm"
            description="Điểm sẽ hiển thị sau khi giảng viên nhập và bạn đã đăng ký học phần."
          />
        </div>
      ) : (
        <div className="space-y-6">
          {groups.map((group) => {
            const tongKet = tongKetTheoKy[group.hocKyId];
            return (
              <section key={group.hocKyId} className="card overflow-hidden">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
                  <div>
                    <h2 className="text-sm font-semibold text-slate-800">
                      {group.tenHocKy} - {group.namHoc}
                    </h2>
                    <p className="text-xs text-slate-500">{group.items.length} học phần</p>
                  </div>
                  {tongKet && (
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <Badge tone="indigo">{tongKet.tongTinChi} tín chỉ</Badge>
                      {tongKet.tinChiCoDiem > 0 ? (
                        <>
                          <Badge tone="sky">GPA 10: {tongKet.gpa10.toFixed(2)}</Badge>
                          <Badge tone="green">GPA 4: {tongKet.gpa4.toFixed(2)}</Badge>
                        </>
                      ) : (
                        <Badge tone="slate">Chưa có điểm</Badge>
                      )}
                    </div>
                  )}
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[820px]">
                    <thead className="bg-white">
                      <tr>
                        <th className="th">Môn học</th>
                        <th className="th">Lớp</th>
                        <th className="th text-center">Tín chỉ</th>
                        <th className="th text-center">Quá trình</th>
                        <th className="th text-center">Cuối kỳ</th>
                        <th className="th text-center">Tổng kết</th>
                        <th className="th">Kết quả</th>
                        <th className="th text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {group.items.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/60">
                          <td className="td">
                            <p className="font-medium text-slate-800">{item.tenMon}</p>
                            <p className="text-xs text-slate-500">{item.maMon}</p>
                          </td>
                          <td className="td text-slate-600">{item.maLop}</td>
                          <td className="td text-center">{item.soTinChi}</td>
                          <td className="td text-center">{formatDiem(item.diemQuaTrinh)}</td>
                          <td className="td text-center">{formatDiem(item.diemCuoiKy)}</td>
                          <td className="td text-center font-semibold text-slate-900">
                            {formatDiem(item.diemTongKet)}
                          </td>
                          <td className="td">{ketQuaBadge(item.diemTongKet)}</td>
                          <td className="td text-right">
                            {item.diemTongKet !== null ? (
                              <Link
                                to={`/phuc-khao-diem?dangKyId=${item.id}`}
                                className="rounded-md border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-indigo-600 transition"
                                title="Nộp hồ sơ phúc khảo cho môn này"
                              >
                                Phúc khảo
                              </Link>
                            ) : (
                              <span className="text-slate-300 text-xs">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
