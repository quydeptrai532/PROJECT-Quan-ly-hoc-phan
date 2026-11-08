import fs from "fs";

// 1. Vietnamese Name Pools
const LAST_NAMES = [
  "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
  "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Đoàn", "Lâm", "Trịnh",
  "Mai", "Đào", "Cao", "Hà", "Lương", "Thái", "Tạ", "Châu", "Nghiêm", "Vương"
];

const MALE_MIDDLES = [
  "Văn", "Đức", "Minh", "Quang", "Hoàng", "Tuấn", "Đình", "Hữu", "Quốc", "Ngọc",
  "Thanh", "Huy", "Gia", "Bảo", "Khắc", "Trọng", "Tiến", "Công", "Xuân", "Nhật"
];

const MALE_FIRSTS = [
  "An", "Bình", "Cường", "Dũng", "Đạt", "Đức", "Hải", "Hiếu", "Hoàng", "Hùng",
  "Huy", "Khánh", "Khoa", "Kiên", "Long", "Minh", "Nam", "Nghĩa", "Phong", "Phúc",
  "Quân", "Quang", "Sơn", "Thắng", "Thịnh", "Tiến", "Trung", "Tuấn", "Tùng", "Việt", "Vinh", "Vũ"
];

const FEMALE_MIDDLES = [
  "Thị", "Ngọc", "Thu", "Phương", "Thảo", "Khánh", "Như", "Mai", "Quỳnh", "Diệu",
  "Mỹ", "Thùy", "Kim", "Bảo", "Hoài", "Hồng", "Thúy", "Tuyết", "Trúc", "Thanh"
];

const FEMALE_FIRSTS = [
  "Anh", "Bình", "Dung", "Duyên", "Giang", "Hà", "Hân", "Hằng", "Hoa", "Hương",
  "Huyền", "Lam", "Lan", "Linh", "Mai", "My", "Nga", "Ngân", "Ngọc", "Nhung",
  "Oanh", "Phương", "Quỳnh", "Thảo", "Thu", "Thủy", "Trang", "Tuyết", "Uyên", "Vân", "Vy", "Yến"
];

function removeVietnameseTones(str) {
  str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
  str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
  str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
  str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
  str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
  str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
  str = str.replace(/đ/g, "d");
  str = str.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, "A");
  str = str.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, "E");
  str = str.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, "I");
  str = str.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, "O");
  str = str.replace(/Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, "U");
  str = str.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, "Y");
  str = str.replace(/Đ/g, "D");
  return str;
}

function generatePhone(index) {
  const prefixes = ["090", "091", "098", "097", "038", "039", "086", "077", "079", "083"];
  const prefix = prefixes[index % prefixes.length];
  const num = String(1000000 + (index * 9871) % 9000000).padStart(7, "0");
  return `${prefix}${num}`;
}

// 4 Faculties
const KHOA = [
  { id: 1, ma: "CNTT", ten: "Công nghệ thông tin" },
  { id: 2, ma: "KT", ten: "Kinh tế & Quản lý" },
  { id: 3, ma: "DT", ten: "Điện tử - Viễn thông" },
  { id: 4, ma: "NN", ten: "Ngoại ngữ ứng dụng" }
];

// 8 Majors
const NGANH = [
  { id: 1, ma: "CNPM", ten: "Kỹ thuật phần mềm", khoaId: 1 },
  { id: 2, ma: "HTTT", ten: "Hệ thống thông tin", khoaId: 1 },
  { id: 3, ma: "KHMT", ten: "Khoa học máy tính", khoaId: 1 },
  { id: 4, ma: "ATTT", ten: "An toàn thông tin", khoaId: 1 },
  { id: 5, ma: "QTKD", ten: "Quản trị kinh doanh", khoaId: 2 },
  { id: 6, ma: "TCNH", ten: "Tài chính - Ngân hàng", khoaId: 2 },
  { id: 7, ma: "DTVT", ten: "Kỹ thuật Điện tử Viễn thông", khoaId: 3 },
  { id: 8, ma: "NNA", ten: "Ngôn ngữ Anh thương mại", khoaId: 4 }
];

// 10 Admin Classes
const LOP_HANH_CHINH = [
  { id: 1, ma: "CNPM01", ten: "Kỹ thuật phần mềm 01", nganhId: 1, khoaId: 1 },
  { id: 2, ma: "CNPM02", ten: "Kỹ thuật phần mềm 02", nganhId: 1, khoaId: 1 },
  { id: 3, ma: "HTTT01", ten: "Hệ thống thông tin 01", nganhId: 2, khoaId: 1 },
  { id: 4, ma: "HTTT02", ten: "Hệ thống thông tin 02", nganhId: 2, khoaId: 1 },
  { id: 5, ma: "KHMT01", ten: "Khoa học máy tính 01", nganhId: 3, khoaId: 1 },
  { id: 6, ma: "ATTT01", ten: "An toàn thông tin 01", nganhId: 4, khoaId: 1 },
  { id: 7, ma: "QTKD01", ten: "Quản trị kinh doanh 01", nganhId: 5, khoaId: 2 },
  { id: 8, ma: "TCNH01", ten: "Tài chính - Ngân hàng 01", nganhId: 6, khoaId: 2 },
  { id: 9, ma: "DTVT01", ten: "Điện tử Viễn thông 01", nganhId: 7, khoaId: 3 },
  { id: 10, ma: "NNA01", ten: "Ngôn ngữ Anh 01", nganhId: 8, khoaId: 4 }
];

