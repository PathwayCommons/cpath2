<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<h1>Error</h1>

<div style="width: 70%">
	<c:out value="${exception}" /> <c:out value="${exception.message}" />
	<c:forEach var="trace" items="${exception.stackTrace}">
			<li>${trace}</li>
	</c:forEach>
</div>
