import {
  FECHA_INICIO_SEMESTRE, FECHA_CIERRE_SEMESTRE,
  MAX_DIAS_ANTICIPACION,
  DIAS_BLOQUEADOS, FECHAS_BLOQUEADAS,
  HORA_APERTURA, HORA_CIERRE,
  HORA_INICIO_ALMUERZO, HORA_FIN_ALMUERZO,
  MIN_DURACION_MINUTOS, MAX_DURACION_HORAS,
  diaIsoDeFecha, horaCorta, calcularDuracionMinutos,
  listarEquipos, listarFranjas, listarReservas, franjasDelDia,
  verificarDisponibilidad, crearReserva
} from './reservas.js';

// Estado del formulario
const estado = {
  seccionActual: 1,
  fecha: null,
  horaInicio: null,
  horaFin: null,
  equipoId: null,
  usuarioId: null
};

// Usuario activo
let usuarioActivo = null;

// Elementos del DOM
let elementos = {};

// Variable para hover en tabla de horarios
let hoverIndex = null;

// Franjas de clase del día que se muestra (la unión de todas las salas)
let franjasDiaActual = [];

// Cache de las franjas de clase: se piden a la API una sola vez
let franjasCache = null;

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
  // Validar sesión activa
  usuarioActivo = JSON.parse(sessionStorage.getItem('usuario_activo'));

  if (!usuarioActivo) {
    window.location.href = 'login.html';
    return;
  }

  // Configurar usuario en estado y navbar
  estado.usuarioId = usuarioActivo.idUsuario;
  document.getElementById('nombre-usuario').textContent = usuarioActivo.nombre || usuarioActivo.email;

  // Configurar cierre de sesión
  document.getElementById('btn-cerrar-sesion').addEventListener('click', cerrarSesion);

  cachearElementos();
  configurarEventos();
  renderizarSelectorFecha();
});

function cerrarSesion() {
  sessionStorage.removeItem('usuario_activo');
  window.location.href = 'login.html';
}

function cachearElementos() {
  elementos = {
    form: document.getElementById('form-reserva'),
    errorGlobal: document.getElementById('error-global'),
    // Sección 1
    seccionFecha: document.getElementById('seccion-fecha'),
    contenedorFechas: document.getElementById('contenedor-fechas'),
    errorFecha: document.getElementById('error-fecha'),
    btnSiguiente1: document.getElementById('btn-siguiente-1'),
    // Sección 2
    seccionHorario: document.getElementById('seccion-horario'),
    tbodyHorarios: document.getElementById('tbody-horarios'),
    horarioSeleccionado: document.getElementById('horario-seleccionado'),
    errorHorario: document.getElementById('error-horario'),
    btnAtras2: document.getElementById('btn-atras-2'),
    btnSiguiente2: document.getElementById('btn-siguiente-2'),
    // Sección 3
    seccionEquipo: document.getElementById('seccion-equipo'),
    grillaEquipos: document.getElementById('grilla-equipos'),
    errorEquipo: document.getElementById('error-equipo'),
    btnAtras3: document.getElementById('btn-atras-3'),
    btnCrearReserva: document.getElementById('btn-crear-reserva'),
    // Steps
    steps: document.querySelectorAll('.step')
  };
}

function configurarEventos() {
  // Sección 1
  elementos.btnSiguiente1.addEventListener('click', () => irASeccion(2));

  // Sección 2
  elementos.btnAtras2.addEventListener('click', () => irASeccion(1));
  elementos.btnSiguiente2.addEventListener('click', () => irASeccion(3));

  // Sección 3
  elementos.btnAtras3.addEventListener('click', () => irASeccion(2));

  // Form submit
  elementos.form.addEventListener('submit', enviarReserva);
}

// ==============================
// SECCIÓN 1: SELECTOR DE FECHA
// ==============================

