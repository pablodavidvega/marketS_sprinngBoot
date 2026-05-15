import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  message: string
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: '' }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  handleReset = () => {
    this.setState({ hasError: false, message: '' })
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback

      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <div className="text-center max-w-md">
            <p className="text-5xl mb-4">⚠️</p>
            <h1 className="text-2xl font-bold text-gray-800 mb-2">Algo salió mal</h1>
            <p className="text-gray-500 mb-6 text-sm">{this.state.message}</p>
            <button
              onClick={this.handleReset}
              className="btn-primary px-6 py-2"
            >
              Volver al inicio
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
