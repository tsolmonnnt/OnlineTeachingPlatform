import FullCalendar from '@fullcalendar/react'
import interactionPlugin from '@fullcalendar/interaction'
import timeGridPlugin from '@fullcalendar/timegrid'
import type { AvailabilitySlot } from '../auth/types'

const SLOT_MIN_HOUR = 8
const SLOT_MAX_HOUR = 20
const CALENDAR_HEIGHT = 520

export type CalendarSlotSelection = {
  start: Date
  startStr: string
}

type Props = {
  slots: AvailabilitySlot[]
  mode: 'teacher' | 'student'
  onCreateSlot?: (selection: CalendarSlotSelection) => void
  onDeleteSlot?: (slot: AvailabilitySlot) => void
  onSelectSlot?: (slot: AvailabilitySlot) => void
  onRangeChange?: (start: Date, end: Date) => void
}

function isWithinBusinessHours(date: Date): boolean {
  const hour = date.getHours()
  if (hour < SLOT_MIN_HOUR) return false
  if (hour >= SLOT_MAX_HOUR) return false
  return true
}

export function WeeklyAvailabilityCalendar({
  slots,
  mode,
  onCreateSlot,
  onDeleteSlot,
  onSelectSlot,
  onRangeChange,
}: Props) {
  return (
    <div className="weeklyCalendarWrap">
      <FullCalendar
        plugins={[timeGridPlugin, interactionPlugin]}
        initialView="timeGridWeek"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: '',
        }}
        allDaySlot={false}
        slotMinTime="08:00:00"
        slotMaxTime="20:00:00"
        slotDuration="00:30:00"
        slotLabelInterval="00:30:00"
        slotLabelFormat={{
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        }}
        eventTimeFormat={{
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        }}
        height={CALENDAR_HEIGHT}
        expandRows={false}
        stickyHeaderDates
        selectable={mode === 'teacher'}
        selectMirror={mode === 'teacher'}
        selectAllow={(selectInfo) => {
          const minutes = Math.round((selectInfo.end.getTime() - selectInfo.start.getTime()) / 60000)
          if (minutes !== 30) return false
          return isWithinBusinessHours(selectInfo.start)
        }}
        select={(selectInfo) => {
          if (mode !== 'teacher' || !onCreateSlot) return
          onCreateSlot({ start: selectInfo.start, startStr: selectInfo.startStr })
        }}
        eventClick={(clickInfo) => {
          const slot = clickInfo.event.extendedProps.slot as AvailabilitySlot | undefined
          if (!slot) return
          if (mode === 'teacher' && !slot.booked && onDeleteSlot) {
            onDeleteSlot(slot)
          }
          if (mode === 'student' && !slot.booked && onSelectSlot) {
            onSelectSlot(slot)
          }
        }}
        eventClassNames={(arg) => {
          const slot = arg.event.extendedProps.slot as AvailabilitySlot | undefined
          if (!slot) return []
          return slot.booked ? ['fc-slot-booked'] : ['fc-slot-available']
        }}
        events={slots.map((slot) => ({
          id: String(slot.id),
          title: slot.booked
            ? `Захиалагдсан${slot.courseSubjectName ? ` · ${slot.courseSubjectName}` : ''}`
            : `Сул${slot.courseSubjectName ? ` · ${slot.courseSubjectName}` : ''}`,
          start: slot.startTime,
          end: slot.endTime,
          slot,
        }))}
        datesSet={(info) => {
          onRangeChange?.(info.start, info.end)
        }}
      />
    </div>
  )
}
