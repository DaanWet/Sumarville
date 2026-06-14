# Ontwerp — JDA 6 + slash commands (roadmap-punt 2)

> Opgesteld 2026-06-15. Implementeert punt 2 van [ANALYSE.md](../../../ANALYSE.md):
> "Migreer naar JDA 6 + slash commands in één beweging."
> Bot: **Sumarville** (repo `DnD_Discord_Bot`).

## Doel & scope

Het JDA-oppervlak van de bot in één beweging migreren van **JDA `3.8.3_464`** (package
`net.dv8tion.jda.core`, prefix-commando's via Message Content) naar **JDA `6.4.2`**
(`net.dv8tion.jda.api`, **slash commands**). Dit lost twee dingen tegelijk op:

1. De API-breuk (`GuildMessageReceivedEvent`, `GuildController`, `Game` zijn verwijderd).
2. Het grootste compliance-risico: de `MESSAGE_CONTENT` privileged intent vervalt, want
   commando's lopen voortaan via Interactions.

### Buiten scope (bewust)

- **Data-laag** (`DataHandlers/*`, `Data.json`, `json-simple`) — blijft. Krijgt alleen
  een package-rename van imports. De echte DB (SQLite + SQLCipher) is **punt 3**.
- Off-by-one in `FoodHandler.checkFood`, en alle overige handler-laag-bugs → **punt 4**.
- Logging/observability (SLF4J/Logback/Sentry), CI, env-var voor Sentry-DSN → **punt 5**.
- Package-herstructurering, Player/DM-samenvoeging, `Character`-type i.p.v. `Map` → **punt 6**.

### Wel meegenomen (bugs in bestanden die we tóch herschrijven)

- Async rol-race in `Player`/`DM` (`createRole().queue(r -> this.role = r)` gevolgd door
  directe `this.role.getId()` → NPE + rol-spam). Wordt synchroon/correct afgehandeld.
- Help-renderer-crash bij commando's zonder alias (`sb.delete(len-2, …)`). Vervalt: de
  help wordt opnieuw opgebouwd uit de geregistreerde command-data.
- `PrivateMessageListener.split(regex, 1)`-bug. Vervalt: de DM-relay verdwijnt.
- **Scheduler-key-collisie** (`LunchMessager`/`SessionReminder` keyen op alléén de datum →
  twee guilds met een sessie op dezelfde dag overschrijven elkaars geplande taak).
  Besluit 2026-06-15: **meefixen** — key op `(guildId, datum)`.

## Beslissingen (vastgelegd 2026-06-15)

| Onderwerp | Keuze |
|---|---|
| Command-layout | **Subcommand-groepen** (`/session add`, `/food add`, …) |
| Dobbelstenen | **Allebei**: behoud `/d4`…`/d100` én voeg flexibele `/roll` toe |
| Java-target | **Java 21 LTS** |
| Testen | **Lichte unit-tests** op pure kernlogica; Discord-wiring handmatig |
| Scheduler-key-bug | Meteen meefixen (key op `(guildId, datum)`) |

## Architectuur

De prefix-machinerie verdwijnt volledig: `MessageListener`, `CommandListener`,
`PrivateMessageListener`, en `Command.isCommandFor` / aliassen.

Nieuwe command-laag:

- **`SlashCommand`** (interface) — elke command levert:
  - `getData()` → `SlashCommandData` (naam, beschrijving, subcommands, opties) voor registratie;
  - `execute(SlashCommandInteractionEvent event)` → de handler.
  - Optioneel: `onButton(...)` / `onModal(...)` voor commands met componenten.
- **Eén klasse per top-level groep** (`SessionCommands`, `FoodCommands`, `NpcCommands`,
  `CharacterCommands`, `DmCommands`, `ConfigCommands`, `DiceCommands`, `HelpCommand`),
  die intern op `event.getSubcommandName()` routeert naar kleine, testbare methoden.
- **`CommandRouter`** (`ListenerAdapter`) — vangt `onSlashCommandInteraction`,
  `onModalInteraction`, `onButtonInteraction` en delegeert op basis van de interactie-id.
- **`CommandRegistry`** — houdt de lijst van `SlashCommand`s; bouwt de registratie-payload.

Registratie:
- **Globaal** via `jda.updateCommands().addCommands(...)` → productie (propagatie tot ~1u).
- Als `TEST_GUILD_ID` (env-var) gezet is: registreer óók als **guild-commands** op die
  guild (verschijnen direct) voor snelle dev-iteratie.

Data-laag: `DataHandlers/*`, `Players/*` (voor zover ze `Guild` aanraken) en de schedulers
krijgen `net.dv8tion.jda.core.*` → `net.dv8tion.jda.api.*`. `Guild.getController()` →
methodes direct op `Guild` (`addRoleToMember`, `modifyNickname`, `createRole`).

## Command-mapping (oud → nieuw)

| Nieuw | Vervangt | Opties / gedrag |
|---|---|---|
| `/d4` … `/d100` | `Dice` (7×) | `amount:int` (max 50), `mode:adv\|dis` |
| `/roll` | (nieuw) | `sides:int` (verplicht), `amount:int`, `mode:adv\|dis` |
| `/session add` | `AddSession` | `date:` (`dd/MM/yyyy`); verleden-check; plant reminder |
| `/session remove` | `RemoveSession` | `date:` of `index:` of `all:bool` |
| `/session list` | `Calendar` | toont geplande sessies |
| `/food add` | `AddFood` | `name:` `emoji:` |
| `/food remove` | `RemoveFood` | `emoji:` of `index:` |
| `/food list` | `GetFood` (dode embed) | toont de lunch-lijst |
| `/food poll` | `GetFood` (huidig effect) | post de reactie-poll |
| `/npc add` | `AddMessage` **+ DM-relay** | `message:` `[npc:]` `[type:basic\|specific]` `[private:bool]` |
| `/npc remove` | `RemoveMessage` | `index:int` |
| `/npc list` | `ShowMessages` | toont basis-messages |
| `/character create` | `Character` (no-arg) | **modal**; `[as_npc:bool]` |
| `/character edit` | `Character edit` | `name:` `attribute:` `value:` |
| `/character remove` | `Character remove` | `name:` |
| `/character show` | `ShowPlayer` | `[target:]` (`all` default / `npc` / naam) |
| `/dm claim` | `DungeonMaster` (no-arg) | claimt DM-rol als er nog geen is |
| `/dm pass` | `DungeonMaster` (arg) | `user:@member` (alleen door huidige DM) |
| `/config role` | `SetConfig` (rollen) | `setting:(DM\|Player)` `value:@role` |
| `/config channel` | `SetConfig` (kanalen) | `setting:(DMChannel\|CalendarChannel\|FoodChannel\|MemeChannel)` `value:#channel` |
| `/help` | help/commands | bouwt overzicht uit geregistreerde command-data |

Top-level commands: `d4 d6 d8 d10 d12 d20 d100 roll session food npc character dm config help`
(15 < Discord-limiet 100).

Opruimingen die in deze mapping zitten:
- `/config` gebruikt Discords **getypte `ROLE`/`CHANNEL`-opties** → geen regex/ID/mention-parsing
  meer (`getMentionedRoles`/`getMentionedChannels` vervallen).
- `food` wordt netjes gesplitst in `list` (de embed die nu nooit verstuurd wordt) en `poll`
  (het reactie-bericht dat `food` nu feitelijk doet).

## Niet-triviale flows

### Character-creatie → Discord modal

`/character create [as_npc:bool]`:
1. Handler leest of de gebruiker al een character heeft (`CharacterHandler`, lokale read — snel
   genoeg om synchroon vóór de eerste interactie-respons te doen).
2. Bestaat er al één → **ephemeral** reply met knoppen **Overwrite** / **Cancel**.
   De Overwrite-knop opent alsnog de modal (een modal moet de eerste respons op een
   interactie zijn; vandaar de knop-tussenstap).
3. Geen bestaande → direct `event.replyModal(...)`.
4. Modal-velden: `name` (kort), `picture` (URL, optioneel). Bij submit (`onModalInteraction`):
   opslaan via `CharacterHandler.addCharacter`, en — tenzij `as_npc` — Player-rol toekennen
   (`guild.addRoleToMember`) en nickname zetten (`guild.modifyNickname`). Daarna bevestigings-embed.

Dit vervangt de hele conversationele `CharacterSheetBuilder.answer()`-state-machine en de
`builders`-lijst in `MessageListener`. (Race/klas/achtergrond stonden al uitgecommentarieerd;
de modal dekt de velden die werkelijk verzameld worden: `name`, `picture`.)

### NPC-DM-relay → ephemeral command

De oude flow: de DM stuurt het bericht privé via DM naar de bot (optioneel met server-id
ervoor), `PrivateMessageListener` checkt de DM-rol en slaat het op als "Specific" message.

Nieuw: `/npc add message:… [type:specific] [private:true]`, uitgevoerd **in de guild**, met een
**ephemeral** reply zodat spelers niets zien. Rol-check (alleen DM-rol mag `private`/specific
toevoegen) blijft, nu in-command. Server-id-targeting vervalt — de command draait al in de juiste
guild. `MESSAGE_CONTENT` in DM's is daarmee niet meer nodig.

## Build & intents (`pom.xml`)

- `net.dv8tion:JDA` `3.8.3_464` → **`6.4.2`**.
- `maven-compiler-plugin` source/target `9` → **`21`** (release 21).
- `maven-shade-plugin` `3.2.1` → **`3.5.x`** (Java 21-compatibel); main-class blijft `Main`.
- **`<repositories>` (jcenter) verwijderen** — alles via Maven Central (json-simple 1.1.1 staat daar).
- `json-simple` **blijft** (data-laag = punt 3).
- **JUnit 5 (Jupiter)** toevoegen (`test` scope) + `maven-surefire-plugin` indien nodig.

Intents (`Main`):

```java
JDABuilder.createDefault(System.getenv("BOT_TOKEN"), EnumSet.of(
        GatewayIntent.GUILD_MEMBERS   // PRIVILEGED — rollen, nicknames, member-lookup
    ))
    .setMemberCachePolicy(MemberCachePolicy.ALL)
    .setActivity(Activity.listening("/help"))
    .addEventListeners(new CommandRouter(registry))
    .build();
```

- **Geen `MESSAGE_CONTENT`** en **geen `GUILD_MESSAGES`** meer nodig (slash commands + het
  versturen van berichten/reacties vereisen ze niet). `GUILD_MEMBERS` blijft de enige privileged
  intent — exact wat in punt 1 voor de intent-review onderbouwd wordt.
- Token uit **`BOT_TOKEN`** env-var i.p.v. `args[0]`.

## Permissies

- `/config role` en `/config channel` → `DefaultMemberPermissions` = **Manage Server**.
- `/dm pass` → in-command check: alleen de huidige DM-rol-houder.
- `/npc add` met `private`/`specific` → in-command check: DM-rol.
- Overige commando's: zoals nu, voor iedereen.

## Testen (JUnit 5)

Pure logica uit de JDA-glue trekken en unit-testen:

- **DiceRoller** — enkele worp binnen bereik, advantage/disadvantage (max/min), multi-roll-som,
  grens van 50 worpen.
- **Datum-parsing/-validatie** — `dd/MM/yyyy` parse, verleden-check (`now - 12u`), ongeldige input.
- **Argument-/config-key-resolutie** — mapping van config-setting-namen, `isInteger`/`isLong`.

De Discord-wiring (modal, knoppen, command-registratie, ephemeral replies) wordt **handmatig**
geverifieerd tegen een privé test-server (`TEST_GUILD_ID`). JDA-mocking is buiten scope.

## Risico's & aandachtspunten

- **`.gitignore` bevat `*.xml`** — `pom.xml` is al getrackt dus edits blijven werken, maar nieuwe
  XML-resources zouden genegeerd worden (relevant voor punt 5/logback, niet nu).
- **Globale registratie propageert traag** (~1u) — daarom de `TEST_GUILD_ID`-route voor dev.
- **Modal als eerste respons** — kan niet ná een `deferReply`. De bestaande-character-check moet
  daarom snel/synchroon of via de knop-tussenstap (zoals hierboven).
- Eén **grote, atomaire migratie** ("in één beweging"): tussenstanden compileren mogelijk niet;
  plan in samenhangende stappen met een werkend eindpunt.
