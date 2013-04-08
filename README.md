

Sanaruudukko Java back-end
Copyright 2013 Teppo Kankaanpaa
--------------------------

This project is the Java port and enhancement of the COBOL back end for 
the game Sanaruudukko. The back-end is a basic REST interface with
database connection using JPA entities. All interfacing is done with
Sanaruudukko.java class, others help with different parts of the game.

RoomManager.java:
- Handles room management such as creating, joining and leaving rooms.

RoundManager.java:
- Handles round management such as starting new round, asking for more time
  in the round.
  
WordManager.java:
- Handles word management such as submitting words and removing them.

Sanaruudukko.java:
- Handles the process, wordwaiter and submitchat queries. The process query
  performs various instantaneous functions such as submitting words and 
  creating rooms. It returns game data in XML to be displayed on the game
  client.
  
- Reverse AJAX/Long-poll AJAX/Comet:
  - The game uses a long-poll AJAX pattern to fetch changing information about
    the game. The game client invokes a query on "wordwaiter", and this query will
    halt until it gets a signal from other parts of the game. These parts notify
    that the game state has changed, and "wordwaiter" query will then decide
    which game state parts need to be sent to the client. The client will then
    perform another "wordwaiter" query to get new data.

- There are some chat functions in the game - players can submit lines of chat
  to other players in the same room, and the game client shows these chat lines.
  The lines will be sent in POST queries instead of GET which is used for the
  "process" query.

AsyncManager.java:
- The game has also some asynchronous routines that will be invoked at certain
  points in the game.
- CalculateValidWords is invoked when a new round is started,
  and it performs a database query for every word in WordList and checks it against
  the current round. It adds all valid words to table ValidWord to be fetched
  quickly later when the round has ended.
- RoundStartWaiter will poll the database when the round is starting but not yet
  actually started (starting time is in the future). When the starting time passes,
  it notifies all the players in the room so that their wordwaiter queries will
  give them the room info and they can see the board.
  
  
Technology keywords: Java EE 6, EJB, JAX-RS, JPA, JTA.

 