#!/usr/bin/env bash
# Generates the API changelog: for every pair of consecutive semver tags (plus HEAD vs. the
# latest tag, if HEAD has moved on), runs `oasdiff changelog` against the merged all.yaml and
# renders it as a portal page. Run from the repo root, after api/build-portal.sh.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

OUT=api/portal/changelog
rm -rf "$OUT"
mkdir -p "$OUT"

# Semver tags only, oldest first. (Portable read loop instead of `mapfile` — macOS ships bash 3.2.)
TAGS=()
while IFS= read -r t; do
  [ -n "$t" ] && TAGS+=("$t")
done < <(git tag -l 'v*' --sort=version:refname)

# Build the ordered list of refs to diff between: tag1->tag2->...->tagN, and tagN->HEAD if HEAD
# has moved past the latest tag (unreleased changes).
REFS=("${TAGS[@]}")
if [ "${#TAGS[@]}" -gt 0 ]; then
  latest="${TAGS[${#TAGS[@]}-1]}"
  if [ -n "$(git rev-list "$latest"..HEAD -- api/acme-*.yaml)" ]; then
    REFS+=("HEAD")
  fi
fi

ENTRIES=()
for ((i = 0; i < ${#REFS[@]} - 1; i++)); do
  base="${REFS[$i]}"
  rev="${REFS[$((i + 1))]}"
  label="${base}..${rev}"
  file="$OUT/${base}..${rev}.html"

  # Compare the bundled per-module specs (composed mode would need real files on disk; git-ref
  # comparison works directly on the committed source specs, no bundling needed since oasdiff
  # resolves $refs itself).
  if oasdiff changelog "${base}:api/acme-hr.yaml" "${rev}:api/acme-hr.yaml" --format html > "$file.hr" 2>/dev/null &&
     oasdiff changelog "${base}:api/acme-crm.yaml" "${rev}:api/acme-crm.yaml" --format html > "$file.crm" 2>/dev/null &&
     oasdiff changelog "${base}:api/acme-supply.yaml" "${rev}:api/acme-supply.yaml" --format html > "$file.supply" 2>/dev/null &&
     oasdiff changelog "${base}:api/acme-build.yaml" "${rev}:api/acme-build.yaml" --format html > "$file.build" 2>/dev/null; then
    # Concatenate the four module changelogs into one page with section headers.
    {
      echo "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
      echo "<title>ACMEsuite Changelog — $label</title>"
      echo "<style>body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:1000px;margin:0 auto;padding:20px}"
      echo "h1{font-size:1.4rem}h2{margin-top:2.5rem;border-top:1px solid #e2e8f0;padding-top:1.5rem}"
      echo "nav a{color:#5b9dff;text-decoration:none;font-weight:600;margin-right:16px}</style></head><body>"
      echo "<nav><a href=\"/\">Portal Home</a><a href=\"/changelog/\">Changelog Index</a></nav>"
      echo "<h1>ACMEsuite API Changelog — $label</h1>"
      echo "<h2>ACMEhr</h2>"; cat "$file.hr"
      echo "<h2>ACMEcrm</h2>"; cat "$file.crm"
      echo "<h2>ACMEsupply</h2>"; cat "$file.supply"
      echo "<h2>ACMEbuild</h2>"; cat "$file.build"
      echo "</body></html>"
    } > "$file"
    rm -f "$file.hr" "$file.crm" "$file.supply" "$file.build"
    ENTRIES+=("$label")
  fi
done

{
  echo '<!doctype html><html lang="en"><head><meta charset="utf-8">'
  echo '<title>ACMEsuite — API Changelog</title>'
  echo '<style>
    :root { --bg:#0f1115; --card:#181b22; --edge:#262b36; --fg:#e8eaed; --muted:#9aa4b2; --accent:#5b9dff; }
    body{margin:0;font:16px/1.55 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;background:var(--bg);color:var(--fg)}
    nav.portal-nav{display:flex;gap:18px;align-items:center;padding:12px 20px;background:#0b0d11;border-bottom:1px solid var(--edge);font-size:.9rem}
    nav.portal-nav a{color:var(--muted);text-decoration:none;font-weight:600}
    nav.portal-nav a:hover,nav.portal-nav a.active{color:var(--fg)}
    .wrap{max-width:760px;margin:0 auto;padding:48px 20px 80px}
    ul{list-style:none;padding:0}
    li a{display:block;background:var(--card);border:1px solid var(--edge);border-radius:10px;padding:14px 18px;margin-bottom:10px;color:var(--fg);text-decoration:none}
    li a:hover{border-color:var(--accent)}
    .empty{color:var(--muted)}
  </style></head><body>'
  echo '<nav class="portal-nav"><a href="/">Portal Home</a><a href="/swagger/">Swagger</a><a href="/redoc/">Redoc</a><a href="/graphql-ui/">GraphQL UI</a><a href="/changelog/" class="active">Changelog</a><a href="/openapi/all.yaml">OpenAPI</a></nav>'
  echo '<div class="wrap"><h1>API Changelog</h1>'
  if [ "${#ENTRIES[@]}" -eq 0 ]; then
    echo "<p class=\"empty\">No version history yet — <code>v1.0.0</code> is the initial release. The next tagged version will show up here as a diff via <a href=\"https://github.com/oasdiff/oasdiff\" style=\"color:var(--accent)\">oasdiff</a>.</p>"
  else
    echo '<ul>'
    for e in "${ENTRIES[@]}"; do
      echo "<li><a href=\"$e.html\">$e</a></li>"
    done
    echo '</ul>'
  fi
  echo '</div></body></html>'
} > "$OUT/index.html"

echo "Changelog built under $OUT/ (${#ENTRIES[@]} comparison(s))"
