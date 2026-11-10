#!/bin/sh
# Nightly backup of the Sumarville database.
#
# The bot keeps one connection open in WAL mode, so copying the file while it runs can give an
# inconsistent copy. This script stops the bot for a few seconds, copies the database together
# with its -wal/-shm files, and starts it again. The copies stay encrypted with DB_ENCRYPTION_KEY.
#
# Backups older than RETENTION_DAYS are deleted, so the 30-day erasure promise in the
# privacy policy also holds for backups.
#
# Install (as root):
#   install -m 700 backup-db.sh /usr/local/sbin/sumarville-backup
#   echo '30 4 * * * root /usr/local/sbin/sumarville-backup' > /etc/cron.d/sumarville-backup
set -eu

SERVICE=sumarville
DATA_DIR=/opt/sumarville/data
BACKUP_DIR=/var/backups/sumarville
RETENTION_DAYS=30

stamp=$(date +%Y-%m-%d_%H%M)
target="$BACKUP_DIR/$stamp"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

systemctl stop "$SERVICE"
# Restart the bot even if the copy fails.
trap 'systemctl start "$SERVICE"' EXIT

mkdir "$target"
cp -p "$DATA_DIR"/sumarville.db* "$target"/

systemctl start "$SERVICE"
trap - EXIT

find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -mtime +"$RETENTION_DAYS" -exec rm -rf {} +

echo "Backup written to $target"
