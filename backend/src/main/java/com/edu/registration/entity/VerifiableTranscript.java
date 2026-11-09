package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ phiên bản bảng điểm điện tử xác thực (QLSV-99 / TASK-OP-12).
 * Đảm bảo tính bất biến, toàn vẹn dữ liệu điểm đã phát hành, hỗ trợ thu hồi và cấp lại.
 */
@Entity
@Table(name = "bang_diem_xac_thuc", indexes = {
        @Index(name = "idx_vt_ma_hash", columnList = "ma_xac_minh_hash", unique = true),
        @Index(name = "idx_vt_sinh_vien", columnList = "sinh_vien_id, hoc_ky_id"),
        @Index(name = "idx_vt_ma_sv", columnList = "ma_sinh_vien"),
        @Index(name = "idx_vt_trang_thai", columnList = "trang_thai")
})
public class VerifiableTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã băm SHA-256 của chuỗi token xác minh ngẫu nhiên có entropy cao (256-bit).
     * Tuyệt đối không lưu token bí mật dạng thô trong cơ sở dữ liệu.
     */
    @Column(name = "ma_xac_minh_hash", nullable = false, unique = true, length = 64)
    private String maXacMinhHash;

    /**
     * Mã hiển thị che mờ (masked) để hiển thị an toàn trên giao diện quản trị / nhật ký kiểm toán.
     * Ví dụ: VT-****-A1B2
     */
    @Column(name = "ma_xac_minh_masked", nullable = false, length = 64)
    private String maXacMinhMasked;

    @Column(name = "ma_sinh_vien", nullable = false, length = 50)
    private String maSinhVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_id")
    private Semester hocKy;

    @Column(name = "ten_hoc_ky", length = 100)
    private String tenHocKy;

    /**
     * Số thứ tự phiên bản phát hành của bảng điểm (1 cho lần đầu, tăng dần khi cấp lại).
     */
    @Column(name = "phien_ban", nullable = false)
    private Integer phienBan = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private VerifiableTranscriptStatus trangThai = VerifiableTranscriptStatus.HIEU_LUC;

    /**
     * Mã băm SHA-256 của tệp PDF bảng điểm tại thời điểm phát hành để xác nhận tính toàn vẹn.
     */
    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "so_mon_da_cong_bo", nullable = false)
    private Integer soMonDaCongBo;

    @Column(name = "tong_tin_chi_co_diem", nullable = false)
    private Integer tongTinChiCoDiem;

    @Column(name = "tong_tin_chi_tich_luy", nullable = false)
    private Integer tongTinChiTichLuy;

    @Column(name = "gpa10", nullable = false)
    private Double gpa10;

    @Column(name = "gpa4", nullable = false)
    private Double gpa4;

    /**
     * Ảnh chụp nhanh (snapshot) dữ liệu bảng điểm tại thời điểm cấp mã (dạng JSON).
     * Đảm bảo bảng điểm phát hành không bị âm thầm thay đổi khi điểm hệ thống thay đổi sau đó.
     */
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(name = "don_vi_phat_hanh", length = 255)
    private String donViPhatHanh;

    @Column(name = "thoi_diem_phat_hanh", nullable = false)
    private LocalDateTime thoiDiemPhatHanh;

    @Column(name = "nguoi_phat_hanh_id")
    private Long nguoiPhatHanhId;

    @Column(name = "nguoi_phat_hanh_username", length = 50)
    private String nguoiPhatHanhUsername;

    @Column(name = "nguoi_phat_hanh_role", length = 30)
    private String nguoiPhatHanhRole;

    @Column(name = "thoi_diem_thu_hoi")
    private LocalDateTime thoiDiemThuHoi;

    @Column(name = "nguoi_thu_hoi_id")
    private Long nguoiThuHoiId;

    @Column(name = "nguoi_thu_hoi_username", length = 50)
    private String nguoiThuHoiUsername;

    @Column(name = "nguoi_thu_hoi_role", length = 30)
    private String nguoiThuHoiRole;

    @Column(name = "ly_do_thu_hoi", length = 500)
    private String lyDoThuHoi;

    /**
     * Liên kết phiên bản trước (bản ghi gốc) khi được cấp lại.
     */
    @Column(name = "ban_ghi_goc_id")
    private Long banGhiGocId;

    /**
     * Liên kết phiên bản mới (bản ghi thay thế) khi bị thu hồi do cấp lại.
     */
    @Column(name = "ban_ghi_thay_the_id")
    private Long banGhiThayTheId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (thoiDiemPhatHanh == null) {
            thoiDiemPhatHanh = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaXacMinhHash() {
        return maXacMinhHash;
    }

    public void setMaXacMinhHash(String maXacMinhHash) {
        this.maXacMinhHash = maXacMinhHash;
    }

    public String getMaXacMinhMasked() {
        return maXacMinhMasked;
    }

    public void setMaXacMinhMasked(String maXacMinhMasked) {
        this.maXacMinhMasked = maXacMinhMasked;
    }

    public String getMaSinhVien() {
        return maSinhVien;
    }

    public void setMaSinhVien(String maSinhVien) {
        this.maSinhVien = maSinhVien;
    }

    public Student getSinhVien() {
        return sinhVien;
    }

    public void setSinhVien(Student sinhVien) {
        this.sinhVien = sinhVien;
    }

    public Semester getHocKy() {
        return hocKy;
    }

    public void setHocKy(Semester hocKy) {
        this.hocKy = hocKy;
    }

    public String getTenHocKy() {
        return tenHocKy;
    }

    public void setTenHocKy(String tenHocKy) {
        this.tenHocKy = tenHocKy;
    }

    public Integer getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Integer phienBan) {
        this.phienBan = phienBan;
    }

    public VerifiableTranscriptStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(VerifiableTranscriptStatus trangThai) {
        this.trangThai = trangThai;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }

    public Integer getSoMonDaCongBo() {
        return soMonDaCongBo;
    }

    public void setSoMonDaCongBo(Integer soMonDaCongBo) {
        this.soMonDaCongBo = soMonDaCongBo;
    }

    public Integer getTongTinChiCoDiem() {
        return tongTinChiCoDiem;
    }

    public void setTongTinChiCoDiem(Integer tongTinChiCoDiem) {
        this.tongTinChiCoDiem = tongTinChiCoDiem;
    }

    public Integer getTongTinChiTichLuy() {
        return tongTinChiTichLuy;
    }

    public void setTongTinChiTichLuy(Integer tongTinChiTichLuy) {
        this.tongTinChiTichLuy = tongTinChiTichLuy;
    }

    public Double getGpa10() {
        return gpa10;
    }

    public void setGpa10(Double gpa10) {
        this.gpa10 = gpa10;
    }

    public Double getGpa4() {
        return gpa4;
    }

    public void setGpa4(Double gpa4) {
        this.gpa4 = gpa4;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getDonViPhatHanh() {
        return donViPhatHanh;
    }

    public void setDonViPhatHanh(String donViPhatHanh) {
        this.donViPhatHanh = donViPhatHanh;
    }

    public LocalDateTime getThoiDiemPhatHanh() {
        return thoiDiemPhatHanh;
    }

    public void setThoiDiemPhatHanh(LocalDateTime thoiDiemPhatHanh) {
        this.thoiDiemPhatHanh = thoiDiemPhatHanh;
    }

    public Long getNguoiPhatHanhId() {
        return nguoiPhatHanhId;
    }

    public void setNguoiPhatHanhId(Long nguoiPhatHanhId) {
        this.nguoiPhatHanhId = nguoiPhatHanhId;
    }

    public String getNguoiPhatHanhUsername() {
        return nguoiPhatHanhUsername;
    }

    public void setNguoiPhatHanhUsername(String nguoiPhatHanhUsername) {
        this.nguoiPhatHanhUsername = nguoiPhatHanhUsername;
    }

    public String getNguoiPhatHanhRole() {
        return nguoiPhatHanhRole;
    }

    public void setNguoiPhatHanhRole(String nguoiPhatHanhRole) {
        this.nguoiPhatHanhRole = nguoiPhatHanhRole;
    }

    public LocalDateTime getThoiDiemThuHoi() {
        return thoiDiemThuHoi;
    }

    public void setThoiDiemThuHoi(LocalDateTime thoiDiemThuHoi) {
        this.thoiDiemThuHoi = thoiDiemThuHoi;
    }

    public Long getNguoiThuHoiId() {
        return nguoiThuHoiId;
    }

    public void setNguoiThuHoiId(Long nguoiThuHoiId) {
        this.nguoiThuHoiId = nguoiThuHoiId;
    }

    public String getNguoiThuHoiUsername() {
        return nguoiThuHoiUsername;
    }

    public void setNguoiThuHoiUsername(String nguoiThuHoiUsername) {
        this.nguoiThuHoiUsername = nguoiThuHoiUsername;
    }

    public String getNguoiThuHoiRole() {
        return nguoiThuHoiRole;
    }

    public void setNguoiThuHoiRole(String nguoiThuHoiRole) {
        this.nguoiThuHoiRole = nguoiThuHoiRole;
    }

    public String getLyDoThuHoi() {
        return lyDoThuHoi;
    }

    public void setLyDoThuHoi(String lyDoThuHoi) {
        this.lyDoThuHoi = lyDoThuHoi;
    }

    public Long getBanGhiGocId() {
        return banGhiGocId;
    }

    public void setBanGhiGocId(Long banGhiGocId) {
        this.banGhiGocId = banGhiGocId;
    }

    public Long getBanGhiThayTheId() {
        return banGhiThayTheId;
    }

    public void setBanGhiThayTheId(Long banGhiThayTheId) {
        this.banGhiThayTheId = banGhiThayTheId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
