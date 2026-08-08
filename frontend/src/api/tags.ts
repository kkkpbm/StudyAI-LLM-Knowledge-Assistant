import http from './index'

export interface Tag {
  id?: number
  name: string
  userId?: number
}

export function getTags() {
  return http.get('/tags')
}

export function createTag(data: Tag) {
  return http.post('/tags', data)
}

export function deleteTag(id: number) {
  return http.delete(`/tags/${id}`)
}