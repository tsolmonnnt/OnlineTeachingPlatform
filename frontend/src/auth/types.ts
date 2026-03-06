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
  avatarUrl: string | null
  hourlyRate: string | number | null
  languages: string[]
  location: string | null
  phone: string | null
  yearsExperience: number | null
  createdAt: string
  updatedAt: string
}

