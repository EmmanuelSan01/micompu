/**
 * Reglas y operaciones de reservas de la interfaz.
 *
 * Las constantes son el espejo de los defaults de config/ConfiguracionReservas
 * (más el bloque fijo de almuerzo de service/ValidadorReservas): solo guían al
 * usuario en el asistente. La autoridad es siempre el backend, que aplica las
 * 15 validaciones con la configuración vigente y responde con el mensaje exacto
 * cuando algo no cumple (los mensajes de error se muestran tal cual llegan).
 */
import { obtener, crear, eliminar } from './api.js';

const FECHA_INICIO_SEMESTRE = '2026-07-06';
const FECHA_CIERRE_SEMESTRE = '2026-10-31';
const MAX_DIAS_ANTICIPACION = 7;
const MIN_DURACION_MINUTOS = 30;
const MAX_DURACION_HORAS = 4;
const DIAS_BLOQUEADOS = [6, 7]; // ISO-8601: 6 = sábado, 7 = domingo.
const FECHAS_BLOQUEADAS = ['2026-07-13', '2026-07-20', '2026-08-07', '2026-08-17', '2026-10-12'];
const HORA_APERTURA = '08:00';
const HORA_CIERRE = '20:00';
const HORA_INICIO_ALMUERZO = '13:00';
const HORA_FIN_ALMUERZO = '14:00';

/*
 * Helpers de fecha y hora.
 *
 * La API serializa las horas con segundos ("08:00:00"); la interfaz trabaja
 * con "HH:MM" y las compara en minutos para no depender del formato.
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
  FECHA_INICIO_SEMESTRE, FECHA_CIERRE_SEMESTRE,
  MAX_DIAS_ANTICIPACION, MIN_DURACION_MINUTOS, MAX_DURACION_HORAS,
  DIAS_BLOQUEADOS, FECHAS_BLOQUEADAS,
  HORA_APERTURA, HORA_CIERRE, HORA_INICIO_ALMUERZO, HORA_FIN_ALMUERZO,
  obtenerFechaHoy, aMinutos, horaCorta, diaIsoDeFecha, calcularDuracionMinutos,
  iniciarSesion, registrarUsuario,
  listarEquipos, listarFranjas, listarReservas, franjasDelDia, obtenerReservasDeUsuario,
  verificarDisponibilidad, crearReserva, cancelarReserva
};
