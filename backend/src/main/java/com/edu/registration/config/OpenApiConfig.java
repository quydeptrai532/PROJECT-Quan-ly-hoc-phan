package com.edu.registration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI 3.0 / Swagger UI cho hệ thống Quản lý Đào tạo (QLSV-94 / TASK-OP-07 Work Package A).
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ thống Quản lý Đào tạo & Đăng ký Học phần API")
                        .version("1.0.0")
                        .description("Tài liệu đặc tả API chuẩn OpenAPI 3.0 / Swagger cho Hệ thống Quản lý Đào tạo và Đăng ký Học phần (QLSV).\n\n"
                                + "### Xác thực & Phân quyền:\n"
                                + "- API sử dụng JSON Web Token (JWT) qua HTTP Authorization Header: `Bearer <token>`.\n"
                                + "- Đăng nhập tại `POST /api/auth/login` để nhận access token.\n"
                                + "- 3 vai trò người dùng chính: `QUAN_TRI` (Quản trị viên / Phòng đào tạo), `GIANG_VIEN` (Giảng viên), `SINH_VIEN` (Sinh viên).\n\n"
                                + "### Vận hành & Bảo mật:\n"
                                + "- Rate limiting bảo vệ điểm cuối xác thực và API nghiệp vụ.\n"
                                + "- Cache danh mục dùng chung và dữ liệu cá nhân phân vùng theo người dùng.\n"
                                + "- Giám sát sức khỏe tại `/api/health` và chỉ số vận hành tại `/api/admin/metrics`.")
                        .contact(new Contact()
                                .name("Phòng Quản lý Đào tạo & CNTT")
                                .email("daotao@university.edu.vn"))
                        .license(new License().name("Bản quyền nội bộ cơ sở giáo dục")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Bearer token nhận được sau khi đăng nhập thành công")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("qlsv-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/h2-console/**")
                .build();
    }
}
