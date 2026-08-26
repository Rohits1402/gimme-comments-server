import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { detectTheme } from './theme.js';
import './styles.css';

const container = document.getElementById('gimme-comments-root');

if (!container) {
  // Nothing to render into. Say so plainly instead of failing silently — the
  // site owner has pasted the script tag but not the div.
  console.error(
    'GimmeComments: no element with id "gimme-comments-root" was found on this page.'
  );
} else {
  // The old client fell back to a hardcoded website id when this attribute was
  // missing, which meant a misconfigured page quietly showed someone else's
  // comments. A missing id is a configuration error and is reported as one.
  const websiteId = container.dataset.gimme_comments_website_id || null;

  createRoot(container).render(
    <StrictMode>
      <App websiteId={websiteId} theme={detectTheme(container)} />
    </StrictMode>
  );
}
