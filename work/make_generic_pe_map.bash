# After the PC data build is done,
# generates generic-physical-entity-map.json (for PC app-ui webapp).
#
# Prerequisites:
#  - gunzip
#  - jq (https://stedolan.github.io/jq/; or install with npm: npm install hjson -g)
gunzip -c "file:downloads/physical_entities.json.gz" | jq -cS 'map(select(.generic)) | reduce .[] as $o ({}; . + {($o.uri): {name: $o.name, label:$o.label, synonyms:$o."hgnc.symbol"}})' > downloads/generic-physical-entity-map.json
echo "Generated downloads/generic-physical-entity-map.json"
