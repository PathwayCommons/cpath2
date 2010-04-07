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
INSERT INTO `l3biochemreact_delta_g` VALUES (39,43);
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
INSERT INTO `l3control_controlled` VALUES (35,10),(48,39);
/*!40000 ALTER TABLE `l3control_controlled` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3control_controller`
--

LOCK TABLES `l3control_controller` WRITE;
/*!40000 ALTER TABLE `l3control_controller` DISABLE KEYS */;
INSERT INTO `l3control_controller` VALUES (35,38),(48,46);
/*!40000 ALTER TABLE `l3control_controller` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3controlledvocabulary_xref`
--

LOCK TABLES `l3controlledvocabulary_xref` WRITE;
/*!40000 ALTER TABLE `l3controlledvocabulary_xref` DISABLE KEYS */;
INSERT INTO `l3controlledvocabulary_xref` VALUES (11,12);
/*!40000 ALTER TABLE `l3controlledvocabulary_xref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_left_x`
--

LOCK TABLES `l3conversion_left_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_left_x` DISABLE KEYS */;
INSERT INTO `l3conversion_left_x` VALUES (10,24),(10,27),(39,16);
/*!40000 ALTER TABLE `l3conversion_left_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_part_stoich_x`
--

LOCK TABLES `l3conversion_part_stoich_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_part_stoich_x` DISABLE KEYS */;
INSERT INTO `l3conversion_part_stoich_x` VALUES (10,28),(10,29),(10,30),(10,31),(39,41),(39,42);
/*!40000 ALTER TABLE `l3conversion_part_stoich_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3conversion_right_x`
--

LOCK TABLES `l3conversion_right_x` WRITE;
/*!40000 ALTER TABLE `l3conversion_right_x` DISABLE KEYS */;
INSERT INTO `l3conversion_right_x` VALUES (10,16),(10,20),(39,33);
/*!40000 ALTER TABLE `l3conversion_right_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element`
--

LOCK TABLES `l3element` WRITE;
/*!40000 ALTER TABLE `l3element` DISABLE KEYS */;
INSERT INTO `l3element` VALUES ('l3unificationxref',1,'http://www.biopax.org/examples/myExample#KEGG_C05345',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C05345',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',2,'http://www.biopax.org/examples/myExample#ChemicalStructure_9',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'[CH]3(n1(c2(c(nc1)c(N)ncn2)))(O[CH]([CH](O)[CH](O)3)COP(=O)(O)OP(O)(=O)OP(O)(=O)O)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',3,'http://www.biopax.org/examples/myExample#ChemicalStructure_8',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C(OP(O)(O)=O)[CH]1([CH](O)[CH](O)C(O)(O1)CO)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',4,'http://www.biopax.org/examples/myExample#KEGG',NULL,NULL,NULL,'KEGG','Kyoto Encyclopedia of Genes and Genomes',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',5,'http://www.biopax.org/examples/myExample#aMAZE',NULL,NULL,NULL,'aMAZE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3provenance',6,'http://www.biopax.org/examples/myExample#SwissProtTrEMBL',NULL,NULL,NULL,'Swiss-Prot/TrEMBL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',7,'http://www.biopax.org/examples/myExample#KEGG_R01786',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'R01786',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',8,'http://www.biopax.org/examples/myExample#SwissProtTrEMBL_P46880',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'uniprot knowledge base',NULL,'P46880',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',9,'http://www.biopax.org/examples/myExample#taxon_562',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'taxon',NULL,'562',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalreaction',10,'http://www.biopax.org/examples/myExample#glucokinase',NULL,NULL,NULL,'beta-D-glu + ATP => beta-D-glu-6-p + ADP','b-D-glu => b-D-glu-6-p',0,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3cellularlocationvocabulary',11,'http://www.biopax.org/examples/myExample#cytoplasm',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',12,'http://www.biopax.org/examples/myExample#GO_0005737',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Gene Ontology',NULL,'GO:0005737',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',13,'http://www.biopax.org/examples/myExample#ChemicalStructure_7',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C(OP(=O)(O)O)[CH]1([CH](O)[CH](O)[CH](O)[CH](O)O1)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',14,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_13',NULL,NULL,NULL,'beta-D-glucose 6-phosphate','b-D-glu-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H13O9P','260.14',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,13,NULL,NULL),('l3unificationxref',15,'http://www.biopax.org/examples/myExample#KEGG_C00668',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'KEGG compound',NULL,'C00668',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',16,'http://www.biopax.org/examples/myExample#alpha-D-glucose_6-phosphate',NULL,NULL,NULL,'beta-D-glucose 6-phosphate','a-D-glu-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,14,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',17,'http://www.biopax.org/examples/myExample#ChemicalStructure_6',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'c12(n(cnc(c(N)ncn1)2)[CH]3(O[CH]([CH](O)[CH](O)3)COP(=O)(O)OP(O)(=O)O))','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',18,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_10',NULL,NULL,NULL,'Adenosine 5\'-diphosphate','ADP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C10H15N5O10P2','427.2',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,17,NULL,NULL),('l3unificationxref',19,'http://www.biopax.org/examples/myExample#KEGG_C00008',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00008',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',20,'http://www.biopax.org/examples/myExample#ADP',NULL,NULL,NULL,'Adenosine 5\'-diphosphate','ADP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,18,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3chemicalstructure',21,'http://www.biopax.org/examples/myExample#ChemicalStructure_5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C1(C(O)C(O)C(O)C(O1)CO)(O)','SMILES',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',22,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_12',NULL,NULL,NULL,'beta-D-glucose','b-D-glu',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H12O6','180.16',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,21,NULL,NULL),('l3unificationxref',23,'http://www.biopax.org/examples/myExample#KEGG_C00267',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00267',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',24,'http://www.biopax.org/examples/myExample#alpha-D-glucose',NULL,NULL,NULL,'beta-D-glucose','b-D-glu',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,22,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmoleculereference',25,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_11',NULL,NULL,NULL,'Adenosine 5\'-triphosphate','ATP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C10H16N5O13P3','507.18',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL),('l3unificationxref',26,'http://www.biopax.org/examples/myExample#KEGG_C00002',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'C00002',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3smallmolecule',27,'http://www.biopax.org/examples/myExample#ATP',NULL,NULL,NULL,'Adenosine 5\'-triphosphate','ATP',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,25,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3stoichiometry',28,'http://www.biopax.org/examples/myExample#Stoichiometry_37',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,24,NULL),('l3stoichiometry',29,'http://www.biopax.org/examples/myExample#Stoichiometry_49',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,20,NULL),('l3stoichiometry',30,'http://www.biopax.org/examples/myExample#Stoichiometry_43',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,27,NULL),('l3stoichiometry',31,'http://www.biopax.org/examples/myExample#Stoichiometry_52',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,16,NULL),('l3smallmoleculereference',32,'http://www.biopax.org/examples/myExample#SmallMoleculeReference_14',NULL,NULL,NULL,'beta-D-fructose 6-phosphate','b-D-fru-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'C6H13O9P','260.14',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL),('l3smallmolecule',33,'http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate',NULL,NULL,NULL,'beta-D-fructose 6-phosphate','b-D-fru-6-p',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,32,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',34,'http://www.biopax.org/examples/myExample#Swiss-ProtTrEMBL_Q9KH85',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'UniProt',NULL,'Q9KH85',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3catalysis',35,'http://www.biopax.org/examples/myExample#glucokinase_converts_alpha-D-glu_to_alpha-D-glu-6-p',NULL,NULL,NULL,'catalysis of (alpha-D-glu <=> alpha-D-glu-6-p)','GLK -> (a-D-glu <=> a-D-glu-6-p)',NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biosource',36,'http://www.biopax.org/examples/myExample#Escherichia_coli',NULL,NULL,NULL,'Escherichia coli',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3proteinreference',37,'http://www.biopax.org/examples/myExample#ProteinReference_15',NULL,NULL,NULL,'glucokinase','GLK',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MTKYALVGDVGGTNARLALCDIASGEISQAKTYSGLDYPSLEAVIRVYLEEHKVEVKDGCIAIACPITGDWVAMTNHTWAFSIAEMKKNLGFSHLEIINDFTAVSMAIPMLKKEHLIQFGGAEPVEGKPIAVYGAGTGLGVAHLVHVDKRWVSLPGEGGHVDFAPNSEEEAIILEILRAEIGHVSAERVLSGPGLVNLYRAIVKADNRLPENLKPKDITERALADSCTDCRRALSLFCVIMGRFGGNLALNLGTFGGVFIAGGIVPRFLEFFKASGFRAAFEDKGRFKEYVHDIPVYLIVHDNPGLLGSGAHLRQTLGHIL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,36,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3protein',38,'http://www.biopax.org/examples/myExample#Protein_54',NULL,NULL,NULL,'glucokinase','GLK',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,37,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalreaction',39,'http://www.biopax.org/examples/myExample#phosphoglucoisomerase',NULL,NULL,NULL,'beta-D-glu-6-p <=> beta-D-fru-6-p','b-D-glu-6-p <=> b-D-fru-6-p',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3unificationxref',40,'http://www.biopax.org/examples/myExample#KEGG_R02740',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'kegg',NULL,'R02740',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3stoichiometry',41,'http://www.biopax.org/examples/myExample#Stoichiometry_57',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,33,NULL),('l3stoichiometry',42,'http://www.biopax.org/examples/myExample#Stoichiometry_58',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1.0',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,16,NULL),('l3deltag',43,'http://www.biopax.org/examples/myExample#DeltaG_12',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'NaN','0.4','NaN','NaN','NaN',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3proteinreference',44,'http://www.biopax.org/examples/myExample#ProteinReference_16',NULL,NULL,NULL,'phosphoglucose isomerase','PGI',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'KTFSEAIISGEWKGYTGKAITDVVNIGIGGSDLGPYMVTEALRPYKNHLNMHFVSNVDGTHIAEVLKKVNPETTLFLVASKTFTTQETMTNAHSARDWFLKAAGDEKHVAKHFAALSTNAKAVGEFGIDTANMFEFWDWVGGRYSLWSAIGLSIVLSIGFDNFVELLSGAHAMDKHFSTTPAEKNLPVLLALIGIWYNNFFGAETEAILPYDQYMHRFAAYFQQGNMESNGKYVDRNGNVVDYQTGPIIWGEPGTNGQHAFYQLIHQGTKMVPCDFIAPAITHNPLFDHHQKLLSKFFAQTEALAFGKSREVVEQEYRDQGKDPAT',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,36,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3publicationxref',45,'http://www.biopax.org/examples/myExample#PublicationXref49',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'PubMed',NULL,'2549346',NULL,NULL,-2147483648,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3protein',46,'http://www.biopax.org/examples/myExample#phosphoglucose_isomerase',NULL,NULL,NULL,'phosphoglucose isomerase','PGI',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,11,NULL,44,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3pathway',47,'http://www.biopax.org/examples/myExample#Pathway50',NULL,NULL,NULL,'Glycolysis Pathway','glycolysis',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,36,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3catalysis',48,'http://www.biopax.org/examples/myExample#phosphoglucose_isomerase_converts_alpha-D-gluc-6-p_to_beta-D-fruc-6-p',NULL,NULL,NULL,'catalysis of (beta-D-glu-6-p <=> beta-D-fruc-6-p)','PGI -> (b-d-glu-6-p <=> b-D-fru-6p)',NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalpathwaystep',49,'http://www.biopax.org/examples/myExample#BiochemicalPathwayStep_3',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,39,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('l3biochemicalpathwaystep',50,'http://www.biopax.org/examples/myExample#BiochemicalPathwayStep_2',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,10,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `l3element` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_ECNumber`
--

LOCK TABLES `l3element_ECNumber` WRITE;
/*!40000 ALTER TABLE `l3element_ECNumber` DISABLE KEYS */;
INSERT INTO `l3element_ECNumber` VALUES (10,'2.7.1.1'),(10,'2.7.1.2'),(39,'5.3.1.9');
/*!40000 ALTER TABLE `l3element_ECNumber` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_author`
--

LOCK TABLES `l3element_author` WRITE;
/*!40000 ALTER TABLE `l3element_author` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3element_author` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_availability`
--

LOCK TABLES `l3element_availability` WRITE;
/*!40000 ALTER TABLE `l3element_availability` DISABLE KEYS */;
INSERT INTO `l3element_availability` VALUES (47,'see http://www.amaze.ulb.ac.be/'),(47,'All data within the pathway has the same availability');
/*!40000 ALTER TABLE `l3element_availability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_comment`
--

LOCK TABLES `l3element_comment` WRITE;
/*!40000 ALTER TABLE `l3element_comment` DISABLE KEYS */;
INSERT INTO `l3element_comment` VALUES (1,'PMID: 9847135'),(2,'ATP'),(3,'beta-fructose-6-phosphate'),(7,'PMID: 9847135'),(8,'PMID: 15608167'),(11,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(12,'PMID: 11483584'),(13,'beta-glucose-6-phosphate'),(15,'PMID: 9847135'),(17,'ADP'),(19,'PMID: 9847135'),(21,'alpha-D-glucose'),(23,'PMID: 9847135'),(26,'PMID: 9847135'),(34,'PMID: 15608167'),(35,'The source of this data did not store catalyses of reactions as separate objects, so there are no unification x-refs pointing to the source of these BioPAX instances.'),(40,'PMID: 9847135'),(44,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(47,'This example is meant to provide an illustration of how various BioPAX slots should be filled; it is not intended to provide useful (or even accurate) biological information'),(48,'The source of this data did not store catalyses of reactions as separate objects, so there are no unification x-refs pointing to the source of these BioPAX instances.');
/*!40000 ALTER TABLE `l3element_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_deltaH_x`
--

LOCK TABLES `l3element_deltaH_x` WRITE;
/*!40000 ALTER TABLE `l3element_deltaH_x` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3element_deltaH_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_deltaS_x`
--

LOCK TABLES `l3element_deltaS_x` WRITE;
/*!40000 ALTER TABLE `l3element_deltaS_x` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3element_deltaS_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_name`
--

LOCK TABLES `l3element_name` WRITE;
/*!40000 ALTER TABLE `l3element_name` DISABLE KEYS */;
INSERT INTO `l3element_name` VALUES (4,'KEGG'),(4,'Kyoto Encyclopedia of Genes and Genomes'),(5,NULL),(5,'aMAZE'),(6,NULL),(6,'Swiss-Prot/TrEMBL'),(10,'glucose ATP phosphotransferase'),(10,'beta-D-glu + ATP => beta-D-glu-6-p + ADP'),(10,'ATP:D-glucose 6-phosphotransferase'),(10,'b-D-glu => b-D-glu-6-p'),(14,'b-D-glu-6-p'),(14,'beta-D-glucose 6-phosphate'),(16,'D-glucose-6-P'),(16,'beta-D-glucose 6-phosphate'),(16,'glucose-6-P'),(16,'beeta-D-glucose-6-p'),(16,'b-D-glucose-6-phoshate'),(16,'a-D-glu-6-p'),(18,'ADP'),(18,'Adenosine 5\'-diphosphate'),(18,'adenosine diphosphate'),(20,'ADP'),(20,'Adenosine 5\'-diphosphate'),(20,'adenosine diphosphate'),(22,'beta-D-glucose'),(22,'b-D-glu'),(24,'beta-D-glucose'),(24,'<FONT FACE=\"Symbol\">a</FONT>-D-glucose'),(24,'b-D-glu'),(25,'Adenosine 5\'-triphosphate'),(25,'adenosine triphosphate'),(25,'ATP'),(27,'Adenosine 5\'-triphosphate'),(27,'adenosine triphosphate'),(27,'ATP'),(32,'b-D-fru-6-p'),(32,'beta-D-fructose 6-phosphate'),(33,'<FONT FACE=\"Symbol\">b</FONT>-D-fructose-6-phosphate'),(33,'b-D-fru-6-p'),(33,'beta-D-fructose 6-phosphate'),(35,'catalysis of (alpha-D-glu <=> alpha-D-glu-6-p)'),(35,'GLK -> (a-D-glu <=> a-D-glu-6-p)'),(36,NULL),(36,'Escherichia coli'),(37,'glucose kinase'),(37,'glucokinase'),(37,'GLK'),(38,'glucokinase'),(38,'GLK'),(38,'GLK_ECOLI'),(39,'beta-D-Glucose 6-phosphate ketol-isomerase'),(39,'beta-D-Glucose 6-phosphate => beta-D-Fructose 6-phosphate'),(39,'beta-D-glu-6-p <=> beta-D-fru-6-p'),(39,'b-D-glu-6-p <=> b-D-fru-6-p'),(44,'GPI'),(44,'phosphoglucose isomerase'),(44,'phosphohexose isomerase'),(44,'PGI'),(44,'PHI'),(44,'glucose-6-phosphate isomerase'),(46,'GPI'),(46,'phosphoglucose isomerase'),(46,'PGI'),(46,'phosphohexose isomerase'),(46,'PHI'),(46,'glucose-6-phosphate isomerase'),(47,'glycolysis'),(47,'Embden-Meyerhof pathway'),(47,'Glycolysis Pathway'),(47,'glucose degradation'),(48,'catalysis of (beta-D-glu-6-p <=> beta-D-fruc-6-p)'),(48,'PGI -> (b-d-glu-6-p <=> b-D-fru-6p)');
/*!40000 ALTER TABLE `l3element_name` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_source`
--

LOCK TABLES `l3element_source` WRITE;
/*!40000 ALTER TABLE `l3element_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3element_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_term`
--

LOCK TABLES `l3element_term` WRITE;
/*!40000 ALTER TABLE `l3element_term` DISABLE KEYS */;
INSERT INTO `l3element_term` VALUES (11,'cytoplasm');
/*!40000 ALTER TABLE `l3element_term` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3element_url`
--

LOCK TABLES `l3element_url` WRITE;
/*!40000 ALTER TABLE `l3element_url` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3element_url` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3entity_data_source`
--

LOCK TABLES `l3entity_data_source` WRITE;
/*!40000 ALTER TABLE `l3entity_data_source` DISABLE KEYS */;
INSERT INTO `l3entity_data_source` VALUES (10,4),(10,5),(16,4),(16,5),(20,4),(20,5),(24,4),(24,5),(27,4),(27,5),(33,4),(33,5),(35,4),(35,5),(38,5),(38,6),(39,4),(39,5),(46,4),(46,5),(47,4),(47,5),(48,4),(48,5);
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
INSERT INTO `l3entity_xref` VALUES (10,7),(20,19),(24,23),(27,26),(33,1),(38,8),(39,40),(46,34),(47,45);
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
INSERT INTO `l3entityref_xref` VALUES (14,15),(18,19),(22,23),(25,26),(32,1),(37,8),(44,34);
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
INSERT INTO `l3interaction_participants_x` VALUES (10,16),(10,20),(10,24),(10,27),(35,10),(35,38),(39,16),(39,33),(48,39),(48,46);
/*!40000 ALTER TABLE `l3interaction_participants_x` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3model`
--

LOCK TABLES `l3model` WRITE;
/*!40000 ALTER TABLE `l3model` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3model_l3element`
--

LOCK TABLES `l3model_l3element` WRITE;
/*!40000 ALTER TABLE `l3model_l3element` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3model_l3element` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3model_ns`
--

LOCK TABLES `l3model_ns` WRITE;
/*!40000 ALTER TABLE `l3model_ns` DISABLE KEYS */;
/*!40000 ALTER TABLE `l3model_ns` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `l3pathway_pathway_component`
--

LOCK TABLES `l3pathway_pathway_component` WRITE;
/*!40000 ALTER TABLE `l3pathway_pathway_component` DISABLE KEYS */;
INSERT INTO `l3pathway_pathway_component` VALUES (47,10),(47,48);
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
INSERT INTO `l3pathwaystep_step_process` VALUES (49,48),(50,35);
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

-- Dump completed on 2010-04-07  1:17:10
