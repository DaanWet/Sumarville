<div align="center">

# 🐉 Sumarville

**Your D&D table's companion on Discord.**
Roll dice, keep character sheets, plan sessions and let your NPCs remind the party that adventure awaits.

[![Invite Sumarville](https://img.shields.io/badge/Invite-Sumarville-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/oauth2/authorize?client_id=577940186755891211&permissions=403000384&integration_type=0&scope=bot+applications.commands)
[![Support server](https://img.shields.io/badge/Support-Server-57F287?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/4Atn8t2)
[![Privacy](https://img.shields.io/badge/Privacy-Policy-grey?style=for-the-badge)](https://sumarville.wettinck.be/privacy-policy.html)

</div>

---

## ✨ Why Sumarville?

- 🎲 **Every die you need.** `/d20`, `/d6`, `/d100` and more, with advantage, disadvantage and up to 50 rolls at once.
- 📜 **Character sheets.** Each player makes a character with a name and portrait. Sumarville sets their server nickname and gives them the Player role.
- 📅 **Session calendar.** Add the next session date and Sumarville posts a reminder in your calendar channel the evening before.
- 🗣️ **NPCs that talk.** Write lines for your NPCs. One of them delivers the session reminder, with the NPC's portrait.
- 🍕 **Lunch poll.** Keep a list of food with an emoji each, and Sumarville posts a poll on session day so everyone can vote.
- 👑 **DM handover.** Claim the Dungeon Master role or pass it to someone else with one command.
- 🔒 **Privacy first.** Only slash commands, no privileged intents, no message reading. Your server's data stays with your server.

---

## 🚀 Get started in 4 steps

1. **[Invite Sumarville](https://discord.com/oauth2/authorize?client_id=577940186755891211&permissions=403000384&integration_type=0&scope=bot+applications.commands)** to your server.
2. **Pick the roles** (needs *Manage Server*):
   ```
   /config role setting:DM value:@Dungeon Master
   /config role setting:Player value:@Adventurer
   ```
3. **Pick the channels** for reminders and the lunch poll:
   ```
   /config channel setting:Calendar channel value:#sessions
   /config channel setting:Food channel value:#lunch
   ```
4. **Let your players join:** everyone runs `/character create`. Done! ⚔️

> 💡 Drag the **Sumarville** role above your Player and DM roles in *Server Settings → Roles*. Discord only lets a bot hand out roles and nicknames below its own role.

---

## 📖 Commands

Type `/help` in Discord for the full list.

### 🎲 Dice

| Command | What it does |
|---|---|
| `/d4` `/d6` `/d8` `/d10` `/d12` `/d20` `/d100` | Roll that die |
| `/roll sides:<2–1000>` | Roll a die with any number of sides |

Every dice command takes two optional options:
- `amount`: roll 1 to 50 times
- `mode`: **Advantage** or **Disadvantage**

### 📜 Characters

| Command | What it does |
|---|---|
| `/character create` | Make your character (name + picture URL). Sets your nickname and Player role |
| `/character create as_npc:True` | Make an NPC instead of your own character |
| `/character edit name:<name> attribute:<name\|picture> value:<new>` | Change a name or picture |
| `/character show [target]` | Show one character, `all` characters or every `npc` |
| `/character remove name:<name>` | Delete a character |

### 📅 Sessions

| Command | What it does |
|---|---|
| `/session add date:<dd/MM/yyyy>` | Plan a session. A reminder goes out the evening before |
| `/session list` | Show the calendar |
| `/session remove [date] [index] [all]` | Cancel one session or all of them |

### 🗣️ NPC messages

| Command | What it does |
|---|---|
| `/npc add message:<text> [npc] [type] [private]` | Add a line an NPC may say in the next reminder |
| `/npc list` | Show the saved basic lines |
| `/npc remove index:<n>` | Remove a basic line (`0` is the first line in `/npc list`) |

- **Basic** lines go into the pool and come back every session.
- **Specific** lines (DM only) are for the next session only. They win over basic lines and disappear after the reminder.

### 🍕 Lunch

| Command | What it does |
|---|---|
| `/food add name:<food> emoji:<emoji>` | Add a dish to the menu |
| `/food list` | Show the menu |
| `/food remove [emoji] [index]` | Remove a dish |
| `/food poll` | Post the lunch poll right now |

### 👑 Dungeon Master & setup

| Command | What it does |
|---|---|
| `/dm claim` | Take the DM role (needs *Manage Roles*) |
| `/dm pass user:<@member>` | Hand the DM role to another player |
| `/config role` / `/config channel` | Link your server's roles and channels (needs *Manage Server*) |
| `/help` | Show all commands |

---

## 🔐 Permissions

Sumarville asks for only what it needs:

| Permission | Why |
|---|---|
| Manage Roles | Give the Player role and pass the DM role |
| Manage Nicknames | Set a player's nickname to their character's name |
| View Channels, Send Messages, Embed Links | Reply and post reminders |
| Add Reactions, Read Message History, Use External Emojis | Add the food emojis (also custom ones from other servers) to the lunch poll |

It uses **no privileged gateway intents**: it does not read your messages, member list or presence.

---

## 🛡️ Privacy & legal

Sumarville stores only what its features need (character names and pictures, session dates, food and NPC lines, and the roles and channels you configure), separately for each server. The database is encrypted and nothing is sold or shared.

- 📄 [Privacy Policy](https://sumarville.wettinck.be/privacy-policy.html)
- 📄 [Terms of Service](https://sumarville.wettinck.be/terms-of-service.html)
- ✉️ Data requests: **privacy@wettinck.be**

---

## 💬 Support

Found a bug or have an idea? Join the **[support server](https://discord.gg/4Atn8t2)**.

<div align="center">

**May your rolls be high and your snacks plentiful.** 🎲🍕

</div>
