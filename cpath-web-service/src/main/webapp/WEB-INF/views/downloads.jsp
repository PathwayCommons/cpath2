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
				<code>&lt;prefix&gt;.&lt;source&gt;.&lt;FORMAT&gt;.&lt;ext&gt;.gz</code>
				<br/>
				<p>Current and old data can be downloaded from our <a href="${cpath.url}/archives">archive site</a>
					(try from that location also if you could not download a file from here).
				</p>
			</blockquote>
			<em>&lt;source&gt;</em>: 'All' (main BioPAX model, used by the
				<a href='<c:url value="/home"/>'>web service</a>), 'Detailed' (no PSI-MI data sources), 
				'Warehouse' (only reference and annotation type BioPAX objects), or a data source name.<br/>
			<em>&lt;FORMAT&gt;</em>: one of the <a href="formats">Output Formats</a>.<br/> 
			<em>&lt;ext&gt;</em>: output id type and file extension, e.g., "hgnc.gmt".
		</div>
		
		<h3>Notes</h3>
		<ul>
			<li>We can generate a BioPAX sub-model and convert to the simple formats on your special request:
				select sources, add columns (for SIF) or description (for GSEA), select binary interaction types and output ID type.
				One can also do it with <a href="http://www.biopax.org/paxtools">Paxtools</a>,
				one of the BioPAX files and blacklist.txt (to ignore ubiquitous small molecules).</li>
			<li>Original and intermediate (cleaned, converted, normalized) data archives
				<a rel="nofollow" href='<c:url value="/datadir"/>'>are available here</a>.</li>
			<li>BioPAX <a href="http://www.biopax.org/validator">validation</a> reports
				for each pathway/interaction data source in the system can be found
				<a rel="nofollow" href='<c:url value="/validations"/>'>here</a>.</li>
			<li>The blacklist.txt is used by the BioPAX to SIF and to SBGN converters to exclude ubiquitous small molecules.
				See also: <a href="https://github.com/BioPAX/Paxtools/wiki/PatternBinaryInteractionFramework"
				target="_blank">Using Binary Interaction Framework</a> for more information (contains blacklist.txt description).</li>
			<li>datasources.txt provides metadata and some statistics about each data source.</li>
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