// 50 Subjects across 8 semesters
const SUBJECTS = [
  // Semester 1
  { id: 1, ma: "INT101", ten: "Lập trình cơ bản", tc: 3, khoaId: 1, ky: 1, tienQuyetMa: [] },
  { id: 2, ma: "MAT101", ten: "Toán rời rạc", tc: 3, khoaId: 1, ky: 1, tienQuyetMa: [] },
  { id: 3, ma: "MAT102", ten: "Giải tích 1", tc: 3, khoaId: 1, ky: 1, tienQuyetMa: [] },
  { id: 4, ma: "PHI101", ten: "Triết học Mác - Lênin", tc: 3, khoaId: 2, ky: 1, tienQuyetMa: [] },
  { id: 5, ma: "ENG101", ten: "Tiếng Anh đại cương 1", tc: 2, khoaId: 4, ky: 1, tienQuyetMa: [] },
  { id: 6, ma: "GEN101", ten: "Pháp luật đại cương", tc: 2, khoaId: 2, ky: 1, tienQuyetMa: [] },

  // Semester 2
  { id: 7, ma: "INT201", ten: "Cấu trúc dữ liệu và giải thuật", tc: 3, khoaId: 1, ky: 2, tienQuyetMa: ["INT101"] },
  { id: 8, ma: "INT102", ten: "Lập trình hướng đối tượng", tc: 3, khoaId: 1, ky: 2, tienQuyetMa: ["INT101"] },
  { id: 9, ma: "MAT103", ten: "Giải tích 2", tc: 3, khoaId: 1, ky: 2, tienQuyetMa: ["MAT102"] },
  { id: 10, ma: "PHY101", ten: "Vật lý đại cương", tc: 3, khoaId: 3, ky: 2, tienQuyetMa: [] },
  { id: 11, ma: "PHI102", ten: "Kinh tế chính trị Mác - Lênin", tc: 2, khoaId: 2, ky: 2, tienQuyetMa: ["PHI101"] },
  { id: 12, ma: "ENG102", ten: "Tiếng Anh đại cương 2", tc: 2, khoaId: 4, ky: 2, tienQuyetMa: ["ENG101"] },

  // Semester 3
  { id: 13, ma: "INT301", ten: "Cơ sở dữ liệu", tc: 3, khoaId: 1, ky: 3, tienQuyetMa: ["INT201"] },
  { id: 14, ma: "INT202", ten: "Kiến trúc máy tính", tc: 3, khoaId: 1, ky: 3, tienQuyetMa: [] },
  { id: 15, ma: "INT203", ten: "Lập trình ứng dụng Java", tc: 3, khoaId: 1, ky: 3, tienQuyetMa: ["INT102"] },
  { id: 16, ma: "MAT201", ten: "Xác suất thống kê", tc: 3, khoaId: 1, ky: 3, tienQuyetMa: [] },
  { id: 17, ma: "ECO101", ten: "Kinh tế vi mô", tc: 3, khoaId: 2, ky: 3, tienQuyetMa: [] },
  { id: 18, ma: "PHI103", ten: "Chủ nghĩa xã hội khoa học", tc: 2, khoaId: 2, ky: 3, tienQuyetMa: [] },

  // Semester 4
  { id: 19, ma: "INT302", ten: "Lập trình web", tc: 3, khoaId: 1, ky: 4, tienQuyetMa: ["INT301"] },
  { id: 20, ma: "INT303", ten: "Mạng máy tính", tc: 3, khoaId: 1, ky: 4, tienQuyetMa: [] },
  { id: 21, ma: "INT204", ten: "Hệ điều hành", tc: 3, khoaId: 1, ky: 4, tienQuyetMa: ["INT202"] },
  { id: 22, ma: "ELE201", ten: "Kỹ thuật số và vi xử lý", tc: 3, khoaId: 3, ky: 4, tienQuyetMa: ["PHY101"] },
  { id: 23, ma: "ECO102", ten: "Kinh tế vĩ mô", tc: 3, khoaId: 2, ky: 4, tienQuyetMa: ["ECO101"] },
  { id: 24, ma: "PHI104", ten: "Lịch sử Đảng Cộng sản VN", tc: 2, khoaId: 2, ky: 4, tienQuyetMa: [] },

  // Semester 5
  { id: 25, ma: "INT304", ten: "Trí tuệ nhân tạo", tc: 3, khoaId: 1, ky: 5, tienQuyetMa: ["INT201"] },
  { id: 26, ma: "INT305", ten: "Hệ quản trị cơ sở dữ liệu", tc: 3, khoaId: 1, ky: 5, tienQuyetMa: ["INT301"] },
  { id: 27, ma: "INT306", ten: "Phân tích thiết kế hệ thống", tc: 3, khoaId: 1, ky: 5, tienQuyetMa: ["INT301"] },
  { id: 28, ma: "INT307", ten: "Phát triển ứng dụng Web nâng cao", tc: 3, khoaId: 1, ky: 5, tienQuyetMa: ["INT302"] },
  { id: 29, ma: "SEC301", ten: "An toàn thông tin căn bản", tc: 3, khoaId: 1, ky: 5, tienQuyetMa: ["INT303"] },
  { id: 30, ma: "PHI105", ten: "Tư tưởng Hồ Chí Minh", tc: 2, khoaId: 2, ky: 5, tienQuyetMa: [] },

  // Semester 6
  { id: 31, ma: "INT401", ten: "Công nghệ phần mềm", tc: 3, khoaId: 1, ky: 6, tienQuyetMa: ["INT306"] },
  { id: 32, ma: "INT402", ten: "Lập trình thiết bị di động", tc: 3, khoaId: 1, ky: 6, tienQuyetMa: ["INT203"] },
  { id: 33, ma: "INT403", ten: "Học máy cơ sở", tc: 3, khoaId: 1, ky: 6, tienQuyetMa: ["INT304"] },
  { id: 34, ma: "CLD301", ten: "Điện toán đám mây", tc: 3, khoaId: 1, ky: 6, tienQuyetMa: ["INT303"] },
  { id: 35, ma: "SEC302", ten: "An ninh mạng và mật mã học", tc: 3, khoaId: 1, ky: 6, tienQuyetMa: ["SEC301"] },
  { id: 36, ma: "MGT301", ten: "Quản trị học đại cương", tc: 3, khoaId: 2, ky: 6, tienQuyetMa: [] },
  { id: 37, ma: "ENG201", ten: "Tiếng Anh giao tiếp chuyên nghiệp", tc: 2, khoaId: 4, ky: 6, tienQuyetMa: ["ENG102"] },

  // Semester 7
  { id: 38, ma: "INT404", ten: "Kiểm thử phần mềm & QA", tc: 3, khoaId: 1, ky: 7, tienQuyetMa: ["INT401"] },
  { id: 39, ma: "INT405", ten: "Phân tích dữ liệu lớn", tc: 3, khoaId: 1, ky: 7, tienQuyetMa: ["INT305"] },
  { id: 40, ma: "INT406", ten: "Thị giác máy tính và Xử lý ảnh", tc: 3, khoaId: 1, ky: 7, tienQuyetMa: ["INT403"] },
  { id: 41, ma: "INT407", ten: "Xử lý ngôn ngữ tự nhiên", tc: 3, khoaId: 1, ky: 7, tienQuyetMa: ["INT403"] },
  { id: 42, ma: "INT408", ten: "DevOps & Tự động hóa CI/CD", tc: 3, khoaId: 1, ky: 7, tienQuyetMa: ["CLD301"] },
  { id: 43, ma: "MGT401", ten: "Quản lý dự án công nghệ thông tin", tc: 3, khoaId: 2, ky: 7, tienQuyetMa: ["MGT301"] },
  { id: 44, ma: "FIN301", ten: "Tài chính doanh nghiệp", tc: 3, khoaId: 2, ky: 7, tienQuyetMa: [] },

  // Semester 8
  { id: 45, ma: "INT490", ten: "Chuyên đề công nghệ mới", tc: 3, khoaId: 1, ky: 8, tienQuyetMa: [] },
  { id: 46, ma: "INT493", ten: "Kiến trúc microservices", tc: 3, khoaId: 1, ky: 8, tienQuyetMa: ["INT307"] },
  { id: 47, ma: "INT494", ten: "Đạo đức nghề nghiệp trong CNTT", tc: 2, khoaId: 1, ky: 8, tienQuyetMa: [] },
  { id: 48, ma: "MGT490", ten: "Khởi nghiệp và Đổi mới sáng tạo", tc: 2, khoaId: 2, ky: 8, tienQuyetMa: [] },
  { id: 49, ma: "PRJ491", ten: "Thực tập tốt nghiệp doanh nghiệp", tc: 4, khoaId: 1, ky: 8, tienQuyetMa: [] },
  { id: 50, ma: "PRJ492", ten: "Khóa luận tốt nghiệp", tc: 6, khoaId: 1, ky: 8, tienQuyetMa: [] }
];

