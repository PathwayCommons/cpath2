<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 BioPAX Data Validations per Data Source" />
	<meta name="keywords" content="cPath2, BioPAX, Validation" />
	<title>Downloads</title>
</head>
<body>

<h2><fmt:message key="cpath2.provider"/> Downloads</h2>

<div style="width: 50%">
<p>
Data exported from the CPath2 server are organized as follows.<br/>
Archives below are sorted alphabetically, and their names follow 
the simple naming pattern:<br/><br/>
<em>cpath2_&lt;source&gt;.&lt;FORMAT&gt;.&lt;ext&gt;.gz</em><br/><br/>, where:
</p>

<ul>
<li>SOURCE - is either a data source standard name, organism taxonomy ID, or 'all';</li>
<li>FORMAT - 'biopax' and other formats, to which the BioPAX L3 was converted</li>
</ul>

<p>
The README.txt briefly describes output formats.
</p>

<p>
Notes:
</p>
<ul>
<li><b>GSEA format</b> - we export only UniProt accession numbers (of <em>ProteinReferences</em>) 
and do not check for whether a pathway and its member entities come from 
different organisms (which, in fact, happens in pathways from PhosphoSitePlus, 
HumanCyc, PantherDb). That also means, some ProteinReferences are not there listed  
(i.e., those not having a UniProt <em>UnificationXref</em>). Hopefully, those missing are 
"generic" ones (check in the corresponding BioPAX archive, or using 'get?uri=...' query, to make sure).</li>
<li><b>SBGN output</b> - is still experimental</li>
<li><b>Separation by species</b> (using taxonomy id) is currently done in a way 
consistent with our full-text search results and using the 
'organism' filter (i.e., using the same CPath2 Lucene full-text index).  
So, e.g., human (9606) data contains other organism entities as well. This is
mainly due to following reasons: a) we do not want to tear apart any  
interactions defined there, despite they are multi-organism; and b) user wants quickly find
and filter, e.g., a biochemical reaction, control, or small molecule associated with 
a particular organism, and this would be hard to do with using original BioPAX models only, without 
additional inference and indexing (neither BioPAX <em>Interaction</em> nor <em>SmallMoleculeReferenc</em>e have
'organism' property; 'organism' value is sometimes not set for <em>Complex</em> entities, etc.)
Such inference makes some entities (interactions and participants) associated with more 
than one organism and thus end up in, e.g., both human and mouse archives.
(Eventually, either original BioPAX data will be using 'organism' property 
consistently and make clear what cross-species pathways/interactions actually mean, 
or we may use more resources and science to check/map to homologs during the data import or export...)
</li>
</ul>

</div>

<ul>
  <c:forEach var="f" items="${files}">
	<li><a href='<c:url value="/downloads/${f.key}"/>'>${f.key}</a>&nbsp;(${f.value})</li>
  </c:forEach>
</ul>

</body>
</html>
