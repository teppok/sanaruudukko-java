/**
 * @author Teppo Kankaanpää
 */

/*
 * startRoom:
 * Input is xml formatted data. If it contains status 10, then
 * we change the view to Room list view.
 * Otherwise we change the view to Room view and initialize
 * the Room view with the word and time data contained in the xml.
 * 
 * This function is called as a result of registerp, joinroom, newroom, so 
 * the back-end provides this data.
 * 
 * If status=1, then username and password combination was incorrect.
 * Sensibly this only happens when called from registerp which is called from
 * the Login view, so we show this error in the Login view.
 */

function startRoom(data) {
	if ($(data).find("status").text() == 1) {
		$("#warning").html("Kirjautuminen ei onnistunut.");
	} else {
		if ($(data).find("status").text() == 10) {
			initRoomList();
	 	} else {
			room = $(data).find("id").text();
			document.getElementById("roomnumber").innerHTML = "Huone: " + $(data).find("roomname").text();
			document.getElementById("titlescreen").style.display = "none";
			document.getElementById("roomform").style.display = "none";
			document.getElementById("playarea").style.display = "block";
			updatewords(data);
			update(data);
			poll();
		}
	}

}

/*
 * setPlayer:
 * Called from Login view.
 * Takes the player name and password from the form values and calls
 * registerp back-end function. 
 * 
 */

function setPlayer() {
	$("#warning").html("");
	var tmpplayer = document.registerform.name.value;
	var tmppasscode = document.registerform.passcode.value; 
	if (tmpplayer.search(/[^a-zA-Z0-9]/) != -1 || tmpplayer.length > 16) {
		$("#warning").html("Viallinen pelaajan nimi.");
		return;
	}
	if (tmppasscode.search(/[^a-zA-Z0-9]/) != -1 || tmppasscode.length > 16) {
		$("#warning").html("Viallinen tunnussana.");
		return;
	}
	player = tmpplayer;
	passcode = tmppasscode;
	if (player != "" && passcode != "") {

		var geturl = "/sanaruudukko/rest/sr/process?func=registerp&player=" + player + "&passcode=" + passcode;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				startRoom(data);
			},
		});
		//document.myForm.word.focus();
	}
}


/*
 * initRoomList:
 * Shows the Room list view.
 * Also calls getrooms back-end function so that we can show the available rooms
 * in the room list view as well.
 * 
 */

function initRoomList() {
	room = -1;
	document.getElementById("titlescreen").style.display = "none";
	document.getElementById("roomform").style.display = "block";
	document.getElementById("playarea").style.display = "none";
	if (player != "") {
		var geturl = "/sanaruudukko/rest/sr/process?func=getrooms&player=" + player + "&passcode=" + passcode;
		$.ajax({
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				showRoomList(data);
			},
		});
	}	
}

/*
 * showRoomList:
 * Input is xml formatted data containing info on the current active rooms in
 * the system.
 * This shows it on a div in the room list view.
 */

function showRoomList(data) {
	var $room = $(data).find("room");

	var roomlist = "";

	$room.each(function () {
		roomlist += "<div><a href='#' onclick='joinroom(" + $(this).find("id").text() + 
		  ");'>Room: " + $(this).find("roomname").text() + 
		  ", Players: " + $(this).find("players").text() + "</a></div>";
	});
	
	if (roomlist == "") { roomlist = "Ei aktiivisia huoneita."; }
	
	$("#roomlist").html(roomlist);
}

/*
 * joinroom:
 * Called from the Room list view. Input is the id of the room that the user wishes to 
 * join, and this function calls the back-end to fulfill this wish.
 */

function joinroom(data) {
	var tmproom = data;
	if (player != "" && passcode != "" && tmproom >= 0) {

		var geturl = "/sanaruudukko/rest/sr/process?func=joinroom&player=" + player + "&passcode=" + passcode + "&room=" + tmproom;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				startRoom(data);
			},
		});
	}
}

/*
 * newroom:
 * Called from the Room list view. Looks at the roomname form value and
 * requests the database to create a new room by this name.
 */

function newroom() {
	$("#warning2").html("");
	var tmproomname = document.roomform.roomname.value;
	if (tmproomname.search(/[^a-zA-Z0-9]/) != -1 || tmproomname.length > 16) {
		$("#warning2").html("Viallinen huoneen nimi.");
		return;
	}
	var roomname = tmproomname;
	if (player != "" && passcode != "" && roomname != "") {

		var geturl = "/sanaruudukko/rest/sr/process?func=newroom&player=" + player + "&passcode=" + passcode + "&roomname=" + roomname;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				startRoom(data);
			},
		});
	}
}

/*
 * leaveRoom:
 * Called from Room view. Leaves the room the player is currently in and 
 * after that returns to Room list view.
 */

function leaveRoom() {
	if (player != "") {
		roundEnded();
		var geturl = "/sanaruudukko/rest/sr/process?func=leaveroom&player=" + player + "&passcode=" + passcode;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				initRoomList();
			},
		});
	}
}

