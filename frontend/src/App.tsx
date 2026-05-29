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
import CollectionList from '@/components/CollectionList';
import KnowledgeBaseList from '@/components/KnowledgeBaseList';
import KnowledgeBaseDetail from '@/components/KnowledgeBaseDetail';
import KnowledgeQuery from '@/components/KnowledgeQuery';
import Settings from '@/components/Settings';
import ToolPanel from '@/components/ToolPanel';
import PhotoStandardization from '@/components/PhotoStandardization';
import ImageFormatConverter from '@/components/ImageFormatConverter';
import FileFormatConverter from '@/components/FileFormatConverter';
import JsonFormatter from '@/components/JsonFormatter';
import DiskAnalyzer from '@/components/DiskAnalyzer';
import RegistryCleaner from '@/components/RegistryCleaner';
import IpAnalyzer from '@/components/IpAnalyzer';
import AppIconGenerator from '@/components/AppIconGenerator';

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
      <Route path="/collections" element={<ProtectedRoute><Layout><CollectionList /></Layout></ProtectedRoute>} />
      <Route path="/knowledge/bases" element={<ProtectedRoute><Layout><KnowledgeBaseList /></Layout></ProtectedRoute>} />
      <Route path="/knowledge/bases/:id" element={<ProtectedRoute><Layout><KnowledgeBaseDetail /></Layout></ProtectedRoute>} />
      <Route path="/knowledge/query" element={<ProtectedRoute><Layout><KnowledgeQuery /></Layout></ProtectedRoute>} />
      <Route path="/tools" element={<ProtectedRoute><Layout><ToolPanel /></Layout></ProtectedRoute>} />
      <Route path="/tools/photo-standardization" element={<ProtectedRoute><Layout><PhotoStandardization /></Layout></ProtectedRoute>} />
      <Route path="/tools/image-converter" element={<ProtectedRoute><Layout><ImageFormatConverter /></Layout></ProtectedRoute>} />
      <Route path="/tools/file-converter" element={<ProtectedRoute><Layout><FileFormatConverter /></Layout></ProtectedRoute>} />
      <Route path="/tools/json-formatter" element={<ProtectedRoute><Layout><JsonFormatter /></Layout></ProtectedRoute>} />
      <Route path="/tools/disk-analyzer" element={<ProtectedRoute><Layout><DiskAnalyzer /></Layout></ProtectedRoute>} />
      <Route path="/tools/registry-cleaner" element={<ProtectedRoute><Layout><RegistryCleaner /></Layout></ProtectedRoute>} />
      <Route path="/tools/ip-analyzer" element={<ProtectedRoute><Layout><IpAnalyzer /></Layout></ProtectedRoute>} />
      <Route path="/tools/app-icon-generator" element={<ProtectedRoute><Layout><AppIconGenerator /></Layout></ProtectedRoute>} />
      <Route path="/settings" element={<ProtectedRoute><Layout><Settings /></Layout></ProtectedRoute>} />
      
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}