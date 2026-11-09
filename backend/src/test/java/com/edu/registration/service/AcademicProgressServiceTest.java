package com.edu.registration.service;

import com.edu.registration.entity.*;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicProgressServiceTest {

    @Test
    void noAssignedCurriculumIsExplicitlyUnconfigured() {
        StudentRepository students = mock(StudentRepository.class);
        CurrentUser current = mock(CurrentUser.class);
        CourseRegistrationRepository regs = mock(CourseRegistrationRepository.class);
        GradeRepository grades = mock(GradeRepository.class);
        CurriculumRepository programs = mock(CurriculumRepository.class);

        User u = new User();
        Student sv = new Student();
        sv.setUser(u);
        when(current.get()).thenReturn(u);
        when(students.findByUser(u)).thenReturn(Optional.of(sv));

        Map<String, Object> result = new AcademicProgressService(students, current, regs, grades, programs).get();
        assertEquals(false, result.get("configured"));
        assertTrue(((String) result.get("message")).contains("Chưa được gán"));
        verifyNoInteractions(regs, grades, programs);
    }

    @Test
    void passedSubjectCreditsAreCountedOnlyOnceAndProgressIsAccountScoped() {
        StudentRepository students = mock(StudentRepository.class);
        CurrentUser current = mock(CurrentUser.class);
        CourseRegistrationRepository regs = mock(CourseRegistrationRepository.class);
        GradeRepository grades = mock(GradeRepository.class);
        CurriculumRepository programs = mock(CurriculumRepository.class);

        User u = new User();
        Student sv = new Student();
        sv.setId(7L);
        sv.setUser(u);
        when(current.get()).thenReturn(u);
        when(students.findByUser(u)).thenReturn(Optional.of(sv));

        Curriculum program = new Curriculum();
        program.setId(12L);
        Major major = new Major();
        major.setTen("Kỹ thuật");
        program.setNganh(major);
        program.setKhoaNhapHoc("2025");
        program.setPhienBan("v1");
        when(programs.findWithDetailsById(12L)).thenReturn(Optional.of(program));
        sv.setChuongTrinh(program);

        Subject subject = new Subject();
        subject.setId(3L);
        subject.setMa("CS101");
        subject.setTen("Nhập môn");
        subject.setSoTinChi(3);

        CurriculumGroup group = new CurriculumGroup();
        group.setTen("Bắt buộc");
        group.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);
        group.setTinChiYeuCau(3);
        group.getMonHoc().add(subject);
        group.getMonHoc().add(subject);
        program.getNhom().add(group);

        Semester term = new Semester();
        term.setNgayKetThuc(LocalDate.now().plusDays(10));
        CourseClass section = new CourseClass();
        section.setMonHoc(subject);
        section.setHocKy(term);
        CourseRegistration enrollment = new CourseRegistration();
        enrollment.setId(9L);
        enrollment.setLopHocPhan(section);
        when(regs.findAllActiveOfStudent(7L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of(enrollment));

        Grade grade = new Grade();
        grade.setDangKy(enrollment);
        grade.setDiemTongKet(8.5);
        when(grades.findByDangKyIdIn(List.of(9L))).thenReturn(List.of(grade));

        Map<String, Object> result = new AcademicProgressService(students, current, regs, grades, programs).get();
        assertEquals(3, result.get("requiredCredits"));
        assertEquals(3, result.get("earnedCredits"));
        assertEquals(100, result.get("completionPercent"));

        List<?> groups = (List<?>) result.get("groups");
        Map<?, ?> rendered = (Map<?, ?>) groups.getFirst();
        assertEquals(1, ((List<?>) rendered.get("courses")).size());
        assertEquals("PASSED", ((Map<?, ?>) ((List<?>) rendered.get("courses")).getFirst()).get("status"));
        verify(regs).findAllActiveOfStudent(7L, RegistrationStatus.DA_DANG_KY);
    }

    @Test
    void missingCoursesPlanReturnsUnconfiguredWhenStudentHasNoProgram() {
        StudentRepository students = mock(StudentRepository.class);
        CurrentUser current = mock(CurrentUser.class);
        CourseRegistrationRepository regs = mock(CourseRegistrationRepository.class);
        GradeRepository grades = mock(GradeRepository.class);
        CurriculumRepository programs = mock(CurriculumRepository.class);
        SemesterRepository semesters = mock(SemesterRepository.class);
        CourseClassRepository classes = mock(CourseClassRepository.class);
        PrerequisiteSubjectRepository prereqs = mock(PrerequisiteSubjectRepository.class);

        User u = new User();
        Student sv = new Student();
        sv.setUser(u);
        when(current.get()).thenReturn(u);
        when(students.findByUser(u)).thenReturn(Optional.of(sv));

        AcademicProgressService service = new AcademicProgressService(students, current, regs, grades, programs, semesters, classes, prereqs);
        Map<String, Object> result = service.getMissingCoursesPlan(null);

        assertEquals(false, result.get("configured"));
        assertTrue(((String) result.get("message")).contains("chưa được gán phiên bản"));
        assertNull(result.get("program"));
        assertTrue(((List<?>) result.get("missingGroups")).isEmpty());
        assertTrue(((List<?>) result.get("registrationSuggestions")).isEmpty());
    }

    @Test
    void missingCoursesPlanUsesAssignedVersionSeparatesGroupsAndExcludesPassedCourses() {
        StudentRepository students = mock(StudentRepository.class);
        CurrentUser current = mock(CurrentUser.class);
        CourseRegistrationRepository regs = mock(CourseRegistrationRepository.class);
        GradeRepository grades = mock(GradeRepository.class);
        CurriculumRepository programs = mock(CurriculumRepository.class);
        SemesterRepository semesters = mock(SemesterRepository.class);
        CourseClassRepository classes = mock(CourseClassRepository.class);
        PrerequisiteSubjectRepository prereqs = mock(PrerequisiteSubjectRepository.class);

        User u = new User();
        Student sv = new Student();
        sv.setId(10L);
        sv.setUser(u);
        when(current.get()).thenReturn(u);
        when(students.findByUser(u)).thenReturn(Optional.of(sv));

        // Chương trình được gán cụ thể (Phiên bản v2024.1)
        Curriculum program = new Curriculum();
        program.setId(100L);
        Major major = new Major();
        major.setTen("Công nghệ thông tin");
        program.setNganh(major);
        program.setKhoaNhapHoc("K2024");
        program.setPhienBan("v2024.1");
        sv.setChuongTrinh(program);
        when(programs.findWithDetailsById(100L)).thenReturn(Optional.of(program));

        // Môn 1: CS101 (Bắt buộc, đã đạt)
        Subject cs101 = new Subject();
        cs101.setId(1L);
        cs101.setMa("CS101");
        cs101.setTen("Lập trình cơ bản");
        cs101.setSoTinChi(3);

        // Môn 2: CS102 (Bắt buộc, học rớt -> cần học lại)
        Subject cs102 = new Subject();
        cs102.setId(2L);
        cs102.setMa("CS102");
        cs102.setTen("Cấu trúc dữ liệu");
        cs102.setSoTinChi(4);

        // Môn 3: CS201 (Bắt buộc, chưa học, có tiên quyết là CS102)
        Subject cs201 = new Subject();
        cs201.setId(3L);
        cs201.setMa("CS201");
        cs201.setTen("Thuật toán nâng cao");
        cs201.setSoTinChi(3);

        // Nhóm bắt buộc: CS101, CS102, CS201 (yêu cầu 10 TC)
        CurriculumGroup batBuoc = new CurriculumGroup();
        batBuoc.setTen("Khối kiến thức cơ sở");
        batBuoc.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);
        batBuoc.setTinChiYeuCau(10);
        batBuoc.getMonHoc().addAll(List.of(cs101, cs102, cs201));
        program.getNhom().add(batBuoc);

        // Môn 4: ELEC01 (Tự chọn, chưa học)
        Subject elec01 = new Subject();
        elec01.setId(4L);
        elec01.setMa("ELEC01");
        elec01.setTen("Phát triển Web");
        elec01.setSoTinChi(3);

        CurriculumGroup tuChon = new CurriculumGroup();
        tuChon.setTen("Tự chọn chuyên ngành");
        tuChon.setLoai(CurriculumGroup.LoaiNhom.TU_CHON);
        tuChon.setTinChiYeuCau(3);
        tuChon.getMonHoc().add(elec01);
        program.getNhom().add(tuChon);

        // Lịch sử học tập:
        // CS101: điểm 8.0 (đạt)
        Semester sem1 = new Semester();
        sem1.setId(50L);
        sem1.setTen("HK1");
        sem1.setNamHoc("2024-2025");
        CourseClass cl1 = new CourseClass();
        cl1.setId(501L);
        cl1.setMa("CS101.01");
        cl1.setMonHoc(cs101);
        cl1.setHocKy(sem1);
        cl1.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        CourseRegistration reg1 = new CourseRegistration();
        reg1.setId(1001L);
        reg1.setLopHocPhan(cl1);
        Grade g1 = new Grade();
        g1.setDangKy(reg1);
        g1.setDiemTongKet(8.0);

        // CS102: điểm 3.5 (chưa đạt -> HOC_LAI)
        CourseClass cl2 = new CourseClass();
        cl2.setId(502L);
        cl2.setMa("CS102.01");
        cl2.setMonHoc(cs102);
        cl2.setHocKy(sem1);
        cl2.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        CourseRegistration reg2 = new CourseRegistration();
        reg2.setId(1002L);
        reg2.setLopHocPhan(cl2);
        Grade g2 = new Grade();
        g2.setDangKy(reg2);
        g2.setDiemTongKet(3.5);

        when(regs.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(10L))
                .thenReturn(List.of(reg1, reg2));
        when(grades.findByDangKyIdIn(List.of(1001L, 1002L)))
                .thenReturn(List.of(g1, g2));
        when(regs.findAllActiveOfStudent(10L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of());

        // Quan hệ tiên quyết: CS201 cần CS102 >= 5.0
        PrerequisiteSubject prereq = new PrerequisiteSubject(cs201, cs102);
        prereq.setLoaiQuanHe(SubjectRelationType.TIEN_QUYET);
        prereq.setDieuKienDat(5.0);
        prereq.setYeuCauDat(true);
        when(prereqs.findByMonHocIdInOrderByIdAsc(anySet())).thenReturn(List.of(prereq));
        when(prereqs.findAllByOrderByIdAsc()).thenReturn(List.of(prereq));

        // Học kỳ tới: HK2 2024-2025
        Semester targetSem = new Semester();
        targetSem.setId(60L);
        targetSem.setTen("HK2");
        targetSem.setNamHoc("2024-2025");
        targetSem.setSoTinChiToiDa(24);
        targetSem.setDangKyTu(LocalDate.now().minusDays(1));
        targetSem.setDangKyDen(LocalDate.now().plusDays(5));
        when(semesters.findById(60L)).thenReturn(Optional.of(targetSem));

        // Các lớp mở kỳ tới:
        // Lớp CS102.02 (mở cho CS102, còn chỗ, không trùng)
        CourseClass openCs102 = new CourseClass();
        openCs102.setId(601L);
        openCs102.setMa("CS102.02");
        openCs102.setMonHoc(cs102);
        openCs102.setHocKy(targetSem);
        openCs102.setThu(2);
        openCs102.setTietBatDau(1);
        openCs102.setSoTiet(3);
        openCs102.setSiSoToiDa(50);
        openCs102.setSiSoHienTai(20);

        // Lớp CS201.01 (mở cho CS201, nhưng sinh viên chưa đạt tiên quyết CS102)
        CourseClass openCs201 = new CourseClass();
        openCs201.setId(602L);
        openCs201.setMa("CS201.01");
        openCs201.setMonHoc(cs201);
        openCs201.setHocKy(targetSem);
        openCs201.setThu(4);
        openCs201.setTietBatDau(1);
        openCs201.setSoTiet(3);
        openCs201.setSiSoToiDa(50);
        openCs201.setSiSoHienTai(10);

        // Lớp ELEC01 không có lớp nào mở trong kỳ tới!
        when(classes.findByHocKyIdOrderByMaAsc(60L)).thenReturn(List.of(openCs102, openCs201));

        AcademicProgressService service = new AcademicProgressService(students, current, regs, grades, programs, semesters, classes, prereqs);
        Map<String, Object> plan = service.getMissingCoursesPlan(60L);

        // Kiểm tra cấu hình và phiên bản
        assertEquals(true, plan.get("configured"));
        Map<?, ?> progDto = (Map<?, ?>) plan.get("program");
        assertEquals("v2024.1", progDto.get("version"));
        assertEquals("K2024", progDto.get("cohort"));

        // Kiểm tra thống kê tín chỉ
        Map<?, ?> summary = (Map<?, ?>) plan.get("summary");
        assertEquals(13, summary.get("totalRequiredCredits")); // 10 bắt buộc + 3 tự chọn
        assertEquals(3, summary.get("totalEarnedCredits"));   // đã đạt CS101 (3 TC)
        assertEquals(10, summary.get("totalMissingCredits")); // còn thiếu 7 bắt buộc + 3 tự chọn

        // Kiểm tra phân nhóm còn thiếu
        List<?> groups = (List<?>) plan.get("missingGroups");
        assertEquals(2, groups.size());

        Map<?, ?> group1 = (Map<?, ?>) groups.stream()
                .filter(g -> "BAT_BUOC".equals(((Map<?, ?>) g).get("groupType")))
                .findFirst().orElseThrow();
        List<?> group1Missing = (List<?>) group1.get("missingCourses");
        // CS101 đã đạt nên KHÔNG có trong danh sách môn thiếu!
        assertEquals(2, group1Missing.size());

        Map<?, ?> missing1 = (Map<?, ?>) group1Missing.get(0);
        assertEquals("CS102", missing1.get("subjectCode"));
        assertEquals("HOC_LAI", missing1.get("studyStatus"));
        assertEquals(true, missing1.get("eligible")); // không có tiên quyết nên đủ điều kiện học lại

        Map<?, ?> missing2 = (Map<?, ?>) group1Missing.get(1);
        assertEquals("CS201", missing2.get("subjectCode"));
        assertEquals(false, missing2.get("eligible")); // chưa đạt CS102
        assertTrue(((String) missing2.get("eligibilityReason")).contains("Chưa đạt môn tiên quyết: CS102"));

        // Kiểm tra gợi ý đăng ký
        List<?> suggestions = (List<?>) plan.get("registrationSuggestions");
        assertEquals(1, suggestions.size());
        Map<?, ?> sug = (Map<?, ?>) suggestions.get(0);
        assertEquals("CS102.02", sug.get("classCode"));
        assertTrue(((String) sug.get("reason")).contains("Môn bắt buộc còn thiếu"));

        // Kiểm tra danh sách loại trừ có giải thích lý do
        List<?> excluded = (List<?>) plan.get("excludedClasses");
        assertEquals(1, excluded.size());
        Map<?, ?> exc = (Map<?, ?>) excluded.get(0);
        assertEquals("CS201.01", exc.get("classCode"));
        assertTrue(((String) exc.get("reason")).contains("Chưa đủ điều kiện tiên quyết"));

        // Kiểm tra môn không mở kỳ tới được thể hiện riêng biệt
        List<?> unoffered = (List<?>) plan.get("unofferedMissingCourses");
        assertEquals(1, unoffered.size());
        Map<?, ?> unoff = (Map<?, ?>) unoffered.get(0);
        assertEquals("ELEC01", unoff.get("subjectCode"));
        assertTrue(((String) unoff.get("note")).contains("không có lớp học phần nào mở"));

        // Kiểm tra cảnh báo nguy cơ chậm tốt nghiệp
        List<?> warnings = (List<?>) plan.get("graduationDelayWarnings");
        assertFalse(warnings.isEmpty());

        boolean hasPrereqChainWarning = warnings.stream()
                .anyMatch(w -> "PREREQUISITE_CHAIN".equals(((Map<?, ?>) w).get("type"))
                        && ((String) ((Map<?, ?>) w).get("description")).contains("CS102"));
        assertTrue(hasPrereqChainWarning);

        boolean hasCreditWarning = warnings.stream()
                .anyMatch(w -> "MISSING_CREDITS".equals(((Map<?, ?>) w).get("type")));
        assertTrue(hasCreditWarning);

        // Kiểm tra có nêu căn cứ cụ thể
        warnings.forEach(w -> {
            assertNotNull(((Map<?, ?>) w).get("basis"));
            assertFalse(((String) ((Map<?, ?>) w).get("basis")).isBlank());
        });

        // Kiểm tra lưu ý quy chế và giới hạn dữ liệu
        List<?> disclaimers = (List<?>) plan.get("limitationsAndDisclaimers");
        assertTrue(disclaimers.stream().anyMatch(d -> ((String) d).contains("tham khảo")));
        assertTrue(disclaimers.stream().anyMatch(d -> ((String) d).contains("chưa cấu hình chu kỳ mở môn")));
        assertTrue(disclaimers.stream().anyMatch(d -> ((String) d).contains("học lại")));
    }
}
