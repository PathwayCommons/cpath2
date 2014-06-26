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
			<em>&lt;FORMAT&gt;</em> is one of the <a href="formats">Output Formats</a>; 
			<em>&lt;ext&gt;</em> is the file type.
		</div>
	  </div>
		
		<h3>Notes</h3>
		<ul>
			<li><strong>GSEA output</strong> - we export only UniProt IDs
				(of <em>ProteinReferences</em>) if they are found.</li>
			<li><strong>Archives by species/source</strong> - were created based on the 
				full-text search results, using 'organism' and 'datasource' filters, respectively.</li>
			<li><strong>Blacklisting</strong> 
				- <a href="http://code.google.com/p/biopax-pattern/wiki/UsingBinaryInteractionFramework#Blacklisting_ubiquitous_small_molecules" 
				target="_blank">blacklist.txt file description</a></li>
			<li><strong>Validation</strong> - one can download full <a href='<c:url value="/validations"/>'>
				BioPAX validation reports (quite simple XML)</a></li>
			<li><strong>Other data</strong> - one can also get <a href='<c:url value="/datadir"/>'>original and normalized data</a> 
				(files at various intermediate states, before merged with UniProt, ChEBI and together into our main BioPAX model)</li>
				
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
