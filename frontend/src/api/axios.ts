import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  withCredentials: true, // envía la cookie auth_token automáticamente en cada request
})

// Si el backend devuelve 401 (o 403 sin estar autenticado), limpia la sesión y redirige al login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const isAuthEndpoint = error.config?.url?.includes('/api/auth/')
    if ((status === 401 || status === 403) && !isAuthEndpoint) {
      localStorage.removeItem('user')
      localStorage.removeItem('lastActive')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
