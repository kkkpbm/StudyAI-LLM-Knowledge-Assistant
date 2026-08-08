import http from './index'

export const generateFlashcards = (noteId: number, count = 6) =>
  http.post(`/learning-center/flashcards/generate/${noteId}`, null, { params: { count } })

export const getDueFlashcards = (limit = 30) =>
  http.get('/learning-center/flashcards/due', { params: { limit } })

export const reviewFlashcard = (id: number, quality: number) =>
  http.put(`/learning-center/flashcards/${id}/review`, { quality })

export const semanticSearch = (query: string, topK = 8) =>
  http.get('/learning-center/semantic-search', { params: { query, topK } })

export const getWeeklyReport = () => http.get('/learning-center/weekly-report')
export const getAchievements = () => http.get('/learning-center/achievements')
export const getTrash = () => http.get('/learning-center/trash')
export const restoreTrash = (id: number) => http.post(`/learning-center/trash/${id}/restore`)
export const deleteTrashPermanently = (id: number) => http.delete(`/learning-center/trash/${id}`)
export const getNoteVersions = (noteId: number) => http.get(`/learning-center/notes/${noteId}/versions`)
export const restoreNoteVersion = (noteId: number, versionId: number) =>
  http.post(`/learning-center/notes/${noteId}/versions/${versionId}/restore`)
export const importNotes = (notes: any[]) => http.post('/learning-center/notes/import', notes)
export const exportNotes = () => http.get('/learning-center/notes/export')
