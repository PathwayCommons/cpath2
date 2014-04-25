<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>Downloads</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<h2>Batch download</h2>
	
	  <div class="row">
		<div class="jumbotron">
		<h3>File names</h3>
		<blockquote><p>
			Data Archives below are sorted alphabetically and named as follows:</p>
			<code>${cpath.name}.${cpath.version}.&lt;SOURCE&gt;.&lt;FORMAT&gt;.&lt;ext&gt;.gz</code>
			</blockquote>
			<em>&lt;SOURCE&gt;</em> is a standard name, taxonomy ID, or 'All';
			<em>&lt;FORMAT&gt;</em> is one of the <a href="formats">output formats</a>; 
			<em>&lt;ext&gt;</em> is the file type.
		</div>
	  </div>
		
		<h3>Notes</h3>
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
		
		<h3>Files</h3>
		<ul>
			<c:forEach var="f" items="${files}">
				<li><a href='<c:url value="/downloads/${f.key}"/>'>${f.key}</a>&nbsp;(${f.value})</li>
			</c:forEach>
		</ul>

	<jsp:include page="footer.jsp" />
</body>
</html>