// Hash constants
const BCRYPT_123456 = "$2a$10$1xgpvc/M47l7To9vFcZ3XuytRqcvzPl1aFRZuOU1iaglI88sGmUeS";
const BCRYPT_ADMIN123 = "$2a$10$QHbVBActe/ujXo35qnD.4u1HLtq9p6PfLN0BqndNrvAeEjkJUamFu";

// Fixed 5 students
const fixedStudents = [
  { username: "sv01", ma: "SV001", hoTen: "Trần Minh Anh", email: "anh.tm@student.edu.vn", khoaId: 1, nganhId: 1, lopId: 1 },
  { username: "sv02", ma: "SV002", hoTen: "Phạm Thu Hà", email: "ha.pt@student.edu.vn", khoaId: 1, nganhId: 1, lopId: 1 },
  { username: "sv03", ma: "SV003", hoTen: "Đỗ Quang Huy", email: "huy.dq@student.edu.vn", khoaId: 1, nganhId: 1, lopId: 2 },
  { username: "sv04", ma: "SV004", hoTen: "Vũ Ngọc Lan", email: "lan.vn@student.edu.vn", khoaId: 1, nganhId: 2, lopId: 3 },
  { username: "sv05", ma: "SV005", hoTen: "Hoàng Gia Bảo", email: "bao.hg@student.edu.vn", khoaId: 1, nganhId: 2, lopId: 3 }
];

// Fixed 3 lecturers
const fixedLecturers = [
  { username: "gv01", ma: "GV01", hoTen: "TS. Nguyễn Văn An", email: "an.nv@university.edu.vn", khoaId: 1 },
  { username: "gv02", ma: "GV02", hoTen: "ThS. Trần Thị Bình", email: "binh.tt@university.edu.vn", khoaId: 1 },
  { username: "gv03", ma: "GV03", hoTen: "ThS. Lê Hoàng Cường", email: "cuong.lh@university.edu.vn", khoaId: 2 }
];

// 50 Lecturers
const lecturers = [...fixedLecturers];
const ACADEMIC_TITLES = ["PGS.TS.", "TS.", "ThS.", "TS.", "ThS.", "PGS.TS.", "TS.", "ThS."];
for (let i = 4; i <= 50; i++) {
  const username = `gv${String(i).padStart(2, "0")}`;
  const ma = `GV${String(i).padStart(2, "0")}`;
  const isMale = (i % 3 !== 0);
  const ho = LAST_NAMES[(i * 7 + 3) % LAST_NAMES.length];
  const dem = isMale
    ? MALE_MIDDLES[(i * 5 + 2) % MALE_MIDDLES.length]
    : FEMALE_MIDDLES[(i * 3 + 1) % FEMALE_MIDDLES.length];
  const ten = isMale
    ? MALE_FIRSTS[(i * 11 + 4) % MALE_FIRSTS.length]
    : FEMALE_FIRSTS[(i * 13 + 5) % FEMALE_FIRSTS.length];

  const title = ACADEMIC_TITLES[i % ACADEMIC_TITLES.length];
  const hoTen = `${title} ${ho} ${dem} ${ten}`;
  const cleanTen = removeVietnameseTones(ten).toLowerCase();
  const cleanHoDem = removeVietnameseTones(ho.charAt(0) + dem.charAt(0)).toLowerCase();
  const email = `${cleanTen}.${cleanHoDem}${i}@university.edu.vn`;
  const khoaId = ((i - 1) % 4) + 1;

  lecturers.push({ username, ma, hoTen, email, khoaId });
}

