#! /bin/bash

# Usage: sh ../generator.sh START_CHANGE END_CHANGE
# Use within the sub directors to generate sequences of commits
# numbered from START to END. All the changes will be to the named file.

file="file.txt"
START=$1
END=$2

# https://stackoverflow.com/a/171041/2844859
for ((i=START;i<=END;i++)); do
  echo "Change $i" > $file
  git commit -m "Change $i" -- $file
done
