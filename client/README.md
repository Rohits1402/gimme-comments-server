# The GimmeComments widget

The embeddable comment box. React 19 and Vite, no runtime dependencies beyond React
itself.

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # writes into ../src/main/resources/static/build
```

`index.html` is a development harness only — it stands in for the third-party page
the widget is embedded into, and is deliberately styled with a serif font, a pink
button and an orange link so that CSS leaking in either direction is obvious. It is
not what gets served.

## How it reaches a page

A site owner pastes two things:

```html
<div id="gimme-comments-root" data-gimme_comments_website_id="..."></div>
<script src="https://your-server/initialize-gimme-comments.js"></script>
```

That script asks `GET /api/v1/initialization` which files exist under
`static/build/static/{js,css}`, then appends a plain `<script src="...">` for each.

Four constraints follow from that, and all four are enforced in `vite.config.js`:

1. **The bundle must be IIFE.** A classic `<script>` cannot execute ES module
   output, which is what Vite emits by default.
2. **No code splitting.** The loader injects the files the server found; a chunk
   fetched at runtime would resolve against the host page's origin, not ours.
3. **The output paths are fixed** at `static/js/` and `static/css/`, because that
   is where `InitializationController` looks and how the loader rebuilds the URL.
4. **No emitted assets.** An asset URL would also resolve against the host page, so
   `assetsInlineLimit` is set high enough that everything becomes a data URI.
   Icons are inline SVG for the same reason.

## Things worth knowing before changing it

**The API origin is not known at build time.** The loader reads the URL it was
itself served from and leaves it on `window.__GIMME_COMMENTS_API__`. `api.js` reads
that, falling back to `VITE_API_BASE` for `npm run dev`. Do not bake a URL in — the
same bundle is served from localhost and from production.

**Every style is scoped under `.gc-root`** — every single one, including plain class
selectors. No global reset, no bare element selectors, no CSS framework.

This is stricter than it first appears, and getting it half right is worse than
obvious. An early version scoped the root block but left rules like `.gc-input` and
`.gc-btn` as bare class selectors, relying on the `--gc-*` custom properties being
undefined outside `.gc-root` to keep them harmless. They were not harmless. The
dashboard uses the same class names, the widget's stylesheet is injected *after* the
page's own, and equal specificity means last one wins — so embedding the widget on
the dashboard silently turned its inputs and buttons transparent. A host page using
any `gc-` prefixed class would have broken the same way.

Element selectors were never the real risk. Class-name collision was.

**The theme comes from the host page, not the operating system.**
`prefers-color-scheme` reports the reader's OS setting, which says nothing about the
page the widget was embedded into — a light site would get a black box. `theme.js`
walks up from the container to the first element that actually paints a background
and measures its luminance instead. A site owner can override it with
`data-gimme_comments_theme="light"` or `"dark"`.

**The token is read on every request, never cached.** The old client built one axios
instance at startup and captured the token then, so after signing in it kept sending
the header it had at page load.
