var showhelp = false;

var player = "";
var passcode = "";
var roundCont = false;
var room = -1;

var polling = false;

var roundStarting = false;

var interval;
var targetDate;

var newRoundRequested = false;
var moreTimeRequested = false;
var lastMinute = false;

function debug(word) {
	if (0) {
		$("#helplink").text($("#helplink").text() + word);
	}
}


/*
 * At the document load: Set some good css values, copy the help info from the
 * help pop-up to the login page. 
 * 
 */

$(document).ready(function() {
	$("#ownwordlistcontainer").css("max-height", ($(window).height() - 55) + "px");
	
	$("#helpcopy").html($("#helpinfo").html());

//	timerUpdate(5000);
//	interval = setInterval(timerCounter, 200);
});

/*
 * poll: Poll function for the long-poll query wordwaiter.
 * 
 */

function poll() {
	if (player != "" && room != -1 && !polling) {
		setTimeout(function() {
			if (room != -1) {
				var geturl = '/sanaruudukko/rest/sr/wordwaiter?player=' + player + "&passcode=" + passcode;
				//		document.myForm.debug.value = geturl;
				polling = true;
				$.ajax({
//					cache: false,
					dataType : 'xml',
					type : 'GET',
					url : geturl,
					timeout: 20000,
					success : function(data) {
						update(data);
						updatewords(data);
						updatechat(data);
					},
					complete : function(data) {
						polling = false;
						poll();
					}
				});
			}
		}, 200);
	}
}

/*
 * timerUpdate:
 * Input is the amount of milliseconds that the current round will still last.
 * Sets the targetDate to the time when the current round will end.
 * Only updates targetDate if it's stricter than the previous targetDate, so that
 * sudden lag spikes don't mess up with the clock.
 * 
 * When the new targetDate is over a minute past the old one, then we do update the
 * targetDate, as this is most probably because more time has been requested by players.
 */

function timerUpdate(remainingMillis) {
	var currentDate = new Date();
	if (targetDate instanceof Date) {
		var targetMillis = targetDate.valueOf();
		if ((currentDate.valueOf() + remainingMillis) < targetMillis || (currentDate.valueOf() + remainingMillis) > targetMillis + 10000) {
			targetDate.setTime(currentDate.valueOf() + remainingMillis);
		}
	} else {
		targetDate = new Date();
		targetDate.setTime(currentDate.valueOf() + remainingMillis);
	}
}

/*
 * timerCounter:
 * Called 5 times per second from roundContinues
 * Takes the difference between targetDate and current time, and
 * updates #time and other things that vary with time.
 */

function timerCounter() {
	var currentDate = new Date();
	var remainingNow = targetDate.valueOf() - currentDate.valueOf();


    if (! roundStarting) {
		if (remainingNow < 0) {
			$('#time').html("(Kierros päättynyt)");
			roundEnded();
		} else {
			var seconds = Math.floor((remainingNow / 1000)) % 60;
			var minutes = Math.floor((remainingNow / 1000) / 60);
			if (seconds < 10) { seconds = '0' + seconds; }
			$("#time").text(minutes + ":" + seconds);
		}
	
		if (remainingNow < 60000) {
			if (lastMinute == false) {
				lastMinute = true;
				updateButtons();
			}
		} else {
			if (lastMinute == true) {
				lastMinute = false;
				updateButtons();
			}
		}
	} else {
		if (remainingNow < 0) {
			$('#time').html("Alkaa: 0:00");
//			roundEnded();
		} else {
			var seconds = Math.floor((remainingNow / 1000)) % 60;
			var minutes = Math.floor((remainingNow / 1000) / 60);
			if (seconds < 10) { seconds = '0' + seconds; }
			$("#time").text("Alkaa: " + minutes + ":" + seconds);
		}
	}

}


/*
 * update:
 * Input is xml formatted data from the backend containing <time> and
 * <board> fields specifying remaining time in hundredths of a second and
 * board as a bare string.
 * 
 */


