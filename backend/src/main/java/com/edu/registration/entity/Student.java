package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sinh_vien")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String ma;

    @Column(nullable = false, length = 150)
    private String hoTen;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String soDienThoai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_id")
    private Faculty khoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_id")
    private Major nganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_id")
    private AdministrativeClass lopHanhChinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuong_trinh_id")
    private Curriculum chuongTrinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_hoc_tap", nullable = false, length = 30)
    private StudentAcademicStatus trangThaiHocTap = StudentAcademicStatus.DANG_HOC;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public Faculty getKhoa() {
        return khoa;
    }

    public void setKhoa(Faculty khoa) {
        this.khoa = khoa;
    }

    public Major getNganh() {
        return nganh;
    }

    public void setNganh(Major nganh) {
        this.nganh = nganh;
    }

    public AdministrativeClass getLopHanhChinh() {
        return lopHanhChinh;
    }

    public void setLopHanhChinh(AdministrativeClass lopHanhChinh) {
        this.lopHanhChinh = lopHanhChinh;
    }

    public Curriculum getChuongTrinh() { return chuongTrinh; }
    public void setChuongTrinh(Curriculum chuongTrinh) { this.chuongTrinh = chuongTrinh; }

    public StudentAcademicStatus getTrangThaiHocTap() {
        return trangThaiHocTap;
    }

    public void setTrangThaiHocTap(StudentAcademicStatus trangThaiHocTap) {
        this.trangThaiHocTap = trangThaiHocTap;
    }
}
