package com.edu.registration.service;

import com.edu.registration.entity.*;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicProgressServiceTest {
 @Test void noAssignedCurriculumIsExplicitlyUnconfigured(){
  StudentRepository students=mock(StudentRepository.class);CurrentUser current=mock(CurrentUser.class);CourseRegistrationRepository regs=mock(CourseRegistrationRepository.class);GradeRepository grades=mock(GradeRepository.class);CurriculumRepository programs=mock(CurriculumRepository.class);
  User u=new User();Student sv=new Student();sv.setUser(u);when(current.get()).thenReturn(u);when(students.findByUser(u)).thenReturn(Optional.of(sv));
  Map<String,Object> result=new AcademicProgressService(students,current,regs,grades,programs).get();
  assertEquals(false,result.get("configured"));assertTrue(((String)result.get("message")).contains("Chưa được gán"));verifyNoInteractions(regs,grades,programs);
 }

 @Test void passedSubjectCreditsAreCountedOnlyOnceAndProgressIsAccountScoped(){
  StudentRepository students=mock(StudentRepository.class);CurrentUser current=mock(CurrentUser.class);CourseRegistrationRepository regs=mock(CourseRegistrationRepository.class);GradeRepository grades=mock(GradeRepository.class);CurriculumRepository programs=mock(CurriculumRepository.class);
  User u=new User();Student sv=new Student();sv.setId(7L);sv.setUser(u);when(current.get()).thenReturn(u);when(students.findByUser(u)).thenReturn(Optional.of(sv));
  Curriculum program=new Curriculum();program.setId(12L);Major major=new Major();major.setTen("Kỹ thuật");program.setNganh(major);program.setKhoaNhapHoc("2025");program.setPhienBan("v1");when(programs.findWithDetailsById(12L)).thenReturn(Optional.of(program));sv.setChuongTrinh(program);
  Subject subject=new Subject();subject.setId(3L);subject.setMa("CS101");subject.setTen("Nhập môn");subject.setSoTinChi(3);
  CurriculumGroup group=new CurriculumGroup();group.setTen("Bắt buộc");group.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);group.setTinChiYeuCau(3);group.getMonHoc().add(subject);group.getMonHoc().add(subject);program.getNhom().add(group);
  Semester term=new Semester();term.setNgayKetThuc(java.time.LocalDate.now().plusDays(10));CourseClass section=new CourseClass();section.setMonHoc(subject);section.setHocKy(term);CourseRegistration enrollment=new CourseRegistration();enrollment.setId(9L);enrollment.setLopHocPhan(section);when(regs.findAllActiveOfStudent(7L,RegistrationStatus.DA_DANG_KY)).thenReturn(List.of(enrollment));
  Grade grade=new Grade();grade.setDangKy(enrollment);grade.setDiemTongKet(8.5);when(grades.findByDangKyIdIn(List.of(9L))).thenReturn(List.of(grade));
  Map<String,Object> result=new AcademicProgressService(students,current,regs,grades,programs).get();
  assertEquals(3,result.get("requiredCredits"));assertEquals(3,result.get("earnedCredits"));assertEquals(100,result.get("completionPercent"));
  List<?> groups=(List<?>)result.get("groups");Map<?,?> rendered=(Map<?,?>)groups.getFirst();assertEquals(1,((List<?>)rendered.get("courses")).size());assertEquals("PASSED",((Map<?,?>)((List<?>)rendered.get("courses")).getFirst()).get("status"));
  verify(regs).findAllActiveOfStudent(7L,RegistrationStatus.DA_DANG_KY);
 }
}
