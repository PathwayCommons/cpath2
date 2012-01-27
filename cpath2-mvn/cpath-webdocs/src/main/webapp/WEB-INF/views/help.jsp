<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="Pathway Commons-2::Home" />
	<meta name="keywords" content="Pathway Commons, cPath2, webservice, help, documentation" />
	<title>Pathway Commons-2::Home</title>
	<!-- JQuery plugins -->
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-1.6.1.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.min.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.css" />" type="text/css"/>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery.ui.override.css" />" type="text/css"/>
	<!-- other -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	<!-- pathwaycommons js code -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/help.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/andreas08.css" />" type="text/css" media="screen,projection" />
</head>

<body>
  <!-- set some variables to string constants from web.properties & messages.properties files -->
  <span id="web_service_url" style="display:none"><fmt:message key="cpath2.url"/></span>
  <span id="command_header_additional_parameters" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters"/></span>
  <span id="command_header_additional_parameters_output" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_output"/></span>
  <span id="command_header_additional_parameters_output_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_output_desc"/></span>
  <span id="command_header_additional_parameters_graph" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_graph"/></span>
  <span id="command_header_additional_parameters_graph_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_graph_desc"/></span>
  <span id="command_header_additional_parameters_datasource" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_datasource"/></span>
  <span id="command_header_additional_parameters_datasource_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_datasource_desc"/></span>
  <span id="command_header_additional_parameters_organism" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_organism"/></span>
  <span id="command_header_additional_parameters_organism_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_organism_desc"/></span>
  <span id="command_header_additional_parameters_biopax" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_biopax"/></span>
  <span id="command_header_additional_parameters_biopax_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_biopax_desc"/></span>
<!-- place the content -->
  <div id="container">
   <jsp:include page="header.jsp" flush="true"/>
    <div id="content">
      <jsp:include page="feedback.jsp" flush="true"/>
	  <!-- description of cpath2 -->
      <h2><fmt:message key="cpath2.welcome"/></h2>
      <p><fmt:message key="cpath2.description"/></p>
	  <ul>
		<li><fmt:message key="cpath2.comp_biologists"/></li>
		<li><fmt:message key="cpath2.software_developers"/></li>
	  </ul>
	  <p><fmt:message key="cpath2.license_terms"/></p>
	  <!-- start of web service api documentation -->
      <h2><fmt:message key="cpath2.web_service_title"/></h2>
      <p><fmt:message key="cpath2.web_service_sub_title"/></p>
	  <!-- list of web service commands -->
	  <ul>
		<li><a href="#find"><fmt:message key="cpath2.command_header_find"/></a></li>
		<li><a href="#get"><fmt:message key="cpath2.command_header_get"/></a></li>
		<li><a href="#graph"><fmt:message key="cpath2.command_header_graph"/></a></li>
		<li><a href="#additional_parameters"><fmt:message key="cpath2.command_header_additional_parameters"/></a></li>
		<li><a href="#errors"><fmt:message key="cpath2.error_code_header"/></a></li>
	  </ul>
	  <!-- miriam -->
      <h3><a NAME="miriam"></a><fmt:message key="cpath2.miriam_title"/></h3>
	  <p><fmt:message key="cpath2.miriam_description"/></p>
	  <!-- command bodies -->
	  <!-- find command -->
	  <h2><a NAME="find"></a><fmt:message key="cpath2.command_header_find"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_find_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_find_query_parameter"/></li>
		<li><fmt:message key="cpath2.command_find_page_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_datasource_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_organism_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_biopax_class_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_find_output1"/><a href=<fmt:message key="cpath2.command_search_response_schema_url"/>><fmt:message key="cpath2.command_search_response_schema"/></a><fmt:message key="cpath2.command_find_output2"/><br/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <fmt:message key="cpath2.command_header_query_description"/><br>
	  <br>
	  <ol>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_1_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_1_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_2_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_2_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_3_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_3_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_4_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_4_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_5_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_5_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_6_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_6_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_7_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_find_example_query_7_url"/></a>
	  </li>
	  </ol>
	  <!-- get command -->
	  <h2><a NAME="get"></a><fmt:message key="cpath2.command_header_get"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_get_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_get_miriam_id_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_format_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_get_output"/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <fmt:message key="cpath2.command_header_query_description"/><br>
	  <ol>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_1_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_1_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_2_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_2_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_3_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_get_example_query_3_url"/></a>
	  </li>
	 </ol>
	  <!-- graph command -->
	  <h2><a NAME="graph"></a><fmt:message key="cpath2.command_header_graph"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_graph_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_graph_kind_parameter"/></li>
		<li><fmt:message key="cpath2.command_graph_source_parameter"/></li>
		<li><fmt:message key="cpath2.command_graph_target_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_format_parameter"/></li>
		<li><fmt:message key="cpath2.command_graph_limit_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_graph_output"/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <fmt:message key="cpath2.command_header_query_description"/><br>
	  <fmt:message key="cpath2.command_header_graph_query_description"/><br>
	  <ol>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_1_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_1_url"/></a>
	  </li>
	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_2_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_2_url"/></a>
	  </li>
	  	  <li><a href=<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_3_url"/>>
		<fmt:message key="cpath2.url"/><fmt:message key="cpath2.command_graph_example_query_3_url"/></a>
	  </li>
	  </ol>
	  <!-- additional parameter details -->
      <h2><a name="additional_parameters"></a><fmt:message key="cpath2.command_header_additional_parameters"/>:</h2>
	  <pre id="output_parameter" style="text-align: left;"></pre>
	  <pre id="graph_parameter" style="text-align: left;"></pre>
	  <pre id="datasource_parameter" style="text-align: left;"></pre>
	  <pre id="organism_parameter" style="text-align: left;"></pre>
	  <pre id="biopax_parameter" style="text-align: left;"></pre>
	  <!-- error codes -->
	  <h2><a name="errors"></a><fmt:message key="cpath2.error_code_header"/>:</h2>
	  <p><a name="errors"></a><fmt:message key="cpath2.error_code_description"/></p>
	  <pre><a name="errors"></a><fmt:message key="cpath2.error_xml_format"/></pre>
	  <p><a name="errors"></a><fmt:message key="cpath2.error_code_footnote"/></p>
	  <!-- error codes table -->
	  <div>
		<table>
		  <tr><th><fmt:message key="cpath2.error_code_label"/></th><th><fmt:message key="cpath2.error_code_description_label"/></th></tr>
		  <tr><td><fmt:message key="cpath2.error_code_450"/></td><td><fmt:message key="cpath2.error_code_450_description"/></td></tr>
		  <tr><td><fmt:message key="cpath2.error_code_452"/></td><td><fmt:message key="cpath2.error_code_452_description"/></td></tr>
		  <tr><td><fmt:message key="cpath2.error_code_460"/></td><td><fmt:message key="cpath2.error_code_460_description"/></td></tr>
		  <tr><td><fmt:message key="cpath2.error_code_500"/></td><td><fmt:message key="cpath2.error_code_500_description"/></td></tr>
		</table>
	  </div>
	  <br>
</div>

   <jsp:include page="footer.jsp" flush="true"/>
  </div>
</body>

</html>
