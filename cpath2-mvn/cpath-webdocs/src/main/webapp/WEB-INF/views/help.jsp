<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 help" />
	<meta name="keywords" content="cPath2, webservice, help, documentation" />
	<title>cPath2 Help</title>
	<script type="text/javascript" src="<c:url value="/resources/scripts/jquery-1.5.1.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/help.js" />"></script>
  	<link rel="stylesheet" href="<c:url value="/resources/css/andreas08.css" />" type="text/css" media="screen,projection" />
</head>

<body>
  <!-- set some variables to string constants from web.properties & messages.properties files -->
  <span id="web_service_url" style="display:none"><fmt:message key="cpath2.url"/></span>
  <span id="command_header_prefix" style="display:none"><fmt:message key="cpath2.command_header_prefix"/></span>
  <span id="command_header_summary" style="display:none"><fmt:message key="cpath2.command_header_summary"/></span>
  <span id="command_header_parameters" style="display:none"><fmt:message key="cpath2.command_header_parameters"/></span>
  <span id="command_header_output" style="display:none"><fmt:message key="cpath2.command_header_output"/></span>
  <span id="command_header_query" style="display:none"><fmt:message key="cpath2.command_header_query"/></span>
  <span id="command_header_query_footnote" style="display:none"><fmt:message key="cpath2.command_header_query_footnote"/></span>
  <span id="command_header_additional_parameters" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters"/></span>
  <span id="command_header_additional_parameters_output" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_output"/></span>
  <span id="command_header_additional_parameters_graph" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_graph"/></span>
  <span id="command_header_additional_parameters_datasource" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_datasource"/></span>
  <span id="command_header_additional_parameters_organism" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_organism"/></span>
  <span id="command_header_additional_parameters_biopax" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_biopax"/></span>
  <span id="command_header_additional_parameters_none" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_none"/></span>
 <!-- place the content -->
  <div id="container">
    <jsp:include page="header.jsp" flush="true"/>
    <div id="content">
      <jsp:include page="feedback.jsp" flush="true"/>
      <h1><fmt:message key="cpath2.welcome"/></h1>
      <br>
      <h1><fmt:message key="cpath2.web_service_title"/></h1>
      <p><fmt:message key="cpath2.web_service_sub_title"/></p>
	  <pre id="command_list" style="text-align: left;"></pre>
	  <pre id="command_bodies" style="text-align: left;"></pre>
      <h2><a name="additional_parameters"></a><fmt:message key="cpath2.command_header_additional_parameters"/>:</h2>
	  <pre id="output_parameter" style="text-align: left;"></pre>
	  <pre id="graph_parameter" style="text-align: left;"></pre>
	  <pre id="datasource_parameter" style="text-align: left;"></pre>
	  <pre id="organism_parameter" style="text-align: left;"></pre>
	  <pre id="biopax_parameter" style="text-align: left;"></pre>
      <br>
      <!--
	  <pre id="info" style="text-align: left;"></pre>
	  <label for="example">Example: </label><a id="example" />
      <p>
        <a href="http://awabi.cbio.mskcc.org/cpath2-docs/">(back to home)</a>
      </p>
      -->
  </div>
    <jsp:include page="footer.jsp" flush="true"/>
  </div>
</body>

</html>
