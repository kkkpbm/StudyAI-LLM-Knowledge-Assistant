import http from './index'

export function summarize(content: string) {
  return http.post('/ai/summarize', { content })
}

export function aiChat(question: string, noteId?: number) {
  return http.post('/ai/chat', { noteId, question })
}

export function genPlan(goal: string) {
  return http.post('/ai/gen-plan', { goal })
}

export function extractGraph(content: string) {
  return http.post('/ai/extract-graph', { content })
}

export function assessDifficulty(content: string) {
  return http.post('/ai/assess', { content })
}

export function syncChatMemory(question: string, answer: string) {
  return http.post('/ai/chat-memory/sync', { question, answer })
}

export interface StreamMeta {
  should_save: boolean
  suggested_title: string
  sources?: Array<{ noteId: number | string; title: string; snippet: string; score: number }>
}

export interface HistoryMsg {
  role: 'user' | 'assistant'
  content: string
}

export type ChatMode = 'knowledge' | 'chat'

// 流式聊天：返回 ReadableStream，前端逐字显示，末尾 yield meta 对象
export async function* aiChatStream(question: string, noteId?: number, signal?: AbortSignal, history?: HistoryMsg[], mode: ChatMode = 'knowledge'): AsyncGenerator<string | StreamMeta> {
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
    },
    body: JSON.stringify({ noteId, question, history: history || [], mode }),
    signal,
  })

  if (!response.ok) {
    throw new Error('Stream request failed')
  }

  const reader = response.body?.getReader()
  if (!reader) throw new Error('No reader')

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const payload = line.slice(6)
        if (payload === '[DONE]') return
        if (payload.startsWith('[ERROR]')) throw new Error(payload.slice(8))
        // [DONE] 后附带 JSON 元数据，如 [DONE]{"should_save":true,...}
        if (payload.startsWith('[DONE]{')) {
          try {
            const meta: StreamMeta = JSON.parse(payload.slice(5))
            yield meta
          } catch { /* ignore */ }
          return
        }
        yield payload
      }
    }
  }
}

// ===== 知识图谱 API =====
export function buildKnowledgeGraph() {
  return http.post('/knowledge-graph/build')
}

export function getKnowledgeRelations() {
  return http.get('/knowledge-graph/relations')
}

export function getNoteRelations(noteId: number) {
  return http.get(`/knowledge-graph/relations/${noteId}`)
}

export function deleteAllRelations() {
  return http.delete('/knowledge-graph/relations')
}

export function deleteNoteRelations(noteId: number) {
  return http.delete(`/knowledge-graph/relations/${noteId}`)
}

export function getConceptCount() {
  return http.get('/knowledge-graph/concepts/count')
}

export function suggestTags(content: string) {
  return http.post('/ai/suggest-tags', { content })
}

export function getLearningInsight() {
  return http.get('/ai/learning-insight')
}

export function getConceptDetail(conceptName: string) {
  return http.get(`/knowledge-graph/concepts/${encodeURIComponent(conceptName)}`)
}

export function createKnowledgeRelation(data: any) {
  return http.post('/knowledge-graph/relations/manual', data)
}

export function updateKnowledgeRelation(id: number, data: any) {
  return http.put(`/knowledge-graph/relations/manual/${id}`, data)
}

export function deleteKnowledgeRelation(id: number) {
  return http.delete(`/knowledge-graph/relations/manual/${id}`)
}

export function getRelationRecommendations() {
  return http.get('/knowledge-graph/recommendations')
}
