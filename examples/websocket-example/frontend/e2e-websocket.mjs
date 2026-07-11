import WebSocket from 'ws'

const baseUrl = process.argv[2] || 'http://localhost:8086'
const wsUrl = baseUrl.replace(/^http/, 'ws') + '/ws'
const marker = 'nebula_e2e_' + Date.now()

class TestClient {
  constructor(userId) {
    this.userId = userId
    this.messages = []
    this.waiters = []
    this.socket = new WebSocket(`${wsUrl}?userId=${encodeURIComponent(userId)}`)
    this.socket.on('message', data => this.accept(JSON.parse(data.toString())))
  }

  async open() {
    if (this.socket.readyState === WebSocket.OPEN) return
    await new Promise((resolve, reject) => {
      this.socket.once('open', resolve)
      this.socket.once('error', reject)
      setTimeout(() => reject(new Error(`连接 ${this.userId} 超时`)), 5000)
    })
  }

  accept(message) {
    const waiterIndex = this.waiters.findIndex(waiter => waiter.predicate(message))
    if (waiterIndex >= 0) {
      const [waiter] = this.waiters.splice(waiterIndex, 1)
      clearTimeout(waiter.timer)
      waiter.resolve(message)
    } else {
      this.messages.push(message)
    }
  }

  waitFor(predicate, timeout = 5000) {
    const existingIndex = this.messages.findIndex(predicate)
    if (existingIndex >= 0) return Promise.resolve(this.messages.splice(existingIndex, 1)[0])
    return new Promise((resolve, reject) => {
      const waiter = { predicate, resolve, reject }
      waiter.timer = setTimeout(() => {
        this.waiters = this.waiters.filter(candidate => candidate !== waiter)
        reject(new Error(`等待 ${this.userId} 消息超时`))
      }, timeout)
      this.waiters.push(waiter)
    })
  }

  send(message) {
    this.socket.send(JSON.stringify(message))
  }

  async close() {
    if (this.socket.readyState === WebSocket.CLOSED) return
    await new Promise(resolve => {
      this.socket.once('close', resolve)
      this.socket.close(1000, 'e2e complete')
    })
  }
}

async function api(path, method = 'GET', body) {
  const response = await fetch(baseUrl + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  })
  const json = await response.json()
  if (!response.ok || json.success !== true) {
    throw new Error(`${method} ${path} 失败: HTTP ${response.status}`)
  }
  return json.data
}

async function expectNoMessage(client, predicate) {
  try {
    await client.waitFor(predicate, 400)
  } catch (error) {
    if (error.message.includes('消息超时')) return
    throw error
  }
  throw new Error(`${client.userId} 收到了不应接收的定向消息`)
}

async function waitForStatus(expectedSessions, expectedUsers) {
  for (let attempt = 0; attempt < 20; attempt++) {
    const status = await api('/ws-api/status')
    if (status.onlineSessions === expectedSessions && status.onlineUsers === expectedUsers) return status
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  throw new Error(`在线状态未变为 sessions=${expectedSessions}, users=${expectedUsers}`)
}

const userA = `${marker}_user_a`
const userB = `${marker}_user_b`
const clientA = new TestClient(userA)
const clientB = new TestClient(userB)

try {
  await clientA.open()
  const connectedA = await clientA.waitFor(message => message.type === 'connected')
  await clientB.open()
  const connectedB = await clientB.waitFor(message => message.type === 'connected')
  await waitForStatus(2, 2)

  clientA.send({ type: 'chat', payload: { content: `${marker}_chat` } })
  await Promise.all([
    clientA.waitFor(message => message.type === 'chat' && message.payload?.content === `${marker}_chat`),
    clientB.waitFor(message => message.type === 'chat' && message.payload?.content === `${marker}_chat`)
  ])

  const broadcast = await api('/ws-api/broadcast', 'POST', {
    type: 'notification', content: `${marker}_broadcast`
  })
  await Promise.all([
    clientA.waitFor(message => message.payload?.content === `${marker}_broadcast`),
    clientB.waitFor(message => message.payload?.content === `${marker}_broadcast`)
  ])

  const toUser = await api('/ws-api/send-to-user', 'POST', {
    type: 'notification', content: `${marker}_user_only`, targetUserId: userA
  })
  await clientA.waitFor(message => message.payload?.content === `${marker}_user_only`)
  await expectNoMessage(clientB, message => message.payload?.content === `${marker}_user_only`)

  const sessionB = connectedB.payload.sessionId
  const toSession = await api('/ws-api/send-to-session', 'POST', {
    type: 'notification', content: `${marker}_session_only`, targetSessionId: sessionB
  })
  await clientB.waitFor(message => message.payload?.content === `${marker}_session_only`)
  await expectNoMessage(clientA, message => message.payload?.content === `${marker}_session_only`)

  clientA.send({ type: 'heartbeat', payload: 'ping' })
  const heartbeat = await clientA.waitFor(message => message.type === 'heartbeat' && message.payload === 'pong')
  const onlineA = await api(`/ws-api/online/${encodeURIComponent(userA)}`)

  await clientB.close()
  await clientA.waitFor(message => message.type === 'system' && message.payload?.event === 'user_left')
  const finalStatus = await waitForStatus(1, 1)
  const onlineB = await api(`/ws-api/online/${encodeURIComponent(userB)}`)

  console.log(JSON.stringify({
    connected: Boolean(connectedA.payload.sessionId && connectedB.payload.sessionId),
    initialSessions: 2,
    initialUsers: 2,
    chatDelivered: true,
    broadcastSentTo: broadcast.sentTo,
    userSentTo: toUser.sentTo,
    sessionSent: toSession.sent,
    heartbeat: heartbeat.payload,
    userAOnline: onlineA.online,
    userBOnlineAfterClose: onlineB.online,
    finalSessions: finalStatus.onlineSessions,
    finalUsers: finalStatus.onlineUsers
  }))
} finally {
  await clientB.close().catch(() => {})
  await clientA.close().catch(() => {})
}
