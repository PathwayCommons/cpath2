<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<h2>Hello cPath2!</h2>
<div>
<br/>
<c:url var="goSearch" value="/cpath/search" />
<a href='<c:out value="${goSearch}"/>'>Search</a>
<br/>
</div>
</body>
</html>
