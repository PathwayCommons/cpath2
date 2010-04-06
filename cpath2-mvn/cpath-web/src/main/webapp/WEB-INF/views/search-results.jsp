<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri = "http://java.sun.com/jsp/jstl/functions"%>

<c:if test="${fn:length(pathwayNames) > 0}">
<table> 
  <tr>
    <th>Pathways</th>
  </tr>
  <c:forEach items="${pathwayNames}" var="name">
    <tr>
	  <td>${name}</td>
	</tr>
  </c:forEach>
</table>
</c:if>

<c:if test="${fn:length(interactionNames) > 0}">
<table> 
  <tr>
    <th>Interactions</th>
  </tr>
  <c:forEach items="${interactionNames}" var="name">
    <tr>
	  <td>${name}</td>
	</tr>
  </c:forEach>
</table>
</c:if>

<c:if test="${fn:length(proteinNames) > 0}">
<table> 
  <tr>
    <th>Proteins</th>
  </tr>
  <c:forEach items="${proteinNames}" var="name">
    <tr>
	  <td>${name}</td>
	</tr>
  </c:forEach>
</table>
</c:if>

<c:if test="${fn:length(otherNames) > 0}">
<table> 
  <tr>
    <th>Other (Named) Entities</th>
  </tr>
  <c:forEach items="${otherNames}" var="name">
    <tr>
	  <td>${name}</td>
	</tr>
  </c:forEach>
</table>
</c:if>
