#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

f1="android-refimpl-app/app/src/main/AndroidManifest.xml"

cd "$basedir"

sed -i -e 's#<!--DEBUG##g' "$f1"
sed -i -e 's#DEBUG-->##g' "$f1"

