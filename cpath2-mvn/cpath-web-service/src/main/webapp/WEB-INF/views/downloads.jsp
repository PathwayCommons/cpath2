<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content="Pathway Commons" />
<meta name="description" content="cPath2 BioPAX Data Validations per Data Source" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" rel="stylesheet" />
<title>Downloads</title>
</head>
<body>

	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Downloads</h2>
		<h3>Description</h3>
		<p>
			Data exported from the CPath2 server are organized as follows.<br />
			Archives below are sorted alphabetically, and their names follow the
			simple naming pattern:
			<br /><br /><code>cpath2_&lt;source&gt;.&lt;FORMAT&gt;.&lt;ext&gt;.gz</code><br /><br />
			where <em>source</em> - is either a data source standard name, organism
			taxonomy ID, or 'all'; <em>FORMAT</em> - 'biopax' and other formats, to which the BioPAX L3
			was converted.
		</p>

		<p>The <a href="<c:url value="/resources/docs/README.txt"/>">README.txt</a> briefly describes the output formats.</p>

		<h4>Notes:</h4>
		<ul>
			<li><strong>GSEA format</strong> - we export only UniProt accession
				numbers (of <em>ProteinReferences</em>) and do not check for whether
				a pathway and its member entities come from different organisms
				(which, in fact, happens in pathways from PhosphoSitePlus, HumanCyc,
				PantherDb). That also means, some ProteinReferences are not there
				listed (i.e., those not having a UniProt <em>UnificationXref</em>).
				Hopefully, those missing are "generic" ones (check in the
				corresponding BioPAX archive, or using 'get?uri=...' query, to make
				sure).</li>
			<li><strong>Archives by species and source</strong> - were generated based on the 
				full-text search results, using 'organism' and 'datasource' filters, respectively.</li>
		</ul>
		
		<h3>FILES:</h3>
		<ul>
			<c:forEach var="f" items="${files}">
				<li><a href='<c:url value="/downloads/${f.key}"/>'>${f.key}</a>&nbsp;(${f.value})</li>
			</c:forEach>
		</ul>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