// 500 Students
const students = [...fixedStudents];
for (let i = 6; i <= 500; i++) {
  const username = `sv${String(i).padStart(2, "0")}`;
  const ma = `SV${String(i).padStart(3, "0")}`;
  const isMale = (i % 2 === 0);
  const ho = LAST_NAMES[(i * 13 + 5) % LAST_NAMES.length];
  const dem = isMale
    ? MALE_MIDDLES[(i * 17 + 7) % MALE_MIDDLES.length]
    : FEMALE_MIDDLES[(i * 19 + 3) % FEMALE_MIDDLES.length];
  const ten = isMale
    ? MALE_FIRSTS[(i * 23 + 11) % MALE_FIRSTS.length]
    : FEMALE_FIRSTS[(i * 29 + 13) % FEMALE_FIRSTS.length];

  const hoTen = `${ho} ${dem} ${ten}`;
  const cleanTen = removeVietnameseTones(ten).toLowerCase();
  const cleanHoDem = removeVietnameseTones(ho.charAt(0) + dem.charAt(0)).toLowerCase();
  const email = `${cleanTen}.${cleanHoDem}${i}@student.edu.vn`;

  const lopObj = LOP_HANH_CHINH[(i - 1) % LOP_HANH_CHINH.length];
  students.push({
    username,
    ma,
    hoTen,
    email,
    khoaId: lopObj.khoaId,
    nganhId: lopObj.nganhId,
    lopId: lopObj.id
  });
}

