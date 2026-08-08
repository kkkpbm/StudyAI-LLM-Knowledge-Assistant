import http from './index'

export interface ReviewReminder {
  id?: number
  userId?: number
  noteId: number
  nextReviewAt: string
  intervalDays?: number
  easeFactor?: number
  repetitionCount?: number
}

export function getUpcomingReviews(limit = 10) {
  return http.get('/reviews/upcoming', { params: { limit } })
}

export function getAllReviews() {
  return http.get('/reviews')
}

export function createReview(noteId: number) {
  return http.post('/reviews', null, { params: { noteId } })
}

export function completeReview(id: number, quality = 4) {
  return http.put(`/reviews/${id}/complete`, null, { params: { quality } })
}

export function deleteReview(id: number) {
  return http.delete(`/reviews/${id}`)
}