function update(data) {
	//	document.myForm.debug.value = data;
		debug(":");
	
		//var xmlDoc = $.parseXML(data);
		//var $xml = $(xmlDoc);
		//var $time = $('time', $xml);
		
		var $time = $(data).find("time");
		var $board = $(data).find("board");
		var $starting = $(data).find("starting");
		
		//				document.myForm.time.value = $test.text();
	    //$("#help").text(data);
		if ($time.text() != "") {
			timerUpdate($time.text() * 10);
			roundContinues();
		}
	
		if ($board.text() != "") {
			populateGrid($board.text());
		}
		
		if ($starting.text() == "1") {
			roundStarting = true;
		} else {
			roundStarting = false;
		}
}

/*
 * roundContinues:
 * Called when the round is still continuing.
 * If it's the first call after the round has been ended, we
 * update buttons, start the timer counter and reset the list of all
 * possible words.
 * 
 */

function roundContinues() {
	if (roundCont == false) {
		interval = setInterval(timerCounter, 200);
		roundCont = true;
//		document.myForm.round.disabled = true;
		updateButtons();
		resetallwords();
	}
}

/*
 * roundEnded:
 * Called when the round has been ended.
 * If it's the first call after the round was continuing, we
 * update buttons, stop the timer counter and induce a back-end query
 * to update the list of all words.
 * 
 */

function roundEnded() {
	if (roundCont == true) {
		clearInterval(interval);
		targetDate = null;
		roundCont = false;
//		document.myForm.round.disabled = false;
		updateButtons();
		getWords();
	}
}

/*
 * updatewords:
 * This rather large function takes as an input some xml data from the
 * back-end, which contains all players in the room, some info on their statuses,
 * score, and all the words they have submitted (or the count of the words, if the
 * round is still continuing and they are not the calling player).
 * 
 * Finally we put this data up on the web page, player's own info to #playerframe 
 * and info on other players to #players.
 */

