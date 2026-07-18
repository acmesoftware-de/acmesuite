// Stamps the running version + git commit into a footer element (from /actuator/info).
// Shared by swagger.html and redoc.html. No CDN; served from the jar under /openapi/.
function acmeStampBuild(elementId) {
  fetch('/actuator/info')
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (info) {
      if (!info) return;
      var v = info.build && info.build.version;
      var commit = info.git && info.git.commit;
      var c = commit && (typeof commit.id === 'object' ? commit.id.abbrev : commit.id);
      var parts = ['ACMEsuite API'];
      if (v) parts.push('v' + v);
      if (c) parts.push('git ' + String(c).substring(0, 8));
      var el = document.getElementById(elementId);
      if (el) el.textContent = parts.join(' · ');
    })
    .catch(function () {});
}