// Build SQL
let sql = `-- ====================================================================
-- SEED DATA DATABASE QUAN_LY_DAO_TAO
-- Tạo bởi Hệ Thống Quản Lý Đào Tạo
-- 500 Sinh viên, 50 Giảng viên, 50 Môn học chia đều 8 kỳ
-- Điểm GPA đầy đủ: Xuất sắc, Giỏi, Khá, Trung bình, Yếu / Cần học lại
-- ====================================================================

CREATE DATABASE IF NOT EXISTS quan_ly_dao_tao CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quan_ly_dao_tao;

SET FOREIGN_KEY_CHECKS = 0;

-- TẠO CÁC BẢNG DDL NẾU CHƯA CÓ
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    trang_thai BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS khoa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nganh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL,
    khoa_id BIGINT NOT NULL,
    CONSTRAINT fk_nganh_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lop_hanh_chinh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL,
    nganh_id BIGINT NOT NULL,
    CONSTRAINT fk_lop_nganh FOREIGN KEY (nganh_id) REFERENCES nganh(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hoc_ky (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ten VARCHAR(100) NOT NULL,
    nam_hoc VARCHAR(20) NOT NULL,
    ngay_bat_dau DATE NOT NULL,
    ngay_ket_thuc DATE NOT NULL,
    dang_ky_tu DATE NOT NULL,
    dang_ky_den DATE NOT NULL,
    so_tin_chi_toi_thieu INT NOT NULL DEFAULT 0,
    so_tin_chi_toi_da INT NOT NULL DEFAULT 24
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mon_hoc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL,
    so_tin_chi INT NOT NULL,
    khoa_id BIGINT NOT NULL,
    CONSTRAINT fk_mon_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mon_tien_quyet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mon_hoc_id BIGINT NOT NULL,
    mon_tien_quyet_id BIGINT NOT NULL,
    UNIQUE KEY uk_mon_tien_quyet (mon_hoc_id, mon_tien_quyet_id),
    CONSTRAINT fk_mtq_mon FOREIGN KEY (mon_hoc_id) REFERENCES mon_hoc(id),
    CONSTRAINT fk_mtq_tienquyet FOREIGN KEY (mon_tien_quyet_id) REFERENCES mon_hoc(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS giang_vien (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ho_ten VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    so_dien_thoai VARCHAR(20),
    khoa_id BIGINT,
    CONSTRAINT fk_gv_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_gv_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sinh_vien (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    ma VARCHAR(20) NOT NULL UNIQUE,
    ho_ten VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    so_dien_thoai VARCHAR(20),
    khoa_id BIGINT,
    nganh_id BIGINT,
    lop_id BIGINT,
    chuong_trinh_id BIGINT,
    CONSTRAINT fk_sv_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_sv_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id),
    CONSTRAINT fk_sv_nganh FOREIGN KEY (nganh_id) REFERENCES nganh(id),
    CONSTRAINT fk_sv_lop FOREIGN KEY (lop_id) REFERENCES lop_hanh_chinh(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lop_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(30) NOT NULL UNIQUE,
    mon_hoc_id BIGINT NOT NULL,
    hoc_ky_id BIGINT NOT NULL,
    giang_vien_id BIGINT NOT NULL,
    phong VARCHAR(20) NOT NULL,
    thu INT NOT NULL,
    tiet_bat_dau INT NOT NULL,
    so_tiet INT NOT NULL,
    si_so_toi_da INT NOT NULL,
    si_so_hien_tai INT NOT NULL DEFAULT 0,
    trang_thai_bang_diem VARCHAR(24),
    phien_ban_cong_bo INT NOT NULL DEFAULT 1,
    gui_duyet_bang_diem_luc DATETIME(6),
    duyet_bang_diem_luc DATETIME(6),
    nguoi_duyet_bang_diem_id BIGINT,
    cong_bo_bang_diem_luc DATETIME(6),
    nguoi_cong_bo_bang_diem_id BIGINT,
    nguong_so_buoi_vang INT,
    nguong_ty_le_vang DOUBLE,
    CONSTRAINT fk_lhp_mon FOREIGN KEY (mon_hoc_id) REFERENCES mon_hoc(id),
    CONSTRAINT fk_lhp_hk FOREIGN KEY (hoc_ky_id) REFERENCES hoc_ky(id),
    CONSTRAINT fk_lhp_gv FOREIGN KEY (giang_vien_id) REFERENCES giang_vien(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dang_ky_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    lop_hoc_phan_id BIGINT NOT NULL,
    thoi_gian_dang_ky DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    trang_thai VARCHAR(20) NOT NULL,
    UNIQUE KEY uk_sv_lhp (sinh_vien_id, lop_hoc_phan_id),
    CONSTRAINT fk_dkhp_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id),
    CONSTRAINT fk_dkhp_lhp FOREIGN KEY (lop_hoc_phan_id) REFERENCES lop_hoc_phan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diem (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dang_ky_id BIGINT NOT NULL UNIQUE,
    diem_qua_trinh DOUBLE,
    diem_cuoi_ky DOUBLE,
    diem_tong_ket DOUBLE,
    cap_nhat_luc DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_diem_dangky FOREIGN KEY (dang_ky_id) REFERENCES dang_ky_hoc_phan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Làm sạch dữ liệu rác cũ nếu có
DELETE FROM diem;
DELETE FROM dang_ky_hoc_phan;
DELETE FROM lop_hoc_phan;
DELETE FROM mon_tien_quyet;
DELETE FROM mon_hoc;
DELETE FROM sinh_vien;
DELETE FROM giang_vien;
DELETE FROM lop_hanh_chinh;
DELETE FROM nganh;
DELETE FROM khoa;
DELETE FROM hoc_ky;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. BẢNG KHOA
INSERT INTO khoa (id, ma, ten) VALUES
(1, 'CNTT', 'Công nghệ thông tin'),
(2, 'KT', 'Kinh tế & Quản lý'),
(3, 'DT', 'Điện tử - Viễn thông'),
(4, 'NN', 'Ngoại ngữ ứng dụng');

-- 2. BẢNG NGANH
INSERT INTO nganh (id, ma, ten, khoa_id) VALUES
(1, 'CNPM', 'Kỹ thuật phần mềm', 1),
(2, 'HTTT', 'Hệ thống thông tin', 1),
(3, 'KHMT', 'Khoa học máy tính', 1),
(4, 'ATTT', 'An toàn thông tin', 1),
(5, 'QTKD', 'Quản trị kinh doanh', 2),
(6, 'TCNH', 'Tài chính - Ngân hàng', 2),
(7, 'DTVT', 'Kỹ thuật Điện tử Viễn thông', 3),
(8, 'NNA', 'Ngôn ngữ Anh thương mại', 4);

-- 3. BẢNG LOP_HANH_CHINH
INSERT INTO lop_hanh_chinh (id, ma, ten, nganh_id) VALUES
(1, 'CNPM01', 'Kỹ thuật phần mềm 01', 1),
(2, 'CNPM02', 'Kỹ thuật phần mềm 02', 1),
(3, 'HTTT01', 'Hệ thống thông tin 01', 2),
(4, 'HTTT02', 'Hệ thống thông tin 02', 2),
(5, 'KHMT01', 'Khoa học máy tính 01', 3),
(6, 'ATTT01', 'An toàn thông tin 01', 4),
(7, 'QTKD01', 'Quản trị kinh doanh 01', 5),
(8, 'TCNH01', 'Tài chính - Ngân hàng 01', 6),
(9, 'DTVT01', 'Điện tử Viễn thông 01', 7),
(10, 'NNA01', 'Ngôn ngữ Anh 01', 8);

-- 4. BẢNG HOC_KY
INSERT INTO hoc_ky (id, ten, nam_hoc, ngay_bat_dau, ngay_ket_thuc, dang_ky_tu, dang_ky_den, so_tin_chi_toi_thieu, so_tin_chi_toi_da) VALUES
(1, 'Học kỳ 1', '2025-2026', '2025-09-05', '2026-01-20', '2025-08-15', '2025-09-25', 12, 24),
(2, 'Học kỳ 2', '2025-2026', '2026-02-15', '2026-06-30', '2026-01-20', '2026-03-05', 12, 24),
(3, 'Học kỳ 1', '2026-2027', DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 12, 24),
(4, 'Học kỳ 2', '2024-2025', '2025-02-15', '2025-06-30', '2025-01-20', '2025-03-05', 12, 24),
(5, 'Học kỳ 1', '2024-2025', '2024-09-05', '2025-01-20', '2024-08-15', '2024-09-25', 12, 24);

-- 5. BẢNG MON_HOC (50 môn học chia đều 8 kỳ)
INSERT INTO mon_hoc (id, ma, ten, so_tin_chi, khoa_id) VALUES
`;

sql += SUBJECTS.map(s => `(${s.id}, '${s.ma}', '${s.ten}', ${s.tc}, ${s.khoaId})`).join(",\n") + ";\n\n";

// Prerequisites
sql += `-- 6. BẢNG MON_TIEN_QUYET\nINSERT INTO mon_tien_quyet (mon_hoc_id, mon_tien_quyet_id) VALUES\n`;
const prqList = [];
SUBJECTS.forEach(s => {
  s.tienQuyetMa.forEach(tqMa => {
    const tq = SUBJECTS.find(sub => sub.ma === tqMa);
    if (tq) {
      prqList.push(`(${s.id}, ${tq.id})`);
    }
  });
});
sql += prqList.join(",\n") + ";\n\n";

// USERS:
// 1 admin (id: 1)
// 50 lecturers (id: 2..51)
// 500 students (id: 52..551)
sql += `-- 7. BẢNG USERS (1 Admin, 50 Giảng viên, 500 Sinh viên)\nINSERT INTO users (id, username, password, role, trang_thai, created_at) VALUES\n`;
const userRows = [];
userRows.push(`(1, 'admin', '${BCRYPT_ADMIN123}', 'QUAN_TRI', 1, NOW())`);

