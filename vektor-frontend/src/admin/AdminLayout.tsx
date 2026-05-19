import { Routes, Route, Link, useNavigate } from 'react-router-dom'
import { Layout, Menu, Button, theme } from 'antd'
import {
  GlobalOutlined, UserOutlined, MailOutlined, ApartmentOutlined,
  FilterOutlined, ControlOutlined, CloudUploadOutlined, LogoutOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../store/authStore'
import DomainsPage from './pages/DomainsPage'
import AccountsPage from './pages/AccountsPage'
import PipelinePage from './pages/PipelinePage'
import BlocklistPage from './pages/BlocklistPage'
import PluginsPage from './pages/PluginsPage'
import BackupPage from './pages/BackupPage'

const { Header, Sider, Content } = Layout

const menuItems = [
  { key: '/admin/domains', icon: <GlobalOutlined />, label: <Link to="/admin/domains">Domains</Link> },
  { key: '/admin/accounts', icon: <UserOutlined />, label: <Link to="/admin/accounts">Accounts</Link> },
  { key: '/admin/pipeline', icon: <ApartmentOutlined />, label: <Link to="/admin/pipeline">Pipeline</Link> },
  { key: '/admin/blocklist', icon: <FilterOutlined />, label: <Link to="/admin/blocklist">Blocklist</Link> },
  { key: '/admin/plugins', icon: <ControlOutlined />, label: <Link to="/admin/plugins">Plugins</Link> },
  { key: '/admin/backup', icon: <CloudUploadOutlined />, label: <Link to="/admin/backup">Backup</Link> },
]

export default function AdminLayout() {
  const logout = useAuthStore(s => s.logout)
  const navigate = useNavigate()
  const { token } = theme.useToken()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220}>
        <div style={{ padding: '16px', color: '#fff', fontWeight: 700, fontSize: 16 }}>
          <MailOutlined /> Vektor Admin
        </div>
        <Menu theme="dark" mode="inline" items={menuItems} defaultSelectedKeys={[location.pathname]} />
      </Sider>
      <Layout>
        <Header style={{ background: token.colorBgContainer, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', padding: '0 24px' }}>
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>Logout</Button>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: token.colorBgContainer, borderRadius: token.borderRadius }}>
          <Routes>
            <Route path="domains" element={<DomainsPage />} />
            <Route path="accounts" element={<AccountsPage />} />
            <Route path="pipeline" element={<PipelinePage />} />
            <Route path="blocklist" element={<BlocklistPage />} />
            <Route path="plugins" element={<PluginsPage />} />
            <Route path="backup" element={<BackupPage />} />
            <Route path="*" element={<DomainsPage />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}
