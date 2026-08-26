// prefers-color-scheme reports the operating system's setting. That is the wrong
// question here: the widget is embedded in someone else's page, and a light site
// viewed by a reader whose laptop is in dark mode would get a black box halfway
// down the article. What matters is the colour the widget will actually sit on,
// so we look at the page instead of the OS — and let the site owner override.

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

export function detectTheme(container) {
  const explicit = container?.dataset?.gimme_comments_theme;
  if (explicit === 'light' || explicit === 'dark') return explicit;

  // Walk outwards until something actually paints a background. Most elements
  // are transparent, so the first opaque ancestor is what the reader sees.
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
