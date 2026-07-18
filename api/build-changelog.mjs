#!/usr/bin/env node
// Baut den API-Changelog des ACMEsoftware-Portals im contractLoop-Stil: je Produkt eine
// Versionstabelle, je Version die Anzahl der Aenderungen (breaking / added / modified) mit
// Breaking-Kennzeichnung und einer aufklappbaren Detailliste. Quellen:
//   - ACMEsuite:    die vier Modul-Specs (api/acme-{hr,crm,supply,build}.yaml) aus der lokalen
//                   Git-Historie, verglichen zwischen den Release-Tags (v*).
//   - ACMEmailtrap: der REST-Vertrag aus dem oeffentlichen Repo (geklont), zwischen seinen Tags.
// oasdiff (JSON) liefert die strukturierten Entries -> keine externen Reports, kein CDN.
//
//   node api/build-changelog.mjs      (aus dem Repo-Root, nach api/build-portal.sh)
//
// Erzeugt: api/portal/changelog/index.html + CHANGELOG-<produkt>.md je Produkt.
import { execFileSync } from 'node:child_process';
import { writeFileSync, mkdtempSync, mkdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const repoRoot = execFileSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim();
const OUT = join(repoRoot, 'api/portal/changelog');
const MAILTRAP_REPO = 'https://github.com/acmesoftware-de/acmemailtrap.git';
const tmpRoot = mkdtempSync(join(tmpdir(), 'acme-cl-'));

const gitIn = (dir) => (args) =>
  execFileSync('git', ['-C', dir, ...args], { encoding: 'utf8', maxBuffer: 256 * 1024 * 1024 });

// Existiert <path> im Git-Baum von <ref>? (Modul kann erst ab einer spaeteren Version existieren.)
function existsAt(gitDir, ref, path) {
  try { execFileSync('git', ['-C', gitDir, 'cat-file', '-e', `${ref}:${path}`], { stdio: 'ignore' }); return true; }
  catch { return false; }
}

// oasdiff ueber GIT-REFS (ref:path) — oasdiff loest externe $refs (z. B. acme-base.yaml) aus dem
// Git-Baum selbst auf, daher kein Bundling/keine Tempdateien noetig. cwd = jeweiliges Repo.
function oasdiffJson(gitDir, baseRef, revRef, path) {
  const out = execFileSync('oasdiff', ['changelog', `${baseRef}:${path}`, `${revRef}:${path}`, '-f', 'json'],
    { cwd: gitDir, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
  if (!out) return [];
  try { const j = JSON.parse(out); return Array.isArray(j) ? j : []; } catch { return []; }
}
function oasdiffMd(gitDir, baseRef, revRef, path) {
  const md = execFileSync('oasdiff', ['changelog', `${baseRef}:${path}`, `${revRef}:${path}`, '-f', 'markdown'],
    { cwd: gitDir, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).replace(/^#\s+.*$/im, '').trim();
  // oasdiff meldet Byte-Unterschiede ohne API-Semantik so — als "nur Metadaten" behandeln.
  return /^No changes to report/i.test(md) ? '' : md;
}

// breaking = level>=3 (oasdiff ERR); added = Text beginnt mit "added"/"endpoint added"; sonst modified.
function classify(entries) {
  let breaking = 0, added = 0, modified = 0;
  for (const e of entries) {
    if ((e.level ?? 0) >= 3) breaking++;
    else if (/^(added|endpoint added)\b/i.test(e.text || '')) added++;
    else modified++;
  }
  return { breaking, added, modified, total: entries.length };
}

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const fmt = (s) => esc(s).replace(/`([^`]+)`/g, '<code>$1</code>');
const slug = (s) => s.replace(/[^\w.-]/g, '_');

// Entries -> Accordion-HTML, gruppiert nach (Modul ->) Operation/Pfad bzw. Section.
function detailHtml(groups) {
  let html = '';
  for (const g of groups) {
    if (!g.entries.length) continue;
    if (g.label) html += `<h4 class="cl-mod">${esc(g.label)}</h4>`;
    const byHeader = new Map();
    for (const e of g.entries) {
      const header = e.path
        ? `${(e.operation || '').toUpperCase()} ${e.path}`
        : (e.section ? e.section[0].toUpperCase() + e.section.slice(1) : 'Allgemein');
      if (!byHeader.has(header)) byHeader.set(header, []);
      byHeader.get(header).push(e);
    }
    for (const [header, items] of byHeader) {
      html += `<h5 class="cl-path">${esc(header)}</h5><ul>`;
      for (const e of items) {
        const br = (e.level ?? 0) >= 3;
        html += `<li${br ? ' class="breaking"' : ''}>${br ? '<span class="cl-l">breaking</span> ' : ''}${fmt(e.text || '')}</li>`;
      }
      html += '</ul>';
    }
  }
  return html || '<p class="muted">Keine API-Aenderungen (nur Metadaten).</p>';
}

// Ein Produkt verarbeiten: Tags -> je Versionsschritt oasdiff ueber alle Specs, Counts + Details.
function buildProduct(cfg) {
  const git = gitIn(cfg.gitDir);
  const tags = git(['tag', '-l', 'v*', '--sort=version:refname']).split('\n').filter(Boolean);
  if (!tags.length) return null;
  const refs = [...tags];
  const label = (ref) => (ref === 'HEAD' ? 'Unreleased' : ref);

  if (cfg.includeHead) {
    const latest = tags[tags.length - 1];
    let moved = false;
    try { moved = git(['rev-list', `${latest}..HEAD`, '--', ...cfg.specs.map((s) => s.path)]).trim().length > 0; } catch { /* keine */ }
    if (moved) refs.push('HEAD');
  }

  const steps = [];
  for (let i = 1; i < refs.length; i++) {
    const prev = refs[i - 1], ref = refs[i];
    const groups = []; let all = [];
    for (const s of cfg.specs) {
      if (!existsAt(cfg.gitDir, ref, s.path)) continue;   // Spec in dieser Version (noch) nicht vorhanden
      let entries = [];
      if (existsAt(cfg.gitDir, prev, s.path)) entries = oasdiffJson(cfg.gitDir, prev, ref, s.path);
      else entries = [{ text: `Neuer Vertrag hinzugefuegt (${s.label})`, level: 1 }];
      groups.push({ label: cfg.specs.length > 1 ? s.label : '', entries });
      all = all.concat(entries);
    }
    steps.push({ ref, prev, counts: classify(all), groups });
  }

  // Markdown-Verlauf (neueste Version zuerst).
  const md = [`# ${cfg.label} — API Changelog`, '',
    `> Automatisch aus der Git-Historie erzeugt (oasdiff je Versionsdelta). ${cfg.intro}`, ''];
  for (let i = refs.length - 1; i >= 0; i--) {
    const ref = refs[i];
    if (i === 0) { md.push(`## ${label(ref)}`, '', '_Ersterfassung des Contracts._', ''); continue; }
    const prev = refs[i - 1];
    let body = '';
    for (const s of cfg.specs) {
      if (!existsAt(cfg.gitDir, prev, s.path) || !existsAt(cfg.gitDir, ref, s.path)) continue;
      const m = oasdiffMd(cfg.gitDir, prev, ref, s.path);
      if (m) body += (cfg.specs.length > 1 ? `\n### ${s.label}\n\n` : '\n') + m + '\n';
    }
    md.push(`## ${label(ref)}`, '', `_Aenderungen gegenueber ${label(prev)}_`, '',
      body.trim() || '_Keine API-Aenderungen (nur Metadaten)._', '');
  }
  return { cfg, refs, steps, label, md: md.join('\n').replace(/\n{3,}/g, '\n\n').trim() + '\n' };
}

// ── Badges + Produktsektion ──
function badges(c) {
  const b = (n, cls, ic) => (n > 0 ? `<span class="cl-badge ${cls}">${ic}${n}</span>` : '');
  return b(c.breaking, 'breaking', '&#9889; ') + b(c.added, 'added', '+') + b(c.modified, 'modified', '~')
    || '<span class="cl-badge none">&mdash;</span>';
}
function productSection(built) {
  const { cfg, refs, steps, label } = built;
  const rows = refs.slice().reverse().map((ref) => {
    const step = steps.find((s) => s.ref === ref);
    const id = `cl-${cfg.key}-${slug(ref)}`;
    const badgeCell = step ? badges(step.counts) : '<span class="cl-badge none">Ersterfassung</span>';
    const detail = step ? detailHtml(step.groups) : '<p class="muted">Ersterfassung des Contracts.</p>';
    const sub = step ? `gegenueber ${label(step.prev)}` : 'Ersterfassung';
    return `        <tr class="cl-row"><td>
          <button class="cl-row-btn" type="button" aria-expanded="false" data-target="${id}">
            <span class="cl-caret">&#9656;</span>
            <span class="cl-version">${esc(label(ref))}</span>
            <span class="cl-vs">${sub}</span>
          </button></td>
          <td class="cl-badges-cell">${badgeCell}</td></tr>
        <tr class="cl-detail" id="${id}"><td colspan="2"><div class="cl-detail-body">${detail}</div></td></tr>`;
  }).join('\n');
  return `      <div class="section-title">${esc(cfg.label)}</div>
      <p class="cl-intro">${esc(cfg.intro)} &nbsp;&middot;&nbsp; <a href="CHANGELOG-${cfg.key}.md">CHANGELOG-${cfg.key}.md</a></p>
      <table class="cl-table"><thead><tr><th>Version</th><th class="cl-badges-head">Aenderungen</th></tr></thead><tbody>
${rows}
      </tbody></table>`;
}

// ── Main ──
mkdirSync(OUT, { recursive: true });

let mailtrapDir = join(tmpRoot, 'mailtrap-clone');
try { execFileSync('git', ['clone', '--quiet', MAILTRAP_REPO, mailtrapDir], { stdio: 'ignore' }); }
catch (e) { console.error('WARN: acmemailtrap-Klon fehlgeschlagen, ueberspringe Produkt: ' + e.message); mailtrapDir = null; }

const products = [
  {
    key: 'suite', label: 'ACMEsuite', gitDir: repoRoot, includeHead: true,
    intro: 'Die vier Modul-Vertraege der ACMEsuite (ACMEhr, ACMEcrm, ACMEsupply, ACMEbuild), verglichen zwischen den veroeffentlichten Release-Tags.',
    specs: [
      { key: 'hr', label: 'ACMEhr', path: 'api/acme-hr.yaml' },
      { key: 'crm', label: 'ACMEcrm', path: 'api/acme-crm.yaml' },
      { key: 'supply', label: 'ACMEsupply', path: 'api/acme-supply.yaml' },
      { key: 'build', label: 'ACMEbuild', path: 'api/acme-build.yaml' },
    ],
  },
];
if (mailtrapDir) products.push({
  key: 'mailtrap', label: 'ACMEmailtrap', gitDir: mailtrapDir, includeHead: false,
  intro: 'Der REST-Vertrag von ACMEmailtrap (die Mail-Falle zum Testen der ACMEsuite-Mailfluesse), verglichen zwischen den veroeffentlichten Release-Tags.',
  specs: [{ key: 'api', label: 'ACMEmailtrap API', path: 'app/src/main/resources/static/openapi.yaml' }],
});

const builtList = [];
for (const p of products) {
  try {
    const b = buildProduct(p);
    if (b) { builtList.push(b); writeFileSync(join(OUT, `CHANGELOG-${p.key}.md`), b.md); }
  } catch (e) { console.error(`WARN: Changelog ${p.key} fehlgeschlagen: ${e.message}`); }
}

const NAV = '<nav class="portal-nav"><a class="brand" href="/"><span class="bars"><i></i><i></i><i></i><i></i></span><span class="wm"><b>ACME</b><span>SOFTWARE</span></span></a><span class="nav-links"><a href="/">Portal</a><a href="/swagger/">Swagger</a><a href="/redoc/">Redoc</a><a href="/graphql-ui/">GraphQL</a><a href="/changelog/" class="active">Changelog</a><a href="/openapi/all.yaml">OpenAPI</a></span><span class="nav-badge">API PORTAL</span></nav>';

const STYLE = `
  .cl-legend{font-family:var(--mono);font-size:12px;color:var(--graphite);margin:2px 0 8px;display:flex;gap:18px;flex-wrap:wrap}
  .cl-legend span{display:inline-flex;align-items:center;gap:7px}
  .cl-intro{color:#3a3a3a;font-size:15px;margin:0 0 14px}
  .cl-intro a{color:var(--build);font-weight:600;text-decoration:none}
  .cl-table{width:100%;border-collapse:collapse;border:2px solid var(--ink);background:#fff}
  .cl-table th{font-family:var(--mono);font-size:11px;letter-spacing:.1em;text-transform:uppercase;text-align:left;color:var(--graphite);border-bottom:2px solid var(--ink);padding:12px 16px}
  .cl-badges-head{text-align:right}
  .cl-row td{border-top:2px solid var(--ink);padding:0;vertical-align:middle}
  .cl-table tbody tr.cl-row:first-child td{border-top:none}
  .cl-row-btn{width:100%;display:flex;align-items:center;gap:12px;background:none;border:none;cursor:pointer;padding:14px 16px;font-family:var(--text)}
  .cl-caret{font-family:var(--mono);color:var(--graphite);transition:transform .12s;display:inline-block}
  .cl-row-btn[aria-expanded="true"] .cl-caret{transform:rotate(90deg)}
  .cl-version{font-family:var(--display);font-size:17px;letter-spacing:-0.02em}
  .cl-vs{font-family:var(--mono);font-size:12px;color:var(--graphite-tint)}
  .cl-badges-cell{padding:14px 16px;text-align:right;white-space:nowrap}
  .cl-badge{font-family:var(--mono);font-size:12px;font-weight:700;padding:3px 9px;margin-left:6px;border:1.5px solid var(--ink);display:inline-block}
  .cl-badge.breaking{background:var(--crm);color:#fff;border-color:var(--crm)}
  .cl-badge.added{background:var(--supply);color:#fff;border-color:var(--supply)}
  .cl-badge.modified{background:var(--build);color:#fff;border-color:var(--build)}
  .cl-badge.none{color:var(--graphite-tint);border-color:#ccc;font-weight:400}
  .cl-detail{display:none}
  .cl-detail.open{display:table-row}
  .cl-detail-body{padding:8px 20px 22px;border-top:1px dashed #cfcfcf;background:#fafaf8}
  .cl-detail-body h4.cl-mod{font-family:var(--display);font-weight:400;font-size:16px;margin:16px 0 6px}
  .cl-detail-body h5.cl-path{font-family:var(--mono);font-size:12px;color:var(--build);margin:12px 0 4px}
  .cl-detail-body ul{margin:0 0 6px;padding-left:20px}
  .cl-detail-body li{font-size:14.5px;line-height:1.5;margin:3px 0}
  .cl-detail-body li.breaking{color:#a01a12}
  .cl-l{font-family:var(--mono);font-size:10.5px;letter-spacing:.06em;text-transform:uppercase;background:var(--crm);color:#fff;padding:1px 6px}
  .muted{color:var(--graphite-tint)}`;

const sections = builtList.map(productSection).join('\n\n');
const html = `<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>ACMEsoftware — API Changelog</title>
<link rel="icon" href="/assets/logo-mark.svg" type="image/svg+xml">
<link rel="stylesheet" href="/assets/fonts/acme-fonts.css">
<link rel="stylesheet" href="/assets/brand.css">
<style>${STYLE}</style>
</head>
<body>
${NAV}
<div class="wrap">
  <header class="page">
    <div class="kicker">api.acmesoftware.de &middot; Verlauf</div>
    <h1>API Changelog</h1>
    <p>Entwicklung der OpenAPI-Vertraege je veroeffentlichter Version. Version anklicken fuer die Details.</p>
  </header>
  <div class="cl-legend">
    <span><span class="cl-badge breaking">&#9889; n</span> Breaking Changes</span>
    <span><span class="cl-badge added">+n</span> Added</span>
    <span><span class="cl-badge modified">~n</span> Modified</span>
  </div>
${sections}
  <div class="foot">
    <p>Automatisch mit <a href="https://github.com/oasdiff/oasdiff">oasdiff</a> aus der Git-Historie der Vertraege erzeugt (kein CDN). ACMEsuite: Release-Tags des Repos <a href="https://github.com/acmesoftware-de/acmesuite">acmesuite</a>; ACMEmailtrap: Release-Tags des Repos <a href="https://github.com/acmesoftware-de/acmemailtrap">acmemailtrap</a>.</p>
  </div>
</div>
<script>
  document.querySelectorAll('.cl-row-btn').forEach(function(btn){
    btn.addEventListener('click', function(){
      var d = document.getElementById(btn.dataset.target);
      var open = btn.getAttribute('aria-expanded') === 'true';
      btn.setAttribute('aria-expanded', open ? 'false' : 'true');
      d.classList.toggle('open', !open);
    });
  });
</script>
</body>
</html>`;

writeFileSync(join(OUT, 'index.html'), html);
rmSync(tmpRoot, { recursive: true, force: true });
console.log(`Changelog gebaut: ${builtList.map((b) => `${b.cfg.label} (${b.refs.length} Versionen)`).join(', ')} -> ${OUT}/index.html`);