function updatewords(data) {
	//	document.myForm.debug.value = data;
	//    document.myForm.time.value = data;
	debug(".");

//	$("#helplink").text($(data).text());

	var $player = $(data).find("players").find("player");
		
	var newHtml = "";
	//var ownHtml = "";
	//var targetContainer = newContainer;
	var containerHTML = "";

	var players = 0;
	
	if ($player.text() == "") { return; }

	var maxPlayers = Math.floor(($(window).width() - $("#upperarea").width() - 70) / 200) - 1;
//	alert($(window).width() + " "+ $("#upperarea").width() + " " + maxPlayers);

	moreTimeRequested = false;
	newRoundRequested = false;

	var backupscroll = 0;
	
	$player.each(function(index, elem) {
		newHtml = "";
		var isPlayer = ($(elem).find('name').text() == player);

		if (isPlayer) {
			backupscroll = $("#ownwordlistcontainer").scrollTop();
		}
		/*
		 if ($ (elem ).find('name').text() == player) {
		 targetHtml = ownHtml;
		 } else {
		 targetContainer = newContainer;
		 }
		 */
		
		
		newHtml = "<div class='playername'>";
		if ($(elem).find("active").text() == "f") {
			newHtml = newHtml + "<span class='inactive'>";
		} else {
			if ($(elem).find("ready").text() == "t") {
				if (isPlayer) {
					newRoundRequested = true;
				}
				newHtml = newHtml + "<span class='ready'>";
			} else {
				if ($(elem).find("moretime").text() == "t" && roundCont) {
					if (isPlayer) {
						moreTimeRequested = true;
					}
					newHtml = newHtml + "<span class='moretime'>";
				} else {
					newHtml = newHtml + "<span>";
				}
			}
		}
		newHtml = newHtml + $(elem).find("name").text() + "</span>";
		
		var total = $(elem).find("totalscore").text();
		if (total == "" || total == " ") { total = "0"; } else {
			total = parseInt(total, 10);
		}
		var thisround = $(elem).find("thisroundscore").text();
		if (thisround == "") { thisround = "?"; } else {
			thisround = parseInt(thisround, 10);
		}
		newHtml = newHtml + "<br>Kerätyt pisteet: " + total + 
		   "<br />Tämä kierros: " + thisround + "</div>";

		if (isPlayer) {
			newHtml = newHtml + "<div id='ownwordlistcontainer'><div id='ownwordlist'>";
		} else {
			newHtml = newHtml + "<div><div class='wordlist'>";
		}
		
		var mode = $(elem).find('mode').text();
		if (mode == 0) {

			for (var i = 0; i < $(elem).find("wordcount").text(); i++) {
				newHtml = newHtml + "<div class='wordrow'>*****</div>";
			}
			//					container.innerHTML = container.innerHTML + $elem.find("name").text();
		}
		if (mode == 1) {
			$(elem).find("item").each(function() {
				var isdisabled = ($(this).find('disabled').text() == "t");
				var isduplicate = ($(this).find("duplicate").text() == "t");
				newHtml = newHtml + "<div class='wordrow'>";
				newHtml = newHtml + "<div class='delete'>";				
					if (isPlayer) {
						newHtml = newHtml + "<a href='#' onclick=\"removeWord('" + $(this).find('word').text() + "'); return false;\">";
						if (isdisabled) {
							newHtml = newHtml + "<img src='img/undelete.png'></a>";
						} else {
							newHtml = newHtml + "<img src='img/delete.png'></a>";
						}
					}
				newHtml = newHtml + "</div>";
								
				if (isduplicate && isdisabled) {
					newHtml = newHtml + "<span class='duplicateword disabledword'>";
				}
				if (isduplicate && ! isdisabled) {
					newHtml = newHtml + "<span class='duplicateword'>";
				}
				if (! isduplicate && isdisabled) {
					newHtml = newHtml + "<span class='disabledword'>";
				}
				if (! isduplicate && ! isdisabled) {
					newHtml = newHtml + "<span class='normal'>";
				}
				newHtml = newHtml + $(this).find("word").text().replace(/a/g, "&Auml;").replace(/o/g, "&Ouml;");
				newHtml = newHtml + "</span> ";
				if ($(this).find("languagecheck").text() == "t") {
					newHtml = newHtml + "<img src='img/checkmark_small.gif'>";
				}
					newHtml = newHtml + "<div class='points'>";
					newHtml = newHtml + parseInt($(this).find("score").text(), 10);
					newHtml = newHtml + " p</div>";
				newHtml = newHtml + "</div>";
			});
		}
		newHtml = newHtml + "</div></div>";
		if (isPlayer) {
			$('div.playerframe').html(newHtml);
			$("#ownwordlistcontainer").css("max-height", ($(window).height() - (55+65+25)) + "px");
			$("#ownwordlistcontainer").scrollTop(backupscroll);
			//						ownContainer.innerHTML = "<div class='playerframe'>" + newHtml + "</div>";
		} else {
			players++;
			if (players % (maxPlayers + 1) == 0) {
				containerHTML = containerHTML + "<div class='otherplayerframerow'>" + newHtml + "</div>";
			} else {
				containerHTML = containerHTML + "<div class='otherplayerframe'>" + newHtml + "</div>";
			}
		}

	});
	if (players > 0) {
		$('#players').html(containerHTML);
		$('#players').css("display", "block");
		if (players > maxPlayers) {
			players = maxPlayers;
		}
		$('#players').css("width", (players * 202) + "px");
	} else {
		$('#players').css("display", "none");
		$('#players').css("width", "10px");
	}
	updateButtons();
}

/*
 * updateButtons:
 * Disables or enables the buttons and labels them based on the state the
 * game is in.
 * 
 */

