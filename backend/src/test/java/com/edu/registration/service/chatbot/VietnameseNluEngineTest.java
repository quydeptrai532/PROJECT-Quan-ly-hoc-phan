package com.edu.registration.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VietnameseNluEngineTest {

    private VietnameseNluEngine nluEngine;

    @BeforeEach
    void setUp() {
        nluEngine = new VietnameseNluEngine();
    }

    @Test
    @DisplayName("Nhận diện Intent đăng ký và trích xuất mã lớp")
    void testRegistrationIntent() {
        var res = nluEngine.parse("Tôi muốn đăng ký lớp LHP01");
        assertEquals(VietnameseNluEngine.Intent.DANG_KY, res.intent());
        assertEquals("LHP01", res.maLop());
    }

    @Test
    @DisplayName("Nhận diện Intent xác nhận đăng ký")
    void testConfirmationIntent() {
        assertEquals(VietnameseNluEngine.Intent.XAC_NHAN, nluEngine.parse("Xác nhận").intent());
        assertEquals(VietnameseNluEngine.Intent.XAC_NHAN, nluEngine.parse("Đồng ý").intent());
        assertEquals(VietnameseNluEngine.Intent.XAC_NHAN, nluEngine.parse("Xác nhận đăng ký").intent());
        assertEquals(VietnameseNluEngine.Intent.XAC_NHAN, nluEngine.parse("OK").intent());
    }

    @Test
    @DisplayName("Nhận diện Intent hủy thao tác")
    void testCancelIntent() {
        assertEquals(VietnameseNluEngine.Intent.HUY_XAC_NHAN, nluEngine.parse("Hủy").intent());
        assertEquals(VietnameseNluEngine.Intent.HUY_XAC_NHAN, nluEngine.parse("Không đăng ký nữa").intent());
        assertEquals(VietnameseNluEngine.Intent.HUY_XAC_NHAN, nluEngine.parse("Bỏ qua").intent());
    }

    @Test
    @DisplayName("Nhận diện tìm lớp thay thế")
    void testAlternativeClassIntent() {
        var res = nluEngine.parse("Lớp LHP01 đã đầy rồi, có lớp nào thay thế không?");
        assertEquals(VietnameseNluEngine.Intent.LOP_THAY_THE, res.intent());
        assertEquals("LHP01", res.maLop());
    }

    @Test
    @DisplayName("Nhận diện tra cứu chỗ còn và lịch học theo thứ")
    void testSeatAvailabilityAndSchedule() {
        var res = nluEngine.parse("Tìm các lớp môn Lập trình Java còn chỗ học thứ 4");
        assertTrue(res.onlyAvailableSeats());
        assertEquals(4, res.thu());
        assertNotNull(res.tenMonOrKeyword());
    }

    @Test
    @DisplayName("Nhận diện lựa chọn số thứ tự")
    void testSelectionIndex() {
        var res1 = nluEngine.parse("1");
        assertEquals(1, res1.selectionIndex());

        var res2 = nluEngine.parse("Chọn 2");
        assertEquals(2, res2.selectionIndex());
    }

    @Test
    @DisplayName("Nhận diện thời khóa biểu cá nhân")
    void testPersonalTimetableIntent() {
        var res = nluEngine.parse("Xem thời khóa biểu của tôi");
        assertEquals(VietnameseNluEngine.Intent.TKB_CA_NHAN, res.intent());
    }

    @Test
    @DisplayName("Nhận diện thống kê quản trị")
    void testStatisticsIntent() {
        var res = nluEngine.parse("Xem thống kê tổng quan đào tạo");
        assertEquals(VietnameseNluEngine.Intent.THONG_KE, res.intent());
    }
}
