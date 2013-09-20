$(document).ready(function() {
	getCommandParameterDetails("/help/formats", "formats");
    getCommandParameterDetails("/help/kinds", "kinds");
    getCommandParameterDetails("/help/directions", "directions");
    getCommandParameterDetails("/help/types", "types");
    getCommandParameterDetails("/help/types/properties", "properties");
    getCommandParameterDetails("/help/types/inverse_properties", "inverse_properties");
});

// This function creates a body for the given command parameter output.
//
// @param helpWSPath - path to web service call
// @param id - the id of the list ('ul') in the DOM
function getCommandParameterDetails(helpWSPath, id) {
	// get some var values from jsp page
    // setup content element and append header
    $('#' + id).empty();
    //$("." + class_name).append('<ul style="margin-bottom: 1px;">');
    // iterate over results (as json) from web service call
    $.getJSON(helpWSPath, function(help) {
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
function switchit(list) {
	var listElementStyle = document.getElementById(list).style;
	if (listElementStyle.display == "none") {
		listElementStyle.display = "block";
	} else {
		listElementStyle.display = "none";
	}
}
