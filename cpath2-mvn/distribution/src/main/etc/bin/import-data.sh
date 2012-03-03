#!/bin/sh

# This is going to be a powerful but experimental script!
# TODO Use Python instead???

# Create new cPath2 databases, import all data, build full-text index, 
# summarize, organize, and backup.
#
# Before you run this script, please define CPATH2_HOME; configure connection 
# and logging properties, metadata table in $CPATH2_HOME/metadata.conf; 
# download, repackage and properly name all the data archives to $CPATH2_HOME/tmp!
# See also: README.TXT.

## Options: 
# --quiet (no questions, no interactive shell), 
# --force (ignore validation errors), 
# --no-backup (no db/index archives at the end), 
# --no-export

## Execute the following stages using cpath-admin.sh script:

# 1. Drop/Create cPath2 databases (double-check db connection and names in $CPATH2_HOME/cpath.properties!)

# 2. Fetch Metadata (parse $CPATH2_HOME/metadata.conf)

# 3. Fetch Warehouse data (also converts to BioPAX and creates small molecule and protein warehouses)

# 4. Fetch Pathway Data (BioPAX, PSI-MI; unpack, persist the original unchanged content)

# 5. Pre-merge (clean, convert, validate, normalize pathway data)

# 6. Merge (merge normalized BioPAX networks with the warehouse, validate again (optionally), and merge into the main storage/network)

# 7. Index


## [TODO] Export batch download and summary data 
# (in $CPATH2_HOME/tmp/*: summary, datasource/, datasource/reactome.owl, organism/, organism/9606.sif, etc...)

# 21...

## [TODO] Create database dump, validation reports, and index directory archive.

# 41...


Done.