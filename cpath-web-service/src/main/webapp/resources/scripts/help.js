$(document).ready(function() {
	getDatasources();
	getCommandParameterDetails("help/formats", "output_parameter");
    getCommandParameterDetails("help/kinds", "graph_parameter");
    getCommandParameterDetails("help/directions", "direction_parameter");
    getCommandParameterDetails("help/types", "biopax_parameter");
    getCommandParameterDetails("help/types/properties", "properties_parameter");
    getCommandParameterDetails("help/types/inverse_properties", "inverse_properties_parameter");
});

// This function creates a body for the given command parameter output.
//
// @param helpWSPath - path to web service call
// @param id - the id of the list ('ul') in the DOM
function getCommandParameterDetails(helpWSPath, id) {
	// get some var values from jsp page
	var base = $('#cpath2_endpoint_url').text();
    // setup content element and append header
    $('#' + id).empty();
    //$("." + class_name).append('<ul style="margin-bottom: 1px;">');
    // iterate over results (as json) from web service call
    $.getJSON(base + helpWSPath, function(help) {
            $.each(help.members, function(idx, member) {
                    if(helpWSPath.indexOf("organism") != -1 
                    		|| helpWSPath.indexOf("kind") != -1 
                    		|| helpWSPath.indexOf("formats") != -1
                    		|| helpWSPath.indexOf("datasource") != -1
                    		){
                    	$("#"+id).append('<li style="margin-left: 2em;">' 
                    		+'<em>'+member.id+'</em> - '+member.info+'</li>');
                    }else {
                    	$("#"+id).append('<li style="margin-left: 2em;">'+member.id+'</li>');
                    }       
            });
    });
}
function getDatasources(){
	// get some var values from jsp page
	var base = $('#cpath2_endpoint_url').text();
    // call the ws and interate over JSON results
    $.getJSON(base + "metadata/datasources", function(datasources){
        $('#pathway_datasources').empty();
        $('#warehouse_datasources').empty();                
        $.each(datasources, function(idx, ds){
    		var tdid = ds.identifier;
    		var displayName = ds.name[0];
    		var otherNames = (ds.name.length > 1) ? " ("+ds.name.slice(1).join(", ")+")" : "";         	
        	if (ds.notPathwaydata) {
        		$('#warehouse_datasources').append(
        			'<dt>'
                    +'<a href="'+ds.urlToHomepage+'">'
                    +'<img src="data:image/gif;base64,'+ds.icon+'" title="'+displayName+' logo"></img>'
                    +'</a><strong> '+displayName+otherNames+'</strong>; type: '+ds.type
                    +'</dt>');
        		$('#warehouse_datasources').append('<dd><p>'+ds.description+'</p></dd>');
        	}else{               		    		
        		$('#pathway_datasources').append(
        			'<dt>'
        			+'<a href="'+ds.urlToHomepage+'">'
        			+'<img src="data:image/gif;base64,'+ds.icon+'" title="'+displayName+' logo"></img>'
        			+'</a><strong> '+displayName+otherNames + '</strong>; data format: '+ds.type+'; uri: '+ds.uri
        		+ '</dt>');
        		$('#pathway_datasources').append(
        			'<dd><p>'+ds.description+'. Adding this data to the system resulted in <strong>' 
           			+ds.counts[0]+'</strong> pathways, <strong>' 
           			+ds.counts[1]+'</strong> interactions and <strong>' 
           			+ds.counts[2]+'</strong> physical entities.</dd></p>'
           		);
        	}
        });
    });
}
function switchit(list) {
	var listElementStyle = document.getElementById(list).style;
	if (listElementStyle.display == "none") {
		listElementStyle.display = "block";
	} else {
		listElementStyle.display = "none";
	}
}
