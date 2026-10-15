import './main.scss';

const MIN = 3;
const MAX = 500;

const $ = (id) => document.getElementById(id);
const text = $('text');
const counter = $('counter');
const speak = $('speak');
const status = $('status');
const result = $('result');
const player = $('player');
const download = $('download');

let currentUrl = null;

text.addEventListener('input', () => {
  const n = text.value.length;
  counter.textContent = `${n} / ${MAX}`;
  counter.classList.toggle('warn', n > MAX - 30);
});

function setStatus(msg, isError = false) {
  status.textContent = msg;
  status.classList.toggle('error', isError);
}

function friendlyError(res, body) {
  if (res.status === 429) return 'Servidor ocupado. Tente novamente em alguns segundos.';
  if (res.status === 400) return `Texto inválido (use de ${MIN} a ${MAX} caracteres).`;
  return 'Erro ao gerar o áudio. Tente novamente.';
}

speak.addEventListener('click', async () => {
  const value = text.value.trim();
  if (value.length < MIN) {
    setStatus(`Digite pelo menos ${MIN} caracteres.`, true);
    return;
  }

  speak.disabled = true;
  speak.classList.add('loading');
  setStatus('Gerando áudio');
  result.hidden = true;

  try {
    const res = await fetch('/tts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: value }),
    });

    if (!res.ok) {
      setStatus(friendlyError(res), true);
      return;
    }

    const blob = await res.blob();
    if (currentUrl) URL.revokeObjectURL(currentUrl);
    currentUrl = URL.createObjectURL(blob);

    player.src = currentUrl;
    download.href = currentUrl;
    result.hidden = false;
    setStatus('Pronto!');
    player.play().catch(() => {});
  } catch {
    setStatus('Não foi possível conectar ao servidor.', true);
  } finally {
    speak.disabled = false;
    speak.classList.remove('loading');
  }
});