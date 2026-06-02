/** API and schedule wall-clock times are stored as Mongolia local time (no offset in the string). */
export const MONGOLIA_TIME_ZONE = 'Asia/Ulaanbaatar'

const NAIVE_LOCAL_DATE_TIME_RE =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/

const MONGOLIA_PARTS_FORMATTER = new Intl.DateTimeFormat('en-CA', {
  timeZone: MONGOLIA_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
})

function pad2(value: number) {
  return String(value).padStart(2, '0')
}

function isValidDate(date: Date) {
  return !Number.isNaN(date.getTime())
}

export function readMongoliaParts(date: Date) {
  const parts = MONGOLIA_PARTS_FORMATTER.formatToParts(date)
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? '00'
  return {
    year: Number(get('year')),
    month: Number(get('month')),
    day: Number(get('day')),
    hour: Number(get('hour')),
    minute: Number(get('minute')),
    second: Number(get('second')),
  }
}

/** Format an instant as naive YYYY-MM-DDTHH:mm:ss in Mongolia wall-clock time for the API. */
export function dateToApiLocalDateTime(date: Date) {
  const p = readMongoliaParts(date)
  return `${p.year}-${pad2(p.month)}-${pad2(p.day)}T${pad2(p.hour)}:${pad2(p.minute)}:${pad2(p.second)}`
}

/** Wall-clock fields from a Date as shown on the local calendar grid (WYSIWYG for slot pick). */
export function dateToApiWallClock(date: Date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
}

const ISO_NAIVE_PREFIX_RE = /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?/

/** Map FullCalendar selection to naive API time matching the grid cell the user clicked. */
export function calendarSelectionToApiDateTime(startStr: string, fallback: Date) {
  const trimmed = startStr.trim()
  const match = trimmed.match(ISO_NAIVE_PREFIX_RE)
  if (match) {
    const [, datePart, timePart, seconds = '00'] = match
    return `${datePart}T${timePart}:${seconds}`
  }
  return isValidDate(fallback) ? dateToApiWallClock(fallback) : dateToApiWallClock(new Date())
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
  return parsed.toLocaleString(locale, {
    timeZone: MONGOLIA_TIME_ZONE,
    ...options,
  })
}
