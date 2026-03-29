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
  createdAt: string
  updatedAt: string
}

export type TeacherSummary = {
  id: number
  userId: number
  fullName: string
  headline: string | null
  subjects: string[]
  skills: string[]
  hourlyRate: string | number | null
  yearsExperience: number | null
  location: string | null
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
}

export type AvailabilitySlot = {
  id: number
  teacherProfileId: number
  startTime: string
  endTime: string
  booked: boolean
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
  createdAt: string
  updatedAt: string
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
