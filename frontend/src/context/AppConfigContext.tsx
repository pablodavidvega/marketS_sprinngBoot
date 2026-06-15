import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react'
import api from '../api/axios'

export interface AppConfigData {
  nombreApp: string
  colorPrimario: string
  logoUrl: string
  descripcion: string
}

interface AppConfigContextValue extends AppConfigData {
  refresh: () => Promise<void>
}

const defaults: AppConfigData = {
  nombreApp: 'MarketS',
  colorPrimario: '#2563eb',
  logoUrl: '',
  descripcion: 'Marketplace interno de tu empresa',
}

const AppConfigContext = createContext<AppConfigContextValue>({
  ...defaults,
  refresh: async () => {},
})

export const useAppConfig = () => useContext(AppConfigContext)

// ── Color utilities ──────────────────────────────────────────────────────────

function hexToRgb(hex: string): [number, number, number] {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!m) return [37, 99, 235]
  return [parseInt(m[1], 16), parseInt(m[2], 16), parseInt(m[3], 16)]
}

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b)
  const l = (max + min) / 2
  if (max === min) return [0, 0, l]
  const d = max - min
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
  let h = 0
  switch (max) {
    case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break
    case g: h = ((b - r) / d + 2) / 6; break
    case b: h = ((r - g) / d + 4) / 6; break
  }
  return [h * 360, s, l]
}

function hslToHex(h: number, s: number, l: number): string {
  h /= 360
  const hue2rgb = (p: number, q: number, t: number) => {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1 / 6) return p + (q - p) * 6 * t
    if (t < 1 / 2) return q
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
    return p
  }
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s
  const p = 2 * l - q
  const toHex = (x: number) => Math.round(x * 255).toString(16).padStart(2, '0')
  return '#' + toHex(hue2rgb(p, q, h + 1 / 3)) + toHex(hue2rgb(p, q, h)) + toHex(hue2rgb(p, q, h - 1 / 3))
}

export function generarTonos(hex: string): Record<string, string> {
  const [r, g, b] = hexToRgb(hex)
  const [h, s, l] = rgbToHsl(r, g, b)
  return {
    '--color-primary-50':  hslToHex(h, Math.min(s * 0.3, 1), 0.97),
    '--color-primary-100': hslToHex(h, Math.min(s * 0.5, 1), 0.93),
    '--color-primary-500': hslToHex(h, s, Math.min(l + 0.1, 0.65)),
    '--color-primary-600': hex,
    '--color-primary-700': hslToHex(h, s, Math.max(l - 0.1, 0.15)),
  }
}

export function aplicarColor(hex: string) {
  const tonos = generarTonos(hex)
  Object.entries(tonos).forEach(([prop, val]) =>
    document.documentElement.style.setProperty(prop, val)
  )
}

// ── Provider ─────────────────────────────────────────────────────────────────

export function AppConfigProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<AppConfigData>(defaults)

  const cargar = useCallback(async () => {
    try {
      const res = await api.get<AppConfigData>('/api/config/app')
      const cfg = { ...defaults, ...res.data }
      setConfig(cfg)
      if (cfg.colorPrimario) aplicarColor(cfg.colorPrimario)
      if (cfg.nombreApp)     document.title = cfg.nombreApp
    } catch {
      // Mantiene defaults — las CSS variables ya están en index.css
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  return (
    <AppConfigContext.Provider value={{ ...config, refresh: cargar }}>
      {children}
    </AppConfigContext.Provider>
  )
}