function renderizarSelectorFecha() {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  elementos.contenedorFechas.innerHTML = '';

  // Generar los próximos 7 días
  for (let i = 1; i <= MAX_DIAS_ANTICIPACION; i++) {
    const fecha = new Date(hoy);
    fecha.setDate(hoy.getDate() + i);

    const fechaStr = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
    const diaSemana = diaIsoDeFecha(fechaStr);
    const esFinDeSemana = DIAS_BLOQUEADOS.includes(diaSemana);
    const esFestivo = FECHAS_BLOQUEADAS.includes(fechaStr);
    const fueraDeSemestre = fechaStr < FECHA_INICIO_SEMESTRE || fechaStr > FECHA_CIERRE_SEMESTRE;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'fecha-btn';
    btn.dataset.fecha = fechaStr;

    const nombreDia = fecha.toLocaleDateString('es-CO', { weekday: 'short' });
    const numeroDia = fecha.getDate();
    const nombreMes = fecha.toLocaleDateString('es-CO', { month: 'short' });

    btn.innerHTML = `
      <span class="fecha-dia">${nombreDia}</span>
      <span class="fecha-numero">${numeroDia}</span>
      <span class="fecha-mes">${nombreMes}</span>
    `;

    if (esFinDeSemana || esFestivo || fueraDeSemestre) {
      btn.classList.add('bloqueado');
      btn.disabled = true;
      if (esFinDeSemana) {
        btn.title = 'Fin de semana';
      } else if (esFestivo) {
        btn.title = 'Festivo';
      } else {
        btn.title = 'Fuera del período del semestre';
      }
    } else {
      btn.addEventListener('click', () => seleccionarFecha(fechaStr, btn));
    }

    elementos.contenedorFechas.appendChild(btn);
  }
}

function seleccionarFecha(fecha, boton) {
  // Deseleccionar anterior
  elementos.contenedorFechas.querySelectorAll('.fecha-btn').forEach(btn => {
    btn.classList.remove('seleccionado');
  });

  // Seleccionar nuevo
  boton.classList.add('seleccionado');
  estado.fecha = fecha;
  elementos.errorFecha.textContent = '';
  elementos.btnSiguiente1.disabled = false;
}

// ==============================
// SECCIÓN 2: TABLA DE HORARIOS
// ==============================

function irASeccion(numero) {
  // Ocultar sección actual
  document.querySelector('.seccion.active')?.classList.remove('active');

  // Actualizar steps
  elementos.steps.forEach((step, i) => {
    step.classList.remove('active', 'completed');
    if (i + 1 < numero) step.classList.add('completed');
    if (i + 1 === numero) step.classList.add('active');
  });

  estado.seccionActual = numero;

  // Mostrar nueva sección
  if (numero === 1) {
    elementos.seccionFecha.classList.add('active');
  } else if (numero === 2) {
    elementos.seccionHorario.classList.add('active');
    renderizarTablaHorarios();
  } else if (numero === 3) {
    elementos.seccionEquipo.classList.add('active');
    renderizarGrillaEquipos();
  }

  limpiarErrorGlobal();
}

async function cargarFranjas() {
  if (!franjasCache) {
    franjasCache = await listarFranjas();
  }
  return franjasCache;
}

/**
 * Franjas de clase del día seleccionado (la unión de todas las salas: la
 * tabla del asistente es un panorama de los horarios de clase). Si la API
 * falla se degrada a solo el bloque de almuerzo y el error se muestra; el
 * backend valida con las franjas reales al crear la reserva.
 */
async function obtenerFranjasDelDia() {
  try {
    return franjasDelDia(await cargarFranjas(), diaIsoDeFecha(estado.fecha));
  } catch (error) {
    mostrarErrorGlobal(error.message);
    return [];
  }
}

function generarBloques() {
  const bloques = [];
  const [horaApertura] = HORA_APERTURA.split(':').map(Number);
  const [horaCierre] = HORA_CIERRE.split(':').map(Number);

  for (let h = horaApertura; h < horaCierre; h++) {
    bloques.push({ hora: `${String(h).padStart(2, '0')}:00`, minutos: 0 });
    bloques.push({ hora: `${String(h).padStart(2, '0')}:30`, minutos: 30 });
  }

  return bloques;
}

function obtenerMotivoBloqueado(hora, franjasDia) {
  // Verificar almuerzo
  if (hora >= HORA_INICIO_ALMUERZO && hora < HORA_FIN_ALMUERZO) {
    return 'Horario de almuerzo';
  }

  // Verificar franjas de clase del día (API)
  for (const franja of franjasDia) {
    if (hora >= horaCorta(franja.horaInicio) && hora < horaCorta(franja.horaFin)) {
      return franja.motivo;
    }
  }

  return null;
}

