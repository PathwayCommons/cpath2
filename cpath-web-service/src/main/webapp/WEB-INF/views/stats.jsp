<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<link href="/resources/css/bootstrap-select.min.css" rel="stylesheet" />

<jsp:include page="head.jsp" />

<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src="/resources/scripts/codes2names.js"></script>
<script type="text/javascript" src="/resources/scripts/stats.js"></script>

<title>cPath2::Log</title>
</head>
<body>
	<jsp:include page="header.jsp" />	
	<h2>Web service access summary</h2>
	<div class="row">
	  <div class="jumbotron">
		  <p>
			Charts below show how many clients were there, and where they came from. 
			Counts are organized in several categories and also per	unique name in each category.
		  	Calls from all client applications, such as Cytoscape apps, PCViz, ChiBE, 
		  	scripts, are equally treated. A single web query increments the total number of 
		  	requests sent on that date from user's location (city) as well as other counts, 
		  	such as - per data source (if provider's data were found in the biopax query 
		  	result or search response), command, data format, file name, etc.
		  </p>
		  <h3>${summary_for}</h3>
		  <select class="selectpicker cpath-logs" 
		  	data-header="Find, select and press the button to refresh"
			data-live-search="true" data-width="30%" data-container="body">
		  </select>
		  <button class="btn btn-primary show-cpath-log">Refresh </button>
	  </div>
	</div>

		<div class="row">
			<h3>Timeline</h3>
			<div class="btn-group" style="display: inline;">
				<button class="btn btn-small btn-default active" id="timeline-cumulative">Cumulative</button>
				<button class="btn btn-small btn-default" id="timeline-by-day">By Day</button>&nbsp;
				<button class="btn btn-small btn-success pull-right" id="timeline-csv">
					<i class="icon-download-alt"></i>Save as CSV 
				</button>
			</div>
		</div>

		<div class="row">
			<div id="timeline-chart" style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
		</div>

		<div class="row">
			<h3>Geography</h3>
			<button class="btn btn-small btn-success pull-right" id="geography-csv">
				<i class="icon-download-alt"></i>Save as CSV 
			</button>
		</div>

		<div class="row">
			<div id="geography-world-chart" style="width: 100%; height: 540px; cursor: pointer;"></div>
		</div>

		<div class="row">
			<h4>
				<span id="geography-country-name"></span> 
				<img id="country-loading" src='<c:url value="/resources/img/loading.gif"/>'
						style="display: none;">
			</h4>
		</div>

		<div class="row">
			<div id="geography-country-chart" style="width: 100%; height: 540px;"></div>
		</div>
	
	<jsp:include page="footer.jsp" />

	<!-- finally, load all page-specific scripts -->
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
