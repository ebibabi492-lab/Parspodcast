import { JalaliDateParts } from '../types';

export const PERSIAN_MONTHS = [
  'فروردین', 'اردیبهشت', 'خرداد',
  'تیر', 'مرداد', 'شهریور',
  'مهر', 'آبان', 'آذر',
  'دی', 'بهمن', 'اسفند'
];

export const PERSIAN_WEEKDAYS = [
  'شنبه', 'یکشنبه', 'دوشنبه',
  'سه‌شنبه', 'چهارشنبه', 'پنج‌شنبه', 'جمعه'
];

export function toPersianDigits(input: string | number): string {
  const str = input.toString();
  const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
  return str.replace(/[0-9]/g, (w) => persianDigits[parseInt(w, 10)]);
}

export function gregorianToJalali(gDate: Date = new Date()): JalaliDateParts {
  const gYear = gDate.getFullYear();
  const gMonth = gDate.getMonth() + 1;
  const gDay = gDate.getDate();
  const calDayOfWeek = gDate.getDay(); // 0 is Sunday, 6 is Saturday

  // Map JS Day of Week (0=Sun, 6=Sat) to Persian (1=Sat, ..., 7=Fri)
  const persianDayOfWeek = calDayOfWeek === 6 ? 1 : calDayOfWeek + 2;

  const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  const jDaysInMonth = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29];

  const gy = gYear - 1600;
  const gm = gMonth - 1;
  const gd = gDay - 1;

  let gDayNo = 365 * gy + Math.floor((gy + 3) / 4) - Math.floor((gy + 99) / 100) + Math.floor((gy + 399) / 400);
  for (let i = 0; i < gm; ++i) {
    gDayNo += gDaysInMonth[i];
  }
  if (gm > 1 && ((gy % 4 === 0 && gy % 100 !== 0) || gy % 400 === 0)) {
    gDayNo++;
  }
  gDayNo += gd;

  let jDayNo = gDayNo - 79;
  const jNp = Math.floor(jDayNo / 12053);
  jDayNo %= 12053;

  let jy = 979 + 33 * jNp + 4 * Math.floor(jDayNo / 1461);
  jDayNo %= 1461;

  if (jDayNo >= 366) {
    jy += Math.floor((jDayNo - 1) / 365);
    jDayNo = (jDayNo - 1) % 365;
  }

  let jm = 0;
  for (let i = 0; i < 11 && jDayNo >= jDaysInMonth[i]; ++i) {
    jDayNo -= jDaysInMonth[i];
    jm = i + 1;
  }
  const jd = jDayNo + 1;
  const month = jm + 1;

  const weekdayName = PERSIAN_WEEKDAYS[persianDayOfWeek - 1] || 'شنبه';
  const monthName = PERSIAN_MONTHS[month - 1] || 'فروردین';
  const formattedStr = `${weekdayName} ${toPersianDigits(jd)} ${monthName} ${toPersianDigits(jy)}`;

  return {
    year: jy,
    month,
    day: jd,
    dayOfWeek: persianDayOfWeek,
    monthName,
    weekdayName,
    formattedStr
  };
}

export function formatJalaliShort(parts: JalaliDateParts): string {
  return `${toPersianDigits(parts.year)}/${toPersianDigits(parts.month.toString().padStart(2, '0'))}/${toPersianDigits(parts.day.toString().padStart(2, '0'))}`;
}
