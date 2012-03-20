#!/bin/sh

# This is going to be a powerful script.
# TODO Use Python instead?

# Create new cPath2 databases, import all data, build full-text index, 
# summarize, organize, and backup.
#
# Before you run this script, please define CPATH2_HOME; configure connection 
# and logging properties, metadata table in $CPATH2_HOME/metadata.conf; 
# download, repackage and properly name all the data archives to $CPATH2_HOME/tmp!
# See also: README.TXT.

## TODO Options: 
# --quiet (no questions, no interactive shell), 
# --force (ignore validation errors), 
# --no-backup (no db/index archives at the end), 
# --no-export

echo "CPATH2 DATA IMPORT"

## Execute the following stages using cpath-admin.sh script:
echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
metadatadb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'metadata.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
maindb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'main.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
moleculesdb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'molecules.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
proteinsdb=`sed '/^\#/d' $CPATH2_HOME/cpath.properties | grep 'proteins.db'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "CPATH2 Properities:"
echo "       metadata.db=$metadatadb"
echo "       main.db=$maindb"
echo "       molecules.db=$moleculesdb"
echo "       proteins.db=$proteinsdb"
echo "       xml.base=$xmlbase"

# 1. Drop/Create cPath2 databases (double-check db connection and names in $CPATH2_HOME/cpath.properties!)
while true; do
read -p "Create/replace $metadatadb database?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -create-tables $metadatadb; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done
while true; do
read -p "Create/replace $maindb database?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -create-tables $maindb; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

while true; do  
read -p "Do you also want to replace warehouse databases?" yn
case $yn 
	in  [Yy]* )  
 while true; do
 read -p "Create/replace $moleculesdb database?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -create-tables $moleculesdb; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
 done;
 while true; do
 read -p "Create/replace $proteinsdb database?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -create-tables $proteinsdb; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
 done; 
 break;;
	[Nn]* )  break;;
	* ) echo "Please answer yes or no.";;
esac
done

# 2. Fetch Metadata (parse $CPATH2_HOME/metadata.conf)
while true; do
read -p "Fetch/update metadata using $CPATH2_HOME/metadata.conf?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -fetch-metadata file://$CPATH2_HOME/metadata.conf; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 3. Fetch Warehouse data (also converts to BioPAX and creates small molecule and protein warehouses)
# for now - do this in the step #4, altogether

# 4. Fetch Pathway Data (BioPAX, PSI-MI; unpack, persist the original unchanged content)
while true; do
read -p "Fetch/convert all data?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -fetch-data --all; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 5. Pre-merge (clean, convert, validate, normalize pathway data)
while true; do
read -p "Premerge (convert/validate/normalize) all?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -premerge; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

# 6. Merge (merge normalized BioPAX networks with the warehouse, validate again (optionally), and merge into the main storage/network)
while true; do
read -p "Merge all?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -merge; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done


# 7. Index
while true; do
read -p "Create "main" full-text index?" yn
	case $yn in
        [Yy]* ) sh $CPATH2_HOME/cpath-admin.sh -create-index main; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
echo "Indexing ended."
done

## [TODO] Export batch download and summary data 
# (in $CPATH2_HOME/tmp/: summary, datasource/, datasource/reactome.owl.zip, organism/, 
# organism/9606.sif.zip, etc... $CPATH2_HOME/tmp/datasource/* will be then exposed by 
# cPath2 batch download web service)

# 21...

## [TODO] Create database dump, validation reports, and index directory archive.

# 41...

