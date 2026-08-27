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
export default defineConfig({
  plugins: [react()],
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
