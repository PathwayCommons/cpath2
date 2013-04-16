<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 BioPAX Data Validation Summary" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"
	 rel="stylesheet" />
<script >
	function switchit(list) {
		var listElementStyle = document.getElementById(list).style;
		if (listElementStyle.display == "none") {
			listElementStyle.display = "block";
		} else {
			listElementStyle.display = "none";
		}
	}
</script>
<title>cPath2::Validation Summary</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Pathway Data Validation Summary</h2>
		<ul id="validations">
			<c:forEach var="result" items="${response.validationResult}"
				varStatus="rstatus">
				<li style="text-decoration: underline"
					title="Click to see more detail"><a
					href="javascript:switchit('result${rstatus.index}')">Resource:&nbsp;${result.description};&nbsp;${result.summary}</a>
				</li>
		<ul id="vcomments" style="display: block;">
	<li>
	  <c:forEach var="comment" items="${result.comment}">
		${comment}&nbsp;
	  </c:forEach>
	</li>
	<li>
	  <c:choose>
		<c:when test="${result.profile != null}">profile: ${result.profile};&nbsp;</c:when>
		<c:otherwise>profile: default;&nbsp;</c:otherwise>
	  </c:choose>
	  auto-fix: ${result.fix}
	</li>
	<li>
	  errors/warnings: ${result.totalProblemsFound};&nbsp;- not fixed: ${result.notFixedProblems};&nbsp; 
	  <c:choose>
		<c:when test="${result.maxErrors > 0}">
			errors limit: ${result.maxErrors} (not fixed)
		</c:when>
		<c:otherwise>
			errors not fixed: ${result.notFixedErrors}
		</c:otherwise>
	  </c:choose>
	</li>
	
	<c:if test="${result.fix}">
	  	<li><a href="javascript:switchit('result${rstatus.index}owl')">Modified BioPAX</a>&nbsp;(HTML-escaped BioPAX RDF/XML)</li>
		<ul id="result${rstatus.index}owl" class="vOwl">
			<li><div>${result.modelDataHtmlEscaped}</div></li>
		</ul>
	</c:if>
  </ul>				
	<ul id="result${rstatus.index}">
	  <c:forEach var="errorType" items="${result.error}" varStatus="estatus">
		<li title="Click to see the error cases">
			<a href="javascript:switchit('result${rstatus.index}type${estatus.index}')">
			${errorType.type}: <em>${errorType.code}</em>,&nbsp;category: <em>${errorType.category}</em>,
			&nbsp;cases: <em>${errorType.totalCases}</em>,&nbsp;
			<c:choose>
				<c:when test="${errorType.notFixedCases > 0}">
				not fixed: <em>${errorType.notFixedCases}</em>
				</c:when>
				<c:otherwise>
				all fixed!
				</c:otherwise>
	  		</c:choose>
			</a><br/>${errorType.message}
		</li>
		<ul id="result${rstatus.index}type${estatus.index}">
		<c:forEach var="errorCase" items="${errorType.errorCase}">
			<li>
				<c:if test="${errorCase.fixed}"><b>[FIXED!]</b>&nbsp;</c:if>
				object:<b>&nbsp;${errorCase.object}</b>
				<div>${errorCase.message}</div>(found by: <em>${errorCase.reportedBy}</em>)
			</li>
		</c:forEach>
		</ul>
	  </c:forEach>
	</ul>
				<br />
			</c:forEach>
		</ul>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
