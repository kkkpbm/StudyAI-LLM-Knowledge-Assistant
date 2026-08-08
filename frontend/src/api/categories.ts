import http from './index'

export interface Category {
  id?: number
  name: string
  color?: string
  userId?: number
}

export function getCategories() {
  return http.get('/categories')
}

export function createCategory(data: Category) {
  return http.post('/categories', data)
}

export function updateCategory(id: number, data: Category) {
  return http.put(`/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return http.delete(`/categories/${id}`)
}