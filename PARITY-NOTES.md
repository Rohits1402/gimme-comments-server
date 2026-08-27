# Parity Notes — New Spring Boot Server vs Old Express Server

*Every place the port deliberately differs from the original `gimme-comments-server` (Express/Node), with the reason. The old server is the contract for **shapes** (JSON keys, envelopes, messages) but NOT for its **bugs** or its **loose status codes**. Settled properly on Day 25's parity audit.*

---

## Deliberate deviations (we changed this on purpose)

| # | Endpoint | Old behavior | New behavior | Why |
|---|----------|--------------|--------------|-----|
| 1 | All creates (`POST` website, comment, register) | returned **200 OK** | return **201 Created** | 201 is the correct HTTP status for "a resource was created". Any sane client treats 2xx as success. |
| 2 | Not-found cases (get/update/delete missing website or comment) | returned **400 Bad Request** with `{msg}` | return **404 Not Found** with `{msg}` | 404 is the honest status for "the thing you named does not exist". 400 means "your request was malformed", which is a different failure. |
| 3 | Validation failures | returned one field error message | return **all** field errors, sorted, joined by `, ` | Deterministic and complete. Old clients still get a single `{msg}` string, so the shape is preserved. |
| 4 | `update_website` (old `Website.js`) | had **no ownership filter** — any logged-in user could edit anyone's website | `update` and `delete` are owner-scoped through `getOwned(id, caller)`; a non-owner receives **404, not 403** | The old code had a genuine security hole. We do not port holes. The 404 also refuses to confirm that someone else's resource exists. Added Day 18. |
| 5 | `update_comment` (old `Comment.js`) | a missing comment returned **200 OK** with `comment: null` | returns **404** with `{msg}` | Returning 200 for a failed update is misleading. |
| 6 | `delete_comment` error message | printed `undefined` for the id (wrong variable in old code) | returns a correct, clean message | Old code bug, not a contract. |
| 7 | User deletion cascade | old hook queried likes by `on_website` with a user id → **never deleted a user's likes** (silent bug) | correctly deletes the user's likes via `deleteByUserId` | Old code bug. See Day 12. |
| 8 | Comment list (`get_all_comments`) | used `.populate('by_user')`, embedding the **entire user document** (including bcrypt password hash + OTP) in every public response | returns only safe comment fields via `CommentResponse` DTO | The old behavior was a live data-leak vulnerability. Our DTO architecture makes it structurally impossible. |
| 9 | Login failures | returned **"User does not exist"** (400) for unknown email but **"Invalid Credentials"** (401) for wrong password — two different messages | returns the **same** "Invalid credentials" (401) for both cases | The old behavior is a **user-enumeration** vulnerability: an attacker learns which emails are registered by comparing messages. Identical responses leak nothing. Account-state failures (email not verified / deactivated) still return their specific 403 reason, which is safe and useful to legitimate users. |
| 10 | `create_like` on a non-existent comment | old app created the like anyway (orphan data pointing at nothing) | returns **404 "Comment not found"** | No reason to knowingly create orphans; cascades keep integrity everywhere else. Also: `delete_like` under a deleted comment now answers 404 rather than the old 400 "has not liked". |
| 11 | Duplicate likes | only an application-level check ("already liked") — two concurrent requests could both pass it and create a double like | same courtesy check **plus a compound unique index** on (user, comment); the race now ends in 409 from the database | Checks in code are a courtesy; constraints in the database are a guarantee. The old schema had no such index. |
| 12 | OTP generation | hardcoded `OTP = 123456` (random commented out); the schema default was `Math.random()` evaluated once at load, so all users shared one OTP; no expiry; no single-use; `Number`-typed (drops leading zeros) | `SecureRandom` 6-digit code stored zero-padded as String in a separate `otp_tokens` table, 10-minute expiry, single-use (deleted on consume), expired rows swept whenever a new OTP is generated (the original MongoDB version used a TTL index; PostgreSQL has no equivalent, and the expiry check in `verify` was always what made it correct — Day 42), purpose-scoped (verification vs reset) | The old OTP was neither one-time, random, nor expiring. Real one-time-password semantics. |
| 13 | OTP generate-otp response | returned "OTP send to email" only if the email existed; unknown email threw "User does not exist" | always returns the same generic "If an account exists, an OTP has been sent"; no OTP created for unknown emails | User-enumeration defense, consistent with deviation #9. Verify/reset return "OTP is invalid" for unknown email too, indistinguishable from a wrong code. |
| 14 | `create_comment` on a non-existent website | old code validated the parent comment but never the website — a comment could be attached to a website id referring to nothing, creating orphan data | `CommentService.create` calls `websiteService.requireExists(websiteId)` first; an unknown website returns **404 "Website with given id not found"** | Same defect as deviation #10, one level up. MongoDB has no foreign keys, so referential integrity is the application's job and nothing else is checking. Reads stay permissive — `GET` on an unknown website still returns an empty list, because that is a truthful answer and the widget calls it on every page load. Added Day 26. |
| 15 | `DELETE /auth/profile/delete-profile` | deleted every comment the user had ever written, plus every reply beneath them | the comments stay; the author becomes empty and renders as "Deleted user" | PostgreSQL uses `ON DELETE SET NULL` on `comments.user_id`. Deleting your account should remove your account, not punch holes in threads other people are reading — which is what every large site does. `AuthorResponse.from(null)` was written on Day 36 and had been waiting for exactly this. Changed during the MongoDB to PostgreSQL migration, Day 43. |
| 16 | `GET /comments/comment/{websiteId}` comment fields | each comment was `comm.toJSON()` on the raw Mongoose document, so it carried `_id` and `createdAt` (Mongoose timestamps are camelCase), plus the fully populated `by_user` | `CommentResponse` returns `id`, `created_at`, and a `by_user` trimmed to `id`/`name`/`profile_image` | Follows from deviation #8. Once the response is a DTO instead of a raw document, the Mongoose field names go with it, and `email` is gone deliberately because this endpoint is public. **Consequence, recorded on Day 36 and confirmed against the live page on Day 47:** the bundled 2023 React widget still reads `_id`, `createdAt` and `by_user.email`, so the demo page renders "undefined | Invalid Date". The widget source lives in `legacy-mern/client` and can be rebuilt; adding `email` back is not an option. |
| 17 | Deleting a comment | only the **author** could delete a comment; a website owner had no way to remove anything from their own page | the author **or the owner of the website the comment sits on** may delete it; a non-owner still gets **404**, not 403, matching deviation #4 | A comments product whose customer cannot remove abuse from their own site is not finished. **Editing deliberately stays author-only** — removing someone's words is moderation, rewriting them and leaving their name on top is not. Covered by `CommentServiceTest`, the project's first service-level test, because the rule spans two different owners and a controller slice with a mocked service cannot see it. |
| 18 | `GET /websites/exists/{id}` | returned `{msg}` only | returns `{msg, website_configuration}` | **Additive, so the old contract still holds** and the faithful message text in the list below is unchanged. The widget's first call already had to happen; returning the site owner's saved settings in the same response means the widget can be told how to look without a second round trip. The column is the `jsonb` one added in `V4`, finally used for something. |
| 19 | The signed-in home | the old admin panel opened on a list of website names and nothing else; there was no way to see activity without going into each site | `GET /api/v1/overview` returns totals, fourteen days of comment counts and the newest comments across every website the caller owns, in one request | New capability rather than a ported one. **The response is shaped by the screen instead of by a table, which is unusual for this API and deliberate:** the alternative was the browser fetching every website, then every website's comments, to work out three numbers. Four aggregate queries plus one paged fetch beat that at any size. **This is the only endpoint that reaches across every website a person owns, so it is the one place a missing filter would leak another customer's comments** — every query filters on the owner of the website a comment sits on, and `OverviewOwnerIsolationTest` proves it against a real database, including the case where someone has written every comment in the system and owns nothing. Days are bucketed in UTC and grouped in Java, because date truncation is one of the things databases disagree about. |

