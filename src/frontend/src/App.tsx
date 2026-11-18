import { Route, Routes } from 'react-router'
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
          <Route path="quan-tri" element={<AdminDashboardPage />} />
          <Route path="quan-tri/nguoi-dung" element={<AdminUsersPage />} />
          <Route path="quan-tri/khoa-hoc" element={<AdminCoursesPage />} />
          <Route path="quan-tri/bai-hoc" element={<AdminLessonsPage />} />
          <Route path="quan-tri/bai-hoc/moi" element={<AdminLessonEditPage />} />
          <Route path="quan-tri/bai-hoc/:lessonId/sua" element={<AdminLessonEditPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
