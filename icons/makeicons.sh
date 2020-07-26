#!/bin/bash
cd `dirname $0`
convert main-page.png -resize 288x288 -fill black -draw 'rectangle 0,0 288,288' -fill white -draw 'circle 144,144 0,144' main-circle.png
convert main-page.png -resize 288x288 main-circle.png -alpha off -compose copy_opacity -composite main-round.png
convert main-round.png -resize  72x72  ../app/src/main/res/mipmap-hdpi/ic_launcher.png
convert main-round.png -resize  48x48  ../app/src/main/res/mipmap-mdpi/ic_launcher.png
convert main-round.png -resize  96x96  ../app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert main-round.png -resize 144x144 ../app/src/main/res/mipmap-xxhdpi/ic_launcher.png
