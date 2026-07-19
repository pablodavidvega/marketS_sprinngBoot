import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  // withCredentials queda por si algún día front/back comparten dominio; la cookie
  // auth_token es cross-site (Vercel + Render) y Safari/iOS la bloquea por ITP, así que
  // la autenticación real viaja como Authorization: Bearer (interceptor de abajo).
  withCredentials: true,
})

// Agrega el JWT guardado en localStorage como Authorization: Bearer en cada request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Si el backend devuelve 401 (o 403 sin estar autenticado), limpia la sesión y redirige al login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const isAuthEndpoint = error.config?.url?.includes('/api/auth/')
    if ((status === 401 || status === 403) && !isAuthEndpoint) {
      localStorage.removeItem('user')
      localStorage.removeItem('authToken')
      localStorage.removeItem('lastActive')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
