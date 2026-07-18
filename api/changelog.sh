#!/usr/bin/env bash
# Builds the portal API changelog (contractLoop-style: per-version change counts + breaking flags
# + expandable details) for ACMEsuite and ACMEmailtrap. Thin wrapper around build-changelog.mjs so
# the CI step (`bash api/changelog.sh`) stays stable. Needs oasdiff + node + git (full history/tags)
# and network access to clone the public acmemailtrap repo. Run after api/build-portal.sh.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
exec node api/build-changelog.mjs
