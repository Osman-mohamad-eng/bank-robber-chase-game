const integer = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });
const dollars = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export const number = (value: number): string => integer.format(value);
export const money = (value: number): string => dollars.format(value);

export function duration(milliseconds: number | null): string {
  if (milliseconds === null) return '—';
  const total = Math.max(0, Math.round(milliseconds));
  const minutes = Math.floor(total / 60_000);
  const seconds = Math.floor(total / 1_000) % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}.${String(total % 1_000).padStart(3, '0')}`;
}

export function dateTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? 'Unknown date' : new Intl.DateTimeFormat(undefined, {
    month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit',
  }).format(date);
}

export const isUuid = (value: string): boolean => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);

// API text is untrusted, even in a local demo. All generated markup uses escaped values.
export const escapeHtml = (value: string | number): string => String(value).replace(/[&<>"']/g,
  character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character] ?? character);

export function pageLabel(page: { items: unknown[]; offset: number; total: number }): string {
  if (page.total === 0) return '0 results';
  if (page.items.length === 0) return `0 on this page · ${number(page.total)} total`;
  return `${number(page.offset + 1)}–${number(page.offset + page.items.length)} of ${number(page.total)}`;
}
