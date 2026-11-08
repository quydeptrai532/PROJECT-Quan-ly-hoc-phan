package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Eligibility is derived from this authenticated student's persisted attempts on every read and write. */
@Service
@Transactional(readOnly = true)
public class RetakeCourseService {
    private static final double DIEM_DAT_HIEN_TAI = 5.0;

    private final CurrentUser currentUser;
    private final StudentRepository sinhVienRepository;
    private final CourseRegistrationRepository dangKyRepository;
    private final GradeRepository diemRepository;
    private final CourseClassRepository lopRepository;
    private final SemesterService hocKyService;

    public RetakeCourseService(CurrentUser currentUser, StudentRepository sinhVienRepository,
                                 CourseRegistrationRepository dangKyRepository, GradeRepository diemRepository,
                                 CourseClassRepository lopRepository, SemesterService hocKyService) {
        this.currentUser = currentUser;
        this.sinhVienRepository = sinhVienRepository;
        this.dangKyRepository = dangKyRepository;
        this.diemRepository = diemRepository;
        this.lopRepository = lopRepository;
        this.hocKyService = hocKyService;
    }

    public List<RegistrationDtos.DangKyHocLaiDto> danhSach(Long hocKyId) {
        Student student = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseRegistration> attempts = dangKyRepository.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(student.getId());
        Map<Long, Grade> grades = gradeMap(attempts);
        Map<Long, List<CourseRegistration>> bySubject = attempts.stream()
                .filter(attempt -> attempt.getLopHocPhan().isBangDiemDaCongBo()
                        && grades.get(attempt.getId()) != null
                        && grades.get(attempt.getId()).getDiemTongKet() != null)
                .collect(Collectors.groupingBy(attempt -> attempt.getLopHocPhan().getMonHoc().getId()));

        return lopRepository.findByHocKyIdOrderByMaAsc(hocKy.getId()).stream()
                .filter(section -> bySubject.containsKey(section.getMonHoc().getId()))
                .map(section -> toDto(section, bySubject.get(section.getMonHoc().getId()), grades))
                .toList();
    }

    private RegistrationDtos.DangKyHocLaiDto toDto(CourseClass section, List<CourseRegistration> attempts,
                                                    Map<Long, Grade> grades) {
        CourseRegistration latestAttempt = attempts.get(0);
        double latest = grades.get(latestAttempt.getId()).getDiemTongKet();
        String type = latest < DIEM_DAT_HIEN_TAI ? "HOC_LAI" : "HOC_CAI_THIEN";
        List<RegistrationDtos.LanHocTruocDto> previous = attempts.stream()
                .sorted(Comparator.comparing((CourseRegistration a) -> a.getLopHocPhan().getHocKy().getNgayBatDau())
                        .thenComparing(CourseRegistration::getId))
                .map(attempt -> new RegistrationDtos.LanHocTruocDto(attempt.getId(),
                        attempt.getLopHocPhan().getMa(), attempt.getLopHocPhan().getHocKy().getTen(),
                        attempt.getThoiGianDangKy(), grades.get(attempt.getId()).getDiemTongKet()))
                .toList();
        return new RegistrationDtos.DangKyHocLaiDto(section.getId(), section.getMa(),
                section.getMonHoc().getId(), section.getMonHoc().getMa(), section.getMonHoc().getTen(),
                section.getMonHoc().getSoTinChi(), section.getGiangVien().getHoTen(), section.getPhong(),
                section.getThu(), section.getTietBatDau(), section.getSoTiet(), section.getSiSoHienTai(),
                section.getSiSoToiDa(), section.getHocKy().dangMoDangKy(), type, previous);
    }

    private Map<Long, Grade> gradeMap(List<CourseRegistration> attempts) {
        if (attempts.isEmpty()) return Map.of();
        return diemRepository.findByDangKyIdIn(attempts.stream().map(CourseRegistration::getId).toList()).stream()
                .collect(Collectors.toMap(grade -> grade.getDangKy().getId(), Function.identity()));
    }
}
