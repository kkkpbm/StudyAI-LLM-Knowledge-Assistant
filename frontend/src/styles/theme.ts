/**
 * Knowledge Atlas brand system.
 */

export const BRAND_COLORS = {
  50:  '#EEF3FF',
  100: '#DDE5FF',
  200: '#C4D0FF',
  300: '#9DAFFF',
  400: '#6F8CFF',
  500: '#4B6FFF',
  600: '#315EFB',
  700: '#2447C9',
  800: '#1B3292',
  900: '#151F5E',
}

export const SEMANTIC_COLORS = {
  success:      '#18A058',
  successLight: '#EAF8F0',
  warning:      '#E29B19',
  warningLight: '#FFF6DE',
  error:        '#E65A42',
  errorLight:   '#FFF0EC',
  info:         '#4C647F',
  infoLight:    '#EEF3F8',
}

export const NEUTRAL_COLORS = {
  50:  '#F8FAFD',
  100: '#F0F3F8',
  200: '#E3E8F0',
  300: '#CBD5E1',
  400: '#94A3B8',
  500: '#64748B',
  600: '#475569',
  700: '#334155',
  800: '#1E293B',
  900: '#111827',
}

export const SHADOWS = {
  xs:   '0 1px 2px rgba(15,23,42,0.04)',
  sm:   '0 4px 12px rgba(15,23,42,0.06)',
  md:   '0 10px 28px rgba(15,23,42,0.08)',
  lg:   '0 18px 48px rgba(15,23,42,0.12)',
  xl:   '0 26px 70px rgba(15,23,42,0.18)',
  card: '0 1px 0 rgba(255,255,255,.86) inset, 0 8px 22px rgba(15,23,42,.055)',
}

export const RADIUS = {
  xs:  '4px',
  sm:  '6px',
  md:  '8px',
  lg:  '12px',
  xl:  '16px',
  '2xl': '22px',
}

export const SPACING = {
  1:  '4px',
  2:  '8px',
  3:  '12px',
  4:  '16px',
  5:  '20px',
  6:  '24px',
  8:  '32px',
  10: '40px',
  12: '48px',
}

export const TYPOGRAPHY = {
  fontSans: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif",
  fontMono: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
  sizes: {
    xs:   '0.75rem',
    sm:   '0.875rem',
    base: '1rem',
    lg:   '1.125rem',
    xl:   '1.25rem',
    '2xl': '1.5rem',
    '3xl': '1.875rem',
  },
}

export const BREAKPOINTS = {
  mobile:  '768px',
  tablet:  '1024px',
  desktop: '1200px',
}

export default {
  brand: BRAND_COLORS,
  semantic: SEMANTIC_COLORS,
  neutral: NEUTRAL_COLORS,
  shadows: SHADOWS,
  radius: RADIUS,
  spacing: SPACING,
  typography: TYPOGRAPHY,
  breakpoints: BREAKPOINTS,
}
