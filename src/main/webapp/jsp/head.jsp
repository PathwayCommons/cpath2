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
biological networks, ontology, knowledge, analysis, cancer research, systems biology" />

<link href="https://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet">
<link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.5.4/bootstrap-select.min.css" rel="stylesheet" />
<link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-switch/3.0.2/css/bootstrap3/bootstrap-switch.min.css" rel="stylesheet" />

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js" integrity="sha384-/Gm+ur33q/W+9ANGYwB2Q4V0ZWApToOzRuA8md/1p9xMMxpqnlguMvk8QuEFWA1B" crossorigin="anonymous"></script>
<script src="https://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js" integrity="sha384-oFMgcGzKX7GaHtF4hx14KbxdsGjyfHK6m1comHjI1FH6g4m6qYre+4cnZbwaYbHD" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-switch/3.0.2/js/bootstrap-switch.min.js" integrity="sha384-TPJ34L4PLYJdJzijqOUEsP/LhDlrfsa+VzUjYMpDg//9TbOLzQO6eZvxSNtiTf7X" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.5.4/bootstrap-select.min.js" integrity="sha384-Qroo7RDDdRY4a2bwWzMpjQ8IPglEYhwmyUOQD6jmfx6aO3+gF5Th0TjCiuFp3Yiv" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-placeholder/2.0.9/jquery.placeholder.min.js" integrity="sha384-t/2aZFgtJsi+k+Gng+Az/BS/xcnedXln8Vo1XuN3C7DKApynLwLNxy6lbzhPDbdb" crossorigin="anonymous"></script>
<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.32/angular.min.js" integrity="sha384-wIkUlWqDZgGRTyTEgoO743so47Fga6iCbEGBflBzRLrgzX2at1upKTOfZnlOP+kn" crossorigin="anonymous"></script>
<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.32/angular-route.min.js" integrity="sha384-qxToRe/Fr1X0dfqb/xr9Fqxo4ZlV/geNMAAjC3FFc+zW5Hq5aXgCF5YN4dKt4Dmz" crossorigin="anonymous"></script>

<link href="<spring:url value='/css/pc.css'/>" rel="stylesheet" />
<script src="<spring:url value='/scripts/pc.js'/>"></script>