## Endpoint coverage (audited Day 25)

**Parity has three separate dimensions, and checking one says nothing about the others:**

| Dimension | Audited | Found |
|---|---|---|
| Which endpoints exist | Day 25 | complete |
| Field names inside responses | Day 28 | `UserResponse` was camelCase where the original was snake_case |
| **Who is allowed to reach each endpoint** | **Day 35** | **`GET /websites/exists/{id}` required authentication; the original left it public on purpose, because the widget calls it as an anonymous visitor on a third-party site** |

That last one was introduced on Day 21 when the security config was narrowed from a blanket `permitAll` to an explicit list. The endpoint was ported correctly; the rule that made it public was not. It surfaced only when the widget was embedded from a real external origin on Day 35.

Every route in the original Express server exists in the Spring Boot port:

| Old route | Status |
|---|---|
| `GET /` | ✅ |
| `POST /auth/register`, `POST /auth/login` | ✅ |
| `POST /auth/account-verification/generate-otp`, `verify-account` | ✅ |
| `POST /auth/forget-password/generate-otp`, `verify-otp`, `PATCH change-password` | ✅ |
| `GET /auth/profile`, `PATCH update-profile`, `PATCH update-password`, `PATCH update-profile-image`, `DELETE delete-profile` | ✅ |
| `GET/POST /websites`, `GET/PATCH/DELETE /websites/{id}`, `GET /websites/exists/{id}` | ✅ |
| `GET/POST /comments/comment/{websiteId}`, `PATCH/DELETE /comments/comment/{commentId}` | ✅ |
| `POST/DELETE /comments/like/{commentId}` | ✅ |
| `GET /api/v1/initialization` (+ static widget at `/build/**`, `/initialize-gimme-comments.js`) | ✅ |

