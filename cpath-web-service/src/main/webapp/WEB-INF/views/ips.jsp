<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src="/resources/scripts/ips.js"></script>
<title>cPath2::IPs</title>
</head>
<body>
	<jsp:include page="header.jsp" />	
	
	<h2>Unique Clients (IP) Access Summary</h2>
	<em>${summary_for}</em>

    <div class="row">
    <div class="jumbotron">
		<p>
			Charts below show numbers of unique IP addresses the user requests originated from. 
		  	All client applications are equally considered. 
		  	To switch the view, select another item from the drop-down list.
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
			<button class="btn btn-small btn-default active" id="timeline-cumulative">Cumulative</button>
			<button class="btn btn-small btn-default" id="timeline-by-day">By Day</button>&nbsp;
			<button class="btn btn-small btn-success pull-right" id="timeline-csv">
				<i class="icon-download-alt"></i>Save as CSV 
			</button>
		</div>
		<div id="timeline-chart" style="width: 100%; height: 540px; margin-top: 2em; margin-bottom: 10em;"></div>
	</div>
	
	<jsp:include page="footer.jsp" />

	<!-- finally, load all page-specific scripts -->
	<script type="text/javascript">
		google.load('visualization', '1', {
			'packages' : [ 'corechart', 'annotatedtimeline' ]
		});
		
		var contextPath = '<%=request.getContextPath()%>';
		
		google.setOnLoadCallback(function() {
			AppIps.setupTimeline();
			AppIps.setupSelectpicker();
		});
	</script>
</body>
</html>
