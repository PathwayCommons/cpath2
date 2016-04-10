<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

</div><!--closing tag for the common #content .container, opened in the header.jsp-->

<footer class="footer">
	<div id="footer_navbar" class="navbar navbar-default navbar-fixed-bottom">
          <div class="container">
			  <p class="navbar-text navbar-left"> <%--project.version is a Maven var.--%>
				<small>Powered by
				<a target="_blank" href="https://pathwaycommons.github.io/cpath2/">cPath2</a> v${project.version}
				&nbsp;(<span class="badge alert-info pc2_tip"></span> unique users
				queried ok <span class="badge alert-info pc2_tok"></span> times).
				&copy; 2006-2016 <a href="http://baderlab.org/" target="_blank">Bader Lab</a> (UofT),
				<a href="http://www.sanderlab.org" target="_blank">cBio</a> (MSKCC; DFCI, HMS)
				and Demir Lab (OHSU).</small>
			  </p>
		 </div>
	</div>	
</footer>

<script>
    // update the number of successful requests (excluding errors);
    $.getJSON('log/totalok', function(tok) {$('.pc2_tok').text(tok);}).error(function() {$('.pc2_tok').text(0);});
    // update the number of unique client IPs;
    $.getJSON('log/totalip', function(tip) {$('.pc2_tip').text(tip);}).error(function() {$('.pc2_tip').text(0);});
</script>
