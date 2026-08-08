import http from './index'

export function getProfile() {
  return http.get('/users/profile')
}

export function updateProfile(data: { email?: string; avatar?: string }) {
  return http.put('/users/profile', data)
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/users/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.put('/users/password', data)
}

export function getLearningStats() {
  return http.get('/users/stats')
}

export function getWeeklyActivity() {
  return http.get('/users/stats/weekly')
}

export function getCategoryDistribution() {
  return http.get('/users/stats/categories')
}
