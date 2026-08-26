import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { Logo, LogoMark } from '../Logo.jsx';

const DEMO_ID = import.meta.env.VITE_DEMO_WEBSITE_ID;

const STEPS = [
  ['Register your site', 'Add the address once. You get an id for it.'],
  ['Paste two lines', 'A div where the box goes, a script that fills it.'],
  ['That is the whole job', 'Readers sign in, comment and reply. You moderate from here.'],
];

const FEATURES = [
  ['Threaded replies', 'Conversations nest, up to four deep, then flatten so they stay readable.'],
  ['Likes that mean something', 'Counts are public. Whether you liked it is yours alone.'],
  ['Moderation', 'Remove anything from your own site. Nobody can rewrite what you wrote.'],
  ['Matches your design', "It reads your page's background and picks light or dark to suit. Or you choose."],
  ['Nothing follows your readers', 'No trackers, no third-party cookies, no advertising.'],
  ['Yours to run', 'MIT licensed, Spring Boot and PostgreSQL. Host it yourself if you would rather.'],
];

function Snippet() {
  const t = (cls, text) => <span className={cls}>{text}</span>;
  return (
    <pre className="gc-hero-code">
      {t('p', '<')}{t('e', 'div')} {t('a', 'id')}={t('s', '"gimme-comments-root"')}
      {'\n    '}{t('a', 'data-gimme_comments_website_id')}={t('s', '"…"')}{t('p', '></')}{t('e', 'div')}{t('p', '>')}
      {'\n'}{t('p', '<')}{t('e', 'script')} {t('a', 'src')}={t('s', '"…/initialize-gimme-comments.js"')}{t('p', '></')}{t('e', 'script')}{t('p', '>')}
    </pre>
  );
}

/** A still of the widget, built from the same pieces the real one uses. It cannot
 *  go out of date the way the 2023 screenshots did. */
function WidgetPreview() {
  return (
    <div className="gc-preview" aria-hidden="true">
      <div className="gc-preview-head">
        <span className="gc-preview-title">Comments</span>
        <span className="gc-preview-chip">
          <span className="gc-preview-av">RC</span>Rohit
        </span>
      </div>
      <div className="gc-preview-count">3 comments</div>
      <div className="gc-preview-box">
        <span className="gc-preview-ph">Add a comment…</span>
        <span className="gc-preview-btn">Comment</span>
      </div>

      <div className="gc-preview-c">
        <span className="gc-preview-av gc-preview-av-lg">AK</span>
        <div>
          <div className="gc-preview-meta">
            <b>Abhianv Kashyap</b> <span>2 hours ago</span>
          </div>
          <p>This dropped straight into my blog. Took about a minute.</p>
          <div className="gc-preview-acts">
            <span className="on">♥ 4</span>
            <span>↩ Reply</span>
          </div>
        </div>
      </div>

      <div className="gc-preview-c gc-preview-reply">
        <span className="gc-preview-av">VG</span>
        <div>
          <div className="gc-preview-meta">
            <b>Vishesh Gupta</b> <span>an hour ago</span>
          </div>
          <p>Same. The dark theme picked itself up automatically.</p>
          <div className="gc-preview-acts">
            <span>♡ 1</span>
            <span>↩ Reply</span>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * The real widget, embedded on our own page. Nothing is more convincing for a
 * comments product than its own comments working — but it is checked first, so a
 * deleted demo website removes the section instead of showing a broken box.
 */
function LiveDemo() {
  const [show, setShow] = useState(false);
  const mounted = useRef(false);

  useEffect(() => {
    if (!DEMO_ID) return;
    let cancelled = false;
    api
      .get(`/websites/exists/${DEMO_ID}`)
      .then(() => !cancelled && setShow(true))
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!show || mounted.current) return;
    mounted.current = true;
    const script = document.createElement('script');
    script.src = `${window.location.origin}/initialize-gimme-comments.js`;
    document.body.appendChild(script);
  }, [show]);

  if (!show) return null;

  return (
    <section className="gc-live">
      <div className="gc-section-label">See it working</div>
      <h2 className="gc-section-title">This is the widget, on this page, right now.</h2>
      <p className="gc-section-sub">
        Not a screenshot. Leave a comment and it is stored by the same server serving
        this page.
      </p>
      <div id="gimme-comments-root" data-gimme_comments_website_id={DEMO_ID} />
    </section>
  );
}

export default function Landing() {
  return (
    <div className="gc-landing">
      <header className="gc-landing-nav">
        <Logo />
        <nav>
          <a href="#how">How it works</a>
          <a href="/swagger-ui.html">API docs</a>
          <a
            href="https://github.com/Rohits1402/gimme-comments-server"
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </a>
          <Link to="/sign-in" className="gc-nav-signin">
            Sign in
          </Link>
          <Link to="/sign-in" className="gc-cta">
            Get started
          </Link>
        </nav>
      </header>

      <section className="gc-hero">
        <div className="gc-hero-text">
          <span className="gc-pill">Free and open source</span>
          <h1>
            Comments on your site
            <br />
            in two lines of HTML.
          </h1>
          <p>
            Threaded replies, likes and moderation, hosted for you. No database to run,
            no scripts to write, nothing tracking your readers.
          </p>
          <div className="gc-hero-cta">
            <Link to="/sign-in" className="gc-cta gc-cta-lg">
              Get started free
            </Link>
            <a href="#how" className="gc-cta-alt">
              See it live
            </a>
          </div>
          <Snippet />
        </div>
        <WidgetPreview />
      </section>

      <section className="gc-how" id="how">
        <div className="gc-section-label">How it works</div>
        <div className="gc-steps">
          {STEPS.map(([title, body], i) => (
            <div className="gc-step" key={title}>
              <span className="gc-step-n">{i + 1}</span>
              <div className="gc-step-title">{title}</div>
              <p>{body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="gc-features">
        {FEATURES.map(([title, body]) => (
          <div key={title}>
            <div className="gc-feature-title">{title}</div>
            <p>{body}</p>
          </div>
        ))}
      </section>

      <LiveDemo />

      <footer className="gc-landing-foot">
        <span className="gc-wordmark">
          <LogoMark size={18} />
          <span>GimmeComments</span>
        </span>
        <div>
          <a href="/swagger-ui.html">API docs</a>
          <a
            href="https://rohits1402.github.io/gimme-comments-server/"
            target="_blank"
            rel="noopener noreferrer"
          >
            Live demo
          </a>
          <a
            href="https://github.com/Rohits1402/gimme-comments-server"
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </a>
          <span>MIT</span>
        </div>
      </footer>
    </div>
  );
}
