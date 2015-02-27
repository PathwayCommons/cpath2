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
		<div class="jumbotron">
			<h2>Batch Downloads</h2>
			<blockquote><p>
				Data files listed below are sorted alphabetically and generally named as follows:</p>
				<code>${cpath.name}.${cpath.version}.&lt;SOURCE&gt;.&lt;FORMAT&gt;.&lt;ext&gt;.gz</code>
				<p>(Previous versions can be downloaded from the <a href="${cpath.url}/archives">${cpath.name} archive</a>)</p>
			</blockquote>
			<em>&lt;SOURCE&gt;</em> - is either 'All' (the integrated BioPAX model loaded into the 
				<a href='<c:url value="/home"/>'>web service</a>),
				'Detailed' (excluding all PSI-MI data providers), or other;<br/>
			<em>&lt;FORMAT&gt;</em> - one of the <a href="formats">Output Formats</a><br/> 
			<em>&lt;ext&gt;</em> - output id type, sub-type, standard file extension, e.g., "hgnc.gmt.tsv".
		</div>
		
		<h3>Notes</h3>
		<ul>				
			<li>blacklist.txt is used by the BioPAX to SIF and SBGN converters to exclude ubiquitous small molecules from output. 
				See <a href="http://code.google.com/p/biopax-pattern/wiki/UsingBinaryInteractionFramework#Blacklisting_ubiquitous_small_molecules" 
				target="_blank">blacklist.txt file description</a> for more information.</li>
			<li>datasources.txt provides metadata and some statistics about each data source.</li>				
			<li>Original and intermediate (after cleaning, conversion, normalization) data archives 
				<a rel="nofollow" href='<c:url value="/datadir"/>'>are available here</a>.
				In addition, <a href="http://www.biopax.org/validator">BioPAX validation</a> reports  
				for each pathway or interaction data source in the system can be found 
				<a rel="nofollow" href='<c:url value="/validations"/>'>here</a>.</li>
			<li>We can also generate a BioPAX sub-model and convert to the simple formats on your special request: 
				select sources, add columns (for SIF) or description (for GSEA), select binary interaction types and output ID type); 
				or one can do it using <a href="http://www.biopax.org/paxtools">Paxtools</a> 
				and our BioPAX archives as input, and the blacklist.txt (to ignore ubiquitous small molecules).</li>			
		</ul>
		
		<h3>Files</h3>
		<ul>
			<c:forEach var="f" items="${files}">
				<li><a rel="nofollow" href='<c:url value="/downloads/${f.key}"/>'>${f.key}</a>&nbsp;(${f.value})</li>
			</c:forEach>
		</ul>

	<jsp:include page="footer.jsp" />
</body>
</html>
