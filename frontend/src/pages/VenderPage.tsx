import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { convertirseEnVendedor } from '../api/usuario'

export default function VenderPage() {
  const navigate = useNavigate()
  const { login } = useAuth()

  const [city, setCity] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await convertirseEnVendedor(city)
      login(data.email, data.role, data.token)
      navigate('/vendedor')
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { error?: string } } })?.response?.data
      setError(data?.error ?? 'No se pudo activar tu cuenta de vendedor. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[70vh] flex items-center justify-center bg-gray-50 px-4 py-8">
      <div className="card w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-2 text-center">Empieza a vender</h1>
        <p className="text-gray-500 text-sm text-center mb-6">
          Activa tu cuenta de vendedor y publica tus productos. Podrás seguir comprando con la misma cuenta.
        </p>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Ciudad</label>
            <input
              className="input w-full"
              value={city}
              onChange={e => setCity(e.target.value)}
              placeholder="¿Desde dónde vas a vender?"
              required
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary mt-2">
            {loading ? 'Activando...' : 'Empezar a vender'}
          </button>
        </form>
      </div>
    </div>
  )
}
