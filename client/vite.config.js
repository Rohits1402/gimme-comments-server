import { rmSync } from 'node:fs';
import { resolve } from 'node:path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// This app is not loaded as a page. initialize-gimme-comments.js asks the server
// which files exist, then appends a plain <script src="..."> for each one onto a
// third-party page. That has three consequences, and all three are enforced here:
//
//  1. A classic <script> cannot execute ES module output, so the bundle is IIFE
//     and dynamic imports are inlined — one JS file, no chunks.
//  2. InitializationController scans static/build/static/{js,css} and the loader
//     rebuilds the URLs as /build/static/js/<name>, so the output paths are fixed.
//  3. Asset URLs would resolve against the HOST page's origin, not ours, so every
//     asset is inlined as a data URI rather than emitted as a file.
/**
 * index.html exists so `npm run dev` has something to serve — it is the harness that
 * stands in for a third-party page. Vite treats it as the build entry and emits a
 * copy, which then ships inside the jar and is publicly reachable at
 * /build/index.html: a page titled "dev harness" on the production domain, with a
 * script tag pointing at the wrong path because the emitted HTML assumes it is served
 * from the root. Nothing loads it and nothing should find it, so it is dropped from
 * the output instead.
 */
function dropHarnessHtml() {
  // Held in a closure, not on the plugin object: `this` inside a Vite hook is the
  // plugin context, so a value stashed on it in one hook is gone by the next.
  let outDir;

  return {
    name: 'drop-harness-html',
    configResolved(config) {
      outDir = config.build.outDir;
    },
    // Deleted after the write rather than removed from the bundle: Vite emits the
    // HTML from its own plugin, which runs after this one, so generateBundle never
    // sees it.
    closeBundle() {
      rmSync(resolve(outDir, 'index.html'), { force: true });
    },
  };
}

export default defineConfig({
  plugins: [react(), dropHarnessHtml()],
  build: {
    outDir: '../src/main/resources/static/build',
    emptyOutDir: true,
    assetsInlineLimit: Number.MAX_SAFE_INTEGER,
    rollupOptions: {
      output: {
        format: 'iife',
        inlineDynamicImports: true,
        entryFileNames: 'static/js/gimme-comments.[hash].js',
        assetFileNames: 'static/css/gimme-comments.[hash][extname]',
      },
    },
  },
});
