import { ApiError, type ApiFieldError } from './api'

function getFieldErrorMessage(fieldError: ApiFieldError): string | null {
  const field = fieldError.field
  const code = fieldError.code

  if (code === 'REQUIRED') {
    return (
      {
        fullName: 'Овог нэрээ оруулна уу.',
        email: 'И-мэйл хаягаа оруулна уу.',
        password: 'Нууц үгээ оруулна уу.',
        role: 'Хэрэглэгчийн төрлөө сонгоно уу.',
      }[field ?? ''] ?? 'Шаардлагатай талбарыг бөглөнө үү.'
    )
  }

  if (code === 'INVALID_EMAIL') {
    return 'И-мэйл хаягаа зөв форматтай оруулна уу.'
  }

  if (code === 'PASSWORD_LENGTH') {
    return 'Нууц үг 8-72 тэмдэгт байх ёстой.'
  }

  if (code === 'INVALID_LENGTH') {
    return (
      {
        headline: 'Товч гарчиг 120 тэмдэгтээс хэтрэхгүй байх ёстой.',
        bio: 'Танилцуулга 2000 тэмдэгтээс хэтрэхгүй байх ёстой.',
        phone: 'Утас 40 тэмдэгтээс хэтрэхгүй байх ёстой.',
      }[field ?? ''] ?? 'Оруулсан мэдээллийн уртыг шалгана уу.'
    )
  }

  return null
}

export function getFriendlyErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) {
    return fallback
  }

  switch (error.code) {
    case 'EMAIL_ALREADY_REGISTERED':
      return 'Энэ и-мэйл хаяг аль хэдийн бүртгэлтэй байна.'
    case 'INVALID_CREDENTIALS':
      return 'И-мэйл эсвэл нууц үг буруу байна.'
    case 'TEACHER_PROFILE_NOT_FOUND':
      return 'Багшийн профайл олдсонгүй.'
    case 'FILE_REQUIRED':
      return 'Эхлээд файл сонгоно уу.'
    case 'IMAGE_FILE_REQUIRED':
      return 'Зөвхөн зураг файл сонгоно уу.'
    case 'IMAGE_TOO_LARGE':
      return 'Зургийн хэмжээ 5MB-аас ихгүй байх ёстой.'
    case 'IMAGE_UPLOAD_NOT_CONFIGURED':
      return 'Зураг байршуулах үйлчилгээ одоогоор тохируулагдаагүй байна.'
    case 'IMAGE_UPLOAD_NO_URL':
    case 'IMAGE_UPLOAD_FAILED':
    case 'BAD_GATEWAY':
      return 'Зураг ачаалах үед алдаа гарлаа.'
    case 'VALIDATION_ERROR': {
      const fieldMessage = error.fieldErrors.map(getFieldErrorMessage).find(Boolean)
      return fieldMessage ?? 'Оруулсан мэдээллээ шалгаад дахин оролдоно уу.'
    }
  }

  switch (error.status) {
    case 400:
      return error.fieldErrors.map(getFieldErrorMessage).find(Boolean) ?? 'Оруулсан мэдээллээ шалгаад дахин оролдоно уу.'
    case 401:
      return 'Энэ үйлдлийг хийхийн тулд дахин нэвтэрнэ үү.'
    case 403:
      return 'Энэ үйлдлийг хийх эрх хүрэлцэхгүй байна.'
    case 404:
      return 'Хүссэн мэдээлэл олдсонгүй.'
    case 409:
      return 'Ийм мэдээлэл аль хэдийн бүртгэлтэй байна.'
    case 413:
      return 'Оруулсан файл хэт том байна.'
    case 503:
      return 'Үйлчилгээ түр ажиллахгүй байна. Дараа дахин оролдоно уу.'
    default:
      return fallback
  }
}
