import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, InputNumber, Popconfirm, message, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

const { Title } = Typography

interface Rule {
  id: string
  name?: string
  priority: number
  condition: string
  action: string
  active: boolean
}

export default function SettingsPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<Rule[]>({
    queryKey: ['rules'],
    queryFn: () => api.get('/webmail/settings/rules').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: unknown) => api.post('/webmail/settings/rules', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['rules'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to create rule'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/webmail/settings/rules/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rules'] }),
  })

  const columns = [
    { title: 'Priority', dataIndex: 'priority', key: 'priority' },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Condition (JSON)', dataIndex: 'condition', key: 'condition', ellipsis: true },
    { title: 'Action (JSON)', dataIndex: 'action', key: 'action', ellipsis: true },
    {
      title: 'Actions', key: 'del',
      render: (_: unknown, record: Rule) => (
        <Popconfirm title="Delete rule?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Title level={4}>Mail Rules</Title>
      <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>Add Rule</Button>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="Add Rule" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="name" label="Name">
            <Input placeholder="Move newsletters" />
          </Form.Item>
          <Form.Item name="priority" label="Priority" initialValue={100}>
            <InputNumber min={1} max={999} />
          </Form.Item>
          <Form.Item name="condition" label="Condition (JSON)" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder='{"field":"from","op":"contains","value":"@newsletter.com"}' />
          </Form.Item>
          <Form.Item name="action" label="Action (JSON)" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder='{"type":"move","target":"Newsletters"}' />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