async function renderizarTablaHorarios() {
  franjasDiaActual = await obtenerFranjasDelDia();
  const bloques = generarBloques();

  elementos.tbodyHorarios.innerHTML = '';

  // La selección de horario se conserva si ya existía de una visita previa.
  hoverIndex = null;
  elementos.btnSiguiente2.disabled = !(estado.horaInicio && estado.horaFin);

  // Agrupar bloques bloqueados por motivo
  const bloquesConMotivo = bloques.map((bloque, index) => {
    const motivo = obtenerMotivoBloqueado(bloque.hora, franjasDiaActual);
    return { ...bloque, index, motivo };
  });

  // Contar bloques consecutivos por motivo
  const gruposMotivo = [];
  let grupoActual = null;

  bloquesConMotivo.forEach((bloque, i) => {
    if (bloque.motivo) {
      if (grupoActual && grupoActual.motivo === bloque.motivo) {
        grupoActual.count++;
        grupoActual.indices.push(i);
      } else {
        grupoActual = { motivo: bloque.motivo, count: 1, startIndex: i, indices: [i] };
        gruposMotivo.push(grupoActual);
      }
    } else {
      grupoActual = null;
    }
  });

  // Crear mapa de primer índice de cada grupo
  const primerIndiceGrupo = new Map();
  gruposMotivo.forEach(grupo => {
    primerIndiceGrupo.set(grupo.startIndex, grupo.count);
  });

  bloquesConMotivo.forEach((bloque, index) => {
    const tr = document.createElement('tr');
    tr.dataset.hora = bloque.hora;
    tr.dataset.index = index;

    const tdHora = document.createElement('td');
    tdHora.className = 'celda-hora';
    tdHora.textContent = bloque.hora;
    tr.appendChild(tdHora);

    const tdEstado = document.createElement('td');
    tdEstado.className = 'celda-estado';

    if (bloque.motivo) {
      tr.classList.add('bloqueado');

      // Solo agregar celda con motivo si es el primer bloque del grupo
      if (primerIndiceGrupo.has(index)) {
        const rowspan = primerIndiceGrupo.get(index);
        tdEstado.rowSpan = rowspan;
        tdEstado.innerHTML = `<span class="motivo-bloqueo">${bloque.motivo}</span>`;
        tr.appendChild(tdEstado);
      }
      // Si no es el primero, no agregar la celda (ya está cubierta por rowspan)
    } else {
      // Celda vacía para bloques disponibles
      tdEstado.textContent = '';
      tr.appendChild(tdEstado);

      tr.addEventListener('click', () => seleccionarBloque(bloque.hora, index));
      tr.addEventListener('dblclick', () => seleccionarBloqueDoble(bloque.hora, index));
      tr.addEventListener('mouseenter', () => handleHover(index));
      tr.addEventListener('mouseleave', () => handleHoverLeave());
    }

    elementos.tbodyHorarios.appendChild(tr);
  });

  actualizarVisualizacionHorario();
}

function handleHover(index) {
  if (estado.horaInicio && !estado.horaFin) {
    const bloques = generarBloques();
    const indexInicio = bloques.findIndex(b => b.hora === estado.horaInicio);

    if (index > indexInicio) {
      hoverIndex = index;
      actualizarVisualizacionHorario();
    }
  }
}

function handleHoverLeave() {
  if (hoverIndex !== null) {
    hoverIndex = null;
    actualizarVisualizacionHorario();
  }
}

function seleccionarBloque(hora, index) {
  const bloques = generarBloques();

  if (!estado.horaInicio) {
    // Primer clic: seleccionar inicio
    estado.horaInicio = hora;
    estado.horaFin = null;
    hoverIndex = null;
  } else if (!estado.horaFin) {
    // Segundo clic: seleccionar fin
    if (hora <= estado.horaInicio) {
      // Si selecciona antes del inicio, reiniciar
      estado.horaInicio = hora;
      estado.horaFin = null;
      hoverIndex = null;
    } else {
      // Calcular hora de fin (fin del bloque seleccionado)
      if (index < bloques.length - 1) {
        estado.horaFin = bloques[index + 1].hora;
      } else {
        estado.horaFin = HORA_CIERRE;
      }
      hoverIndex = null;
      validarRangoHorario();
    }
  } else {
    // Ya hay rango, reiniciar con nuevo inicio
    estado.horaInicio = hora;
    estado.horaFin = null;
    hoverIndex = null;
  }

  actualizarVisualizacionHorario();
}

