/**
 * sockjs-client / browser-crypto expect Node's `global`.
 * Must run before any app modules that import those libraries.
 */
(globalThis as typeof globalThis & { global: typeof globalThis }).global = globalThis;
