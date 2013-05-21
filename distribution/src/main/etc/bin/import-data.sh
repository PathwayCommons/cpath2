#!/bin/sh

# cPath2 version ${version} data import script
# to create new databases, import data, build the full-text index, summarize, organize, etc.
#
# Before you run this script, please define CPATH2_HOME; configure connection 
# and logging properties, metadata table in $CPATH2_HOME/metadata.conf; 
# download, and perhaps re-package and re-name pathway data archives in $CPATH2_HOME/data dir!
# See also: README.TXT.
#
# Edit this script as you like. 
# Mind using "--force" and organism names in the section #5 tasks
# (try running cpath2-cli.sh without arguments for more details).
#

echo "CPATH2 DATA IMPORT"

## Execute the following stages using cpath2-cli.sh script:
echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
maindb=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.db='  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "This cPath2 Instance Uses:"
echo "       main.db=$maindb"
echo "       xml.base=$xmlbase"

## %TODO - also check all required configuration and properties files are present and in good shape, and report errors, if any.

# START
# Drop/Create a new cPath2 database instance (using current metadata.conf and cpath2.properties)
while true; do
read -p "Create a new cPath2 instance using $CPATH2_HOME/cpath2.properties and $CPATH2_HOME/cpath2.properties?" yn
	case $yn in
        [Yy]* ) 
        sh $CPATH2_HOME/cpath2-cli.sh -fetch-metadata file://$CPATH2_HOME/metadata.conf;
        sh $CPATH2_HOME/cpath2-cli.sh -create-warehouse;
		sh $CPATH2_HOME/cpath2-cli.sh -update-mapping;   
        sh $CPATH2_HOME/cpath2-cli.sh -premerge; 
        sh $CPATH2_HOME/cpath2-cli.sh -merge --force;
        sh $CPATH2_HOME/cpath2-cli.sh -create-index;
        sh $CPATH2_HOME/cpath2-cli.sh -update-counts;
        sh $CPATH2_HOME/cpath2-cli.sh -create-blacklist;
        sh $CPATH2_HOME/cpath2-cli.sh -create-downloads;
        sh $CPATH2_HOME/cpath2-cli.sh -clear-cache;
        break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done


