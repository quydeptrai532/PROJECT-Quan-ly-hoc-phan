package com.edu.registration.service;

import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AcademicProgressService {

    private static final double PASS = AcademicRules.DIEM_DAT;

    private final StudentRepository students;
    private final CurrentUser current;
    private final CourseRegistrationRepository registrations;
    private final GradeRepository grades;
    private final CurriculumRepository programs;
    private final SemesterRepository semesters;
    private final CourseClassRepository classes;
    private final PrerequisiteSubjectRepository prereqs;

    public AcademicProgressService(StudentRepository s, CurrentUser c, CourseRegistrationRepository r,
                                   GradeRepository g, CurriculumRepository p) {
        this(s, c, r, g, p, null, null, null);
    }

    @Autowired
    public AcademicProgressService(StudentRepository students,
                                   CurrentUser current,
                                   CourseRegistrationRepository registrations,
                                   GradeRepository grades,
                                   CurriculumRepository programs,
                                   @Autowired(required = false) SemesterRepository semesters,
                                   @Autowired(required = false) CourseClassRepository classes,
                                   @Autowired(required = false) PrerequisiteSubjectRepository prereqs) {
        this.students = students;
        this.current = current;
        this.registrations = registrations;
        this.grades = grades;
        this.programs = programs;
        this.semesters = semesters;
        this.classes = classes;
        this.prereqs = prereqs;
    }

    public Map<String, Object> get() {
        Student sv = students.findByUser(current.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        Map<String, Object> out = new LinkedHashMap<>();
        if (sv.getChuongTrinh() == null) {
            out.put("configured", false);
            out.put("message", "Chưa được gán chương trình đào tạo; chưa thể tính tỷ lệ hoàn thành.");
            out.put("groups", List.of());
            out.put("rules", List.of("Điểm tổng kết từ 5.0 được xem là đạt theo quy tắc đăng ký hiện có."));
            return out;
        }

        Curriculum p = programs.findWithDetailsById(sv.getChuongTrinh().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chương trình"));
        List<CourseRegistration> dks = registrations.findAllActiveOfStudent(sv.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> byReg = grades.findByDangKyIdIn(dks.stream().map(CourseRegistration::getId).toList())
                .stream().collect(Collectors.toMap(d -> d.getDangKy().getId(), d -> d));
        Map<Long, List<CourseRegistration>> attempts = dks.stream()
                .collect(Collectors.groupingBy(d -> d.getLopHocPhan().getMonHoc().getId()));

        Set<Long> seen = new HashSet<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        int total = 0, earned = 0;
        boolean complete = true;

        for (CurriculumGroup group : p.getNhom()) {
            int gt = group.getTinChiYeuCau(), ge = 0;
            List<Map<String, Object>> courses = new ArrayList<>();
            List<Subject> subjects = group.getMonHoc().stream().sorted(Comparator.comparing(Subject::getMa)).toList();
            for (Subject m : subjects) {
                if (!seen.add(m.getId())) continue;
                List<CourseRegistration> a = attempts.getOrDefault(m.getId(), List.of());
                boolean passed = a.stream().filter(d -> d.getLopHocPhan().isBangDiemDaCongBo())
                        .map(d -> byReg.get(d.getId())).filter(Objects::nonNull)
                        .map(Grade::getDiemTongKet).filter(Objects::nonNull).anyMatch(x -> x >= PASS);
                boolean active = a.stream().anyMatch(d -> d.getLopHocPhan().getHocKy().getDangKyTu() != null
                        && !LocalDate.now().isAfter(d.getLopHocPhan().getHocKy().getNgayKetThuc())
                        && (!d.getLopHocPhan().isBangDiemDaCongBo() || byReg.get(d.getId()) == null || byReg.get(d.getId()).getDiemTongKet() == null));
                boolean failed = a.stream().filter(d -> d.getLopHocPhan().isBangDiemDaCongBo())
                        .map(d -> byReg.get(d.getId())).filter(Objects::nonNull)
                        .map(Grade::getDiemTongKet).filter(Objects::nonNull).anyMatch(x -> x < PASS);
                String status = passed ? "PASSED" : active ? "IN_PROGRESS" : failed ? "RETAKE" : "NOT_STUDIED";
                if (passed) {
                    ge += m.getSoTinChi();
                }
                Map<String, Object> course = new LinkedHashMap<>();
                course.put("id", m.getId());
                course.put("code", m.getMa());
                course.put("name", m.getTen());
                course.put("credits", m.getSoTinChi());
                course.put("status", status);
                course.put("reason", passed ? "Có ít nhất một kết quả đạt." :
                        active ? "Đang có đăng ký trong học kỳ chưa kết thúc." :
                                failed ? "Có kết quả dưới ngưỡng đạt; cần học lại." :
                                        "Chưa có kết quả đạt hoặc đăng ký đang học.");
                courses.add(course);
            }
            int groupEarned = Math.min(gt, ge);
            total += gt;
            earned += groupEarned;
            if (groupEarned < gt) complete = false;
            groups.add(Map.of("name", group.getTen(), "type", group.getLoai().name(),
                    "requiredCredits", gt, "earnedCredits", groupEarned, "courses", courses));
        }

        out.put("configured", true);
        out.put("program", Map.of("id", p.getId(), "major", p.getNganh().getTen(),
                "cohort", p.getKhoaNhapHoc(), "version", p.getPhienBan()));
        out.put("groups", groups);
        out.put("requiredCredits", total);
        out.put("earnedCredits", earned);
        out.put("completionPercent", total == 0 ? null : Math.min(100, earned * 100 / total));
        out.put("complete", complete && total > 0);
        out.put("rules", List.of(
                "Chỉ học phần gắn trong chương trình được tính.",
                "Một môn và tín chỉ của môn chỉ được tính một lần.",
                "Đạt khi điểm tổng kết >= 5.0 theo quy tắc đang có.",
                "Tín chỉ môn tự chọn được giới hạn ở yêu cầu của nhóm."
        ));
        return out;
    }

    /**
     * Kế hoạch môn còn thiếu (Academic Planner - QLSV-96 / TASK-OP-09).
     * Dùng đúng phiên bản chương trình được gán của sinh viên; không gộp dữ liệu từ phiên bản khác.
     */
    public Map<String, Object> getMissingCoursesPlan(Long hocKyId) {
        Student sv = students.findByUser(current.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        Map<String, Object> out = new LinkedHashMap<>();

        if (sv.getChuongTrinh() == null) {
            out.put("configured", false);
            out.put("message", "Sinh viên chưa được gán phiên bản chương trình đào tạo; chưa thể lập kế hoạch môn còn thiếu.");
            out.put("program", null);
            out.put("summary", null);
            out.put("missingGroups", List.of());
            out.put("registrationSuggestions", List.of());
            out.put("excludedClasses", List.of());
            out.put("unofferedMissingCourses", List.of());
            out.put("graduationDelayWarnings", List.of());
            out.put("limitationsAndDisclaimers", List.of(
                    "Cần được gán phiên bản chương trình đào tạo cụ thể trước khi hệ thống có thể đối chiếu môn còn thiếu.",
                    "Quy chế thay điểm và tính GPA/tín chỉ cho lần học lại, học cải thiện chưa được cấu hình. Hệ thống không tự quyết quy tắc thay điểm hoặc GPA."
            ));
            return out;
        }

        // 1. Khảo sát đúng phiên bản chương trình được gán
        Curriculum program = programs.findWithDetailsById(sv.getChuongTrinh().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chương trình đào tạo"));
        String cohort = program.getKhoaNhapHoc() != null ? program.getKhoaNhapHoc().trim() : null;

        // 2. Toàn bộ lịch sử đăng ký và điểm đã công bố của sinh viên
        List<CourseRegistration> allAttempts = registrations.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(sv.getId());
        List<Long> regIds = allAttempts.stream().map(CourseRegistration::getId).toList();
        Map<Long, Grade> gradeMap = regIds.isEmpty() ? Map.of() :
                grades.findByDangKyIdIn(regIds).stream()
                        .collect(Collectors.toMap(g -> g.getDangKy().getId(), g -> g));

        // Nhóm các lần học theo môn học
        Map<Long, List<CourseRegistration>> attemptsBySubject = allAttempts.stream()
                .collect(Collectors.groupingBy(d -> d.getLopHocPhan().getMonHoc().getId()));

        Set<Long> passedSubjectIds = new HashSet<>();
        Map<Long, Double> latestPublishedScore = new HashMap<>();
        Map<Long, List<Map<String, Object>>> attemptDetailsMap = new HashMap<>();

        for (Map.Entry<Long, List<CourseRegistration>> entry : attemptsBySubject.entrySet()) {
            Long subId = entry.getKey();
            List<CourseRegistration> subAttempts = entry.getValue();
            List<Map<String, Object>> attList = new ArrayList<>();
            Double latestScore = null;
            boolean hasPassed = false;

            for (CourseRegistration reg : subAttempts) {
                Grade g = gradeMap.get(reg.getId());
                boolean isPublished = reg.getLopHocPhan().isBangDiemDaCongBo();
                Double finalGrade = (isPublished && g != null) ? g.getDiemTongKet() : null;
                if (finalGrade != null) {
                    if (latestScore == null) {
                        latestScore = finalGrade;
                    }
                    if (finalGrade >= PASS) {
                        hasPassed = true;
                    }
                }
                Map<String, Object> attInfo = new LinkedHashMap<>();
                attInfo.put("registrationId", reg.getId());
                attInfo.put("classCode", reg.getLopHocPhan().getMa());
                attInfo.put("semesterName", reg.getLopHocPhan().getHocKy().getTen() + " " + reg.getLopHocPhan().getHocKy().getNamHoc());
                attInfo.put("score", finalGrade);
                attInfo.put("isPublished", isPublished);
                attInfo.put("passed", finalGrade != null && finalGrade >= PASS);
                attList.add(attInfo);
            }
            if (hasPassed) {
                passedSubjectIds.add(subId);
            }
            if (latestScore != null) {
                latestPublishedScore.put(subId, latestScore);
            }
            attemptDetailsMap.put(subId, attList);
        }

        // Đăng ký đang học (active)
        List<CourseRegistration> activeRegs = registrations.findAllActiveOfStudent(sv.getId(), RegistrationStatus.DA_DANG_KY);

        // 3. Khảo sát học kỳ mục tiêu (kỳ tới / kỳ đang chọn)
        Semester resolvedTargetSemester = null;
        if (semesters != null) {
            if (hocKyId != null) {
                resolvedTargetSemester = semesters.findById(hocKyId).orElse(null);
            }
            if (resolvedTargetSemester == null) {
                LocalDate today = LocalDate.now();
                resolvedTargetSemester = semesters.findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(today, today)
                        .or(() -> semesters.findFirstByOrderByDangKyDenDesc())
                        .orElse(null);
            }
        }
        final Semester targetSemester = resolvedTargetSemester;
        final Long targetTermId = targetSemester != null ? targetSemester.getId() : null;

        // 4. Quan hệ tiên quyết và song hành cho các môn trong chương trình
        Set<Long> curriculumSubjectIds = program.getNhom().stream()
                .flatMap(g -> g.getMonHoc().stream())
                .map(Subject::getId)
                .collect(Collectors.toSet());

        List<PrerequisiteSubject> allPrereqs = prereqs != null && !curriculumSubjectIds.isEmpty() ?
                prereqs.findByMonHocIdInOrderByIdAsc(curriculumSubjectIds) : List.of();
        Map<Long, List<PrerequisiteSubject>> prereqsBySubject = allPrereqs.stream()
                .collect(Collectors.groupingBy(q -> q.getMonHoc().getId()));

        // Tất cả quan hệ tiên quyết trong toàn trường để dò chuỗi phụ thuộc phục vụ cảnh báo
        List<PrerequisiteSubject> systemAllPrereqs = prereqs != null ? prereqs.findAllByOrderByIdAsc() : List.of();

        // 5. Phân nhóm môn còn thiếu thành Bắt buộc và Tự chọn (không liệt kê môn đã đạt như môn thiếu)
        int totalRequired = 0, totalEarned = 0;
        int mandatoryRequired = 0, mandatoryEarned = 0;
        int electiveRequired = 0, electiveEarned = 0;

        List<Map<String, Object>> missingGroups = new ArrayList<>();
        List<Map<String, Object>> allMissingCourses = new ArrayList<>();
        Set<Long> missingSubjectIdSet = new HashSet<>();

        List<CurriculumGroup> sortedGroups = program.getNhom().stream()
                .sorted(Comparator.comparing((CurriculumGroup g) -> g.getLoai() == CurriculumGroup.LoaiNhom.BAT_BUOC ? 0 : 1)
                        .thenComparing(CurriculumGroup::getTen, Comparator.nullsLast(String::compareTo)))
                .toList();

        for (CurriculumGroup group : sortedGroups) {
            boolean isMandatory = group.getLoai() == CurriculumGroup.LoaiNhom.BAT_BUOC;
            int groupReq = group.getTinChiYeuCau();
            int groupEarnedRaw = 0;

            List<Map<String, Object>> groupMissingCourses = new ArrayList<>();
            List<Subject> sortedSubjects = group.getMonHoc().stream()
                    .sorted(Comparator.comparing(Subject::getMa)).toList();

            for (Subject subj : sortedSubjects) {
                boolean isPassed = passedSubjectIds.contains(subj.getId());
                if (isPassed) {
                    groupEarnedRaw += subj.getSoTinChi();
                    // Môn đã đạt KHÔNG được liệt kê như môn thiếu
                    continue;
                }

                // Môn chưa đạt: phân loại trạng thái học tập theo chính sách hiện hành
                List<CourseRegistration> subAttempts = attemptsBySubject.getOrDefault(subj.getId(), List.of());
                boolean hasActive = subAttempts.stream().anyMatch(d -> d.getLopHocPhan().getHocKy().getDangKyTu() != null
                        && !LocalDate.now().isAfter(d.getLopHocPhan().getHocKy().getNgayKetThuc())
                        && (!d.getLopHocPhan().isBangDiemDaCongBo() || gradeMap.get(d.getId()) == null || gradeMap.get(d.getId()).getDiemTongKet() == null));
                boolean hasFailed = subAttempts.stream().anyMatch(d -> d.getLopHocPhan().isBangDiemDaCongBo()
                        && gradeMap.get(d.getId()) != null && gradeMap.get(d.getId()).getDiemTongKet() != null
                        && gradeMap.get(d.getId()).getDiemTongKet() < PASS);

                String studyStatus = hasFailed ? "HOC_LAI" : hasActive ? "DANG_HOC" : "CHUA_HOC";
                String studyStatusLabel = hasFailed ? "Cần học lại" : hasActive ? "Đang học kỳ này" : "Chưa học";

                // Kiểm tra quan hệ tiên quyết / song hành
                List<PrerequisiteSubject> subjectRelations = prereqsBySubject.getOrDefault(subj.getId(), List.of());
                List<Map<String, Object>> unmetPrereqs = new ArrayList<>();
                List<Map<String, Object>> coRequisites = new ArrayList<>();
                boolean eligible = true;

                for (PrerequisiteSubject rel : subjectRelations) {
                    if (!rel.laHieuLucCho(targetSemester, cohort, LocalDate.now())) {
                        continue;
                    }
                    if (rel.laQuanHeTienQuyet() && rel.isYeuCauDatHieuLuc()) {
                        double reqScore = rel.getDieuKienDatHieuLuc();
                        Long preId = rel.getMonTienQuyet().getId();
                        Double preScore = latestPublishedScore.get(preId);
                        boolean prePassed = passedSubjectIds.contains(preId) && (preScore == null || preScore >= reqScore);
                        if (!prePassed) {
                            eligible = false;
                            Map<String, Object> unmet = new LinkedHashMap<>();
                            unmet.put("subjectId", preId);
                            unmet.put("subjectCode", rel.getMonTienQuyet().getMa());
                            unmet.put("subjectName", rel.getMonTienQuyet().getTen());
                            unmet.put("requiredScore", reqScore);
                            unmet.put("actualScore", preScore);
                            unmet.put("reason", "Chưa đạt môn tiên quyết: " + rel.getMonTienQuyet().getMa() + " - " + rel.getMonTienQuyet().getTen()
                                    + " (yêu cầu điểm tổng kết công bố >= " + reqScore + ")");
                            unmetPrereqs.add(unmet);
                        }
                    } else if (rel.getLoaiQuanHeHieuLuc() == SubjectRelationType.SONG_HANH) {
                        Map<String, Object> coreq = new LinkedHashMap<>();
                        coreq.put("subjectId", rel.getMonTienQuyet().getId());
                        coreq.put("subjectCode", rel.getMonTienQuyet().getMa());
                        coreq.put("subjectName", rel.getMonTienQuyet().getTen());
                        coreq.put("note", "Môn song hành: " + rel.getMonTienQuyet().getMa() + " - " + rel.getMonTienQuyet().getTen()
                                + " (khuyến nghị học đồng thời hoặc đã học; quy chế tự động chặn chờ chủ dự án chốt).");
                        coRequisites.add(coreq);
                    }
                }

                String eligibilityReason = eligible
                        ? "Đủ điều kiện tiên quyết theo cấu hình hiện hành."
                        : "Chưa đủ điều kiện tiên quyết: " + unmetPrereqs.stream().map(u -> (String) u.get("reason")).collect(Collectors.joining("; "));

                Map<String, Object> courseItem = new LinkedHashMap<>();
                courseItem.put("subjectId", subj.getId());
                courseItem.put("subjectCode", subj.getMa());
                courseItem.put("subjectName", subj.getTen());
                courseItem.put("credits", subj.getSoTinChi());
                courseItem.put("groupName", group.getTen());
                courseItem.put("groupType", group.getLoai().name());
                courseItem.put("studyStatus", studyStatus);
                courseItem.put("studyStatusLabel", studyStatusLabel);
                courseItem.put("latestScore", latestPublishedScore.get(subj.getId()));
                courseItem.put("attempts", attemptDetailsMap.getOrDefault(subj.getId(), List.of()));
                courseItem.put("eligible", eligible);
                courseItem.put("eligibilityReason", eligibilityReason);
                courseItem.put("unmetPrerequisites", unmetPrereqs);
                courseItem.put("coRequisites", coRequisites);

                groupMissingCourses.add(courseItem);
                allMissingCourses.add(courseItem);
                missingSubjectIdSet.add(subj.getId());
            }

            int groupEarned = isMandatory ? groupEarnedRaw : Math.min(groupReq, groupEarnedRaw);
            int groupMissing = Math.max(0, groupReq - groupEarned);

            totalRequired += groupReq;
            totalEarned += groupEarned;
            if (isMandatory) {
                mandatoryRequired += groupReq;
                mandatoryEarned += groupEarned;
            } else {
                electiveRequired += groupReq;
                electiveEarned += groupEarned;
            }

            Map<String, Object> grpMap = new LinkedHashMap<>();
            grpMap.put("groupId", group.getId());
            grpMap.put("groupName", group.getTen());
            grpMap.put("groupType", group.getLoai().name());
            grpMap.put("requiredCredits", groupReq);
            grpMap.put("earnedCredits", groupEarned);
            grpMap.put("missingCredits", groupMissing);
            grpMap.put("isFulfilled", groupMissing == 0);
            grpMap.put("missingCourses", groupMissingCourses);
            missingGroups.add(grpMap);
        }

        int totalMissing = Math.max(0, totalRequired - totalEarned);
        int mandatoryMissing = Math.max(0, mandatoryRequired - mandatoryEarned);
        int electiveMissing = Math.max(0, electiveRequired - electiveEarned);

        // 6. Gợi ý lớp mở kỳ tới và phân loại lớp loại trừ
        List<CourseClass> targetSemesterClasses = (classes != null && targetSemester != null) ?
                classes.findByHocKyIdOrderByMaAsc(targetSemester.getId()) : List.of();
        Map<Long, List<CourseClass>> classesBySubject = targetSemesterClasses.stream()
                .collect(Collectors.groupingBy(c -> c.getMonHoc().getId()));

        List<CourseRegistration> currentTermRegs = targetTermId != null ?
                activeRegs.stream().filter(r -> r.getLopHocPhan().getHocKy().getId().equals(targetTermId)).toList() : List.of();
        List<CourseClass> registeredClassesInTerm = currentTermRegs.stream().map(CourseRegistration::getLopHocPhan).toList();
        Set<Long> registeredSubjectIds = registeredClassesInTerm.stream().map(c -> c.getMonHoc().getId()).collect(Collectors.toSet());
        int currentRegCredits = registeredClassesInTerm.stream().mapToInt(c -> c.getMonHoc().getSoTinChi()).sum();
        int maxCredits = targetSemester != null ? targetSemester.getSoTinChiToiDa() : 24;

        List<Map<String, Object>> registrationSuggestions = new ArrayList<>();
        List<Map<String, Object>> excludedClasses = new ArrayList<>();
        List<Map<String, Object>> unofferedMissingCourses = new ArrayList<>();

        List<CourseClass> acceptedSuggestions = new ArrayList<>();
        int plannedCreditsAccumulator = currentRegCredits;
        Set<Long> suggestedSubjectIds = new HashSet<>();

        for (Map<String, Object> missingCourse : allMissingCourses) {
            Long subjectId = ((Number) missingCourse.get("subjectId")).longValue();
            String subjectCode = (String) missingCourse.get("subjectCode");
            String subjectName = (String) missingCourse.get("subjectName");
            int credits = ((Number) missingCourse.get("credits")).intValue();
            String groupType = (String) missingCourse.get("groupType");
            boolean eligible = Boolean.TRUE.equals(missingCourse.get("eligible"));
            String eligibilityReason = (String) missingCourse.get("eligibilityReason");

            List<CourseClass> offeredSections = classesBySubject.getOrDefault(subjectId, List.of());

            if (offeredSections.isEmpty()) {
                // Môn không mở kỳ tới phải được thể hiện riêng, không biến thành gợi ý đăng ký khả thi
                Map<String, Object> unoffered = new LinkedHashMap<>();
                unoffered.put("subjectId", subjectId);
                unoffered.put("subjectCode", subjectCode);
                unoffered.put("subjectName", subjectName);
                unoffered.put("credits", credits);
                unoffered.put("groupType", groupType);
                unoffered.put("groupName", missingCourse.get("groupName"));
                unoffered.put("eligible", eligible);
                unoffered.put("studyStatus", missingCourse.get("studyStatus"));
                unoffered.put("note", "Môn học không có lớp học phần nào mở trong học kỳ " + (targetSemester != null ? targetSemester.getTen() : "được chọn") + ".");
                unofferedMissingCourses.add(unoffered);
                continue;
            }

            for (CourseClass section : offeredSections) {
                // Kiểm tra các điều kiện loại trừ
                if (!eligible) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType, "Chưa đủ điều kiện tiên quyết: " + eligibilityReason));
                    continue;
                }
                if (registeredSubjectIds.contains(subjectId)) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType, "Sinh viên đã có đăng ký lớp của môn học này trong học kỳ " + (targetSemester != null ? targetSemester.getTen() : "")));
                    continue;
                }
                if (section.getConLai() <= 0 || section.getSiSoHienTai() >= section.getSiSoToiDa()) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType, "Lớp học phần đã hết chỗ (" + section.getSiSoHienTai() + "/" + section.getSiSoToiDa() + ")."));
                    continue;
                }
                Optional<CourseClass> conflictRegistered = registeredClassesInTerm.stream().filter(section::trungLichVoi).findFirst();
                if (conflictRegistered.isPresent()) {
                    CourseClass cr = conflictRegistered.get();
                    excludedClasses.add(makeExcludedClassDto(section, groupType,
                            "Trùng lịch học với lớp đã đăng ký " + cr.getMa() + " (" + cr.getMonHoc().getTen() + ") vào Thứ "
                                    + cr.getThu() + ", tiết " + cr.getTietBatDau() + "–" + cr.getTietKetThuc() + "."));
                    continue;
                }
                if (currentRegCredits + credits > maxCredits) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType,
                            "Vượt giới hạn tín chỉ tối đa của học kỳ (" + maxCredits + " TC). Hiện đã đăng ký "
                                    + currentRegCredits + " TC, môn yêu cầu " + credits + " TC."));
                    continue;
                }

                // Kiểm tra trùng lặp hoặc xung đột với các gợi ý đã được chấp nhận trước đó
                if (suggestedSubjectIds.contains(subjectId)) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType,
                            "Lớp học phần cùng môn với lớp gợi ý đã chọn (lựa chọn khung giờ thay thế)."));
                    continue;
                }
                Optional<CourseClass> conflictSuggested = acceptedSuggestions.stream().filter(section::trungLichVoi).findFirst();
                if (conflictSuggested.isPresent()) {
                    CourseClass cs = conflictSuggested.get();
                    excludedClasses.add(makeExcludedClassDto(section, groupType,
                            "Trùng lịch học với lớp gợi ý " + cs.getMa() + " (" + cs.getMonHoc().getTen() + ") vào Thứ "
                                    + cs.getThu() + ", tiết " + cs.getTietBatDau() + "–" + cs.getTietKetThuc() + "."));
                    continue;
                }
                if (plannedCreditsAccumulator + credits > maxCredits) {
                    excludedClasses.add(makeExcludedClassDto(section, groupType,
                            "Nếu thêm vào danh sách gợi ý sẽ vượt trần tín chỉ của kỳ (" + maxCredits + " TC)."));
                    continue;
                }

                // Gợi ý khả thi
                acceptedSuggestions.add(section);
                suggestedSubjectIds.add(subjectId);
                plannedCreditsAccumulator += credits;

                Map<String, Object> sug = new LinkedHashMap<>();
                sug.put("classId", section.getId());
                sug.put("classCode", section.getMa());
                sug.put("subjectId", section.getMonHoc().getId());
                sug.put("subjectCode", section.getMonHoc().getMa());
                sug.put("subjectName", section.getMonHoc().getTen());
                sug.put("credits", section.getMonHoc().getSoTinChi());
                sug.put("groupType", groupType);
                sug.put("dayOfWeek", section.getThu());
                sug.put("startPeriod", section.getTietBatDau());
                sug.put("periods", section.getSoTiet());
                sug.put("room", section.getPhong());
                sug.put("lecturerName", section.getGiangVien() != null ? section.getGiangVien().getHoTen() : "Chưa phân công");
                sug.put("remainingSeats", section.getConLai());
                sug.put("totalSeats", section.getSiSoToiDa());
                sug.put("semesterId", section.getHocKy().getId());
                sug.put("semesterName", section.getHocKy().getTen() + " " + section.getHocKy().getNamHoc());
                sug.put("eligible", true);
                sug.put("reason", "Môn " + ("BAT_BUOC".equals(groupType) ? "bắt buộc" : "tự chọn")
                        + " còn thiếu; đủ điều kiện tiên quyết; lớp đang mở, còn chỗ và không xung đột lịch.");
                registrationSuggestions.add(sug);
            }
        }

        // 7. Cảnh báo nguy cơ chậm tốt nghiệp (có căn cứ cụ thể, nêu rõ là thông tin tham khảo)
        List<Map<String, Object>> graduationDelayWarnings = new ArrayList<>();

        // Căn cứ 1: Môn tiên quyết chuỗi dài chưa hoàn thành
        Map<Long, List<String>> downstreamMap = new HashMap<>();
        for (PrerequisiteSubject rel : systemAllPrereqs) {
            if (rel.laQuanHeTienQuyet() && rel.isYeuCauDatHieuLuc() && rel.isDangHieuLucHieuLuc()) {
                Long preId = rel.getMonTienQuyet().getId();
                Long subId = rel.getMonHoc().getId();
                if (curriculumSubjectIds.contains(subId) && curriculumSubjectIds.contains(preId)) {
                    downstreamMap.computeIfAbsent(preId, k -> new ArrayList<>()).add(rel.getMonHoc().getMa());
                }
            }
        }

        for (Map.Entry<Long, List<String>> entry : downstreamMap.entrySet()) {
            Long unpassedPreId = entry.getKey();
            if (!passedSubjectIds.contains(unpassedPreId)) {
                String preCode = allMissingCourses.stream().filter(c -> c.get("subjectId").equals(unpassedPreId))
                        .map(c -> (String) c.get("subjectCode")).findFirst().orElse("Môn #" + unpassedPreId);
                String preName = allMissingCourses.stream().filter(c -> c.get("subjectId").equals(unpassedPreId))
                        .map(c -> (String) c.get("subjectName")).findFirst().orElse("");
                List<String> downList = entry.getValue();

                Map<String, Object> warn = new LinkedHashMap<>();
                warn.put("type", "PREREQUISITE_CHAIN");
                warn.put("severity", "CANH_BAO");
                warn.put("title", "Nguy cơ trễ chuỗi môn tiên quyết: " + preCode);
                warn.put("description", "Môn " + preCode + (!preName.isBlank() ? " (" + preName + ")" : "")
                        + " là môn tiên quyết cho các môn tiếp theo trong chương trình: " + String.join(", ", downList)
                        + ". Việc chưa hoàn thành môn này có nguy cơ làm tắc nghẽn lộ trình đăng ký ở các kỳ sau.");
                warn.put("basis", "Cấu hình quan hệ môn tiên quyết trong chương trình đào tạo.");
                graduationDelayWarnings.add(warn);
            }
        }

        // Căn cứ 2: Số tín chỉ còn thiếu
        if (totalMissing > 0) {
            Map<String, Object> warnCredit = new LinkedHashMap<>();
            warnCredit.put("type", "MISSING_CREDITS");
            warnCredit.put("severity", totalMissing > 30 ? "NGUY_CO_CAO" : "THONG_TIN");
            warnCredit.put("title", "Khối lượng tín chỉ tích lũy còn thiếu");
            warnCredit.put("description", "Sinh viên hiện còn thiếu " + totalMissing + " tín chỉ (gồm "
                    + mandatoryMissing + " TC bắt buộc và " + electiveMissing + " TC tự chọn) để hoàn thành phiên bản chương trình "
                    + program.getPhienBan() + " (ngành " + program.getNganh().getTen() + ", khóa " + program.getKhoaNhapHoc() + ").");
            warnCredit.put("basis", "Chỉ tiêu tín chỉ quy định trong phiên bản chương trình đào tạo được gán.");
            graduationDelayWarnings.add(warnCredit);
        }

        // Căn cứ 3: Môn bắt buộc còn thiếu nhưng không mở lớp kỳ tới
        List<String> unofferedMandatory = unofferedMissingCourses.stream()
                .filter(m -> "BAT_BUOC".equals(m.get("groupType")))
                .map(m -> (String) m.get("subjectCode"))
                .toList();
        if (!unofferedMandatory.isEmpty()) {
            Map<String, Object> warnUnoffered = new LinkedHashMap<>();
            warnUnoffered.put("type", "UNOFFERED_MANDATORY");
            warnUnoffered.put("severity", "CANH_BAO");
            warnUnoffered.put("title", "Môn bắt buộc chưa mở lớp trong kỳ tới");
            warnUnoffered.put("description", "Có " + unofferedMandatory.size() + " môn bắt buộc còn thiếu ("
                    + String.join(", ", unofferedMandatory) + ") không có lớp học phần nào mở trong học kỳ "
                    + (targetSemester != null ? targetSemester.getTen() : "được chọn") + ". Sinh viên cần theo dõi đợt mở lớp các kỳ sau.");
            warnUnoffered.put("basis", "Kế hoạch mở lớp học phần của học kỳ được chọn so với môn bắt buộc trong chương trình.");
            graduationDelayWarnings.add(warnUnoffered);
        }

        // Đóng gói kết quả trả về
        out.put("configured", true);
        out.put("program", Map.of(
                "id", program.getId(),
                "major", program.getNganh().getTen(),
                "cohort", program.getKhoaNhapHoc(),
                "version", program.getPhienBan()
        ));
        out.put("summary", Map.of(
                "totalRequiredCredits", totalRequired,
                "totalEarnedCredits", totalEarned,
                "totalMissingCredits", totalMissing,
                "mandatoryRequiredCredits", mandatoryRequired,
                "mandatoryEarnedCredits", mandatoryEarned,
                "mandatoryMissingCredits", mandatoryMissing,
                "electiveRequiredCredits", electiveRequired,
                "electiveEarnedCredits", electiveEarned,
                "electiveMissingCredits", electiveMissing,
                "completionPercent", totalRequired == 0 ? 0 : Math.min(100, totalEarned * 100 / totalRequired)
        ));
        out.put("missingGroups", missingGroups);
        out.put("upcomingSemester", targetSemester != null ? Map.of(
                "id", targetSemester.getId(),
                "name", targetSemester.getTen(),
                "academicYear", targetSemester.getNamHoc(),
                "isOpenForRegistration", targetSemester.dangMoDangKy(),
                "maxCredits", maxCredits,
                "registeredCredits", currentRegCredits,
                "remainingCreditsAvailable", Math.max(0, maxCredits - currentRegCredits)
        ) : null);
        out.put("registrationSuggestions", registrationSuggestions);
        out.put("excludedClasses", excludedClasses);
        out.put("unofferedMissingCourses", unofferedMissingCourses);
        out.put("graduationDelayWarnings", graduationDelayWarnings);
        out.put("limitationsAndDisclaimers", List.of(
                "Cảnh báo nguy cơ chậm tốt nghiệp chỉ mang tính chất tham khảo dựa trên tiến độ tín chỉ và chuỗi môn tiên quyết, không khẳng định sinh viên sẽ tốt nghiệp hay không.",
                "Hạn chế dữ liệu: Hệ thống hiện tại chưa cấu hình chu kỳ mở môn cố định (kỳ thu/kỳ xuân) và chưa có quy chế xét tốt nghiệp chính thức (chuẩn đầu ra ngoại ngữ, tin học, rèn luyện); hệ thống không tự suy đoán các tiêu chí ngoài dữ liệu.",
                "Quy chế học lại/cải thiện: Môn chưa đạt (điểm < 5.0) cần đăng ký học lại theo chính sách hiện hành. Hệ thống lưu riêng kết quả từng lần học, không tự quyết quy tắc thay điểm hoặc GPA."
        ));
        return out;
    }

    private Map<String, Object> makeExcludedClassDto(CourseClass section, String groupType, String reason) {
        Map<String, Object> exc = new LinkedHashMap<>();
        exc.put("classId", section.getId());
        exc.put("classCode", section.getMa());
        exc.put("subjectId", section.getMonHoc().getId());
        exc.put("subjectCode", section.getMonHoc().getMa());
        exc.put("subjectName", section.getMonHoc().getTen());
        exc.put("credits", section.getMonHoc().getSoTinChi());
        exc.put("groupType", groupType);
        exc.put("dayOfWeek", section.getThu());
        exc.put("startPeriod", section.getTietBatDau());
        exc.put("periods", section.getSoTiet());
        exc.put("room", section.getPhong());
        exc.put("lecturerName", section.getGiangVien() != null ? section.getGiangVien().getHoTen() : "Chưa phân công");
        exc.put("remainingSeats", section.getConLai());
        exc.put("totalSeats", section.getSiSoToiDa());
        exc.put("reason", reason);
        return exc;
    }
}
