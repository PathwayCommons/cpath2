#!/bin/sh
# Converts the BioPAX data to other formats and summaries (requires blacklist.txt and paxtools.jar be in ./downloads)
echo "export.sh started..."
cd ./downloads

$JAVA_HOME/bin/java -Xmx64g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true -jar paxtools.jar toGSEA 'pc-biopax.owl.gz' 'pc-hgnc.gmt' 'hgnc.symbol' 'organisms=9606'

$JAVA_HOME/bin/java -Xmx64g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true -jar paxtools.jar toSIF 'pc-biopax.owl.gz' 'pc-hgnc.txt' seqDb=hgnc -extended -andSif exclude=neighbor_of

$JAVA_HOME/bin/java -Xmx64g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true -jar paxtools.jar summarize 'pc-biopax.owl.gz' 'pathways.txt' --pathways

$JAVA_HOME/bin/java -Xmx64g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true -jar paxtools.jar summarize 'pc-biopax.owl.gz' 'physical_entities.json' --uri-ids

cat physical_entities.json | jq -cS 'map(select(.generic)) | reduce .[] as $o ({}; . + {($o.uri): {name: $o.name, label:$o.label, synonyms:$o."hgnc.symbol"}})' > generic-physical-entity-map.json

rename 's/txt\.sif/sif/' *.txt.sif
#compress the generated files except: blacklist.txt, generic-physical-entity-map.json
gzip pc-*.sif pc-*.gmt pc-*.txt datasources.txt pathways.txt uniprot.txt physical_entities.json

cd ..
echo "export.sh completed."