"use strict";

$(function() {	
	// make code pretty
	window.prettyPrint && prettyPrint();
});


function switchit(id) {
	var style = document.getElementById(id).style;
	if (!style.display || style.display == "none") {
		style.display = "block";
	} else {
		style.display = "none";
	}
}

$(window).on('resize load', function() {
    
	$('body').css({
        "padding-top": $("#header_navbar").height() + 10 + "px"
    }, {
        "padding-bottom": $("#footer_navbar").height() + 10 + "px"
    });
    
    $('.nav-target').css({
        "padding-top": $("#header_navbar").height() + 10 + "px"
    }, {
        "margin-top": $("#header_navbar").height() + 10 + "px"
    });
    
});
