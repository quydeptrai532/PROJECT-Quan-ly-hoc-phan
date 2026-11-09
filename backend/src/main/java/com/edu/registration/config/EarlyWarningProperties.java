package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình tập trung cho Hệ thống Cảnh báo Sớm (Early Warning / Academic Alerts - QLSV-97 / TASK-OP-10).
 *
 * <p>Lưu ý quan trọng: Các ngưỡng kích hoạt và chính sách hiển thị quyền riêng tư hiện tại đang áp dụng
 * cấu hình tập trung dự kiến và CHƯA ĐƯỢC HỘI ĐỒNG ĐÀO TẠO / CHỦ DỰ ÁN PHÊ DUYỆT CHÍNH THỨC.
 * Hệ thống KHÔNG tự đặt ngưỡng chính thức và ghi chú rõ ràng các thông số chờ xác nhận.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.early-warning")
public class EarlyWarningProperties {

    /** Bật/tắt toàn bộ hệ thống cảnh báo sớm */
    private boolean enabled = true;

    /** Cờ đánh dấu đã có quy chế chính thức của Nhà trường hay chưa */
    private boolean officialRegulation = false;

    /** Ghi chú giải thích tình trạng pháp lý và quy chế */
    private String policyNote = "Các ngưỡng cảnh báo học tập sớm (điểm quá trình, số buổi/tỷ lệ vắng, suy giảm GPA, thiếu tín chỉ) " +
            "và chính sách phân quyền riêng tư hiển thị cho sinh viên đang áp dụng cấu hình tập trung dự kiến " +
            "và CHỜ HỘI ĐỒNG ĐÀO TẠO / CHỦ DỰ ÁN PHÊ DUYỆT CHÍNH THỨC.";

    private DiemQuaTrinhConfig diemQuaTrinh = new DiemQuaTrinhConfig();
    private DiemDanhConfig diemDanh = new DiemDanhConfig();
    private GpaGiamConfig gpaGiam = new GpaGiamConfig();
    private TienDoTinChiConfig tienDoTinChi = new TienDoTinChiConfig();

    public static class DiemQuaTrinhConfig {
        private boolean enabled = true;
        /** Ngưỡng điểm quá trình tối thiểu (dưới mức này sẽ kích hoạt cảnh báo, mặc định 5.0) */
        private double nguongDiem = 5.0;
        private String mucDo = "CANH_BAO";
        private boolean choPhepSinhVienXem = true;
        private String ghiChu = "Cần Hội đồng Đào tạo xác nhận ngưỡng điểm quá trình chính thức: 4.0 (nguy cơ rớt học phần) hay 5.0 (dưới mức đạt).";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getNguongDiem() { return nguongDiem; }
        public void setNguongDiem(double nguongDiem) { this.nguongDiem = nguongDiem; }
        public String getMucDo() { return mucDo; }
        public void setMucDo(String mucDo) { this.mucDo = mucDo; }
        public boolean isChoPhepSinhVienXem() { return choPhepSinhVienXem; }
        public void setChoPhepSinhVienXem(boolean choPhepSinhVienXem) { this.choPhepSinhVienXem = choPhepSinhVienXem; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    }

    public static class DiemDanhConfig {
        private boolean enabled = true;
        /** Ngưỡng số buổi vắng tối đa (vượt quá hoặc bằng sẽ kích hoạt cảnh báo, mặc định 3 buổi) */
        private int nguongSoBuoiVang = 3;
        /** Ngưỡng tỷ lệ vắng tối đa (%) (mặc định 20.0%) */
        private double nguongTyLeVang = 20.0;
        private String mucDo = "NGHIEM_TRONG";
        private boolean choPhepSinhVienXem = true;
        private String ghiChu = "Cần xác nhận ngưỡng vắng tuyệt đối (3 buổi) hay tỷ lệ (20%) làm tiêu chuẩn cảnh báo/cấm thi chính thức.";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getNguongSoBuoiVang() { return nguongSoBuoiVang; }
        public void setNguongSoBuoiVang(int nguongSoBuoiVang) { this.nguongSoBuoiVang = nguongSoBuoiVang; }
        public double getNguongTyLeVang() { return nguongTyLeVang; }
        public void setNguongTyLeVang(double nguongTyLeVang) { this.nguongTyLeVang = nguongTyLeVang; }
        public String getMucDo() { return mucDo; }
        public void setMucDo(String mucDo) { this.mucDo = mucDo; }
        public boolean isChoPhepSinhVienXem() { return choPhepSinhVienXem; }
        public void setChoPhepSinhVienXem(boolean choPhepSinhVienXem) { this.choPhepSinhVienXem = choPhepSinhVienXem; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    }

