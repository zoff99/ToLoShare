#! /bin/sh
url_start_screen='https://github.com/zoff99/ToFShare/releases/download/nightly/screen_shot_android_29_11.png'

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"
wget "$url_start_screen" -O ./fastlane/metadata/android/en-US/images/phoneScreenshots/101.png
