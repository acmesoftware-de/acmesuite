# Vendored portal assets (no-CDN)

The API portal must load **nothing from a CDN** — every third-party runtime is vendored here and
referenced locally (`/assets/vendor/...`). This mirrors the ACMEsoftware brand-kit policy.

| File | Package / source | Version | Used by |
|------|------------------|---------|---------|
| `redoc.standalone.js` | `cdn.redocly.com/redoc/.../redoc.standalone.js` | 2.5.0 | Redoc pages (`redoc/*.html`) |
| `redocly-logo-mini.svg` | `cdn.redoc.ly/redoc/logo-mini.svg` | — | Redoc "API docs by Redocly" mark |
| `swagger-ui.css`, `swagger-ui-bundle.js`, `swagger-ui-standalone-preset.js` | npm `swagger-ui-dist` | 5.x | `swagger/index.html` |
| `graphiql.min.js`, `graphiql.min.css` | npm `graphiql` | 2.4.7 | `graphql-ui/index.html` |
| `react.production.min.js`, `react-dom.production.min.js` | `unpkg.com/react(-dom)@18/umd/...` | 18.x | GraphiQL peer deps |

Fonts (Archivo / Archivo Black / Space Mono, SIL OFL) live under `../fonts/`; brand styles in
`../brand.css`. These come from the acmesoftware-de brand kit.

## Patch applied to `redoc.standalone.js`

The upstream bundle hard-codes the "powered by" logo URL to `https://cdn.redoc.ly/redoc/logo-mini.svg`.
That one CDN reference is rewritten to the local copy:

    https://cdn.redoc.ly/redoc/logo-mini.svg  ->  /assets/vendor/redocly-logo-mini.svg

Redoc's prerender also emits a `<link>` to Google Fonts (Montserrat/Roboto); `api/portal-brandify.py`
strips that at build time. **When bumping the Redoc version, re-download the bundle and re-apply the
logo-URL rewrite** (and re-check the Google-Fonts strip still matches).
