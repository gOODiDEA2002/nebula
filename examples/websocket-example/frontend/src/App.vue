<template>
  <div class="app">
    <header class="header">
      <h1>Nebula WebSocket Demo</h1>
      <div class="status" :class="{ connected: wsConnected }">
        {{ wsConnected ? 'Connected' : 'Disconnected' }}
      </div>
    </header>

    <div class="main">
      <aside class="sidebar">
        <h3>Connection</h3>
        <div class="form-group">
          <label>Nickname</label>
          <input v-model="nickname" placeholder="Enter nickname" :disabled="wsConnected" />
        </div>
        <button v-if="!wsConnected" @click="connect" class="btn btn-primary">
          Connect
        </button>
        <button v-else @click="disconnect" class="btn btn-danger">
          Disconnect
        </button>

        <div class="info" v-if="wsConnected">
          <p>Session: <code>{{ sessionId || '-' }}</code></p>
          <p>Online: <strong>{{ onlineCount }}</strong></p>
        </div>
      </aside>

      <section class="chat">
        <div class="messages" ref="messagesRef">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message"
            :class="msg.cls"
          >
            <span class="meta">{{ msg.meta }}</span>
            <span class="text">{{ msg.text }}</span>
          </div>
        </div>

        <div class="input-bar">
          <input
            v-model="inputText"
            placeholder="Type a message..."
            :disabled="!wsConnected"
            @keyup.enter="sendMessage"
          />
          <button @click="sendMessage" :disabled="!wsConnected" class="btn btn-primary">
            Send
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted } from 'vue'

const nickname = ref('User-' + Math.floor(Math.random() * 1000))
const inputText = ref('')
const wsConnected = ref(false)
const sessionId = ref('')
const onlineCount = ref(0)
const messages = ref([])
const messagesRef = ref(null)

let ws = null

function connect() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${location.host}/ws`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    wsConnected.value = true
    addSystemMessage('Connected to server')
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      handleMessage(msg)
    } catch {
      addSystemMessage('Received: ' + event.data)
    }
  }

  ws.onclose = (event) => {
    wsConnected.value = false
    addSystemMessage(`Disconnected (code=${event.code})`)
    ws = null
  }

  ws.onerror = () => {
    addSystemMessage('Connection error')
  }
}

function disconnect() {
  if (ws) {
    ws.close()
  }
}

function sendMessage() {
  if (!ws || !inputText.value.trim()) return

  const payload = {
    type: 'chat',
    payload: {
      content: inputText.value.trim(),
      nickname: nickname.value
    }
  }

  ws.send(JSON.stringify(payload))
  inputText.value = ''
}

function handleMessage(msg) {
  if (msg.type === 'chat') {
    const data = msg.payload || {}
    const from = data.from || 'unknown'
    addMessage(from, data.content || '', 'msg-chat')
  } else if (msg.type === 'system') {
    const data = msg.payload || {}
    if (data.event === 'user_joined') {
      onlineCount.value = data.onlineCount || 0
      addSystemMessage(`A user joined (online: ${onlineCount.value})`)
    } else if (data.event === 'user_left') {
      onlineCount.value = data.onlineCount || 0
      addSystemMessage(`A user left (online: ${onlineCount.value})`)
    } else {
      addSystemMessage(JSON.stringify(data))
    }
  } else if (msg.type === 'notification') {
    const data = msg.payload || {}
    addMessage('Server', data.content || JSON.stringify(data), 'msg-notification')
  } else if (msg.type === 'connected') {
    sessionId.value = msg.payload?.sessionId || ''
    addSystemMessage('Session established: ' + sessionId.value)
  }
}

function addMessage(from, text, cls) {
  const time = new Date().toLocaleTimeString()
  messages.value.push({ meta: `[${time}] ${from}:`, text, cls })
  scrollToBottom()
}

function addSystemMessage(text) {
  const time = new Date().toLocaleTimeString()
  messages.value.push({ meta: `[${time}]`, text, cls: 'msg-system' })
  scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  const el = messagesRef.value
  if (el) el.scrollTop = el.scrollHeight
}

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
  color: #333;
}

.app {
  max-width: 960px;
  margin: 0 auto;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #1a1a2e;
  color: #fff;
}

.header h1 { font-size: 18px; font-weight: 600; }

.status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #e74c3c;
}
.status.connected { background: #27ae60; }

.main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  padding: 16px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar h3 { font-size: 14px; color: #666; text-transform: uppercase; }

.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 12px; color: #999; }
.form-group input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.info { font-size: 13px; color: #666; }
.info code { background: #f0f0f0; padding: 2px 4px; border-radius: 2px; font-size: 11px; }

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #3498db; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #2980b9; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-danger:hover:not(:disabled) { background: #c0392b; }

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
}
.message .meta { color: #999; margin-right: 8px; }

.msg-system { color: #7f8c8d; font-style: italic; }
.msg-system .meta { color: #bdc3c7; }
.msg-chat .meta { color: #2980b9; }
.msg-notification .meta { color: #8e44ad; }

.input-bar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
}
.input-bar input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
</style>
