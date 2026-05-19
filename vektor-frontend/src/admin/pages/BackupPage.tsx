import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, Tag, Popconfirm, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

interface BackupJob {
  id: string
  pluginId: string
  schedule: string
  enabled: boolean
  lastStatus: 'IDLE' | 'RUNNING' | 'SUCCESS' | 'FAILURE'
  lastRunAt?: string
  nextRunAt?: string
}

const statusColor: Record<string, string> = {
  IDLE: 'default', RUNNING: 'processing', SUCCESS: 'success', FAILURE: 'error',
}

export default function BackupPage() {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const { data, isLoading } = useQuery<BackupJob[]>({
    queryKey: ['backup-jobs'],
    queryFn: () => api.get('/admin/backup').then(r => r.data),
  })

  const create = useMutation({
    mutationFn: (values: unknown) => api.post('/admin/backup', values),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['backup-jobs'] }); setOpen(false); form.resetFields() },
    onError: () => message.error('Failed to create backup job'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/backup/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['backup-jobs'] }),
  })

  const columns = [
    { title: 'Plugin', dataIndex: 'pluginId', key: 'pluginId' },
    { title: 'Schedule', dataIndex: 'schedule', key: 'schedule' },
    { title: 'Enabled', dataIndex: 'enabled', key: 'enabled', render: (v: boolean) => v ? 'Yes' : 'No' },
    {
      title: 'Status', dataIndex: 'lastStatus', key: 'lastStatus',
      render: (s: string) => <Tag color={statusColor[s]}>{s}</Tag>,
    },
    { title: 'Last Run', dataIndex: 'lastRunAt', key: 'lastRunAt', render: (v?: string) => v ? new Date(v).toLocaleString() : '-' },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, record: BackupJob) => (
        <Popconfirm title="Delete this job?" onConfirm={() => remove.mutate(record.id)}>
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button icon={<PlusOutlined />} type="primary" onClick={() => setOpen(true)}>Add Backup Job</Button>
      <Table rowKey="id" dataSource={data} columns={columns} loading={isLoading} />
      <Modal title="Add Backup Job" open={open} onOk={() => form.submit()} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical" onFinish={v => create.mutate(v)}>
          <Form.Item name="pluginId" label="Plugin ID" rules={[{ required: true }]}>
            <Input placeholder="vektor-backup-local" />
          </Form.Item>
          <Form.Item name="schedule" label="Cron schedule" rules={[{ required: true }]}>
            <Input placeholder="0 2 * * *" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
