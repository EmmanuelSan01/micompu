import { registrarUsuario } from './reservas.js';

function ocultarErrores() {
  document.getElementById('emailError').style.display = 'none';
  document.getElementById('emailDomainError').style.display = 'none';
  document.getElementById('passwordError').style.display = 'none';
}

function mostrarError(id) {
  document.getElementById(id).style.display = 'block';
}

/** Validaciones locales del formulario: dominio del correo y coincidencia. */
function validarRegistro(email, password, repeatPassword) {
  if (!email.endsWith('@unab.edu.co')) {
    return 'emailDomainError';
  }
  if (password !== repeatPassword) {
    return 'passwordError';
  }
  return null;
}

async function enviarRegistro(evento) {
  evento.preventDefault();
  ocultarErrores();

  const nombre = document.getElementById('name').value.trim();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const repeatPassword = document.getElementById('repeatPassword').value;

  const error = validarRegistro(email, password, repeatPassword);
  if (error) {
    mostrarError(error);
    return;
  }

  try {
    await registrarUsuario(nombre, email, password);
    window.location.href = 'login.html';
  } catch (e) {
    // 409: la API responde "Ya existe un usuario registrado con el correo …".
    if (e.estado === 409) {
      mostrarError('emailError');
    } else {
      alert('Error al crear la cuenta: ' + e.message);
    }
  }
}

document.getElementById('registrationForm').addEventListener('submit', enviarRegistro);
