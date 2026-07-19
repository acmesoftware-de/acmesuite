#!/usr/bin/env python3
"""Post-process Redocly `build-docs` output into the ACMEsuite brand:
  1. inject the shared portal top-navigation (bar-mark + links + a variant dropdown) after <body>,
  2. load the local brand fonts + brand.css (no CDN),
  3. rewrite the Redoc runtime <script> from cdn.redocly.com to the vendored copy.

Redoc renders one spec per page, so the dropdown just navigates between the per-module pages
(All / hr / crm / supply / build / base). Run from repo root:
    python3 api/portal-brandify.py api/portal/redoc/*.html
"""
import os
import re
import sys

CDN = "https://cdn.redocly.com/redoc/v2.5.0/bundles/redoc.standalone.js"
LOCAL = "/assets/vendor/redoc.standalone.js"

# Redoc's prerender injects a Google Fonts stylesheet for its default Montserrat/Roboto.
# The brand theme uses locally-served Archivo/Space Mono, so this link must go (no CDN).
GOOGLE_FONTS_LINK = re.compile(r'<link[^>]*fonts\.googleapis\.com[^>]*>')

HEAD = (
    '<link rel="icon" href="/assets/logo-mark.svg" type="image/svg+xml">'
    '<link rel="stylesheet" href="/assets/fonts/acme-fonts.css">'
    '<link rel="stylesheet" href="/assets/brand.css">'
    '<style>body{margin:0}nav.portal-nav{position:relative;z-index:20}</style>'
)

# Variant dropdown: (value, label, key). `all` is served at /redoc/ (index.html).
VARIANTS = [
    ("/redoc/", "All APIs", "all"),
    ("/redoc/hr.html", "ACMEhr", "hr"),
    ("/redoc/crm.html", "ACMEcrm", "crm"),
    ("/redoc/supply.html", "ACMEsupply", "supply"),
    ("/redoc/build.html", "ACMEbuild", "build"),
    ("/redoc/base.html", "acme-base (shared)", "base"),
    ("/redoc/mailtrap.html", "ACMEmailtrap", "mailtrap"),
]


def variant_of(path):
    """Map a redoc output filename to its variant key (all/hr/crm/supply/build/base)."""
    name = os.path.basename(path).replace(".html", "")
    if name in ("all", "index"):
        return "all"
    return name if name in {v[2] for v in VARIANTS} else "all"


def nav(current):
    opts = "".join(
        '<option value="{v}"{sel}>{label}</option>'.format(
            v=v, label=label, sel=" selected" if key == current else "")
        for v, label, key in VARIANTS
    )
    return (
        '<nav class="portal-nav">'
        '<a class="brand" href="/"><span class="bars"><i></i><i></i><i></i><i></i></span>'
        '<span class="wm"><b>ACME</b><span>SOFTWARE</span></span></a>'
        '<span class="nav-links">'
        '<a href="/">Portal</a>'
        '<a href="/swagger/">Swagger</a>'
        '<a href="/redoc/" class="active">Redoc</a>'
        '<a href="/graphql-ui/">GraphQL</a>'
        '<a href="/changelog/">Changelog</a>'
        '<a href="/openapi/all.yaml">OpenAPI</a>'
        '</span>'
        '<select class="variant-select" aria-label="API-Variante"'
        ' onchange="if(this.value)location.href=this.value">' + opts + '</select>'
        '<span class="nav-badge">API PORTAL</span>'
        '</nav>'
    )


def brandify(path):
    with open(path, encoding="utf-8") as f:
        s = f.read()
    changed = 0
    if CDN in s:
        s = s.replace(CDN, LOCAL); changed += 1
    s, n = GOOGLE_FONTS_LINK.subn("", s); changed += n
    if "/assets/brand.css" not in s and "</head>" in s:
        s = s.replace("</head>", HEAD + "</head>", 1); changed += 1
    if 'class="portal-nav"' not in s and "<body>" in s:
        s = s.replace("<body>", "<body>" + nav(variant_of(path)), 1); changed += 1
    with open(path, "w", encoding="utf-8") as f:
        f.write(s)
    print(f"  brandified {path} ({changed} edits)")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("usage: portal-brandify.py <redoc.html> [...]")
    for p in sys.argv[1:]:
        brandify(p)
