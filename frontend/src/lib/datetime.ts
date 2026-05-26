const NAIVE_LOCAL_DATE_TIME_RE =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/

function pad2(value: number) {
  return String(value).padStart(2, '0')
}

function isValidDate(date: Date) {
  return !Number.isNaN(date.getTime())
}

export function parseLocalDateTime(value: string) {
  const normalized = value.trim().slice(0, 19)
  const match = normalized.match(NAIVE_LOCAL_DATE_TIME_RE)
  if (match) {
    const [, year, month, day, hour, minute, second = '00'] = match
    return new Date(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second),
      0,
    )
  }

  return new Date(value)
}

export function dateToApiLocalDateTime(date: Date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
}

export function datetimeLocalInputToApi(value: string) {
  const match = value.trim().match(NAIVE_LOCAL_DATE_TIME_RE)
  if (match) {
    const [, year, month, day, hour, minute, second = '00'] = match
    return `${year}-${month}-${day}T${hour}:${minute}:${second}`
  }

  const parsed = parseLocalDateTime(value)
  return isValidDate(parsed) ? dateToApiLocalDateTime(parsed) : value
}

export function apiDateTimeToDatetimeLocalValue(value: string) {
  const parsed = parseLocalDateTime(value)
  if (!isValidDate(parsed)) return ''

  return `${parsed.getFullYear()}-${pad2(parsed.getMonth() + 1)}-${pad2(parsed.getDate())}T${pad2(parsed.getHours())}:${pad2(parsed.getMinutes())}`
}

export function formatLocalDateTime(
  value: string,
  locale = 'mn-MN',
  options?: Intl.DateTimeFormatOptions,
) {
  const parsed = parseLocalDateTime(value)
  if (!isValidDate(parsed)) return value
  return parsed.toLocaleString(locale, options)
}
