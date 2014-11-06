/* In fact, at the moment, simple tests here are for cpath2 web service testing 
 * rather than for the Javascript code (so, this is sort of temporary hack 
 * misusing the QUnit framework...; this must be done in integration tests, 
 * using, e.g., cpath-clinet, or better tests could be generated with Selenium...)
 * */
$(function() {
	//to prevent adding '[]' to a request param. name when an array is the values
	
	test("Example test", 1, function() {
		ok(true, "this test is fine");
	});	
	
	
	module("Module: non-public server-side API");
	
	var urls = ['favicon.ico','datasources','log','log/ips'];
	test('Test if paths exist', urls.length, function() {
	    for (var i = 0; i < urls.length; i++) {
	        var url = urls[i];
	        stop();
	        $.get(url, function() {
				ok(true, "success");
				start();
			});
	    }
	});
	
	
	module("Module: server-side API");
	
	test("Test 'help/types' call", 1, function() {
		var help = $.getJSON("help/types");
		ok(help, "success");
	});
	
	
	test("Test GET graph (neighborhood, using three UniProt IDs)", 1, function() {
		jQuery.ajaxSettings.traditional = true;
		var res = $.get("graph", {"kind":"neighborhood", "source":["P01732","O14508","P07766"]});
		ok(res, "success");
	});	
	
	
	test("Test 10 search queries (~5/sec)", 10, function() {		
		/* do not run too many in parallel (or IP address will be blacklisted) */
		for(var i=0;i<10;i++) {
			// - without foo:i arg, the query is actually sent only once, ignoring the rest
			var data = {"type":"xref", "q":'xrefid:"P01732" xrefid:"O14508" '
				+ 'xrefid:"P07766" xrefid:"P16410" xrefid:"Q99731" xrefid:"P12755" '
				+ 'xrefid:"P25942" xrefid:"P01137" xrefid:"P29400" '
				+ 'xrefid:"P42081" xrefid:"P51679" xrefid:"Q07812" '
				+ 'xrefid:"Q9Y6W8" xrefid:"Q9Y5U5" xrefid:"P25445" xrefid:"P10747"', "foo":i+""};
			jQuery.ajaxSettings.traditional = true;
			setTimeout(execGet("search", data), 200);
		}
	});
	
	test("Test 5 neighborhood graph queries (POST ~1/sec, 16 UniProt IDs)", 5, function() {
		for(var i=0;i<5;i++) {	
			// - without foo:i arg, the query is actually sent only once, ignoring the rest
			var data = {"kind":"neighborhood", "source":["P01732","O14508","P07766","P16410",
			                                               "Q99731","P12755","P25942","P01137", 
			                                               "P29400","P42081","P51679","Q07812",
			                                               "Q9Y6W8","Q9Y5U5","P25445","P10747"], "foo":i+""};
			jQuery.ajaxSettings.traditional = true;
			setTimeout(execPost("graph", data), 1000);
		}
	});
	
	
	function execGet(url, data) {
		stop();
		var res = $.get(url, data);
		ok(res, "success");//TODO check for more details (error 500 or 460 vs. result found)
		start();
	}
	
	function execPost(url, data) {
		stop();
		var res = $.post(url, data);
		ok(res, "success");//TODO check for more details
		start();
	}

});