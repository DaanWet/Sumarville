# JDA 6 + Slash Commands Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Sumarville Discord bot from JDA `3.8.3_464` (prefix commands via Message Content) to JDA `6.4.2` with slash commands, removing the `MESSAGE_CONTENT` privileged intent.

**Architecture:** A small command framework (`SlashCommand` interface + `CommandRegistry` + `CommandRouter` `ListenerAdapter`) replaces the old `MessageListener`/`CommandListener`/`PrivateMessageListener` prefix-parsing layer. Each top-level command (or subcommand group) is one class. The data layer (`DataHandlers/*`, `Data.json`) is untouched except for JDA package-rename imports. Character creation becomes a Discord modal; the NPC DM-relay becomes an ephemeral slash command.

**Tech Stack:** Java 21, JDA 6.4.2 (Maven Central), JUnit 5 (Jupiter), maven-shade-plugin 3.5.x, json-simple 1.1.1 (kept until roadmap item 3).

**Spec:** [docs/superpowers/specs/2026-06-15-jda6-slash-commands-design.md](../specs/2026-06-15-jda6-slash-commands-design.md)

---

## Conventions used by every task

**Git (project rule — trunk-based):** commit directly on `master`. **Never** create a feature branch, **never** add a `Co-Authored-By` trailer, **never** `git push` (the user pushes). Commit messages use Conventional Commits.

**Build/test commands** (run from repo root):
- Compile: `mvn -q clean compile`
- Test: `mvn -q test`
- Package fat jar: `mvn -q clean package`
- Run a single test class: `mvn -q test -Dtest=DiceRollerTest`

**Package layout** (reuses existing capitalized `Commands` package to avoid a Windows case-collision with a new lowercase `commands` dir):
- `Commands.Framework` → `SlashCommand`, `CommandRegistry`, `CommandRouter`
- `Commands.Dice` → `DiceRoller`, `DiceCommands`
- `Commands.Calendar` → `SessionDateParser`, `SessionCommands`, `SessionReminder` (migrated, kept)
- `Commands.Food` → `FoodCommands`, `LunchMessager` (migrated, kept)
- `Commands.NPCMessages` → `NpcCommands`
- `Commands.Users` → `CharacterCommands`, `DmCommands`
- `Commands` (root) → `ConfigCommands`, `ConfigKeys`, `HelpCommand`
- `Players` → `Person`, `Player`, `DM` (migrated)
- `DataHandlers` → all (import rename only)

**JDA 6 component import paths** (Components V2 — confirm once against the 6.4.2 jar when first used; the rest of the API was verified against the 6.4.2 javadoc):
```java
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;          // V2 location
import net.dv8tion.jda.api.components.actionrow.ActionRow;      // V2 location
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.emoji.Emoji;
```

**Critical sequencing note:** Bumping the JDA version (Task 1.1) immediately breaks compilation of all old `net.dv8tion.jda.core.*` code. There is no compiling intermediate state until the entire old JDA-facing layer is migrated or deleted. **Phase 1 (Tasks 1.1–1.11) is therefore one atomic cut: do NOT commit until Task 1.11, where the build is green again.** From Phase 2 onward, each command group is independent and every task ends green and committed.

---

# Phase 1 — The cut: JDA 6 foundation + data/players/schedulers + dice/help vertical slice

> One commit at the end (Task 1.11). The result compiles, the bot starts, and `/d4`…`/d100`, `/roll`, and `/help` work. All other commands are temporarily gone (re-added in Phases 2–7).

## Task 1.1: Rewrite `pom.xml`

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Replace the whole file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>me.Damascus2000</groupId>
    <artifactId>DnD_Discord_Bot</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.3</version>
                <configuration>
                    <shadedArtifactAttached>true</shadedArtifactAttached>
                    <transformers>
                        <transformer implementation=
                            "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                            <mainClass>Main</mainClass>
                        </transformer>
                    </transformers>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>net.dv8tion</groupId>
            <artifactId>JDA</artifactId>
            <version>6.4.2</version>
        </dependency>
        <dependency>
            <groupId>com.googlecode.json-simple</groupId>
            <artifactId>json-simple</artifactId>
            <version>1.1.1</version>
            <exclusions>
                <exclusion>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Notes: the `<repositories>` jcenter block is gone (Maven Central is default). The json-simple→junit transitive exclusion avoids an old JUnit 4 on the runtime classpath. No commit yet (build is now red).

## Task 1.2: Rename JDA imports in the data layer

**Files:**
- Modify: `src/main/java/DataHandlers/DataHandler.java`
- Modify: `src/main/java/DataHandlers/CharacterHandler.java`
- Modify: `src/main/java/DataHandlers/CalendarHandler.java`
- Modify: `src/main/java/DataHandlers/ConfigHandler.java`
- Modify: `src/main/java/DataHandlers/FoodHandler.java`
- Modify: `src/main/java/DataHandlers/NPCMessageHandler.java`
- Modify: `src/main/java/Players/Person.java`

- [ ] **Step 1: In each file above, change the JDA import**

Replace every `import net.dv8tion.jda.core.` with `import net.dv8tion.jda.api.` (these files only import `...entities.Guild` and `...entities.Role`). No other change in these seven files.

`ConfigHandler.getChannel(...)` calls `g.getDefaultChannel().getIdLong()`. In JDA 6 `getDefaultChannel()` returns a `@Nullable DefaultGuildChannelUnion` which still has `getIdLong()`, so it compiles unchanged. (Its latent NPE/`ClassCastException` is a data-layer bug → roadmap item 4; leave it.)

## Task 1.3: Migrate `Players/Player.java` and `Players/DM.java` (fix async role race)

**Files:**
- Modify: `src/main/java/Players/Player.java`
- Modify: `src/main/java/Players/DM.java`

- [ ] **Step 1: Replace `Player.java`**

```java
package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.Color;

public class Player extends Person {

    public Player(Guild g) {
        String playerId = new ConfigHandler(g).getPlayerRoleID();
        if (playerId.equals("0")) {
            // .complete() blocks until the role exists, fixing the old async NPE/role-spam race.
            this.role = g.createRole().setColor(Color.orange).setName("Dungeon Delvers").complete();
            new ConfigHandler(g).setConfig(this.role.getId(), "Player");
        } else {
            this.role = g.getRoleById(playerId);
        }
    }
}
```

- [ ] **Step 2: Replace `DM.java`**

```java
package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.Color;

public class DM extends Person {

    public DM(Guild g) {
        String dmId = new ConfigHandler(g).getDMRoleID();
        if (dmId.equals("0")) {
            this.role = g.createRole().setColor(Color.MAGENTA).setName("DM").complete();
            new ConfigHandler(g).setConfig(this.role.getId(), "DM");
        } else {
            this.role = g.getRoleById(dmId);
        }
    }
}
```

Caveat: `.complete()` must not run on the JDA websocket thread. All call sites in this plan are slash/modal/button handlers (worker threads) and `SessionReminder.onRestart` (main thread), which are safe.

## Task 1.4: Migrate `Commands/Food/LunchMessager.java` (JDA 6 API + per-guild key)

**Files:**
- Modify: `src/main/java/Commands/Food/LunchMessager.java`

- [ ] **Step 1: Replace the file**

