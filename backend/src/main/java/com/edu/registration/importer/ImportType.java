package com.edu.registration.importer;

/**
 * Danh mục các loại hình import dữ liệu chuẩn hóa trong hệ thống QLSV (QLSV-90 / TASK-OP-03).
 */
public enum ImportType {
    SINH_VIEN(
            "SINH_VIEN",
            "Sinh viên",
            "CSV",
            ".csv",
            "text/csv; charset=UTF-8",
            "mau-import-sinh-vien.csv",
            "ROW_BY_ROW",
            "SKIP_IF_EXISTS",
            "Import tài khoản và hồ sơ sinh viên từ tệp CSV UTF-8 (có BOM)"
    ),
    GIANG_VIEN(
            "GIANG_VIEN",
            "Giảng viên",
            "CSV",
            ".csv",
            "text/csv; charset=UTF-8",
            "mau-import-giang-vien.csv",
            "ROW_BY_ROW",
            "SKIP_IF_EXISTS",
            "Import tài khoản và thông tin giảng viên từ tệp CSV UTF-8 (có BOM)"
    ),
    DIEM_LOPHOCPHAN(
            "DIEM_LOPHOCPHAN",
            "Điểm lớp học phần",
            "XLSX",
            ".xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "mau-nhap-diem.xlsx",
            "ALL_OR_NOTHING",
            "OVERWRITE_EXISTING",
            "Import điểm quá trình và cuối kỳ của sinh viên trong lớp học phần từ tệp Excel .xlsx"
    );

    private final String code;
    private final String tenHienThi;
    private final String dinhDangTep;
    private final String phanMoRong;
    private final String mimeType;
    private final String tenTepMau;
    private final String transactionPolicy;
    private final String updatePolicy;
    private final String moTa;

    ImportType(
            String code,
            String tenHienThi,
            String dinhDangTep,
            String phanMoRong,
            String mimeType,
            String tenTepMau,
            String transactionPolicy,
            String updatePolicy,
            String moTa
    ) {
        this.code = code;
        this.tenHienThi = tenHienThi;
        this.dinhDangTep = dinhDangTep;
        this.phanMoRong = phanMoRong;
        this.mimeType = mimeType;
        this.tenTepMau = tenTepMau;
        this.transactionPolicy = transactionPolicy;
        this.updatePolicy = updatePolicy;
        this.moTa = moTa;
    }

    public String getCode() {
        return code;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getDinhDangTep() {
        return dinhDangTep;
    }

    public String getPhanMoRong() {
        return phanMoRong;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getTenTepMau() {
        return tenTepMau;
    }

    public String getTransactionPolicy() {
        return transactionPolicy;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public String getMoTa() {
        return moTa;
    }
}
