
function getAccessCounts(){
    $.post("../logs/summary", function(map){
        $('#accessCounts').empty();             
        $.each(map, function(key, val){
       		$('#accessCounts')
       			.append('<dt>' + key + '</dt>')
       			.append('<dd>' + val + '</dd>');
        });
    }, "json");
}