```java
package Commands.Food;

import DataHandlers.ConfigHandler;
import DataHandlers.FoodHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LunchMessager {

    private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Keyed per (guild, date) so two guilds with a session on the same day no longer collide.
    private static final Map<String, ScheduledFuture<?>> map = new HashMap<>();

    private static String key(String guildId, LocalDateTime date) {
        return guildId + "|" + (date == null ? "now" : date.format(sdf));
    }

    public static void makeMessage(LocalDateTime date, Guild g) {
        LocalDateTime messagedate = (date == null) ? LocalDateTime.now() : date;
        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messagedate);
        TextChannel ch = g.getTextChannelById(new ConfigHandler(g).getChannel("FoodChannel"));
        if (ch == null) {
            return;
        }
        ScheduledFuture<?> task = ch.sendMessage(getFood(date, g)).queueAfter(
                diff > 0 ? diff : 0, TimeUnit.MILLISECONDS,
                message -> getEmojis(g).forEach(s -> message.addReaction(Emoji.fromFormatted(s)).queue()));
        map.put(key(g.getId(), date), task);
    }

    public static void cancelMessage(String guildId, LocalDateTime date) {
        String k = key(guildId, date);
        ScheduledFuture<?> task = map.remove(k);
        if (task != null) {
            task.cancel(false);
        }
    }

    private static String getFood(LocalDateTime date, Guild g) {
        FoodHandler f = new FoodHandler(g);
        StringBuilder sb = new StringBuilder();
        sb.append("Wat eten we ?").append(date != null ? " Session: " + sdf.format(date) : "");
        f.getFood().forEach(food -> sb.append("\n").append(food.get("Name")).append(": ").append(food.get("Emoji")));
        return sb.toString();
    }

    private static ArrayList<String> getEmojis(Guild g) {
        FoodHandler f = new FoodHandler(g);
        ArrayList<String> emoji = new ArrayList<>();
        f.getFood().forEach(s -> emoji.add(s.get("Emoji")));
        return emoji;
    }
}
```

Note the `TextChannel` import path moved in JDA (`...entities.channel.concrete.TextChannel`); confirm against the jar.

## Task 1.5: Migrate `Commands/Calendar/SessionReminder.java` (JDA 6 API + per-guild key)

**Files:**
- Modify: `src/main/java/Commands/Calendar/SessionReminder.java`

- [ ] **Step 1: Replace the file**

```java
package Commands.Calendar;

import Commands.Food.LunchMessager;
import DataHandlers.CalendarHandler;
import DataHandlers.CharacterHandler;
import DataHandlers.ConfigHandler;
import DataHandlers.NPCMessageHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SessionReminder {

    private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, ScheduledFuture<?>> map = new HashMap<>();
    private static final Map<String, ScheduledFuture<?>> mememap = new HashMap<>();
    private static final Random random = new Random();

    private static String key(String guildId, LocalDateTime date) {
        return guildId + "|" + date.format(sdf);
    }

    public static void makeMemeMessage(LocalDateTime date, Guild g) {
        LocalDateTime mDate = date.minusHours(24);
        long mdiff = ChronoUnit.MILLIS.between(LocalDateTime.now(), mDate);
        TextChannel mch = g.getTextChannelById(new ConfigHandler(g).getChannel("MemeChannel"));
        if (mch == null) {
            return;
        }
        ScheduledFuture<?> mtask = mch.sendMessage(
                "`24h Left to post your meme to make a chance to get a special reward!`")
                .queueAfter(mdiff > 0 ? mdiff : 0, TimeUnit.MILLISECONDS);
        mememap.put(key(g.getId(), date), mtask);
    }

    public static void makeMessage(LocalDateTime date, Guild g) {
        LocalDateTime messageDate = date.minusHours(6);
        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messageDate);

        NPCMessageHandler npch = new NPCMessageHandler(g);
        ArrayList<Map<String, String>> messages = npch.getSpecificMessages();
        if (messages.size() == 0) {
            messages = npch.getBasicMessages();
        }
        int i = messages.size() > 0 ? random.nextInt(messages.size()) : -1;
        EmbedBuilder eb = new EmbedBuilder();
        String name = i != -1 ? messages.get(i).get("npc") : "";
        String custommessage = i != -1 ? messages.get(i).get("message") : "It's time to go on an adventure!";
        eb.addField(custommessage, "Don't forget our session tomorrow!", false);

        CharacterHandler chh = new CharacterHandler(g);
        if (!name.equals("") && !chh.getPicture(name, "name").equals("")) {
            eb.setAuthor(name, chh.getPicture(name, "name"), chh.getPicture(name, "name"));
        } else {
            eb.setAuthor(!name.equals("") ? name : g.getSelfMember().getEffectiveName());
        }

        TextChannel ch = g.getTextChannelById(new ConfigHandler(g).getChannel("CalendarChannel"));
        if (ch == null) {
            return;
        }
        ScheduledFuture<?> task = ch.sendMessageEmbeds(eb.build()).queueAfter(
                diff > 0 ? diff : 0, TimeUnit.MILLISECONDS,
                message -> {
                    LunchMessager.makeMessage(date, g);
                    npch.clearSpecific();
                });
        map.put(key(g.getId(), date), task);
    }

    public static void cancelSession(String guildId, LocalDateTime date) {
        String k = key(guildId, date);
        ScheduledFuture<?> task = map.remove(k);
        if (task != null) {
            task.cancel(false);
        }
        ScheduledFuture<?> meme = mememap.remove(k);
        if (meme != null) {
            meme.cancel(false);
        }
    }

    public static void onRestart(Guild g) {
        CalendarHandler calendarHandler = new CalendarHandler(g);
        ArrayList<LocalDateTime> sessions = calendarHandler.getSessions(false);
        sessions.forEach(session -> {
            LocalDateTime messageDate = session.minusHours(4);
            long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messageDate);
            long difffood = ChronoUnit.MILLIS.between(LocalDateTime.now(), session);
            if (diff > 0) {
                makeMessage(session, g);
            } else if (difffood > 0) {
                LunchMessager.makeMessage(session, g);
            }
        });
    }
}
```

This also fixes the old `cancelSession` NPE (it called `mememap.get(date).cancel` unguarded) by null-checking.

## Task 1.6: Delete the old prefix-command layer

**Files:**
- Delete: `src/main/java/Listeners/MessageListener.java`
- Delete: `src/main/java/Listeners/CommandListener.java`
- Delete: `src/main/java/Listeners/PrivateMessageListener.java`
- Delete: `src/main/java/Players/CharacterSheet/CharacterSheetBuilder.java`
- Delete: `src/main/java/Commands/Command.java`
- Delete: `src/main/java/Commands/Dice.java`
- Delete: `src/main/java/Commands/SetConfig.java`
- Delete: `src/main/java/Commands/Calendar/AddSession.java`
- Delete: `src/main/java/Commands/Calendar/RemoveSession.java`
- Delete: `src/main/java/Commands/Calendar/Calendar.java`
- Delete: `src/main/java/Commands/Food/AddFood.java`
- Delete: `src/main/java/Commands/Food/GetFood.java`
- Delete: `src/main/java/Commands/Food/RemoveFood.java`
- Delete: `src/main/java/Commands/NPCMessages/AddMessage.java`
- Delete: `src/main/java/Commands/NPCMessages/RemoveMessage.java`
- Delete: `src/main/java/Commands/NPCMessages/ShowMessages.java`
- Delete: `src/main/java/Commands/Users/Character.java`
- Delete: `src/main/java/Commands/Users/DungeonMaster.java`
- Delete: `src/main/java/Commands/Users/ShowPlayer.java`

