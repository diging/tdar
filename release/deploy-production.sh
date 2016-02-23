#!/bin/sh
echoerr() { echo "$@" 1>&2; }
sudo echo "deploying production"
#java -XshowSettings 2>&1 | grep tmp | awk -F= '{print "wro4jDir:" $2 "/.wro4j/"}'

if [  $(id -u) -eq 0  ]
 then
   echoerr "This script should NOT be run as root"
   exit 1
fi

echo "the following users are logged in:"
LOGGED=$(curl -silent https://core.tdar.org/admin/loggedIn | grep "\[")
echo "\t$LOGGED"

while true; do
    read -p "Is the build clean (GREEN) and are there NO ACTIVE users? " yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

export PARAM=" -C "
while getopts Dr: opt; do
  case $opt in
    r)
      export PARAM=" -r $OPTARG "
      ;;
    D)
      export PARAM=""
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

echo " hg update $PARAM "


cd /home/tdar/tdar.src/
hg pull
hg update $PARAM
perl release/release.pl
if [ -e /usr/local/lib/nagios3/amazon-sns-notify.rb ]
then
        /usr/local/lib/nagios3/amazon-sns-notify.rb "tdar.org > DEPLOY - $(hg id -n -b)"
fi


cd /home/tdar/tdar.src/
hg pull
hg update -C
mvn clean install -DskipTests -Djetty.skip=true -pl database,locales,core,base,search
mvn clean compile war:war -Pminify-web-resources -pl tag,web,oai-pmh,dataone
mvn process-resources -Pliquibase -pl web

if [ $? -ne 0 ] 
  then
   echoerr "==============================================="
   echoerr "|               BUILD FAILED                  |"
   echoerr "|             SKIPPING  DEPLOY                |"
   echoerr "==============================================="
   exit 1
  else
      sudo service tomcat7 stop
      sudo cp web/target/tdar-web.war ~tdar/app/ROOT.war
      sudo rm -Rrf ~tdar/app/ROOT
      sudo cp oai-pmh/target/tdar-oai-pmh.war ~tdar/app/oai-pmh.war
      sudo rm -Rrf ~tdar/app/oai-pmh
      # sudo cp dataone/target/tdar-dataone.war ~tdar/app/dataone.war
      # sudo rm -Rrf ~tdar/app/dataone
      sudo cp tag/target/tdar-tag.war ~tdar/app/services.war
      sudo rm -Rrf ~tdar/app/services
      sudo service tomcat7 restart
fi
