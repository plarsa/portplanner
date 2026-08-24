#!/bin/bash
# /opt/portplaner/deploy.sh
# Placed on EC2 once. Called by GitHub Actions via SSM.
set -euo pipefail

APP_DIR="/opt/portplaner"
JAR="$APP_DIR/app.jar"
SERVICE="portplaner"
S3_BUCKET="DIN-S3-BUCKET"   # ersätt med ditt bucket-namn

echo "=== Portplaner deploy $(date -u) ==="

echo "Downloading JAR from S3..."
aws s3 cp "s3://$S3_BUCKET/portplaner/app.jar" "$JAR.new"

echo "Stopping service..."
systemctl stop "$SERVICE" || true

echo "Replacing JAR..."
mv "$JAR.new" "$JAR"

echo "Starting service..."
systemctl start "$SERVICE"

echo "Waiting for service to come up..."
sleep 5
systemctl is-active --quiet "$SERVICE" && echo "Service is running." || {
  echo "ERROR: service failed to start"
  journalctl -u "$SERVICE" -n 50 --no-pager
  exit 1
}

echo "=== Deploy complete ==="
