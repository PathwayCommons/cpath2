# cPath2: pathway data builder and server.

-  [![Build Status](https://travis-ci.org/PathwayCommons/cpath2.svg?branch=master)](https://travis-ci.org/PathwayCommons/cpath2) 
-  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/c9722bf60f714e87a7137ff2f2586926)](https://www.codacy.com/app/IgorRodchenkov/cpath2?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=PathwayCommons/cpath2&amp;utm_campaign=Badge_Grade)

cPath2 is a biological pathway data integration and query platform 
for [Pathway Commons](http://www.pathwaycommons.org) (PC) projects.

Pathway Commons is a convenient point of access to biological pathway 
information and analysis methods collected from several curated public 
pathway and interaction databases. Our effort builds on the diverse and 
active community of pathway databases, the development in this community 
of a common language for biological pathway data exchange 
([BioPAX](http://www.biopax.org)) and work in the C. Sander and G. Bader 
groups since late 2002 in building infrastructure for managing, integrating, 
and analyzing biological pathway information. 
 
BioPAX is a standard language that enables integration, exchange, 
visualization and complex analysis of biological pathway data. Specifically, 
BioPAX supports data exchange between pathway data groups and thus reduces 
the complexity of interchange between data formats by providing an accepted 
standard format for pathway data.

Our long-term vision is to achieve a complete computable map of the cell across all species and 
conditions. We aim to provide for the efficient exchange of pathway data; aggregation and 
integration of pathways from major available sources into a shared public information store;
distribution in a value-added, standardized form; availability to the end-user via advanced 
internet web service technology; and dissemination of the technology of biological knowledge 
representation and pathway analysis by collaboration and training.

  
## Credits ###
[![IntelliJ IDEA](http://imagej.net/_images/thumb/1/1b/Intellij-idea.png/97px-Intellij-idea.png)](http://www.jetbrains.com/idea)

**Thanks**: to many open source projects and groups, such as Apache, Eclipse, Spring, etc., and, of course, - to GitHub.

**Funding**: "Pathway Commons: Research Resource for Biological Pathways"; Chris Sander, Gary Bader; 1U41HG006623 (formerly NIH P41HG04118)

## Build and try the demo app

Check out this project and switch to the project directory (cpath2) and build with maven:

    mvn clean install

which takes some time (minutes, due to the ontology files get parsed and integration tests are run) and results in a new cpath2.jar created in the target folder and demo data in the target/work folder (`mvn package` would not do). Once complete, start the default (CPATH2_HOME="target/work") test/demo cpath2 server from the cpath2 project directory and go to `http://localhost:8080`:

    java -jar target/cpath2.war -s
    
and watch the cpath2.log to see the app starts and prints "Working ('home') directory: target/work", then "Loading the BioPAX Model from target/work/downloads/PathwayCommonsDemo11.All.BIOPAX.owl.gz", and that the "model is ready", etc. Ignore the warnings for now, e.g., about blacklist.txt was not found, for it's not needed for demo and normally generated during the cpath2 "premerge" step, after index gets created.) Expect that most queries or example links won't return any result as there are not much data in the default instance; try to find e.g. all the pathways there with `http://localhost:8080/search.json?q=*&type=pathway` (remove `type` parameter to list all the objects; remove `.json` to get XML back).

## Configuration

### Working directory

Directory 'work' is where the configuration, data files and indices are saved.  

    cd work

The directory may contain: 
- application.properties (to configure various server and model options);
- metadata.json (describes the bio data to be imported/integrated);
- data/ directory (where original and intermediate data are stored);
- index/ (Lucene index directory for the final BioPAX model);
- downloads/ (where blacklist.txt and all the output data archives are created);
- logback.xml (custom logging can be enabled by -Dlogback.configurationFile=logback.xml JVM option);

To see available commands and options, run: 

    bash cpath2.sh

In order to create a new cpath2 instance, define or update the metadata.json, 
prepare input data archives (see below how), and run 

    bash cpath2.sh --build

, which normally takes a day or two - executes the following data integration steps: 
import the metadata, clean, convert to BioPAX, normalize the data, build the BioPAX warehouse, 
merge into the main BioPAX model, create Lucene index, several summary files and `export.sh` script to convert 
the final BioPAX models to SIF, GMT, TXT formats. 

    cd downloads
    
Copy the latest paxtools.jar into this current directory and run -

    sh export.sh 2>&1 >console.out & 
    
(- which takes overnight or a day and night); upload/copy/move (but keep at least blacklist.txt, *All.BIOPAX.owl.gz)
all the files from this here and ../data/ directories to the file server, or configure so that they can be downloaded from e.g. `http://www.pathwaycommons.org/archives/PC2/v{version_number}` (or else).

Once the instance is configured and data processed, run the web service using the same 
script as follows:

    nohup bash cpath2.sh --server 2>&1 &

(watch the `nohup.out` and `cpath2.log`)

### Metadata

The cPath2 metadata configuration file is a JSON file (default is `metadata.json`).
 
The data Converter and Cleaner classes are not required to be implemented in the cpath2 project sources. 
It's possible to plug into the data build stage external 
cleaner/converter classes after the cpath2 JAR is released:
simply include to the cpath2.sh Java options like:
 `-cp /path-to/MyDataCleanerImpl.class;/path-to/MyDataConverterImpl.class` 

### Data

One (a data manager) has to find, download, re-pack (zip) and put original 
biological pathway data files to the data/ sub-directory.

#### Warehouse data

Download UniProt, ChEBI, id-mapping tables into the data/ directory, e.g.:
 - `wget ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz`
 - `wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi.obo`
 - etc...

Re-pack/rename the data, i.e., make uniprot_human.zip, chebi.zip, etc.  
Filenames must be the corresponding metadata identifier, as in metadata.json; 
only ZIP archive (can contain multiple files) is currently supported by cPath2 data importer 
(not .zip or no extension means the data will be read as is).

#### Pathway data 

Note: BioPAX L1, L2 models will be auto-converted to the BioPAX Level3. 

Prepare original BioPAX and PSI-MI/PSI-MITAB data archives in the 'data' folder as follows:
 - download (wget) original files or archives from the pathway resource (e.g., `wget http://www.reactome.org/download/current/biopax3.zip`) 
 - extract what you need (e.g. some species data only)
 - create a new zip archive using name like `<IDENTIFIER>.zip` (datasource identifier, e.g., `reactome_human.zip`).
