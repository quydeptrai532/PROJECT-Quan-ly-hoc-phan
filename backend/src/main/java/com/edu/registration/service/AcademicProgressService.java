package com.edu.registration.service;

import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class AcademicProgressService {
 private static final double PASS=AcademicRules.DIEM_DAT;
 private final StudentRepository students; private final CurrentUser current; private final CourseRegistrationRepository registrations;
 private final GradeRepository grades; private final CurriculumRepository programs;
 public AcademicProgressService(StudentRepository s,CurrentUser c,CourseRegistrationRepository r,GradeRepository g,CurriculumRepository p){students=s;current=c;registrations=r;grades=g;programs=p;}
 public Map<String,Object> get(){
  Student sv=students.findByUser(current.get()).orElseThrow(()->ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
  Map<String,Object> out=new LinkedHashMap<>();
  if(sv.getChuongTrinh()==null){out.put("configured",false);out.put("message","Chưa được gán chương trình đào tạo; chưa thể tính tỷ lệ hoàn thành.");out.put("groups",List.of());out.put("rules",List.of("Điểm tổng kết từ 5.0 được xem là đạt theo quy tắc đăng ký hiện có."));return out;}
  Curriculum p=programs.findWithDetailsById(sv.getChuongTrinh().getId()).orElseThrow(()->ApiException.notFound("Không tìm thấy chương trình"));
  List<CourseRegistration> dks=registrations.findAllActiveOfStudent(sv.getId(),RegistrationStatus.DA_DANG_KY);
  Map<Long,Grade> byReg=grades.findByDangKyIdIn(dks.stream().map(CourseRegistration::getId).toList()).stream().collect(Collectors.toMap(d->d.getDangKy().getId(),d->d));
  Map<Long,List<CourseRegistration>> attempts=dks.stream().collect(Collectors.groupingBy(d->d.getLopHocPhan().getMonHoc().getId()));
  Set<Long> seen=new HashSet<>(); List<Map<String,Object>> groups=new ArrayList<>(); int total=0,earned=0; boolean complete=true;
  for(CurriculumGroup group:p.getNhom()){
   int gt=group.getTinChiYeuCau(), ge=0; List<Map<String,Object>> courses=new ArrayList<>();
   List<Subject> subjects=group.getMonHoc().stream().sorted(Comparator.comparing(Subject::getMa)).toList();
   for(Subject m:subjects){ if(!seen.add(m.getId())) continue; List<CourseRegistration> a=attempts.getOrDefault(m.getId(),List.of());
    boolean passed=a.stream().filter(d->d.getLopHocPhan().isBangDiemDaCongBo()).map(d->byReg.get(d.getId())).filter(Objects::nonNull).map(Grade::getDiemTongKet).filter(Objects::nonNull).anyMatch(x->x>=PASS);
    boolean active=a.stream().anyMatch(d->d.getLopHocPhan().getHocKy().getDangKyTu()!=null && !LocalDate.now().isAfter(d.getLopHocPhan().getHocKy().getNgayKetThuc()) && (!d.getLopHocPhan().isBangDiemDaCongBo() || byReg.get(d.getId())==null || byReg.get(d.getId()).getDiemTongKet()==null));
    boolean failed=a.stream().filter(d->d.getLopHocPhan().isBangDiemDaCongBo()).map(d->byReg.get(d.getId())).filter(Objects::nonNull).map(Grade::getDiemTongKet).filter(Objects::nonNull).anyMatch(x->x<PASS);
    String status=passed?"PASSED":active?"IN_PROGRESS":failed?"RETAKE":"NOT_STUDIED";
    if(passed) ge=m.getSoTinChi();
    Map<String,Object> course=new LinkedHashMap<>();course.put("id",m.getId());course.put("code",m.getMa());course.put("name",m.getTen());course.put("credits",m.getSoTinChi());course.put("status",status);course.put("reason",passed?"Có ít nhất một kết quả đạt.":active?"Đang có đăng ký trong học kỳ chưa kết thúc.":failed?"Có kết quả dưới ngưỡng đạt; cần học lại.":"Chưa có kết quả đạt hoặc đăng ký đang học.");courses.add(course);
   }
   int groupEarned=Math.min(gt,ge); total+=gt;earned+=groupEarned;if(groupEarned<gt)complete=false;
   groups.add(Map.of("name",group.getTen(),"type",group.getLoai().name(),"requiredCredits",gt,"earnedCredits",groupEarned,"courses",courses));
  }
  out.put("configured",true);out.put("program",Map.of("id",p.getId(),"major",p.getNganh().getTen(),"cohort",p.getKhoaNhapHoc(),"version",p.getPhienBan()));out.put("groups",groups);out.put("requiredCredits",total);out.put("earnedCredits",earned);out.put("completionPercent",total==0?null:Math.min(100,earned*100/total));out.put("complete",complete && total>0);out.put("rules",List.of("Chỉ học phần gắn trong chương trình được tính.","Một môn và tín chỉ của môn chỉ được tính một lần.","Đạt khi điểm tổng kết >= 5.0 theo quy tắc đang có.","Tín chỉ môn tự chọn được giới hạn ở yêu cầu của nhóm."));return out;
 }
}
