#!/bin/bash
cd `dirname $0`
rsync -auv help.php toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/index.php
rsync -auv *.png toutatis:/var/www/www.outerworldapps.com/docs/HSIWatch/
