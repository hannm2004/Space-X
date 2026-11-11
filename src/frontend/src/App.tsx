import { Route, Routes } from 'react-router'
import { AuthProvider } from './AuthContext'
import Layout from './Layout'
import ChangePasswordPage from './pages/ChangePasswordPage'
import CoursePage from './pages/CoursePage'
import HomePage from './pages/HomePage'
import LessonPage from './pages/LessonPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
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
          <Route path="doi-mat-khau" element={<ChangePasswordPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
