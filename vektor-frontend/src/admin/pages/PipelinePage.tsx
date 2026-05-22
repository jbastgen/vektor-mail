import { useCallback, useState } from 'react'
import { Button, Space, Select, message, Input } from 'antd'
import { SaveOutlined, PlusOutlined } from '@ant-design/icons'
import ReactFlow, {
  Background, Controls, MiniMap,
  addEdge, useNodesState, useEdgesState,
  type Connection, type Edge, type Node,
} from 'reactflow'
import 'reactflow/dist/style.css'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '@/common/api'

interface PipelineRoute {
  id: string
  name: string
  definition: string
  enabled: boolean
}

const initialNodes: Node[] = [
  { id: 'smtp-in', position: { x: 50, y: 150 }, data: { label: 'SMTP Listener' }, type: 'input' },
  { id: 'blocklist', position: { x: 250, y: 50 }, data: { label: 'IP Blocklist' } },
  { id: 'greylist', position: { x: 250, y: 150 }, data: { label: 'Greylisting' } },
  { id: 'spam', position: { x: 250, y: 250 }, data: { label: 'Spam Filter' } },
  { id: 'rules', position: { x: 450, y: 150 }, data: { label: 'Rules Engine' } },
  { id: 'encrypt', position: { x: 650, y: 150 }, data: { label: 'Encrypt' } },
  { id: 'store', position: { x: 850, y: 150 }, data: { label: 'Storage' }, type: 'output' },
]

const initialEdges: Edge[] = [
  { id: 'e1', source: 'smtp-in', target: 'blocklist' },
  { id: 'e2', source: 'smtp-in', target: 'greylist' },
  { id: 'e3', source: 'smtp-in', target: 'spam' },
  { id: 'e4', source: 'blocklist', target: 'rules' },
  { id: 'e5', source: 'greylist', target: 'rules' },
  { id: 'e6', source: 'spam', target: 'rules' },
  { id: 'e7', source: 'rules', target: 'encrypt' },
  { id: 'e8', source: 'encrypt', target: 'store' },
]

export default function PipelinePage() {
  const qc = useQueryClient()
  const [nodes, , onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  const [selectedRoute, setSelectedRoute] = useState<string | null>(null)
  const [yamlDef, setYamlDef] = useState('')

  const onConnect = useCallback((params: Connection) => setEdges(eds => addEdge(params, eds)), [setEdges])

  const { data: routes } = useQuery<PipelineRoute[]>({
    queryKey: ['pipeline-routes'],
    queryFn: () => api.get('/admin/pipeline').then(r => r.data),
  })

  const save = useMutation({
    mutationFn: () => {
      const body = { name: selectedRoute || 'default', definition: yamlDef, enabled: true }
      return selectedRoute
        ? api.put(`/admin/pipeline/${selectedRoute}`, body)
        : api.post('/admin/pipeline', body)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pipeline-routes'] }); message.success('Route saved') },
    onError: () => message.error('Failed to save route'),
  })

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Space>
        <Select
          placeholder="Select route"
          style={{ width: 200 }}
          allowClear
          options={routes?.map(r => ({ value: r.id, label: r.name }))}
          onChange={id => {
            setSelectedRoute(id)
            const r = routes?.find(x => x.id === id)
            if (r) setYamlDef(r.definition ?? '')
          }}
        />
        <Button icon={<SaveOutlined />} type="primary" onClick={() => save.mutate()}>
          Save Route
        </Button>
        <Button icon={<PlusOutlined />} onClick={() => { setSelectedRoute(null); setYamlDef('') }}>
          New
        </Button>
      </Space>

      <div style={{ display: 'flex', gap: 16 }}>
        <div style={{ height: 500, flex: 2, border: '1px solid #d9d9d9', borderRadius: 8 }}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </div>
        <div style={{ flex: 1 }}>
          <Input.TextArea
            value={yamlDef}
            onChange={e => setYamlDef(e.target.value)}
            rows={24}
            placeholder="YAML route definition..."
            style={{ fontFamily: 'monospace' }}
          />
        </div>
      </div>
    </Space>
  )
}
