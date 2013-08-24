
function SearchAPI(service) {
	this.service = service;
	this.search = function(callback, errorback) {
		var vars = [], hash;
		
		var uri = document.URL;
	    var q = uri.split('?')[1];
	    if(q != undefined){
	        q = q.split('&');
	        for(var i = 0; i < q.length; i++){
	            hash = q[i].split('=');
	            vars.push(hash[1]);
	            vars[hash[0]] = hash[1];
	        }
	    }
	    
	    var index = uri.indexOf("://");
	    var index2 = uri.indexOf("/", index+3);
	    var domain = uri.substring(index+3, index2);
	    
	    index = domain.indexOf(":");
	    if (index>0) {
	    	domain = domain.substring(0, index);
	    }
	    
		var queryString = vars["q"];
		if (queryString) {
		 	$.ajax({
				   type: 'GET',
				    url: service + '/jsonp',
				    jsonpCallback: 'jsonCallback',
				    contentType: "application/json",
				    dataType: 'jsonp',
				    data: {
				    	q : queryString,
				    	d : domain
				    },
				    success: function(json) {
				    	callback(json);
				    },
				    error: function(e) {
				    	errorback(e);
				    }
			});
		} else {
			callback({ queryString : "", posts: []});
		}
	}; 
}

