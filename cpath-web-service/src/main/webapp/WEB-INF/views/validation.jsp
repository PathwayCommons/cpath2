<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<style type="text/css">.hidden { display: none; }</style>
<jsp:include page="head.jsp"/>
<title>cPath2::Validation Summary</title>
</head>
<body>
<jsp:include page="header.jsp" />
<h2>Pathway Data Validation Summary</h2>
<ul id="validations">
  <c:forEach var="result" items="${response.validationResult}" varStatus="rstatus">
	<li style="text-decoration: underline">
	Resource:&nbsp;${result.description};&nbsp;${result.summary}
	</li>
	<ul id="vcomments">
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
<%-- no normalized biopax ever shows up here because cpath2 clears it and saves the xml to separate files --%>	
  </ul>				
	<ul id="result${rstatus.index}">
	  <c:forEach var="errorType" items="${result.error}" varStatus="estatus">
		<li title="Click to see the error cases">
			<a class="hider" hide-id="result${rstatus.index}type${estatus.index}" href="#">
				${errorType.type}: <em>${errorType.code}</em></a>
			,&nbsp;category: <em>${errorType.category}</em>,
			&nbsp;cases: <em>${errorType.totalCases}</em>,&nbsp;
			<c:choose>
				<c:when test="${errorType.notFixedCases > 0}">
				not fixed: <em>${errorType.notFixedCases}</em>
				</c:when>
				<c:otherwise>
				all fixed!
				</c:otherwise>
	  		</c:choose>
			<br/>${errorType.message}
		</li>
		<ul class="hidden" id="result${rstatus.index}type${estatus.index}">
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

<jsp:include page="footer.jsp" />

<script>
$(function() {       
    $('.hider').on('click', function(){
        var $hider = $(this);
        var hideeid = $hider.attr('hide-id');
        var $hidee = $('#' + hideeid);
        $hidee.toggleClass('hidden');
        return false; /*prevent following the fake link*/
    });    
});
</script>
</body>
</html>
