// Theme registry. Importing this module pulls in the whole look:
//   1. the shared component layer (class contract), then
//   2. each theme's tokens + fonts (bundled — no CDN).
// All themes are bundled and coexist; the active one is selected by the `data-theme`
// attribute on the app root (see src/theme/ThemeProvider + src/App).
//
// To add a theme: create themes/<id>/{tokens.css,fonts.css,meta.ts}, then import its
// tokens and register its meta below. No changes to src/ are required.

import './brand.css'
import './base/components.css'

import './acme/tokens.css'
import './paper/tokens.css'

import { acmeMeta } from './acme/meta'
import { paperMeta } from './paper/meta'
import type { ThemeMeta } from '../src/theme/types'

export const THEMES: ThemeMeta[] = [acmeMeta, paperMeta]

export const DEFAULT_THEME_ID = acmeMeta.id
