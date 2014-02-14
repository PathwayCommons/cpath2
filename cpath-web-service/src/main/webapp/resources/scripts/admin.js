var Admin = (function() {

  function setup() {
	  $('.datasources').html(''); //clear	  	  
  	  $.getJSON('foo', function(entries) {
  		$.each(entries, function(i, ev){
  			  $('.datasources')
			    .append("TODO");
  		});
  	  });  	  
  }  
  
  return {
    'setup': setup
  };

})();

