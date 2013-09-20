$(document).ready(function() {
	getDatasources();
});

function getDatasources(){
	// get some var values from jsp page
    // call the ws and interate over JSON results
    $.getJSON("/metadata/datasources", function(datasources){
        $('#pathway_datasources').empty();
        $('#warehouse_datasources').empty();                
        $.each(datasources, function(idx, ds){
    		var tdid = ds.identifier;
    		var displayName = ds.name[0];
    		var otherNames = (ds.name.length > 1) ? " ("+ds.name.slice(1).join(", ")+")" : "";         	
        	if (ds.notPathwaydata) {
        		$('#warehouse_datasources').append('<dt><a href="'+ds.urlToHomepage+'">'
                    +'<img src="data:image/gif;base64,'+ds.icon+'" title="'+displayName
                    +' logo"></img></a><strong> '+displayName+otherNames+'</strong></dt>');
        		$('#warehouse_datasources').append('<dd><p>'+ds.description+'</p></dd>');
        	}else{               		    		
        		$('#pathway_datasources').append('<dt><a href="'+ds.urlToHomepage+'">'
        			+'<img src="data:image/gif;base64,'+ds.icon+'" title="'+displayName
        			+' logo"></img></a><strong> '+displayName+otherNames
        			+'</strong>; format: '+ds.type+'; uri: '+ds.uri+ '</dt>');
        		$('#pathway_datasources').append(
        			'<dd><p>'+ds.description+'<br/><strong>' 
           			+ds.counts[0]+' pathways, ' 
           			+ds.counts[1]+' interactions, ' 
           			+ds.counts[2]+' physical entities (states).</dd></strong></p>');
        	}
        });
    });
}