lecturers.forEach((gv, idx) => {
  userRows.push(`(${idx + 2}, '${gv.username}', '${BCRYPT_123456}', 'GIANG_VIEN', 1, NOW())`);
});

students.forEach((sv, idx) => {
  userRows.push(`(${idx + 52}, '${sv.username}', '${BCRYPT_123456}', 'SINH_VIEN', 1, NOW())`);
});
sql += userRows.join(",\n") + ";\n\n";

// GIANG_VIEN
sql += `-- 8. BẢNG GIANG_VIEN (50 Giảng viên với học hàm TS, PGS.TS, ThS và tên người thật)\nINSERT INTO giang_vien (id, user_id, ma, ho_ten, email, so_dien_thoai, khoa_id) VALUES\n`;
const gvRows = lecturers.map((gv, idx) => {
  const phone = generatePhone(idx + 1);
  return `(${idx + 1}, ${idx + 2}, '${gv.ma}', '${gv.hoTen}', '${gv.email}', '${phone}', ${gv.khoaId})`;
});
sql += gvRows.join(",\n") + ";\n\n";

// SINH_VIEN
sql += `-- 9. BẢNG SINH_VIEN (500 Sinh viên với tên người Việt Nam thật, email, SĐT chuẩn)\nINSERT INTO sinh_vien (id, user_id, ma, ho_ten, email, so_dien_thoai, khoa_id, nganh_id, lop_id) VALUES\n`;
const svRows = students.map((sv, idx) => {
  const phone = generatePhone(idx + 100);
  return `(${idx + 1}, ${idx + 52}, '${sv.ma}', '${sv.hoTen}', '${sv.email}', '${phone}', ${sv.khoaId}, ${sv.nganhId}, ${sv.lopId})`;
});
sql += svRows.join(",\n") + ";\n\n";

// LOP_HOC_PHAN
const CLASSES_HP = [
  // Past Semester 1 (HK1 2025-2026 - id 1)
  { id: 1, ma: "MAT101.01-HK1", monId: 2, hkId: 1, gvId: 2, phong: "A105", thu: 4, tiet: 7, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 2, ma: "ECO101.01-HK1", monId: 17, hkId: 1, gvId: 3, phong: "C301", thu: 3, tiet: 4, soTiet: 3, siSoMax: 60, trangThai: "DA_CONG_BO" },
  { id: 3, ma: "INT101.01-HK1", monId: 1, hkId: 1, gvId: 1, phong: "A101", thu: 2, tiet: 1, soTiet: 3, siSoMax: 60, trangThai: "DA_CONG_BO" },
  { id: 4, ma: "MAT102.01-HK1", monId: 3, hkId: 1, gvId: 4, phong: "A102", thu: 5, tiet: 1, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 5, ma: "PHI101.01-HK1", monId: 4, hkId: 1, gvId: 5, phong: "C302", thu: 6, tiet: 4, soTiet: 3, siSoMax: 60, trangThai: "DA_CONG_BO" },
  { id: 6, ma: "ENG101.01-HK1", monId: 5, hkId: 1, gvId: 6, phong: "B201", thu: 3, tiet: 7, soTiet: 2, siSoMax: 45, trangThai: "DA_CONG_BO" },
  { id: 7, ma: "GEN101.01-HK1", monId: 6, hkId: 1, gvId: 7, phong: "C303", thu: 7, tiet: 1, soTiet: 2, siSoMax: 60, trangThai: "DA_CONG_BO" },

  // Past Semester 2 (HK2 2025-2026 - id 2)
  { id: 8, ma: "INT101.01-HK2", monId: 1, hkId: 2, gvId: 1, phong: "A101", thu: 2, tiet: 1, soTiet: 3, siSoMax: 60, trangThai: "DA_CONG_BO" },
  { id: 9, ma: "INT201.01-HK2", monId: 7, hkId: 2, gvId: 8, phong: "A102", thu: 3, tiet: 1, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 10, ma: "INT102.01-HK2", monId: 8, hkId: 2, gvId: 9, phong: "A103", thu: 4, tiet: 1, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 11, ma: "MAT103.01-HK2", monId: 9, hkId: 2, gvId: 10, phong: "A104", thu: 5, tiet: 4, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 12, ma: "PHY101.01-HK2", monId: 10, hkId: 2, gvId: 11, phong: "D401", thu: 6, tiet: 1, soTiet: 3, siSoMax: 50, trangThai: "DA_CONG_BO" },
  { id: 13, ma: "PHI102.01-HK2", monId: 11, hkId: 2, gvId: 12, phong: "C301", thu: 2, tiet: 7, soTiet: 2, siSoMax: 60, trangThai: "DA_CONG_BO" },
  { id: 14, ma: "ENG102.01-HK2", monId: 12, hkId: 2, gvId: 13, phong: "B202", thu: 4, tiet: 7, soTiet: 2, siSoMax: 45, trangThai: "DA_CONG_BO" },

  // Current Semester (HK1 2026-2027 - id 3 - đang mở đăng ký)
  { id: 15, ma: "INT201.01", monId: 7, hkId: 3, gvId: 1, phong: "A101", thu: 2, tiet: 1, soTiet: 3, siSoMax: 45, trangThai: "NHAP" },
  { id: 16, ma: "INT201.02", monId: 7, hkId: 3, gvId: 2, phong: "A102", thu: 4, tiet: 4, soTiet: 3, siSoMax: 45, trangThai: "NHAP" },
  { id: 17, ma: "INT301.01", monId: 13, hkId: 3, gvId: 1, phong: "B201", thu: 3, tiet: 1, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 18, ma: "INT302.01", monId: 19, hkId: 3, gvId: 2, phong: "A103", thu: 5, tiet: 1, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 19, ma: "INT303.01", monId: 20, hkId: 3, gvId: 1, phong: "B202", thu: 6, tiet: 4, soTiet: 3, siSoMax: 50, trangThai: "NHAP" },
  { id: 20, ma: "INT304.01", monId: 25, hkId: 3, gvId: 2, phong: "A104", thu: 7, tiet: 1, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 21, ma: "ECO101.01", monId: 17, hkId: 3, gvId: 3, phong: "C301", thu: 3, tiet: 4, soTiet: 3, siSoMax: 55, trangThai: "NHAP" },
  { id: 22, ma: "MAT101.01", monId: 2, hkId: 3, gvId: 2, phong: "A105", thu: 4, tiet: 7, soTiet: 3, siSoMax: 50, trangThai: "NHAP" },

  // Additional active semester 3 classes taught by various lecturers
  { id: 23, ma: "INT202.01", monId: 14, hkId: 3, gvId: 14, phong: "B203", thu: 2, tiet: 4, soTiet: 3, siSoMax: 45, trangThai: "NHAP" },
  { id: 24, ma: "INT203.01", monId: 15, hkId: 3, gvId: 15, phong: "Lab01", thu: 5, tiet: 7, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 25, ma: "MAT201.01", monId: 16, hkId: 3, gvId: 16, phong: "A106", thu: 6, tiet: 1, soTiet: 3, siSoMax: 45, trangThai: "NHAP" },
  { id: 26, ma: "SEC301.01", monId: 29, hkId: 3, gvId: 17, phong: "Lab02", thu: 7, tiet: 4, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 27, ma: "INT401.01", monId: 31, hkId: 3, gvId: 18, phong: "B204", thu: 2, tiet: 7, soTiet: 3, siSoMax: 45, trangThai: "NHAP" },
  { id: 28, ma: "INT403.01", monId: 33, hkId: 3, gvId: 19, phong: "A107", thu: 3, tiet: 7, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 29, ma: "CLD301.01", monId: 34, hkId: 3, gvId: 20, phong: "B205", thu: 4, tiet: 1, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 30, ma: "INT404.01", monId: 38, hkId: 3, gvId: 21, phong: "A108", thu: 5, tiet: 4, soTiet: 3, siSoMax: 40, trangThai: "NHAP" },
  { id: 31, ma: "INT408.01", monId: 42, hkId: 3, gvId: 22, phong: "Lab03", thu: 6, tiet: 7, soTiet: 3, siSoMax: 40, trangThai: "NHAP" }
];

