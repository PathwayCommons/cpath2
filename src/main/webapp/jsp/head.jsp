<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="author" content="${cpath.name}" />
<meta name="description" content="${cpath.name} web services, version ${cpath.version}, 
powered by cPath2 software, version @project.version@" />
<meta name="keywords" content="${cpath.name}, cPath2, cPathSquared, web service, 
BioPAX, pathway, network, biological pathways, bioinformatics, computational biology, 
pathway commons, baderlab, toronto, mskcc, cbio, graph query, data integration, 
biologicaal networks, ontology, knowledge, analysis, cancer research, systems biology" />

<link href="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet">
<%--<link href="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css"  rel="stylesheet">--%>
<link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.5.4/bootstrap-select.min.css" rel="stylesheet" />
<link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-switch/3.0.2/css/bootstrap3/bootstrap-switch.min.css" rel="stylesheet" />

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<script src="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-switch/3.0.2/js/bootstrap-switch.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.5.4/bootstrap-select.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-placeholder/2.0.9/jquery.placeholder.min.js"></script>
<script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular.min.js"></script>
<script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular-route.min.js"></script>

<link href="<spring:url value='/css/pc.css'/>" rel="stylesheet" />
<script src="<spring:url value='/scripts/pc.js'/>"></script>
