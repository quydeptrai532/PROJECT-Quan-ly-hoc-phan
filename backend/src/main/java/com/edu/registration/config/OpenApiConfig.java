package com.edu.registration.config;

import com.edu.registration.exception.GlobalExceptionHandler;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình OpenAPI 3.0 / Swagger UI cho hệ thống Quản lý Đào tạo (QLSV-101 / TASK-OP-07A).
 * Cung cấp tài liệu API toàn diện, cơ chế xác thực JWT Bearer, phân nhóm API và mô tả mã lỗi chuẩn.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ thống Quản lý Đào tạo & Đăng ký Học phần (QLSV) API")
                        .version("1.0.0")
                        .description("""
                                ## Tài liệu đặc tả API chuẩn OpenAPI 3.0 / Swagger UI
                                Hệ thống Quản lý Đào tạo và Đăng ký Học phần (QLSV).

                                ### 1. Cơ chế Xác thực & Phân quyền (Authentication & Authorization)
                                - Hầu hết các API nghiệp vụ yêu cầu xác thực bằng **JSON Web Token (JWT)**.
                                - Gửi kèm HTTP Authorization Header: `Bearer <token>`.
                                - Để lấy access token: gửi thông tin tài khoản đến `POST /api/auth/login`.
                                - Sau khi có token, nhấn nút **Authorize** (biểu tượng ổ khóa) ở góc phải trên cùng giao diện Swagger UI và dán token vào ô `Value`.
                                - Hệ thống áp dụng kiểm soát quyền nghiêm ngặt theo 3 vai trò:
                                  - `QUAN_TRI`: Quản trị viên hệ thống và Chuyên viên Phòng Đào tạo.
                                  - `GIANG_VIEN`: Giảng viên phụ trách giảng dạy và đánh giá điểm số.
                                  - `SINH_VIEN`: Sinh viên đăng ký học phần, xem thời khóa biểu, tra cứu điểm và gửi hỗ trợ.
                                - **Nguyên tắc an toàn:** Bật Swagger UI trên môi trường phát triển (dev/local/h2) **không làm giảm cấp độ bảo vệ** của các endpoint nghiệp vụ. Mọi yêu cầu gọi API nhạy cảm đều bắt buộc phải kèm JWT token có vai trò tương ứng; không mở API công khai chỉ vì Swagger UI đang kích hoạt.

                                ### 2. Chuẩn phản hồi lỗi (Standard Error Format)
                                Phản hồi lỗi nghiệp vụ và validation từ hệ thống trả về cấu trúc JSON thống nhất:
                                ```json
                                {
                                  "message": "Thông điệp mô tả lỗi chi tiết cho người dùng",
                                  "timestamp": "2026-11-10T09:00:00Z",
                                  "errors": [
                                    {
                                      "dong": 1,
                                      "cot": "email",
                                      "truong": "email",
                                      "message": "Email không hợp lệ"
                                    }
                                  ]
                                }
                                ```
                                - `400 Bad Request`: Dữ liệu gửi lên không đúng định dạng hoặc vi phạm ràng buộc validation/nghiệp vụ.
                                - `401 Unauthorized`: Chưa gửi kèm JWT Bearer token hoặc token đã hết hạn/không hợp lệ.
                                - `403 Forbidden`: Người dùng đã xác thực nhưng không có vai trò phù hợp để thực hiện thao tác.
                                - `404 Not Found`: Không tìm thấy thực thể/tài nguyên yêu cầu trong cơ sở dữ liệu.
                                - `409 Conflict`: Xung đột dữ liệu hoặc xung đột đồng thời do Optimistic Locking.
                                - `429 Too Many Requests`: Vượt quá hạn mức tần suất gọi API (Rate Limiting).

                                ### 3. Kiểm soát theo môi trường triển khai
                                - **Môi trường dev / local / h2:** Swagger UI được kích hoạt tại `/swagger-ui.html` và tài liệu JSON tại `/v3/api-docs`.
                                - **Môi trường production:** Swagger UI và OpenAPI Docs được vô hiệu hóa hoàn toàn theo chính sách an toàn thông tin. Khi truy cập vào các đường dẫn tài liệu trong môi trường sản xuất, hệ thống sẽ trả về phản hồi an toàn HTTP 404 Not Found, ngăn chặn lộ cấu trúc hệ thống.
                                """)
                        .contact(new Contact()
                                .name("Phòng Quản lý Đào tạo & Bộ phận Kỹ thuật CNTT")
                                .email("daotao@example.edu.vn"))
                        .license(new License().name("Bản quyền nội bộ cơ sở giáo dục đào tạo")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Môi trường Phát triển (Local / Dev Server)"),
                        new Server().url("https://dev-daotao.example.edu.vn").description("Môi trường Thử nghiệm (Staging / Testing)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Bearer token nhận được sau khi đăng nhập thành công qua `/api/auth/login`"))
                        .addResponses("UnauthorizedError", new ApiResponse()
                                .description("Chưa xác thực hoặc token không hợp lệ (HTTP 401)")
                                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        new MediaType().schema(new Schema<GlobalExceptionHandler.ErrorResponse>().$ref("#/components/schemas/ErrorResponse")))))
                        .addResponses("ForbiddenError", new ApiResponse()
                                .description("Không có quyền truy cập tài nguyên hoặc thao tác bị cấm (HTTP 403)")
                                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        new MediaType().schema(new Schema<GlobalExceptionHandler.ErrorResponse>().$ref("#/components/schemas/ErrorResponse")))))
                        .addResponses("NotFoundError", new ApiResponse()
                                .description("Không tìm thấy tài nguyên yêu cầu (HTTP 404)")
                                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        new MediaType().schema(new Schema<GlobalExceptionHandler.ErrorResponse>().$ref("#/components/schemas/ErrorResponse")))))
                        .addResponses("ValidationError", new ApiResponse()
                                .description("Dữ liệu đầu vào không hợp lệ hoặc vi phạm kiểm tra ràng buộc (HTTP 400)")
                                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        new MediaType().schema(new Schema<GlobalExceptionHandler.ErrorResponse>().$ref("#/components/schemas/ErrorResponse"))))));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("1-tat-ca")
                .displayName("1. Toàn bộ API hệ thống")
                .pathsToMatch("/api/**")
                .pathsToExclude("/h2-console/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authAndCommonApi() {
        return GroupedOpenApi.builder()
                .group("2-xac-thuc-dung-chung")
                .displayName("2. Xác thực & Danh mục dùng chung")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/health",
                        "/api/common/**",
                        "/api/hoc-ky",
                        "/api/phong-hoc/hoat-dong",
                        "/api/public/**",
                        "/api/notifications/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi studentApi() {
        return GroupedOpenApi.builder()
                .group("3-sinh-vien")
                .displayName("3. Phân hệ Sinh viên")
                .pathsToMatch(
                        "/api/sinh-vien/**",
                        "/api/dang-ky/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi lecturerApi() {
        return GroupedOpenApi.builder()
                .group("4-giang-vien")
                .displayName("4. Phân hệ Giảng viên")
                .pathsToMatch(
                        "/api/giang-vien/**",
                        "/api/diem/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi supportAndAttendanceApi() {
        return GroupedOpenApi.builder()
                .group("5-ho-tro-diem-danh")
                .displayName("5. Điểm danh & Hỗ trợ sinh viên")
                .pathsToMatch(
                        "/api/diem-danh/**",
                        "/api/yeu-cau-ho-tro/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("6-quan-tri")
                .displayName("6. Phân hệ Quản trị (Admin)")
                .pathsToMatch(
                        "/api/admin/**"
                )
                .build();
    }
}
