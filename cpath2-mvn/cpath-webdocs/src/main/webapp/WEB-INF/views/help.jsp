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
	<script type="text/javascript" src="<c:url value="/resources/jquery-1.5.1.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/json.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/help.js" />"></script>
</head>
<body>
	<h2><fmt:message key="cpath2.welcome"/> <fmt:message key="cpath2.provider"/></h2>

	<!-- this label is used in the javascript as a constant (to get the cPath2 ws base URL) -->
	<p>
		(<span id="wsroot" ><fmt:message key="cpath2.url"/></span>)
	</p>
	<div style="float: left;">
		<div style="padding-bottom: 1em;">
		<h2 id="title"></h2>
		<label for="prev">back to: </label><a id="prev" href="#">help</a>
		</div>
		<div id="content" style="float: none;">
			<pre id="info" style="text-align: left;"></pre><br />
			<label for="example">Example: </label><a id="example" /><br /> 
		</div>
		<br />
	</div>
	
	<div id="tree" style="float: right; padding: 2em;">
		<em>TODO: navigation (a tree?)</em><br />
	</div>
	
	<!-- 
	<div id="mask" style="display: none;"></div>
	<div id="popup" style="display: none;">
		<div class="span-8 last">
			<h3>Example</h3>
			<form>
				<fieldset>
					<p>
						<label for="result" /><br />
						<textarea id="result" style="display: none;" readonly="readonly" />
					</p>
				</fieldset>
			</form>
			<a href="#" onclick="closePopup();">Close</a>
		</div>
	</div>
	-->

</body>
</html>
