window.onload = function() {
    function output(style, text){
        messages.innerHTML += "<br/><span class='" + style + "'>" + text + "</span>";
      }

    const input = document.getElementById('input');
    const openBtn = document.getElementById('open');
    const sendBtn = document.getElementById('send');
    const closeBtn = document.getElementById('close');
    const messages = document.getElementById('messages');
    const playerTable = document.getElementById('players');
    const pickGameButton = document.getElementById('pickGameButton');
    const playerNameInput = document.getElementById('playerName');
    const registerPlayerBtn = document.getElementById('registerPlayer');

    var socket;
    var playerName;

    function createPlayerList(players) {
        try {
            console.log('createPlayerList players ', players)
            const playerRows = Object.keys(players).map((playerName) =>
                `<tr><td>${playerName}</td><td>${players[playerName].gameKey}</td></tr>`
            ).join();
            const table =
                `<table><thead><tr><th>Player name</th><th>Game ID</th></tr></thead><tbody>${playerRows}</tbody></table>`;
            playerTable.innerHTML = table;
        } catch (e) {
            console.log("failed to create player list, error: ", e);
        }
    }

    openBtn.onclick = function(e) {
        e.preventDefault();
        if (socket !== undefined) {
            output("error", "Already connected");
            return;
        }

        var uri = "ws://" + location.host + location.pathname;
        uri = uri.substring(0, uri.lastIndexOf('/'));
        socket = new WebSocket(uri);

        socket.onerror = function(error) {
            output("error", error);
        };

        socket.onopen = function(event) {
            output("opened", "Connected to " + event.currentTarget.url);
        };

        socket.onmessage = function(event) {
            var message = event.data;
            console.log('event ', event)
            try {
                const parsed = JSON.parse(message);
                console.log('Parsed message ', parsed)
                const msgType = parsed?.msgType;
                switch (msgType) {
                    case 'playerList':
                        createPlayerList(parsed.content);
                        break;
                    case 'text':
                        output("received", "<<< " + message);
                        break;
                    default:
                        output("received", "<<< " + message);
                }
            } catch(e) {
                console.log('Encountered error while handling message: ' + message, e);
                output("error with message", "<<< " + message);
            }
        };

        socket.onclose = function(event) {
            output("closed", "Disconnected: " + event.code + " " + event.reason);
            socket = undefined;
        };
    };

    sendBtn.onclick = function(e) {
        if (socket == undefined) {
          output("error", 'Not connected');
          return;
        }
        var text = document.getElementById("input").value;
        socket.send(JSON.stringify({ msgType: 'text', content: text }));
        output("sent", ">>> " + text);
    };

    closeBtn.onclick = function(e) {
        if (socket == undefined) {
          output('error', 'Not connected');
          return;
        }
        socket.close(1000, "Close button clicked");
    };

    registerPlayerBtn.onclick = function(e) {
        if (socket == undefined) {
            output('error', 'Not connected');
            return;
        }
        socket.send(JSON.stringify({
            msgType: 'registerplayer',
            content: {
                playerName: playerNameInput.value
            }
        }));
        playerName = playerNameInput.value
        registerPlayerBtn.setAttribute('disabled', true)
    }

    pickGameButton.onclick = (function() {
        if (socket == undefined) {
            output('error', 'Not connected');
            return;
        }
        socket.send(JSON.stringify({
            msgType: 'pickgame',
            content: {
                gameKey: 'Treasure Hunt',
                playerName: playerName
            }
        }))
    })
};