- [ ] **Step 1: Delete all files listed above**

```bash
git rm src/main/java/Listeners/MessageListener.java src/main/java/Listeners/CommandListener.java src/main/java/Listeners/PrivateMessageListener.java src/main/java/Players/CharacterSheet/CharacterSheetBuilder.java src/main/java/Commands/Command.java src/main/java/Commands/Dice.java src/main/java/Commands/SetConfig.java src/main/java/Commands/Calendar/AddSession.java src/main/java/Commands/Calendar/RemoveSession.java src/main/java/Commands/Calendar/Calendar.java src/main/java/Commands/Food/AddFood.java src/main/java/Commands/Food/GetFood.java src/main/java/Commands/Food/RemoveFood.java src/main/java/Commands/NPCMessages/AddMessage.java src/main/java/Commands/NPCMessages/RemoveMessage.java src/main/java/Commands/NPCMessages/ShowMessages.java src/main/java/Commands/Users/Character.java src/main/java/Commands/Users/DungeonMaster.java src/main/java/Commands/Users/ShowPlayer.java
```

(`Commands/Dice.java` the class is deleted so the new `Commands/Dice/` package directory can exist.)

## Task 1.7: Create the command framework

**Files:**
- Create: `src/main/java/Commands/Framework/SlashCommand.java`
- Create: `src/main/java/Commands/Framework/CommandRegistry.java`
- Create: `src/main/java/Commands/Framework/CommandRouter.java`

- [ ] **Step 1: `SlashCommand.java`**

```java
package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

/** A unit of slash-command behaviour. Components (buttons/modals) it owns must use ids
 *  prefixed with {@code getId() + ":"} so the router can dispatch them back here. */
public interface SlashCommand {

    /** Stable namespace for this command's component ids (e.g. "character"). */
    String getId();

    /** Top-level command names this handler responds to (e.g. ["d4", ..., "roll"]). */
    List<String> getCommandNames();

    /** Registration payload (one entry per top-level command). */
    List<SlashCommandData> getCommandData();

    void execute(SlashCommandInteractionEvent event);

    default void onButton(ButtonInteractionEvent event) {}

    default void onModal(ModalInteractionEvent event) {}
}
```

- [ ] **Step 2: `CommandRegistry.java`**

```java
package Commands.Framework;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {

    private final List<SlashCommand> commands = new ArrayList<>();
    private final Map<String, SlashCommand> byCommandName = new HashMap<>();
    private final Map<String, SlashCommand> byId = new HashMap<>();

    public void register(SlashCommand cmd) {
        commands.add(cmd);
        byId.put(cmd.getId(), cmd);
        for (String name : cmd.getCommandNames()) {
            byCommandName.put(name, cmd);
        }
    }

    public List<SlashCommand> all() {
        return commands;
    }

    public SlashCommand byCommandName(String name) {
        return byCommandName.get(name);
    }

    public SlashCommand byComponentId(String componentId) {
        int sep = componentId.indexOf(':');
        String ns = sep == -1 ? componentId : componentId.substring(0, sep);
        return byId.get(ns);
    }

    public List<SlashCommandData> allCommandData() {
        List<SlashCommandData> data = new ArrayList<>();
        for (SlashCommand c : commands) {
            data.addAll(c.getCommandData());
        }
        return data;
    }
}
```

- [ ] **Step 3: `CommandRouter.java`**

```java
package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandRouter extends ListenerAdapter {

    private final CommandRegistry registry;

    public CommandRouter(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand cmd = registry.byCommandName(event.getName());
        if (cmd != null) {
            cmd.execute(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getComponentId());
        if (cmd != null) {
            cmd.onButton(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getModalId());
        if (cmd != null) {
            cmd.onModal(event);
        }
    }
}
```

## Task 1.8: Create `DiceRoller` (pure logic) — TDD

**Files:**
- Create: `src/main/java/Commands/Dice/DiceRoller.java`
- Test: `src/test/java/Commands/Dice/DiceRollerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package Commands.Dice;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DiceRollerTest {

    @Test
    void rollIsWithinBounds() {
        DiceRoller r = new DiceRoller(new Random(42));
        for (int i = 0; i < 1000; i++) {
            int v = r.roll(20);
            assertTrue(v >= 1 && v <= 20, "out of bounds: " + v);
        }
    }

    @Test
    void rollManyReturnsRequestedCountWithinBounds() {
        DiceRoller r = new DiceRoller(new Random(1));
        int[] rolls = r.rollMany(6, 5);
        assertEquals(5, rolls.length);
        for (int v : rolls) {
            assertTrue(v >= 1 && v <= 6);
        }
    }

    @Test
    void sumAddsAllRolls() {
        DiceRoller r = new DiceRoller(new Random(1));
        assertEquals(15, r.sum(new int[]{1, 2, 3, 4, 5}));
    }
}
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `mvn -q test -Dtest=DiceRollerTest`
Expected: compile failure / FAIL — `DiceRoller` does not exist.

- [ ] **Step 3: Implement `DiceRoller`**

```java
package Commands.Dice;

import java.util.Random;

public class DiceRoller {

    private final Random random;

    public DiceRoller() {
        this(new Random());
    }

    public DiceRoller(Random random) {
        this.random = random;
    }

    public int roll(int sides) {
        return random.nextInt(sides) + 1;
    }

    public int[] rollMany(int sides, int amount) {
        int[] rolls = new int[amount];
        for (int i = 0; i < amount; i++) {
            rolls[i] = roll(sides);
        }
        return rolls;
    }

    public int sum(int[] rolls) {
        int total = 0;
        for (int r : rolls) {
            total += r;
        }
        return total;
    }
}
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn -q test -Dtest=DiceRollerTest`
Expected: PASS (note: the whole module must compile for surefire to run; if other Phase 1 files are incomplete this will fail to compile — that is expected until Task 1.11).

## Task 1.9: Create `DiceCommands`

**Files:**
- Create: `src/main/java/Commands/Dice/DiceCommands.java`

- [ ] **Step 1: Write the file**

```java
package Commands.Dice;

