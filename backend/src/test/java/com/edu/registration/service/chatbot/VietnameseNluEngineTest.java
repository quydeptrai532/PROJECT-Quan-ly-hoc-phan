package com.edu.registration.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    @DisplayName("Nhận diện lời chào hỏi tự nhiên")
    void testGreetingIntent() {
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("Xin chào").intent());
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("Chào bạn!").intent());
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("hello").intent());
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("Hi").intent());
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("Alo").intent());
        assertEquals(VietnameseNluEngine.Intent.GREETING, nluEngine.parse("Xin chào trợ lý").intent());
    }

    @Test
    @DisplayName("Lời chào kèm yêu cầu tra cứu vẫn ưu tiên đúng intent nghiệp vụ")
    void testGreetingMixedWithBusinessRequest() {
        var res = nluEngine.parse("Chào bạn, tôi muốn đăng ký lớp LHP01");
        assertEquals(VietnameseNluEngine.Intent.DANG_KY, res.intent());
    }

    @Test
    @DisplayName("Nhận diện lời cảm ơn và tạm biệt")
    void testThanksAndGoodbyeIntent() {
        assertEquals(VietnameseNluEngine.Intent.THANKS, nluEngine.parse("Cảm ơn bạn").intent());
        assertEquals(VietnameseNluEngine.Intent.THANKS, nluEngine.parse("thanks").intent());
        assertEquals(VietnameseNluEngine.Intent.GOODBYE, nluEngine.parse("Tạm biệt").intent());
        assertEquals(VietnameseNluEngine.Intent.GOODBYE, nluEngine.parse("bye").intent());
    }

    @Test
    @DisplayName("Nhận diện câu hỏi thời tiết")
    void testWeatherIntent() {
        assertEquals(VietnameseNluEngine.Intent.THOI_TIET,
                nluEngine.parse("Thời tiết hôm nay thế nào?").intent());
        assertEquals(VietnameseNluEngine.Intent.THOI_TIET,
                nluEngine.parse("Nhiệt độ hôm nay bao nhiêu?").intent());
        assertEquals(VietnameseNluEngine.Intent.THOI_TIET,
                nluEngine.parse("Dự báo thời tiết Đà Nẵng").intent());
    }

    @Test
    @DisplayName("Câu hỏi lịch sử/thời tiết/du lịch không bị nhầm thành lịch học hay giảng viên")
    void testGeneralKnowledgeQuestionsAreNotMisrouted() {
        assertEquals(VietnameseNluEngine.Intent.UNKNOWN,
                nluEngine.parse("Lịch sử Việt Nam có gì nổi bật?").intent());
        assertNotEquals(VietnameseNluEngine.Intent.TRA_CUU_LICH,
                nluEngine.parse("Thời tiết hôm nay thế nào?").intent());
        assertNotEquals(VietnameseNluEngine.Intent.TRA_CUU_LICH,
                nluEngine.parse("Du lịch Đà Lạt có gì?").intent());
    }

    @Test
    @DisplayName("Vẫn nhận diện đúng câu hỏi giảng viên và phòng học")
    void testLecturerAndRoomIntentStillWork() {
        assertEquals(VietnameseNluEngine.Intent.TRA_CUU_GIANG_VIEN,
                nluEngine.parse("Giảng viên dạy lớp LHP01 là ai?").intent());
        assertEquals(VietnameseNluEngine.Intent.TRA_CUU_PHONG,
                nluEngine.parse("Lớp LHP01 học ở phòng nào?").intent());
    }

    @Test
    @DisplayName("Nhận diện câu hỏi giới thiệu về trợ lý")
    void testSmallTalkIntent() {
        assertEquals(VietnameseNluEngine.Intent.SMALL_TALK, nluEngine.parse("Bạn là ai?").intent());
        assertEquals(VietnameseNluEngine.Intent.SMALL_TALK, nluEngine.parse("Bạn tên gì").intent());
        assertEquals(VietnameseNluEngine.Intent.SMALL_TALK, nluEngine.parse("Bạn khỏe không").intent());
    }
}