// Distribute remaining lecturers into classes
for (let i = 32; i <= 60; i++) {
  const subIdx = (i * 3) % SUBJECTS.length;
  const sub = SUBJECTS[subIdx];
  const gvId = ((i - 1) % 50) + 1;
  const phong = `P${String(100 + (i % 25)).padStart(3, "0")}`;
  const thu = (i % 6) + 2;
  const tiet = (i % 3) * 3 + 1;
  const section = Math.floor((i - 32) / 2) + 10;
  CLASSES_HP.push({
    id: i,
    ma: `${sub.ma}.${String(section).padStart(2, "0")}`,
    monId: sub.id,
    hkId: 3,
    gvId: gvId,
    phong: phong,
    thu: thu,
    tiet: tiet,
    soTiet: 3,
    siSoMax: 50,
    trangThai: "NHAP"
  });
}

// Enrollments & Grades
const ENROLLMENTS = [];
const GRADES = [];
const enrolledSet = new Set();
let enrollId = 1;

function addGrade(svId, classId, quaTrinh, cuoiKy) {
  const key = `${svId}_${classId}`;
  if (enrolledSet.has(key)) return;
  enrolledSet.add(key);
  const tongKet = Math.round((0.4 * quaTrinh + 0.6 * cuoiKy) * 10) / 10;
  ENROLLMENTS.push({ id: enrollId, sinhVienId: svId, lopId: classId, trangThai: "DA_DANG_KY" });
  GRADES.push({ id: enrollId, dangKyId: enrollId, qt: quaTrinh, ck: cuoiKy, tk: tongKet });
  enrollId++;
}

function addCurrentEnroll(svId, classId) {
  const key = `${svId}_${classId}`;
  if (enrolledSet.has(key)) return;
  enrolledSet.add(key);
  ENROLLMENTS.push({ id: enrollId, sinhVienId: svId, lopId: classId, trangThai: "DA_DANG_KY" });
  enrollId++;
}

// 1. Fixed enrollments for sv01..sv05 for test compatibility:
// sv01
addGrade(1, 8, 8.5, 9.0); // lop INT101.01-HK2 (tk: 8.8)
addGrade(1, 1, 7.0, 8.0); // lop MAT101.01-HK1 (tk: 7.6)
addGrade(1, 2, 6.5, 7.0); // lop ECO101.01-HK1 (tk: 6.8)
addCurrentEnroll(1, 19);  // INT303.01
addCurrentEnroll(1, 15);  // INT201.01
addCurrentEnroll(1, 22);  // MAT101.01
addCurrentEnroll(1, 21);  // ECO101.01

// sv02 (no current enrollments to allow E2E registration tests)
addGrade(2, 8, 7.0, 8.0); // INT101.01-HK2 (tk: 7.6)
addGrade(2, 1, 8.0, 8.5); // MAT101.01-HK1 (tk: 8.3)

// sv03
addGrade(3, 8, 3.5, 4.0); // INT101.01-HK2 (tk: 3.8 - FAIL / HỌC LẠI)
addGrade(3, 2, 5.0, 6.0); // ECO101.01-HK1 (tk: 5.6)
addCurrentEnroll(3, 19);  // INT303.01
addCurrentEnroll(3, 22);  // MAT101.01
addCurrentEnroll(3, 21);  // ECO101.01

// sv04 (no announced grades in tests, has current enrollments)
addCurrentEnroll(4, 19);
addCurrentEnroll(4, 22);
addCurrentEnroll(4, 21);

// sv05
addCurrentEnroll(5, 19);
addCurrentEnroll(5, 22);
addCurrentEnroll(5, 21);

