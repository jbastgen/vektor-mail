import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

interface BlocklistEntry {
  id: string
  type: 'IP' | 'DOMAIN' | 'CIDR'
  value: string
  reason?: string
  addedAt: string
}

export default function BlocklistPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<BlocklistEntry[]>({
    queryKey: ['blocklist'],
    queryFn: () => api.get('/admin/blocklist').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: unknown) => api.post('/admin/blocklist', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['blocklist'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to add entry'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/blocklist/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['blocklist'] }),
  })

  const typeColor: Record<string, string> = { IP: 'red', DOMAIN: 'orange', CIDR: 'volcano' }

  const columns = [
    { title: 'Type', dataIndex: 'type', key: 'type', render: (t: string) => <Tag color={typeColor[t]}>{t}</Tag> },
    { title: 'Value', dataIndex: 'value', key: 'value' },
    { title: 'Reason', dataIndex: 'reason', key: 'reason' },
    { title: 'Added', dataIndex: 'addedAt', key: 'addedAt', render: (v: string) => new Date(v).toLocaleString() },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, record: BlocklistEntry) => (
        <Popconfirm title="Remove from blocklist?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small">Remove</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>Add Entry</Button>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="Block IP/Domain" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="type" label="Type" rules={[{ required: true }]} initialValue="IP">
            <Select options={[{ value: 'IP' }, { value: 'DOMAIN' }, { value: 'CIDR' }]} />
          </Form.Item>
          <Form.Item name="value" label="Value" rules={[{ required: true }]}>
            <Input placeholder="192.168.1.1 or spam.example.com" />
          </Form.Item>
          <Form.Item name="reason" label="Reason">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
