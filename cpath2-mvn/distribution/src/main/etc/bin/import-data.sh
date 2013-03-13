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
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
maindb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'main.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
warehousedb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'warehouse.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "CPATH2 Properities:"
echo "       main.db=$maindb"
echo "       warehouse.db=$warehousedb"
echo "       xml.base=$xmlbase"

## %TODO - also check all required configuration and properties files are present and in good shape, and report errors, if any.

# START
# 1. Drop/Create cPath2 databases (double-check db connection and names in $CPATH2_HOME/cpath.properties!)
while true; do
read -p "Create/replace $maindb database?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath2-cli.sh -create-tables $maindb; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done
while true; do  
read -p "Create/replace $warehousedb database?" yn
  case $yn in
	[Yy]* )  sh $CPATH2_HOME/cpath2-cli.sh -create-tables $warehousedb; break;;
	[Nn]* )  break;;
	* ) echo "Please answer yes or no.";;
  esac
done

# 2. Fetch Metadata (parse $CPATH2_HOME/metadata.conf)
while true; do
read -p "Fetch/update metadata using $CPATH2_HOME/metadata.conf?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath2-cli.sh -fetch-metadata file://$CPATH2_HOME/metadata.conf; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 3. Fetch all data and save to the local directory
while true; do
read -p "Fetch all data to the local directory ($CPATH2_HOME/data/)?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath2-cli.sh -fetch-data; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 4. Build the Warehouse and id-mapping tables
while true; do
read -p "Build the Warehouse and id-mapping tables?" yn
	case $yn in
        [Yy]* ) 
        	sh $CPATH2_HOME/cpath2-cli.sh -create-warehouse
			sh $CPATH2_HOME/cpath2-cli.sh -update-mapping
        	break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 5. Premerge, Merge, Index, create the blacklist and downloads (cannot be run in parallel!)
while true; do
read -p "Process ALL pathway data (clean, convert, validate, normalize, merge, index, blacklist, an generate archives)?" yn
	case $yn in
        [Yy]* ) 
        	sh $CPATH2_HOME/cpath2-cli.sh -premerge; 
        	sh $CPATH2_HOME/cpath2-cli.sh -merge --force; 
        	sh $CPATH2_HOME/cpath2-cli.sh -create-index; 
        	sh $CPATH2_HOME/cpath2-cli.sh -create-blacklist; 
        	sh $CPATH2_HOME/cpath2-cli.sh -create-downloads "homo sapiens,mus musculus"; 
        	break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done


# 21...
## %TODO Create database dump, validation reports, and index directory archive.


# 41...


# print all commands
#echo "INFO: all cpath2 commands:"
#sh $CPATH2_HOME/cpath2-cli.sh

