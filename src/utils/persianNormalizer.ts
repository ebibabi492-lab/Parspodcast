const ONES = ['', 'یک', 'دو', 'سه', 'چهار', 'پنج', 'شش', 'هفت', 'هشت', 'نه'];
const TEENS = ['ده', 'یازده', 'دوازده', 'سیزده', 'چهارده', 'پانصد', 'شانزده', 'هفده', 'هجده', 'نوزده'];
const TENS = ['', '', 'بیست', 'سی', 'چهل', 'پنجاه', 'شصت', 'هفتاد', 'هشتاد', 'نود'];
const HUNDREDS = ['', 'صد', 'دویست', 'سیصد', 'چهارصد', 'پانصد', 'ششصد', 'هفتصد', 'هشتصد', 'نهصد'];
const SCALES = ['', 'هزار', 'میلیون', 'میلیارد', 'تریلیون'];

export function threeDigitsToWords(num: number): string {
  const parts: string[] = [];
  const h = Math.floor(num / 100);
  const remainder = num % 100;

  if (h > 0) parts.push(HUNDREDS[h]);

  if (remainder >= 10 && remainder <= 19) {
    parts.push(TEENS[remainder - 10]);
  } else {
    const t = Math.floor(remainder / 10);
    const o = remainder % 10;
    if (t > 0) parts.push(TENS[t]);
    if (o > 0) parts.push(ONES[o]);
  }

  return parts.join(' و ');
}

export function numberToPersianWords(n: number): string {
  if (n === 0) return 'صفر';
  if (n < 0) return 'منفی ' + numberToPersianWords(-n);

  let num = n;
  const chunks: string[] = [];
  let scaleIndex = 0;

  while (num > 0) {
    const chunk = num % 1000;
    if (chunk > 0) {
      const words = threeDigitsToWords(chunk);
      const scale = SCALES[scaleIndex];
      chunks.unshift(scale ? `${words} ${scale}` : words);
    }
    num = Math.floor(num / 1000);
    scaleIndex++;
  }

  return chunks.join(' و ');
}

export function normalizePersianText(raw: string): string {
  if (!raw) return '';

  let text = raw;

  // Unify Arabic characters
  text = text.replace(/ي/g, 'ی');
  text = text.replace(/ك/g, 'ک');
  text = text.replace(/ة|ۀ/g, 'ه');
  text = text.replace(/ؤ/g, 'و');
  text = text.replace(/إ|أ/g, 'ا');
  text = text.replace(/ء/g, '');
  text = text.replace(/ـ/g, ''); // Remove Tatweel

  // Remove diacritics
  text = text.replace(/[\u064B-\u065F]/g, '');

  // Convert Persian/Arabic digits to ASCII first to find numbers
  const asciiConverted = text.replace(/[۰-۹٠-٩]/g, (d) => {
    const code = d.charCodeAt(0);
    if (code >= 0x06f0 && code <= 0x06f9) return String(code - 0x06f0);
    if (code >= 0x0660 && code <= 0x0669) return String(code - 0x0660);
    return d;
  });

  // Expand numbers to words
  const expanded = asciiConverted.replace(/\b\d+\b/g, (match) => {
    const num = parseInt(match, 10);
    if (!isNaN(num) && num < 1000000000) {
      return numberToPersianWords(num);
    }
    return match;
  });

  // Format punctuation for TTS pauses
  return expanded
    .replace(/\s+/g, ' ')
    .replace(/،/g, ' ، ')
    .replace(/؛/g, ' ؛ ')
    .replace(/\./g, ' . ')
    .trim();
}
