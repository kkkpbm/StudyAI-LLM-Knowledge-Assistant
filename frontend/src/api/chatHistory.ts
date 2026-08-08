import http from './index'

export interface ChatRecord {
  id?: number
  userId?: number
  role: string
  content: string
  noteId?: number | null
  createdAt?: string
}

export function getChatHistory(limit = 100) {
  return http.get('/chat-history', { params: { limit } })
}

export function saveChatMessage(data: {
  role: string
  content: string
  noteId?: number | null
}) {
  return http.post('/chat-history', data)
}

export function clearChatHistory() {
  return http.delete('/chat-history')
}
