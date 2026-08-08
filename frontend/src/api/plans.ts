import http from './index'

export interface PlanItem {
  id?: number
  planId?: number
  title?: string
  noteId?: number | null
  orderNum?: number
  estimatedMinutes?: number
  completed?: boolean
  completedAt?: string
}

export interface LearningPlan {
  id?: number
  title: string
  description?: string
  goal?: string
  startDate?: string
  endDate?: string
  status?: number
  aiGenerated?: boolean
  items?: PlanItem[]
}

export function getPlans() {
  return http.get('/plans')
}

export function getPlan(id: number) {
  return http.get(`/plans/${id}`)
}

export function createPlan(data: LearningPlan) {
  return http.post('/plans', data)
}

export function updatePlan(id: number, data: Partial<LearningPlan>) {
  return http.put(`/plans/${id}`, data)
}

export function deletePlan(id: number) {
  return http.delete(`/plans/${id}`)
}

export function getPlanItems(planId: number) {
  return http.get(`/plans/${planId}/items`)
}

export function addPlanItem(planId: number, data: PlanItem) {
  return http.post(`/plans/${planId}/items`, data)
}

export function completePlanItem(planId: number, itemId: number) {
  return http.put(`/plans/${planId}/items/${itemId}/complete`)
}
