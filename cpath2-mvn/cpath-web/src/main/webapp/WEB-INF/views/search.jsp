<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<h1>Search Paxtools</h1>

<c:if test="${not empty statusMessageKey}">
    <p><fmt:message key="${statusMessageKey}"/></p>
</c:if>

<c:url var="url" value="/cpath/search" />
<form:form action="${url}">
  <table>
    <tr>
      <td>Search:</td>
      <td>
       <input name= "queryString" type="text"/>
      </td>
    </tr>
  </table>
  <br />
  <input type="submit" name="search" value="Search"/>
</form:form>