function seleccionarBloqueDoble(hora, index) {
  const bloques = generarBloques();
  estado.horaInicio = hora;

  if (index < bloques.length - 1) {
    estado.horaFin = bloques[index + 1].hora;
  } else {
    estado.horaFin = HORA_CIERRE;
  }

  hoverIndex = null;
  validarRangoHorario();
  actualizarVisualizacionHorario();
}

function validarRangoHorario() {
  elementos.errorHorario.textContent = '';
  elementos.btnSiguiente2.disabled = true;

  if (!estado.horaInicio || !estado.horaFin) return;

  // Validación 7: Hora inicio >= apertura
  if (estado.horaInicio < HORA_APERTURA) {
    elementos.errorHorario.textContent = `La hora de inicio no puede ser anterior a las ${HORA_APERTURA}`;
    return;
  }

  // Validación 8: Hora fin <= cierre
  if (estado.horaFin > HORA_CIERRE) {
    elementos.errorHorario.textContent = `La hora de fin no puede ser posterior a las ${HORA_CIERRE}`;
    return;
  }

  // Forma de la reserva: hora fin > hora inicio
  if (estado.horaFin <= estado.horaInicio) {
    elementos.errorHorario.textContent = 'La hora de fin debe ser posterior a la hora de inicio';
    return;
  }

  // Validación 9: Duración mínima
  const duracionMinutos = calcularDuracionMinutos(estado.horaInicio, estado.horaFin);
  if (duracionMinutos < MIN_DURACION_MINUTOS) {
    elementos.errorHorario.textContent = `La duración mínima de la reserva es de ${MIN_DURACION_MINUTOS} minutos`;
    return;
  }

  // Validación 10: Duración máxima
  if (duracionMinutos > MAX_DURACION_HORAS * 60) {
    elementos.errorHorario.textContent = `La duración máxima de la reserva es de ${MAX_DURACION_HORAS} horas`;
    return;
  }

  // Validación 11: No solapar almuerzo
  const solapaAlmuerzo = !(estado.horaFin <= HORA_INICIO_ALMUERZO || estado.horaInicio >= HORA_FIN_ALMUERZO);
  if (solapaAlmuerzo) {
    elementos.errorHorario.textContent = `El horario seleccionado solapa con el bloque de almuerzo (${HORA_INICIO_ALMUERZO}-${HORA_FIN_ALMUERZO})`;
    return;
  }

  // Validación 12: No solapar franjas de clase del día
  for (const franja of franjasDiaActual) {
    const solapaFranja = !(estado.horaFin <= horaCorta(franja.horaInicio)
      || estado.horaInicio >= horaCorta(franja.horaFin));
    if (solapaFranja) {
      elementos.errorHorario.textContent = `El horario seleccionado solapa con: ${franja.motivo} (${horaCorta(franja.horaInicio)}-${horaCorta(franja.horaFin)})`;
      return;
    }
  }

  // Rango válido
  elementos.btnSiguiente2.disabled = false;
}

