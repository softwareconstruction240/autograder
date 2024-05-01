#! /bin/bash

# Usage: sh ../commit.sh "Commit message" [<DAYS AGO>]
# Use within the sub directors to change a file with a given commit message
# Change will be to the named file.
# If provided, the days ago flag will set the commit to N days ago

file="file.txt"
CONTENT=$1
DAYS_AGO=$2
[ -z "$DAYS_AGO" ] && DAYS_AGO="0"

echo "$CONTENT" > $file
git commit -m "$CONTENT" --date="$DAYS_AGO days ago" -- $file
