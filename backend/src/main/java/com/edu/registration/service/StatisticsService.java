package com.edu.registration.service;

import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    public static final int SAP_DAY_PERCENT = 90;
    public static final int IT_DANG_KY_PERCENT = 25;
    public static final int SAP_DONG_TRONG_NGAY = 7;

    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final SubjectRepository monHocRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final SemesterService hocKyService;
    private final FacultyRepository khoaRepository;

    public StatisticsService(StudentRepository sinhVienRepository, LecturerRepository giangVienRepository,
                          SubjectRepository monHocRepository, CourseClassRepository lopHocPhanRepository,
                          CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                          SemesterService hocKyService, FacultyRepository khoaRepository) {
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.monHocRepository = monHocRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
        this.khoaRepository = khoaRepository;
    }

    public StatDtos.TongQuanDto tongQuan() {
        Semester hocKy = hocKyService.hocKyHienTai();
        return new StatDtos.TongQuanDto(
                sinhVienRepository.count(),
                giangVienRepository.count(),
                monHocRepository.count(),
                lopHocPhanRepository.count(),
                dangKyHocPhanRepository.countByTrangThai(RegistrationStatus.DA_DANG_KY),
                hocKy != null ? hocKy.getTen() + " - " + hocKy.getNamHoc() : "Chưa có học kỳ");
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId) {
        return theoMon(hocKyId, null);
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId, Long khoaId) {
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseClass> danhSachLop = khoaId == null
                ? lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId())
                : lopHocPhanRepository.findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(hocKy.getId(), khoaId);

        Map<Subject, List<CourseClass>> theoMon = danhSachLop.stream()
                .collect(Collectors.groupingBy(CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));

        List<StatDtos.ThongKeMonDto> ketQua = new ArrayList<>();
        theoMon.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getMa()))
                .forEach(entry -> {
                    Subject monHoc = entry.getKey();
                    List<CourseClass> lopCuaMon = entry.getValue();
                    int tongChiTieu = lopCuaMon.stream().mapToInt(CourseClass::getSiSoToiDa).sum();
                    int daDangKy = lopCuaMon.stream().mapToInt(CourseClass::getSiSoHienTai).sum();
                    double tiLe = tongChiTieu == 0 ? 0
                            : Math.round((daDangKy * 1000.0 / tongChiTieu)) / 10.0;
                    ketQua.add(new StatDtos.ThongKeMonDto(monHoc.getId(), monHoc.getMa(), monHoc.getTen(),
                            lopCuaMon.size(), tongChiTieu, daDangKy, tiLe));
                });
        return ketQua;
    }

    public StatDtos.HanhDongHocVuDto hanhDong(Long hocKyId, Long khoaId) {
        Semester hocKy = hocKyId == null ? hocKyService.hocKyHienTai() : hocKyService.lay(hocKyId);
        if (hocKy == null) {
            return new StatDtos.HanhDongHocVuDto(null, null, khoaId, null, LocalDateTime.now(), List.of());
        }
        String tenKhoa = khoaId == null ? null : khoaRepository.findById(khoaId)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")).getTen();
        List<CourseClass> lopHocPhans = khoaId == null
                ? lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId())
                : lopHocPhanRepository.findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(hocKy.getId(), khoaId);
        Map<Long, Long> siSoThucTe = lopHocPhanRepository.countDangKyHienHanhTheoLop(
                        hocKy.getId(), khoaId, RegistrationStatus.DA_DANG_KY).stream()
                .collect(Collectors.toMap(CourseClassRepository.DangKyTheoLop::getLopHocPhanId,
                        CourseClassRepository.DangKyTheoLop::getSoDangKy));

        List<StatDtos.CanhBaoHocVuDto> canhBao = new ArrayList<>();
        Map<Subject, List<CourseClass>> theoMon = lopHocPhans.stream().collect(Collectors.groupingBy(
                CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));
        for (CourseClass lop : lopHocPhans) {
            long daDangKy = siSoThucTe.getOrDefault(lop.getId(), 0L);
            int chiTieu = lop.getSiSoToiDa();
            double tiLe = chiTieu == 0 ? 0 : daDangKy * 100.0 / chiTieu;
            if (tiLe >= SAP_DAY_PERCENT) {
                canhBao.add(canhBao("LOP_GAN_DAY", "Lớp gần đầy",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng từ " + SAP_DAY_PERCENT + "%.",
                        lop, daDangKy, chiTieu, tiLe, hocKy));
            } else if (tiLe <= IT_DANG_KY_PERCENT) {
                canhBao.add(canhBao("LOP_IT_DANG_KY", "Lớp ít đăng ký",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng không quá "
                                + IT_DANG_KY_PERCENT + "%.", lop, daDangKy, chiTieu, tiLe, hocKy));
            }
        }
        for (Map.Entry<Subject, List<CourseClass>> entry : theoMon.entrySet()) {
            List<CourseClass> sections = entry.getValue();
            long daDangKy = sections.stream().mapToLong(lop -> siSoThucTe.getOrDefault(lop.getId(), 0L)).sum();
            long tongChiTieu = sections.stream().mapToLong(CourseClass::getSiSoToiDa).sum();
            if (tongChiTieu > 0 && daDangKy >= tongChiTieu) {
                Subject mon = entry.getKey();
                double tiLe = daDangKy * 100.0 / tongChiTieu;
                canhBao.add(new StatDtos.CanhBaoHocVuDto("MON_HET_CHO", "Nhu cầu môn đạt chỉ tiêu",
                        daDangKy + "/" + tongChiTieu + " chỗ đã đăng ký trên " + sections.size()
                                + " lớp; đăng ký đang hiệu lực đạt hoặc vượt tổng chỉ tiêu.",
                        "Tổng đăng ký đang hiệu lực của các lớp môn / tổng chỉ tiêu các lớp môn >= 100%.",
                        null, null, mon.getId(), mon.getMa(), mon.getTen(), sections.size(), daDangKy,
                        tongChiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                        hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                                + "&keyword=" + mon.getMa()));
            }
        }
        LocalDate homNay = LocalDate.now();
        long soNgayConLai = java.time.temporal.ChronoUnit.DAYS.between(homNay, hocKy.getDangKyDen());
        if (hocKy.dangMoDangKy() && soNgayConLai >= 0 && soNgayConLai <= SAP_DONG_TRONG_NGAY) {
            canhBao.add(new StatDtos.CanhBaoHocVuDto("DOT_DANG_KY_SAP_DONG", "Đợt đăng ký sắp đóng",
                    "Còn " + soNgayConLai + " ngày; hạn đăng ký " + hocKy.getDangKyDen() + ".",
                    "Đợt đang mở và ngày kết thúc nằm trong " + SAP_DONG_TRONG_NGAY + " ngày tới.",
                    null, null, null, null, null, lopHocPhans.size(), 0, 0, 0,
                    hocKy.getId(), tenHocKy(hocKy), hocKy.getDangKyDen(),
                    "/quan-tri/hoc-ky"));
        }
        return new StatDtos.HanhDongHocVuDto(hocKy.getId(), tenHocKy(hocKy), khoaId, tenKhoa,
                LocalDateTime.now(), canhBao);
    }

    private StatDtos.CanhBaoHocVuDto canhBao(String loai, String tieuDe, String lyDo, CourseClass lop,
                                              long daDangKy, long chiTieu, double tiLe, Semester hocKy) {
        return new StatDtos.CanhBaoHocVuDto(loai, tieuDe, lyDo,
                "Sĩ số đăng ký đang hiệu lực / chỉ tiêu lớp; " + ("LOP_GAN_DAY".equals(loai)
                        ? "ngưỡng >= " + SAP_DAY_PERCENT + "%" : "ngưỡng <= " + IT_DANG_KY_PERCENT + "%"),
                lop.getId(), lop.getMa(), lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                1, daDangKy, chiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                        + "&keyword=" + java.net.URLEncoder.encode(lop.getMa(), StandardCharsets.UTF_8));
    }

    private String tenHocKy(Semester hocKy) {
        return hocKy.getTen() + " - " + hocKy.getNamHoc();
    }

    public byte[] xuatDanhSachLop(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);

        Map<Long, Grade> diemMap = danhSach.isEmpty() ? Map.of()
                : diemRepository.findByDangKyIdIn(danhSach.stream().map(CourseRegistration::getId).toList())
                .stream().collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("STT,Mã sinh viên,Họ tên,Email,Điểm quá trình,Điểm cuối kỳ,Điểm tổng kết\n");
        int stt = 1;
        for (CourseRegistration dangKy : danhSach) {
            Grade diem = diemMap.get(dangKy.getId());
            csv.append(stt++).append(',')
                    .append(escape(dangKy.getSinhVien().getMa())).append(',')
                    .append(escape(dangKy.getSinhVien().getHoTen())).append(',')
                    .append(escape(dangKy.getSinhVien().getEmail())).append(',')
                    .append(diem != null && diem.getDiemQuaTrinh() != null ? diem.getDiemQuaTrinh() : "").append(',')
                    .append(diem != null && diem.getDiemCuoiKy() != null ? diem.getDiemCuoiKy() : "").append(',')
                    .append(diem != null && diem.getDiemTongKet() != null ? diem.getDiemTongKet() : "").append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
