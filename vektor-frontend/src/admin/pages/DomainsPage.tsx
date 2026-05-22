import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, Switch, message, Popconfirm } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

interface Domain {
  id: string
  name: string
  active: boolean
  dkimSelector?: string
  spfPolicy?: string
  dmarcPolicy?: string
}

export default function DomainsPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<Domain[]>({
    queryKey: ['domains'],
    queryFn: () => api.get('/admin/domains').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: Partial<Domain>) => api.post('/admin/domains', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['domains'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to create domain'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/domains/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['domains'] }),
  })

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Active', dataIndex: 'active', key: 'active', render: (v: boolean) => v ? 'Yes' : 'No' },
    { title: 'DKIM Selector', dataIndex: 'dkimSelector', key: 'dkimSelector' },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, record: Domain) => (
        <Popconfirm title="Delete this domain?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>Add Domain</Button>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="Add Domain" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="name" label="Domain name" rules={[{ required: true }]}>
            <Input placeholder="example.com" />
          </Form.Item>
          <Form.Item name="active" label="Active" valuePropName="checked" initialValue={true}>
            <Switch />
          </Form.Item>
          <Form.Item name="dkimSelector" label="DKIM Selector">
            <Input placeholder="mail" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
