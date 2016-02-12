<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<spring:url value="/resources" var="resources" />
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src="${resources}/scripts/codes2names.js"></script>
<script type="text/javascript" src="${resources}/scripts/stats.js"></script>
<title>cPath2::Log</title>
</head>
<body>
	<jsp:include page="header.jsp" />	
	
	<h2>Access Summary</h2>
	<em>Current ${summary_for}</em>

    <div class="row">
      <div class="jumbotron">
		<p>
		  	Calls from all applications are treated equally. 
		  	Each increments the total number of requests sent on that date 
		  	from user's location and other counts, such as by provider, command.
			(IP addresses were not analyzed before 2014/11).
		</p>
		<select class="selectpicker" 
	  		data-header="Select a category or name to display (reload page)"
			data-live-search="true" data-width="33%" data-container="body">
		</select>
	  </div>
	  <p>
		<spring:url value="/log" var="log"/>
		<em>See also:</em> <a rel="nofollow" href="${log}/totals">a quick summary</a>,
		<a rel="nofollow" href="${log}/totalok">no. successful requests</a>,
		<a rel="nofollow" href="${log}/totalip">no. unique users</a>.
	  </p>
	</div>
	<div class="row">
		<h2>Timelines by Day</h2>
		<p><em>${from_to}</em></p>
		<div class="btn-group" style="display: inline;">
			<button class="btn btn-small btn-default" id="timeline-cumulative">Cumulative</button>
			<button class="btn btn-small btn-default active" id="timeline-by-day">Simple</button>&nbsp;
		</div>	
	</div>
	<div class="row">
		<h3>Requests</h3>
		<div id="timeline-chart" style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
		<button class="btn btn-small btn-success pull-right" id="timeline-csv"><i class="icon-download-alt"></i>Save as CSV</button>
	</div>	
	<div class="row">
		<h3>Unique Users</h3>
		<div id="iptimeline-chart" style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
		<button class="btn btn-small btn-success pull-right" id="iptimeline-csv"><i class="icon-download-alt"></i>Save as CSV</button>
	</div>	
	<div class="row">
		<h3>Geography</h3>
		<button class="btn btn-small btn-success pull-right" id="geography-csv">
			<i class="icon-download-alt"></i>Save as CSV 
		</button>
		<div id="geography-world-chart" style="width: 100%; height: 540px; cursor: pointer;"></div>
	</div>
	<div class="row">
		<h3>
			<span id="geography-country-name"></span> 
			<img id="country-loading" src="${resources}/img/loading.gif"
					style="display: none;">
		</h3>
		<div id="geography-country-chart" style="width: 100%; height: 540px;"></div>
	</div>
	<br/>
	
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
