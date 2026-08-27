/**
 * The GimmeComments mark: a hexagon with a G whose crossbar runs out to the right.
 * Redrawn from the 2023 logo as a path rather than shipped as a PNG, so it takes
 * the colour of whatever it sits in, stays sharp at any size, and needs no asset
 * file — which matters because the widget build inlines everything.
 */
export function LogoMark({ size = 26 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      aria-hidden="true"
      className="gc-logo-mark"
    >
      <path
        d="M16 2.2 28 9v14l-12 6.8L4 23V9z"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinejoin="round"
      />
      <path
        d="M20.5 12.4a5.6 5.6 0 1 0 .6 5.9h-5.2"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function Logo({ size = 26 }) {
  return (
    <span className="gc-wordmark">
      <LogoMark size={size} />
      <span>GimmeComments</span>
    </span>
  );
}
