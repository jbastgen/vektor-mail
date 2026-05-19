import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, Popconfirm, message, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

const { Title } = Typography

interface Folder {
  id: string
  name: string
  path: string
  uidValidity: number
}

export default function FoldersPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<Folder[]>({
    queryKey: ['folders'],
    queryFn: () => api.get('/webmail/folders').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: { name: string; path: string }) => api.post('/webmail/folders', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['folders'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to create folder'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/webmail/folders/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
    onError: () => message.error('Cannot delete system folder'),
  })

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Path', dataIndex: 'path', key: 'path' },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, record: Folder) => (
        <Popconfirm title="Delete folder and all its messages?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small" disabled={['INBOX', 'Sent', 'Drafts', 'Trash', 'Junk'].includes(record.path)}>
            Delete
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Space>
        <Title level={4} style={{ margin: 0 }}>Folders</Title>
        <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>New Folder</Button>
      </Space>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="New Folder" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="name" label="Display Name" rules={[{ required: true }]}>
            <Input placeholder="My Folder" />
          </Form.Item>
          <Form.Item name="path" label="Path" rules={[{ required: true }]}>
            <Input placeholder="MyFolder" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
