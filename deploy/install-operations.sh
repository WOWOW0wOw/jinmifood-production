#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

sudo install -m 0644 deploy/systemd/jinmifood-backup.service /etc/systemd/system/jinmifood-backup.service
sudo install -m 0644 deploy/systemd/jinmifood-backup.timer /etc/systemd/system/jinmifood-backup.timer
sudo install -m 0644 deploy/systemd/jinmifood-build-cache-prune.service /etc/systemd/system/jinmifood-build-cache-prune.service
sudo install -m 0644 deploy/systemd/jinmifood-build-cache-prune.timer /etc/systemd/system/jinmifood-build-cache-prune.timer
sudo systemctl daemon-reload
sudo systemctl enable --now jinmifood-backup.timer
sudo systemctl enable --now jinmifood-build-cache-prune.timer

if ! sudo swapon --show=NAME --noheadings | grep -qx '/swapfile'; then
  if [ ! -f /swapfile ]; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile >/dev/null
  fi
  sudo swapon /swapfile
fi

if ! grep -q '^/swapfile ' /etc/fstab; then
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

printf 'vm.swappiness=10\nvm.vfs_cache_pressure=50\n' | sudo tee /etc/sysctl.d/99-jinmifood.conf >/dev/null
sudo sysctl --system >/dev/null

sudo systemctl list-timers jinmifood-backup.timer jinmifood-build-cache-prune.timer --no-pager
sudo swapon --show
