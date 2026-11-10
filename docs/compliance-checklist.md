# Compliance checklist — Sumarville

> Practical, step-by-step guide for roadmap item 1 (see [ANALYSE.md](../ANALYSE.md)).
> Goal: stay operational past **100 servers** and **10,000 unique users**, and satisfy GDPR.
> Created 14 June 2026. Updated 10 November 2026.

> **Status (10 November 2026):** the legal pages are live and the repo is renamed to `Sumarville`. The code for roadmap items 2–5 (slash commands, encrypted database) is on `master` but **not yet deployed**. Do the deploy first, then fix the gaps marked **⚠** below, and only then submit app verification. Since commit `c311cbe` the bot requests **no privileged intents**, so Gate B (Section 5) no longer applies. Unticked boxes in sections 1–6 are portal steps that cannot be checked from the repo: they may already be done.

There are **two separate Discord gates** that can apply at this scale, plus GDPR obligations:

- **Gate A — App verification** (required to grow past **100 servers**).
- **Gate B — Privileged Intent review** (2025 rule: triggered at **10,000 unique users**). Once you cross the threshold you get a notification and a **90-day clock** to submit; the Bot keeps working in the meantime. **Does not apply anymore:** the bot requests no privileged intents (see Section 5).

The Bot keeps working during review, so the deadline is the thing to manage — **start early.**

---

## 0. Things you must fill in or set up first

These appear as placeholders in the documents and the texts below. Resolve them before going live:

- [x] **Bot name** — "Sumarville" (the bot's existing name; used throughout these docs). Make sure it matches the application name in the Developer Portal.
- [x] **Governing-law country** — Belgium (set in [terms-of-service.md](./terms-of-service.md) §11).
- [x] **`privacy@wettinck.be`** — email forwarding to the owner's private inbox is set up.
- [x] **Discord support server** — invite added to both legal docs: https://discord.gg/4Atn8t2 (confirm it is set to **never expire**).
- [x] **Publish via GitHub Pages** on `sumarville.wettinck.be` — enable Pages from `/docs`, set the custom domain + DNS (see Appendix B). Then paste the URLs into the portal (Section 3).

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

- [x] Publish **Privacy Policy** ([privacy-policy.md](./privacy-policy.md)) and **Terms of Service** ([terms-of-service.md](./terms-of-service.md)) via GitHub Pages (Appendix B).
- [ ] **⚠ Bring the Privacy Policy in line with the new build — push only after the deploy** (Pages publishes `docs/` immediately):
  - §2: remove the row "Direct message content (Dungeon Masters)". Since the slash commands, NPC dialogue is added with `/npc add` and the bot no longer reads direct messages.
  - §5: mention that stored data is encrypted at rest (SQLCipher).
  - §6 promises deletion "when the Bot is removed from the server", but no code does this yet. Either build it (also for servers the bot left while offline) or change the text.
  - Update the "Last updated" date.
- [ ] In **Developer Portal → your application → General Information**, paste:
  - **Privacy Policy URL:** `https://sumarville.wettinck.be/privacy-policy.html`
  - **Terms of Service URL:** `https://sumarville.wettinck.be/terms-of-service.html`

---

## 4. Submit app verification (Gate A — at 100 servers)

- [ ] In the Developer Portal, start the **verification** flow.
- [ ] Provide: a clear description of what the Bot does, the policy URLs (Section 3), and complete the identity step (Section 2).
- [ ] If asked how data is handled, use the **data-handling description** in **Appendix A.2**. Deploy the new build first: the encryption at rest it describes only exists in the new build.
- [ ] Submit and wait for Discord's response; respond promptly to any follow-up.

---

## 5. Privileged-intent review (Gate B — at 10,000 users) — not needed

**Not needed anymore.** Since commit `c311cbe` the bot requests no privileged intents, so there is nothing to justify.

| Intent | Request it? | Notes |
|---|---|---|
| `GUILD_MEMBERS` | **No** | Dropped in `c311cbe`. The caller's roles and permissions arrive with each slash command; nicknames are changed after fetching the one member over REST; `/character show` mentions players by user ID. Trade-off: `/dm claim` now needs the **Manage Roles** permission, because without the intent the bot cannot see whether someone already holds the DM role. |
| `MESSAGE_CONTENT` | **No** | No longer used: all commands are slash commands since roadmap item 2, and the bot no longer reads direct messages. |
| `PRESENCE` | No | Not used. |

- [ ] After the deploy: in **Developer Portal → Bot → Privileged Gateway Intents**, switch **all three** intents **off** (Server Members, Presence, Message Content). The new build works without them. Do this only after the deploy: the old build still requests the Server Members intent and cannot connect without it.
- [x] Do **not** request `GUILD_MEMBERS` or `MESSAGE_CONTENT` — not needed.

---

## 6. Keep it alive

- [x] ~~**Annual re-application** for privileged intents~~ — not needed: the bot requests no privileged intents. If a new feature ever needs one, Section 5 applies again.
- [ ] Keep the legal docs' "Last updated" date current whenever the data practices change.

---

## Appendix A — Ready-to-paste texts for verification

### A.1 `GUILD_MEMBERS` use-case justification

Removed: the bot no longer requests this intent (commit `c311cbe`).

### A.2 Data-handling description

> **Data stored:** Discord user IDs, user-supplied character names and avatar image URLs, server (guild) IDs, admin-configured role and channel IDs, and user-submitted feature content (calendar session dates, food/lunch entries, and NPC dialogue text).
> **Storage & location:** stored per-server in a database that is encrypted at rest (SQLCipher, AES-256), on infrastructure controlled solely by the bot operator, located in the EU, with access restricted to the operator. The encryption key is kept outside the database and the code repository.
> **Retention:** retained while the bot is present in the server; deleted when the bot is removed, or upon a verified erasure request, without undue delay and within 30 days.

> **⚠ Before pasting:** the retention line promises deletion when the bot is removed, which the code does not do yet (see Section 3). Build it or adjust this line together with the Privacy Policy. Backups (not set up yet) must also be rotated within 30 days for this promise to hold.
> **Sharing:** never sold or shared with third parties; processed only to provide the bot's features.
> **User rights:** users may request access to or deletion of their data via privacy@wettinck.be.
> **Policy:** full Privacy Policy published at https://sumarville.wettinck.be/privacy-policy.html.

---

## Appendix B — Publishing via GitHub Pages

The legal pages live in the **`docs/`** folder (`docs/privacy-policy.md`, `docs/terms-of-service.md`, `docs/index.md`, `docs/_config.yml`, `docs/CNAME`). Serving Pages from `/docs` publishes **only** these pages — never `src/` or `Data.json`. The site is hosted on the custom domain **`sumarville.wettinck.be`**.

**Setup (do these on github.com / your DNS)** — done; the site is live and the repo is renamed:

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

- This checklist covers roadmap **item 1** only. The `MESSAGE_CONTENT` risk was resolved by roadmap **item 2** (JDA 6 + slash commands); encryption at rest by **item 3** (SQLite + SQLCipher).
- Sources are listed at the bottom of [ANALYSE.md](../ANALYSE.md).
