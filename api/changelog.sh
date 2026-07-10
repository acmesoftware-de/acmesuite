#!/usr/bin/env bash
# Generates the API changelog: for every pair of consecutive semver tags (plus HEAD vs. the
# latest tag, if HEAD has moved on), runs `oasdiff changelog` against the merged all.yaml and
# renders it as a portal page. Run from the repo root, after api/build-portal.sh.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

OUT=api/portal/changelog
rm -rf "$OUT"
mkdir -p "$OUT"

# Shared ACMEsoftware brand top-navigation (same shell as the rest of the portal, see
# api/portal/assets/brand.css and api/portal-brandify.py). Changelog link marked active.
PORTAL_NAV_CHANGELOG='<nav class="portal-nav"><a class="brand" href="/"><span class="bars"><i></i><i></i><i></i><i></i></span><span class="wm"><b>ACME</b><span>SOFTWARE</span></span></a><span class="nav-links"><a href="/">Portal</a><a href="/swagger/">Swagger</a><a href="/redoc/">Redoc</a><a href="/graphql-ui/">GraphQL</a><a href="/changelog/" class="active">Changelog</a><a href="/openapi/all.yaml">OpenAPI</a></span><span class="nav-badge">API PORTAL</span></nav>'

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
      echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">'
      echo "<title>ACMEsuite Changelog — $label</title>"
      echo '<link rel="icon" href="/assets/logo-mark.svg" type="image/svg+xml">'
      echo '<link rel="stylesheet" href="/assets/fonts/acme-fonts.css"><link rel="stylesheet" href="/assets/brand.css">'
      echo '<style>.wrap h1{font-family:var(--display);font-weight:400;font-size:30px;letter-spacing:-0.03em}.wrap h2{font-family:var(--display);font-weight:400;font-size:21px;margin-top:2.4rem;border-top:2px solid var(--ink);padding-top:1.4rem}</style></head><body>'
      echo "$PORTAL_NAV_CHANGELOG"
      echo '<div class="wrap">'
      echo "<h1>ACMEsuite API Changelog — $label</h1>"
      echo "<h2>ACMEhr</h2>"; cat "$file.hr"
      echo "<h2>ACMEcrm</h2>"; cat "$file.crm"
      echo "<h2>ACMEsupply</h2>"; cat "$file.supply"
      echo "<h2>ACMEbuild</h2>"; cat "$file.build"
      echo "</div></body></html>"
    } > "$file"
    rm -f "$file.hr" "$file.crm" "$file.supply" "$file.build"
    ENTRIES+=("$label")
  fi
done

{
  echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">'
  echo '<title>ACMEsuite — API Changelog</title>'
  echo '<link rel="icon" href="/assets/logo-mark.svg" type="image/svg+xml">'
  echo '<link rel="stylesheet" href="/assets/fonts/acme-fonts.css"><link rel="stylesheet" href="/assets/brand.css">'
  echo '<style>.wrap h1{font-family:var(--display);font-weight:400;font-size:clamp(30px,4vw,44px);letter-spacing:-0.03em;margin:0 0 20px}.wrap ul{list-style:none;padding:0}.wrap li a{display:block;background:#fff;border:2px solid var(--ink);padding:14px 18px;margin-bottom:12px;color:var(--ink);text-decoration:none;font-family:var(--mono);font-weight:700}.wrap li a:hover{transform:translateY(-2px)}.empty{color:#3a3a3a}</style></head><body>'
  echo "$PORTAL_NAV_CHANGELOG"
  echo '<div class="wrap"><h1>API Changelog</h1>'
  if [ "${#ENTRIES[@]}" -eq 0 ]; then
    echo "<p class=\"empty\">No version history yet — <code>v1.0.0</code> is the initial release. The next tagged version will show up here as a diff via <a href=\"https://github.com/oasdiff/oasdiff\">oasdiff</a>.</p>"
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
