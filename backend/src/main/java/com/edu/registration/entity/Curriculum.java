package com.edu.registration.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chuong_trinh_dao_tao", uniqueConstraints = @UniqueConstraint(columnNames = {"nganh_id", "khoa_nhap_hoc", "phien_ban"}))
public class Curriculum {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "nganh_id", nullable = false) private Major nganh;
    @Column(name = "khoa_nhap_hoc", nullable = false, length = 50) private String khoaNhapHoc;
    @Column(nullable = false, length = 80) private String phienBan;
    @OneToMany(mappedBy = "chuongTrinh", cascade = CascadeType.ALL, orphanRemoval = true) private Set<CurriculumGroup> nhom = new HashSet<>();
    public Long getId(){return id;} public void setId(Long v){id=v;} public Major getNganh(){return nganh;} public void setNganh(Major v){nganh=v;}
    public String getKhoaNhapHoc(){return khoaNhapHoc;} public void setKhoaNhapHoc(String v){khoaNhapHoc=v;}
    public String getPhienBan(){return phienBan;} public void setPhienBan(String v){phienBan=v;}
    public Set<CurriculumGroup> getNhom(){return nhom;}
}
