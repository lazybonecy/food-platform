const WS_URL = 'ws://localhost:8082/ws/notification'

let ws = null
let reconnectTimer = null
let onMessage = null
let pingTimer = null

export function connectWs(messageHandler) {
  onMessage = messageHandler
  doConnect()
}

export function disconnectWs() {
  clearTimeout(reconnectTimer)
  clearInterval(pingTimer)
  reconnectTimer = null
  pingTimer = null
  onMessage = null
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
}

function doConnect() {
  const token = localStorage.getItem('accessToken')
  if (!token) return

  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

  ws = new WebSocket(`${WS_URL}?token=${token}`)

  ws.onopen = () => {
    // 心跳保活
    pingTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send('ping')
      }
    }, 25000)
  }

  ws.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const data = JSON.parse(event.data)
      if (onMessage) onMessage(data)
    } catch (e) { /* ignore */ }
  }

  ws.onclose = () => {
    clearInterval(pingTimer)
    pingTimer = null
    // 自动重连（仅在用户已登录时）
    if (localStorage.getItem('accessToken')) {
      reconnectTimer = setTimeout(doConnect, 3000)
    }
  }

  ws.onerror = () => {
    ws.close()
  }
}
