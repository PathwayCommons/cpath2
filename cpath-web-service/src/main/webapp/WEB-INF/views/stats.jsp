<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 simple access log summary" />
<meta name="keywords" content="${cpath.name}, cPath2, BioPAX, accesslog, stats" />
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link href="<c:url value="/resources/css/bootstrap.min.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/bootstrap-select.min.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/cpath2.css"/>" rel="stylesheet" media="screen"/>
<script	type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/bootstrap.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/bootstrap-select.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/codes2names.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/stats.js"/>"></script>
<title>cPath2: Stats</title>
</head>
<body>
	
	<jsp:include page="header.jsp" />
	
	<div id="content">
		<h2>Access Summary for ${summary_for}</h2>
		<section id="read_and_jump">
		<!-- to be filled by Javascript and styled with Tweeter Bootstrap -->
		<div class="row-fluid">
		  <p>
			Charts below can display counts of remote user requests that used one of 
		  	<a href="<c:url value="/home.html#commands"/>">cpath2 web service commands</a>,  
		  	organized by category (e.g., Total, Providers, Files), or just for one thing
		  	(e.g., selected data source, filename, format).
		  	Calls from all cPath2 client applications, such as Cytoscape apps, PCViz, ChiBE, 
		  	scripts, are equally treated. A single web query increments the total number of 
		  	requests sent on that date from user's location (country, region, city) and,  
		  	usually, kicks other access counts, such as per: pathway/interaction data provider 
		  	(when corresponding data contributed to the result, or data source mentioned 
		  	in a search response), command name (e.g., search, graph type, traverse), data format 
		  	requested, file downloaded, error type, etc. 
		  </p>
		  <select class="selectpicker cpath-logs" 
		  	data-header="Find, select and press the button to refresh"
			data-live-search="true" data-width="30%" data-container="body">
		  </select>
		  <button class="btn btn-success show-cpath-log">Update</button>
		</div>
		<br/>
		</section>
		<section id="by_date">
		<div class="row">
			<div class="span6">
				<h3 style="display: inline; margin-right: 1em;">Timeline</h3>
				<div class="btn-group" style="display: inline;">
					<button class="btn btn-small active" id="timeline-cumulative">Cumulative</button>
					<button class="btn btn-small" id="timeline-by-day">By Day</button>
				</div>
			</div>
			<div class="span6">
				<button class="btn btn-small pull-right" id="timeline-csv">
					<i class="icon-download-alt"></i> Save as CSV
				</button>
			</div>
		</div>

		<div class="row">
			<div class="span12">
				<div id="timeline-chart"
					style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
			</div>
		</div>
		</section>
		<section id="by_geolocation">
		<div class="row">
			<div class="span6">
				<h3>Geography</h3>
			</div>
			<div class="span6">
				<button class="btn btn-small pull-right" id="geography-csv">
					<i class="icon-download-alt"></i> Save as CSV
				</button>
			</div>
		</div>

		<div class="row">
			<div class="span12">
				<div id="geography-world-chart"
					style="width: 100%; height: 540px; cursor: pointer;"></div>
			</div>
		</div>

		<div class="row">
			<div class="span12">
				<h4>
					<span id="geography-country-name"></span> <img id="country-loading"
						src="<c:url value="/resources/images/loading.gif"/>"
						style="display: none;">
				</h4>
			</div>
		</div>

		<div class="row">
			<div class="span12">
				<div id="geography-country-chart"
					style="width: 100%; height: 540px;"></div>
			</div>
		</div>
	</section>
	</div>
	
	<jsp:include page="footer.jsp" />

	<script type="text/javascript">
		google.load('visualization', '1', {
			'packages' : [ 'corechart', 'geochart', 'annotatedtimeline' ]
		});
		
		//cpath2 app's root context path
		var contextPath = '<%=request.getContextPath()%>';
		
		google.setOnLoadCallback(function() {
			AppStats.setupTimeline();
			AppStats.setupGeography();
			AppStats.setupSelectEvents();
		});
	</script>

</body>
</html>
