export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN'

export type User = {
  id: number
  fullName: string
  email: string
  role: Role
}

export type AuthResponse = {
  accessToken: string
  user: User
}

export type TeacherProfile = {
  id: number
  userId: number
  headline: string | null
  bio: string | null
  subjects: string[]
  skills: string[]
  avatarUrl: string | null
  hourlyRate: string | number | null
  languages: string[]
  location: string | null
  phone: string | null
  yearsExperience: number | null
  verified: boolean
  createdAt: string
  updatedAt: string
}

export type TeacherSummary = {
  id: number
  userId: number
  fullName: string
  headline: string | null
  avatarUrl: string | null
  subjects: string[]
  skills: string[]
  hourlyRate: string | number | null
  yearsExperience: number | null
  location: string | null
  verified: boolean
  averageRating: number | null
  reviewCount: number
}

export type TeacherDetail = {
  id: number
  userId: number
  fullName: string
  headline: string | null
  bio: string | null
  subjects: string[]
  skills: string[]
  avatarUrl: string | null
  hourlyRate: string | number | null
  languages: string[]
  location: string | null
  phone: string | null
  yearsExperience: number | null
  verified: boolean
  averageRating: number | null
  reviewCount: number
}

export type ReviewItem = {
  id: number
  bookingId: number
  studentName: string
  rating: number
  comment: string | null
  createdAt: string
}

export type TeachingMaterial = {
  id: number
  teacherProfileId: number
  courseSubjectId: number | null
  courseSubjectName: string | null
  title: string
  description: string | null
  secureUrl: string | null
  contentType: string | null
  sizeBytes: number | null
  createdAt: string
}

export type PublicPlatformStats = {
  verifiedTeacherCount: number
  totalBookings: number
  studentCount: number
  totalTeachers: number
}

export type AdminStats = {
  totalUsers: number
  studentCount: number
  teacherCount: number
  adminCount: number
  totalBookings: number
  verifiedTeacherCount: number
  totalTeachers: number
}

export type AdminTeacherRow = {
  teacherProfileId: number
  userId: number
  fullName: string
  email: string
  verified: boolean
}

export type UserRow = {
  id: number
  fullName: string
  email: string
  role: Role
}

export type QuizSummary = {
  id: number
  courseSubjectId: number | null
  courseSubjectName: string | null
  title: string
  description: string | null
  timeLimitMinutes: number
  published: boolean
  questionCount: number
  createdAt: string
}

export type QuizPublic = {
  id: number
  teacherProfileId: number
  title: string
  description: string | null
  timeLimitMinutes: number
  questions: {
    id: number
    orderIndex: number
    questionType: 'MCQ' | 'TRUE_FALSE' | 'SHORT_ANSWER'
    prompt: string
    optionsJson: string | null
  }[]
}

export type AttemptResult = {
  attemptId: number
  quizId: number
  score: number
  maxScore: number
  percent: number
}

export type AvailabilitySlot = {
  id: number
  teacherProfileId: number
  startTime: string
  endTime: string
  booked: boolean
  courseSubjectId: number | null
  courseSubjectName: string | null
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export type Booking = {
  id: number
  status: BookingStatus
  subject: string
  note: string | null
  studentUserId: number
  studentName: string
  teacherId: number
  teacherName: string
  slotId: number
  slotStartTime: string
  slotEndTime: string
  courseSubjectId: number | null
  courseSubjectName: string | null
  createdAt: string
  updatedAt: string
}

export type TeacherDashboard = {
  confirmedLessonsToday: number
  confirmedLessonsTomorrow: number
  pendingBookingsAsTeacher: number
  averageRating: number | null
  reviewCount: number
  unreadNotifications: number
  uniqueStudentsConfirmed: number
  recentBookings: Booking[]
}

export type StudentDashboard = {
  upcomingConfirmedLessons: number
  pendingBookingsAsStudent: number
  unreadNotifications: number
  recentBookings: Booking[]
}

export type NotificationItem = {
  id: number
  title: string
  message: string
  isRead: boolean
  createdAt: string
}

export type CourseCategory = {
  id: number
  name: string
  description: string | null
}

export type CourseSubject = {
  id: number
  name: string
  description: string | null
  categoryId: number
  categoryName: string
}
