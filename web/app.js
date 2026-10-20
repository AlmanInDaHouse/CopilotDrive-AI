'use strict';
const $ = (id) => document.getElementById(id);
const audio = $('audio');
let playlist = []; let position = -1; let currentUrl = null; let lastAlerts = '';
const fmt = (n) => (typeof n === 'number' ? String(Math.round(n * 10) / 10) : '—');
function say(msg) {
  if (!$('speak').checked || !('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(msg);
  utterance.lang = 'es-ES'; utterance.rate = 1;
  window.speechSynthesis.speak(utterance);
}
function bubble(who, msg) {
  const element = document.createElement('div'); element.className = 'bubble ' + who;
  element.textContent = msg; $('conversation').appendChild(element);
  $('conversation').scrollTop = $('conversation').scrollHeight;
}
async function update() {
  try {
    const response = await fetch('/api/telemetry', {cache: 'no-store'});
    if (!response.ok) throw new Error('HTTP ' + response.status);
    const s = await response.json();
    $('speed').textContent = fmt(s.speed_kmh); $('rpm').textContent = fmt(s.rpm);
    $('coolant').textContent = fmt(s.coolant_c); $('fuel').textContent = fmt(s.fuel_percent);
    $('voltage').textContent = fmt(s.battery_voltage);
    const sim = s.source === 'simulator';
    $('source').textContent = sim ? '◉ SIMULADOR · Datos ficticios' : '◉ OBD-II · ' + s.status;
    $('status').textContent = s.status === 'connected' ? '● Conectado' : '● Sin conexión OBD';
    $('status').classList.toggle('offline', s.status !== 'connected');
    const messages = s.alerts || [];
    $('alerts').textContent = messages.length ? messages.join(' · ') : 'Sin alertas de umbral en este momento.';
    $('alerts').classList.toggle('warning', messages.length > 0);
    const alertKey = messages.join('|');
    if (alertKey && alertKey !== lastAlerts) say('Aviso: ' + alertKey);
    lastAlerts = alertKey;
  } catch (error) {
    $('status').textContent = '● Servidor desconectado'; $('status').classList.add('offline');
    $('source').textContent = 'No hay telemetría disponible';
  }
}
function loadTrack(index) {
  if (!playlist.length) { $('track').textContent = 'Selecciona audio local primero'; return false; }
  position = (index + playlist.length) % playlist.length;
  if (currentUrl) URL.revokeObjectURL(currentUrl);
  currentUrl = URL.createObjectURL(playlist[position]); audio.src = currentUrl;
  $('track').textContent = playlist[position].name; return true;
}
async function play() {
  if (position < 0 && !loadTrack(0)) return false;
  try { await audio.play(); $('play').textContent = 'Ⅱ'; return true; }
  catch { $('track').textContent = 'Pulsa ▶ para activar el audio'; return false; }
}
function pause() { audio.pause(); $('play').textContent = '▶'; }
async function next() { if (loadTrack(position + 1)) return await play(); return false; }
async function action(kind) {
  if (kind === 'music.play') return await play();
  if (kind === 'music.pause') { pause(); return true; }
  if (kind === 'music.next') return await next();
  return true;
}
async function ask(text) {
  if (!text || !text.trim()) return;
  bubble('user', text); $('askInput').value = '';
  try {
    const res = await fetch('/api/assistant', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({text}) });
    if (!res.ok) throw new Error('Servicio no disponible');
    const data = await res.json();
    const ok = await action(data.action);
    const reply = !ok && data.action?.startsWith('music.') ? 'Primero selecciona tus archivos de música locales. No hay reproducción externa configurada.' : data.reply;
    bubble('ai', reply); say(reply);
  } catch { bubble('ai', 'Error de conexión con el servidor local.'); }
}
$('askForm').addEventListener('submit', event => { event.preventDefault(); ask($('askInput').value); });
$('files').addEventListener('change', event => {
  if (currentUrl) URL.revokeObjectURL(currentUrl);
  playlist = Array.from(event.target.files).filter(f => f.type.startsWith('audio/'));
  position = -1; audio.pause(); audio.removeAttribute('src'); $('play').textContent = '▶';
  $('track').textContent = playlist.length ? playlist.length + ' pistas listas' : 'Sin pistas cargadas';
});
$('play').addEventListener('click', () => { if (audio.paused) play(); else pause(); });
$('next').addEventListener('click', () => next());
$('previous').addEventListener('click', async () => { if (loadTrack(position - 1)) await play(); });
audio.addEventListener('ended', next);
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (!Recognition) { $('mic').disabled = true; $('speechHint').textContent = 'El navegador no admite Web Speech Recognition. Puedes escribir comandos.'; }
else {
  $('mic').addEventListener('click', () => {
    const recognizer = new Recognition(); recognizer.lang = 'es-ES'; recognizer.interimResults = false; recognizer.maxAlternatives = 1;
    recognizer.onresult = (ev) => ask(ev.results[0][0].transcript);
    recognizer.onerror = (ev) => { $('speechHint').textContent = 'Micrófono: ' + ev.error + '. Revisa permisos y origen HTTPS/localhost.'; };
    recognizer.onend = () => { $('mic').disabled = false; $('mic').textContent = '🎙 Hablar (detenido)'; };
    $('mic').disabled = true; $('mic').textContent = 'Escuchando...';
    try { recognizer.start(); } catch { $('mic').disabled = false; }
  });
}
update(); setInterval(update, 3000);
