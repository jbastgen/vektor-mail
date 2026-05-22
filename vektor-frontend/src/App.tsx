import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import LoginPage from './common/LoginPage'
import AdminLayout from './admin/AdminLayout'
import WebmailLayout from './webmail/WebmailLayout'

export default function App() {
  const token = useAuthStore(s => s.token)

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/admin/*"
        element={token ? <AdminLayout /> : <Navigate to="/login" replace />}
      />
      <Route
        path="/webmail/*"
        element={token ? <WebmailLayout /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to="/webmail" replace />} />
    </Routes>
  )
}
