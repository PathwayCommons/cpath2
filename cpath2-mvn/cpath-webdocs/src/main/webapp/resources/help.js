$(document).ready(function() {
	getHelp("help");
});

function getHelp(helpPath) {
	var base = $('#wsroot').text();
	
	//alert("now in: " + helpPath + ", came from: " + prevHelpPath);
	$.getJSON(base + "/" + helpPath, function(help) {
		$('#title').text(help.title);
		$('#info').text(help.info);
		if(help.example) {
			$('#example').text(base + "/" + help.example);
			$('#example').attr('href', base + "/" + help.example);
		} else {
			$('#example').text("");
			$('#example').attr('href',"#");
		}
		// add children (next help steps)
		$('#content .members').remove();
		var newUl = $('<ul class="members">');
		$('#info').after(newUl);
		$.each(help.members, function(idx, v) {
			if(v.members.length > 0) {
				// add clickable links 
				$(".members").append('<li><a id="' 
						+ v.id + '" href="#">' 
					+ v.id + '</a>' 
					//+ " - " + v.title 
					+ '</li>');
				$("#"+ v.id).bind(
						"click",
					function() {
						$('#prev').text(helpPath);
						$("#prev").bind(
								"click",
							function() {
								getHelp(helpPath);
							}
						);
						
						// refresh content
						getHelp(helpPath + "/" + v.id);
					}
				);
			} else {
				// add the description without a link
				// (incl. when no datasources/organisms are loaded to cpath2)
				$(".members").append('<li><span id="' 
					+ v.id + '">' + v.id + ": "
					+ " (" + v.title + "): " 
					+ v.info + '</span></li>');
			} 
		});
	});
}

function showPopup() {		
	$('body').css('overflow','hidden');
	$('#popup').fadeIn('fast');
	$('#mask').fadeIn('fast');
}

function closePopup() {
	$('#popup').fadeOut('fast');
	$('#mask').fadeOut('fast');
	$('body').css('overflow','auto');
}

function createTree() {
	//TODO
}

function getResult() {
	//TODO
}


