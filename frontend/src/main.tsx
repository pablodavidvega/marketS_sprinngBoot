import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import { CarritoProvider } from './context/CarritoContext'
import { AppConfigProvider } from './context/AppConfigContext'
import { FavoritosProvider } from './context/FavoritosContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AppConfigProvider>
        <AuthProvider>
          <CarritoProvider>
            <FavoritosProvider>
              <App />
            </FavoritosProvider>
          </CarritoProvider>
        </AuthProvider>
      </AppConfigProvider>
    </BrowserRouter>
  </StrictMode>,
)
