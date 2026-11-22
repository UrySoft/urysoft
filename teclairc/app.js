'use strict';
let publishedVersion = document.body.dataset.version;
let subscriptionsAvailable;

function renderRelease() {
  const copy = COPY[document.documentElement.lang];
  document.querySelector('[data-t="version"]').textContent = copy.version.replace('{version}', publishedVersion);
  const availability = subscriptionsAvailable === undefined ? 'unknown' : subscriptionsAvailable ? 'subscribe' : 'standby';
  document.querySelector('#availability').textContent = copy[availability];
}

async function updateDownload() {
  const response = await fetch('/updates/teclairc.json', {cache: 'no-store'});
  if (!response.ok) return;
  const data = await response.json();
  const version = data.versionName;
  if (data.app !== 'teclairc' || data.status !== 'published' || typeof version !== 'string' || !/^\d+(?:\.\d+){1,3}$/.test(version)) return;
  const path = '/descargas/teclairc/TeclaIRC-' + version + '.apk';
  const url = new URL(data.downloadUrl);
  const official = url.href === 'https://urysoft.pages.dev' + path || url.href === location.origin + path;
  const release = url.href === 'https://github.com/UrySoft/urysoft/releases/download/teclairc-' + version + '/TeclaIRC-' + version + '.apk';
  if (!official && !release) return;
  document.querySelectorAll('.tecla-download').forEach(link => { link.href = official ? path : url.href; });
  publishedVersion = version;
  renderRelease();
}

async function updateStatus() {
  const response = await fetch('/api/teclairc/live/status', {cache: 'no-store'});
  if (!response.ok) return;
  const data = await response.json();
  if (typeof data.available !== 'boolean') return;
  subscriptionsAvailable = data.available;
  renderRelease();
}

document.querySelector('#lang').addEventListener('change', renderRelease);
renderRelease();
updateDownload().catch(() => {});
updateStatus().catch(() => {});
