import { useState } from 'react';
import { api } from './api.js';
import { useStore } from './store.jsx';
import { Avatar, Button, IconEdit, IconHeart, IconReply, IconTrash, TimeAgo } from './ui.jsx';

const MAX_DEPTH = 4;

/** The API returns a flat list; comment_parent points at the comment above. */
function buildTree(comments) {
  const byId = new Map(comments.map((c) => [c.id, { ...c, replies: [] }]));
  const roots = [];

  for (const node of byId.values()) {
    const parent = node.comment_parent ? byId.get(node.comment_parent) : null;
    // A reply whose parent is missing would otherwise vanish from the page.
    if (parent) parent.replies.push(node);
    else roots.push(node);
  }

  const byOldest = (a, b) => new Date(a.created_at) - new Date(b.created_at);
  roots.sort(byOldest);
  for (const node of byId.values()) node.replies.sort(byOldest);
  return roots;
}

const URL_OR_MARK = /(https?:\/\/[^\s<]+)|(\*\*[^*]+\*\*)|(`[^`]+`)/g;

/**
 * A deliberately small formatter: links, bold, inline code, and line breaks.
 * It returns React elements and never touches innerHTML, so a comment cannot
 * inject markup no matter what it contains.
 */
function formatBody(text) {
  // Trailing newlines would otherwise render as blank lines above the actions.
  return text.trimEnd().split('\n').map((line, lineIndex) => {
    const parts = [];
    let last = 0;

    for (const match of line.matchAll(URL_OR_MARK)) {
      if (match.index > last) parts.push(line.slice(last, match.index));
      const [token] = match;

      if (token.startsWith('http')) {
        parts.push(
          <a
            key={match.index}
            href={token}
            target="_blank"
            rel="noopener noreferrer nofollow ugc"
          >
            {token}
          </a>
        );
      } else if (token.startsWith('**')) {
        parts.push(<strong key={match.index}>{token.slice(2, -2)}</strong>);
      } else {
        parts.push(<code key={match.index}>{token.slice(1, -1)}</code>);
      }
      last = match.index + token.length;
    }

    if (last < line.length) parts.push(line.slice(last));
    return (
      <span key={lineIndex} className="gc-line">
        {parts.length ? parts : ' '}
      </span>
    );
  });
}

function Composer({ parentId, initialValue = '', submitLabel, onDone, onCancel }) {
  const { websiteId, loadComments, notify } = useStore();
  const [text, setText] = useState(initialValue);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    const body = text.trim();
    if (!body) return;

    setBusy(true);
    try {
      await onDone(body);
      setText('');
      await loadComments();
    } catch (err) {
      notify('error', err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="gc-composer" onSubmit={submit}>
      <textarea
        className="gc-textarea"
        rows={parentId || initialValue ? 2 : 3}
        placeholder={parentId ? 'Write a reply…' : 'Add a comment…'}
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      <div className="gc-composer-actions">
        <span className="gc-hint">**bold**, `code`, links</span>
        <div className="gc-row">
          {onCancel ? (
            <Button size="sm" onClick={onCancel}>
              Cancel
            </Button>
          ) : null}
          <Button
            variant="primary"
            size="sm"
            type="submit"
            busy={busy}
            disabled={!text.trim()}
          >
            {submitLabel}
          </Button>
        </div>
      </div>
    </form>
  );
}

function CommentNode({ comment, depth, onRequireAuth }) {
  const { websiteId, user, comments, setComments, loadComments, notify } = useStore();
  const [replying, setReplying] = useState(false);
  const [editing, setEditing] = useState(false);

  const isMine = Boolean(user?.id && comment.by_user?.id === user.id);

  const toggleLike = async () => {
    if (!user) return onRequireAuth();

    const liked = comment.i_liked;
    // Update in place first. Refetching the whole list on every click makes a
    // like feel slow, and the change we are making is one we can predict exactly.
    setComments((current) =>
      current.map((c) =>
        c.id === comment.id
          ? { ...c, i_liked: !liked, liked_by: (c.liked_by ?? 0) + (liked ? -1 : 1) }
          : c
      )
    );

    try {
      if (liked) await api.delete(`/comments/like/${comment.id}`);
      else await api.post(`/comments/like/${comment.id}`);
    } catch (err) {
      notify('error', err.message);
      // Our prediction was wrong, so ask the server what is actually true.
      loadComments();
    }
  };

  const remove = async () => {
    try {
      await api.delete(`/comments/comment/${comment.id}`);
      await loadComments();
    } catch (err) {
      notify('error', err.message);
    }
  };

  return (
    <li className="gc-comment">
      <Avatar user={comment.by_user} size={depth === 0 ? 36 : 28} />

      <div className="gc-comment-main">
        <div className="gc-comment-head">
          <span className="gc-name">{comment.by_user?.name || 'Deleted user'}</span>
          <TimeAgo iso={comment.created_at} />
        </div>

        {editing ? (
          <Composer
            initialValue={comment.comment_description}
            submitLabel="Save"
            onCancel={() => setEditing(false)}
            onDone={async (body) => {
              await api.patch(`/comments/comment/${comment.id}`, {
                comment_description: body,
              });
              setEditing(false);
            }}
          />
        ) : (
          <div className="gc-body">{formatBody(comment.comment_description)}</div>
        )}

        <div className="gc-actions">
          <button
            type="button"
            className={`gc-action${comment.i_liked ? ' gc-action-on' : ''}`}
            onClick={toggleLike}
          >
            <IconHeart filled={comment.i_liked} />
            {comment.liked_by ?? 0}
          </button>

          {depth < MAX_DEPTH ? (
            <button
              type="button"
              className="gc-action"
              onClick={() => (user ? setReplying((v) => !v) : onRequireAuth())}
            >
              <IconReply />
              Reply
            </button>
          ) : null}

          {isMine ? (
            <>
              <button
                type="button"
                className="gc-action"
                onClick={() => setEditing((v) => !v)}
              >
                <IconEdit />
                Edit
              </button>
              <button type="button" className="gc-action gc-action-danger" onClick={remove}>
                <IconTrash />
                Delete
              </button>
            </>
          ) : null}
        </div>

        {replying ? (
          <Composer
            parentId={comment.id}
            submitLabel="Reply"
            onCancel={() => setReplying(false)}
            onDone={async (body) => {
              await api.post(`/comments/comment/${websiteId}`, {
                comment_description: body,
                comment_parent: comment.id,
              });
              setReplying(false);
            }}
          />
        ) : null}

        {comment.replies.length ? (
          <ul className="gc-thread">
            {comment.replies.map((reply) => (
              <CommentNode
                key={reply.id}
                comment={reply}
                depth={depth + 1}
                onRequireAuth={onRequireAuth}
              />
            ))}
          </ul>
        ) : null}
      </div>
    </li>
  );
}

export default function Comments({ onRequireAuth }) {
  const { websiteId, comments, user } = useStore();
  const roots = buildTree(comments);
  const total = comments.length;

  return (
    <div className="gc-comments">
      <div className="gc-count">
        {total === 0 ? 'No comments yet' : total === 1 ? '1 comment' : `${total} comments`}
      </div>

      {user ? (
        <Composer
          submitLabel="Comment"
          onDone={(body) =>
            api.post(`/comments/comment/${websiteId}`, { comment_description: body })
          }
        />
      ) : (
        <button type="button" className="gc-signin-prompt" onClick={onRequireAuth}>
          Sign in to join the conversation
        </button>
      )}

      {roots.length ? (
        <ul className="gc-thread gc-thread-root">
          {roots.map((c) => (
            <CommentNode
              key={c.id}
              comment={c}
              depth={0}
              onRequireAuth={onRequireAuth}
            />
          ))}
        </ul>
      ) : null}
    </div>
  );
}
