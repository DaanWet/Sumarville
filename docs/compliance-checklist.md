# Compliance checklist — Sumarville

> Practical, step-by-step guide for roadmap item 1 (see [ANALYSE.md](../ANALYSE.md)).
> Goal: stay operational past **100 servers** and **10,000 unique users**, and satisfy GDPR.
> Created 14 June 2026.

There are **two separate Discord gates** that can apply at this scale, plus GDPR obligations:

- **Gate A — App verification** (required to grow past **100 servers**).
- **Gate B — Privileged Intent review** (2025 rule: triggered at **10,000 unique users**). Once you cross the threshold you get a notification and a **90-day clock** to submit; the Bot keeps working in the meantime.

The Bot keeps working during review, so the deadline is the thing to manage — **start early.**

---

## 0. Things you must fill in or set up first

These appear as placeholders in the documents and the texts below. Resolve them before going live:

- [x] **Bot name** — "Sumarville" (the bot's existing name; used throughout these docs). Make sure it matches the application name in the Developer Portal.
- [x] **Governing-law country** — Belgium (set in [terms-of-service.md](./terms-of-service.md) §10).
- [x] **`privacy@wettinck.be`** — email forwarding to the owner's private inbox is set up.
- [x] **Discord support server** — invite added to both legal docs: https://discord.gg/4Atn8t2 (confirm it is set to **never expire**).
- [ ] **Publish via GitHub Pages** on `sumarville.wettinck.be` — enable Pages from `/docs`, set the custom domain + DNS (see Appendix B). Then paste the URLs into the portal (Section 3).

---

## 1. Secure the owner account

- [ ] Enable **2FA** on the Discord account that owns the application (Authenticator app recommended).
- [ ] Verify the account's **email** and **phone number**.

> 2FA on the owner account is mandatory for verification and intent review.

---

## 2. Identity verification

- [ ] In the **Developer Portal → your application**, complete **identity verification** (government ID via Discord's verification partner). This is private to Discord; it does **not** appear in your public policies.

---

## 3. Publish the legal documents

- [ ] Publish **Privacy Policy** ([privacy-policy.md](./privacy-policy.md)) and **Terms of Service** ([terms-of-service.md](./terms-of-service.md)) via GitHub Pages (Appendix B).
- [ ] In **Developer Portal → your application → General Information**, paste:
  - **Privacy Policy URL:** `https://sumarville.wettinck.be/privacy-policy.html`
  - **Terms of Service URL:** `https://sumarville.wettinck.be/terms-of-service.html`

---

## 4. Submit app verification (Gate A — at 100 servers)

- [ ] In the Developer Portal, start the **verification** flow.
- [ ] Provide: a clear description of what the Bot does, the policy URLs (Section 3), and complete the identity step (Section 2).
- [ ] Submit and wait for Discord's response; respond promptly to any follow-up.

---

## 5. Submit privileged-intent review (Gate B — at 10,000 users)

You must justify **each** privileged intent with a concrete use case and describe your data handling. Ask for the **minimum** scope.

| Intent | Request it? | Notes |
|---|---|---|
| `GUILD_MEMBERS` | **Yes** | Needed for roles, nicknames, and the DM-check. Justification ready in Appendix A. |
| `MESSAGE_CONTENT` | **Avoid** | Currently used for prefix-command parsing + DM relay. Discord **rejects** this when the feature can be done via slash commands — which is exactly this case. **Plan: migrate to slash commands (roadmap item 2), then this intent is no longer needed.** Do not build your compliance plan around getting it approved. |
| `PRESENCE` | No | Not used. |

- [ ] Submit the **`GUILD_MEMBERS`** request using the use-case text in **Appendix A**.
- [ ] Attach the **data-handling description** in **Appendix A**.
- [ ] Do **not** request `MESSAGE_CONTENT`; prioritise the slash-command migration instead.

---

## 6. Keep it alive

- [ ] **Annual re-application** for privileged intents is required — set a yearly reminder.
- [ ] Keep the legal docs' "Last updated" date current whenever the data practices change.

---

## Appendix A — Ready-to-paste texts for the intent review

### A.1 `GUILD_MEMBERS` use-case justification

> Sumarville uses the Server Members (GUILD_MEMBERS) intent to: (1) assign the server-configured "Player" role to a member when they create a character, and to recognise members holding the "Dungeon Master" role; (2) set a member's server nickname to their chosen character name; and (3) verify whether a member who direct-messages the bot holds the Dungeon Master role before storing their message as in-game NPC dialogue for that server. The bot reads member role membership and updates roles/nicknames **only in direct response to an explicit user command**. It does not build a standalone member database, does not track presence, and does not process member data for any purpose other than delivering these features within the server that issued the command.

### A.2 Data-handling description

> **Data stored:** Discord user IDs, user-supplied character names and avatar image URLs, server (guild) IDs, admin-configured role and channel IDs, and user-submitted feature content (calendar session dates, food/lunch entries, and NPC dialogue text).
> **Storage & location:** stored per-server on infrastructure controlled solely by the bot operator, located in the EU, with access restricted to the operator.
> **Retention:** retained while the bot is present in the server; deleted when the bot is removed, or upon a verified erasure request, without undue delay and within 30 days.
> **Sharing:** never sold or shared with third parties; processed only to provide the bot's features.
> **User rights:** users may request access to or deletion of their data via privacy@wettinck.be.
> **Policy:** full Privacy Policy published at https://sumarville.wettinck.be/privacy-policy.html.

---

## Appendix B — Publishing via GitHub Pages

The legal pages live in the **`docs/`** folder (`docs/privacy-policy.md`, `docs/terms-of-service.md`, `docs/index.md`, `docs/_config.yml`, `docs/CNAME`). Serving Pages from `/docs` publishes **only** these pages — never `src/` or `Data.json`. The site is hosted on the custom domain **`sumarville.wettinck.be`**.

**Setup (do these on github.com / your DNS):**

1. Push `master` (the `docs/` folder must be on GitHub).
2. *(Optional, decided)* Rename the repo: **Settings → General → Repository name → `Sumarville` → Rename**. GitHub auto-redirects old links. Afterwards update your local remote — see the note below. (With the custom domain the repo name does not appear in the public URLs.)
3. **Settings → Pages → Build and deployment → Source** → **Deploy from a branch** → **Branch** = `master`, **Folder** = `/docs` → **Save**.
4. **DNS** (at the wettinck.be DNS provider, e.g. Cloudflare): add a **CNAME** record — name `sumarville`, target `daanwet.github.io` (no repo path), proxy **DNS only** (grey cloud).
5. **Settings → Pages → Custom domain** → `sumarville.wettinck.be` → **Save**. GitHub picks up the `docs/CNAME` file and runs a DNS check.
6. When the DNS check is green, tick **Enforce HTTPS** (certificate can take a few minutes up to 24h).

**Resulting public URLs (HTTPS):**

- Landing: `https://sumarville.wettinck.be/`
- Privacy Policy: `https://sumarville.wettinck.be/privacy-policy.html`
- Terms of Service: `https://sumarville.wettinck.be/terms-of-service.html`

> These are already filled into Section 3 and Appendix A.2. The `_config.yml` applies a theme (Cayman) so the pages render as styled HTML, and excludes this checklist from the published site.

> **After renaming the repo**, update the local git remote:
> `git remote set-url origin https://github.com/DaanWet/Sumarville.git`

---

## Appendix C — Setting up `privacy@wettinck.be` (free forwarding)

You do **not** need paid mail hosting; forwarding is enough to receive GDPR requests.

- **Cloudflare Email Routing (free):** move `wettinck.be`'s DNS to Cloudflare → Email → Email Routing → add a route `privacy@wettinck.be` → forwards to your private inbox.
- **ImprovMX (free tier):** add the MX records they provide to `wettinck.be` → create the `privacy@` alias → forwards to your private inbox.

To reply from the alias, set up "Send mail as" in Gmail/your client (optional — receiving is what GDPR requires). Your private address never appears publicly; only `privacy@wettinck.be` does.

---

## Notes / links

- This checklist covers roadmap **item 1** only. The `MESSAGE_CONTENT` risk is fully resolved by roadmap **item 2** (JDA 6 + slash commands).
- Sources are listed at the bottom of [ANALYSE.md](../ANALYSE.md).