import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DiceCommands implements SlashCommand {

    private static final int[] DICE = {4, 6, 8, 10, 12, 20, 100};
    private static final int MAX_ROLLS = 50;

    private final DiceRoller roller = new DiceRoller();

    @Override
    public String getId() {
        return "dice";
    }

    @Override
    public List<String> getCommandNames() {
        List<String> names = new ArrayList<>();
        for (int s : DICE) {
            names.add("d" + s);
        }
        names.add("roll");
        return names;
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        List<SlashCommandData> data = new ArrayList<>();
        for (int s : DICE) {
            data.add(Commands.slash("d" + s, "Rolls a d" + s)
                    .addOptions(amountOption(), modeOption()));
        }
        data.add(Commands.slash("roll", "Rolls a die with a custom number of sides")
                .addOptions(
                        new OptionData(OptionType.INTEGER, "sides", "Number of sides", true)
                                .setRequiredRange(2, 1000),
                        amountOption(), modeOption()));
        return data;
    }

    private OptionData amountOption() {
        return new OptionData(OptionType.INTEGER, "amount", "How many times to roll (max 50)")
                .setRequiredRange(1, MAX_ROLLS);
    }

    private OptionData modeOption() {
        return new OptionData(OptionType.STRING, "mode", "Advantage or disadvantage")
                .addChoice("Advantage", "adv")
                .addChoice("Disadvantage", "dis");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int sides = event.getName().equals("roll")
                ? event.getOption("sides").getAsInt()
                : Integer.parseInt(event.getName().substring(1));

        String mode = event.getOption("mode", null, OptionMapping::getAsString);
        int amount = event.getOption("amount", 1, OptionMapping::getAsInt);

        String name = event.getMember() != null ? event.getMember().getEffectiveName()
                : event.getUser().getName();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setThumbnail(imageFor(sides));

        if (mode != null) {
            int first = roller.roll(sides);
            int second = roller.roll(sides);
            boolean adv = mode.equals("adv");
            int chosen = adv ? Math.max(first, second) : Math.min(first, second);
            eb.addField(
                    String.format("%s rolled a d%d with %svantage and got:", name, sides, adv ? "ad" : "disad"),
                    String.format("%d & %d => %d", first, second, chosen),
                    true);
        } else if (amount > 1) {
            int[] rolls = roller.rollMany(sides, amount);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rolls.length; i++) {
                sb.append(i == 0 ? "" : " + ").append(rolls[i]);
            }
            sb.append(" = ").append(roller.sum(rolls));
            eb.addField(String.format("%s rolled %d times a d%d and got:", name, amount, sides), sb.toString(), true);
        } else {
            eb.addField(String.format("%s rolled a d%d and got:", name, sides),
                    Integer.toString(roller.roll(sides)), true);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private String imageFor(int sides) {
        if (sides == 100) {
            return "https://www.dnddice.com/media/wysiwyg/d10_.jpg";
        }
        if (sides == 4 || sides == 6 || sides == 8 || sides == 10 || sides == 12 || sides == 20) {
            return "https://www.dnddice.com/media/wysiwyg/d" + sides + ".jpg";
        }
        return null;
    }
}
```

## Task 1.10: Create `HelpCommand`

**Files:**
- Create: `src/main/java/Commands/HelpCommand.java`

- [ ] **Step 1: Write the file**

```java
package Commands;

import Commands.Framework.CommandRegistry;
import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.List;

public class HelpCommand implements SlashCommand {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getId() {
        return "help";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("help");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("help", "Shows all Sumarville commands"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Sumarville Commands");
        eb.setColor(Color.ORANGE);
        for (SlashCommand cmd : registry.all()) {
            for (SlashCommandData data : cmd.getCommandData()) {
                StringBuilder value = new StringBuilder(data.getDescription());
                List<SubcommandData> subs = data.getSubcommands();
                if (!subs.isEmpty()) {
                    for (SubcommandData sub : subs) {
                        value.append("\n• `").append(sub.getName()).append("` — ").append(sub.getDescription());
                    }
                }
                eb.addField("/" + data.getName(), value.toString(), false);
            }
        }
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
```

Note: `SlashCommandData.getSubcommands()` returns the subcommands added so far; confirm the accessor name against the jar (alternatively store descriptions in each `SlashCommand`).

## Task 1.11: Rewrite `Main.java`, build green, commit the cut

**Files:**
- Modify: `src/main/java/Main.java`

- [ ] **Step 1: Replace `Main.java`**

```java
import Commands.ConfigCommands;
import Commands.HelpCommand;
import Commands.Calendar.SessionCommands;
import Commands.Calendar.SessionReminder;
import Commands.Dice.DiceCommands;
import Commands.Food.FoodCommands;
import Commands.Framework.CommandRegistry;
import Commands.Framework.CommandRouter;
import Commands.NPCMessages.NpcCommands;
import Commands.Users.CharacterCommands;
import Commands.Users.DmCommands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.EnumSet;

public class Main {

    public static void main(String[] args) throws Exception {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new DiceCommands());
        registry.register(new SessionCommands());
        registry.register(new FoodCommands());
        registry.register(new NpcCommands());
        registry.register(new CharacterCommands());
        registry.register(new DmCommands());
        registry.register(new ConfigCommands());
        registry.register(new HelpCommand(registry));

        String token = System.getenv("BOT_TOKEN");
        JDA jda = JDABuilder.createDefault(token, EnumSet.of(GatewayIntent.GUILD_MEMBERS))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new CommandRouter(registry))
                .build();
        jda.awaitReady();

        String testGuildId = System.getenv("TEST_GUILD_ID");
        if (testGuildId != null && !testGuildId.isBlank()) {
            Guild g = jda.getGuildById(testGuildId);
            if (g != null) {
                g.updateCommands().addCommands(registry.allCommandData()).queue();
            }
        } else {
            jda.updateCommands().addCommands(registry.allCommandData()).queue();
        }

        jda.getGuilds().forEach(SessionReminder::onRestart);
    }
}
```

This references `SessionCommands`, `FoodCommands`, `NpcCommands`, `CharacterCommands`, `DmCommands`, `ConfigCommands` — which do not exist yet. To make Phase 1 compile, create **temporary minimal stubs** for those five-plus classes now and replace them in Phases 2–7.

- [ ] **Step 2: Create temporary stubs (deleted/replaced in later phases)**

For each of `Commands/Calendar/SessionCommands.java`, `Commands/Food/FoodCommands.java`, `Commands/NPCMessages/NpcCommands.java`, `Commands/Users/CharacterCommands.java`, `Commands/Users/DmCommands.java`, `Commands/ConfigCommands.java`, write a stub implementing `SlashCommand` that registers nothing. Example for `SessionCommands` (mirror the pattern for the others, changing package/class/`getId`):

```java
package Commands.Calendar;

import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class SessionCommands implements SlashCommand {
    @Override public String getId() { return "session"; }
    @Override public List<String> getCommandNames() { return List.of(); }
    @Override public List<SlashCommandData> getCommandData() { return List.of(); }
    @Override public void execute(SlashCommandInteractionEvent event) {}
}
```

(Stub `getId` values: `session`, `food`, `npc`, `character`, `dm`, `config`. `ConfigCommands` lives in package `Commands`.)

- [ ] **Step 3: Compile and test the whole module**

Run: `mvn -q clean package`
Expected: BUILD SUCCESS; `DiceRollerTest` passes; a shaded jar is produced.

- [ ] **Step 4: Manual smoke test against a test server**

Set `BOT_TOKEN` and `TEST_GUILD_ID` env vars; run the jar; in the test guild confirm `/d20`, `/d20 mode:Advantage`, `/roll sides:20 amount:3`, and `/help` work.

- [ ] **Step 5: Commit the cut**

```bash
git add -A
git commit -m "feat(jda6): migrate to JDA 6 + slash command framework with dice/help

Replaces the prefix-command listener layer with a SlashCommand framework
(registry + router), bumps JDA 3->6 and Java 9->21, migrates the data layer,
players and schedulers, and ships /d4../d100, /roll and /help. Other command
groups are temporary stubs, re-added in following commits. (roadmap item 2)"
```

---

# Phase 2 — Session commands

## Task 2.1: Create `SessionDateParser` (pure logic) — TDD

**Files:**
- Create: `src/main/java/Commands/Calendar/SessionDateParser.java` (replaces nothing new)
- Test: `src/test/java/Commands/Calendar/SessionDateParserTest.java`

- [ ] **Step 1: Write the failing test**

```java
package Commands.Calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class SessionDateParserTest {

    private final SessionDateParser parser = new SessionDateParser();

    @Test
    void parsesDayMonthYearToStartOfDay() {
        LocalDateTime date = parser.parse("25/12/2026");
        assertEquals(LocalDateTime.of(2026, 12, 25, 0, 0), date);
    }

    @Test
    void formatRoundTrips() {
        assertEquals("25/12/2026", parser.format(parser.parse("25/12/2026")));
    }

    @Test
    void invalidInputThrows() {
        assertThrows(DateTimeParseException.class, () -> parser.parse("2026-12-25"));
    }

    @Test
    void isInFutureUsesTwelveHourGrace() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 18, 0);
        // start of today is 18h before now -> still "future" within the 12h grace? 18h > 12h -> false
        assertFalse(parser.isInFuture(LocalDateTime.of(2026, 6, 15, 0, 0), now.plusHours(13)));
        // tomorrow is clearly in the future
        assertTrue(parser.isInFuture(LocalDateTime.of(2026, 6, 16, 0, 0), now));
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `mvn -q test -Dtest=SessionDateParserTest`
Expected: FAIL — `SessionDateParser` does not exist.

- [ ] **Step 3: Implement**

```java
package Commands.Calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SessionDateParser {

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** @throws DateTimeParseException if the input is not dd/MM/yyyy */
    public LocalDateTime parse(String input) {
        return LocalDate.from(fmt.parse(input)).atStartOfDay();
    }

    public String format(LocalDateTime date) {
        return fmt.format(date);
    }

    public boolean isInFuture(LocalDateTime date, LocalDateTime now) {
        return date.isAfter(now.minusHours(12));
    }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `mvn -q test -Dtest=SessionDateParserTest`
Expected: PASS.

## Task 2.2: Implement `SessionCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/Calendar/SessionCommands.java`

- [ ] **Step 1: Replace the stub with the full implementation**

```java
package Commands.Calendar;

import Commands.Framework.SlashCommand;
import DataHandlers.CalendarHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SessionCommands implements SlashCommand {

    private final SessionDateParser dates = new SessionDateParser();

    @Override
    public String getId() {
        return "session";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("session");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("session", "Manage D&D sessions")
                .addSubcommands(
                        new SubcommandData("add", "Add a session to the calendar")
                                .addOptions(new OptionData(OptionType.STRING, "date", "Date as dd/MM/yyyy", true)),
                        new SubcommandData("remove", "Remove a session")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "date", "Date as dd/MM/yyyy"),
                                        new OptionData(OptionType.INTEGER, "index", "1-based index in the list"),
                                        new OptionData(OptionType.BOOLEAN, "all", "Remove every session")),
                        new SubcommandData("list", "Show the calendar")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        CalendarHandler calendar = new CalendarHandler(guild);
        switch (event.getSubcommandName()) {
            case "add" -> add(event, guild, calendar);
            case "remove" -> remove(event, guild, calendar);
            case "list" -> list(event, calendar);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, Guild guild, CalendarHandler calendar) {
        String raw = event.getOption("date").getAsString();
        LocalDateTime date;
        try {
            date = dates.parse(raw);
        } catch (DateTimeParseException ex) {
            event.reply("Usage: /session add date:dd/MM/yyyy").setEphemeral(true).queue();
            return;
        }
        if (!dates.isInFuture(date, LocalDateTime.now())) {
            event.reply("You cannot plan a session in the past.").setEphemeral(true).queue();
            return;
        }
        if (calendar.getSessions(false).contains(date)) {
            event.reply("There already is a session planned on " + raw).setEphemeral(true).queue();
            return;
        }
        calendar.addSession(date);
        SessionReminder.makeMessage(date, guild);
        event.reply("Successfully added a session on " + raw).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild, CalendarHandler calendar) {
        boolean all = event.getOption("all", false, OptionMapping::getAsBoolean);
        if (all) {
            new ArrayList<>(calendar.getSessions(true)).forEach(d -> {
                calendar.removeSession(d);
                SessionReminder.cancelSession(guild.getId(), d);
            });
            event.reply("Successfully removed all sessions.").queue();
            return;
        }
        Integer index = event.getOption("index", null, OptionMapping::getAsInt);
        String raw = event.getOption("date", null, OptionMapping::getAsString);

        LocalDateTime target = null;
        if (raw != null) {
            try {
                target = dates.parse(raw);
            } catch (DateTimeParseException ex) {
                event.reply("Usage: /session remove date:dd/MM/yyyy").setEphemeral(true).queue();
                return;
            }
            if (!calendar.getSessions(true).contains(target)) {
                event.reply("There is no session on " + raw).setEphemeral(true).queue();
                return;
            }
        } else if (index != null) {
            List<LocalDateTime> upcoming = calendar.getSessions(false);
            if (index < 1 || index > upcoming.size()) {
                event.reply("There is no session #" + index).setEphemeral(true).queue();
                return;
            }
            target = upcoming.get(index - 1);
        } else {
            event.reply("Provide a date, an index, or all:true.").setEphemeral(true).queue();
            return;
        }
        calendar.removeSession(target);
        SessionReminder.cancelSession(guild.getId(), target);
        event.reply("Successfully removed the session on " + dates.format(target)).queue();
    }

    private void list(SlashCommandInteractionEvent event, CalendarHandler calendar) {
        List<LocalDateTime> sessions = calendar.getSessions(false);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.addField("Next Session:", sessions.isEmpty() ? "No planned sessions yet" : dates.format(sessions.get(0)), true);
        if (sessions.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < sessions.size(); i++) {
                sb.append(i == 1 ? "" : "\n").append(dates.format(sessions.get(i)));
            }
            eb.addField("Planned Sessions:", sb.toString(), false);
        }
        event.replyEmbeds(eb.build()).queue();
    }
}
```

- [ ] **Step 2: Compile + test**

Run: `mvn -q clean package`
Expected: BUILD SUCCESS, tests pass.

- [ ] **Step 3: Manual smoke test** — in the test guild: `/session add date:31/12/2026`, `/session list`, `/session remove index:1`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(session): /session add|remove|list as slash commands"
```

---

# Phase 3 — Food commands

## Task 3.1: Implement `FoodCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/Food/FoodCommands.java`

- [ ] **Step 1: Replace the stub**

```java
package Commands.Food;

import Commands.Framework.SlashCommand;
import DataHandlers.FoodHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FoodCommands implements SlashCommand {

    @Override
    public String getId() {
        return "food";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("food");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("food", "Manage the lunch list")
                .addSubcommands(
                        new SubcommandData("add", "Add food to the lunch list")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "name", "Name of the food", true),
                                        new OptionData(OptionType.STRING, "emoji", "Emoji to react with", true)),
                        new SubcommandData("remove", "Remove food from the lunch list")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "emoji", "Emoji of the food"),
                                        new OptionData(OptionType.INTEGER, "index", "1-based index in the list")),
                        new SubcommandData("list", "Show the lunch list"),
                        new SubcommandData("poll", "Post the lunch poll in the food channel")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        FoodHandler food = new FoodHandler(guild);
        switch (event.getSubcommandName()) {
            case "add" -> add(event, food);
            case "remove" -> remove(event, food);
            case "list" -> list(event, food);
            case "poll" -> poll(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, FoodHandler food) {
        String name = event.getOption("name").getAsString();
        String emoji = event.getOption("emoji").getAsString();
        if (food.checkFood(emoji) == -1) {
            food.addFood(name, emoji);
            event.reply(String.format("Successfully added %s to the lunch-list with %s as emoji", name, emoji)).queue();
        } else {
            event.reply(String.format("The emoji %s is already part of the lunch-list", emoji)).setEphemeral(true).queue();
        }
    }

    private void remove(SlashCommandInteractionEvent event, FoodHandler food) {
        Integer index = event.getOption("index", null, OptionMapping::getAsInt);
        String emoji = event.getOption("emoji", null, OptionMapping::getAsString);
        List<Map<String, String>> list = food.getFood();
        int target;
        if (index != null) {
            if (index < 1 || index > list.size()) {
                event.reply(String.format("The lunch-list only contains %d items", list.size())).setEphemeral(true).queue();
                return;
            }
            target = index - 1;
        } else if (emoji != null) {
            target = food.checkFood(emoji);
            if (target == -1) {
                event.reply(emoji + " is not an emoji on the lunch-list").setEphemeral(true).queue();
                return;
            }
        } else {
            event.reply("Provide an emoji or an index.").setEphemeral(true).queue();
            return;
        }
        String name = list.get(target).get("Name");
        String foundEmoji = list.get(target).get("Emoji");
        food.removeFood(target);
        event.reply(String.format("Successfully removed %s with %s as emoji", name, foundEmoji)).queue();
    }

    private void list(SlashCommandInteractionEvent event, FoodHandler food) {
        List<Map<String, String>> list = food.getFood();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.isEmpty()) {
            eb.addField("Food", "The lunch-list is empty", true);
        } else {
            StringBuilder sb = new StringBuilder();
            List<Map<String, String>> items = new ArrayList<>(list);
            for (int i = 0; i < items.size(); i++) {
                sb.append(i == 0 ? "" : "\n").append(items.get(i).get("Name")).append(" ").append(items.get(i).get("Emoji"));
            }
            eb.addField("Food", sb.toString(), true);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private void poll(SlashCommandInteractionEvent event, Guild guild) {
        LunchMessager.makeMessage(null, guild);
        event.reply("Lunch poll posted.").setEphemeral(true).queue();
    }
}
```

Note: `FoodHandler.checkFood` has a known off-by-one bug (returns `i-1`) — left as-is for roadmap item 4; this command faithfully calls it.

- [ ] **Step 2: Compile + test** — `mvn -q clean package` → BUILD SUCCESS.
- [ ] **Step 3: Manual smoke test** — `/food add name:Pizza emoji:🍕`, `/food list`, `/food poll`, `/food remove emoji:🍕`.
- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(food): /food add|remove|list|poll as slash commands"
```

---

# Phase 4 — NPC message commands (incl. former DM-relay)

## Task 4.1: Implement `NpcCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/NPCMessages/NpcCommands.java`

- [ ] **Step 1: Replace the stub**

```java
package Commands.NPCMessages;

import Commands.Framework.SlashCommand;
import DataHandlers.CharacterHandler;
import DataHandlers.NPCMessageHandler;
import Players.DM;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;
import java.util.Map;

public class NpcCommands implements SlashCommand {

    @Override
    public String getId() {
        return "npc";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("npc");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("npc", "Manage NPC messages")
                .addSubcommands(
                        new SubcommandData("add", "Add an NPC message")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "message", "The message text", true),
                                        new OptionData(OptionType.STRING, "npc", "NPC name (optional)"),
                                        new OptionData(OptionType.STRING, "type", "Message type")
                                                .addChoice("Basic", "Basic")
                                                .addChoice("Specific (next session, DM only)", "Specific"),
                                        new OptionData(OptionType.BOOLEAN, "private", "Reply only to you")),
                        new SubcommandData("remove", "Remove a basic NPC message")
                                .addOptions(new OptionData(OptionType.INTEGER, "index", "0-based index", true)),
                        new SubcommandData("list", "Show the basic NPC messages")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        switch (event.getSubcommandName()) {
            case "add" -> add(event, guild);
            case "remove" -> remove(event, guild);
            case "list" -> list(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, Guild guild) {
        String message = event.getOption("message").getAsString();
        String npc = event.getOption("npc", "", OptionMapping::getAsString);
        String type = event.getOption("type", "Basic", OptionMapping::getAsString);
        boolean isPrivate = event.getOption("private", false, OptionMapping::getAsBoolean);

        if (("Specific".equals(type) || isPrivate) && !isDm(event.getMember(), guild)) {
            event.reply("Only the DM can add specific/private NPC messages.").setEphemeral(true).queue();
            return;
        }
        if (npc.isEmpty()) {
            CharacterHandler chh = new CharacterHandler(guild);
            // keep parity with old behaviour: only known NPC names are stored as a name
            if (!chh.getAllCharacterNames(true).contains(npc)) {
                npc = "";
            }
        }
        new NPCMessageHandler(guild).addMessage(message, type, npc);
        event.reply(String.format("Added '%s' to the %s NPC-message list", message, type)).setEphemeral(isPrivate).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        int index = event.getOption("index").getAsInt();
        NPCMessageHandler handler = new NPCMessageHandler(guild);
        if (index >= 0 && index < handler.getBasicMessages().size()) {
            handler.removeMessage(index);
            event.reply("Removed message #" + index).queue();
        } else {
            event.reply("There is no message #" + index).setEphemeral(true).queue();
        }
    }

    private void list(SlashCommandInteractionEvent event, Guild guild) {
        List<Map<String, String>> messages = new NPCMessageHandler(guild).getBasicMessages();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Basic NPC messages");
        for (Map<String, String> m : messages) {
            String name = m.get("npc");
            eb.addField(name != null && !name.isEmpty() ? name : "Sumarville", m.get("message"), false);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private boolean isDm(Member member, Guild guild) {
        if (member == null) {
            return false;
        }
        Role dmRole = new DM(guild).getRole();
        return dmRole != null && member.getRoles().contains(dmRole);
    }
}
```

- [ ] **Step 2: Compile + test** — `mvn -q clean package` → BUILD SUCCESS.
- [ ] **Step 3: Manual smoke test** — `/npc add message:Hello npc:Gandalf`, `/npc list`, `/npc add message:Secret type:Specific private:true` (as DM), `/npc remove index:0`.
- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(npc): /npc add|remove|list slash commands, replacing the DM relay"
```

---

# Phase 5 — Character commands (modal + buttons)

## Task 5.1: Implement `CharacterCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/Users/CharacterCommands.java`

- [ ] **Step 1: Replace the stub**

```java
package Commands.Users;

import Commands.Framework.SlashCommand;
import DataHandlers.CharacterHandler;
import Players.DM;
import Players.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CharacterCommands implements SlashCommand {

    @Override
    public String getId() {
        return "character";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("character");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("character", "Manage character sheets")
                .addSubcommands(
                        new SubcommandData("create", "Create your character sheet")
                                .addOptions(new OptionData(OptionType.BOOLEAN, "as_npc", "Create an NPC instead of your own character")),
                        new SubcommandData("edit", "Edit a character attribute")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "name", "Character name", true),
                                        new OptionData(OptionType.STRING, "attribute", "Attribute to change", true)
                                                .addChoice("name", "name")
                                                .addChoice("picture", "picture"),
                                        new OptionData(OptionType.STRING, "value", "New value", true)),
                        new SubcommandData("remove", "Remove a character")
                                .addOptions(new OptionData(OptionType.STRING, "name", "Character name", true)),
                        new SubcommandData("show", "Show a character (or all)")
                                .addOptions(new OptionData(OptionType.STRING, "target", "all, npc, or a character name"))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        switch (event.getSubcommandName()) {
            case "create" -> create(event, guild);
            case "edit" -> edit(event, guild);
            case "remove" -> remove(event, guild);
            case "show" -> show(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void create(SlashCommandInteractionEvent event, Guild guild) {
        boolean asNpc = event.getOption("as_npc", false, OptionMapping::getAsBoolean);
        Member member = event.getMember();
        CharacterHandler handler = new CharacterHandler(guild);

        if (!asNpc) {
            if (member != null && isDm(member, guild)) {
                event.reply("Character creation cancelled — you are the DM.").setEphemeral(true).queue();
                return;
            }
            if (member != null && handler.getCharacter(member.getId(), "userid") != null) {
                event.reply("You already have a character. Overwrite it?")
                        .setEphemeral(true)
                        .addComponents(ActionRow.of(
                                Button.danger("character:overwrite:self", "Overwrite"),
                                Button.secondary("character:cancel", "Cancel")))
                        .queue();
                return;
            }
        }
        event.replyModal(buildModal(asNpc)).queue();
    }

    private Modal buildModal(boolean asNpc) {
        TextInput name = TextInput.create("name", TextInputStyle.SHORT)
                .setPlaceholder("Character name")
                .setRequired(true)
                .setMaxLength(100)
                .build();
        TextInput picture = TextInput.create("picture", TextInputStyle.SHORT)
                .setPlaceholder("Image URL (optional)")
                .setRequired(false)
                .build();
        return Modal.create(asNpc ? "character:create:npc" : "character:create:self", "Create character")
                .addComponents(Label.of("Name", name), Label.of("Picture", picture))
                .build();
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("character:cancel")) {
            event.editMessage("Character creation cancelled.").setComponents().queue();
        } else if (id.equals("character:overwrite:self")) {
            Guild guild = event.getGuild();
            if (guild != null && event.getMember() != null) {
                new CharacterHandler(guild).removeCharacter(event.getMember().getId(), "userid");
            }
            event.replyModal(buildModal(false)).queue();
        }
    }

    @Override
    public void onModal(ModalInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("This only works in a server.").setEphemeral(true).queue();
            return;
        }
        boolean asNpc = event.getModalId().equals("character:create:npc");
        String name = event.getValue("name").getAsString();
        String picture = event.getValue("picture") != null ? event.getValue("picture").getAsString() : "";

        Map<String, String> sheet = new java.util.HashMap<>();
        sheet.put("userid", asNpc ? "" : member.getId());
        sheet.put("name", name);
        sheet.put("picture", picture);

        new CharacterHandler(guild).addCharacter(sheet);
        if (!asNpc) {
            guild.addRoleToMember(member, new Player(guild).getRole()).queue();
            guild.modifyNickname(member, name).queue();
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setTitle("Newly Created Character:");
        eb.addField("Name", name, true);
        eb.addField("Picture", picture.isEmpty() ? "—" : picture, true);
        event.replyEmbeds(eb.build()).queue();
    }

    private void edit(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        String attribute = event.getOption("attribute").getAsString();
        String value = event.getOption("value").getAsString();
        CharacterHandler handler = new CharacterHandler(guild);
        if (handler.getCharacter(name, "name") == null) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        handler.editCharacter(name, attribute, value);
        if (attribute.equals("name")) {
            Map<String, String> updated = handler.getCharacter(value, "name");
            if (updated != null && !updated.get("userid").isEmpty()) {
                Member m = guild.getMemberById(updated.get("userid"));
                if (m != null) {
                    guild.modifyNickname(m, value).queue();
                }
            }
        }
        event.reply(String.format("Edited %s of %s to %s", attribute, name, value)).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        CharacterHandler handler = new CharacterHandler(guild);
        Map<String, String> character = handler.getCharacter(name, "name");
        if (character == null) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        handler.removeCharacter(name, "name");
        if (!character.get("userid").isEmpty()) {
            Member m = guild.getMemberById(character.get("userid"));
            if (m != null) {
                guild.modifyNickname(m, null).queue();
            }
        }
        event.reply("Successfully removed " + name).queue();
    }

    private void show(SlashCommandInteractionEvent event, Guild guild) {
        String target = event.getOption("target", "all", OptionMapping::getAsString);
        CharacterHandler handler = new CharacterHandler(guild);
        List<Map<String, String>> list;
        if (target.equalsIgnoreCase("all")) {
            list = handler.getAllCharacters(false);
        } else if (target.equalsIgnoreCase("npc")) {
            list = handler.getAllCharacters(true);
        } else {
            Map<String, String> character = handler.getCharacter(target, "name");
            if (character == null) {
                character = handler.getCharacter(target, "userid");
            }
            list = character == null ? null : new ArrayList<>(Collections.singletonList(character));
        }
        if (list == null) {
            event.reply("No character found for " + target).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.size() == 1) {
            for (Map.Entry<String, String> e : list.get(0).entrySet()) {
                String key = e.getKey();
                eb.addField(key.substring(0, 1).toUpperCase() + key.substring(1), e.getValue(), true);
            }
        } else {
            eb.setTitle("Players");
            for (Map<String, String> c : list) {
                String userid = c.get("userid");
                String who = userid.isEmpty() ? "NPC"
                        : (guild.getMemberById(userid) != null ? guild.getMemberById(userid).getAsMention() : "Unknown");
                eb.addField(c.get("name"), who, true);
            }
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private boolean isDm(Member member, Guild guild) {
        var dmRole = new DM(guild).getRole();
        return dmRole != null && member.getRoles().contains(dmRole);
    }
}
```

- [ ] **Step 2: Compile + test** — `mvn -q clean package` → BUILD SUCCESS.
- [ ] **Step 3: Manual smoke test** — `/character create` (fill the modal), run it again to hit the Overwrite/Cancel buttons, `/character create as_npc:true`, `/character show`, `/character show target:npc`, `/character edit name:X attribute:picture value:http://...`, `/character remove name:X`.
- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(character): /character create (modal)|edit|remove|show, replacing the conversational builder"
```

---

# Phase 6 — Dungeon Master commands

## Task 6.1: Implement `DmCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/Users/DmCommands.java`

- [ ] **Step 1: Replace the stub**

```java
package Commands.Users;

import Commands.Framework.SlashCommand;
import Players.DM;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class DmCommands implements SlashCommand {

    @Override
    public String getId() {
        return "dm";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("dm");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("dm", "Claim or pass the Dungeon Master role")
                .addSubcommands(
                        new SubcommandData("claim", "Claim the DM role if nobody has it"),
                        new SubcommandData("pass", "Pass the DM role to someone else")
                                .addOptions(new OptionData(OptionType.USER, "user", "New Dungeon Master", true))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        Role dmRole = new DM(guild).getRole();
        List<Member> currentDms = guild.getMembersWithRoles(dmRole);

        if (event.getSubcommandName().equals("claim")) {
            if (currentDms.isEmpty()) {
                guild.addRoleToMember(member, dmRole).queue();
                event.reply("You are now the Dungeon Master.").queue();
            } else {
                event.reply("There already is a Dungeon Master.").setEphemeral(true).queue();
            }
            return;
        }

        // pass
        if (currentDms.isEmpty() || !currentDms.get(0).equals(member)) {
            event.reply("You are not the DM, so you can't pass the role.").setEphemeral(true).queue();
            return;
        }
        Member newDm = event.getOption("user").getAsMember();
        if (newDm == null) {
            event.reply("That user is not part of this server.").setEphemeral(true).queue();
            return;
        }
        guild.addRoleToMember(newDm, dmRole).queue();
        guild.removeRoleFromMember(member, dmRole).queue();
        event.reply("Passed the Dungeon Master role to " + newDm.getEffectiveName()).queue();
    }
}
```

- [ ] **Step 2: Compile + test** — `mvn -q clean package` → BUILD SUCCESS.
- [ ] **Step 3: Manual smoke test** — `/dm claim`, then `/dm pass user:@someone` (as current DM).
- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(dm): /dm claim|pass as slash commands"
```

---

# Phase 7 — Config commands

## Task 7.1: Create `ConfigKeys` (pure logic) — TDD

**Files:**
- Create: `src/main/java/Commands/ConfigKeys.java`
- Test: `src/test/java/Commands/ConfigKeysTest.java`

- [ ] **Step 1: Write the failing test**

```java
package Commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigKeysTest {

    @Test
    void roleKeysAreRecognised() {
        assertTrue(ConfigKeys.isRoleKey("DM"));
        assertTrue(ConfigKeys.isRoleKey("Player"));
        assertFalse(ConfigKeys.isRoleKey("FoodChannel"));
    }

    @Test
    void channelKeysAreRecognised() {
        assertTrue(ConfigKeys.isChannelKey("DMChannel"));
        assertTrue(ConfigKeys.isChannelKey("CalendarChannel"));
        assertTrue(ConfigKeys.isChannelKey("FoodChannel"));
        assertTrue(ConfigKeys.isChannelKey("MemeChannel"));
        assertFalse(ConfigKeys.isChannelKey("DM"));
    }
}
```

- [ ] **Step 2: Run, verify fail** — `mvn -q test -Dtest=ConfigKeysTest` → FAIL (no class).

- [ ] **Step 3: Implement**

```java
package Commands;

import java.util.Set;

public final class ConfigKeys {

    public static final Set<String> ROLE_KEYS = Set.of("DM", "Player");
    public static final Set<String> CHANNEL_KEYS = Set.of("DMChannel", "CalendarChannel", "FoodChannel", "MemeChannel");

    private ConfigKeys() {
    }

    public static boolean isRoleKey(String key) {
        return ROLE_KEYS.contains(key);
    }

    public static boolean isChannelKey(String key) {
        return CHANNEL_KEYS.contains(key);
    }
}
```

- [ ] **Step 4: Run, verify pass** — `mvn -q test -Dtest=ConfigKeysTest` → PASS.

## Task 7.2: Implement `ConfigCommands` (replace the stub)

**Files:**
- Modify: `src/main/java/Commands/ConfigCommands.java`

- [ ] **Step 1: Replace the stub**

```java
package Commands;

import Commands.Framework.SlashCommand;
import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class ConfigCommands implements SlashCommand {

    @Override
    public String getId() {
        return "config";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("config");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        OptionData roleSetting = new OptionData(OptionType.STRING, "setting", "Which role to set", true)
                .addChoice("DM", "DM")
                .addChoice("Player", "Player");
        OptionData channelSetting = new OptionData(OptionType.STRING, "setting", "Which channel to set", true)
                .addChoice("DM channel", "DMChannel")
                .addChoice("Calendar channel", "CalendarChannel")
                .addChoice("Food channel", "FoodChannel")
                .addChoice("Meme channel", "MemeChannel");
        return List.of(Commands.slash("config", "Configure Sumarville")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("role", "Map a server role to a Sumarville role")
                                .addOptions(roleSetting,
                                        new OptionData(OptionType.ROLE, "value", "The role", true)),
                        new SubcommandData("channel", "Map a channel to a Sumarville function")
                                .addOptions(channelSetting,
                                        new OptionData(OptionType.CHANNEL, "value", "The channel", true))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        ConfigHandler config = new ConfigHandler(guild);
        String setting = event.getOption("setting").getAsString();

        if (event.getSubcommandName().equals("role")) {
            if (!ConfigKeys.isRoleKey(setting)) {
                event.reply("Unknown role setting.").setEphemeral(true).queue();
                return;
            }
            Role role = event.getOption("value").getAsRole();
            config.setConfig(role.getId(), setting);
            event.reply(String.format("Set the %s role to %s", setting, role.getName())).queue();
        } else {
            if (!ConfigKeys.isChannelKey(setting)) {
                event.reply("Unknown channel setting.").setEphemeral(true).queue();
                return;
            }
            GuildChannel channel = event.getOption("value").getAsChannel();
            config.setConfig(channel.getId(), setting);
            event.reply(String.format("Set the %s to %s", setting, channel.getName())).queue();
        }
    }
}
```

- [ ] **Step 2: Compile + test** — `mvn -q clean package` → BUILD SUCCESS; all tests pass.
- [ ] **Step 3: Manual smoke test** — `/config role setting:DM value:@DM`, `/config channel setting:FoodChannel value:#lunch`. Confirm a non-admin cannot see/use `/config`.
- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(config): /config role|channel with typed pickers and Manage-Server gate"
```

---

# Phase 8 — Final verification

## Task 8.1: Full build, test, and end-to-end smoke test

- [ ] **Step 1: Clean build + all tests**

Run: `mvn -q clean package`
Expected: BUILD SUCCESS; `DiceRollerTest`, `SessionDateParserTest`, `ConfigKeysTest` all pass; shaded jar produced.

- [ ] **Step 2: Run against the test guild and exercise every command**

With `BOT_TOKEN` + `TEST_GUILD_ID` set, verify the full surface: dice (`/d4`–`/d100`, `/roll`, adv/dis, amount), `/session add|remove|list`, `/food add|remove|list|poll`, `/npc add|remove|list` (incl. DM-only specific/private), `/character create|edit|remove|show` (modal + overwrite buttons + as_npc), `/dm claim|pass`, `/config role|channel`, `/help`.

- [ ] **Step 3: Confirm `MESSAGE_CONTENT` is gone**

Grep the source for `MESSAGE_CONTENT` and `getContentRaw` — there should be no matches. Confirm `Main` declares only `GatewayIntent.GUILD_MEMBERS`.

Run: `git grep -nE "MESSAGE_CONTENT|getContentRaw|jda\.core" -- src/main` → expected: no output.

- [ ] **Step 4: Commit any final cleanups (if needed)**

```bash
git add -A
git commit -m "chore(jda6): final cleanup after slash command migration"
```

---

## Notes carried forward (out of scope here)

- **Roadmap item 3:** replace `Data.json`/json-simple with SQLite + SQLCipher; revisit the `DataHandlers` (still JDA-coupled via `Guild`).
- **Roadmap item 4 (bugs left in the data layer):** `FoodHandler.checkFood` off-by-one (`return i - 1`); `ConfigHandler.getChannel` returns a boxed `long` default and can NPE on `getDefaultChannel()`.
- **Roadmap item 5:** SLF4J/Logback + Sentry (replace `printStackTrace`), `SENTRY_DSN` env var, CI; note `.gitignore` ignores `*.xml`, which will hide a new `logback.xml` unless force-added.
- **Discord portal:** after deploy, register the global commands once; the `GUILD_MEMBERS` intent still needs the privileged-intent review (item 1).
```
