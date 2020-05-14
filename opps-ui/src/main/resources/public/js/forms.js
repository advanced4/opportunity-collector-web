function regformhash(form, username, email, password, conf) {
     // Check each field has a value
    if (username.value == ''         ||
          email.value == ''     || 
          password.value == ''  ||
          conf.value == '') {
 
        custom_show_message("Error", 'You must provide all the requested details. Please try again', "alert");
        return false;
    }
 
	if(!check_password(password, conf, form)){
		return false;
	}

	// this is stupid but on failure, if we append "p" twice,
	// then javascript won't read its value
	var element = document.getElementById("hiddenpw");
	if(element != null){
        element.parentNode.removeChild(element);
    }

    // Create a new element input, this will be our hashed password field. 
    var p = document.createElement("input");
 
    // Add the new element to our form. 
    form.appendChild(p);
    p.id = "hiddenpw";
    p.name = "p";
    p.type = "hidden";
    p.value = hex_sha512(password.value);
 
    // Make sure the plaintext password doesn't get sent. 
    password.value = "";
    conf.value = "";
 
    return true;
}

// testforempty because if we DO call it with an empty string (changeotherpw) we want to catch that,
// but if we don't provide that arg at all (changpw) then just ignore it
function changepw(form, password, conf, email="testforempty") {
	
     // Check each field has a value
    if (  email.value == ''  || password.value == ''  || conf.value == '') {
        custom_show_message("Error", 'You must provide all the requested details. Please try again', "alert");
        return false;
    }
 
	if(!check_password(password, conf, form)){
		return false;
	}
	
    // Create a new element input, this will be our hashed password field. 
    var p = document.createElement("input");
 
    // Add the new element to our form. 
    form.appendChild(p);
    p.name = "p";
    p.type = "hidden";
    p.value = hex_sha512(password.value);
 
    // Make sure the plaintext password doesn't get sent. 
    password.value = "";
    conf.value = "";

    return true;
}

function hibp_check_pass(password){
	var api = "https://api.pwnedpasswords.com/range/";
	
	var hash = sha1.create();
	hash.update(password.value);
	var hex = hash.hex();
	
	var prefix = hex.substring(0,5);
	var suffix = hex.substring(5,40);
	
	var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", api + prefix, false ); // false for synchronous request

    xmlHttp.send( null );
	var hashes = xmlHttp.responseText.split(/\r?\n/)
	var seen = 0;

	for (var i = 0; i < hashes.length; i++) {
		thash = hashes[i].split(':')[0].toLowerCase();
		
		if(thash == suffix){
			seen = hashes[i].split(':')[1];
		}
	}
	
	if(seen == 0)
		return true;
	else{
		custom_show_message("Error", "This password is insecure, and has been seen " + seen + " times in leaked databases", "alert");
		return false;
	}
}

function check_password(password, conf, form){
	// Check that the password is sufficiently long (min 6 chars)
    // The check is duplicated below, but this is included to give more
    // specific guidance to the user
    if (password.value.length < 9) {
        custom_show_message("Error", 'Passwords must be at least 10 characters long.  Please try again', "alert");
        form.password.focus();
        return false;
    }
 
	if (password.value.length > 127) {
        custom_show_message("Error", 'Passwords must be less 128 characters long.  Please try again', "alert");
        form.password.focus();
        return false;
    }
	
    // At least one number, one lowercase and one uppercase letter 
    // At least six characters 
//    var re = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{9,}/; 
//    if (!re.test(password.value)) {
//        custom_show_message("Error", 'Passwords must contain at least one number, one lowercase and one uppercase letter.  Please try again', "alert");
//        return false;
//    }
 
    // Check password and confirmation are the same
    if (password.value != conf.value) {
        custom_show_message("Error", 'Your password and confirmation do not match. Please try again', "alert");
        form.password.focus();
        return false;
    }
 
	if(!hibp_check_pass(password)){
		return false;
	}
	
	return true;
}