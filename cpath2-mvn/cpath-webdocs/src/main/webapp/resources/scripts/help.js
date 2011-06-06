$(document).ready(function() {
    getCommandParameterDetails("help/formats", "output_parameter", "#command_header_additional_parameters_output", "#command_header_additional_parameters_output_desc");
    getCommandParameterDetails("help/kinds", "graph_parameter", "#command_header_additional_parameters_graph", "#command_header_additional_parameters_graph_desc");
    getCommandParameterDetails("help/datasources", "datasource_parameter", "#command_header_additional_parameters_datasource", "#command_header_additional_parameters_datasource_desc");
    getCommandParameterDetails("help/organisms", "organism_parameter", "#command_header_additional_parameters_organism", "#command_header_additional_parameters_organism_desc");
    getCommandParameterDetails("help/types", "biopax_parameter", "#command_header_additional_parameters_biopax", "#command_header_additional_parameters_biopax_desc");
    setOnClicks(); // auto-set 'onclick' handlers for particular A elements (query examples)
});

//
// This function creates a body for the given command parameter output.
//
// @param helpWSPath - path to web service call
// @param clazz - the name of the class that will be created in the DOM
// @param header - the key into messages.properties to header string 
// @param parameterFootnote - the key into the message.properties to the parameter description
//
function getCommandParameterDetails(helpWSPath, clazz, header, parameterDesc) {

    // get some var values from jsp page
    var base = $('#web_service_url').text();
    var command_header = $(header).text();
	var parameter_desc = $(parameterDesc).text();
    var class_name = clazz + "_class"

    // setup content element and append header
    $('#content .' + class_name).remove();
    var ui = $('<div class="' + class_name + '">');
    $('#' + clazz).after(ui);
    $("." + class_name).append('<h3><a name="valid_' + clazz + '"></a>' + command_header + '</h3>');
	if (parameter_desc.length > 0) {
		$("." + class_name).append('<p>' + parameter_desc + '</p>');
	}
    $("." + class_name).append('<ul style="margin-bottom: 1px;">');

    // interate over results (as json) from web service call
    $.getJSON(base + "/" + helpWSPath, function(help) {
            $.each(help.members, function(idx, member) {
                    var info = '';
                    if (helpWSPath.indexOf("organism") != -1) {
                            info = ' ' + member.info
                    }
                    else if (helpWSPath.indexOf("kind") != -1 || helpWSPath.indexOf("formats") != -1) {
                            info = ' [' + member.info + ']'
                    }
                    $("." + class_name).append('<li style="margin-left: 2em;">' + member.id + info + '</li>');
            });
            $("." + class_name).append('</ul>');
            $("." + class_name).append('</div>');
			$("." + class_name).append('<br>');
    });
}


/* sets a special onclick handler for 
 * all 'A' elemets with name="example"
 */
function setOnClicks() {
	//select all the a tag with name equal to modal
    $('a[name=example]').click(function(e) {
        //Cancel the link behavior
        e.preventDefault();
        //Get the A tag
        var url = $(this).attr('href');
    	//alert("Show content from: " + url);	
    	$.get(url, function(data) {
    		var content;
    		if(url.indexOf("xml") != -1) {
    			content = (new XMLSerializer()).serializeToString(data);
    		} 
    		else if(url.toLowerCase().indexOf("json") != -1){
    			content = JSON.stringify(data,null,null);
    		} else {
    			content = data;
    		}
    		
    		//show the query result in a new modal window!
    		//note: <textarea/> plays a very important role: to auto-encode special symbols in its content!
        	$('<div><textarea rows="100" cols="50">' + content + '</textarea></div>').dialogpaper({
				title: "Example Query (press 'Esc' to close window)",
				width: 500,
				height: 300,
				resizable: true,
				modal: true
			});
        });
    });
}



