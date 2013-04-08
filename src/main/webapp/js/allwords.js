/**
 * @author Teppo Kankaanpää
 */

var allWordsShown = false;

var allWordsRequested = false;

/*
 * initAllWords:
 * Requests the all words list if the system is in a good state (the list not 
 * 	already requested, the round is not continuing).
 * 
 * 
 */

function initAllWords() {
	if (! allWordsRequested && ! roundCont) {
		allWordsRequested = true;
		if (player != "" && passcode != "") {
			$("#allwords").html("Ladataan.");
			var geturl = "/sanaruudukko/rest/sr/process?func=allwords&player=" + player + "&passcode=" + passcode;
			$.ajax({
				type : 'GET',
				datatype : 'xml',
				url : geturl,
				success : function(data) {
					showAllWords(data);
				},
			});
	    }
	}
}

/*
 * showAllWords:
 * The input is some xml formatted data from the back-end that contains the
 * list of all possible words in the word list that exist in the grid. This
 * function shows these words in the #allwords div.
 */

function showAllWords(data) {
	var $word = $(data).find("word");

	var allwords = "<div class='allwordlist'>";

	var i = 0;
	$word.each(function () {
		if (i % 20 == 19) { 
			allwords += "</div><div class='allwordlist'>";
		}
		allwords += $(this).text().replace(/a/g, "&Auml;").replace(/o/g, "&Ouml;") + "<br />";
		i++;
	});
	allwords += "</div>";
	
	if (i == 0) { allwords = "Ei sanoja."; }
	
	$("#allwords").html(allwords);

}

/*
 * resetallwords:
 * Clears and hides the all words list. This function is called when a new round starts.
 * 
 */

function resetallwords() {
	allWordsRequested = false;
	allWordsShown = false;
	$("#allwords").html("Kierros on kesken.");
	document.getElementById('allwords').style.display = 'none';
}

/*
 * showhideallwords:
 * Shows and hides the all words div, and on show we also do the call to update the
 * list using initAllWords().
 */

function showhideallwords() {
		if (allWordsShown == false) {
			initAllWords();
			allWordsShown = true;
			document.myForm.showhide.value="Piilota kaikki mahdollisuudet";
			document.getElementById('allwords').style.display = 'block';
		} else {
			allWordsShown = false;
			document.myForm.showhide.value="Näytä kaikki mahdollisuudet";
			document.getElementById('allwords').style.display = 'none';
		}
}

