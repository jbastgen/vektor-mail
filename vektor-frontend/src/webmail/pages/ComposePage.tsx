import { Form, Input, Button, Space, message, Typography } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import api from '@/common/api'

const { Title } = Typography

interface ComposeForm {
  to: string
  cc?: string
  subject: string
  body: string
}

export default function ComposePage() {
  const [form] = Form.useForm<ComposeForm>()
  const navigate = useNavigate()

  const send = useMutation({
    mutationFn: (values: ComposeForm) =>
      api.post('/webmail/compose', {
        to: values.to.split(',').map(s => s.trim()),
        cc: values.cc ? values.cc.split(',').map(s => s.trim()) : [],
        subject: values.subject,
        body: values.body,
      }),
    onSuccess: () => {
      message.success('Message sent')
      navigate('/webmail/inbox')
    },
    onError: () => message.error('Failed to send message'),
  })

  return (
    <Space direction="vertical" style={{ width: '100%', maxWidth: 720 }}>
      <Title level={4}>New Message</Title>
      <Form form={form} layout="vertical" onFinish={v => send.mutate(v)}>
        <Form.Item name="to" label="To" rules={[{ required: true, message: 'At least one recipient required' }]}>
          <Input placeholder="recipient@example.com, another@example.com" />
        </Form.Item>
        <Form.Item name="cc" label="Cc">
          <Input placeholder="cc@example.com" />
        </Form.Item>
        <Form.Item name="subject" label="Subject" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="body" label="Message" rules={[{ required: true }]}>
          <Input.TextArea rows={12} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={send.isPending}>
            Send
          </Button>
        </Form.Item>
      </Form>
    </Space>
  )
}
