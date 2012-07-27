<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 BioPAX Data Validation Summary" />
	<meta name="keywords" content="cPath2, BioPAX, Validation" />
	<script type="text/javascript">
	  function switchit(list) {
		var listElementStyle = document.getElementById(list).style;
		if (listElementStyle.display == "none") {
			listElementStyle.display = "block";
		} else {
			listElementStyle.display = "none";
		}
	  }
	</script>
	<title>Validation Results</title>
</head>
<body>

<h2>Validation Results</h2>
<ul>
<c:forEach var="result" items="${response.validationResult}" varStatus="rstatus">
	<li style="text-decoration: underline" title="Click to see more detail">
		<a href="javascript:switchit('result${rstatus.index}')">Resource:&nbsp;${result.description};&nbsp;${result.summary}</a>
	</li>
	<ul style="list-style: inside;">
	<li>
	  <c:forEach var="comment" items="${result.comment}">
		${comment}&nbsp;
	  </c:forEach>
	</li>
	<li>auto-fix: ${result.fix};&nbsp;normalize: ${result.normalize}</li>
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
	</ul>
	
	<ul id="result${rstatus.index}" style="display: none; list-style: decimal;">
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
		<ul id="result${rstatus.index}type${estatus.index}" style="display: none">
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
	<br/>
</c:forEach>
</ul>


</body>
</html>
