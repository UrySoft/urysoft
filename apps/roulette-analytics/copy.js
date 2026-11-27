const translation = {
  es: {
    apps: 'Aplicaciones', intro: 'Registra tiradas, explora las frecuencias y simula jugadas desde el tapete de ruleta.',
    download: 'Descargar para Android', help: 'Guía de instalación en Android',
    reinstall: 'Si tienes una versión anterior, desinstálala antes de instalar la 1.1.29. Después, vuelve a entrar con la misma cuenta para conservar tus créditos. A partir de esta versión recibirás avisos de actualización al abrir la app.',
    board: 'Tapete interactivo', boardText: 'Introduce el número que ha salido tocando su casilla y ajusta las fichas de la simulación.',
    stats: 'Historial y estadísticas', statsText: 'Consulta frecuencias y rachas. Puedes usar una foto para cargar las estadísticas iniciales.',
    account: '50 créditos de bienvenida', accountText: 'Crea tu cuenta con usuario y contraseña. Consulta el saldo desde el tapete y consérvalo al cambiar de dispositivo.',
    referrals: 'Invita a tus amigos', referralsText: 'Comparte tu enlace desde Créditos. Recibe 50 créditos por cada alta con tu código y 5 cuando uno de tus invitados consigue una nueva alta.',
    invitation: 'Tu invitación', invitationHelp: 'Introduce este código al crear tu cuenta desde la aplicación.',
    copyCode: 'Copiar código', copied: 'Código copiado.', selectCode: 'Selecciona Copiar para guardar el código.',
    notice: 'Para simulación e investigación estadística. Los resultados históricos no predicen la siguiente tirada.',
    back: 'Volver a UrySoft', admin: 'Administración'
  },
  ca: {
    apps: 'Aplicacions', intro: 'Registra tirades, explora les freqüències i simula jugades des del tapet de ruleta.',
    download: 'Descarrega per a Android', help: 'Guia d’instal·lació a Android',
    reinstall: 'Si tens una versió anterior, desinstal·la-la abans d’instal·lar la 1.1.29. Torna a entrar amb el mateix compte per conservar els crèdits. A partir d’aquesta versió rebràs avisos d’actualització en obrir l’app.',
    board: 'Tapet interactiu', boardText: 'Introdueix el número que ha sortit tocant-ne la casella i ajusta les fitxes de la simulació.',
    stats: 'Historial i estadístiques', statsText: 'Consulta les freqüències i les ratxes. Pots fer servir una foto per carregar les estadístiques inicials.',
    account: '50 crèdits de benvinguda', accountText: 'Crea el teu compte amb usuari i contrasenya. Consulta el saldo des del tapet i conserva’l quan canviïs de dispositiu.',
    referrals: 'Convida els teus amics', referralsText: 'Comparteix el teu enllaç des de Crèdits. Rep 50 crèdits per cada alta amb el teu codi i 5 quan un dels teus convidats aconsegueix una alta nova.',
    invitation: 'La teva invitació', invitationHelp: 'Introdueix aquest codi quan creïs el compte des de l’aplicació.',
    copyCode: 'Copia el codi', copied: 'Codi copiat.', selectCode: 'Selecciona Copia per desar el codi.',
    notice: 'Per a simulació i recerca estadística. Els resultats històrics no prediuen la tirada següent.',
    back: 'Torna a UrySoft', admin: 'Administració'
  },
  en: {
    apps: 'Apps', intro: 'Record spins, explore frequencies, and simulate plays on an interactive roulette layout.',
    download: 'Download for Android', help: 'Android installation guide',
    reinstall: 'If you have an earlier version, uninstall it before installing 1.1.29. Sign in with the same account to keep your credits. From this version onward, the app will check for updates when it opens.',
    board: 'Interactive layout', boardText: 'Tap the winning number to record a spin and adjust the chips used in the simulation.',
    stats: 'History and statistics', statsText: 'Review frequencies and streaks. You can use a photo to load initial statistics.',
    account: '50 welcome credits', accountText: 'Create an account with a username and password. See your balance on the roulette layout and keep it across devices.',
    referrals: 'Invite your friends', referralsText: 'Share your link from Credits. Earn 50 credits for each new account using your code and 5 when one of your invitees refers a new user.',
    invitation: 'Your invitation', invitationHelp: 'Enter this code when you create an account in the app.',
    copyCode: 'Copy code', copied: 'Code copied.', selectCode: 'Select Copy to save the code.',
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
const code = new URLSearchParams(location.search).get('ref')?.toUpperCase();
if (code && /^RA-[0-9A-F]{12}$/.test(code)) {
  const field = document.getElementById('invitation-code');
  const status = document.getElementById('copy-status');
  field.value = code;
  document.getElementById('invitation').hidden = false;
  document.getElementById('copy-invitation').onclick = async () => {
    try {
      await navigator.clipboard.writeText(code);
      status.textContent = translation[language.value].copied;
    } catch {
      field.select();
      status.textContent = translation[language.value].selectCode;
    }
  };
}
