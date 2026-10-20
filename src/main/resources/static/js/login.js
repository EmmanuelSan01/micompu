import { iniciarSesion } from './reservas.js';

function ocultarErrores() {
  document.getElementById('emailError').style.display = 'none';
  document.getElementById('emailDomainError').style.display = 'none';
  document.getElementById('passwordError').style.display = 'none';
}

function mostrarError(id) {
  document.getElementById(id).style.display = 'block';
}

async function enviarLogin(evento) {
  evento.preventDefault();
  ocultarErrores();

  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;

  // El dominio institucional se valida aquí; el resto lo decide la API.
  if (!email.endsWith('@unab.edu.co')) {
    mostrarError('emailDomainError');
    return;
  }

  try {
    const usuario = await iniciarSesion(email, password);
    sessionStorage.setItem('usuario_activo', JSON.stringify(usuario));
    window.location.href = 'index.html';
  } catch (error) {
    // La API distingue los dos casos: 404 = correo no registrado, 400 = contraseña incorrecta.
    if (error.estado === 404) {
      mostrarError('emailError');
    } else if (error.estado === 400) {
      mostrarError('passwordError');
    } else {
      alert('Error durante el inicio de sesión: ' + error.message);
    }
  }
}

document.getElementById('loginForm').addEventListener('submit', enviarLogin);
