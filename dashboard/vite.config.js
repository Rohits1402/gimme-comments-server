import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Unlike the widget, this is an ordinary page the user navigates to, so none of the
// widget's constraints apply: modules, code splitting and real asset files are all
// fine here. Two things do matter.
//
//  1. base is '/app/' because that is where the built files are served from, and
//     index.html has to reference them absolutely — the dashboard is reached at '/'
//     and at nested routes like '/websites/<id>', so relative asset paths would
//     resolve differently depending on the route.
//  2. outDir is static/app, never static/build. InitializationController scans
//     static/build/static/{js,css} and the loader injects whatever it finds into
//     third-party pages; dashboard code landing there would be shipped to strangers.
export default defineConfig(({ mode }) => {
  // A config file runs in Node, where import.meta.env does not exist and process.env
  // holds only real environment variables. loadEnv is what reads the .env files.
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    base: '/app/',
    build: {
      outDir: '../src/main/resources/static/app',
      emptyOutDir: true,
    },
    server: {
      port: 5174,
      proxy: {
        // Defaults to a locally running server. Point it at a deployed one for a
        // look at real data: VITE_PROXY_TARGET=https://… in .env.local
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
