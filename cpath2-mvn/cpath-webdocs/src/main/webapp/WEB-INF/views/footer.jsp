<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div id="footer">
  <p>
    <table width=100%>
      <tr>
        <td valign=top><fmt:message key="cpath2.footer_text1"/>
          <a href="http://www.cbio.mskcc.org"><fmt:message key="cpath2.mskcc"/></a>
          <fmt:message key="cpath2.footer_text2"/> <a href="http://baderlab.org/"><fmt:message key="cpath2.UofT"/></a>.
        </td>
        <td valign=top align="right">Powered by <a href="http://code.google.com/p/pathway-commons/">cPath2</a> v${project.version}
        </td>
    </table>
  </p>
</div>
