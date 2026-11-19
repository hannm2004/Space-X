import { Route, Routes } from 'react-router'
import AdminLayout from './AdminLayout'
import { AuthProvider } from './AuthContext'
import Layout from './Layout'
import AdminCoursesPage from './pages/AdminCoursesPage'
import AdminDashboardPage from './pages/AdminDashboardPage'
import AdminLessonEditPage from './pages/AdminLessonEditPage'
import AdminLessonsPage from './pages/AdminLessonsPage'
import AdminUsersPage from './pages/AdminUsersPage'
import ChangePasswordPage from './pages/ChangePasswordPage'
import CoursePage from './pages/CoursePage'
import HomePage from './pages/HomePage'
import LessonPage from './pages/LessonPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import { CopyrightPage, PrivacyPage, TermsPage } from './pages/PolicyPages'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import SearchPage from './pages/SearchPage'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="khoa-hoc/:courseSlug" element={<CoursePage />} />
          <Route path="bai/:lessonSlug" element={<LessonPage />} />
          <Route path="tim-kiem" element={<SearchPage />} />
          <Route path="dang-nhap" element={<LoginPage />} />
          <Route path="dang-ky" element={<RegisterPage />} />
          <Route path="ho-so" element={<ProfilePage />} />
          <Route path="doi-mat-khau" element={<ChangePasswordPage />} />
          <Route path="chinh-sach-bao-mat" element={<PrivacyPage />} />
          <Route path="dieu-khoan-su-dung" element={<TermsPage />} />
          <Route path="ban-quyen" element={<CopyrightPage />} />
          <Route path="quan-tri" element={<AdminLayout />}>
            <Route index element={<AdminDashboardPage />} />
            <Route path="nguoi-dung" element={<AdminUsersPage />} />
            <Route path="khoa-hoc" element={<AdminCoursesPage />} />
            <Route path="bai-hoc" element={<AdminLessonsPage />} />
            <Route path="bai-hoc/moi" element={<AdminLessonEditPage />} />
            <Route path="bai-hoc/:lessonId/sua" element={<AdminLessonEditPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
