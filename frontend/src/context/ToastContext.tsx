import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'

type ToastType = 'success' | 'error' | 'info'

interface Toast {
  id: number
  type: ToastType
  message: string
}

interface ToastContextType {
  success: (message: string) => void
  error: (message: string) => void
  info: (message: string) => void
}

const ToastContext = createContext<ToastContextType | null>(null)

let nextId = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const remove = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  const add = useCallback((type: ToastType, message: string) => {
    const id = ++nextId
    setToasts(prev => [...prev, { id, type, message }])
    setTimeout(() => remove(id), 4000)
  }, [remove])

  const success = useCallback((message: string) => add('success', message), [add])
  const error   = useCallback((message: string) => add('error', message),   [add])
  const info    = useCallback((message: string) => add('info', message),    [add])

  const icons: Record<ToastType, string> = {
    success: '✓',
    error:   '✕',
    info:    'ℹ',
  }

  const colors: Record<ToastType, string> = {
    success: 'bg-green-600',
    error:   'bg-red-600',
    info:    'bg-blue-600',
  }

  return (
    <ToastContext.Provider value={{ success, error, info }}>
      {children}

      {/* Toaster — fixed bottom-right */}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
        {toasts.map(t => (
          <div
            key={t.id}
            className={`flex items-start gap-3 px-4 py-3 rounded-xl text-white shadow-lg pointer-events-auto
              animate-fade-in ${colors[t.type]}`}
          >
            <span className="font-bold text-lg leading-none mt-0.5">{icons[t.type]}</span>
            <p className="text-sm flex-1 leading-snug">{t.message}</p>
            <button
              onClick={() => remove(t.id)}
              className="text-white/70 hover:text-white leading-none text-lg"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast debe usarse dentro de ToastProvider')
  return ctx
}
