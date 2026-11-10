# Deploying Sumarville on Linux

Runs the bot as a systemd service under its own user, with secrets in a root-only env file and a nightly encrypted backup.

Files in this folder:

- `sumarville.service` — systemd unit
- `sumarville.env.example` — template for `/etc/sumarville/sumarville.env`
- `backup-db.sh` — nightly database backup with 30-day retention

## 1. Build (on your PC)

```
mvn clean package
```

Upload `target/DnD_Discord_Bot-1.0-SNAPSHOT-shaded.jar` to the server, e.g. to your home directory. **Not** the `Sumarville.jar` in the repo root: that is the 2019 build.

## 2. Prepare the server (once)

Java 21 or newer:

```
sudo apt install openjdk-21-jre-headless   # Ubuntu 22.04+ / Debian 13
java -version
```

On Debian 12, `openjdk-21` is not in the default repositories; use the Adoptium (Temurin) repository instead.

User and folders:

```
sudo useradd --system --no-create-home --shell /usr/sbin/nologin sumarville
sudo mkdir -p /opt/sumarville /etc/sumarville
```

Secrets:

```
sudo install -m 600 -o root -g root sumarville.env.example /etc/sumarville/sumarville.env
openssl rand -base64 32          # paste as DB_ENCRYPTION_KEY, and save it in your password manager
sudo nano /etc/sumarville/sumarville.env
```

## 3. Stop the old bot and move its data

Find how the old bot runs:

```
ps -ef | grep [j]ava
```

Stop it (`kill <pid>`, or quit its `screen`/`tmux` session, or `systemctl stop` if it was a service). Note the folder it ran in: its `Data.json` is there.

```
sudo cp -p /path/to/old/Data.json /path/to/old/Data.json.bak-before-sumarville   # keep for rollback
sudo cp /path/to/old/Data.json /opt/sumarville/Data.json
sudo cp ~/DnD_Discord_Bot-1.0-SNAPSHOT-shaded.jar /opt/sumarville/sumarville.jar
sudo chown -R sumarville:sumarville /opt/sumarville
```

## 4. Start

```
sudo cp sumarville.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now sumarville
journalctl -u sumarville -f
```

Expect `Imported legacy data for N guilds from /opt/sumarville/Data.json`. The importer renames `Data.json` to `Data.json.imported`.

If it refuses to start:

- `DB_ENCRYPTION_KEY is not set` — the env file is missing or empty.
- `No legacy Data.json at …` — put it in `/opt/sumarville` and `sudo systemctl restart sumarville`.
- `Disallowed intents` / close code 4014 — turn on **Server Members Intent** in the Developer Portal (Bot tab).

## 5. Check

- In a server where the bot has been for years, type `/` and check the bot's commands appear. If not, re-invite it with the scopes `bot applications.commands` (this does not kick it).
- Try `/help`, `/session`, `/character show`.
- Then remove the plaintext copy: `sudo rm /opt/sumarville/Data.json.imported`. Keep the `.bak` from step 3 somewhere safe until you are sure, then delete it too.

## 6. Backups

```
sudo install -m 700 backup-db.sh /usr/local/sbin/sumarville-backup
echo '30 4 * * * root /usr/local/sbin/sumarville-backup' | sudo tee /etc/cron.d/sumarville-backup
sudo /usr/local/sbin/sumarville-backup   # test once
```

The bot is down for a few seconds each night. Backups stay encrypted with the same key. Copy them off the server too if you can.

## Updating later

```
sudo cp ~/DnD_Discord_Bot-1.0-SNAPSHOT-shaded.jar /opt/sumarville/sumarville.jar
sudo systemctl restart sumarville
```

## Rollback

```
sudo systemctl disable --now sumarville
```

Then start the old bot the way it ran before, with `Data.json.bak-before-sumarville` restored as its `Data.json`. Anything changed in the new bot since the switch is not in that file.