    public static class GpaGiamConfig {
        private boolean enabled = true;
        /** Số học kỳ liên tiếp xem xét xu hướng GPA (mặc định 2 học kỳ) */
        private int soHocKyLienTiep = 2;
        /** Mức giảm GPA tối thiểu giữa các kỳ liên tiếp để kích hoạt cảnh báo (hệ 4, mặc định 0.3) */
        private double mucGiamToiThieu = 0.3;
        private String mucDo = "CANH_BAO";
        /** Chính sách riêng tư: sinh viên có được xem loại cảnh báo này không */
        private boolean choPhepSinhVienXem = false;
        private String ghiChu = "Chính sách quyền riêng tư: Cảnh báo suy giảm GPA hiện chỉ mở cho Cán bộ Quản trị và Cố vấn học tập; " +
                "chờ quyết định chính thức về việc có hiển thị trực tiếp cho Sinh viên hay không.";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getSoHocKyLienTiep() { return soHocKyLienTiep; }
        public void setSoHocKyLienTiep(int soHocKyLienTiep) { this.soHocKyLienTiep = soHocKyLienTiep; }
        public double getMucGiamToiThieu() { return mucGiamToiThieu; }
        public void setMucGiamToiThieu(double mucGiamToiThieu) { this.mucGiamToiThieu = mucGiamToiThieu; }
        public String getMucDo() { return mucDo; }
        public void setMucDo(String mucDo) { this.mucDo = mucDo; }
        public boolean isChoPhepSinhVienXem() { return choPhepSinhVienXem; }
        public void setChoPhepSinhVienXem(boolean choPhepSinhVienXem) { this.choPhepSinhVienXem = choPhepSinhVienXem; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    }

    public static class TienDoTinChiConfig {
        private boolean enabled = true;
        /** Số tín chỉ chuẩn kỳ vọng tích lũy trung bình mỗi học kỳ (mặc định 14 tín chỉ) */
        private int tinChiChuanMoiKy = 14;
        /** Số tín chỉ thiếu tối thiểu so với tiến độ chuẩn để cảnh báo (mặc định 8 tín chỉ) */
        private int nguongTinChiThieu = 8;
        /** Ngưỡng tỷ lệ hoàn thành tối thiểu (%) so với tiến độ chuẩn (mặc định 70.0%) */
        private double nguongTyLeHoanThanh = 70.0;
        private String mucDo = "CANH_BAO";
        private boolean choPhepSinhVienXem = true;
        private String ghiChu = "Cần Phòng Đào tạo xác nhận tiến độ chuẩn 14 tín chỉ/kỳ và ngưỡng thiếu 8 tín chỉ có phân nhánh theo từng ngành học hay không.";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getTinChiChuanMoiKy() { return tinChiChuanMoiKy; }
        public void setTinChiChuanMoiKy(int tinChiChuanMoiKy) { this.tinChiChuanMoiKy = tinChiChuanMoiKy; }
        public int getNguongTinChiThieu() { return nguongTinChiThieu; }
        public void setNguongTinChiThieu(int nguongTinChiThieu) { this.nguongTinChiThieu = nguongTinChiThieu; }
        public double getNguongTyLeHoanThanh() { return nguongTyLeHoanThanh; }
        public void setNguongTyLeHoanThanh(double nguongTyLeHoanThanh) { this.nguongTyLeHoanThanh = nguongTyLeHoanThanh; }
        public String getMucDo() { return mucDo; }
        public void setMucDo(String mucDo) { this.mucDo = mucDo; }
        public boolean isChoPhepSinhVienXem() { return choPhepSinhVienXem; }
        public void setChoPhepSinhVienXem(boolean choPhepSinhVienXem) { this.choPhepSinhVienXem = choPhepSinhVienXem; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    }

    /** Danh sách tóm tắt các giá trị chờ Hội đồng Đào tạo xác nhận */
    public List<String> layDanhSachGiaTriChoXacNhan() {
        List<String> items = new ArrayList<>();
        items.add("Quy chế chính thức về Hệ thống Cảnh báo Sớm (officialRegulation = false).");
        items.add("Luật Điểm quá trình: " + diemQuaTrinh.getGhiChu());
        items.add("Luật Điểm danh: " + diemDanh.getGhiChu());
        items.add("Luật GPA giảm: " + gpaGiam.getGhiChu());
        items.add("Luật Tiến độ tín chỉ: " + tienDoTinChi.getGhiChu());
        return items;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isOfficialRegulation() { return officialRegulation; }
    public void setOfficialRegulation(boolean officialRegulation) { this.officialRegulation = officialRegulation; }
    public String getPolicyNote() { return policyNote; }
    public void setPolicyNote(String policyNote) { this.policyNote = policyNote; }
    public DiemQuaTrinhConfig getDiemQuaTrinh() { return diemQuaTrinh; }
    public void setDiemQuaTrinh(DiemQuaTrinhConfig diemQuaTrinh) { this.diemQuaTrinh = diemQuaTrinh; }
    public DiemDanhConfig getDiemDanh() { return diemDanh; }
    public void setDiemDanh(DiemDanhConfig diemDanh) { this.diemDanh = diemDanh; }
    public GpaGiamConfig getGpaGiam() { return gpaGiam; }
    public void setGpaGiam(GpaGiamConfig gpaGiam) { this.gpaGiam = gpaGiam; }
    public TienDoTinChiConfig getTienDoTinChi() { return tienDoTinChi; }
    public void setTienDoTinChi(TienDoTinChiConfig tienDoTinChi) { this.tienDoTinChi = tienDoTinChi; }
}
