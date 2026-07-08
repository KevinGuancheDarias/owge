(function () {
  var q = document.getElementById('q');
  var r = document.getElementById('qr');
  if (!q || !r || !window.OWGE_SEARCH) return;
  q.addEventListener('input', function () {
    var v = q.value.trim().toLowerCase();
    r.innerHTML = '';
    if (v.length < 2) { r.style.display = 'none'; return; }
    var hits = window.OWGE_SEARCH.filter(function (e) {
      return e.t.toLowerCase().indexOf(v) >= 0;
    }).slice(0, 15);
    hits.forEach(function (e) {
      var a = document.createElement('a');
      a.href = window.OWGE_ROOT + e.h;
      var k = document.createElement('span');
      k.className = 'k';
      k.textContent = e.k;
      a.appendChild(k);
      a.appendChild(document.createTextNode(' ' + e.t));
      r.appendChild(a);
    });
    r.style.display = hits.length ? 'block' : 'none';
  });
  document.addEventListener('click', function (ev) {
    if (ev.target !== q && !r.contains(ev.target)) r.style.display = 'none';
  });
})();
