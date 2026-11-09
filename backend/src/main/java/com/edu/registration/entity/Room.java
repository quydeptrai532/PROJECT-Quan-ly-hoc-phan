
package com.edu.registration.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "phong_hoc",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_phong_hoc_ma",
                        columnNames = "ma_phong"
                )
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phong", nullable = false, length = 30)
    private String ma;

    @Column(name = "suc_chua", nullable = false)
    private int sucChua;

    @Column(name = "loai_phong", length = 50)
    private String loai;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "phong_hoc_thiet_bi",
            joinColumns = @JoinColumn(name = "phong_id")
    )
    @Column(name = "ten_thiet_bi", nullable = false, length = 100)
    private Set<String> thietBi = new LinkedHashSet<>();

    @Column(name = "hoat_dong", nullable = false)
    private boolean hoatDong = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public int getSucChua() {
        return sucChua;
    }

    public void setSucChua(int sucChua) {
        this.sucChua = sucChua;
    }

    public String getLoai() {
        return loai;
    }

    public void setLoai(String loai) {
        this.loai = loai;
    }

    public Set<String> getThietBi() {
        return thietBi;
    }

    public void setThietBi(Set<String> thietBi) {
        this.thietBi = thietBi == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(thietBi);
    }

    public boolean isHoatDong() {
        return hoatDong;
    }

    public void setHoatDong(boolean hoatDong) {
        this.hoatDong = hoatDong;
    }
}
