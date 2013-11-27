<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

	<header>
		<a href="${cpath.url}">
			<img alt="Owner's Logo" src="${cpath.logoUrl}" width="72" height="72">
		</a>
		<h1>
			${cpath.name} Web Services, Version ${cpath.version}
		</h1>
		<c:if test="${cpath.adminEnabled}">
			<strong>Running in the Maintenance Mode.</strong>
		</c:if>
		<div id="menu">
		  <ul>
		  	<c:if test="${cpath.adminEnabled}">
				<li><a href="<c:url value="/admin"/>">Admin</a></li>
			</c:if>
			<li><a href="<c:url value='/' />">Introduction</a></li>
			<li><a href="<c:url value="/datasources.html"/>">Data</a></li>
			<li><a href="<c:url value="/downloads.html"/>">Downloads</a></li>
			<li><a href="<c:url value="/home.html#commands"/>">Commands</a></li>
			<li><a href="<c:url value="/home.html#additional_parameters"/>">Parameters</a></li>
		  </ul>
		</div>
	</header>
	<br/>

