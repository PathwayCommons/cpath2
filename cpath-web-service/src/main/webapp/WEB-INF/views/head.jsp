<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="author" content="${cpath.name}" />
<meta name="description" content="${cpath.name} web services, version ${cpath.version}, 
powered by cPath2 software, version ${project.version}" />
<meta name="keywords" content="${cpath.name}, cPath2, cPathSquared, web service, 
BioPAX, pathway, network, biological pathways, bioinformatics, computational biology, 
pathway commons, baderlab, toronto, mskcc, cbio, graph query, data integration, 
biologicaal networks, ontology, knowledge, analysis, cancer research, systems biology" />

<link href="<spring:url value='/resources/css/bootstrap.min.css'/>" rel="stylesheet" />
<%-- <link href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet"> --%>
<link href="<spring:url value='/resources/css/bootstrap-select.min.css'/>" rel="stylesheet" />
<link href="<spring:url value='/resources/css/bootstrap-switch.min.css'/>" rel="stylesheet" />
<link href="<spring:url value='/resources/css/pc.css'/>" rel="stylesheet" />

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<%-- <script src="<spring:url value='/resources/scripts/jquery.min.js'/>"></script> --%>
<%-- <script src="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script> --%>
<script src="<spring:url value='/resources/scripts/bootstrap.min.js'/>"></script>
<script src="<spring:url value='/resources/scripts/bootstrap-switch.min.js'/>"></script>
<script src="<spring:url value='/resources/scripts/bootstrap-select.min.js'/>"></script>
<script src="<spring:url value='/resources/scripts/jquery.placeholder.js'/>"></script>

<%-- <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular.min.js"></script> --%>
<script src="<spring:url value='/resources/scripts/angular.js'/>"></script>
<%-- <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular-route.min.js"></script> --%>
<script src="<spring:url value='/resources/scripts/angular-route.js'/>"></script>

<!--[if lt IE 8]>
      <script src="<spring:url value='/resources/scripts/icon-font-ie7.js'/>"></script>
      <script src="<spring:url value='/resources/scripts/lte-ie7-24.js'/>"></script>
<![endif]-->
<%-- HTML5 shim, for IE6-8 support of HTML5 elements --%>
<!--[if lt IE 9]>
	<script src="<spring:url value='/resources/scripts/html5shiv.js'/>"></script>
<![endif]-->

<script src="<spring:url value='/resources/scripts/pc.js'/>"></script>

<script>
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
    ga('create', '${cpath.ga}', 'auto');
    ga('send', 'pageview');
</script>
