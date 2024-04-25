#! /bin/bash

# Usage: sh ../commit.sh "Commit message"
# Use within the sub directors to change a file with a given commit message
# Change will be to the named file.

file="file.txt"
CONTENT=$1

echo "$CONTENT" > $file
git commit -m "$CONTENT" -- $file
