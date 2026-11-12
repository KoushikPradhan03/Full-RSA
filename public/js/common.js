const API = '/api';
const CAT_ICON = { Starters: '◈', Mains: '◉', Desserts: '◇', Drinks: '◎' };
const CAT_EMOJI = { Starters: '🍢', Mains: '🍛', Desserts: '🍮', Drinks: '🥤' };
const FLOW = {
  'Dine-in':  ['Placed', 'Preparing', 'Ready', 'Served'],
  'Takeaway': ['Placed', 'Preparing', 'Ready', 'Delivered'],
  'Delivery': ['Placed', 'Preparing', 'Ready', 'Out for Delivery', 'Delivered']
};

/* ---------- auth + api ---------- */
function getAuth() { try { return JSON.parse(localStorage.getItem('hb_auth')); } catch (e) { return null; } }
function setAuth(a) { localStorage.setItem('hb_auth', JSON.stringify(a)); }
function forceLogin() { localStorage.removeItem('hb_auth'); location.href = '/login.html'; }
function logout() { api('/logout', {}).catch(() => {}).finally(forceLogin); }
function requireRole(role) {
  const a = getAuth();
  if (!a || a.user.role !== role) { location.href = '/login.html'; return null; }
  return a.user;
}
async function api(path, data) {
  const auth = getAuth();
  const opts = { method: data ? 'POST' : 'GET', headers: {} };
  if (auth) opts.headers['X-Token'] = auth.token;
  if (data) opts.body = new URLSearchParams(data);
  const res = await fetch(API + path, opts);
  const json = await res.json().catch(() => ({ error: 'Bad server response' }));
  if (!res.ok) {
    if (res.status === 401 && auth) forceLogin();
    throw new Error(json.error || 'Request failed');
  }
  return json;
}

/* ---------- small helpers ---------- */
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function money(n) { return '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 }); }
function today() { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 10); }
function toast(msg, type) {
  const t = document.createElement('div');
  t.className = 'toast ' + (type === 'err' ? 'err' : '');
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}
function openModal(html) {
  closeModal();
  const m = document.createElement('div');
  m.className = 'modal-bg'; m.id = 'modal';
  m.innerHTML = '<div class="modal">' + html + '</div>';
  document.body.appendChild(m);
}
function closeModal() { const m = document.getElementById('modal'); if (m) m.remove(); }

/* ---------- layout ---------- */
function renderTopbar(user, tabs) {
  document.getElementById('topbar').innerHTML = `
  <div class="nav">
    <div class="brand"><span class="gold">◈</span> Heaven Bites <span class="role-tag">${user.role}</span></div>
    <div class="tabs">${tabs.map(t => `<button class="tab" data-view="${t[0]}">${t[1]}</button>`).join('')}</div>
    <div class="nav-right"><span class="muted small">${esc(user.name)}</span>
      <button class="btn btn-ghost btn-sm" onclick="logout()">Logout</button></div>
  </div>`;
}
function setupTabs(onShow) {
  const tabs = [...document.querySelectorAll('.tab[data-view]')];
  function show(v) {
    tabs.forEach(t => t.classList.toggle('active', t.dataset.view === v));
    document.querySelectorAll('.view').forEach(x => x.classList.toggle('hidden', x.id !== 'view-' + v));
    onShow && onShow(v);
  }
  tabs.forEach(t => t.onclick = () => show(t.dataset.view));
  window.showView = show;
  show(tabs[0].dataset.view);
}

/* ---------- orders ---------- */
function nextStatus(o) {
  const f = FLOW[o.type]; const i = f.indexOf(o.status);
  return (i >= 0 && i < f.length - 1) ? f[i + 1] : null;
}
function orderCard(o, actions) {
  const f = FLOW[o.type], idx = f.indexOf(o.status);
  const lines = o.lines.map(l => `<div class="line"><span>${l.qty} × ${esc(l.name)}</span><span>${money(l.price * l.qty)}</span></div>`).join('');
  const steps = o.status === 'Cancelled' ? '' :
    `<div class="steps">${f.map((s, i) => `<span class="step ${i <= idx ? 'done' : ''}">${s}</span>`).join('')}</div>`;
  const where = o.type === 'Dine-in' ? ' · Table ' + esc(o.info) : (o.info ? ' · ' + esc(o.info) : '');
  return `<div class="order-card">
    <div class="oc-head"><div><b class="mono">#${o.id}</b> <span class="muted small">${esc(o.createdAt)}</span></div>
      <span class="badge st-${o.status.replace(/\s/g, '')}">${o.status}</span></div>
    <div class="muted small">${esc(o.customerName)} · ${o.type}${where}</div>
    ${steps}<div class="lines">${lines}</div>
    <div class="oc-foot"><div><b>${money(o.total)}</b>
      ${o.billNo ? `<span class="badge ${o.paid ? 'st-Paid' : 'st-Unpaid'}">${o.paid ? 'Paid · ' + esc(o.payMethod) : 'Bill ' + o.billNo}</span>` : ''}</div>
      <div class="actions">${actions || ''}</div></div></div>`;
}
function orderActions(o, opts = {}) {
  let h = ''; const n = nextStatus(o);
  if (n && o.status !== 'Cancelled') h += `<button class="btn btn-sm" onclick="ordStatus('${o.id}','${n}')">Mark ${n}</button>`;
  if (opts.cancel && !['Served', 'Delivered', 'Cancelled'].includes(o.status))
    h += `<button class="btn btn-sm btn-danger" onclick="ordStatus('${o.id}','Cancelled')">Cancel</button>`;
  return h;
}
async function ordStatus(id, status) {
  try { await api('/orders/status', { id, status }); toast('Order #' + id + ' → ' + status); window.refresh && window.refresh(); }
  catch (e) { toast(e.message, 'err'); }
}

