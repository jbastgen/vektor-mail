import { Routes, Route, Link, useNavigate } from 'react-router-dom'
import { Layout, Menu, Button, theme, Typography } from 'antd'
import {
  InboxOutlined, SendOutlined, FolderOutlined,
  SettingOutlined, LogoutOutlined, MailOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../store/authStore'
import InboxPage from './pages/InboxPage'
import ComposePage from './pages/ComposePage'
import FoldersPage from './pages/FoldersPage'
import SettingsPage from './pages/SettingsPage'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const menuItems = [
  { key: '/webmail/inbox', icon: <InboxOutlined />, label: <Link to="/webmail/inbox">Inbox</Link> },
  { key: '/webmail/compose', icon: <SendOutlined />, label: <Link to="/webmail/compose">Compose</Link> },
  { key: '/webmail/folders', icon: <FolderOutlined />, label: <Link to="/webmail/folders">Folders</Link> },
  { key: '/webmail/settings', icon: <SettingOutlined />, label: <Link to="/webmail/settings">Settings</Link> },
]

export default function WebmailLayout() {
  const { logout, email } = useAuthStore()
  const navigate = useNavigate()
  const { token } = theme.useToken()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={200}>
        <div style={{ padding: '16px', color: '#fff', fontWeight: 700 }}>
          <MailOutlined /> Vektor Mail
        </div>
        <Menu theme="dark" mode="inline" items={menuItems} defaultSelectedKeys={[location.pathname]} />
      </Sider>
      <Layout>
        <Header style={{
          background: token.colorBgContainer,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 24px',
        }}>
          <Text type="secondary">{email}</Text>
          <Button icon={<LogoutOutlined />} onClick={() => { logout(); navigate('/login') }}>Logout</Button>
        </Header>
        <Content style={{ margin: 16, padding: 24, background: token.colorBgContainer, borderRadius: token.borderRadius }}>
          <Routes>
            <Route path="inbox" element={<InboxPage />} />
            <Route path="compose" element={<ComposePage />} />
            <Route path="folders" element={<FoldersPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="*" element={<InboxPage />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}
