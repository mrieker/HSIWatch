#!/bin/bash
cd `dirname $0`
rsync -auv help.html toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/index.html
rsync -auv *.png toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/
