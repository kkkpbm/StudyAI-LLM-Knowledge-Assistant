import http from './index'

export interface Note {
  id?: number
  title: string
  contentMd: string
  categoryId?: number | null
  difficultyLevel?: string
  tagIds?: number[]
}

export function getNotes(params: any) {
  return http.get('/notes', { params })
}

export function getNote(id: number) {
  return http.get(`/notes/${id}`)
}

export function createNote(data: Note) {
  return http.post('/notes', data)
}

export function updateNote(id: number, data: Note) {
  return http.put(`/notes/${id}`, data)
}

export function deleteNote(id: number) {
  return http.delete(`/notes/${id}`)
}

export function getCategories() {
  return http.get('/categories')
}

export function createCategory(data: any) {
  return http.post('/categories', data)
}

export function getTags() {
  return http.get('/tags')
}

export function createTag(data: any) {
  return http.post('/tags', data)
}

/** 上传学习资料并启动“提取文本 → AI 整理 → 人工确认”的解析工作流。 */
export function parseDocument(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/document-workflows/parse', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function confirmDocumentWorkflow(id: number, data: any) {
  return http.post(`/document-workflows/${id}/confirm`, data)
}

export function getDocumentWorkflows() {
  return http.get('/document-workflows')
}
