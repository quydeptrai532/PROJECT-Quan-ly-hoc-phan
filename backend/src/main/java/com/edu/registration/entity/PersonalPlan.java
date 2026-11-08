package com.edu.registration.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="ke_hoach_ca_nhan")
public class PersonalPlan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sinh_vien_id",nullable=false) private Student sinhVien;
 @Column(nullable=false,length=120) private String ten;
 @Column(nullable=false) private LocalDateTime capNhatLuc=LocalDateTime.now();
 @OneToMany(mappedBy="keHoach",cascade=CascadeType.ALL,orphanRemoval=true) private List<PlannedCourse> mon=new ArrayList<>();
 public Long getId(){return id;} public Student getSinhVien(){return sinhVien;} public void setSinhVien(Student v){sinhVien=v;} public String getTen(){return ten;} public void setTen(String v){ten=v;} public LocalDateTime getCapNhatLuc(){return capNhatLuc;} public void setCapNhatLuc(LocalDateTime v){capNhatLuc=v;} public List<PlannedCourse> getMon(){return mon;}
}
