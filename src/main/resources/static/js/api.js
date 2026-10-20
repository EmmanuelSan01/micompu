/**
 * Cliente REST de la API (/api): la única capa del frontend que habla con
 * fetch. Toda la lógica de negocio vive en el backend.
 *
 * Los errores llegan con la forma {estado, error, mensaje} (véase
 * controller/ManejadorErrores): ErrorApi conserva el código HTTP para que
 * cada página reaccione al caso exacto (404, 400, 409).
 */
const API_BASE_URL = '/api';

/** Error de la API: el mensaje llega redactado por el backend. */
class ErrorApi extends Error {
  constructor(mensaje, estado) {
    super(mensaje);
    this.estado = estado;
  }
}

/** Ejecuta una petición y devuelve el cuerpo JSON (o null si es 204). */
async function solicitar(recurso, opciones = {}) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_BASE_URL}/${recurso}`, opciones);
  } catch (error) {
    throw new Error('No fue posible conectar con la API: ' + error.message);
  }

  if (!respuesta.ok) {
    let mensaje = `Error HTTP ${respuesta.status}`;
    try {
      const cuerpo = await respuesta.json();
      if (cuerpo && cuerpo.mensaje) {
        mensaje = cuerpo.mensaje;
      }
    } catch {
      // Respuesta sin cuerpo JSON legible: se conserva el mensaje HTTP.
    }
    throw new ErrorApi(mensaje, respuesta.status);
  }

  return respuesta.status === 204 ? null : respuesta.json();
}

/** GET /api/{recurso}. */
async function obtener(recurso) {
  return solicitar(recurso);
}

/** POST /api/{recurso} con cuerpo JSON: devuelve el recurso creado (200/201). */
async function crear(recurso, datos) {
  return solicitar(recurso, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos)
  });
}

/** DELETE /api/{recurso}/{id}. */
async function eliminar(recurso, id) {
  return solicitar(`${recurso}/${id}`, { method: 'DELETE' });
}

export { API_BASE_URL, ErrorApi, obtener, crear, eliminar };
