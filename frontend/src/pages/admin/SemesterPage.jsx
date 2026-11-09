import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { CalendarRange, CalendarClock } from "lucide-react";
import CrudPage from "../../components/CrudPage";
import { Badge, PageHeader } from "../../components/ui";
import { formatDate } from "../../lib/format";
import RegistrationPeriodTab from "./RegistrationPeriodTab";

export default function SemesterPage({ initialTab = "hoc-ky" }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabFromQuery = searchParams.get("tab");
  const [activeTab, setActiveTab] = useState(tabFromQuery || initialTab);

  useEffect(() => {
    if (tabFromQuery && tabFromQuery !== activeTab) {
      setActiveTab(tabFromQuery);
    }
  }, [tabFromQuery]);

  const switchTab = (tab) => {
    setActiveTab(tab);
    setSearchParams(tab === "hoc-ky" ? {} : { tab });
  };

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        <button
          type="button"
          onClick={() => switchTab("hoc-ky")}
          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition ${
            activeTab === "hoc-ky"
              ? "bg-slate-900 text-white shadow-sm"
              : "bg-white text-slate-600 hover:bg-slate-100"
          }`}
        >
          <CalendarRange size={16} />
          Học kỳ & Khung thời gian
        </button>
        <button
          type="button"
          onClick={() => switchTab("dot-dang-ky")}
          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition ${
            activeTab === "dot-dang-ky"
              ? "bg-slate-900 text-white shadow-sm"
              : "bg-white text-slate-600 hover:bg-slate-100"
          }`}
        >
          <CalendarClock size={16} />
          Đợt đăng ký & Phân luồng đối tượng (QLSV-77)
        </button>
      </div>

      {activeTab === "hoc-ky" ? (
        <CrudPage
          icon={CalendarRange}
          title="Học kỳ"
          description="Cấu hình thời gian học, thời gian mở đăng ký và giới hạn tín chỉ cho từng học kỳ."
          endpoint="/admin/hoc-ky"
          searchKeys={["ten", "namHoc"]}
          emptyValues={{
            ten: "",
            namHoc: "",
            ngayBatDau: "",
            ngayKetThuc: "",
            dangKyTu: "",
            dangKyDen: "",
            soTinChiToiThieu: "12",
            soTinChiToiDa: "24",
          }}
          columns={[
            {
              key: "ten",
              label: "Học kỳ",
              render: (row) => (
                <div>
                  <p className="font-medium text-slate-800">{row.ten}</p>
                  <p className="text-xs text-slate-500">{row.namHoc}</p>
                </div>
              ),
            },
            {
              key: "thoiGianHoc",
              label: "Thời gian học",
              render: (row) => `${formatDate(row.ngayBatDau)} - ${formatDate(row.ngayKetThuc)}`,
            },
            {
              key: "thoiGianDangKy",
              label: "Đăng ký chung",
              render: (row) => (
                <div>
                  <p>{formatDate(row.dangKyTu)} - {formatDate(row.dangKyDen)}</p>
                  <Badge tone={row.dangMoDangKy ? "green" : "slate"} className="mt-1">
                    {row.dangMoDangKy ? "Đang mở" : "Đã đóng"}
                  </Badge>
                </div>
              ),
            },
            {
              key: "tinChi",
              label: "Tín chỉ",
              render: (row) => `${row.soTinChiToiThieu} - ${row.soTinChiToiDa}`,
            },
          ]}
          fields={[
            { name: "ten", label: "Tên học kỳ", required: true, placeholder: "VD: Học kỳ 1" },
            { name: "namHoc", label: "Năm học", required: true, placeholder: "VD: 2026-2027" },
            { name: "ngayBatDau", label: "Ngày bắt đầu", type: "date", required: true },
            { name: "ngayKetThuc", label: "Ngày kết thúc", type: "date", required: true },
            { name: "dangKyTu", label: "Mở đăng ký", type: "date", required: true },
            { name: "dangKyDen", label: "Đóng đăng ký", type: "date", required: true },
            {
              name: "soTinChiToiThieu",
              label: "Tín chỉ tối thiểu",
              type: "number",
              required: true,
              min: 0,
            },
            {
              name: "soTinChiToiDa",
              label: "Tín chỉ tối đa",
              type: "number",
              required: true,
              min: 1,
            },
          ]}
        />
      ) : (
        <div>
          <PageHeader
            icon={CalendarClock}
            title="Đợt đăng ký & Phân luồng đối tượng"
            description="Cấu hình khoảng thời gian riêng cho từng nhóm đối tượng (khóa, ngành, lớp, nhóm), mức ưu tiên, gia hạn, đóng sớm và mở bổ sung kèm audit log."
          />
          <RegistrationPeriodTab />
        </div>
      )}
    </div>
  );
}