/* ---------- bookings ---------- */
function bookingCard(b, actions) {
  return `<div class="order-card">
    <div class="oc-head"><div><b class="mono">#${b.id}</b> <span class="muted small">${esc(b.date)} · ${esc(b.time)}</span></div>
      <span class="badge st-${b.status}">${b.status}</span></div>
    <div class="muted small">${esc(b.customerName)} · ${b.guests} guests · 📞 ${esc(b.phone)}</div>
    <div class="oc-foot"><b>Table ${b.tableNo}</b><div class="actions">${actions || ''}</div></div></div>`;
}
function bookingActions(b) {
  const next = { Pending: ['Confirmed', 'Confirm'], Confirmed: ['Seated', 'Seat'], Seated: ['Completed', 'Complete'] }[b.status];
  let h = '';
  if (next) h += `<button class="btn btn-sm" onclick="bkStatus('${b.id}','${next[0]}')">${next[1]}</button>`;
  if (!['Completed', 'Cancelled'].includes(b.status))
    h += `<button class="btn btn-sm btn-danger" onclick="bkStatus('${b.id}','Cancelled')">Cancel</button>`;
  return h;
}
async function bkStatus(id, status) {
  try { await api('/bookings/status', { id, status }); toast('Booking updated'); window.refresh && window.refresh(); }
  catch (e) { toast(e.message, 'err'); }
}

/* ---------- bill popup ---------- */
function showBill(o) {
  const rows = o.lines.map(l => `<tr><td>${esc(l.name)}</td><td>${l.qty}</td><td>${money(l.price)}</td><td>${money(l.price * l.qty)}</td></tr>`).join('');
  openModal(`<div id="bill" class="bill">
    <h2 class="center">Heaven Bites</h2>
    <p class="center small muted">Authentic Indian Fine Dining</p><hr>
    <div class="between small"><span>Bill: <b>${esc(o.billNo || '—')}</b></span><span>${esc(o.createdAt)}</span></div>
    <div class="small">Order #${o.id} · ${o.type}${o.type === 'Dine-in' ? ' · Table ' + esc(o.info) : ''}<br>Customer: ${esc(o.customerName)}</div>
    <table class="bill-table"><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amt</th></tr>${rows}</table><hr>
    <div class="between"><span>Subtotal</span><span>${money(o.subtotal)}</span></div>
    <div class="between"><span>GST (5%)</span><span>${money(o.tax)}</span></div>
    <div class="between"><b>TOTAL</b><b>${money(o.total)}</b></div><hr>
    <p class="center small">${o.paid ? 'PAID via ' + esc(o.payMethod) : 'PAYMENT PENDING'}</p>
    <p class="center small">Thank you! Visit again.</p>
  </div>
  <div class="modal-actions"><button class="btn btn-ghost" onclick="closeModal()">Close</button>
  <button class="btn" onclick="window.print()">Print</button></div>`);
}

/* ---------- order taking form (used by cashier + waiter) ---------- */
async function renderOrderTaker(id, opts = {}) {
  const box = document.getElementById(id);
  const items = (await api('/menu')).items.filter(i => i.available);
  const types = opts.types || ['Dine-in', 'Takeaway', 'Delivery'];
  box.innerHTML = `<div class="panel">
    <div class="form-row">
      <div><label>Customer name</label><input id="ot-name" placeholder="Walk-in"></div>
      <div><label>Order type</label><select id="ot-type">${types.map(t => `<option>${t}</option>`).join('')}</select></div>
      <div id="ot-info-wrap"></div>
    </div>
    <div class="grid-sm">${items.map(i => `<div class="pick"><div><b>${esc(i.name)}</b>
      <div class="muted small">${money(i.price)} · max ${i.maxPerOrder}</div></div>
      <input type="number" class="ot-qty" data-id="${i.id}" data-price="${i.price}" min="0" max="${i.maxPerOrder}" value="0"></div>`).join('')}</div>
    <div class="between total-bar"><b>Total (incl. GST): <span id="ot-total">₹0</span></b>
      <button class="btn" id="ot-submit">Place order</button></div></div>`;
  const typeSel = document.getElementById('ot-type'), wrap = document.getElementById('ot-info-wrap');
  function drawInfo() {
    const t = typeSel.value;
    if (t === 'Dine-in') wrap.innerHTML = '<label>Table</label><select id="ot-info">' + [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(n => `<option>${n}</option>`).join('') + '</select>';
    else if (t === 'Delivery') wrap.innerHTML = '<label>Delivery address</label><input id="ot-info" placeholder="Full address">';
    else wrap.innerHTML = '';
  }
  function total() {
    let s = 0; box.querySelectorAll('.ot-qty').forEach(q => s += (+q.value || 0) * +q.dataset.price);
    document.getElementById('ot-total').textContent = money(s * 1.05);
  }
  typeSel.onchange = drawInfo; drawInfo();
  box.querySelectorAll('.ot-qty').forEach(q => q.oninput = total);
  document.getElementById('ot-submit').onclick = async () => {
    const list = [];
    box.querySelectorAll('.ot-qty').forEach(q => { if (+q.value > 0) list.push(q.dataset.id + ':' + q.value); });
    try {
      await api('/orders/place', {
        type: typeSel.value,
        info: (document.getElementById('ot-info') || {}).value || '',
        customerName: document.getElementById('ot-name').value,
        items: list.join(',')
      });
      toast('Order placed');
      opts.onDone ? opts.onDone() : renderOrderTaker(id, opts);
    } catch (e) { toast(e.message, 'err'); }
  };
}