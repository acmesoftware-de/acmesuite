#!/usr/bin/env bash
# Builds the static API developer portal: bundles + lints the four module specs,
# merges them into a combined all.yaml (openapi-merge-cli), renders Redoc pages,
# and assembles everything under api/portal/ ready to serve/upload as-is.
# Run from the repo root (same convention as `npx @redocly/cli lint`).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

MODULES="hr crm supply build"
REDOCLY="@redocly/cli@1"

npx --yes $REDOCLY lint

rm -rf api/.bundled api/portal/redoc api/portal/openapi
mkdir -p api/.bundled api/portal/redoc api/portal/openapi

for m in $MODULES; do
  npx --yes $REDOCLY bundle "api/acme-$m.yaml" -o "api/.bundled/acme-$m.yaml"
  npx --yes $REDOCLY build-docs "api/acme-$m.yaml" -o "api/portal/redoc/$m.html"
  cp "api/.bundled/acme-$m.yaml" "api/portal/openapi/acme-$m.yaml"
done
npx --yes $REDOCLY bundle api/acme-base.yaml -o api/portal/openapi/acme-base.yaml
# acme-base is both the platform API (auth/users/providers) and the shared component library the
# module specs pull in via $ref — render it as its own Redoc page so it shows up in the portal and
# the variant dropdown.
npx --yes $REDOCLY build-docs api/acme-base.yaml -o api/portal/redoc/base.html

# ACMEmailtrap: a separate product (the mail trap for testing ACMEsuite mail flows), not a suite
# module — render its own Redoc page + downloadable spec, but keep it OUT of the merged all.yaml.
npx --yes $REDOCLY bundle api/acme-mailtrap.yaml -o api/portal/openapi/acme-mailtrap.yaml
npx --yes $REDOCLY build-docs api/acme-mailtrap.yaml -o api/portal/redoc/mailtrap.html

npx --yes openapi-merge-cli@latest --config api/merge-config.json

# openapi-merge-cli inherits info.title/version from the first input (ACMEhr); relabel the
# combined view. Only the first `title:`/`version:` line right under `info:` needs patching.
awk '
  /^info:$/ { in_info=1 }
  in_info && !done_title && /^  title:/ { print "  title: ACMEsuite — All APIs"; done_title=1; next }
  in_info && !done_version && /^  version:/ { print "  version: 0.2.0"; done_version=1; next }
  { print }
' api/portal/openapi/all.yaml > api/portal/openapi/all.yaml.tmp
mv api/portal/openapi/all.yaml.tmp api/portal/openapi/all.yaml

npx --yes $REDOCLY build-docs api/portal/openapi/all.yaml -o api/portal/redoc/all.html
cp api/portal/redoc/all.html api/portal/redoc/index.html

# Wrap the generated Redoc pages in the ACMEsoftware brand shell (top nav + local fonts) and
# rewrite their CDN dependencies to the vendored copies under api/portal/assets/ (no-CDN).
python3 api/portal-brandify.py api/portal/redoc/*.html

rm -rf api/.bundled

# GraphQL Mesh reads its OpenAPI sources from mesh/openapi/ (Docker build context) — keep them
# in sync with the portal's bundled specs so Mesh always reflects the current contracts.
mkdir -p mesh/openapi
cp api/portal/openapi/acme-hr.yaml mesh/openapi/acme-hr.yaml
cp api/portal/openapi/acme-crm.yaml mesh/openapi/acme-crm.yaml
cp api/portal/openapi/acme-supply.yaml mesh/openapi/acme-supply.yaml
cp api/portal/openapi/acme-build.yaml mesh/openapi/acme-build.yaml

echo "Portal built under api/portal/, Mesh sources synced under mesh/openapi/"
