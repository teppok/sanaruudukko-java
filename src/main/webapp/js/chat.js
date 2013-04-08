/**
 * @author Teppo Kankaanpää
 */


var chatLines = new Array();
var chatIds = new Array();
var chatIndex = 0;

/*
 * htmlEscape:
 * Escapes a string so that it can be output safely.
 *  
 */

function htmlEscape(str) {
    return String(str)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
}

/*
 * submitChat:
 * Takes a chat line and submits it in a POST query to the database. 
 */

function submitChat() {
	if (player != "" && room != -1) {
		var chat = document.myForm.chat.value;
		var geturl = "/sanaruudukko/rest/sr/submitchat";
//		var postdata = "player=" + player + "&passcode=" + passcode + "chat=" + chat.replace(/=/g, "%3D").replace(/&/g, "%3F");
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'POST',
			data : { player: player, passcode: passcode, chat: chat },
			url : geturl,
			success : function(data) {
				updatechat(data);
			}
		});
		document.myForm.chat.value = "";
	}
}

/*
 * updatechat:
 * Input is some xml formatted data containing chat records of chat lines
 * that haven't been displayed yet.
 * 
 * We take this and update an array chatLines that contains the last 30 chat lines
 * that we keep in the back log. We show these in the #inchatbox div.
 * 
 * We check that we haven't already seen the chat line by keeping a list of chatIds,
 * the encountered chat line ids.
 * 
 * There is a concurrency risk here if this routine is entered by two threads at the
 * same time, presenting the same chat line with the same chat id. This can happen
 * if submitChat() and poll() happen to call updatechat() at the same time with the
 * same data. This causes two similar chat lines to show.
 */

function updatechat(data) {

	var $chatrecord = $(data).find("chatrecord");

	if ($chatrecord.length > 0) {
	    $chatrecord.each(function() {
	    	var id = $(this).find("id");
	    	if ($.inArray(id, chatIds) < 0) {
		    	chatLines[chatIndex] = "<b>" + htmlEscape($(this).find("player").text()) + "</b>: " + htmlEscape($(this).find("line").text()) + "<br />";
		    	chatIds[chatIndex] = id;
		    	chatIndex++;
		    }
	    });

	//	$("#debug").text(chatLines[chatIndex-1]);
	    var i;
	    
		if (chatIndex > 30) {
			for (i=chatIndex-30; i<chatIndex; i++) {
				chatLines[i-(chatIndex-30)] = chatLines[i];
				chatIds[i-(chatIndex-30)] = chatIds[i];
			}
			chatIndex = 30;
		}
	
		chatHTML = "";
		for (i=0; i<chatIndex; i++) {
			chatHTML = chatHTML + chatLines[i];
		}

        $("#inchatbox").html(chatHTML);
	
		var height = $("#inchatbox").height() - 180;
		if (height < 0) { height = 0; }
//		$("#debug").text(chatLines[chatIndex-1]);
		$("#chatbox").scrollTop(height);
	}
}

