package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.entity.AcademicCohort;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Curriculum;
import com.edu.registration.entity.CurriculumGroup;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SubjectRelationType;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AcademicCohortRepository;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final AdministrativeClassRepository lopHanhChinhRepository;
    private final SubjectRepository monHocRepository;
    private final PrerequisiteSubjectRepository monTienQuyetRepository;
    private final SemesterRepository hocKyRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final AcademicCohortRepository khoaHocRepository;
    private final CurriculumRepository chuongTrinhRepository;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;
    private final AuditLogService auditLogService;

    public CatalogService(FacultyRepository khoaRepository, MajorRepository nganhRepository,
                          AdministrativeClassRepository lopHanhChinhRepository, SubjectRepository monHocRepository,
                          PrerequisiteSubjectRepository monTienQuyetRepository, SemesterRepository hocKyRepository,
                          CourseClassRepository lopHocPhanRepository, DtoMapper mapper,
                          AuditLogService auditLogService) {
        this(khoaRepository, nganhRepository, lopHanhChinhRepository, monHocRepository,
                monTienQuyetRepository, hocKyRepository, lopHocPhanRepository,
                null, null, null, mapper, auditLogService);
    }

    @Autowired
    public CatalogService(FacultyRepository khoaRepository, MajorRepository nganhRepository,
                          AdministrativeClassRepository lopHanhChinhRepository, SubjectRepository monHocRepository,
                          PrerequisiteSubjectRepository monTienQuyetRepository, SemesterRepository hocKyRepository,
                          CourseClassRepository lopHocPhanRepository,
                          AcademicCohortRepository khoaHocRepository,
                          CurriculumRepository chuongTrinhRepository,
                          CurrentUser currentUser,
                          DtoMapper mapper,
                          AuditLogService auditLogService) {
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.monHocRepository = monHocRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.hocKyRepository = hocKyRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.khoaHocRepository = khoaHocRepository;
        this.chuongTrinhRepository = chuongTrinhRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.KhoaDto> danhSachKhoa() {
        return khoaRepository.findAllByOrderByMaAsc().stream().map(mapper::khoa).toList();
    }

    public CatalogDtos.KhoaDto taoKhoa(CatalogDtos.KhoaRequest request) {
        String ma = request.ma().trim();
        if (khoaRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã khoa đã tồn tại");
        }
        Faculty khoa = new Faculty();
        khoa.setMa(ma);
        khoa.setTen(request.ten().trim());
        return mapper.khoa(khoaRepository.save(khoa));
    }

    public CatalogDtos.KhoaDto suaKhoa(Long id, CatalogDtos.KhoaRequest request) {
        Faculty khoa = khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
        String ma = request.ma().trim();
        if (!khoa.getMa().equalsIgnoreCase(ma) && khoaRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã khoa đã tồn tại");
        }
        khoa.setMa(ma);
        khoa.setTen(request.ten().trim());
        return mapper.khoa(khoa);
    }

    public void xoaKhoa(Long id) {
        khoaRepository.delete(khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.NganhDto> danhSachNganh() {
        return nganhRepository.findAllByOrderByMaAsc().stream().map(mapper::nganh).toList();
    }

    public CatalogDtos.NganhDto taoNganh(CatalogDtos.NganhRequest request) {
        String ma = request.ma().trim();
        if (nganhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã ngành đã tồn tại");
        }
        Major nganh = new Major();
        nganh.setMa(ma);
        nganh.setTen(request.ten().trim());
        nganh.setKhoa(layKhoa(request.khoaId()));
        return mapper.nganh(nganhRepository.save(nganh));
    }

    public CatalogDtos.NganhDto suaNganh(Long id, CatalogDtos.NganhRequest request) {
        Major nganh = nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
        String ma = request.ma().trim();
        if (!nganh.getMa().equalsIgnoreCase(ma) && nganhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã ngành đã tồn tại");
        }
        nganh.setMa(ma);
        nganh.setTen(request.ten().trim());
        nganh.setKhoa(layKhoa(request.khoaId()));
        return mapper.nganh(nganh);
    }

    public void xoaNganh(Long id) {
        nganhRepository.delete(nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.LopHanhChinhDto> danhSachLopHanhChinh() {
        return lopHanhChinhRepository.findAllByOrderByMaAsc().stream().map(mapper::lopHanhChinh).toList();
    }

    public CatalogDtos.LopHanhChinhDto taoLopHanhChinh(CatalogDtos.LopHanhChinhRequest request) {
        String ma = request.ma().trim();
        if (lopHanhChinhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp hành chính đã tồn tại");
        }
        AdministrativeClass lop = new AdministrativeClass();
        lop.setMa(ma);
        lop.setTen(request.ten().trim());
        lop.setNganh(layNganh(request.nganhId()));
        return mapper.lopHanhChinh(lopHanhChinhRepository.save(lop));
    }

    public CatalogDtos.LopHanhChinhDto suaLopHanhChinh(Long id, CatalogDtos.LopHanhChinhRequest request) {
        AdministrativeClass lop = lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại"));
        String ma = request.ma().trim();
        if (!lop.getMa().equalsIgnoreCase(ma) && lopHanhChinhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp hành chính đã tồn tại");
        }
        lop.setMa(ma);
        lop.setTen(request.ten().trim());
        lop.setNganh(layNganh(request.nganhId()));
        return mapper.lopHanhChinh(lop);
    }

    public void xoaLopHanhChinh(Long id) {
        lopHanhChinhRepository.delete(lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại")));
    }

    // ==================== KHÓA HỌC / NIÊN KHÓA (ACADEMIC COHORT) ====================

    @Transactional(readOnly = true)
    public List<CatalogDtos.KhoaHocDto> danhSachKhoaHoc(String keyword, Boolean dangHieuLuc, Integer namBatDau) {
        if (khoaHocRepository == null) {
            return List.of();
        }
        String kw = normalizeKeyword(keyword);
        return khoaHocRepository.findAllByOrderByNamBatDauDescMaAsc().stream()
                .filter(c -> dangHieuLuc == null || c.isDangHieuLuc() == dangHieuLuc)
                .filter(c -> namBatDau == null || c.getNamBatDau() == namBatDau)
                .filter(c -> kw == null
                        || containsIgnoreCase(c.getMa(), kw)
                        || containsIgnoreCase(c.getTen(), kw)
                        || containsIgnoreCase(c.getNienKhoa(), kw)
                        || containsIgnoreCase(c.getMoTa(), kw))
                .map(mapper::khoaHoc)
                .toList();
    }

    public CatalogDtos.KhoaHocDto taoKhoaHoc(CatalogDtos.KhoaHocRequest request) {
        yeuCauKhoaHocRepo();
        return auditLogService.execute("COHORT_CREATED", "ACADEMIC_COHORT", null, audit -> {
            kiemTraKhoaHocRequest(request);
            String ma = request.ma().trim();
            if (khoaHocRepository.existsByMaIgnoreCase(ma)) {
                throw ApiException.conflict("Mã khóa học/niên khóa đã tồn tại");
            }
            AcademicCohort cohort = new AcademicCohort();
            apDungKhoaHoc(cohort, request, true);
            AcademicCohort saved = khoaHocRepository.save(cohort);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            return mapper.khoaHoc(saved);
        });
    }

    public CatalogDtos.KhoaHocDto suaKhoaHoc(Long id, CatalogDtos.KhoaHocRequest request) {
        yeuCauKhoaHocRepo();
        return auditLogService.execute("COHORT_UPDATED", "ACADEMIC_COHORT", id, audit -> {
            AcademicCohort cohort = khoaHocRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Khóa học/niên khóa không tồn tại"));
            Map<String, Object> before = auditSnapshot(cohort);
            kiemTraKhoaHocRequest(request);
            String ma = request.ma().trim();
            if (!cohort.getMa().equalsIgnoreCase(ma) && khoaHocRepository.existsByMaIgnoreCase(ma)) {
                throw ApiException.conflict("Mã khóa học/niên khóa đã tồn tại");
            }
            apDungKhoaHoc(cohort, request, false);
            AcademicCohort saved = khoaHocRepository.save(cohort);
            audit.changes(before, auditSnapshot(saved));
            return mapper.khoaHoc(saved);
        });
    }

    public void xoaKhoaHoc(Long id) {
        yeuCauKhoaHocRepo();
        auditLogService.execute("COHORT_DELETED", "ACADEMIC_COHORT", id, audit -> {
            AcademicCohort cohort = khoaHocRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Khóa học/niên khóa không tồn tại"));
            audit.changes(auditSnapshot(cohort), Map.of("trangThai", "DELETED"));
            kiemTraKhoaHocDangDuocSuDung(cohort);
            khoaHocRepository.delete(cohort);
            return null;
        });
    }

    private void yeuCauKhoaHocRepo() {
        if (khoaHocRepository == null) {
            throw ApiException.badRequest("Chưa cấu hình kho dữ liệu khóa học/niên khóa");
        }
    }

    private void kiemTraKhoaHocRequest(CatalogDtos.KhoaHocRequest request) {
        if (request.namBatDau() > request.namKetThuc()) {
            throw ApiException.badRequest("Năm bắt đầu không được lớn hơn năm kết thúc");
        }
        if (request.hieuLucTu() != null && request.hieuLucDen() != null
                && request.hieuLucTu().isAfter(request.hieuLucDen())) {
            throw ApiException.badRequest("Ngày bắt đầu hiệu lực phải trước hoặc bằng ngày kết thúc hiệu lực");
        }
        if (request.hocKyBatDauId() != null && request.hocKyKetThucId() != null) {
            Semester hkBatDau = layHocKy(request.hocKyBatDauId());
            Semester hkKetThuc = layHocKy(request.hocKyKetThucId());
            if (hkBatDau.getNgayBatDau().isAfter(hkKetThuc.getNgayBatDau())) {
                throw ApiException.badRequest("Học kỳ bắt đầu hiệu lực phải trước hoặc cùng học kỳ kết thúc hiệu lực");
            }
        }
    }

    private void apDungKhoaHoc(AcademicCohort cohort, CatalogDtos.KhoaHocRequest request, boolean isNew) {
        cohort.setMa(request.ma().trim());
        cohort.setTen(request.ten().trim());
        cohort.setNienKhoa(request.nienKhoa().trim());
        cohort.setNamBatDau(request.namBatDau());
        cohort.setNamKetThuc(request.namKetThuc());
        cohort.setHocKyBatDau(request.hocKyBatDauId() != null ? layHocKy(request.hocKyBatDauId()) : null);
        cohort.setHocKyKetThuc(request.hocKyKetThucId() != null ? layHocKy(request.hocKyKetThucId()) : null);
        cohort.setHieuLucTu(request.hieuLucTu());
        cohort.setHieuLucDen(request.hieuLucDen());
        cohort.setDangHieuLuc(request.dangHieuLuc() == null || request.dangHieuLuc());
        cohort.setMoTa(normalizeText(request.moTa()));

        LocalDateTime now = LocalDateTime.now();
        User actor = layNguoiThucHien();
        if (isNew) {
            cohort.setTaoLuc(now);
            if (actor != null) {
                cohort.setNguoiTaoId(actor.getId());
                cohort.setNguoiTaoUsername(actor.getUsername());
            }
        }
        cohort.setCapNhatLuc(now);
        if (actor != null) {
            cohort.setNguoiCapNhatId(actor.getId());
            cohort.setNguoiCapNhatUsername(actor.getUsername());
        }
    }

    private void kiemTraKhoaHocDangDuocSuDung(AcademicCohort cohort) {
        List<String> thamChieu = new ArrayList<>();
        if (chuongTrinhRepository != null) {
            List<String> ctList = chuongTrinhRepository.findAll().stream()
                    .filter(ct -> ct.getKhoaNhapHoc() != null
                            && (ct.getKhoaNhapHoc().trim().equalsIgnoreCase(cohort.getMa())
                            || ct.getKhoaNhapHoc().trim().equalsIgnoreCase(cohort.getNienKhoa())))
                    .map(ct -> ct.getNganh().getMa() + "-" + ct.getKhoaNhapHoc() + " (" + ct.getPhienBan() + ")")
                    .toList();
            if (!ctList.isEmpty()) {
                thamChieu.add("chương trình đào tạo: " + String.join(", ", ctList));
            }
        }
        if (monTienQuyetRepository.existsByKhoaNhapHocIgnoreCase(cohort.getMa())
                || monTienQuyetRepository.existsByKhoaNhapHocIgnoreCase(cohort.getNienKhoa())) {
            thamChieu.add("quan hệ môn học áp dụng cho khóa " + cohort.getMa());
        }
        if (!thamChieu.isEmpty()) {
            throw ApiException.conflict("Không thể xóa khóa học/niên khóa " + cohort.getMa()
                    + " vì đang được sử dụng bởi " + String.join("; ", thamChieu));
        }
    }

    private Map<String, Object> auditSnapshot(AcademicCohort cohort) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("maKhoaHoc", cohort.getMa());
        map.put("tenKhoaHoc", cohort.getTen());
        map.put("nienKhoa", cohort.getNienKhoa());
        map.put("namBatDau", cohort.getNamBatDau());
        map.put("namKetThuc", cohort.getNamKetThuc());
        if (cohort.getHocKyBatDau() != null) {
            map.put("hocKyBatDauId", cohort.getHocKyBatDau().getId());
        }
        if (cohort.getHocKyKetThuc() != null) {
            map.put("hocKyKetThucId", cohort.getHocKyKetThuc().getId());
        }
        if (cohort.getHieuLucTu() != null) {
            map.put("hieuLucTu", cohort.getHieuLucTu().toString());
        }
        if (cohort.getHieuLucDen() != null) {
            map.put("hieuLucDen", cohort.getHieuLucDen().toString());
        }
        map.put("dangHieuLuc", cohort.isDangHieuLuc());
        if (cohort.getNguoiTaoUsername() != null) {
            map.put("nguoiTaoUsername", cohort.getNguoiTaoUsername());
        }
        if (cohort.getNguoiCapNhatUsername() != null) {
            map.put("nguoiCapNhatUsername", cohort.getNguoiCapNhatUsername());
        }
        return map;
    }

    // ==================== MÔN HỌC (SUBJECT) ====================

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonHocDto> danhSachMonHoc() {
        return danhSachMonHoc(null, null);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonHocDto> danhSachMonHoc(String keyword, Long khoaId) {
        String kw = normalizeKeyword(keyword);
        return monHocRepository.findAllByOrderByMaAsc().stream()
                .filter(m -> khoaId == null || (m.getKhoa() != null && m.getKhoa().getId().equals(khoaId)))
                .filter(m -> kw == null
                        || containsIgnoreCase(m.getMa(), kw)
                        || containsIgnoreCase(m.getTen(), kw)
                        || (m.getKhoa() != null && containsIgnoreCase(m.getKhoa().getTen(), kw)))
                .map(mapper::monHoc)
                .toList();
    }

    public CatalogDtos.MonHocDto taoMonHoc(CatalogDtos.MonHocRequest request) {
        return auditLogService.execute("COURSE_CREATED", "COURSE", null, audit -> {
            String ma = request.ma().trim();
            if (monHocRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã môn học đã tồn tại");
            }
            Subject monHoc = new Subject();
            monHoc.setMa(ma);
            monHoc.setTen(request.ten().trim());
            monHoc.setSoTinChi(request.soTinChi());
            monHoc.setKhoa(layKhoa(request.khoaId()));
            capNhatMetadataMonHoc(monHoc, true);
            Subject saved = monHocRepository.save(monHoc);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            return mapper.monHoc(saved);
        });
    }

    public CatalogDtos.MonHocDto suaMonHoc(Long id, CatalogDtos.MonHocRequest request) {
        return auditLogService.execute("COURSE_UPDATED", "COURSE", id, audit -> {
            Subject monHoc = monHocRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            Map<String, Object> before = auditSnapshot(monHoc);
            String ma = request.ma().trim();
            if (!monHoc.getMa().equalsIgnoreCase(ma) && monHocRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã môn học đã tồn tại");
            }
            monHoc.setMa(ma);
            monHoc.setTen(request.ten().trim());
            monHoc.setSoTinChi(request.soTinChi());
            monHoc.setKhoa(layKhoa(request.khoaId()));
            capNhatMetadataMonHoc(monHoc, false);
            audit.changes(before, auditSnapshot(monHoc));
            return mapper.monHoc(monHoc);
        });
    }

    public void xoaMonHoc(Long id) {
        auditLogService.execute("COURSE_DELETED", "COURSE", id, audit -> {
            Subject monHoc = monHocRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            audit.changes(auditSnapshot(monHoc), Map.of("trangThai", "DELETED"));
            if (lopHocPhanRepository.countByMonHocId(id) > 0) {
                throw ApiException.conflict("Không thể xóa môn học " + monHoc.getMa()
                        + " vì đang được lớp học phần sử dụng");
            }
            if (chuongTrinhRepository != null) {
                boolean usedInCurriculum = chuongTrinhRepository.findAll().stream()
                        .anyMatch(ct -> ct.getNhom().stream()
                                .anyMatch(nhom -> nhom.getMonHoc().stream().anyMatch(m -> m.getId().equals(id))));
                if (usedInCurriculum) {
                    throw ApiException.conflict("Không thể xóa môn học " + monHoc.getMa()
                            + " vì đang được chương trình đào tạo sử dụng");
                }
            }
            if (monTienQuyetRepository.existsByMonHocId(id) || monTienQuyetRepository.countByMonTienQuyetId(id) > 0) {
                throw ApiException.conflict("Không thể xóa môn học " + monHoc.getMa()
                        + " vì đang có cấu hình quan hệ môn học");
            }
            monHocRepository.delete(monHoc);
            return null;
        });
    }

    private void capNhatMetadataMonHoc(Subject monHoc, boolean isNew) {
        LocalDateTime now = LocalDateTime.now();
        User actor = layNguoiThucHien();
        if (isNew) {
            monHoc.setTaoLuc(now);
            if (actor != null) {
                monHoc.setNguoiTaoId(actor.getId());
                monHoc.setNguoiTaoUsername(actor.getUsername());
            }
        }
        monHoc.setCapNhatLuc(now);
        if (actor != null) {
            monHoc.setNguoiCapNhatId(actor.getId());
            monHoc.setNguoiCapNhatUsername(actor.getUsername());
        }
    }

    private Map<String, Object> auditSnapshot(Subject monHoc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("maMon", monHoc.getMa());
        map.put("tenMon", monHoc.getTen());
        map.put("soTinChi", monHoc.getSoTinChi());
        map.put("khoaId", monHoc.getKhoa().getId());
        if (monHoc.getNguoiTaoUsername() != null) {
            map.put("nguoiTaoUsername", monHoc.getNguoiTaoUsername());
        }
        if (monHoc.getNguoiCapNhatUsername() != null) {
            map.put("nguoiCapNhatUsername", monHoc.getNguoiCapNhatUsername());
        }
        return map;
    }

    // ==================== QUAN HỆ MÔN HỌC (SUBJECT RELATIONS) ====================

    @Transactional(readOnly = true)
    public List<CatalogDtos.QuyCheQuanHeMonDto> danhSachQuyCheQuanHeMon() {
        return Arrays.stream(SubjectRelationType.values())
                .map(type -> new CatalogDtos.QuyCheQuanHeMonDto(
                        type.name(),
                        type.getTenHienThi(),
                        type.isQuyCheDaChot(),
                        type.isQuyCheDaChot() ? "DA_CHOT_AP_DUNG" : "CHO_CHU_DU_AN_CHOT",
                        type.getMoTaQuyCheHienHanh(),
                        type.getPhanCanChuDuAnChot()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonTienQuyetDto> danhSachQuanHeMon(String keyword,
                                                               Long monHocId,
                                                               String loaiQuanHe,
                                                               Long hocKyId,
                                                               String khoaNhapHoc,
                                                               Boolean dangHieuLuc) {
        SubjectRelationType filterType = loaiQuanHe != null && !loaiQuanHe.isBlank()
                ? parseLoaiQuanHe(loaiQuanHe)
                : null;
        Semester filterSemester = hocKyId != null ? layHocKy(hocKyId) : null;
        String kw = normalizeKeyword(keyword);
        String cohortFilter = normalizeKeyword(khoaNhapHoc);

        return monTienQuyetRepository.findAllByOrderByIdAsc().stream()
                .filter(q -> monHocId == null
                        || q.getMonHoc().getId().equals(monHocId)
                        || q.getMonTienQuyet().getId().equals(monHocId))
                .filter(q -> filterType == null || q.getLoaiQuanHeHieuLuc() == filterType)
                .filter(q -> dangHieuLuc == null || q.isDangHieuLucHieuLuc() == dangHieuLuc)
                .filter(q -> cohortFilter == null
                        || (q.getKhoaNhapHoc() != null && containsIgnoreCase(q.getKhoaNhapHoc(), cohortFilter)))
                .filter(q -> filterSemester == null || lienQuanHocKy(q, filterSemester))
                .filter(q -> kw == null
                        || containsIgnoreCase(q.getMonHoc().getMa(), kw)
                        || containsIgnoreCase(q.getMonHoc().getTen(), kw)
                        || containsIgnoreCase(q.getMonTienQuyet().getMa(), kw)
                        || containsIgnoreCase(q.getMonTienQuyet().getTen(), kw)
                        || containsIgnoreCase(q.getLoaiQuanHeHieuLuc().getTenHienThi(), kw)
                        || containsIgnoreCase(q.getMaNhomTuChon(), kw)
                        || containsIgnoreCase(q.getKhoaNhapHoc(), kw)
                        || containsIgnoreCase(q.getGhiChu(), kw))
                .map(mapper::monTienQuyet)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonTienQuyetDto> danhSachTienQuyet(Long monHocId) {
        return danhSachTienQuyet(monHocId, null, null);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonTienQuyetDto> danhSachTienQuyet(Long monHocId, String loaiQuanHe, Boolean dangHieuLuc) {
        layMonHoc(monHocId);
        SubjectRelationType filterType = loaiQuanHe != null && !loaiQuanHe.isBlank()
                ? parseLoaiQuanHe(loaiQuanHe)
                : null;
        return monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHocId).stream()
                .filter(q -> filterType == null || q.getLoaiQuanHeHieuLuc() == filterType)
                .filter(q -> dangHieuLuc == null || q.isDangHieuLucHieuLuc() == dangHieuLuc)
                .map(mapper::monTienQuyet)
                .toList();
    }

    public CatalogDtos.MonTienQuyetDto themTienQuyet(Long monHocId, CatalogDtos.MonTienQuyetRequest request) {
        return auditLogService.execute("PREREQUISITE_ADDED", "COURSE_PREREQUISITE", monHocId, audit ->
                luuMoiQuanHeMon(monHocId, request, audit));
    }

    public CatalogDtos.MonTienQuyetDto taoQuanHeMon(CatalogDtos.QuanHeMonRequest request) {
        if (request.monHocId() == null) {
            throw ApiException.badRequest("Vui lòng chọn môn học chính");
        }
        return auditLogService.execute("PREREQUISITE_ADDED", "COURSE_PREREQUISITE", request.monHocId(), audit ->
                luuMoiQuanHeMon(request.monHocId(), request.toMonTienQuyetRequest(), audit));
    }

    public CatalogDtos.MonTienQuyetDto suaTienQuyet(Long monHocId, Long lienKetId,
                                                    CatalogDtos.MonTienQuyetRequest request) {
        layMonHoc(monHocId);
        PrerequisiteSubject existing = monTienQuyetRepository.findWithDetailsById(lienKetId)
                .orElseThrow(() -> ApiException.notFound("Quan hệ môn học không tồn tại"));
        if (!existing.getMonHoc().getId().equals(monHocId)) {
            throw ApiException.badRequest("Liên kết không thuộc môn học này");
        }
        CatalogDtos.QuanHeMonRequest updateReq = new CatalogDtos.QuanHeMonRequest(
                monHocId,
                request.resolvedMonLienQuanId(),
                request.resolvedMonLienQuanId(),
                request.loaiQuanHe(),
                request.dieuKienDat(),
                request.yeuCauDat(),
                request.maNhomTuChon(),
                request.tinChiTuChonYeuCau(),
                request.khoaNhapHoc(),
                request.hocKyHieuLucTuId(),
                request.hocKyHieuLucDenId(),
                request.hieuLucTu(),
                request.hieuLucDen(),
                request.dangHieuLuc(),
                request.ghiChu()
        );
        return suaQuanHeMon(lienKetId, updateReq);
    }

    public CatalogDtos.MonTienQuyetDto suaQuanHeMon(Long lienKetId, CatalogDtos.QuanHeMonRequest request) {
        return auditLogService.execute("SUBJECT_RELATION_UPDATED", "COURSE_PREREQUISITE", lienKetId, audit -> {
            PrerequisiteSubject lienKet = monTienQuyetRepository.findWithDetailsById(lienKetId)
                    .orElseThrow(() -> ApiException.notFound("Quan hệ môn học không tồn tại"));
            Map<String, Object> before = auditSnapshot(lienKet);

            Long monHocId = request.monHocId() != null ? request.monHocId() : lienKet.getMonHoc().getId();
            Long monLienQuanId = request.resolvedMonLienQuanId() != null
                    ? request.resolvedMonLienQuanId()
                    : lienKet.getMonTienQuyet().getId();

            Subject monHoc = layMonHoc(monHocId);
            Subject monLienQuan = layMonHoc(monLienQuanId);
            SubjectRelationType loai = request.loaiQuanHe() != null && !request.loaiQuanHe().isBlank()
                    ? parseLoaiQuanHe(request.loaiQuanHe())
                    : lienKet.getLoaiQuanHeHieuLuc();

            kiemTraHopLeQuanHeMon(monHoc, monLienQuan, loai, request.toMonTienQuyetRequest(), lienKetId);

            lienKet.setMonHoc(monHoc);
            lienKet.setMonTienQuyet(monLienQuan);
            lienKet.setLoaiQuanHe(loai);
            apDungChiTietQuanHeMon(lienKet, loai, request.toMonTienQuyetRequest(), false);

            PrerequisiteSubject saved = monTienQuyetRepository.save(lienKet);
            audit.changes(before, auditSnapshot(saved));
            return mapper.monTienQuyet(saved);
        });
    }

    public void xoaTienQuyet(Long monHocId, Long lienKetId) {
        auditLogService.execute("PREREQUISITE_REMOVED", "COURSE_PREREQUISITE", lienKetId, audit -> {
            PrerequisiteSubject lienKet = monTienQuyetRepository.findWithDetailsById(lienKetId)
                    .orElseGet(() -> monTienQuyetRepository.findById(lienKetId)
                            .orElseThrow(() -> ApiException.notFound("Liên kết môn tiên quyết không tồn tại")));
            if (!lienKet.getMonHoc().getId().equals(monHocId)) {
                throw ApiException.badRequest("Liên kết không thuộc môn học này");
            }
            audit.changes(auditSnapshot(lienKet), Map.of("trangThai", "DELETED"));
            kiemTraQuanHeDangDuocSuDung(lienKet);
            monTienQuyetRepository.delete(lienKet);
            return null;
        });
    }

    public void xoaQuanHeMon(Long lienKetId) {
        auditLogService.execute("PREREQUISITE_REMOVED", "COURSE_PREREQUISITE", lienKetId, audit -> {
            PrerequisiteSubject lienKet = monTienQuyetRepository.findWithDetailsById(lienKetId)
                    .orElseGet(() -> monTienQuyetRepository.findById(lienKetId)
                            .orElseThrow(() -> ApiException.notFound("Quan hệ môn học không tồn tại")));
            audit.changes(auditSnapshot(lienKet), Map.of("trangThai", "DELETED"));
            kiemTraQuanHeDangDuocSuDung(lienKet);
            monTienQuyetRepository.delete(lienKet);
            return null;
        });
    }

    private CatalogDtos.MonTienQuyetDto luuMoiQuanHeMon(Long monHocId,
                                                        CatalogDtos.MonTienQuyetRequest request,
                                                        AuditLogService.AuditContext audit) {
        if (request == null || request.resolvedMonLienQuanId() == null) {
            throw ApiException.badRequest("Vui lòng chọn môn học liên quan");
        }
        Subject monHoc = layMonHoc(monHocId);
        Subject monLienQuan = layMonHoc(request.resolvedMonLienQuanId());
        SubjectRelationType loai = parseLoaiQuanHe(request.loaiQuanHe());

        kiemTraHopLeQuanHeMon(monHoc, monLienQuan, loai, request, null);

        PrerequisiteSubject lienKet = new PrerequisiteSubject(monHoc, monLienQuan, loai);
        apDungChiTietQuanHeMon(lienKet, loai, request, true);

        PrerequisiteSubject saved = monTienQuyetRepository.save(lienKet);
        audit.entityId(saved.getId());
        audit.changes(Map.of(), auditSnapshot(saved));
        return mapper.monTienQuyet(saved);
    }

    private void kiemTraHopLeQuanHeMon(Subject monHoc,
                                       Subject monLienQuan,
                                       SubjectRelationType loai,
                                       CatalogDtos.MonTienQuyetRequest request,
                                       Long boQuaId) {
        if (monHoc.getId().equals(monLienQuan.getId())) {
            throw ApiException.badRequest("Môn học không thể là tiên quyết của chính nó (không được tự tham chiếu)");
        }

        boolean daTonTai = monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHoc.getId()).stream()
                .anyMatch(q -> (boQuaId == null || !q.getId().equals(boQuaId))
                        && q.getMonTienQuyet().getId().equals(monLienQuan.getId())
                        && q.getLoaiQuanHeHieuLuc() == loai);
        if (daTonTai || (boQuaId == null && loai == SubjectRelationType.TIEN_QUYET
                && monTienQuyetRepository.existsByMonHocIdAndMonTienQuyetId(monHoc.getId(), monLienQuan.getId())
                && monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHoc.getId()).isEmpty())) {
            if (loai == SubjectRelationType.TIEN_QUYET) {
                throw ApiException.conflict("Môn tiên quyết đã tồn tại");
            }
            throw ApiException.conflict("Quan hệ " + loai.getTenHienThi().toLowerCase(Locale.ROOT)
                    + " giữa hai môn học này đã tồn tại");
        }

        if (loai == SubjectRelationType.TIEN_QUYET) {
            kiemTraChuTrinhTienQuyet(monHoc, monLienQuan, boQuaId);
        }

        if (request.dieuKienDat() != null && (request.dieuKienDat() < 0.0 || request.dieuKienDat() > 10.0)) {
            throw ApiException.badRequest("Điều kiện điểm đạt phải từ 0.0 đến 10.0");
        }

        if (request.tinChiTuChonYeuCau() != null && request.tinChiTuChonYeuCau() < 1) {
            throw ApiException.badRequest("Số tín chỉ tự chọn yêu cầu phải lớn hơn hoặc bằng 1");
        }

        if (request.hieuLucTu() != null && request.hieuLucDen() != null
                && request.hieuLucTu().isAfter(request.hieuLucDen())) {
            throw ApiException.badRequest("Ngày bắt đầu hiệu lực phải trước hoặc bằng ngày kết thúc hiệu lực");
        }

        if (request.hocKyHieuLucTuId() != null && request.hocKyHieuLucDenId() != null) {
            Semester hkTu = layHocKy(request.hocKyHieuLucTuId());
            Semester hkDen = layHocKy(request.hocKyHieuLucDenId());
            if (hkTu.getNgayBatDau().isAfter(hkDen.getNgayBatDau())) {
                throw ApiException.badRequest("Học kỳ bắt đầu hiệu lực phải trước hoặc cùng học kỳ kết thúc hiệu lực");
            }
        }
    }

    /**
     * Kiểm tra không cho tạo chu trình (2 đỉnh hoặc nhiều đỉnh A -> B -> C -> A) trong đồ thị môn tiên quyết.
     */
    private void kiemTraChuTrinhTienQuyet(Subject monHoc, Subject monTienQuyet, Long boQuaId) {
        if (boQuaId == null && monTienQuyetRepository.existsByMonHocIdAndMonTienQuyetId(monTienQuyet.getId(), monHoc.getId())) {
            List<PrerequisiteSubject> reverseDirect = monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monTienQuyet.getId());
            boolean reverseIsPrereq = reverseDirect.isEmpty() || reverseDirect.stream()
                    .anyMatch(q -> q.getMonTienQuyet().getId().equals(monHoc.getId()) && q.laQuanHeTienQuyet());
            if (reverseIsPrereq) {
                throw ApiException.badRequest("Tạo thành vòng lặp tiên quyết giữa hai môn học ("
                        + monHoc.getMa() + " -> " + monTienQuyet.getMa() + " -> " + monHoc.getMa() + ")");
            }
        }

        List<PrerequisiteSubject> allRelations = monTienQuyetRepository.findAllByOrderByIdAsc();
        if (allRelations.isEmpty()) {
            return;
        }

        Map<Long, List<Subject>> prereqGraph = new HashMap<>();
        Map<Long, String> maMonById = new HashMap<>();
        maMonById.put(monHoc.getId(), monHoc.getMa());
        maMonById.put(monTienQuyet.getId(), monTienQuyet.getMa());

        for (PrerequisiteSubject rel : allRelations) {
            if (boQuaId != null && boQuaId.equals(rel.getId())) {
                continue;
            }
            if (!rel.laQuanHeTienQuyet()) {
                continue;
            }
            Subject from = rel.getMonHoc();
            Subject to = rel.getMonTienQuyet();
            maMonById.put(from.getId(), from.getMa());
            maMonById.put(to.getId(), to.getMa());
            prereqGraph.computeIfAbsent(from.getId(), k -> new ArrayList<>()).add(to);
        }

        // BFS từ monTienQuyet: nếu đi tới được monHoc thì cạnh monHoc -> monTienQuyet sẽ tạo chu trình
        Queue<Long> queue = new ArrayDeque<>();
        Map<Long, Long> parent = new HashMap<>();
        queue.add(monTienQuyet.getId());
        parent.put(monTienQuyet.getId(), null);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(monHoc.getId())) {
                List<String> cycleCodes = new ArrayList<>();
                Long step = monHoc.getId();
                while (step != null) {
                    cycleCodes.add(maMonById.getOrDefault(step, "#" + step));
                    step = parent.get(step);
                }
                Collections.reverse(cycleCodes);
                cycleCodes.add( 0, monHoc.getMa());
                throw ApiException.badRequest("Tạo thành vòng lặp tiên quyết giữa hai môn học (chu trình: "
                        + String.join(" -> ", cycleCodes) + ")");
            }
            for (Subject next : prereqGraph.getOrDefault(current, List.of())) {
                if (!parent.containsKey(next.getId())) {
                    parent.put(next.getId(), current);
                    queue.add(next.getId());
                }
            }
        }
    }

    private void apDungChiTietQuanHeMon(PrerequisiteSubject lienKet,
                                        SubjectRelationType loai,
                                        CatalogDtos.MonTienQuyetRequest request,
                                        boolean isNew) {
        boolean yeuCauDat = request.yeuCauDat() != null
                ? request.yeuCauDat()
                : (loai == SubjectRelationType.TIEN_QUYET
                || loai == SubjectRelationType.TUONG_DUONG
                || loai == SubjectRelationType.THAY_THE);
        lienKet.setYeuCauDat(yeuCauDat);

        if (request.dieuKienDat() != null) {
            lienKet.setDieuKienDat(request.dieuKienDat());
        } else if (yeuCauDat) {
            lienKet.setDieuKienDat(AcademicRules.DIEM_DAT);
        } else {
            lienKet.setDieuKienDat(null);
        }

        lienKet.setMaNhomTuChon(normalizeText(request.maNhomTuChon()));
        lienKet.setTinChiTuChonYeuCau(request.tinChiTuChonYeuCau());
        lienKet.setKhoaNhapHoc(normalizeText(request.khoaNhapHoc()));
        lienKet.setHocKyHieuLucTu(request.hocKyHieuLucTuId() != null ? layHocKy(request.hocKyHieuLucTuId()) : null);
        lienKet.setHocKyHieuLucDen(request.hocKyHieuLucDenId() != null ? layHocKy(request.hocKyHieuLucDenId()) : null);
        lienKet.setHieuLucTu(request.hieuLucTu());
        lienKet.setHieuLucDen(request.hieuLucDen());
        lienKet.setDangHieuLuc(request.dangHieuLuc() == null || request.dangHieuLuc());
        lienKet.setGhiChu(normalizeText(request.ghiChu()));

        LocalDateTime now = LocalDateTime.now();
        User actor = layNguoiThucHien();
        if (isNew) {
            lienKet.setTaoLuc(now);
            if (actor != null) {
                lienKet.setNguoiTaoId(actor.getId());
                lienKet.setNguoiTaoUsername(actor.getUsername());
            }
        }
        lienKet.setCapNhatLuc(now);
        if (actor != null) {
            lienKet.setNguoiCapNhatId(actor.getId());
            lienKet.setNguoiCapNhatUsername(actor.getUsername());
        }
    }

    /**
     * Chặn xóa quan hệ môn học đang được chương trình đào tạo hoặc lớp học phần sử dụng.
     */
    private void kiemTraQuanHeDangDuocSuDung(PrerequisiteSubject lienKet) {
        Subject monHoc = lienKet.getMonHoc();
        Subject monLienQuan = lienKet.getMonTienQuyet();
        List<String> lyDoChan = new ArrayList<>();

        if (chuongTrinhRepository != null) {
            List<String> chuongTrinhSuDung = new ArrayList<>();
            for (Curriculum ct : chuongTrinhRepository.findAll()) {
                if (lienKet.getKhoaNhapHoc() != null && !lienKet.getKhoaNhapHoc().isBlank()
                        && ct.getKhoaNhapHoc() != null
                        && !ct.getKhoaNhapHoc().trim().equalsIgnoreCase(lienKet.getKhoaNhapHoc().trim())) {
                    continue;
                }
                Set<Long> subjectIds = ct.getNhom().stream()
                        .map(CurriculumGroup::getMonHoc)
                        .flatMap(Set::stream)
                        .map(Subject::getId)
                        .collect(Collectors.toSet());
                if (subjectIds.contains(monHoc.getId()) || subjectIds.contains(monLienQuan.getId())) {
                    chuongTrinhSuDung.add(ct.getNganh().getMa() + " - Khóa " + ct.getKhoaNhapHoc() + " (" + ct.getPhienBan() + ")");
                }
            }
            if (!chuongTrinhSuDung.isEmpty()) {
                lyDoChan.add("chương trình đào tạo (" + String.join(", ", chuongTrinhSuDung) + ")");
            }
        }

        List<CourseClass> danhSachLop = lopHocPhanRepository.findByMonHocIdOrderByMaAsc(monHoc.getId());
        List<String> lopSuDung = danhSachLop.stream()
                .filter(lop -> lienKet.laHieuLucCho(
                        lop.getHocKy(),
                        lienKet.getKhoaNhapHoc(),
                        lop.getHocKy() != null ? lop.getHocKy().getNgayBatDau() : null))
                .map(CourseClass::getMa)
                .limit(5)
                .toList();
        if (!lopSuDung.isEmpty()) {
            lyDoChan.add("lớp học phần (" + String.join(", ", lopSuDung) + ")");
        }

        if (!lyDoChan.isEmpty()) {
            throw ApiException.conflict("Không thể xóa quan hệ môn học " + monHoc.getMa() + " - " + monLienQuan.getMa()
                    + " vì đang được sử dụng bởi " + String.join(" và ", lyDoChan));
        }
    }

    private boolean lienQuanHocKy(PrerequisiteSubject q, Semester hocKy) {
        if (q.getHocKyHieuLucTu() != null && q.getHocKyHieuLucTu().getId().equals(hocKy.getId())) {
            return true;
        }
        if (q.getHocKyHieuLucDen() != null && q.getHocKyHieuLucDen().getId().equals(hocKy.getId())) {
            return true;
        }
        return q.laHieuLucCho(hocKy, null, hocKy.getNgayBatDau());
    }

    private SubjectRelationType parseLoaiQuanHe(String loaiQuanHe) {
        if (loaiQuanHe == null || loaiQuanHe.isBlank()) {
            return SubjectRelationType.TIEN_QUYET;
        }
        try {
            return SubjectRelationType.valueOf(loaiQuanHe.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Loại quan hệ môn học không hợp lệ: " + loaiQuanHe);
        }
    }

    private Map<String, Object> auditSnapshot(PrerequisiteSubject lienKet) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("monHocId", lienKet.getMonHoc().getId());
        map.put("maMon", lienKet.getMonHoc().getMa());
        map.put("monTienQuyetId", lienKet.getMonTienQuyet().getId());
        map.put("monLienQuanId", lienKet.getMonTienQuyet().getId());
        map.put("maMonLienQuan", lienKet.getMonTienQuyet().getMa());
        map.put("loaiQuanHe", lienKet.getLoaiQuanHeHieuLuc().name());
        if (lienKet.getDieuKienDat() != null) {
            map.put("dieuKienDat", lienKet.getDieuKienDat());
        }
        map.put("yeuCauDat", lienKet.isYeuCauDatHieuLuc());
        if (lienKet.getMaNhomTuChon() != null) {
            map.put("maNhomTuChon", lienKet.getMaNhomTuChon());
        }
        if (lienKet.getTinChiTuChonYeuCau() != null) {
            map.put("tinChiTuChonYeuCau", lienKet.getTinChiTuChonYeuCau());
        }
        if (lienKet.getKhoaNhapHoc() != null) {
            map.put("khoaNhapHoc", lienKet.getKhoaNhapHoc());
        }
        if (lienKet.getHocKyHieuLucTu() != null) {
            map.put("hocKyHieuLucTuId", lienKet.getHocKyHieuLucTu().getId());
        }
        if (lienKet.getHocKyHieuLucDen() != null) {
            map.put("hocKyHieuLucDenId", lienKet.getHocKyHieuLucDen().getId());
        }
        if (lienKet.getHieuLucTu() != null) {
            map.put("hieuLucTu", lienKet.getHieuLucTu().toString());
        }
        if (lienKet.getHieuLucDen() != null) {
            map.put("hieuLucDen", lienKet.getHieuLucDen().toString());
        }
        map.put("dangHieuLuc", lienKet.isDangHieuLucHieuLuc());
        if (lienKet.getNguoiTaoUsername() != null) {
            map.put("nguoiTaoUsername", lienKet.getNguoiTaoUsername());
        }
        if (lienKet.getNguoiCapNhatUsername() != null) {
            map.put("nguoiCapNhatUsername", lienKet.getNguoiCapNhatUsername());
        }
        return map;
    }

    // ==================== HỌC KỲ (SEMESTER) ====================

    @Transactional(readOnly = true)
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyRepository.findAllByOrderByNgayBatDauDesc().stream().map(mapper::hocKy).toList();
    }

    public CatalogDtos.HocKyDto taoHocKy(CatalogDtos.HocKyRequest request) {
        return auditLogService.execute("REGISTRATION_WINDOW_CREATED", "SEMESTER", null, audit -> {
            kiemTraHocKy(request);
            if (hocKyRepository.existsByTenAndNamHoc(request.ten().trim(), request.namHoc().trim())) {
                throw ApiException.conflict("Học kỳ đã tồn tại trong năm học này");
            }
            Semester hocKy = new Semester();
            apDungHocKy(hocKy, request);
            Semester saved = hocKyRepository.save(hocKy);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            return mapper.hocKy(saved);
        });
    }

    public CatalogDtos.HocKyDto suaHocKy(Long id, CatalogDtos.HocKyRequest request) {
        return auditLogService.execute("REGISTRATION_WINDOW_UPDATED", "SEMESTER", id, audit -> {
            Semester hocKy = hocKyRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
            Map<String, Object> before = auditSnapshot(hocKy);
            kiemTraHocKy(request);
            apDungHocKy(hocKy, request);
            audit.changes(before, auditSnapshot(hocKy));
            return mapper.hocKy(hocKy);
        });
    }

    public void xoaHocKy(Long id) {
        auditLogService.execute("REGISTRATION_WINDOW_DELETED", "SEMESTER", id, audit -> {
            Semester hocKy = hocKyRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
            audit.changes(auditSnapshot(hocKy), Map.of("trangThai", "DELETED"));
            if (lopHocPhanRepository.countByHocKyId(id) > 0) {
                throw ApiException.conflict("Học kỳ đã có lớp học phần, không thể xóa");
            }
            if (monTienQuyetRepository.countByHocKyHieuLucTuIdOrHocKyHieuLucDenId(id, id) > 0) {
                throw ApiException.conflict("Học kỳ đang được cấu hình làm đợt hiệu lực của quan hệ môn học, không thể xóa");
            }
            hocKyRepository.delete(hocKy);
            return null;
        });
    }

    private Map<String, Object> auditSnapshot(Semester hocKy) {
        return Map.of(
                "tenHocKy", hocKy.getTen(),
                "namHoc", hocKy.getNamHoc(),
                "dangKyTu", hocKy.getDangKyTu().toString(),
                "dangKyDen", hocKy.getDangKyDen().toString(),
                "soTinChiToiThieu", hocKy.getSoTinChiToiThieu(),
                "soTinChiToiDa", hocKy.getSoTinChiToiDa());
    }

    private void kiemTraHocKy(CatalogDtos.HocKyRequest request) {
        if (request.ngayBatDau().isAfter(request.ngayKetThuc())) {
            throw ApiException.badRequest("Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.dangKyTu().isAfter(request.dangKyDen())) {
            throw ApiException.badRequest("Ngày mở đăng ký phải trước ngày đóng đăng ký");
        }
        if (request.soTinChiToiThieu() > request.soTinChiToiDa()) {
            throw ApiException.badRequest("Số tín chỉ tối thiểu không được lớn hơn số tín chỉ tối đa");
        }
    }

    private void apDungHocKy(Semester hocKy, CatalogDtos.HocKyRequest request) {
        hocKy.setTen(request.ten().trim());
        hocKy.setNamHoc(request.namHoc().trim());
        hocKy.setNgayBatDau(request.ngayBatDau());
        hocKy.setNgayKetThuc(request.ngayKetThuc());
        hocKy.setDangKyTu(request.dangKyTu());
        hocKy.setDangKyDen(request.dangKyDen());
        hocKy.setSoTinChiToiThieu(request.soTinChiToiThieu());
        hocKy.setSoTinChiToiDa(request.soTinChiToiDa());
    }

    private Faculty layKhoa(Long id) {
        return khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
    }

    private Major layNganh(Long id) {
        return nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
    }

    private Subject layMonHoc(Long id) {
        return monHocRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
    }

    private Semester layHocKy(Long id) {
        return hocKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
    }

    private User layNguoiThucHien() {
        if (currentUser == null) {
            return null;
        }
        try {
            return currentUser.get();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String source, String lowerKeyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }
}
