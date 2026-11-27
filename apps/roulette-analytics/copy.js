const translation = {
  es: {
    apps: 'Aplicaciones', intro: 'Registra tiradas, explora las frecuencias y simula jugadas desde el tapete de ruleta.',
    download: 'Descargar para Android', help: 'Guía de instalación en Android',
    board: 'Tapete interactivo', boardText: 'Introduce el número que ha salido tocando su casilla y ajusta las fichas de la simulación.',
    stats: 'Historial y estadísticas', statsText: 'Consulta frecuencias y rachas. Puedes usar una foto para cargar las estadísticas iniciales.',
    account: 'Acceso con tu cuenta', accountText: 'Crea un usuario y una contraseña. Tu cuenta conserva el saldo al cambiar de dispositivo.',
    notice: 'Para simulación e investigación estadística. Los resultados históricos no predicen la siguiente tirada.',
    back: 'Volver a UrySoft', admin: 'Administración'
  },
  ca: {
    apps: 'Aplicacions', intro: 'Registra tirades, explora les freqüències i simula jugades des del tapet de ruleta.',
    download: 'Descarrega per a Android', help: 'Guia d’instal·lació a Android',
    board: 'Tapet interactiu', boardText: 'Introdueix el número que ha sortit tocant-ne la casella i ajusta les fitxes de la simulació.',
    stats: 'Historial i estadístiques', statsText: 'Consulta les freqüències i les ratxes. Pots fer servir una foto per carregar les estadístiques inicials.',
    account: 'Accés amb el teu compte', accountText: 'Crea un usuari i una contrasenya. El compte conserva el saldo quan canvies de dispositiu.',
    notice: 'Per a simulació i recerca estadística. Els resultats històrics no prediuen la tirada següent.',
    back: 'Torna a UrySoft', admin: 'Administració'
  },
  en: {
    apps: 'Apps', intro: 'Record spins, explore frequencies, and simulate plays on an interactive roulette layout.',
    download: 'Download for Android', help: 'Android installation guide',
    board: 'Interactive layout', boardText: 'Tap the winning number to record a spin and adjust the chips used in the simulation.',
    stats: 'History and statistics', statsText: 'Review frequencies and streaks. You can use a photo to load initial statistics.',
    account: 'Access with your account', accountText: 'Create a username and password. Your account keeps its credit balance across devices.',
    notice: 'For simulation and statistical research. Past results do not predict the next spin.',
    back: 'Back to UrySoft', admin: 'Administration'
  }
};
const language = document.getElementById('language');
function render(requested) {
  const current = Object.hasOwn(translation, requested) ? requested : 'en';
  language.value = current;
  document.documentElement.lang = current;
  for (const element of document.querySelectorAll('[data-copy]')) {
    element.textContent = translation[current][element.dataset.copy];
  }
}
let preferred;
try { preferred = localStorage.getItem('urysoft-lang'); } catch {}
render(preferred || navigator.language.slice(0, 2));
language.onchange = () => {
  try { localStorage.setItem('urysoft-lang', language.value); } catch {}
  render(language.value);
};
