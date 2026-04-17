import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { AuthProvider, useAuth } from './store/AuthContext';
import MainLayout from './components/MainLayout';
import LoginPage from './pages/LoginPage';
import AgentPage from './pages/AgentPage';
import SkillPage from './pages/SkillPage';
import McpPage from './pages/McpPage';
import DashboardPage from './pages/DashboardPage';
import SystemListPage from './pages/SystemListPage';
import MonitoringListPage from './pages/MonitoringListPage';
import DataCollectPage from './pages/DataCollectPage';
import ReportPage from './pages/ReportPage';
import OverviewReportPage from './pages/OverviewReportPage';
import BranchReportPage from './pages/BranchReportPage';
import ConfirmationPage from './pages/ConfirmationPage';
import NotificationPage from './pages/NotificationPage';
import './App.css';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route path="/" element={
        <ProtectedRoute>
          <MainLayout />
        </ProtectedRoute>
      }>
        <Route index element={<AgentPage />} />
        <Route path="agent" element={<AgentPage />} />
        <Route path="skills" element={<SkillPage />} />
        <Route path="mcp" element={<McpPage />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="systems" element={<SystemListPage />} />
        <Route path="monitorings" element={<MonitoringListPage />} />
        <Route path="collect" element={<DataCollectPage />} />
        <Route path="reports" element={<ReportPage />} />
        <Route path="overview-report" element={<OverviewReportPage />} />
        <Route path="branch-report" element={<BranchReportPage />} />
        <Route path="confirmations" element={<ConfirmationPage />} />
        <Route path="notifications" element={<NotificationPage />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </ConfigProvider>
  );
}