function actualizarVisualizacionHorario() {
  const bloques = generarBloques();

  // Limpiar clases
  elementos.tbodyHorarios.querySelectorAll('tr').forEach(tr => {
    tr.classList.remove('inicio-seleccionado', 'fin-seleccionado', 'en-rango', 'en-rango-hover', 'ultimo-en-rango');
  });

  if (!estado.horaInicio) {
    elementos.horarioSeleccionado.textContent = 'Sin seleccionar';
    return;
  }

  const indexInicio = bloques.findIndex(b => b.hora === estado.horaInicio);
  let indexFin = null;

  if (estado.horaFin) {
    // Si horaFin es HORA_CIERRE, el índice es el último bloque
    if (estado.horaFin === HORA_CIERRE) {
      indexFin = bloques.length;
    } else {
      indexFin = bloques.findIndex(b => b.hora === estado.horaFin);
    }
  }

  const filas = elementos.tbodyHorarios.querySelectorAll('tr');

  filas.forEach(tr => {
    const index = parseInt(tr.dataset.index);

    // Marcar inicio
    if (index === indexInicio) {
      tr.classList.add('inicio-seleccionado');
    }

    // Si hay fin definido, mostrar rango
    if (indexFin !== null && index >= indexInicio && index < indexFin && !tr.classList.contains('bloqueado')) {
      tr.classList.add('en-rango');

      // Marcar el último bloque del rango
      if (index === indexFin - 1) {
        tr.classList.add('ultimo-en-rango');
      }
    }

    // Si hay hover (solo cuando no hay fin), mostrar preview
    if (hoverIndex !== null && !estado.horaFin && index >= indexInicio && index <= hoverIndex && !tr.classList.contains('bloqueado')) {
      tr.classList.add('en-rango-hover');
    }
  });

  if (estado.horaInicio && estado.horaFin) {
    elementos.horarioSeleccionado.textContent = `${estado.horaInicio} - ${estado.horaFin}`;
  } else {
    elementos.horarioSeleccionado.textContent = `Inicio: ${estado.horaInicio} (selecciona fin)`;
  }
}

// ==============================
// SECCIÓN 3: GRILLA DE EQUIPOS
// ==============================

async function renderizarGrillaEquipos() {
  elementos.grillaEquipos.innerHTML = '';
  estado.equipoId = null;
  elementos.btnCrearReserva.disabled = true;

  let equipos, reservas, franjas;
  try {
    [equipos, reservas, franjas] = await Promise.all([listarEquipos(), listarReservas(), cargarFranjas()]);
  } catch (error) {
    mostrarErrorGlobal(error.message);
    return;
  }

  for (const equipo of equipos) {
    const div = document.createElement('div');
    div.classList.add('equipo-item');

    const idStr = String(equipo.idEquipo).padStart(2, '0');
    div.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="equipo-icono" aria-hidden="true">
        <rect x="2" y="3" width="20" height="14" rx="2"/>
        <path d="M8 21h8M12 17v4"/>
        <text x="12" y="11" text-anchor="middle" dominant-baseline="middle" font-size="6" font-weight="700" stroke="none" fill="currentColor" font-family="DM Sans, sans-serif">${idStr}</text>
      </svg>
    `;
    div.dataset.id = equipo.idEquipo;

    // Equipo activo, sin reservas que se crucen y sin clase en su sala
    const disponible = verificarDisponibilidad(
      equipo,
      estado.fecha,
      estado.horaInicio,
      estado.horaFin,
      franjas,
      reservas
    );

    if (!disponible) {
      div.classList.add('no-disponible');
    } else {
      div.addEventListener('click', () => seleccionarEquipo(equipo.idEquipo, div));
    }

    elementos.grillaEquipos.appendChild(div);
  }
}

function seleccionarEquipo(id, elemento) {
  // Deseleccionar anterior
  elementos.grillaEquipos.querySelectorAll('.equipo-item').forEach(el => {
    el.classList.remove('seleccionado');
  });

  // Seleccionar nuevo
  elemento.classList.add('seleccionado');
  estado.equipoId = id;
  elementos.btnCrearReserva.disabled = false;
  elementos.errorEquipo.textContent = '';
}

// ===================
// ENVÍO Y RESULTADO
// ===================

async function enviarReserva(e) {
  e.preventDefault();

  limpiarErrorGlobal();
  elementos.btnCrearReserva.disabled = true;

  try {
    // La API aplica las 15 validaciones y responde el mensaje exacto si algo falla.
    await crearReserva(estado.usuarioId, estado.equipoId, estado.fecha, estado.horaInicio, estado.horaFin);
    window.location.href = 'reservas.html';
  } catch (error) {
    mostrarErrorGlobal(error.message);
    elementos.btnCrearReserva.disabled = false;
  }
}

function mostrarErrorGlobal(mensaje) {
  elementos.errorGlobal.textContent = mensaje;
  elementos.errorGlobal.hidden = false;
}

function limpiarErrorGlobal() {
  elementos.errorGlobal.textContent = '';
  elementos.errorGlobal.hidden = true;
}
