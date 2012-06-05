#!/bin/sh
# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- 
#
# Given output from generate.sh in a directory (FILESDIR), issue commands
# to this script to place files in proper place for agent.
#
# I.e., this script makes it easier to prep the agent for each test.
#
# Bryan Smith - Sun May 17 2010
# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- 

# Update this to point to directories holding the declarative add
# and initialization files
FILESDIR="/data/Sources/Soar/family-reasoner/trial-files"

# Update this to point to agent root directory (where init and propose
# files go)
AGENTDIR="/data/Sources/Soar/family-reasoner"

if [ "$#" -ne "3" ]; then
  echo "Three required parameters: (i) number of families, (ii) number of queries and (iii) trial number"
  exit 1
fi

SUFFIX="$1-families-$2-queries-$3-trial"

DECLARATIVE_ADD_FROM="$FILESDIR/_firstload.$SUFFIX"
DECLARATIVE_ADD_TO="$AGENTDIR/_firstload.soar"

echo "> cp -f $DECLARATIVE_ADD_FROM $DECLARATIVE_ADD_TO"
cp -f $DECLARATIVE_ADD_FROM $DECLARATIVE_ADD_TO

INIT_FROM="$FILESDIR/initialize-family-reasoner.$SUFFIX"
INIT_TO="$AGENTDIR/initialize-family-reasoner.soar"

echo "> cp -f $INIT_FROM $INIT_TO"
cp -f $INIT_FROM $INIT_TO


