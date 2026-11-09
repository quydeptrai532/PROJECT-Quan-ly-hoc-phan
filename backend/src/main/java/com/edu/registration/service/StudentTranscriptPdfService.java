package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentTranscriptPdfService {

    private static final Logger log = LoggerFactory.getLogger(StudentTranscriptPdfService.class);
    private static final String NOT_ASSIGNED = "Chưa cập nhật";
    private static final DateTimeFormatter ISSUE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String FONT_REGULAR_RESOURCE = "liberation/LiberationSans-Regular.ttf";
    private static final String FONT_BOLD_RESOURCE = "liberation/LiberationSans-Bold.ttf";
    private static final String FONT_ITALIC_RESOURCE = "liberation/LiberationSans-Italic.ttf";

    private static final byte[] FONT_REGULAR_BYTES = loadFontBytes(FONT_REGULAR_RESOURCE);
    private static final byte[] FONT_BOLD_BYTES = loadFontBytes(FONT_BOLD_RESOURCE);
    private static final byte[] FONT_ITALIC_BYTES = loadFontBytes(FONT_ITALIC_RESOURCE);

    private static final Color COLOR_PRIMARY = new Color(30, 41, 59);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(203, 213, 225);
    private static final Color COLOR_HEADER_BG = new Color(241, 245, 249);
    private static final Color COLOR_SEMESTER_BG = new Color(226, 232, 240);
    private static final Color COLOR_SUMMARY_BG = new Color(248, 250, 252);
    private static final Color COLOR_PASS = new Color(21, 128, 61);
    private static final Color COLOR_FAIL = new Color(185, 28, 28);

    private final CurrentUser currentUser;
    private final StudentRepository sinhVienRepository;
    private final StudentAcademicProfileService studentAcademicProfileService;
    private final GradeService diemService;
    private final String donViPhatHanh;
    private final Clock clock;

    @Autowired
    public StudentTranscriptPdfService(
            CurrentUser currentUser,
            StudentRepository sinhVienRepository,
            StudentAcademicProfileService studentAcademicProfileService,
            GradeService diemService,
            @Value("${app.export.bang-diem.don-vi-phat-hanh:}") String donViPhatHanh) {
        this(currentUser, sinhVienRepository, studentAcademicProfileService, diemService,
                donViPhatHanh, Clock.systemDefaultZone());
    }

    StudentTranscriptPdfService(
            CurrentUser currentUser,
            StudentRepository sinhVienRepository,
            StudentAcademicProfileService studentAcademicProfileService,
            GradeService diemService,
            String donViPhatHanh,
            Clock clock) {
        this.currentUser = currentUser;
        this.sinhVienRepository = sinhVienRepository;
        this.studentAcademicProfileService = studentAcademicProfileService;
        this.diemService = diemService;
        this.donViPhatHanh = donViPhatHanh == null || donViPhatHanh.isBlank() ? null : donViPhatHanh.trim();
        this.clock = clock;
    }

    public PdfFile xuatBangDiemPdf(Long hocKyId) {
        RegistrationDtos.BangDiemPdfDto duLieu = layDuLieuBangDiem(hocKyId);
        byte[] noiDung = taoFilePdf(duLieu);
        String tenTep = taoTenTep(duLieu.sinhVien().maSinhVien(), hocKyId);
        log.info("Sinh viên {} xuất bảng điểm PDF ({} học phần đã công bố, hocKyId={})",
                duLieu.sinhVien().maSinhVien(), duLieu.tongSoMonDaCongBo(), hocKyId);
        return new PdfFile(tenTep, noiDung, duLieu.tongSoMonDaCongBo(), duLieu);
    }

    public RegistrationDtos.BangDiemPdfDto layDuLieuBangDiem(Long hocKyId) {
        Student sinhVien = laySinhVienHienTai();
        StudentAcademicProfileDto hoSo = studentAcademicProfileService.getMyProfile();
        RegistrationDtos.LichSuHocTapDto lichSu = diemService.lichSuHocTap(hocKyId, null);

        List<RegistrationDtos.MucLichSuHocTapDto> mucDaCongBo = lichSu.items().stream()
                .filter(item -> item.diemTongKet() != null && !"CHUA_CO_DIEM".equals(item.trangThaiKetQua()))
                .toList();

        if (mucDaCongBo.isEmpty()) {
            throw ApiException.badRequest("Chưa có kết quả học tập đã công bố để xuất bảng điểm PDF");
        }

        Map<Long, RegistrationDtos.HocKyTongKetDto> tongKetMap = lichSu.hocKy().stream()
                .collect(Collectors.toMap(RegistrationDtos.HocKyTongKetDto::hocKyId, Function.identity()));

        Map<Long, List<RegistrationDtos.MucLichSuHocTapDto>> theoHocKy = mucDaCongBo.stream()
                .collect(Collectors.groupingBy(RegistrationDtos.MucLichSuHocTapDto::hocKyId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RegistrationDtos.HocKyBangDiemPdfDto> danhSachHocKy = new ArrayList<>();
        int sttToanBang = 1;
        for (Map.Entry<Long, List<RegistrationDtos.MucLichSuHocTapDto>> entry : theoHocKy.entrySet()) {
            Long kyId = entry.getKey();
            List<RegistrationDtos.MucLichSuHocTapDto> danhSachMon = entry.getValue();
            RegistrationDtos.MucLichSuHocTapDto dau = danhSachMon.get(0);
            RegistrationDtos.HocKyTongKetDto tongKetKy = tongKetMap.get(kyId);

            int tinChiDatKy = 0;
            List<RegistrationDtos.MucBangDiemPdfDto> mucTrongKy = new ArrayList<>(danhSachMon.size());
            for (RegistrationDtos.MucLichSuHocTapDto item : danhSachMon) {
                if (item.diemTongKet() >= AcademicRules.DIEM_DAT) {
                    tinChiDatKy += item.soTinChi();
                }
                double diemHe4 = diemService.quyDoiHe4(item.diemTongKet());
                mucTrongKy.add(new RegistrationDtos.MucBangDiemPdfDto(
                        sttToanBang++,
                        item.dangKyId(),
                        item.hocKyId(),
                        item.tenHocKy(),
                        item.namHoc(),
                        item.maMon(),
                        item.tenMon(),
                        item.soTinChi(),
                        item.maLop(),
                        item.lanHoc(),
                        item.tongSoLanHoc(),
                        item.diemQuaTrinh(),
                        item.diemCuoiKy(),
                        item.diemTongKet(),
                        diemHe4,
                        item.trangThaiKetQua(),
                        item.tenTrangThaiKetQua()));
            }

            int tinChiCoDiemKy = tongKetKy != null ? tongKetKy.tinChiCoDiem() : 0;
            double gpa10Ky = tongKetKy != null ? tongKetKy.gpa10() : 0.0;
            double gpa4Ky = tongKetKy != null ? tongKetKy.gpa4() : 0.0;

            danhSachHocKy.add(new RegistrationDtos.HocKyBangDiemPdfDto(
                    kyId,
                    dau.tenHocKy(),
                    dau.namHoc(),
                    tinChiCoDiemKy,
                    tinChiDatKy,
                    gpa10Ky,
                    gpa4Ky,
                    mucTrongKy));
        }

        RegistrationDtos.ThongTinSinhVienPdfDto thongTinSv = new RegistrationDtos.ThongTinSinhVienPdfDto(
                sinhVien.getMa(),
                sinhVien.getHoTen(),
                chuanHoaGiaTri(sinhVien.getEmail()),
                dinhDangMaTen(hoSo.khoaMa(), hoSo.khoaTen()),
                dinhDangMaTen(hoSo.nganhMa(), hoSo.nganhTen()),
                dinhDangMaTen(hoSo.lopMa(), hoSo.lopTen()),
                chuanHoaGiaTri(hoSo.khoaNhapHoc()),
                chuanHoaGiaTri(hoSo.chuongTrinhPhienBan()));

        boolean coNhieuLanHoc = mucDaCongBo.stream().anyMatch(item -> item.tongSoLanHoc() > 1)
                || lichSu.coNhieuLanHoc();

        return new RegistrationDtos.BangDiemPdfDto(
                thongTinSv,
                donViPhatHanh,
                LocalDateTime.now(clock),
                danhSachHocKy,
                mucDaCongBo.size(),
                lichSu.tinChiCoDiem(),
                lichSu.tinChiTichLuy(),
                lichSu.gpa10(),
                lichSu.gpa4(),
                coNhieuLanHoc);
    }

    private byte[] taoFilePdf(RegistrationDtos.BangDiemPdfDto duLieu) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BaseFont bfRegular = BaseFont.createFont(
                    "LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, FONT_REGULAR_BYTES, null);
            BaseFont bfBold = BaseFont.createFont(
                    "LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, FONT_BOLD_BYTES, null);
            BaseFont bfItalic = BaseFont.createFont(
                    "LiberationSans-Italic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, FONT_ITALIC_BYTES, null);

            Font fontOrg = new Font(bfBold, 11, Font.BOLD, COLOR_PRIMARY);
            Font fontTitle = new Font(bfBold, 16, Font.BOLD, COLOR_PRIMARY);
            Font fontSubtitle = new Font(bfItalic, 9.5f, Font.ITALIC, COLOR_MUTED);
            Font fontSection = new Font(bfBold, 11, Font.BOLD, COLOR_PRIMARY);
            Font fontLabel = new Font(bfBold, 9.5f, Font.BOLD, COLOR_PRIMARY);
            Font fontValue = new Font(bfRegular, 9.5f, Font.NORMAL, COLOR_PRIMARY);
            Font fontTableHeader = new Font(bfBold, 9f, Font.BOLD, COLOR_PRIMARY);
            Font fontTableCell = new Font(bfRegular, 9f, Font.NORMAL, COLOR_PRIMARY);
            Font fontTableCellBold = new Font(bfBold, 9f, Font.BOLD, COLOR_PRIMARY);
            Font fontPass = new Font(bfBold, 9f, Font.BOLD, COLOR_PASS);
            Font fontFail = new Font(bfBold, 9f, Font.BOLD, COLOR_FAIL);
            Font fontNote = new Font(bfItalic, 8.5f, Font.ITALIC, COLOR_MUTED);

            String thoiDiemStr = duLieu.thoiDiemPhatHanh().format(ISSUE_TIME_FORMATTER);

            Document document = new Document(PageSize.A4, 36, 36, 40, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new TranscriptFooterPageEvent(
                    bfRegular, duLieu.sinhVien().maSinhVien(), thoiDiemStr));

            document.open();
            document.addTitle("Bảng điểm cá nhân - " + duLieu.sinhVien().maSinhVien());
            document.addAuthor("Hệ thống Quản lý đào tạo");

            if (duLieu.donViPhatHanh() != null && !duLieu.donViPhatHanh().isBlank()) {
                Paragraph orgPara = new Paragraph(duLieu.donViPhatHanh().toUpperCase(Locale.ROOT), fontOrg);
                orgPara.setAlignment(Element.ALIGN_CENTER);
                orgPara.setSpacingAfter(4f);
                document.add(orgPara);
            }

            Paragraph titlePara = new Paragraph("BẢNG ĐIỂM HỌC TẬP CÁ NHÂN", fontTitle);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(3f);
            document.add(titlePara);

            Paragraph subPara = new Paragraph(
                    "Bản trích xuất kết quả học tập đã công bố trên hệ thống · Thời điểm phát hành: " + thoiDiemStr,
                    fontSubtitle);
            subPara.setAlignment(Element.ALIGN_CENTER);
            subPara.setSpacingAfter(14f);
            document.add(subPara);

            document.add(taoBangThongTinSinhVien(duLieu.sinhVien(), fontLabel, fontValue));
            document.add( taoBangTongHopToanKhoa(duLieu, fontLabel, fontValue));

            for (RegistrationDtos.HocKyBangDiemPdfDto ky : duLieu.hocKy()) {
                Paragraph kyHeader = new Paragraph(
                        String.format(Locale.US,
                                "%s - Năm học %s   |   Tín chỉ có điểm: %d   |   Tín chỉ đạt: %d   |   GPA hệ 10: %.2f   |   GPA hệ 4: %.2f",
                                ky.tenHocKy(), ky.namHoc(), ky.tinChiCoDiem(), ky.tinChiDat(), ky.gpa10(), ky.gpa4()),
                        fontSection);
                kyHeader.setSpacingBefore(10f);
                kyHeader.setSpacingAfter(5f);
                document.add(kyHeader);

                document.add(taoBangDiemHocKy(
                        ky.items(), fontTableHeader, fontTableCell, fontTableCellBold, fontPass, fontFail));
            }

            document.add(taoKhoiGhiChu(duLieu, fontLabel, fontNote));

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Không thể khởi tạo tệp bảng điểm PDF", ex);
        }
    }

    private PdfPTable taoBangThongTinSinhVien(RegistrationDtos.ThongTinSinhVienPdfDto sv,
                                              Font fontLabel, Font fontValue) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{18f, 32f, 20f, 30f});
        table.setSpacingAfter(10f);

        themCapThongTin(table, "Họ và tên:", sv.hoTen(), fontLabel, fontValue);
        themCapThongTin(table, "Mã sinh viên:", sv.maSinhVien(), fontLabel, fontValue);
        themCapThongTin(table, "Khoa:", sv.khoa(), fontLabel, fontValue);
        themCapThongTin(table, "Ngành:", sv.nganh(), fontLabel, fontValue);
        themCapThongTin(table, "Lớp hành chính:", sv.lopHanhChinh(), fontLabel, fontValue);
        themCapThongTin(table, "Email:", sv.email(), fontLabel, fontValue);
        themCapThongTin(table, "Khóa nhập học:", sv.khoaNhapHoc(), fontLabel, fontValue);
        themCapThongTin(table, "Phiên bản CTĐT:", sv.phienBanChuongTrinh(), fontLabel, fontValue);
        return table;
    }

    private void themCapThongTin(PdfPTable table, String nhan, String giaTri,
                                 Font fontLabel, Font fontValue) {
        PdfPCell labelCell = new PdfPCell(new Phrase(nhan, fontLabel));
        labelCell.setBackgroundColor(COLOR_SUMMARY_BG);
        labelCell.setBorderColor(COLOR_BORDER);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(giaTri != null ? giaTri : NOT_ASSIGNED, fontValue));
        valueCell.setBackgroundColor(Color.WHITE);
        valueCell.setBorderColor(COLOR_BORDER);
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    private PdfPTable taoBangTongHopToanKhoa(RegistrationDtos.BangDiemPdfDto duLieu,
                                             Font fontLabel, Font fontValue) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{20f, 20f, 22f, 19f, 19f});
        table.setSpacingAfter(6f);

        themOThongKe(table, "Học phần đã công bố", String.valueOf(duLieu.tongSoMonDaCongBo()), fontLabel, fontValue);
        themOThongKe(table, "Tổng tín chỉ có điểm", String.valueOf(duLieu.tongTinChiCoDiem()), fontLabel, fontValue);
        themOThongKe(table, "Tín chỉ tích lũy (≥ 5.0)", String.valueOf(duLieu.tinChiTichLuy()), fontLabel, fontValue);
        themOThongKe(table, "GPA tích lũy hệ 10", String.format(Locale.US, "%.2f", duLieu.gpa10()), fontLabel, fontValue);
        themOThongKe(table, "GPA tích lũy hệ 4", String.format(Locale.US, "%.2f", duLieu.gpa4()), fontLabel, fontValue);
        return table;
    }

    private void themOThongKe(PdfPTable table, String tieuDe, String giaTri,
                              Font fontLabel, Font fontValue) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(tieuDe + "\n", fontValue));
        p.add(new Phrase(giaTri, fontLabel));
        PdfPCell cell = new PdfPCell(p);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private PdfPTable taoBangDiemHocKy(List<RegistrationDtos.MucBangDiemPdfDto> items,
                                       Font fontHeader, Font fontCell, Font fontCellBold,
                                       Font fontPass, Font fontFail) throws DocumentException {
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{5f, 9.5f, 23.5f, 18f, 6f, 7f, 7f, 7.5f, 7f, 9.5f});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSpacingAfter(6f);

        String[] headers = {
                "STT", "Mã môn", "Tên môn học", "Lớp học phần", "Tín chỉ",
                "Quá trình", "Cuối kỳ", "Tổng kết", "Hệ 4", "Kết quả"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(COLOR_SEMESTER_BG);
            cell.setBorderColor(COLOR_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5f);
            table.addCell(cell);
        }

        for (RegistrationDtos.MucBangDiemPdfDto item : items) {
            String maLopHienThi = item.tongSoLanHoc() > 1
                    ? item.maLop() + " (Lần " + item.lanHoc() + "/" + item.tongSoLanHoc() + ")"
                    : item.maLop();
            boolean dat = "DAT".equals(item.trangThaiKetQua());

            table.addCell(taoODuLieu(String.valueOf(item.stt()), fontCell, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(item.maMon(), fontCellBold, Element.ALIGN_LEFT));
            table.addCell(taoODuLieu(item.tenMon(), fontCell, Element.ALIGN_LEFT));
            table.addCell(taoODuLieu(maLopHienThi, fontCell, Element.ALIGN_LEFT));
            table.addCell(taoODuLieu(String.valueOf(item.soTinChi()), fontCell, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(dinhDangDiem(item.diemQuaTrinh()), fontCell, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(dinhDangDiem(item.diemCuoiKy()), fontCell, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(dinhDangDiem(item.diemTongKet()), fontCellBold, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(String.format(Locale.US, "%.1f", item.diemHe4()), fontCell, Element.ALIGN_CENTER));
            table.addCell(taoODuLieu(item.tenTrangThaiKetQua(), dat ? fontPass : fontFail, Element.ALIGN_CENTER));
        }

        return table;
    }

    private PdfPCell taoODuLieu(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4.5f);
        return cell;
    }

    private PdfPTable taoKhoiGhiChu(RegistrationDtos.BangDiemPdfDto duLieu,
                                    Font fontLabel, Font fontNote) {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100f);
        box.setSpacingBefore(8f);

        Paragraph p = new Paragraph();
        p.add(new Phrase("Ghi chú về phạm vi dữ liệu:\n", fontLabel));
        p.add(new Phrase("• Bảng điểm PDF chỉ xuất các học phần đã có điểm tổng kết được công bố (ngưỡng đạt từ 5.0/10). Các học phần đang học hoặc chờ công bố điểm không đưa vào tệp này.\n", fontNote));
        p.add(new Phrase("• Quy chế thay thế điểm khi học lại/học cải thiện để tính GPA chính thức chưa được cấu hình; các lần học đã công bố được liệt kê riêng và tính theo công thức hiện hành của hệ thống.\n", fontNote));
        if (duLieu.donViPhatHanh() == null || duLieu.donViPhatHanh().isBlank()) {
            p.add(new Phrase("• Tên đơn vị phát hành, mẫu bảng điểm hành chính, chữ ký và con dấu xác nhận chưa được cấu hình trong hệ thống; tài liệu này chỉ phục vụ tra cứu học vụ cá nhân.", fontNote));
        } else {
            p.add(new Phrase("• Mẫu chữ ký và con dấu xác nhận hành chính chưa được cấu hình trong hệ thống; tài liệu này chỉ phục vụ tra cứu học vụ cá nhân.", fontNote));
        }

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(COLOR_SUMMARY_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(7f);
        box.addCell(cell);
        return box;
    }

    private Student laySinhVienHienTai() {
        User user = currentUser.get();
        if (user.getId() != null) {
            return sinhVienRepository.findWithAcademicProfileByUserId(user.getId())
                    .or(() -> sinhVienRepository.findByUser(user))
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        }
        return sinhVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private static String dinhDangDiem(Double diem) {
        if (diem == null) {
            return "—";
        }
        return String.format(Locale.US, "%.1f", diem);
    }

    private static String chuanHoaGiaTri(String value) {
        return value == null || value.isBlank() ? NOT_ASSIGNED : value.trim();
    }

    private static String dinhDangMaTen(String ma, String ten) {
        boolean coMa = ma != null && !ma.isBlank() && !NOT_ASSIGNED.equals(ma);
        boolean coTen = ten != null && !ten.isBlank() && !NOT_ASSIGNED.equals(ten);
        if (coMa && coTen) {
            return ten + " (" + ma + ")";
        }
        if (coTen) {
            return ten;
        }
        if (coMa) {
            return ma;
        }
        return NOT_ASSIGNED;
    }

    private static String taoTenTep(String maSinhVien, Long hocKyId) {
        String maAnToan = (maSinhVien == null || maSinhVien.isBlank() ? "sinh-vien" : maSinhVien)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (hocKyId != null) {
            return "bang-diem-" + maAnToan + "-hk" + hocKyId + ".pdf";
        }
        return "bang-diem-" + maAnToan + ".pdf";
    }

    private static byte[] loadFontBytes(String resourcePath) {
        try (InputStream is = StudentTranscriptPdfService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy font PDF: " + resourcePath);
            }
            return is.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể đọc font PDF: " + resourcePath, ex);
        }
    }

    private static final class TranscriptFooterPageEvent extends PdfPageEventHelper {
        private final BaseFont baseFont;
        private final String maSinhVien;
        private final String thoiDiemPhatHanh;
        private PdfTemplate totalPagesTemplate;

        private TranscriptFooterPageEvent(BaseFont baseFont, String maSinhVien, String thoiDiemPhatHanh) {
            this.baseFont = baseFont;
            this.maSinhVien = maSinhVien;
            this.thoiDiemPhatHanh = thoiDiemPhatHanh;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 18f;

            cb.saveState();
            cb.setColorStroke(COLOR_BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(document.left(), document.bottom() - 6f);
            cb.lineTo(document.right(), document.bottom() - 6f);
            cb.stroke();
            cb.restoreState();

            cb.beginText();
            cb.setFontAndSize(baseFont, 8f);
            cb.setColorFill(COLOR_MUTED);
            cb.showTextAligned(Element.ALIGN_LEFT,
                    "Bảng điểm cá nhân · Mã SV: " + maSinhVien + " · Phát hành: " + thoiDiemPhatHanh,
                    document.left(), y, 0);

            String pageText = "Trang " + writer.getPageNumber() + " / ";
            float textWidth = baseFont.getWidthPoint(pageText, 8f);
            float rightX = document.right() - 14f;
            cb.showTextAligned(Element.ALIGN_RIGHT, pageText, rightX, y, 0);
            cb.endText();

            cb.addTemplate(totalPagesTemplate, rightX, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, 8f);
            totalPagesTemplate.setColorFill(COLOR_MUTED);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPagesTemplate.endText();
        }
    }

    public record PdfFile(String tenTep, byte[] noiDung, int soMonDaCongBo,
                          RegistrationDtos.BangDiemPdfDto duLieu) {
    }
}
