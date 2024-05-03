#! /bin/bash

# Usage: sh ../commit.sh "Commit message" [[<DATE_VALUE> [NUM_LINES]]
# Use within the sub directors to change a file with a given commit message
# Change will be to the named file.
# If provided, the days ago flag will set the commit to N days ago

file="file.txt"
CONTENT=$1
DATE_VALUE=$2
NUM_LINES=$3
[ -z "$DATE_VALUE" ] && DATE_VALUE="0 days ago"
[ -z "$NUM_LINES" ] && NUM_LINES="20"

echo "$CONTENT" > $file
for ((i=2;i<=NUM_LINES;i++)); do
    echo "$CONTENT" >> $file
done


git commit -m "$CONTENT" --date="$DATE_VALUE" -- $file
