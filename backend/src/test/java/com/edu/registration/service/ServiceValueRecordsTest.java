package com.edu.registration.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceValueRecordsTest {

    private final byte[] bytesA = "noi-dung-a".getBytes(StandardCharsets.UTF_8);
    private final byte[] bytesACopy = "noi-dung-a".getBytes(StandardCharsets.UTF_8);
    private final byte[] bytesB = "noi-dung-b".getBytes(StandardCharsets.UTF_8);
    private final Object notARecord = "khong-phai-record";

    @Test
    void tepTaiXuongComparesArrayContent() {
        SupportRequestService.TepTaiXuong original =
                new SupportRequestService.TepTaiXuong("tep.pdf", "application/pdf", bytesA);
        SupportRequestService.TepTaiXuong sameContent =
                new SupportRequestService.TepTaiXuong("tep.pdf", "application/pdf", bytesACopy);

        boolean reflexive = original.equals(original);
        assertTrue(reflexive);
        assertEquals(sameContent, original);
        assertEquals(sameContent.hashCode(), original.hashCode());
        assertNotEquals(new SupportRequestService.TepTaiXuong("khac.pdf", "application/pdf", bytesA), original);
        assertNotEquals(new SupportRequestService.TepTaiXuong("tep.pdf", "text/plain", bytesA), original);
        assertNotEquals(new SupportRequestService.TepTaiXuong("tep.pdf", "application/pdf", bytesB), original);
        assertNotEquals(null, original);
        assertNotEquals(notARecord, original);
        assertTrue(original.toString().contains("TepTaiXuong"));
    }

    @Test
    void pdfFileComparesArrayContent() {
        StudentTranscriptPdfService.PdfFile original =
                new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 3, null);
        StudentTranscriptPdfService.PdfFile sameContent =
                new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesACopy, 3, null, null, null, null);
        StudentTranscriptPdfService.PdfFile full =
                new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesACopy, 3, null,
                        "ma-xac-minh", "https://verify", 2);
        StudentTranscriptPdfService.PdfFile fullCopy =
                new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 3, null,
                        "ma-xac-minh", "https://verify", 2);

        boolean reflexive = original.equals(original);
        assertTrue(reflexive);
        assertEquals(sameContent, original);
        assertEquals(sameContent.hashCode(), original.hashCode());
        assertEquals(fullCopy, full);
        assertEquals(fullCopy.hashCode(), full.hashCode());
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("khac.pdf", bytesA, 3, null), original);
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesB, 3, null), original);
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 4, null), original);
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 3, null,
                "ma-khac", "https://verify", 2), full);
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 3, null,
                "ma-xac-minh", "https://verify-khac", 2), full);
        assertNotEquals(new StudentTranscriptPdfService.PdfFile("bang-diem.pdf", bytesA, 3, null,
                "ma-xac-minh", "https://verify", 5), full);
        assertNotEquals(null, original);
        assertNotEquals(notARecord, original);
        assertTrue(original.toString().contains("PdfFile"));
    }

    @Test
    void classExportResultComparesArrayContent() {
        StatisticsService.ClassExportResult original =
                new StatisticsService.ClassExportResult("thong-ke.csv", bytesA, 10);
        StatisticsService.ClassExportResult sameContent =
                new StatisticsService.ClassExportResult("thong-ke.csv", bytesACopy, 10);

        boolean reflexive = original.equals(original);
        assertTrue(reflexive);
        assertEquals(sameContent, original);
        assertEquals(sameContent.hashCode(), original.hashCode());
        assertNotEquals(new StatisticsService.ClassExportResult("khac.csv", bytesA, 10), original);
        assertNotEquals(new StatisticsService.ClassExportResult("thong-ke.csv", bytesB, 10), original);
        assertNotEquals(new StatisticsService.ClassExportResult("thong-ke.csv", bytesA, 11), original);
        assertNotEquals(null, original);
        assertNotEquals(notARecord, original);
        assertTrue(original.toString().contains("ClassExportResult"));
    }

    @Test
    void csvFileComparesArrayContent() {
        ClassStudentExportService.CsvFile original =
                new ClassStudentExportService.CsvFile("sinh-vien.csv", bytesA, 25);
        ClassStudentExportService.CsvFile sameContent =
                new ClassStudentExportService.CsvFile("sinh-vien.csv", bytesACopy, 25);

        boolean reflexive = original.equals(original);
        assertTrue(reflexive);
        assertEquals(sameContent, original);
        assertEquals(sameContent.hashCode(), original.hashCode());
        assertNotEquals(new ClassStudentExportService.CsvFile("khac.csv", bytesA, 25), original);
        assertNotEquals(new ClassStudentExportService.CsvFile("sinh-vien.csv", bytesB, 25), original);
        assertNotEquals(new ClassStudentExportService.CsvFile("sinh-vien.csv", bytesA, 26), original);
        assertNotEquals(null, original);
        assertNotEquals(notARecord, original);
        assertTrue(original.toString().contains("CsvFile"));
    }

    @Test
    void xlsxFileComparesArrayContent() {
        GradeImportService.XlsxFile original =
                new GradeImportService.XlsxFile("bang-diem.xlsx", bytesA, 40);
        GradeImportService.XlsxFile sameContent =
                new GradeImportService.XlsxFile("bang-diem.xlsx", bytesACopy, 40);

        boolean reflexive = original.equals(original);
        assertTrue(reflexive);
        assertEquals(sameContent, original);
        assertEquals(sameContent.hashCode(), original.hashCode());
        assertNotEquals(new GradeImportService.XlsxFile("khac.xlsx", bytesA, 40), original);
        assertNotEquals(new GradeImportService.XlsxFile("bang-diem.xlsx", bytesB, 40), original);
        assertNotEquals(new GradeImportService.XlsxFile("bang-diem.xlsx", bytesA, 41), original);
        assertNotEquals(null, original);
        assertNotEquals(notARecord, original);
        assertTrue(original.toString().contains("XlsxFile"));
    }
}
