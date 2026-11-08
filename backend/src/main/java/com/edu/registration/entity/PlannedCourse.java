package com.edu.registration.entity;
import jakarta.persistence.*;
@Entity @Table(name="ke_hoach_mon",uniqueConstraints=@UniqueConstraint(columnNames={"ke_hoach_id","lop_hoc_phan_id"}))
public class PlannedCourse {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ke_hoach_id",nullable=false) private PersonalPlan keHoach;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="lop_hoc_phan_id",nullable=false) private CourseClass lopHocPhan;
 public Long getId(){return id;} public PersonalPlan getKeHoach(){return keHoach;} public void setKeHoach(PersonalPlan v){keHoach=v;} public CourseClass getLopHocPhan(){return lopHocPhan;} public void setLopHocPhan(CourseClass v){lopHocPhan=v;}
}
