-- MySQL dump 10.11
--
-- Host: localhost    Database: cpath2
-- ------------------------------------------------------
-- Server version	5.0.81

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `l3biochemreact_delta_g`
--

LOCK TABLES `l3biochemreact_delta_g` WRITE;
/*!40000 ALTER TABLE `l3biochemreact_delta_g` DISABLE KEYS */;
INSERT INTO `l3biochemreact_delta_g` VALUES (35,42);
/*!40000 ALTER TABLE `l3biochemreact_delta_g` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biochemreact_keq`
--

LOCK TABLES `l3biochemreact_keq` WRITE;
/*!40000 ALTER TABLE `l3biochemreact_keq` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biochemreact_keq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement`
--

LOCK TABLES `l3biopaxelement` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement` DISABLE KEYS */;
INSERT INTO `l3biopaxelement` VALUES ('l3cellularlocationvocabulary',1,'http://www.biopax.org/examples/myExample#cytoplasm',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',2,'http://www.biopax.org/examples/myExample#GO_0005737',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Gene Ontology',NULL,'GO:0005737',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',3,'http://www.biopax.org/examples/myExample#ChemicalStructure_7',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C(OP(=O)(O)O)[CH]1([CH](O)[CH](O)[CH](O)[CH](O)O1)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',4,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_13',NULL,NULL,NULL,'beta-D-glucose 6-phosphate','b-D-glu-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H13O9P','260.14',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL),('l3unificationxref',5,'http://www.biopax.org/examples/myExample#KEGG_C00668',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'KEGG compound',NULL,'C00668',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',6,'http://www.biopax.org/examples/myExample#alpha-D-glucose_6-phosphate',NULL,NULL,NULL,'beta-D-glucose 6-phosphate','a-D-glu-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',7,'http://www.biopax.org/examples/myExample#aMAZE',NULL,NULL,NULL,'aMAZE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',8,'http://www.biopax.org/examples/myExample#KEGG',NULL,NULL,NULL,'KEGG','Kyoto Encyclopedia of Genes and Genomes',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3stoichiometry',9,'http://www.biopax.org/examples/myExample#Stoichiometry_52',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,6,NULL),('l3catalysis',10,'http://www.biopax.org/examples/myExample#glucokinase_converts_alpha-D-glu_to_alpha-D-glu-6-p',NULL,NULL,NULL,'catalysis of (alpha-D-glu <=> alpha-D-glu-6-p)','GLK -> (a-D-glu <=> a-D-glu-6-p)',NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',11,'http://www.biopax.org/examples/myExample#taxon_562',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'taxon',NULL,'562',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biosource',12,'http://www.biopax.org/examples/myExample#Escherichia_coli',NULL,NULL,NULL,'Escherichia coli',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3proteinreference',13,'http://www.biopax.org/examples/myExample#ProteinReference_15',NULL,NULL,NULL,'glucokinase','GLK',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MTKYALVGDVGGTNARLALCDIASGEISQAKTYSGLDYPSLEAVIRVYLEEHKVEVKDGCIAIACPITGDWVAMTNHTWAFSIAEMKKNLGFSHLEIINDFTAVSMAIPMLKKEHLIQFGGAEPVEGKPIAVYGAGTGLGVAHLVHVDKRWVSLPGEGGHVDFAPNSEEEAIILEILRAEIGHVSAERVLSGPGLVNLYRAIVKADNRLPENLKPKDITERALADSCTDCRRALSLFCVIMGRFGGNLALNLGTFGGVFIAGGIVPRFLEFFKASGFRAAFEDKGRFKEYVHDIPVYLIVHDNPGLLGSGAHLRQTLGHIL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,12,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',14,'http://www.biopax.org/examples/myExample#SwissProtTrEMBL_P46880',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'uniprot knowledge base',NULL,'P46880',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3protein',15,'http://www.biopax.org/examples/myExample#Protein_54',NULL,NULL,NULL,'glucokinase','GLK',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,13,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',16,'http://www.biopax.org/examples/myExample#SwissProtTrEMBL',NULL,NULL,NULL,'Swiss-Prot/TrEMBL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalreaction',17,'http://www.biopax.org/examples/myExample#glucokinase',NULL,NULL,NULL,'beta-D-glu + ATP => beta-D-glu-6-p + ADP','b-D-glu => b-D-glu-6-p',0,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',18,'http://www.biopax.org/examples/myExample#KEGG_R01786',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'R01786',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',19,'http://www.biopax.org/examples/myExample#ChemicalStructure_6',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'c12(n(cnc(c(N)ncn1)2)[CH]3(O[CH]([CH](O)[CH](O)3)COP(=O)(O)OP(O)(=O)O))','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',20,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_10',NULL,NULL,NULL,'Adenosine 5\'-diphosphate','ADP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C10H15N5O10P2','427.2',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,19,NULL,NULL),('l3unificationxref',21,'http://www.biopax.org/examples/myExample#KEGG_C00008',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00008',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',22,'http://www.biopax.org/examples/myExample#ADP',NULL,NULL,NULL,'Adenosine 5\'-diphosphate','ADP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,20,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',23,'http://www.biopax.org/examples/myExample#ChemicalStructure_5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C1(C(O)C(O)C(O)C(O1)CO)(O)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',24,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_12',NULL,NULL,NULL,'beta-D-glucose','b-D-glu',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H12O6','180.16',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,23,NULL,NULL),('l3unificationxref',25,'http://www.biopax.org/examples/myExample#KEGG_C00267',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00267',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',26,'http://www.biopax.org/examples/myExample#alpha-D-glucose',NULL,NULL,NULL,'beta-D-glucose','b-D-glu',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,24,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',27,'http://www.biopax.org/examples/myExample#ChemicalStructure_9',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'[CH]3(n1(c2(c(nc1)c(N)ncn2)))(O[CH]([CH](O)[CH](O)3)COP(=O)(O)OP(O)(=O)OP(O)(=O)O)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',28,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_11',NULL,NULL,NULL,'Adenosine 5\'-triphosphate','ATP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C10H16N5O13P3','507.18',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,27,NULL,NULL),('l3unificationxref',29,'http://www.biopax.org/examples/myExample#KEGG_C00002',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00002',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',30,'http://www.biopax.org/examples/myExample#ATP',NULL,NULL,NULL,'Adenosine 5\'-triphosphate','ATP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,28,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3stoichiometry',31,'http://www.biopax.org/examples/myExample#Stoichiometry_37',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,26,NULL),('l3stoichiometry',32,'http://www.biopax.org/examples/myExample#Stoichiometry_49',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,22,NULL),('l3stoichiometry',33,'http://www.biopax.org/examples/myExample#Stoichiometry_43',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,30,NULL),('l3unificationxref',34,'http://www.biopax.org/examples/myExample#KEGG_C05345',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C05345',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalreaction',35,'http://www.biopax.org/examples/myExample#phosphoglucoisomerase',NULL,NULL,NULL,'beta-D-glu-6-p <=> beta-D-fru-6-p','b-D-glu-6-p <=> b-D-fru-6-p',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',36,'http://www.biopax.org/examples/myExample#KEGG_R02740',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'R02740',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',37,'http://www.biopax.org/examples/myExample#ChemicalStructure_8',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C(OP(O)(O)=O)[CH]1([CH](O)[CH](O)C(O)(O1)CO)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',38,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_14',NULL,NULL,NULL,'beta-D-fructose 6-phosphate','b-D-fru-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H13O9P','260.14',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,37,NULL,NULL),('l3smallmolecule',39,'http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate',NULL,NULL,NULL,'beta-D-fructose 6-phosphate','b-D-fru-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,38,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3stoichiometry',40,'http://www.biopax.org/examples/myExample#Stoichiometry_57',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,39,NULL),('l3stoichiometry',41,'http://www.biopax.org/examples/myExample#Stoichiometry_58',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,6,NULL),('l3deltag',42,'http://www.biopax.org/examples/myExample#DeltaG_12',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'NaN','0.4','NaN','NaN','NaN',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3proteinreference',43,'http://www.biopax.org/examples/myExample#ProteinReference_16',NULL,NULL,NULL,'phosphoglucose isomerase','PGI',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'KTFSEAIISGEWKGYTGKAITDVVNIGIGGSDLGPYMVTEALRPYKNHLNMHFVSNVDGTHIAEVLKKVNPETTLFLVASKTFTTQETMTNAHSARDWFLKAAGDEKHVAKHFAALSTNAKAVGEFGIDTANMFEFWDWVGGRYSLWSAIGLSIVLSIGFDNFVELLSGAHAMDKHFSTTPAEKNLPVLLALIGIWYNNFFGAETEAILPYDQYMHRFAAYFQQGNMESNGKYVDRNGNVVDYQTGPIIWGEPGTNGQHAFYQLIHQGTKMVPCDFIAPAITHNPLFDHHQKLLSKFFAQTEALAFGKSREVVEQEYRDQGKDPAT',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,12,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',44,'http://www.biopax.org/examples/myExample#Swiss-ProtTrEMBL_Q9KH85',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'UniProt',NULL,'Q9KH85',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3publicationxref',45,'http://www.biopax.org/examples/myExample#PublicationXref49',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'PubMed',NULL,'2549346',NULL,NULL,-2147483648,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3protein',46,'http://www.biopax.org/examples/myExample#phosphoglucose_isomerase',NULL,NULL,NULL,'phosphoglucose isomerase','PGI',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,43,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3pathway',47,'http://www.biopax.org/examples/myExample#Pathway50',NULL,NULL,NULL,'Glycolysis Pathway','glycolysis',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,12,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3catalysis',48,'http://www.biopax.org/examples/myExample#phosphoglucose_isomerase_converts_alpha-D-gluc-6-p_to_beta-D-fruc-6-p',NULL,NULL,NULL,'catalysis of (beta-D-glu-6-p <=> beta-D-fruc-6-p)','PGI -> (b-d-glu-6-p <=> b-D-fru-6p)',NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalpathwaystep',49,'http://www.biopax.org/examples/myExample#BiochemicalPathwayStep_3',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,35,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalpathwaystep',50,'http://www.biopax.org/examples/myExample#BiochemicalPathwayStep_2',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `l3biopaxelement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_ECNumber`
--

LOCK TABLES `l3biopaxelement_ECNumber` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_ECNumber` DISABLE KEYS */;
INSERT INTO `l3biopaxelement_ECNumber` VALUES (17,'2.7.1.1'),(17,'2.7.1.2'),(35,'5.3.1.9');
/*!40000 ALTER TABLE `l3biopaxelement_ECNumber` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_author`
--

LOCK TABLES `l3biopaxelement_author` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_author` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biopaxelement_author` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_availability`
--

LOCK TABLES `l3biopaxelement_availability` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_availability` DISABLE KEYS */;
INSERT INTO `l3biopaxelement_availability` VALUES (47,'see http://www.amaze.ulb.ac.be/'),(47,'All data within the pathway has the same availability');
/*!40000 ALTER TABLE `l3biopaxelement_availability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_comment`
--

LOCK TABLES `l3biopaxelement_comment` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_comment` DISABLE KEYS */;
INSERT INTO `l3biopaxelement_comment` VALUES (1,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(2,'PMID: 11483584'),(3,'beta-glucose-6-phosphate'),(5,'PMID: 9847135'),(10,'The source of this data did not store catalyses of reactions as separate objects, so there are no unification x-refs pointing to the source of these BioPAX instances.'),(14,'PMID: 15608167'),(18,'PMID: 9847135'),(19,'ADP'),(21,'PMID: 9847135'),(23,'alpha-D-glucose'),(25,'PMID: 9847135'),(27,'ATP'),(29,'PMID: 9847135'),(34,'PMID: 9847135'),(36,'PMID: 9847135'),(37,'beta-fructose-6-phosphate'),(43,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(44,'PMID: 15608167'),(47,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(48,'The source of this data did not store catalyses of reactions as separate objects, so there are no unification x-refs pointing to the source of these BioPAX instances.');
/*!40000 ALTER TABLE `l3biopaxelement_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_deltaH_x`
--

LOCK TABLES `l3biopaxelement_deltaH_x` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_deltaH_x` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biopaxelement_deltaH_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_deltaS_x`
--

LOCK TABLES `l3biopaxelement_deltaS_x` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_deltaS_x` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biopaxelement_deltaS_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_name`
--

LOCK TABLES `l3biopaxelement_name` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_name` DISABLE KEYS */;
INSERT INTO `l3biopaxelement_name` VALUES (4,'b-D-glu-6-p'),(4,'beta-D-glucose 6-phosphate'),(6,'D-glucose-6-P'),(6,'beta-D-glucose 6-phosphate'),(6,'glucose-6-P'),(6,'beeta-D-glucose-6-p'),(6,'b-D-glucose-6-phoshate'),(6,'a-D-glu-6-p'),(7,NULL),(7,'aMAZE'),(8,'KEGG'),(8,'Kyoto Encyclopedia of Genes and Genomes'),(10,'catalysis of (alpha-D-glu <=> alpha-D-glu-6-p)'),(10,'GLK -> (a-D-glu <=> a-D-glu-6-p)'),(12,NULL),(12,'Escherichia coli'),(13,'glucose kinase'),(13,'glucokinase'),(13,'GLK'),(15,'glucokinase'),(15,'GLK'),(15,'GLK_ECOLI'),(16,NULL),(16,'Swiss-Prot/TrEMBL'),(17,'glucose ATP phosphotransferase'),(17,'beta-D-glu + ATP => beta-D-glu-6-p + ADP'),(17,'ATP:D-glucose 6-phosphotransferase'),(17,'b-D-glu => b-D-glu-6-p'),(20,'ADP'),(20,'Adenosine 5\'-diphosphate'),(20,'adenosine diphosphate'),(22,'ADP'),(22,'Adenosine 5\'-diphosphate'),(22,'adenosine diphosphate'),(24,'beta-D-glucose'),(24,'b-D-glu'),(26,'beta-D-glucose'),(26,'<FONT FACE=\"Symbol\">a</FONT>-D-glucose'),(26,'b-D-glu'),(28,'Adenosine 5\'-triphosphate'),(28,'adenosine triphosphate'),(28,'ATP'),(30,'Adenosine 5\'-triphosphate'),(30,'adenosine triphosphate'),(30,'ATP'),(35,'beta-D-Glucose 6-phosphate ketol-isomerase'),(35,'beta-D-Glucose 6-phosphate => beta-D-Fructose 6-phosphate'),(35,'beta-D-glu-6-p <=> beta-D-fru-6-p'),(35,'b-D-glu-6-p <=> b-D-fru-6-p'),(38,'b-D-fru-6-p'),(38,'beta-D-fructose 6-phosphate'),(39,'<FONT FACE=\"Symbol\">b</FONT>-D-fructose-6-phosphate'),(39,'b-D-fru-6-p'),(39,'beta-D-fructose 6-phosphate'),(43,'GPI'),(43,'phosphoglucose isomerase'),(43,'phosphohexose isomerase'),(43,'PGI'),(43,'PHI'),(43,'glucose-6-phosphate isomerase'),(46,'GPI'),(46,'phosphoglucose isomerase'),(46,'PGI'),(46,'phosphohexose isomerase'),(46,'PHI'),(46,'glucose-6-phosphate isomerase'),(47,'glycolysis'),(47,'Embden-Meyerhof pathway'),(47,'Glycolysis Pathway'),(47,'glucose degradation'),(48,'catalysis of (beta-D-glu-6-p <=> beta-D-fruc-6-p)'),(48,'PGI -> (b-d-glu-6-p <=> b-D-fru-6p)');
/*!40000 ALTER TABLE `l3biopaxelement_name` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_source`
--

LOCK TABLES `l3biopaxelement_source` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biopaxelement_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_term`
--

LOCK TABLES `l3biopaxelement_term` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_term` DISABLE KEYS */;
INSERT INTO `l3biopaxelement_term` VALUES (1,'cytoplasm');
/*!40000 ALTER TABLE `l3biopaxelement_term` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3biopaxelement_url`
--

LOCK TABLES `l3biopaxelement_url` WRITE;
/*!40000 ALTER TABLE `l3biopaxelement_url` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3biopaxelement_url` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3catalysis_cofactor`
--

LOCK TABLES `l3catalysis_cofactor` WRITE;
/*!40000 ALTER TABLE `l3catalysis_cofactor` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3catalysis_cofactor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3complex_compo_stoichiom`
--

LOCK TABLES `l3complex_compo_stoichiom` WRITE;
/*!40000 ALTER TABLE `l3complex_compo_stoichiom` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3complex_compo_stoichiom` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3complex_components`
--

LOCK TABLES `l3complex_components` WRITE;
/*!40000 ALTER TABLE `l3complex_components` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3complex_components` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3control_controlled`
--

LOCK TABLES `l3control_controlled` WRITE;
/*!40000 ALTER TABLE `l3control_controlled` DISABLE KEYS */;
INSERT INTO `l3control_controlled` VALUES (10,17),(48,35);
/*!40000 ALTER TABLE `l3control_controlled` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3control_controller`
--

LOCK TABLES `l3control_controller` WRITE;
/*!40000 ALTER TABLE `l3control_controller` DISABLE KEYS */;
INSERT INTO `l3control_controller` VALUES (10,15),(48,46);
/*!40000 ALTER TABLE `l3control_controller` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3controlledvocabulary_xref`
--

LOCK TABLES `l3controlledvocabulary_xref` WRITE;
/*!40000 ALTER TABLE `l3controlledvocabulary_xref` DISABLE KEYS */;
INSERT INTO `l3controlledvocabulary_xref` VALUES (1,2);
/*!40000 ALTER TABLE `l3controlledvocabulary_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_left_x`
--

LOCK TABLES `l3conversion_left_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_left_x` DISABLE KEYS */;
INSERT INTO `l3conversion_left_x` VALUES (17,26),(17,30),(35,6);
/*!40000 ALTER TABLE `l3conversion_left_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_part_stoich_x`
--

LOCK TABLES `l3conversion_part_stoich_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_part_stoich_x` DISABLE KEYS */;
INSERT INTO `l3conversion_part_stoich_x` VALUES (17,9),(17,31),(17,32),(17,33),(35,40),(35,41);
/*!40000 ALTER TABLE `l3conversion_part_stoich_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_right_x`
--

LOCK TABLES `l3conversion_right_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_right_x` DISABLE KEYS */;
INSERT INTO `l3conversion_right_x` VALUES (17,6),(17,22),(35,39);
/*!40000 ALTER TABLE `l3conversion_right_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entity_data_source`
--

LOCK TABLES `l3entity_data_source` WRITE;
/*!40000 ALTER TABLE `l3entity_data_source` DISABLE KEYS */;
INSERT INTO `l3entity_data_source` VALUES (6,7),(6,8),(10,7),(10,8),(15,7),(15,16),(17,7),(17,8),(22,7),(22,8),(26,7),(26,8),(30,7),(30,8),(35,7),(35,8),(39,7),(39,8),(46,7),(46,8),(47,7),(47,8),(48,7),(48,8);
/*!40000 ALTER TABLE `l3entity_data_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entity_evidence`
--

LOCK TABLES `l3entity_evidence` WRITE;
/*!40000 ALTER TABLE `l3entity_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entity_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entity_xref`
--

LOCK TABLES `l3entity_xref` WRITE;
/*!40000 ALTER TABLE `l3entity_xref` DISABLE KEYS */;
INSERT INTO `l3entity_xref` VALUES (15,14),(17,18),(22,21),(26,25),(30,29),(35,36),(39,34),(46,44),(47,45);
/*!40000 ALTER TABLE `l3entity_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityfeature_evidence`
--

LOCK TABLES `l3entityfeature_evidence` WRITE;
/*!40000 ALTER TABLE `l3entityfeature_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityfeature_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityfeature_feature_loc`
--

LOCK TABLES `l3entityfeature_feature_loc` WRITE;
/*!40000 ALTER TABLE `l3entityfeature_feature_loc` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityfeature_feature_loc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityfeature_member_feature`
--

LOCK TABLES `l3entityfeature_member_feature` WRITE;
/*!40000 ALTER TABLE `l3entityfeature_member_feature` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityfeature_member_feature` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityref_entity_feature`
--

LOCK TABLES `l3entityref_entity_feature` WRITE;
/*!40000 ALTER TABLE `l3entityref_entity_feature` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityref_entity_feature` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityref_entity_ref_type`
--

LOCK TABLES `l3entityref_entity_ref_type` WRITE;
/*!40000 ALTER TABLE `l3entityref_entity_ref_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityref_entity_ref_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityref_evidence`
--

LOCK TABLES `l3entityref_evidence` WRITE;
/*!40000 ALTER TABLE `l3entityref_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityref_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityref_member_entity_ref`
--

LOCK TABLES `l3entityref_member_entity_ref` WRITE;
/*!40000 ALTER TABLE `l3entityref_member_entity_ref` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3entityref_member_entity_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entityref_xref`
--

LOCK TABLES `l3entityref_xref` WRITE;
/*!40000 ALTER TABLE `l3entityref_xref` DISABLE KEYS */;
INSERT INTO `l3entityref_xref` VALUES (4,5),(13,14),(20,21),(24,25),(28,29),(38,34),(43,44);
/*!40000 ALTER TABLE `l3entityref_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3evidence_confidence`
--

LOCK TABLES `l3evidence_confidence` WRITE;
/*!40000 ALTER TABLE `l3evidence_confidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3evidence_confidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3evidence_evidence_code`
--

LOCK TABLES `l3evidence_evidence_code` WRITE;
/*!40000 ALTER TABLE `l3evidence_evidence_code` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3evidence_evidence_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3evidence_experimental_form`
--

LOCK TABLES `l3evidence_experimental_form` WRITE;
/*!40000 ALTER TABLE `l3evidence_experimental_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3evidence_experimental_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3evidence_xref`
--

LOCK TABLES `l3evidence_xref` WRITE;
/*!40000 ALTER TABLE `l3evidence_xref` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3evidence_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3expform_exp_feture`
--

LOCK TABLES `l3expform_exp_feture` WRITE;
/*!40000 ALTER TABLE `l3expform_exp_feture` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3expform_exp_feture` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3expform_exp_form_desc`
--

LOCK TABLES `l3expform_exp_form_desc` WRITE;
/*!40000 ALTER TABLE `l3expform_exp_form_desc` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3expform_exp_form_desc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3interaction_interact_type_x`
--

LOCK TABLES `l3interaction_interact_type_x` WRITE;
/*!40000 ALTER TABLE `l3interaction_interact_type_x` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3interaction_interact_type_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3interaction_participants_x`
--

LOCK TABLES `l3interaction_participants_x` WRITE;
/*!40000 ALTER TABLE `l3interaction_participants_x` DISABLE KEYS */;
INSERT INTO `l3interaction_participants_x` VALUES (10,15),(10,17),(17,6),(17,22),(17,26),(17,30),(35,6),(35,39),(48,35),(48,46);
/*!40000 ALTER TABLE `l3interaction_participants_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathway_pathway_component`
--

LOCK TABLES `l3pathway_pathway_component` WRITE;
/*!40000 ALTER TABLE `l3pathway_pathway_component` DISABLE KEYS */;
INSERT INTO `l3pathway_pathway_component` VALUES (47,17),(47,48);
/*!40000 ALTER TABLE `l3pathway_pathway_component` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathway_pathway_order`
--

LOCK TABLES `l3pathway_pathway_order` WRITE;
/*!40000 ALTER TABLE `l3pathway_pathway_order` DISABLE KEYS */;
INSERT INTO `l3pathway_pathway_order` VALUES (47,49),(47,50);
/*!40000 ALTER TABLE `l3pathway_pathway_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathwaystep_evidence`
--

LOCK TABLES `l3pathwaystep_evidence` WRITE;
/*!40000 ALTER TABLE `l3pathwaystep_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3pathwaystep_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathwaystep_next_step`
--

LOCK TABLES `l3pathwaystep_next_step` WRITE;
/*!40000 ALTER TABLE `l3pathwaystep_next_step` DISABLE KEYS */;
INSERT INTO `l3pathwaystep_next_step` VALUES (50,49);
/*!40000 ALTER TABLE `l3pathwaystep_next_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathwaystep_step_process`
--

LOCK TABLES `l3pathwaystep_step_process` WRITE;
/*!40000 ALTER TABLE `l3pathwaystep_step_process` DISABLE KEYS */;
INSERT INTO `l3pathwaystep_step_process` VALUES (49,48),(50,10);
/*!40000 ALTER TABLE `l3pathwaystep_step_process` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3physicalentity_member_pe`
--

LOCK TABLES `l3physicalentity_member_pe` WRITE;
/*!40000 ALTER TABLE `l3physicalentity_member_pe` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3physicalentity_member_pe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3physicalentity_mod_at`
--

LOCK TABLES `l3physicalentity_mod_at` WRITE;
/*!40000 ALTER TABLE `l3physicalentity_mod_at` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3physicalentity_mod_at` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3physicalentity_not_mod_at`
--

LOCK TABLES `l3physicalentity_not_mod_at` WRITE;
/*!40000 ALTER TABLE `l3physicalentity_not_mod_at` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3physicalentity_not_mod_at` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3score_xref`
--

LOCK TABLES `l3score_xref` WRITE;
/*!40000 ALTER TABLE `l3score_xref` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3score_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3seqloc_region_type`
--

LOCK TABLES `l3seqloc_region_type` WRITE;
/*!40000 ALTER TABLE `l3seqloc_region_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3seqloc_region_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3tempreact_product`
--

LOCK TABLES `l3tempreact_product` WRITE;
/*!40000 ALTER TABLE `l3tempreact_product` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3tempreact_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3tempreact_regulatory_element`
--

LOCK TABLES `l3tempreact_regulatory_element` WRITE;
/*!40000 ALTER TABLE `l3tempreact_regulatory_element` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3tempreact_regulatory_element` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-04-05 21:30:11
