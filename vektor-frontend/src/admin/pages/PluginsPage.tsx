import { Table, Button, Space, Input, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

interface PluginConfig {
  id: string
  pluginId: string
  config: string
  updatedAt: string
}

export default function PluginsPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery<PluginConfig[]>({
    queryKey: ['plugin-configs'],
    queryFn: () => api.get('/admin/plugins').then(r => r.data),
  })

  const save = useMutation({
    mutationFn: ({ pluginId, config }: { pluginId: string; config: string }) =>
      api.put(`/admin/plugins/${pluginId}`, { config }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['plugin-configs'] }); message.success('Config saved') },
    onError: () => message.error('Failed to save config'),
  })

  const columns = [
    { title: 'Plugin ID', dataIndex: 'pluginId', key: 'pluginId' },
    {
      title: 'Config (JSON)', dataIndex: 'config', key: 'config',
      render: (config: string, record: PluginConfig) => (
        <Input.TextArea
          defaultValue={config}
          rows={3}
          style={{ fontFamily: 'monospace' }}
          onBlur={e => save.mutate({ pluginId: record.pluginId, config: e.target.value })}
        />
      ),
    },
    { title: 'Updated', dataIndex: 'updatedAt', key: 'updatedAt', render: (v: string) => v ? new Date(v).toLocaleString() : '-' },
  ]

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Table rowKey="pluginId" dataSource={data} columns={columns} loading={isLoading} />
    </Space>
  )
}
