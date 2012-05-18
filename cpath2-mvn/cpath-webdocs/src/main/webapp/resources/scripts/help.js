$(document).ready(function() {
    getCommandParameterDetails("help/formats", "output_parameter", "#command_header_additional_parameters_output", "#command_header_additional_parameters_output_desc");
    getCommandParameterDetails("help/kinds", "graph_parameter", "#command_header_additional_parameters_graph", "#command_header_additional_parameters_graph_desc");
    getCommandParameterDetails("help/directions", "direction_parameter", "#command_header_additional_parameters_direction", "#command_header_additional_parameters_direction_desc");
    getCommandParameterDetails("help/datasources", "datasource_parameter", "#command_header_additional_parameters_datasource", "#command_header_additional_parameters_datasource_desc");
    getCommandParameterDetails("help/organisms", "organism_parameter", "#command_header_additional_parameters_organism", "#command_header_additional_parameters_organism_desc");
    getCommandParameterDetails("help/types", "biopax_parameter", "#command_header_additional_parameters_biopax", "#command_header_additional_parameters_biopax_desc");
    getCommandParameterDetails("help/types/properties", "properties_parameter", "#command_header_additional_parameters_properties", "#command_header_additional_parameters_properties_desc");
    getCommandParameterDetails("help/types/inverse_properties", "inverse_properties_parameter", "#command_header_additional_parameters_inverse_properties", "#command_header_additional_parameters_inverse_properties_desc");
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
	var parameter_desc = $(parameterDesc).html();
    var class_name = clazz + "_class"

    // setup content element and append header
    $('#content .' + class_name).remove();
    var ui = $('<div class="' + class_name + '">');
    $('#' + clazz).after(ui);
    $("." + class_name).append('<h3><a name="available_' + clazz + '"></a>' + command_header + '</h3>');
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
                        info = ' ' + member.info
                    }
                    else if (helpWSPath.indexOf("datasource") != -1) {
                        info = ' ' + member.info
                    }
                    $("." + class_name).append('<li style="margin-left: 2em;">' + member.id + info + '</li>');
            });
            $("." + class_name).append('</ul>');
            $("." + class_name).append('</div>');
    });
}
