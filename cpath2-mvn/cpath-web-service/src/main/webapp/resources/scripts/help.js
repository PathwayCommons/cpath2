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
                    if (helpWSPath.indexOf("organism") != -1 
                    		|| helpWSPath.indexOf("kind") != -1 
                    		|| helpWSPath.indexOf("formats") != -1
                    		|| helpWSPath.indexOf("datasource") != -1
                    		) {
                    	$("#" + id).append('<li style="margin-left: 2em;">' 
                    		+ '<em>' + member.id + '</em> - ' + member.info + '</li>');
                    } else  {
                    	$("#" + id).append('<li style="margin-left: 2em;">' 
                    		+ member.id + '</li>');
                    }       
            });
    });
}

function getDatasources() {
	// get some var values from jsp page
	var base = $('#cpath2_endpoint_url').text();
    // call the ws and interate over JSON results
    $.getJSON(base + "metadata", function(datasources) {
        $('#datasources').empty();
        $('#datasources').append('<tr><th>Logo</th><th>Names (filter by)</th>'
        	+ '<th>Data Type and Version</th><th>Pathways</th><th>Interactions</th><th>Molecules (states)</th></tr>'); 
        $('#warehouse').empty();
        $('#warehouse').append("<tr>");
                
        $.each(datasources, function(idx, ds) {
        	if (ds.type.toLowerCase() == "biopax" || ds.type.toLowerCase() == "psi_mi") 
        	{               		
        		var tdid = ds.identifier+ds.version;
        		
        		$('#datasources').append('<tr>' 
           		+ '<td class="datasource_logo_table_cell"><a href="'+ds.urlToHomepage+'">'
           		+ '<img class="data_source_logo" src="data:image/gif;base64,' 
           		+ ds.icon + '" title="URI=' + ds.uri + '"></img></a></td>'
           		+ '<td class="datasource_logo_table_cell" style="white-space: nowrap">' + ds.name.split(";").join("<br/>")
           		+ '</td><td class="datasource_logo_table_cell" style="width: 35%"><em>' + ds.type + '</em><br/>' + ds.description
           		+ '</td><td id="' + tdid + '_pw" class="datasource_num_table_cell">'
           		+ '</td><td id="' + tdid + '_it" class="datasource_num_table_cell">'
           		+ '</td><td id="' + tdid + '_pe" class="datasource_num_table_cell">'
           		+ '</td></tr>');
        		
        		// get counts (async.)
        		$.getJSON(base + "/search.json?q=*&type=pathway&datasource=" + ds.uri, function(res) {
        			$('td#' + tdid + '_pw').text(res.numHits);
        		});				
        		$.getJSON(base + "/search.json?q=*&type=interaction&datasource=" + ds.uri, function(res) {
        			$('td#' + tdid + '_it').text(res.numHits);
        		});
        		$.getJSON(base + "/search.json?q=*&type=physicalentity&datasource=" + ds.uri, function(res) {
        			$('td#' + tdid + '_pe').text(res.numHits);
        		});
        	} else {
        		$('#warehouse').append('<td class="datasource_logo_table_cell">'
        		+ '<a href="'+ds.urlToHomepage+'">'
        		+ '<img class="data_source_logo" src="data:image/gif;base64,' + ds.icon 
                + '" title="' + ds.description + '">' + ds.description + '</img></a></td>');
        	}
        });
        
        $('#warehouse').append("</tr>");    
        
        $('#datasources').append('<tr>' 
           		+ '<td class="datasource_logo_table_cell">Total:</td>'
           		+ '<td class="datasource_logo_table_cell" style="white-space: nowrap">'
           		+ '</td><td class="datasource_logo_table_cell">'
           		+ '</td><td id="total_pw" class="datasource_num_table_cell">'
           		+ '</td><td id="total_it" class="datasource_num_table_cell">'
           		+ '</td><td id="total_pe" class="datasource_num_table_cell">'
           		+ '</td></tr>');
    });
    
	$.getJSON(base + "/search.json?q=*&type=pathway", function(res) {
		$('td#total_pw').text(res.numHits);
	});				
	$.getJSON(base + "/search.json?q=*&type=interaction", function(res) {
		$('td#total_it').text(res.numHits);
	});
	$.getJSON(base + "/search.json?q=*&type=physicalentity", function(res) {
		$('td#total_pe').text(res.numHits);
	});
    
    
}
