/** Build stamp baked in at build time (see vite.config.ts). */
export const BUILD = {
  version: __APP_VERSION__,
  commit: __GIT_COMMIT__,
  time: __BUILD_TIME__,
} as const

/** Short human-readable label, e.g. "v0.1.0 · a1b2c3d". */
export const versionLabel = `v${BUILD.version} · ${BUILD.commit}`
