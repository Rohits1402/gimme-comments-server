// prefers-color-scheme reports the operating system's setting. That is the wrong
// question here: the widget is embedded in someone else's page, and a light site
// viewed by a reader whose laptop is in dark mode would get a black box halfway
// down the article. What matters is the colour the widget will actually sit on.

function parseColor(value) {
  const match = value?.match(/rgba?\(([^)]+)\)/);
  if (!match) return null;
  const parts = match[1].split(/[\s,/]+/).filter(Boolean).map(Number);
  if (parts.length < 3 || parts.some(Number.isNaN)) return null;
  return { r: parts[0], g: parts[1], b: parts[2], a: parts.length > 3 ? parts[3] : 1 };
}

/** Relative luminance, the same weighting the WCAG contrast rules use. */
function luminance({ r, g, b }) {
  const channel = (v) => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

function detectFromPage(container) {
  // Walk outwards until something actually paints a background. Most elements are
  // transparent, so the first opaque ancestor is what the reader sees.
  let el = container;
  while (el) {
    const color = parseColor(getComputedStyle(el).backgroundColor);
    if (color && color.a > 0.1) {
      return luminance(color) < 0.4 ? 'dark' : 'light';
    }
    el = el.parentElement;
  }
  // Nothing on the page committed to a background. Browsers paint white in that
  // case unless the page opted into dark, which is what this query tells us.
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * Three sources, most specific first: the attribute on this page, the setting the
 * site owner saved in the dashboard, and finally what the page looks like.
 */
export function resolveTheme(container, config) {
  const onThisPage = container?.dataset?.gimme_comments_theme;
  if (onThisPage === 'light' || onThisPage === 'dark') return onThisPage;

  const saved = config?.theme;
  if (saved === 'light' || saved === 'dark') return saved;

  return detectFromPage(container);
}

// The accent arrives from the database and is written into a CSS custom property,
// so it is checked rather than trusted. Hex only: nothing else can carry a url()
// or a second declaration.
const HEX = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i;

export function safeAccent(value) {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  return HEX.test(trimmed) ? trimmed : null;
}
