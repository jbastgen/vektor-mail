import { useState } from 'react'
import { Table, Tag, Button, Drawer, Space, Typography, Popconfirm, message } from 'antd'
import { DeleteOutlined, ReloadOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

const { Text, Title } = Typography

interface Message {
  id: string
  fromAddress: string
  toAddresses: string
  subject?: string
  receivedAt: string
  sizeBytes: number
  flags?: string
}

interface Page<T> {
  content: T[]
  totalElements: number
  number: number
  size: number
}

export default function InboxPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Message | null>(null)
  const pageSize = 20

  const { data, isLoading, refetch } = useQuery<Page<Message>>({
    queryKey: ['inbox', page],
    queryFn: async () => {
      const folders = await api.get('/webmail/folders').then(r => r.data)
      const inbox = folders.find((f: { path: string }) => f.path === 'INBOX')
      if (!inbox) return { content: [], totalElements: 0, number: 0, size: pageSize }
      return api.get(`/webmail/messages?mailboxId=${inbox.id}&page=${page}&size=${pageSize}`).then(r => r.data)
    },
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/webmail/messages/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inbox'] })
      setSelected(null)
      message.success('Message deleted')
    },
  })

  const columns = [
    { title: 'From', dataIndex: 'fromAddress', key: 'fromAddress', ellipsis: true },
    { title: 'Subject', dataIndex: 'subject', key: 'subject', ellipsis: true, render: (v?: string) => v || '(no subject)' },
    { title: 'Received', dataIndex: 'receivedAt', key: 'receivedAt', render: (v: string) => new Date(v).toLocaleString() },
    { title: 'Size', dataIndex: 'sizeBytes', key: 'sizeBytes', render: (v: number) => `${(v / 1024).toFixed(1)} KB` },
    {
      title: '', key: 'del',
      render: (_: unknown, record: Message) => (
        <Popconfirm title="Delete message?" onConfirm={e => { e?.stopPropagation(); remove.mutate(record.id) }}>
          <Button size="small" icon={<DeleteOutlined />} danger onClick={e => e.stopPropagation()} />
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Space>
        <Title level={4} style={{ margin: 0 }}>Inbox</Title>
        <Button icon={<ReloadOutlined />} size="small" onClick={() => refetch()}>Refresh</Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={data?.content}
        columns={columns}
        loading={isLoading}
        onRow={record => ({ onClick: () => setSelected(record), style: { cursor: 'pointer' } })}
        pagination={{
          current: page + 1,
          pageSize,
          total: data?.totalElements,
          onChange: p => setPage(p - 1),
        }}
      />
      <Drawer
        title={selected?.subject || '(no subject)'}
        open={!!selected}
        onClose={() => setSelected(null)}
        width={640}
        extra={
          <Popconfirm title="Delete?" onConfirm={() => selected && remove.mutate(selected.id)}>
            <Button danger icon={<DeleteOutlined />}>Delete</Button>
          </Popconfirm>
        }
      >
        {selected && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Text><strong>From:</strong> {selected.fromAddress}</Text>
            <Text><strong>To:</strong> {selected.toAddresses}</Text>
            <Text><strong>Received:</strong> {new Date(selected.receivedAt).toLocaleString()}</Text>
            {selected.flags && <Tag>{selected.flags}</Tag>}
          </Space>
        )}
      </Drawer>
    </Space>
  )
}
