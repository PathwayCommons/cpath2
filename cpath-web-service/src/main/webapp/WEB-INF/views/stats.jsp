<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>

<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 simple access log summary" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/bootstrap.min.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/bootstrap-select.min.css"/>" rel="stylesheet" />
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
		<p>
		<!-- TODO a general description of what we log and that all sort of clients contribute to this stats... -->
		</p>
		<br/>
		<!-- to be filled by Javascript and styled with Tweeter Bootstrap -->
		<div class="row-fluid">
		  <select class="selectpicker cpath-logs" 
		  	data-header="Find, select and press the button to refresh"
			data-live-search="true" data-width="30%" data-container="body">
		  </select>
		  <button class="btn btn-success show-cpath-log">Update</button>
		</div>
		<br/>
		
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

	</div>
	<jsp:include page="footer.jsp" />


	<script type="text/javascript">
		google.load('visualization', '1', {
			'packages' : [ 'corechart', 'geochart', 'annotatedtimeline' ]
		});
		
		google.setOnLoadCallback(function() {
			AppStats.setupTimeline();
			AppStats.setupGeography();
			AppStats.setupSelectEvents();
		});
	</script>

</body>
</html>
