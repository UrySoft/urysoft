const reasons = {
  abuse: 'Acoso u odio', sexual: 'Contenido sexual o de explotación', violence: 'Violencia o amenazas',
  spam: 'Spam o engaño', privacy: 'Datos personales', other: 'Otro motivo',
};
const subjects = {
  user: 'Usuario', message: 'Mensaje', text: 'Texto generado', image: 'Imagen generada',
  video: 'Vídeo generado', post: 'Publicación generada', reply: 'Respuesta generada',
};
const apps = { teclairc: 'TeclaIRC', minairo: 'Minairó' };

export function appReportQueue(request) {
  const panel = document.querySelector('#app-reports');
  if (!panel) return { load() {}, clear() {} };
  const list = panel.querySelector('#app-report-list');
  const status = panel.querySelector('#app-report-status-message');
  const filter = panel.querySelector('#app-report-filter');
  const application = panel.querySelector('#app-report-app');
  const more = panel.querySelector('#more-app-reports');
  let cursor = null;
  let generation = 0;

  function element(tag, text) {
    const node = document.createElement(tag);
    node.textContent = text || '';
    return node;
  }

  function render(item) {
    const article = element('article');
    article.className = 'panel';
    article.append(element('h2', apps[item.app] + ' · ' + subjects[item.subject]));
    article.append(element('p', [reasons[item.reason], new Date(item.created_at).toLocaleString('es-ES'), item.reference].join(' · ')));
    if (item.network) article.append(element('p', [item.alias, item.network, item.channel].filter(Boolean).join(' · ')));
    if (item.action) article.append(element('p', 'Acción de Minairó ' + item.action));
    if (item.details) article.append(element('p', item.details));
    if (item.excerpt) {
      const evidence = element('details');
      evidence.append(element('summary', 'Ver fragmento denunciado'));
      const excerpt = element('blockquote', item.excerpt);
      excerpt.style.whiteSpace = 'pre-wrap';
      evidence.append(excerpt);
      article.append(evidence);
    }

    const decision = element('textarea');
    decision.value = item.resolution;
    decision.placeholder = 'Motivo y medida aplicada';
    decision.setAttribute('aria-label', 'Decisión de moderación');
    decision.maxLength = 1000;
    article.append(decision);
    const controls = element('div');
    controls.className = 'filters';
    const next = item.status === 'open'
      ? [['resolved', 'Resolver'], ['dismissed', 'Archivar']]
      : [['open', 'Reabrir']];
    for (const [target, caption] of next) {
      const control = element('button', caption);
      controls.append(control);
      control.type = 'button';
      control.onclick = async () => {
        if (target !== 'open' && !decision.value.trim()) {
          status.textContent = 'Indica la decisión de moderación.';
          decision.focus();
          return;
        }
        for (const button of controls.querySelectorAll('button')) button.disabled = true;
        try {
          await request('app-reports','PATCH',{
            id: item.id, version: item.updated_at, status: target, resolution: decision.value.trim(),
          });
          await load();
        } catch (error) {
          status.textContent = error.message;
          for (const button of controls.querySelectorAll('button')) button.disabled = false;
        }
      };
    }
    article.append(controls);
    return article;
  }

  async function load(append = false) {
    const ticket = ++generation;
    if (!append) {
      cursor = null;
      list.replaceChildren();
    }
    more.hidden = true;
    status.textContent = 'Cargando…';
    const query = new URLSearchParams({ status: filter.value, app: application.value });
    if (append && cursor) query.set('before', String(cursor));
    try {
      const response = await request('app-reports?' + query);
      if (ticket !== generation) return;
      for (const item of response.items) list.append(render(item));
      cursor = response.next;
      more.hidden = !cursor;
      status.textContent = list.children.length ? '' : 'No hay denuncias en este filtro.';
    } catch (error) {
      if (ticket === generation) status.textContent = error.message;
    }
  }

  filter.onchange = () => load();
  application.onchange = () => load();
  panel.querySelector('#reload-app-reports').onclick = () => load();
  more.onclick = () => load(true);
  return {
    load,
    clear() {
      generation++;
      cursor = null;
      more.hidden = true;
      list.replaceChildren();
      status.textContent = '';
    },
  };
}