Scaffolding removed at this checkpoint: `HelloController` (Day 4 teaching endpoints) and `UserController` (`/api/v1/users/me`, the Day 17 JWT test endpoint, superseded by `GET /auth/profile`).

## Faithful shapes (we reproduce these exactly, on purpose)

- **Response envelopes:** `{websites:[...]}`, `{website:{...}}`, `{msg, website}`, `{comments:[...]}`, `{msg, comment}`, `{msg}`.
- **JSON key dialect (snake_case), including the irregular ones:** comment fields use `by_user`, `on_website`, `comment_parent` (not `user_id`/`website_id`/`parent_comment_id`) — handled with per-field `@JsonProperty`. Website `by_user` likewise.
- **Exact message texts**, even the odd ones: `"Website Profile deleted"`, `"Website found with id : {id}"`, `"No parent comment found with id : {id}"`.
- **User fields are snake_case too** — `profile_image` and `email_verified`, matching the old Mongoose `User` model. These were accidentally camelCase until **Day 28**, when the generated OpenAPI schema put every response side by side and made the odd one out obvious. The Day 25 audit missed it because that audit compared *endpoints*, not *field names within responses*. Affected register, `GET /auth/profile`, `update-profile`, and `update-profile-image`.
- **`GET /api/v1/initialization` is camelCase on purpose** — `jsFiles` and `cssFiles`, because the old `Initialization.js` returned `res.json({ jsFiles, cssFiles })` and the widget loader reads those exact keys.
- **No `user_id` in any request.** The old API never accepted one and neither does this one. Identity always comes from the JWT, resolved by `@AuthenticationPrincipal`.

## Faithful oddities carried on purpose

- **The comment routes overload one URL shape** — `/comment/{id}` means websiteId for GET and POST, but commentId for PATCH and DELETE. Ugly, and kept, because the React client calls these exact URLs and drop-in replacement is the whole point of the port. A v2 API would separate them. A lesson in what not to design fresh.
- **Missing-resource statuses are not uniform.** An unknown website on comment creation returns 404; an unknown *parent comment* returns 400 with the original wording, `"No parent comment found with id : {id}"`. Deliberate as of Day 26: the message text is listed above as faithful, and the 400 was kept rather than quietly changed. Worth revisiting in a v2.

## Temporary states — all resolved (kept as a record)

*This section used to describe the project's current state, and so it rotted: it claimed the API was unauthenticated and storing plain-text passwords for weeks after both stopped being true. Rewritten in the past tense on Day 26, which cannot go stale. The general rule this taught: **write down the "why", let the code state the "what".***

| Was temporary | Resolved |
|---|---|
| Every endpoint unauthenticated | Day 14 (filter chain) through Day 20 |
| Passwords stored as plain text | Day 15 (bcrypt, cost 10) |
| `Like` had no compound unique index, so double-likes were possible | Day 18 |
| `user_id` accepted in bodies and query params as scaffolding | Day 18 — removed from the contract entirely |
| Comment creation did not check that the website existed | Day 26 |
| `liked_by` and `i_liked` missing from the comment list — the old `get_all_comments` aggregated the Like collection and read the `Authorization` header *optionally*, so anonymous readers saw counts and a logged-in reader also got `i_liked`. Found Day 47; the Day 25 audit had missed it because that audit compared **endpoints**, and both like endpoints exist — the gap lived inside a *different* endpoint's response body, the same blind spot that hid the camelCase user fields until Day 28 | Day 49 — two batch queries (a grouped count, and the caller's liked ids), so the list costs three queries no matter how many comments. Create and update still send no like data, matching the old API: `liked_by` and `i_liked` are `Long`/`Boolean` and omitted when null, because zero is a claim and absent is the truth |
