package com.edu.registration.service;

import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.ClassroomRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ClassroomService {

    private static final Map<String, String> LOAI_PHONG_LABELS = Map.of(
            "LY_THUYET", "Phòng học lý thuyết",
            "THUC_HANH_MAY_TINH", "Phòng thực hành máy tính",
            "HOI_TRUONG", "Hội trường / Giảng đường lớn",
            "THI_NGHIEM", "Phòng thí nghiệm / Lab",
            "DA_NANG", "Phòng học đa năng"
    );

    private final ClassroomRepository classroomRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    public ClassroomService(ClassroomRepository classroomRepository,
                            CourseClassRepository lopHocPhanRepository,
                            AuditLogService auditLogService,
                            CurrentUser currentUser) {
        this.classroomRepository = classroomRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ClassroomDtos.PhongHocDto> danhSach(String keyword, String loaiPhong,
                                                   ClassroomStatus trangThai,
                                                   Integer sucChuaMin, Integer sucChuaMax,
                                                   String toaNha) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String lp = (loaiPhong != null && !loaiPhong.isBlank()) ? loaiPhong.trim() : null;
        String tn = (toaNha != null && !toaNha.isBlank()) ? toaNha.trim() : null;

        List<Classroom> list = classroomRepository.timKiemPhong(kw, lp, trangThai, sucChuaMin, sucChuaMax, tn);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClassroomDtos.PhongHocDto> danhSachHoatDong() {
        return classroomRepository.findByTrangThaiOrderByMaAsc(ClassroomStatus.HOAT_DONG)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClassroomDtos.PhongHocDto chiTiet(Long id) {
        Classroom c = classroomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Phòng học không tồn tại"));
        return toDto(c);
    }

    public ClassroomDtos.PhongHocDto tao(ClassroomDtos.PhongHocRequest request) {
        String maChuanHoa = chuanHoaMaPhong(request.ma());
        if (classroomRepository.existsByMaIgnoreCase(maChuanHoa)) {
            throw ApiException.conflict("Mã phòng học '" + maChuanHoa + "' đã tồn tại");
        }
        if (request.sucChua() == null || request.sucChua() <= 0) {
            throw ApiException.badRequest("Sức chứa phòng học phải là số nguyên dương lớn hơn 0");
        }

        return auditLogService.execute("CLASSROOM_CREATED", "CLASSROOM", null, audit -> {
            LocalDateTime now = LocalDateTime.now();
            User actor = currentUser.get();

            Classroom c = new Classroom();
            c.setMa(maChuanHoa);
            c.setTen(request.ten().trim());
            c.setToaNha(chuanHoaChuoi(request.toaNha()));
            c.setTang(request.tang());
            c.setViTri(chuanHoaChuoi(request.viTri()));
            c.setSucChua(request.sucChua());
            c.setLoaiPhong(chuanHoaLoaiPhong(request.loaiPhong()));
            c.setThietBi(chuanHoaChuoi(request.thietBi()));
            c.setTrangThai(request.trangThai() != null ? request.trangThai() : ClassroomStatus.HOAT_DONG);
            c.setGhiChu(chuanHoaChuoi(request.ghiChu()));
            c.setNgayTao(now);
            c.setNgayCapNhat(now);
            if (actor != null) {
                c.setNguoiCapNhatUsername(actor.getUsername());
            }

            Classroom saved = classroomRepository.save(c);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            return toDto(saved);
        });
    }

    public ClassroomDtos.PhongHocDto sua(Long id, ClassroomDtos.PhongHocRequest request) {
        Classroom c = classroomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Phòng học không tồn tại"));

        String maChuanHoa = chuanHoaMaPhong(request.ma());
        if (classroomRepository.existsByMaIgnoreCaseAndIdNot(maChuanHoa, id)) {
            throw ApiException.conflict("Mã phòng học '" + maChuanHoa + "' đã tồn tại ở phòng khác");
        }
        if (request.sucChua() == null || request.sucChua() <= 0) {
            throw ApiException.badRequest("Sức chứa phòng học phải là số nguyên dương lớn hơn 0");
        }

        return auditLogService.execute("CLASSROOM_UPDATED", "CLASSROOM", id, audit -> {
            Map<String, Object> before = auditSnapshot(c);
            LocalDateTime now = LocalDateTime.now();
            User actor = currentUser.get();

            c.setMa(maChuanHoa);
            c.setTen(request.ten().trim());
            c.setToaNha(chuanHoaChuoi(request.toaNha()));
            c.setTang(request.tang());
            c.setViTri(chuanHoaChuoi(request.viTri()));
            c.setSucChua(request.sucChua());
            c.setLoaiPhong(chuanHoaLoaiPhong(request.loaiPhong()));
            c.setThietBi(chuanHoaChuoi(request.thietBi()));
            if (request.trangThai() != null) {
                c.setTrangThai(request.trangThai());
            }
            c.setGhiChu(chuanHoaChuoi(request.ghiChu()));
            c.setNgayCapNhat(now);
            if (actor != null) {
                c.setNguoiCapNhatUsername(actor.getUsername());
            }

            Classroom saved = classroomRepository.save(c);
            Map<String, Object> after = auditSnapshot(saved);
            audit.changes(before, after);
            return toDto(saved);
        });
    }

    public ClassroomDtos.PhongHocDto doiTrangThai(Long id, ClassroomStatus trangThaiMoi, String ghiChu) {
        if (trangThaiMoi == null) {
            throw ApiException.badRequest("Trạng thái mới không được để trống");
        }
        Classroom c = classroomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Phòng học không tồn tại"));

        return auditLogService.execute("CLASSROOM_STATUS_CHANGED", "CLASSROOM", id, audit -> {
            Map<String, Object> before = auditSnapshot(c);
            c.setTrangThai(trangThaiMoi);
            if (ghiChu != null && !ghiChu.isBlank()) {
                c.setGhiChu(ghiChu.trim());
            }
            c.setNgayCapNhat(LocalDateTime.now());
            User actor = currentUser.get();
            if (actor != null) {
                c.setNguoiCapNhatUsername(actor.getUsername());
            }

            Classroom saved = classroomRepository.save(c);
            Map<String, Object> after = auditSnapshot(saved);
            audit.changes(before, after);
            return toDto(saved);
        });
    }

    public void xoa(Long id) {
        Classroom c = classroomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Phòng học không tồn tại"));

        // Kiểm tra ràng buộc với lớp học phần / lịch học hiện có
        long soLopTheoId = lopHocPhanRepository.countByPhongHocId(id);
        long soLopTheoMa = lopHocPhanRepository.countByPhongIgnoreCase(c.getMa());
        long tongLop = Math.max(soLopTheoId, soLopTheoMa);

        if (tongLop > 0) {
            throw ApiException.badRequest("Phòng học " + c.getMa()
                    + " đã được xếp lịch cho " + tongLop
                    + " lớp học phần, không thể xóa cứng. Vui lòng chuyển trạng thái phòng sang 'Ngừng sử dụng'.");
        }

        auditLogService.execute("CLASSROOM_DELETED", "CLASSROOM", id, audit -> {
            audit.changes(auditSnapshot(c), Map.of("trangThai", "DELETED"));
            classroomRepository.delete(c);
            return null;
        });
    }

    @Transactional(readOnly = true)
    public ClassroomDtos.TaxonomyPhongHocDto layTaxonomy() {
        List<ClassroomDtos.TaxonomyItemDto> loaiPhong = List.of(
                new ClassroomDtos.TaxonomyItemDto("LY_THUYET", "Phòng học lý thuyết", "Bàn ghế tiêu chuẩn, bảng viết, micro, máy chiếu"),
                new ClassroomDtos.TaxonomyItemDto("THUC_HANH_MAY_TINH", "Phòng thực hành máy tính", "Trang bị dàn máy tính PC nối mạng LAN/Internet"),
                new ClassroomDtos.TaxonomyItemDto("HOI_TRUONG", "Hội trường / Giảng đường lớn", "Sức chứa lớn trên 80 chỗ, âm thanh hội thảo"),
                new ClassroomDtos.TaxonomyItemDto("THI_NGHIEM", "Phòng thí nghiệm / Lab", "Thiết bị chuyên dụng môn khoa học kỹ thuật"),
                new ClassroomDtos.TaxonomyItemDto("DA_NANG", "Phòng học đa năng", "Bàn ghế di động, màn hình tương tác thông minh")
        );

        List<ClassroomDtos.TaxonomyItemDto> thietBiPhoBien = List.of(
                new ClassroomDtos.TaxonomyItemDto("MAY_CHIEU", "Máy chiếu / Màn chiếu", "Trình chiếu slide bài giảng"),
                new ClassroomDtos.TaxonomyItemDto("DIEU_HOA", "Điều hòa nhiệt độ", "Làm mát không gian phòng học"),
                new ClassroomDtos.TaxonomyItemDto("MICRO_AM_THANH", "Micro & Hệ thống âm thanh", "Khuếch đại giọng giảng viên"),
                new ClassroomDtos.TaxonomyItemDto("DAN_MAY_TINH", "Dàn máy tính (PC)", "Dành cho phòng lab/thực hành"),
                new ClassroomDtos.TaxonomyItemDto("BANG_THONG_MINH", "Bảng tương tác thông minh", "Màn hình cảm ứng giảng dạy"),
                new ClassroomDtos.TaxonomyItemDto("CAMERA_HYBRID", "Camera & Thiết bị Hybrid", "Hỗ trợ học trực tuyến kết hợp"),
                new ClassroomDtos.TaxonomyItemDto("WIFI_CHUYEN_DUNG", "Wi-Fi chuyên dụng", "Bộ phát Wi-Fi chịu tải cao")
        );

        List<ClassroomDtos.TaxonomyItemDto> trangThai = List.of(
                new ClassroomDtos.TaxonomyItemDto("HOAT_DONG", "Hoạt động", "Sẵn sàng xếp lịch cho lớp học phần mới"),
                new ClassroomDtos.TaxonomyItemDto("BAO_TRI", "Bảo trì", "Tạm ngừng nhận lịch học mới do đang sửa chữa/bảo dưỡng"),
                new ClassroomDtos.TaxonomyItemDto("NGUNG_SU_DUNG", "Ngừng sử dụng", "Ngừng hoạt động vĩnh viễn hoặc dài hạn; bảo lưu lịch sử lớp cũ")
        );

        return new ClassroomDtos.TaxonomyPhongHocDto(loaiPhong, thietBiPhoBien, trangThai);
    }

    public static String chuanHoaMaPhong(String ma) {
        if (ma == null || ma.isBlank()) {
            throw ApiException.badRequest("Mã phòng học không được để trống");
        }
        return ma.trim().toUpperCase();
    }

    private static String chuanHoaChuoi(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private static String chuanHoaLoaiPhong(String lp) {
        if (lp == null || lp.isBlank()) return "LY_THUYET";
        return lp.trim().toUpperCase();
    }

    private ClassroomDtos.PhongHocDto toDto(Classroom c) {
        long soLop = Math.max(
                lopHocPhanRepository.countByPhongHocId(c.getId()),
                lopHocPhanRepository.countByPhongIgnoreCase(c.getMa())
        );
        String loaiLabel = LOAI_PHONG_LABELS.getOrDefault(c.getLoaiPhong(), c.getLoaiPhong());
        String ttLabel = c.getTrangThai() != null ? c.getTrangThai().getTenHienThi() : "Hoạt động";

        return new ClassroomDtos.PhongHocDto(
                c.getId(),
                c.getMa(),
                c.getTen(),
                c.getToaNha(),
                c.getTang(),
                c.getViTri(),
                c.getSucChua(),
                c.getLoaiPhong(),
                loaiLabel,
                c.getThietBi(),
                c.getTrangThai(),
                ttLabel,
                c.getGhiChu(),
                c.getNgayTao(),
                c.getNgayCapNhat(),
                c.getNguoiCapNhatUsername(),
                soLop
        );
    }

    private Map<String, Object> auditSnapshot(Classroom c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("phongHocId", c.getId());
        map.put("maPhong", c.getMa());
        map.put("tenPhong", c.getTen());
        if (c.getToaNha() != null) map.put("toaNha", c.getToaNha());
        if (c.getTang() != null) map.put("tang", c.getTang());
        if (c.getViTri() != null) map.put("viTri", c.getViTri());
        map.put("sucChua", c.getSucChua());
        map.put("loaiPhong", c.getLoaiPhong());
        if (c.getThietBi() != null) map.put("thietBi", c.getThietBi());
        map.put("trangThaiPhong", c.getTrangThai() != null ? c.getTrangThai().name() : null);
        if (c.getGhiChu() != null) map.put("ghiChu", c.getGhiChu());
        return map;
    }
}
