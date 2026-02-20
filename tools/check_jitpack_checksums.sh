#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../android-refimpl-app/"
cd "$basedir"

f1='app/witness.gradle'

INPUT=$(cat "$f1" 2>/dev/null |grep 'com.github.zoff99:pkgs_ToxAndroidRefImpl:')

#!/bin/bash

# 1. Define the input string
# INPUT="'com.github.zoff99:pkgs_ToxAndroidRefImpl:1.0.172:pkgs_ToxAndroidRefImpl-1.0.172.aar:48e8125b6432960f67602e106831493417d6fbcb6581e38dc671653136b22829',"

# 2. Isolate the Filename and the Embedded Hash
# We use 'cut' with ':' as a delimiter to pull fields 4 and 5
FILENAME=$(echo "$INPUT" | cut -d':' -f4)
EMBEDDED_HASH=$(echo "$INPUT" | cut -d':' -f5 | tr -d "',")

echo "Target File: $FILENAME"
echo "Embedded Hash: $EMBEDDED_HASH"

# 3. Download the official .sha256 file
SHA_URL="https://raw.githubusercontent.com/zoff99/pkgs_ToxAndroidRefImpl/refs/heads/main/pkgs_ToxAndroidRefImpl-1.0.172.aar.sha256"
REMOTE_SHA_FILE="remote.sha256"

echo "Downloading remote hash..."
curl -sL "$SHA_URL" -o "$REMOTE_SHA_FILE"

# 4. Extract the hash from the downloaded file
# (Removing extra spaces/filenames that might be in the .sha256 file)
REMOTE_HASH=$(awk '{print $1}' "$REMOTE_SHA_FILE")

echo "Remote Hash:   $REMOTE_HASH"

# 5. Compare the sums
if [ "$EMBEDDED_HASH" == "$REMOTE_HASH" ]; then
    echo "SUCCESS: The SHA256 sums match!"
    rm "$REMOTE_SHA_FILE"
    exit 0
else
    echo "FAILURE: The SHA256 sums do NOT match."
    rm "$REMOTE_SHA_FILE"
    exit 1
fi


