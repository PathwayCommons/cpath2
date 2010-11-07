<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<h2>This is cPath^2!</h2>
<div>
<br/>
<c:url var="goSearch" value="/search" />
<a href='<c:out value="${goSearch}"/>'>Search</a>
<br/>
<c:url var="hello" value="/hello" />
<a href='<c:out value="${hello}"/>'>Test (Hello)</a>
<br/>
</div>
</body>
</html>
