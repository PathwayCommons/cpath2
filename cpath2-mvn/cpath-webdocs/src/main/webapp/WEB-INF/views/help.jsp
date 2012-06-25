<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2::Demo (version ${project.version})" />
	<meta name="keywords" content="Pathway Commons, cPath2, cPathSquared webservice, help, demo, documentation" />
	<title>cPath2::Demo</title>
	<!-- JQuery plugins -->
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-1.6.1.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.min.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.css" />" type="text/css"/>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery.ui.override.css" />" type="text/css"/>
	<!-- other -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	<!-- cpath2 js code -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/help.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/andreas08.css" />" type="text/css" media="screen,projection" />
</head>

<body>
  <!-- set some variables to string constants from messages.properties -->
  <p>
  <span id="web_service_url" style="display:none"><fmt:message key="cpath2.url"/></span>
  <span id="command_header_additional_parameters" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters"/></span>
  <span id="command_header_additional_parameters_output" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_output"/></span>
  <span id="command_header_additional_parameters_output_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_output_desc"/></span>
  <span id="command_header_additional_parameters_graph" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_graph"/></span>
  <span id="command_header_additional_parameters_graph_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_graph_desc"/></span>
  <span id="command_header_additional_parameters_direction" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_direction"/></span>
  <span id="command_header_additional_parameters_direction_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_direction_desc"/></span>
  <span id="command_header_additional_parameters_datasource" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_datasource"/></span>
  <span id="command_header_additional_parameters_datasource_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_datasource_desc"/></span>
  <span id="command_header_additional_parameters_organism" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_organism"/></span>
  <span id="command_header_additional_parameters_organism_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_organism_desc"/></span>
  <span id="command_header_additional_parameters_biopax" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_biopax"/></span>
  <span id="command_header_additional_parameters_biopax_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_biopax_desc"/></span>
  <span id="command_header_additional_parameters_properties" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_properties"/></span>
  <span id="command_header_additional_parameters_properties_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_properties_desc"/></span>
  <span id="command_header_additional_parameters_inverse_properties" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_inverse_properties"/></span>
  <span id="command_header_additional_parameters_inverse_properties_desc" style="display:none"><fmt:message key="cpath2.command_header_additional_parameters_inverse_properties_desc"/></span>
  </p>
