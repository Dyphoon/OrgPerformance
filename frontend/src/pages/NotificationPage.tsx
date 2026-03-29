import { useState, useEffect } from 'react';
import { List, Card, Tag, Button, Empty } from 'antd';
import { BellOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { api } from '../api';
import type { Notification } from '../types';
import { useAuth } from '../store/AuthContext';

export default function NotificationPage() {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchNotifications = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await api.notifications.list(user.id);
      setNotifications(data || []);
    } catch (error) {
      console.error('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [user]);

  const handleMarkAsRead = async (id: number) => {
    try {
      await api.notifications.markAsRead(id);
      setNotifications(prev => prev.map(n => 
        n.id === id ? { ...n, isRead: true } : n
      ));
    } catch (error) {
      console.error('Failed to mark as read');
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'site': return <BellOutlined />;
      case 'email': return <BellOutlined />;
      case 'im': return <BellOutlined />;
      default: return <BellOutlined />;
    }
  };

  return (
    <div>
      <h2>通知中心</h2>
      
      <Card>
        <List
          loading={loading}
          dataSource={notifications}
          locale={{ emptyText: <Empty description="暂无通知" /> }}
          renderItem={(item: Notification) => (
            <List.Item
              style={{ 
                background: item.isRead ? '#fff' : '#f0f7ff',
                padding: 16,
                borderRadius: 8,
                marginBottom: 8,
              }}
              actions={[
                !item.isRead && (
                  <Button 
                    key="read" 
                    size="small" 
                    icon={<CheckCircleOutlined />}
                    onClick={() => handleMarkAsRead(item.id)}
                  >
                    标记已读
                  </Button>
                )
              ].filter(Boolean)}
            >
              <List.Item.Meta
                avatar={
                  <div style={{ 
                    width: 40, 
                    height: 40, 
                    borderRadius: '50%', 
                    background: item.isRead ? '#ddd' : '#2E4057',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    fontSize: 18,
                  }}>
                    {getTypeIcon(item.type)}
                  </div>
                }
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span>{item.title}</span>
                    {!item.isRead && <Tag color="blue">新</Tag>}
                  </div>
                }
                description={
                  <div>
                    <p style={{ margin: '8px 0', color: '#666' }}>{item.content}</p>
                    <span style={{ fontSize: 12, color: '#999' }}>
                      {new Date(item.createdAt).toLocaleString()}
                    </span>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
