import { createContext, useContext, useState, useCallback, useEffect } from 'react'

type ToastType = 'success' | 'error'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside ToastProvider')
  return ctx
}

let nextId = 0
const DURATION = 4000

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = ++nextId
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), DURATION)
  }, [])

  const dismiss = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3 pointer-events-none">
        {toasts.map(t => (
          <ToastCard key={t.id} toast={t} onDismiss={dismiss} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastCard({ toast, onDismiss }: { toast: ToastItem; onDismiss: (id: number) => void }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const t = setTimeout(() => setVisible(true), 10)
    return () => clearTimeout(t)
  }, [])

  const isSuccess = toast.type === 'success'

  return (
    <div
      className={`pointer-events-auto flex items-start gap-3 rounded-xl border bg-white px-4 py-3 shadow-lg
        transition-all duration-300
        ${visible ? 'translate-x-0 opacity-100' : 'translate-x-8 opacity-0'}
        ${isSuccess ? 'border-green-200' : 'border-red-200'}`}
      style={{ minWidth: '280px', maxWidth: '400px' }}
    >
      <svg
        className={`w-5 h-5 flex-shrink-0 mt-0.5 ${isSuccess ? 'text-green-500' : 'text-red-500'}`}
        fill="none" viewBox="0 0 24 24" stroke="currentColor"
      >
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d={isSuccess
            ? 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
            : 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
          }
        />
      </svg>

      <span className={`flex-1 text-sm font-medium ${isSuccess ? 'text-green-800' : 'text-red-800'}`}>
        {toast.message}
      </span>

      <button
        onClick={() => onDismiss(toast.id)}
        className="text-gray-400 hover:text-gray-600 p-0.5 -mt-0.5 flex-shrink-0"
      >
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  )
}
