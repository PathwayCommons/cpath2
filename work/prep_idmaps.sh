#!/bin/bash

##
# Creates an archive of additional id-mapping files 
# - ready for importing into PC2 (cpath2) warehouse.
# Usage: no parameters.
##

# download ChEMBL to ChEBI mapping file from UniChem
IN="src1src7.txt.gz"
if [ ! -f "$IN" ]; then
  wget ftp://ftp.ebi.ac.uk/pub/databases/chembl/UniChem/data/wholeSourceMapping/src_id1/$IN
fi
# write to
OUT=chembl-chebi.unichem.txt
# write title line
echo "ChEMBL	ChEBI" > "$OUT"
# read from the second line
gzip -cd $IN | sed 1d | while read ID1 ID2; do
  echo "$ID1	$ID2" >> "$OUT"
done

# download ChEBI to PharmGKB Drug mapping file
IN="src7src17.txt.gz"
if [ ! -f "$IN" ]; then
  wget ftp://ftp.ebi.ac.uk/pub/databases/chembl/UniChem/data/wholeSourceMapping/src_id7/$IN
fi
# write to
OUT=pharmgkbdrug-chebi.unichem.txt
# write title line
echo "PharmGKB Drug	ChEBI" > "$OUT"
# read from the second line
gzip -cd $IN | sed 1d | while read ID1 ID2; do
  # swap columns
  echo "$ID2	$ID1" >> "$OUT"
done

# download ChEBI to PubChem Compound mapping file
IN="src7src22.txt.gz"
if [ ! -f "$IN" ]; then
  wget ftp://ftp.ebi.ac.uk/pub/databases/chembl/UniChem/data/wholeSourceMapping/src_id7/$IN
fi
# write to
OUT=pubchemcid-chebi.unichem.txt
# write title line
echo "PubChem-compound	ChEBI" > "$OUT"
# read from the second line
gzip -cd $IN | sed 1d | while read ID1 ID2; do
  # swap columns
  echo "$ID2	$ID1" >> "$OUT"
done

# zip, cleanup
zip unichem_mapping.zip *.unichem.txt
rm *.unichem.txt
rm src*.txt.gz
echo "done."
