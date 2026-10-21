/**
 * Operaciones y reglas de reservas de la interfaz.
 */
import { obtener, crear, eliminar } from './api.js';

/*
 * Configuración vigente (caché por carga de página).
 *
 * La API serializa las horas con segundos ("08:00:00"); se normalizan a
 * "HH:MM" para que el asistente compare cadenas simples, y las horas se
 * comparan en minutos para no depender del formato.
 */
let configCache = null;

/**
 * Reglas vigentes de las reservas: semestre, anticipación, duraciones,
 * días y fechas bloqueados, apertura/cierre y almuerzo. Se piden a la API
 * una sola vez por carga de página (si un administrador cambia la
 * configuración en caliente, la siguiente carga de la página la recoge).
 */
async function obtenerConfiguracion() {
  if (!configCache) {
    const reglas = await obtener('configuracion-reservas');
    configCache = {
      fechaInicioSemestre: reglas.fechaInicioSemestre,
      fechaCierreSemestre: reglas.fechaCierreSemestre,
      maxDiasAnticipacion: reglas.maxDiasAnticipacion,
      maxReservasActivas: reglas.maxReservasActivas,
      minDuracionMinutos: reglas.minDuracionMinutos,
      maxDuracionHoras: reglas.maxDuracionHoras,
      diasBloqueados: reglas.diasBloqueados,
      fechasBloqueadas: reglas.fechasBloqueadas,
      horaApertura: horaCorta(reglas.horaApertura),
      horaCierre: horaCorta(reglas.horaCierre),
      horaInicioAlmuerzo: horaCorta(reglas.horaInicioAlmuerzo),
      horaFinAlmuerzo: horaCorta(reglas.horaFinAlmuerzo)
    };
  }
  return configCache;
}

/*
 * Helpers de fecha y hora.
 */

/** Fecha de hoy en forma local "YYYY-MM-DD". */
function obtenerFechaHoy() {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

/** "HH:MM" o "HH:MM:SS" -> minutos desde medianoche. */
function aMinutos(hora) {
  const [horas, minutos] = hora.split(':').map(Number);
  return horas * 60 + minutos;
}

/** Recorta los segundos que agrega la serialización ("08:00:00" -> "08:00"). */
function horaCorta(hora) {
  return hora.slice(0, 5);
}

/** Día ISO-8601 de una fecha "YYYY-MM-DD" (1 = lunes … 7 = domingo). */
function diaIsoDeFecha(fecha) {
  const dia = new Date(`${fecha}T00:00:00`).getDay(); // 0 = domingo.
  return dia === 0 ? 7 : dia;
}

/** Duración percibida de un horario, en minutos (igual que el backend). */
function calcularDuracionMinutos(horaInicio, horaFin) {
  return aMinutos(horaFin) - aMinutos(horaInicio);
}

/*
 * Sesión y registro.
 */

/** Inicia sesión en la API: 200 con el usuario o ErrorApi 404/400. */
async function iniciarSesion(email, password) {
  return crear('sesion', { email, passwordHash: password });
}

/**
 * Registra un usuario nuevo con el rol "estudiante" (o el primer rol activo
 * si no existe): la API exige rol y estado explícitos en el cuerpo.
 */
async function registrarUsuario(nombre, email, password) {
  const roles = await obtener('roles');
  const rol = roles.find(r => r.nombre.toLowerCase() === 'estudiante') || roles[0];
  if (!rol) {
    throw new Error('No hay roles registrados: no es posible crear cuentas nuevas.');
  }
  return crear('usuarios', {
    nombre,
    email,
    passwordHash: password,
    rol: { idRol: rol.idRol },
    estado: true
  });
}

/*
 * Equipos, franjas de clase y reservas.
 */

/** Equipos de todas las salas (con su sala), ordenados por id. */
async function listarEquipos() {
  const equipos = await obtener('equipos');
  return equipos.sort((a, b) => a.idEquipo - b.idEquipo);
}

/** Franjas de clase activas de todas las salas (con su sala). */
async function listarFranjas() {
  return obtener('franjas-clase');
}

/** Reservas existentes (con usuario y equipo resueltos por la API). */
async function listarReservas() {
  return obtener('reservas');
}

/** Franjas de clase de un día de la semana, de todas las salas. */
function franjasDelDia(franjas, diaSemana) {
  return franjas.filter(franja => franja.diaSemana === diaSemana);
}

/** Reservas activas del usuario (fecha >= hoy), ordenadas por fecha y hora. */
async function obtenerReservasDeUsuario(idUsuario) {
  const hoy = obtenerFechaHoy();
  return (await listarReservas())
    .filter(r => r.usuario.idUsuario === idUsuario && r.fecha >= hoy)
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.horaInicio.localeCompare(b.horaInicio));
}

/**
 * Disponibilidad de un equipo para la fecha y el horario dados: espejo de las
 * validaciones 12 (franja de clase de su sala), 13 (equipo activo) y 15
 * (solapamiento) del backend. Un equipo de una sala sin clase en ese horario
 * puede seguir disponible aunque otra sala tenga clase (la tabla del paso 2
 * muestra la unión de todas las salas; esta función es la exacta).
 */
function verificarDisponibilidad(equipo, fecha, horaInicio, horaFin, franjas, reservas) {
  if (!equipo.estado) {
    return false;
  }

  const reservado = reservas.some(r =>
    r.equipo.idEquipo === equipo.idEquipo &&
    r.fecha === fecha &&
    horaCorta(r.horaInicio) < horaFin && horaCorta(r.horaFin) > horaInicio
  );
  if (reservado) {
    return false;
  }

  const diaSemana = diaIsoDeFecha(fecha);
  const enClase = franjas.some(f =>
    f.sala.idSala === equipo.sala.idSala &&
    f.diaSemana === diaSemana &&
    horaCorta(f.horaInicio) < horaFin && horaCorta(f.horaFin) > horaInicio
  );
  return !enClase;
}

/** Crea la reserva: la API aplica las 15 validaciones y responde el mensaje exacto. */
async function crearReserva(idUsuario, idEquipo, fecha, horaInicio, horaFin) {
  return crear('reservas', {
    usuario: { idUsuario },
    equipo: { idEquipo },
    fecha,
    horaInicio,
    horaFin
  });
}

/** Cancela una reserva por su id (su eliminación es física en la API). */
async function cancelarReserva(idReserva) {
  return eliminar('reservas', idReserva);
}

export {
  obtenerConfiguracion,
  obtenerFechaHoy, aMinutos, horaCorta, diaIsoDeFecha, calcularDuracionMinutos,
  iniciarSesion, registrarUsuario,
  listarEquipos, listarFranjas, listarReservas, franjasDelDia, obtenerReservasDeUsuario,
  verificarDisponibilidad, crearReserva, cancelarReserva
};
