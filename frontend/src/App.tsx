import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import Login from '@/components/Login';
import Register from '@/components/Register';
import Layout from '@/components/Layout';
import Dashboard from '@/components/Dashboard';
import AgentList from '@/components/AgentList';
import ArticleList from '@/components/ArticleList';
import ArticleDetail from '@/components/ArticleDetail';
import ArticleForm from '@/components/ArticleForm';
import Settings from '@/components/Settings';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  if (!isLoggedIn) {
    return <Navigate to="/login" />;
  }
  return <>{children}</>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  if (isLoggedIn) {
    return <Navigate to="/dashboard" />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
      
      <Route path="/" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
      <Route path="/dashboard" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
      <Route path="/agents" element={<ProtectedRoute><Layout><AgentList /></Layout></ProtectedRoute>} />
      <Route path="/articles" element={<ProtectedRoute><Layout><ArticleList /></Layout></ProtectedRoute>} />
      <Route path="/articles/new" element={<ProtectedRoute><Layout><ArticleForm /></Layout></ProtectedRoute>} />
      <Route path="/articles/:id" element={<ProtectedRoute><Layout><ArticleDetail /></Layout></ProtectedRoute>} />
      <Route path="/articles/:id/edit" element={<ProtectedRoute><Layout><ArticleForm /></Layout></ProtectedRoute>} />
      <Route path="/settings" element={<ProtectedRoute><Layout><Settings /></Layout></ProtectedRoute>} />
      
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}