function updateButtons() {
	if (roundCont) {
		if (moreTimeRequested) {
			document.myForm.round.value = "Peruuta aikapyyntö";
		} else {
			document.myForm.round.value = "Lisää aikaa";
		}
		document.myForm.round.disabled = ! lastMinute;
	} else {
		if (newRoundRequested) {
			document.myForm.round.value = "Peruuta kierrospyyntö";
		} else {
			document.myForm.round.value = "Uusi kierros";
		}
		document.myForm.round.disabled = false;
	}
	document.myForm.showhide.disabled = roundCont;
}

/*
 * populateGrid:
 * Input is a string of 16 characters, and we put them in the
 * elements #lett<i> in the table for i=1..16.
 * 
 */

function populateGrid(grid) {
	for (var i = 0; i < 16; i++) {
		var container = document.getElementById("lett" + (i + 1));
		if (grid.charAt(i) == 'a')
			container.innerHTML = "&Auml;";
		else if (grid.charAt(i) == 'o')
			container.innerHTML = "&Ouml;";
		else
			container.innerHTML = grid.charAt(i);
/*			
		else if (grid.charAt(i) == 'F')
			container.innerHTML = "FG";
		else if (grid.charAt(i) == 'B')
			container.innerHTML = "BC";*/
	}
}

/*
 * newRound:
 * Request a new round or more time by calling back-end function, depending
 * on whether the round is still going on or has ended.
 * Back-end returns round data (only with newround) and word data, so we update
 * those.
 */

function newRound() {
	if (player != "") {
		if (! roundCont) {
			
			var geturl = "/sanaruudukko/rest/sr/process?func=newround&player=" + player + "&passcode=" + passcode;
			$.ajax({
				async: false,
				cache: false,
				type : 'GET',
				url : geturl,
				success : function(data) {
					update(data);
					updatewords(data);
				},
			});
		} else {
			var geturl = "/sanaruudukko/rest/sr/process?func=moretime&player=" + player + "&passcode=" + passcode;
			$.ajax({
				async: false,
				cache: false,
				type : 'GET',
				url : geturl,
				success : function(data) {
					update(data);
					updatewords(data);
				},
			});
		}
	}
}

/*
 * submitWord:
 * Submits a word to the back-end function. Back-end returns word data
 * so we update that.
 */

function submitWord() {
	if (player != "" && room != -1 && word != "") {
		debug("-");
		var word = document.myForm.word.value.toUpperCase().replace(/Ä/g, "a").replace(/Ö/g, "o");
		var geturl = "/sanaruudukko/rest/sr/process?func=submitword&player=" + player + "&passcode=" + passcode + "&word=" + word;
		//document.myForm.debug.value = geturl;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				updatewords(data);
			}
		});
		document.myForm.word.value = "";
	}
}

/*
 * getWords:
 * Queries word data from the back-end. This call is invoked when the round has ended
 * and we want to see other players' words.
 */

function getWords() {
	if (player != "" && room != -1) {
//		debug("-");
		var geturl = "/sanaruudukko/rest/sr/process?func=getwords&player=" + player + "&passcode=" + passcode;
		$.ajax({
//			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				updatewords(data);
			}
		});
	}
}

/*
 * removeWord:
 * Requests back-end to disable (or enable) the specified word.
 */

function removeWord(data) {
	if (player != "" && room != -1) {
		var geturl = "/sanaruudukko/rest/sr/process?func=removeword&player=" + player + "&passcode=" + passcode + "&word=" + data;
		$.ajax({
			cache: false,
			dataType : 'xml',
			type : 'GET',
			url : geturl,
			success : function(data) {
				updatewords(data);
			}
		});
	}
}

/*
 * showhidehelp:
 * Shows or hides the help pop-up.
 */

function showhidehelp() {
	if (showhelp == false) {
		showhelp = true;
		document.getElementById('helpinfo').style.display = 'block';
	} else {
		showhelp = false;
		document.getElementById('helpinfo').style.display = 'none';
	}
}

