import { useState, useEffect } from 'react';
import { Layout, Menu, Badge, Dropdown, Avatar } from 'antd';
import { 
  RobotOutlined,
  DashboardOutlined, 
  AppstoreOutlined, 
  CheckSquareOutlined, 
  BarChartOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  CheckCircleOutlined,
  ToolOutlined,
  ApiOutlined
} from '@ant-design/icons';
import { useNavigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { api } from '../api';

const { Header, Content, Sider } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, hasRole } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (user) {
      api.notifications.getUnreadCount(user.id).then(setUnreadCount).catch(() => {});
    }
  }, [user]);

  const menuItems = [
    { key: '/agent', icon: <RobotOutlined />, label: 'AI助手' },
    { key: '/skills', icon: <ToolOutlined />, label: '技能市场' },
    { key: '/mcp', icon: <ApiOutlined />, label: 'MCP服务' },
    ...(hasRole('admin') ? [
      { key: '/model-providers', icon: <ApiOutlined />, label: '模型服务' },
    ] : []),
    { key: '/dashboard', icon: <DashboardOutlined />, label: '驾驶舱' },
    ...(hasRole('admin') ? [
      { key: '/systems', icon: <AppstoreOutlined />, label: '考核体系管理' },
      { key: '/monitorings', icon: <CheckSquareOutlined />, label: '月度监测' },
    ] : []),
    ...(hasRole('collector') ? [
      { key: '/collect', icon: <CheckSquareOutlined />, label: '收数任务' },
    ] : []),
    ...((hasRole('admin') || hasRole('leader')) ? [
      {
        key: 'reports-group',
        icon: <BarChartOutlined />,
        label: '绩效报表',
        children: [
          { key: '/overview-report', label: '总览报表' },
          { key: '/branch-report', label: '分支报表' },
        ],
      },
      { key: '/confirmations', icon: <CheckCircleOutlined />, label: '绩效结果确认' },
    ] : []),
  ];

  const userMenuItems = [
    { key: 'profile', icon: <UserOutlined />, label: 'Profile' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Logout', danger: true },
  ];

  const handleUserMenuClick = ({ key }: { key: string }) => {
    if (key === 'logout') {
      logout();
      navigate('/login');
    }
  };

  const selectedKey = '/' + location.pathname.split('/')[1];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        background: '#2E4057',
        padding: '0 24px'
      }}>
        <div style={{ color: 'white', fontSize: 20, fontWeight: 'bold' }}>
          机构绩效管理系统
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Badge count={unreadCount} size="small">
            <BellOutlined 
              style={{ fontSize: 20, color: 'white', cursor: 'pointer' }}
              onClick={() => navigate('/notifications')}
            />
          </Badge>
          <Dropdown menu={{ items: userMenuItems, onClick: handleUserMenuClick }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <span style={{ color: 'white' }}>{user?.name || user?.username}</span>
            </div>
          </Dropdown>
        </div>
      </Header>
      
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        
        <Layout style={{ padding: '0 24px 24px' }}>
          <Content style={{ margin: '24px 0' }}>
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}
