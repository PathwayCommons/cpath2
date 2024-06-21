#!/bin/sh
##
# Get each pathway from PC and convert to SBGN 
# if there are processes (excluding sub-pathways and their child processes).
##

PC2="http://www.pathwaycommons.org/pc2"
echo "Using $PC2"
page=0
while [ $page -lt 63 ]
do
  echo "\nSEARCH PAGE $page"
  curl -sS -H "accept: application/json" "$PC2/search?q=*&type=pathway&page=$page" | jq -r '.searchHit[].uri' | while read uri ; do
    name=$(echo "$uri" | sed -e 's/[^A-Za-z0-9._-]/_/g')
    # skip trivial pathways
    EMPTY=$(curl -sS -H "accept: application/json" "$PC2/traverse?path=Pathway/pathwayComponent:Interaction&uri=$uri" | jq -r '.empty')
    if [ "$EMPTY" = "true" ] ; then
        EMPTY=$(curl -sS -H "accept: application/json" "$PC2/traverse?path=Pathway/pathwayOrder/stepProcess:Interaction&uri=$uri" | jq -r '.empty')
        if [ "$EMPTY" = "true" ] ; then
    	    echo "SKIPPED $uri (no Interactions)"
        fi
    fi
    ERR=$(curl -sS "$PC2/get?uri=$uri" > owl)
    case "$ERR" in 
      *Exception* ) echo "FAILED $uri $ERR" ; continue ;;
    esac
    ERR=$(java -Xmx32g -jar paxtools.jar toSBGN owl $name.xml $* 2>&1 >>paxtools.log)
    case "$ERR" in 
      *Exception* ) echo "FAILED $uri $ERR" ;;
      * ) echo "SAVED $uri" ;;
    esac
#break
  done
  page=$((page+1))
#break
done

echo "\nAll done!"

