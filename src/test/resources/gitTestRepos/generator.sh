#! /bin/bash

# Usage: sh ../generator.sh START_CHANGE END_CHANGE [DAYS_AGO]
# Use within the sub directors to generate sequences of commits
# numbered from START to END. All the changes will be to the named file.
# If provided, all commits will be set to N days ago.

file="file.txt"
START=$1
END=$2
DAYS_AGO=$2
[ -z "$DAYS_AGO" ] && DAYS_AGO="0"

# https://stackoverflow.com/a/171041/2844859
for ((i=START;i<=END;i++)); do
  echo "Change $i" > $file
  git commit -m "Change $i" --date="$DAYS_AGO days ago" -- $file
done
