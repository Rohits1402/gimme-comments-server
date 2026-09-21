/**
 * People type "www.example.com", not "https://www.example.com". Refusing that is
 * pedantry — the scheme is obvious. Normalising also stops the same site being
 * registered twice under two spellings, which the unique constraint on the URL
 * would otherwise happily allow. Returns null when the input is not an address.
 */
export function normaliseUrl(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(withScheme);
    if (!url.hostname.includes('.') && url.hostname !== 'localhost') return null;
    return url.origin + url.pathname.replace(/\/+$/, '');
  } catch {
    return null;
  }
}

