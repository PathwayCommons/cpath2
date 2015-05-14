<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src='<c:url value="/resources/scripts/codes2names.js"/>'></script>
<script type="text/javascript" src='<c:url value="/resources/scripts/stats.js"/>'></script>
<title>cPath2::Log</title>
</head>
<body>
	<jsp:include page="header.jsp" />	
	
	<h2>Access Summary</h2>
	<em>${summary_for}</em>

    <div class="row">
    <div class="jumbotron">
		<p>
			Charts below show how many requests were there and where did they come from. 
		  	Calls from all client applications are treated equally. 
		  	A single web query increments the total number of 
		  	requests sent on that date from user's location (city) and other counts
		  	in several categories, such as per original data provider, command, format, file name.
		  	To switch the view, select another item from the drop-down list.
		  	Access statistics are computed from January 2014.	
		</p>
		<select class="selectpicker" 
	  		data-header="Select a category or name to display"
			data-live-search="true" data-width="33%" data-container="body">
		</select>	  
	</div>
	</div>

	<div class="row">
		<h3>Timeline</h3>
		<div class="btn-group" style="display: inline;">
			<button class="btn btn-small btn-default" id="timeline-cumulative">Cumulative</button>
			<button class="btn btn-small btn-default active" id="timeline-by-day">By Day</button>&nbsp;
			<button class="btn btn-small btn-success pull-right" id="timeline-csv">
				<i class="icon-download-alt"></i>Save as CSV 
			</button>
		</div>
		<div id="timeline-chart" style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
	</div>

	<div class="row">
		<h3>Geography</h3>
		<button class="btn btn-small btn-success pull-right" id="geography-csv">
			<i class="icon-download-alt"></i>Save as CSV 
		</button>
		<div id="geography-world-chart" style="width: 100%; height: 540px; cursor: pointer;"></div>
	</div>

	<div class="row">
		<h4>
			<span id="geography-country-name"></span> 
			<img id="country-loading" src='<c:url value="/resources/img/loading.gif"/>'
					style="display: none;">
		</h4>
		<div id="geography-country-chart" style="width: 100%; height: 540px;"></div>
	</div>
	
	<jsp:include page="footer.jsp" />

	<!-- finally, load all page-specific scripts -->
	<script type="text/javascript">
		google.load('visualization', '1', {
			'packages' : [ 'corechart', 'geochart', 'annotatedtimeline' ]
		});
		
		var contextPath = '<%=request.getContextPath()%>';
		
		google.setOnLoadCallback(function() {
			AppStats.setupTimeline();
			AppStats.setupGeography();
			AppStats.setupSelectpicker();
		});
	</script>
</body>
</html>
