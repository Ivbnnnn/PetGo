import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import LoginPage from './pages/LoginPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import Layout from './components/Layout';
import DashboardPage from './pages/DashboardPage';
import WalkRequestsPage from './pages/WalkRequestsPage';
import PetsPage from './pages/PetsPage';
import TagsPage from './pages/TagsPage';
import DistrictsPage from './pages/DistrictsPage';
import UsersPage from './pages/UsersPage';
import AuditPage from './pages/AuditPage';
import CsvPage from './pages/CsvPage';
import WalkHistoryPage from './pages/WalkHistoryPage';
import { isAdmin } from './utils/roles';

function Protected({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.passwordChangeRequired) return <Navigate to="/change-password" replace />;
  return children;
}

function AdminOnly({ children }) {
  const { user } = useAuth();
  return isAdmin(user) ? children : <Navigate to="/walk-requests" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/change-password" element={<ChangePasswordPage />} />
          <Route path="/" element={<Protected><Layout /></Protected>}>
            <Route index element={<DashboardPage />} />
            <Route path="walk-requests" element={<WalkRequestsPage />} />
            <Route path="pets" element={<PetsPage />} />
            <Route path="tags" element={<TagsPage />} />
            <Route path="districts" element={<DistrictsPage />} />
            <Route path="users" element={<AdminOnly><UsersPage /></AdminOnly>} />
            <Route path="audit" element={<AdminOnly><AuditPage /></AdminOnly>} />
            <Route path="csv" element={<CsvPage />} />
            <Route path="walk-history" element={<WalkHistoryPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
