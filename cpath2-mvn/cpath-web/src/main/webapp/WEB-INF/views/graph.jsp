<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<h1>Test Graph Query</h1>

<form:form action="/cpath-web-service/graph">
    <table>
    <tr>
      <td>Graph</td>
      <td>
       <input name= "kind" type="text" value="${kind}"/>
      </td>
    </tr>
      <tr>
      <td>Format</td>
      <td>
       <input name= "format" type="text" value="${format}"/>
      </td>
    </tr>
    <tr>
      <td>Source nodes</td>
      <td>
       <input name= "source" type="text" value="${source}"/>
      </td>
    </tr>
      <tr>
      <td>Destination nodes</td>
      <td>
       <input name= "dest" type="text" value="${dest}"/>
      </td>
    </tr>
  </table>
  <br />
  <input type="submit" name="search" value="Search"/>
</form:form>
