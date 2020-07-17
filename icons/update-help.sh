#!/bin/bash
cd `dirname $0`
rsync -auv help.html toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/index.html
rsync -auv main-page.png menu-page.png menu2-page.png filled-chin.png empty-chin.png toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/
