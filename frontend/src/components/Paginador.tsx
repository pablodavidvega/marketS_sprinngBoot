interface PaginadorProps {
  paginaActual: number
  totalPaginas: number
  onCambiarPagina: (pagina: number) => void
}

export default function Paginador({ paginaActual, totalPaginas, onCambiarPagina }: PaginadorProps) {
  if (totalPaginas <= 1) return null

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={() => onCambiarPagina(paginaActual - 1)}
        disabled={paginaActual === 0}
        className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
      >
        Anterior
      </button>
      <span className="text-sm text-gray-500">
        Página {paginaActual + 1} de {totalPaginas}
      </span>
      <button
        onClick={() => onCambiarPagina(paginaActual + 1)}
        disabled={paginaActual >= totalPaginas - 1}
        className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-40"
      >
        Siguiente
      </button>
    </div>
  )
}
