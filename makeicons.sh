#!/bin/bash
cd `dirname $0`
convert icons/sfm-ils-7-at-facno-trimmed.png -resize  72x72   app/src/main/res/mipmap-hdpi/ic_launcher.png
convert icons/sfm-ils-7-at-facno-trimmed.png -resize  48x48   app/src/main/res/mipmap-mdpi/ic_launcher.png
convert icons/sfm-ils-7-at-facno-trimmed.png -resize  96x96   app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert icons/sfm-ils-7-at-facno-trimmed.png -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