// 2. Fixed enrollments for sv06..sv20 matching previous seed:
for (let i = 6; i <= 20; i++) {
  const svId = i;
  const qt = 6.5 + (i % 3);
  const ck = 7.0 + (i % 2);
  addGrade(svId, 8, qt, ck); // lop INT101.01-HK2
  addCurrentEnroll(svId, i % 2 === 0 ? 16 : 15); // INT201.02 / INT201.01
  addCurrentEnroll(svId, 19); // INT303.01
  addCurrentEnroll(svId, 22); // MAT101.01
  addCurrentEnroll(svId, 21); // ECO101.01
}

// 3. Realistic GPA distribution for sv21..sv500:
for (let i = 21; i <= 500; i++) {
  const svId = i;
  const tier = i % 100;
  let baseScore = 7.0;

  if (tier < 10) {
    baseScore = 8.8 + ((i * 3) % 10) * 0.1;
  } else if (tier < 35) {
    baseScore = 8.0 + ((i * 7) % 8) * 0.1;
  } else if (tier < 75) {
    baseScore = 6.5 + ((i * 11) % 15) * 0.1;
  } else if (tier < 90) {
    baseScore = 5.0 + ((i * 5) % 14) * 0.1;
  } else {
    // Yếu / Học lại: điểm dưới 5.0
    baseScore = 3.2 + ((i * 9) % 16) * 0.1;
  }

  // Assign 2 past courses with grades
  const pastClasses = [
    1 + (i % 7), // 1..7 (HK1 2025-2026)
    8 + (i % 7)  // 8..14 (HK2 2025-2026)
  ];

  pastClasses.forEach(cId => {
    let qt = Math.min(10, Math.max(1.0, Math.round((baseScore + ((i % 5) - 2) * 0.4) * 10) / 10));
    let ck = Math.min(10, Math.max(1.0, Math.round((baseScore + ((i % 7) - 3) * 0.3) * 10) / 10));
    addGrade(svId, cId, qt, ck);
  });

  // Current semester enrollments (use classes 23..60, avoiding test classes 15, 16, 17, 18, 19, 21, 22)
  const currentClassId = 23 + (i % 38);
  addCurrentEnroll(svId, currentClassId);
  if (i % 3 === 0) {
    const curClass2 = 23 + ((i + 7) % 38);
    if (curClass2 !== currentClassId) {
      addCurrentEnroll(svId, curClass2);
    }
  }
}

// Calculate actual siSoHienTai for each class
const classCountMap = {};
ENROLLMENTS.forEach(e => {
  classCountMap[e.lopId] = (classCountMap[e.lopId] || 0) + 1;
});

CLASSES_HP.forEach(c => {
  c.siSoHienTai = classCountMap[c.id] || 0;
  if (c.siSoHienTai > c.siSoMax) {
    c.siSoMax = c.siSoHienTai + 10;
  }
});

// Write LOP_HOC_PHAN to SQL
sql += `-- 10. BẢNG LOP_HOC_PHAN (${CLASSES_HP.length} lớp học phần)\nINSERT INTO lop_hoc_phan (id, ma, mon_hoc_id, hoc_ky_id, giang_vien_id, phong, thu, tiet_bat_dau, so_tiet, si_so_toi_da, si_so_hien_tai, trang_thai_bang_diem, phien_ban_cong_bo, duyet_bang_diem_luc, cong_bo_bang_diem_luc) VALUES\n`;
const lhpRows = CLASSES_HP.map(c => {
  const duyet = c.trangThai === "DA_CONG_BO" ? "DATE_SUB(NOW(), INTERVAL 30 DAY)" : "NULL";
  const congbo = c.trangThai === "DA_CONG_BO" ? "DATE_SUB(NOW(), INTERVAL 25 DAY)" : "NULL";
  return `(${c.id}, '${c.ma}', ${c.monId}, ${c.hkId}, ${c.gvId}, '${c.phong}', ${c.thu}, ${c.tiet}, ${c.soTiet}, ${c.siSoMax}, ${c.siSoHienTai}, '${c.trangThai}', 1, ${duyet}, ${congbo})`;
});
sql += lhpRows.join(",\n") + ";\n\n";

// Write DANG_KY_HOC_PHAN to SQL in chunks
sql += `-- 11. BẢNG DANG_KY_HOC_PHAN (${ENROLLMENTS.length} lượt đăng ký)\n`;
const chunkSize = 200;
for (let i = 0; i < ENROLLMENTS.length; i += chunkSize) {
  const chunk = ENROLLMENTS.slice(i, i + chunkSize);
  sql += `INSERT INTO dang_ky_hoc_phan (id, sinh_vien_id, lop_hoc_phan_id, thoi_gian_dang_ky, trang_thai) VALUES\n`;
  sql += chunk.map(e => `(${e.id}, ${e.sinhVienId}, ${e.lopId}, NOW(), '${e.trangThai}')`).join(",\n") + ";\n\n";
}

// Write DIEM to SQL in chunks
sql += `-- 12. BẢNG DIEM (${GRADES.length} bản ghi điểm, phân bố Xuất sắc, Giỏi, Khá, Trung bình, Yếu/Học lại)\n`;
for (let i = 0; i < GRADES.length; i += chunkSize) {
  const chunk = GRADES.slice(i, i + chunkSize);
  sql += `INSERT INTO diem (id, dang_ky_id, diem_qua_trinh, diem_cuoi_ky, diem_tong_ket, cap_nhat_luc) VALUES\n`;
  sql += chunk.map(g => `(${g.id}, ${g.dangKyId}, ${g.qt}, ${g.ck}, ${g.tk}, NOW())`).join(",\n") + ";\n\n";
}

fs.writeFileSync("seed_data.sql", sql, "utf-8");
console.log(`Successfully generated seed_data.sql with DDL! Total lines: ${sql.split("\n").length}`);
