<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

	<div id="header">
		<a href="<fmt:message key="provider.url" />">
			<img alt="Owner's Logo" src="<fmt:message key="provider.logo.url" />" width="72" height="72">
		</a>
		<h1>
			<fmt:message key="provider.name" />, version <fmt:message key="provider.version" /> 
		</h1>
		<c:if test="${maintenanceModeEnabled}">
			<strong>Running in the <a href="<c:url value="/admin.html"/>">Maintenance</a> mode.</strong>
		</c:if>
		<div id="menu">
		  <ul>
			<li><a href="<c:url value="/home.html#data_sources"/>">Data Sources</a></li>
			<li><a href="<c:url value="/downloads.html"/>">Downloads</a></li>
			<li><a href="<c:url value="/home.html#commands"/>">Commands</a></li>
			<li><a href="<c:url value="/home.html#additional_parameters"/>">Parameters</a></li>
			<li><a href="<c:url value="/home.html#errors"/>">Errors</a></li>
		  </ul>
		</div>
	</div><br />
