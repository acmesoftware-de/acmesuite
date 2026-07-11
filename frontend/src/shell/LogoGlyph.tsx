// ACMEsoftware Bar-Mark: four ASCENDING bars in the fixed brand color sequence
// (Red · Yellow · Blue · Green). Geometry and colors per brand.acmesoftware.de.
// Colors come from the brand tokens (--mark-*) via the fill classes — never themed.
export function LogoGlyph() {
  return (
    <svg className="acme-logo" viewBox="0 0 37 40" role="img" aria-label="ACMEsuite" xmlns="http://www.w3.org/2000/svg">
      <rect className="acme-logo-bar acme-logo-bar--crm" x="0" y="24" width="7" height="16" />
      <rect className="acme-logo-bar acme-logo-bar--hr" x="10" y="14" width="7" height="26" />
      <rect className="acme-logo-bar acme-logo-bar--bld" x="20" y="6" width="7" height="34" />
      <rect className="acme-logo-bar acme-logo-bar--sup" x="30" y="0" width="7" height="40" />
    </svg>
  )
}
