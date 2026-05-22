import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

function DomainSelect() {
  const { data } = useQuery<{ id: string; name: string }[]>({
    queryKey: ['domains'],
    queryFn: () => api.get('/admin/domains').then(r => r.data),
  })
  return (
    <Select
      options={data?.map(d => ({ value: d.id, label: d.name }))}
      placeholder="Select domain"
    />
  )
}

interface Account {
  id: string
  email: string
  active: boolean
  roles: string[]
  quotaBytes: number
}

export default function AccountsPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<Account[]>({
    queryKey: ['accounts'],
    queryFn: () => api.get('/admin/accounts').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: unknown) => api.post('/admin/accounts', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['accounts'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to create account'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/accounts/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts'] }),
  })

  const columns = [
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Roles', dataIndex: 'roles', key: 'roles',
      render: (roles: string[]) => roles?.map(r => <Tag key={r} color="blue">{r}</Tag>),
    },
    { title: 'Active', dataIndex: 'active', key: 'active', render: (v: boolean) => v ? 'Yes' : 'No' },
    { title: 'Quota (MB)', dataIndex: 'quotaBytes', key: 'quotaBytes', render: (v: number) => v === 0 ? 'Unlimited' : `${(v / 1024 / 1024).toFixed(0)} MB` },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, record: Account) => (
        <Popconfirm title="Delete this account?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>Add Account</Button>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="Add Account" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true, min: 8 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="domainId" label="Domain" rules={[{ required: true }]}>
            <DomainSelect />
          </Form.Item>
          <Form.Item name="roles" label="Roles" initialValue={['USER']}>
            <Select mode="multiple" options={[
              { value: 'USER', label: 'User' },
              { value: 'ADMIN', label: 'Admin' },
              { value: 'DOMAIN_ADMIN', label: 'Domain Admin' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
