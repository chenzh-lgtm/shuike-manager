export interface TeachingPlan {
  id?: number
  teacherId?: number
  courseId?: number
  semesterId?: number
  classInfo?: string
  textbookInfo?: string
  summary?: string
  status?: string
  submitTime?: string
  collegeReviewTime?: string
  officeReviewTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface TeachingPlanFile {
  id?: number
  planId?: number
  fileName: string
  fileUrl: string
  fileType: string
  fileSize: number
}
