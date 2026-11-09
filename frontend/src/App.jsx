import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import { homePath, useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import ProfilePage from "./pages/ProfilePage";
import RoleDashboard from "./pages/RoleDashboard";
import RegisterPage from "./pages/student/RegisterPage";
import CoursePlannerPage from "./pages/student/CoursePlannerPage";
import MyCoursesPage from "./pages/student/MyCoursesPage";
import RegistrationHistoryPage from "./pages/student/RegistrationHistoryPage";
import RetakeRegistrationPage from "./pages/student/RetakeRegistrationPage";
import TimetablePage from "./pages/student/TimetablePage";
import GradesPage from "./pages/student/GradesPage";
import StudyHistoryPage from "./pages/student/StudyHistoryPage";
import LecturerClassesPage from "./pages/lecturer/LecturerClassesPage";
import LecturerTimetablePage from "./pages/lecturer/TimetablePage";
import GradeEntryPage from "./pages/lecturer/GradeEntryPage";
import LecturerAttendancePage from "./pages/lecturer/LecturerAttendancePage";
import StudentAttendancePage from "./pages/student/StudentAttendancePage";
import DashboardPage from "./pages/admin/DashboardPage";
import SemesterPage from "./pages/admin/SemesterPage";
import FacultyPage from "./pages/admin/FacultyPage";
import MajorPage from "./pages/admin/MajorPage";
import AdminClassPage from "./pages/admin/AdminClassPage";
import SubjectPage from "./pages/admin/SubjectPage";
import ClassroomPage from "./pages/admin/ClassroomPage";
import CourseClassPage from "./pages/admin/CourseClassPage";
import StudentPage from "./pages/admin/StudentPage";
import LecturerPage from "./pages/admin/LecturerPage";
import AuditLogPage from "./pages/admin/AuditLogPage";
import NotificationsPage from "./pages/NotificationsPage";
import ProgressPlannerPage from "./pages/student/ProgressPlannerPage";
import CurriculumPage from "./pages/admin/CurriculumPage";
import SupportRequestsPage from "./pages/student/SupportRequestsPage";
import AdminSupportRequestsPage from "./pages/admin/AdminSupportRequestsPage";
import AuditAlertsPage from "./pages/admin/AuditAlertsPage";
import GradeAppealsPage from "./pages/student/GradeAppealsPage";
import AdminGradeAppealsPage from "./pages/admin/AdminGradeAppealsPage";
import AdminLookupPage from "./pages/admin/AdminLookupPage";

function RootRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homePath(user.role) : "/login"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RootRedirect />} />

      <Route
        path="/tong-quan"
        element={
          <ProtectedRoute roles={["SINH_VIEN", "GIANG_VIEN"]}>
            <RoleDashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/dang-ky"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <RegisterPage />
          </ProtectedRoute>
        }
      />
      <Route path="/dang-ky-hoc-lai" element={<ProtectedRoute roles={["SINH_VIEN"]}><RetakeRegistrationPage /></ProtectedRoute>} />
      <Route
        path="/ke-hoach-hoc-tap"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <CoursePlannerPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/hoc-phan-cua-toi"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <MyCoursesPage />
          </ProtectedRoute>
        }
      />
      <Route path="/lich-su-dang-ky" element={<ProtectedRoute roles={["SINH_VIEN"]}><RegistrationHistoryPage /></ProtectedRoute>} />
      <Route
        path="/thoi-khoa-bieu"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <TimetablePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/diem"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <GradesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lich-su-hoc-tap"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <StudyHistoryPage />
          </ProtectedRoute>
        }
      />
      <Route path="/chuyen-can-cua-toi" element={<ProtectedRoute roles={["SINH_VIEN"]}><StudentAttendancePage /></ProtectedRoute>} />
      <Route path="/tien-do" element={<ProtectedRoute roles={["SINH_VIEN"]}><ProgressPlannerPage /></ProtectedRoute>} />
      <Route path="/ke-hoach-mon-thieu" element={<ProtectedRoute roles={["SINH_VIEN"]}><ProgressPlannerPage /></ProtectedRoute>} />
      <Route path="/yeu-cau-ho-tro" element={<ProtectedRoute roles={["SINH_VIEN"]}><SupportRequestsPage /></ProtectedRoute>} />
      <Route path="/phuc-khao-diem" element={<ProtectedRoute roles={["SINH_VIEN"]}><GradeAppealsPage /></ProtectedRoute>} />
      <Route
        path="/thong-bao"
        element={
          <ProtectedRoute roles={["SINH_VIEN", "GIANG_VIEN"]}>
            <NotificationsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/lop-cua-toi"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <LecturerClassesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lop-cua-toi/thoi-khoa-bieu"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <LecturerTimetablePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lop-cua-toi/:id/nhap-diem"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <GradeEntryPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lop-cua-toi/:id/diem-danh"
        element={
          <ProtectedRoute roles={["GIANG_VIEN", "QUAN_TRI"]}>
            <LecturerAttendancePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/quan-tri"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/hoc-ky"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SemesterPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/khoa"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <FacultyPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/nganh"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <MajorPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/lop-hanh-chinh"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <AdminClassPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/mon-hoc"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SubjectPage initialTab="mon-hoc" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/quan-he-mon"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SubjectPage initialTab="quan-he-mon" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/khoa-hoc"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SubjectPage initialTab="khoa-hoc" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/phong-hoc"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <ClassroomPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/quan-tri/lop-hoc-phan"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <CourseClassPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/sinh-vien"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <StudentPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/giang-vien"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <LecturerPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/nhat-ky-hoat-dong"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <AuditLogPage />
          </ProtectedRoute>
        }
      />
      <Route path="/quan-tri/chuong-trinh-dao-tao" element={<ProtectedRoute roles={["QUAN_TRI"]}><CurriculumPage /></ProtectedRoute>} />
      <Route path="/quan-tri/yeu-cau-ho-tro" element={<ProtectedRoute roles={["QUAN_TRI"]}><AdminSupportRequestsPage /></ProtectedRoute>} />
      <Route path="/quan-tri/phuc-khao" element={<ProtectedRoute roles={["QUAN_TRI"]}><AdminGradeAppealsPage /></ProtectedRoute>} />

      <Route
        path="/ho-so"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/quan-tri/canh-bao-audit"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <AuditAlertsPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
        <Route path="/quan-tri/tra-cuu-lich-su"
               element={
            <ProtectedRoute roles={["QUAN_TRI"]}><AdminLookupPage />
            </ProtectedRoute>}
        />.
    </Routes>
  );
}
