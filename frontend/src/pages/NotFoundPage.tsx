import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="text-center">
        <p className="text-8xl font-bold text-gray-200 mb-4">404</p>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Página no encontrada</h1>
        <p className="text-gray-500 mb-6">La ruta que buscas no existe.</p>
        <Link to="/" className="btn-primary px-6 py-2">
          Volver al inicio
        </Link>
      </div>
    </div>
  )
}