<!-- place the content -->
  <div id="container">
   <jsp:include page="header.jsp" flush="true"/>
    <div id="content">
      <jsp:include page="feedback.jsp" flush="true"/>
	  <!-- description of cpath2 -->
      <h2><fmt:message key="cpath2.welcome"/></h2>
      <p><fmt:message key="cpath2.description"/></p>
	  <p><fmt:message key="cpath2.license_terms"/></p>
	  <!-- start of web service api documentation -->
      <h2><fmt:message key="cpath2.web_service_title"/></h2>
      <p><fmt:message key="cpath2.web_service_sub_title"/></p>
	  <!-- list of web service commands -->
	  <ol>
		<li><a href="#search"><fmt:message key="cpath2.command_header_search"/></a></li>
		<li><a href="#get"><fmt:message key="cpath2.command_header_get"/></a></li>
		<li><a href="#graph"><fmt:message key="cpath2.command_header_graph"/></a></li>
		<li><a href="#traverse"><fmt:message key="cpath2.command_header_traverse"/></a></li>
		<li><a href="#top_pathways"><fmt:message key="cpath2.command_header_tp"/></a></li>
		<li><a href="#help"><fmt:message key="cpath2.command_header_help"/></a></li>
	  </ol>
		<a href="#additional_parameters"><fmt:message key="cpath2.command_header_additional_parameters"/></a><br/>
		<a href="#errors"><fmt:message key="cpath2.error_code_header"/></a><br/>
	  <br/>
	  
	  <!-- URIs -->
      <h3><a name="miriam"></a><fmt:message key="cpath2.uri_title"/></h3>
	  <p><fmt:message key="cpath2.uri_description"/></p>
	  <h3><a name="enco"></a><fmt:message key="cpath2.encoding_title"/></h3>
	  <p><fmt:message key="cpath2.encoding_description"/></p>
	 
	  <!-- command bodies -->
	 
	 <ol> 
	  
	  <!-- search command -->
	  <li>
	  <h2><a name="search"></a><fmt:message key="cpath2.command_header_search"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_search_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_search_query_parameter"/></li>
		<li><fmt:message key="cpath2.command_search_page_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_datasource_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_organism_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_biopax_class_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_search_output1"/><a href="resources/schemas/cpath2.xsd.txt"><fmt:message key="cpath2.command_search_response_schema"/></a><fmt:message key="cpath2.command_search_output2"/><br/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <br/>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/search.xml?q=Q06609"><fmt:message key="cpath2.example.search1"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=xrefid:Q06609"><fmt:message key="cpath2.example.search2"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search.json?q=Q06609"><fmt:message key="cpath2.example.search3"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search.json?q=Q06609&type=pathway"><fmt:message key="cpath2.example.search4"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=brca2&type=proteinreference&organism=homo%20sapiens&datasource=pid"><fmt:message key="cpath2.example.search5"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=brc*&type=control&organism=9606&datasource=reactome"><fmt:message key="cpath2.example.search6"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=a*&page=3"><fmt:message key="cpath2.example.search7"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=+binding%20NOT%20transcription*&type=control&page=0"><fmt:message key="cpath2.example.search8"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=pathway:immune&type=conversion"><fmt:message key="cpath2.example.search9"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=*&type=pathway&datasource=panther"><fmt:message key="cpath2.example.search10"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/search?q=*&type=biosource"><fmt:message key="cpath2.example.search11"/></a></li>
	  </ol>
	  </li>
	  <!-- get command -->
	  <li>
	  <h2><a name="get"></a><fmt:message key="cpath2.command_header_get"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_get_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_get_uri_parameter"/></li>
		<li><fmt:message key="cpath2.command_header_format_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_get_output"/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3><br/>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/get?uri=urn:miriam:uniprot:Q06609">
		<fmt:message key="cpath2.example.get1"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/get?uri=http:%2F%2Fwww.reactome.org%2Fbiopax/48887Pathway137">
		<fmt:message key="cpath2.example.get2"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/get?uri=http://pid.nci.nih.gov/biopaxpid_55597&format=BINARY_SIF">
		<fmt:message key="cpath2.example.get3"/></a>
	  </li>
	 </ol>
	 </li>
	  
	  <!-- graph command -->
	  <li>
	  <h2><a name="graph"></a><fmt:message key="cpath2.command_header_graph"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_graph_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_graph_kind_parameter"/></li>
		<li><fmt:message key="cpath2.command_graph_source_parameter"/></li>
		<li><fmt:message key="cpath2.command_graph_target_parameter"/></li>
        <li><fmt:message key="cpath2.command_graph_direction_parameter"/></li>
        <li><fmt:message key="cpath2.command_graph_limit_parameter"/></li>
        <li><fmt:message key="cpath2.command_header_format_parameter"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_graph_output"/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <fmt:message key="cpath2.command_header_graph_query_description"/><br/>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/graph?source=http:%2F%2Fwww.reactome.org%2Fbiopax%2F48892Protein3105&kind=neighborhood">
		<fmt:message key="cpath2.example.graph1"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/graph?source=urn:miriam:uniprot:O88207&kind=neighborhood">
		<fmt:message key="cpath2.example.graph2"/></a>
	  </li>
	  	  <li><a href="<fmt:message key="cpath2.url"/>/graph?source=urn:miriam:uniprot:O88207&kind=neighborhood&format=EXTENDED_BINARY_SIF">
		<fmt:message key="cpath2.example.graph3"/></a>
	  </li>
	  </ol>
	  </li>
	  
	  <!-- traverse command -->
	  <li>
	  <h2><a name="traverse"></a><fmt:message key="cpath2.command_header_traverse"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_traverse_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
	  <ul>
		<li><fmt:message key="cpath2.command_traverse_uri_param"/></li>
		<li><fmt:message key="cpath2.command_traverse_path_param"/></li>
	  </ul>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_search_output1"/><a href="resources/schemas/cpath2.xsd.txt"><fmt:message key="cpath2.command_search_response_schema"/></a><fmt:message key="cpath2.command_traverse_output2"/><br/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <fmt:message key="cpath2.command_header_traverse_query_description"/><br/>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=urn:miriam:uniprot:P38398&path=ProteinReference/organism/displayName">
		<fmt:message key="cpath2.example.traverse1"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=urn:miriam:uniprot:P38398&uri=urn:miriam:uniprot:Q06609&path=ProteinReference/organism/">
		<fmt:message key="cpath2.example.traverse2"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=urn:miriam:uniprot:Q06609&path=ProteinReference%2fentityReferenceOf:Protein%2fname">
		<fmt:message key="cpath2.example.traverse3"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=urn:miriam:uniprot:P38398&path=ProteinReference%2fentityReferenceOf:Protein/">
		<fmt:message key="cpath2.example.traverse4"/></a>
	  </li>	
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=urn:miriam:uniprot:P38398&uri=http://pid.nci.nih.gov/biopaxpid_68613&uri=http://www.reactome.org/biopax/48887Protein2992&uri=urn:miriam:taxonomy:9606&path=Named%2fname">
		<fmt:message key="cpath2.example.traverse5"/></a>
	  </li>		  
	  <li><a href="<fmt:message key="cpath2.url"/>/traverse?uri=http://pid.nci.nih.gov/biopaxpid_55597&path=Pathway%2fpathwayComponent:Interaction/participant/displayName">
		<fmt:message key="cpath2.example.traverse6"/></a>
	  </li>	
	  </ol>	
	  </li>

	  <!-- top_pathways command -->
	  <li>
	  <h2><a name="top_pathways"></a><fmt:message key="cpath2.command_header_tp"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_tp_summary"/>
	  <h3><fmt:message key="cpath2.command_header_parameters"/></h3>
		no parameters
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_search_output1"/><a href="resources/schemas/cpath2.xsd.txt">
	  <fmt:message key="cpath2.command_search_response_schema"/></a><fmt:message key="cpath2.command_tp_output2"/><br/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/top_pathways">
		<fmt:message key="cpath2.example.top_pathways1"/></a>
	  </li>
	  <li><a href="<fmt:message key="cpath2.url"/>/top_pathways.json">
		<fmt:message key="cpath2.example.top_pathways2"/></a>
	  </li>
	  </ol>
	  </li>	

	  <!-- help command -->
	  <li>
	  <h2><a name="help"></a><fmt:message key="cpath2.command_header_help"/></h2>
	  <h3><fmt:message key="cpath2.command_header_summary"/></h3>
	  <fmt:message key="cpath2.command_help_summary"/>
	  <h3><fmt:message key="cpath2.command_header_output"/></h3>
	  <fmt:message key="cpath2.command_help_output"/><a href="resources/schemas/cpath2.xsd.txt"><fmt:message key="cpath2.command_search_response_schema"/></a><br/>
	  <h3><fmt:message key="cpath2.command_header_query"/></h3>
	  <br/>
	  <ol>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/commands"><fmt:message key="cpath2.example.help1"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/commands.json"><fmt:message key="cpath2.example.help12"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/commands/search"><fmt:message key="cpath2.example.help11"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/types"><fmt:message key="cpath2.example.help2"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/kinds"><fmt:message key="cpath2.example.help3"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/organisms"><fmt:message key="cpath2.example.help4"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/datasources"><fmt:message key="cpath2.example.help9"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/directions"><fmt:message key="cpath2.example.help5"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/types/properties"><fmt:message key="cpath2.example.help6"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/types/provenance/properties"><fmt:message key="cpath2.example.help7"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help/types/inverse_properties"><fmt:message key="cpath2.example.help8"/></a></li>
	  <li><a href="<fmt:message key="cpath2.url"/>/help"><fmt:message key="cpath2.example.help0"/></a></li>
	  </ol>
	  </li>
	  
	  </ol>
	<br/>
	
	  <!-- additional parameter details -->
      <h2><a name="additional_parameters"></a><fmt:message key="cpath2.command_header_additional_parameters"/>:</h2>
	  <pre id="output_parameter" style="text-align: left;"></pre>
	  <pre id="graph_parameter" style="text-align: left;"></pre>
	  <pre id="direction_parameter" style="text-align: left;"></pre>
	  <pre id="datasource_parameter" style="text-align: left;"></pre>
	  <pre id="biopax_parameter" style="text-align: left;"></pre>
	  <pre id="properties_parameter" style="text-align: left;"></pre>
	  <pre id="inverse_properties_parameter" style="text-align: left;"></pre>
	  
	  <!-- error codes -->
	  <h2><a name="errors"></a><fmt:message key="cpath2.error_code_header"/>:</h2>
	  <p><fmt:message key="cpath2.error_code_description"/><a href="resources/schemas/cpath2.xsd.txt">
	  <fmt:message key="cpath2.command_search_response_schema"/></a></p>
	  <p><fmt:message key="cpath2.error_code_footnote"/></p>
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
	  <br/>
</div>

   <jsp:include page="footer.jsp" flush="true"/>
  </div>
</body>

</html>
