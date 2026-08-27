# The GimmeComments dashboard

The landing page and the signed-in dashboard. React 19, Vite and react-router.

```bash
npm install
npm run dev     # http://localhost:5174, proxying /api to a local server on 8080
npm run build   # writes into ../src/main/resources/static/app
```

## How it is served

Spring serves the built files from `/app/**`, and `DashboardController` forwards each
of the app's routes to `/app/index.html` so the browser can ask for `/websites/<id>`
directly and still get the app.

Those routes are **listed** in that controller rather than caught with `/**`. A
controller mapping takes precedence over Spring's static resource handling, so a
catch-all would swallow `/build/**`, `/uploads/**` and the widget loader — and the
widget would stop loading on every site that embeds it. Adding a route to the app
means adding it in two places: `App.jsx` and `DashboardController`, plus `SecurityConfig`
if it should be reachable signed out.

## This is not the widget, and the rules are opposite

`client/` is injected into strangers' pages and is constrained accordingly: one IIFE
bundle, no code splitting, no emitted assets, every style scoped under `.gc-root`.

None of that applies here. This is an ordinary page the user navigates to, so modules,
code splitting, real asset files, a global reset and `prefers-color-scheme` are all
correct. The two apps share a visual language and almost no constraints.

One thing that does carry over: `outDir` is `static/app` and must never be
`static/build`. `InitializationController` scans `static/build/static/{js,css}` and the
loader injects whatever it finds into third-party pages, so dashboard code landing
there would be shipped to strangers' websites.

## The environment variable trap

`VITE_DEMO_WEBSITE_ID` is inlined at build time. `.env` holds the production demo
website; a local override belongs in **`.env.development.local`**, not `.env.local`.

Vite loads `.env.local` in *every* mode, including production builds. A local override
in that file gets compiled into the deployed bundle, and the failure is silent: the
landing page asks production for a website id that only exists on your machine, gets a
404, and quietly hides the live demo section. `.env.development.local` is only read by
`npm run dev`.

## Things worth knowing before changing it

**Three auth states, not two.** `store.jsx` tracks `ready` separately from `user`,
because "nobody is signed in" and "we have not asked yet" are different. Collapsing
them makes the dashboard flash its sign-in screen on every refresh.

**The overview is one request.** `GET /api/v1/overview` returns totals, fourteen days
of activity and the recent comments together. Do not rebuild that in the browser by
fetching every website and then every website's comments — that is what it replaced.

**The embed snippet is built from `window.location.origin`**, so it is correct on
localhost and in production. The 2023 panel hardcoded the server address, double slash
included.
