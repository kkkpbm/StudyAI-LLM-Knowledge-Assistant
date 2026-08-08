import http from './index'

export function getDashboardOverview() {
  return http.get('/dashboard/overview')
}

export function getCategoryDistribution() {
  return http.get('/dashboard/category-distribution')
}
