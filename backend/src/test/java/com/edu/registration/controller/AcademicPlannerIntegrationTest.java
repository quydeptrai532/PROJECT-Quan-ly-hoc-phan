package com.edu.registration.controller;

import com.edu.registration.entity.*;
import com.edu.registration.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AcademicPlannerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("QLSV-96: Chưa đăng nhập truy cập endpoint kế hoạch môn còn thiếu bị trả về 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/sinh-vien/me/ke-hoach-mon-thieu"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("QLSV-96: Giảng viên hoặc Quản trị viên không được truy cập endpoint của sinh viên (HTTP 403)")
    void nonStudentRolesForbidden() throws Exception {
        String tokenGv = login("gv01", "123456");
        mockMvc.perform(get("/api/sinh-vien/me/ke-hoach-mon-thieu")
                        .header("Authorization", "Bearer " + tokenGv))
                .andExpect(status().isForbidden());

        String tokenAdmin = login("admin", "admin123");
        mockMvc.perform(get("/api/sinh-vien/me/ke-hoach-mon-thieu")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QLSV-96: Sinh viên chưa được gán chương trình nhận thông báo trạng thái rỗng/lỗi rõ ràng (HTTP 200 configured=false)")
    void studentWithoutCurriculumReturnsCleanUnconfiguredState() throws Exception {
        String tokenSv = login("sv04", "123456");
        MvcResult res = mockMvc.perform(get("/api/sinh-vien/me/ke-hoach-mon-thieu")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json(res);
        assertFalse(body.get("configured").asBoolean());
        assertTrue(body.get("message").asText().contains("chưa được gán"));
        assertTrue(body.get("missingGroups").isEmpty());
        assertTrue(body.get("registrationSuggestions").isEmpty());
        assertTrue(body.get("limitationsAndDisclaimers").size() > 0);
    }

    @Test
    @DisplayName("QLSV-96: Sinh viên có chương trình đào tạo được khảo sát đầy đủ thông tin kế hoạch và cảnh báo")
    void studentWithCurriculumReceivesCompletePlannerData() throws Exception {
        // Gán chương trình đào tạo cho sv02
        Student sv02 = studentRepository.findByMa("SV002").orElseThrow();
        Curriculum program = new Curriculum();
        program.setNganh(sv02.getNganh());
        program.setKhoaNhapHoc("2024");
        program.setPhienBan("v2024.TEST");

        CurriculumGroup batBuoc = new CurriculumGroup();
        batBuoc.setChuongTrinh(program);
        batBuoc.setTen("Bắt buộc cơ sở");
        batBuoc.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);
        batBuoc.setTinChiYeuCau(6);

        Subject int101 = subjectRepository.findByMa("INT101").orElseThrow();
        Subject int201 = subjectRepository.findByMa("INT201").orElseThrow();
        batBuoc.getMonHoc().add(int101);
        batBuoc.getMonHoc().add(int201);
        program.getNhom().add(batBuoc);

        curriculumRepository.save(program);
        sv02.setChuongTrinh(program);
        studentRepository.save(sv02);

        String tokenSv02 = login("sv02", "123456");
        MvcResult res = mockMvc.perform(get("/api/sinh-vien/me/ke-hoach-mon-thieu")
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = json(res);
        assertTrue(body.get("configured").asBoolean());
        assertEquals("v2024.TEST", body.get("program").get("version").asText());
        assertEquals("2024", body.get("program").get("cohort").asText());

        // sv02 đã đạt INT101 (trong DataSeeder có lopCu INT101 điểm 7.0/8.0)
        // Nên INT101 không nằm trong missingCourses!
        JsonNode missingGroups = body.get("missingGroups");
        assertEquals(1, missingGroups.size());
        JsonNode missingCourses = missingGroups.get(0).get("missingCourses");
        assertEquals(1, missingCourses.size());
        assertEquals("INT201", missingCourses.get(0).get("subjectCode").asText());

        // Kiểm tra có cảnh báo và giới hạn dữ liệu rõ ràng
        assertTrue(body.has("graduationDelayWarnings"));
        assertTrue(body.has("limitationsAndDisclaimers"));
        assertTrue(body.get("limitationsAndDisclaimers").size() >= 3);
    }
}
