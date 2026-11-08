package com.edu.registration.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name="nhom_chuong_trinh")
public class CurriculumGroup {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="chuong_trinh_id",nullable=false) private Curriculum chuongTrinh;
 @Column(nullable=false,length=120) private String ten;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private LoaiNhom loai;
 @Column(nullable=false) private int tinChiYeuCau;
 @ManyToMany @JoinTable(name="nhom_chuong_trinh_mon_hoc",joinColumns=@JoinColumn(name="nhom_id"),inverseJoinColumns=@JoinColumn(name="mon_hoc_id")) private Set<Subject> monHoc=new HashSet<>();
 public enum LoaiNhom { BAT_BUOC, TU_CHON }
 public Long getId(){return id;} public Curriculum getChuongTrinh(){return chuongTrinh;} public void setChuongTrinh(Curriculum v){chuongTrinh=v;}
 public String getTen(){return ten;} public void setTen(String v){ten=v;} public LoaiNhom getLoai(){return loai;} public void setLoai(LoaiNhom v){loai=v;}
 public int getTinChiYeuCau(){return tinChiYeuCau;} public void setTinChiYeuCau(int v){tinChiYeuCau=v;} public Set<Subject> getMonHoc(){return monHoc;}
}
