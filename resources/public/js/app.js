window.onload = function() {
    function output(style, text){
        messages.innerHTML += "<br/><span class='" + style + "'>" + text + "</span>";
      }

    const input = document.getElementById('input');
    const openBtn = document.getElementById('open');
    const sendBtn = document.getElementById('send');
    const closeBtn = document.getElementById('close');
    const messages = document.getElementById('messages');
    const players = document.getElementById('players');
    const getPlayersBtn = document.getElementById('getPlayers');

    var socket;

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
            output("received", "<<< " + message);
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

    getPlayersBtn.onclick = function(e) {
        if (socket == undefined) {
            output('error', 'Not connected');
            return;
        }
        socket.send(JSON.stringify({ msgType: 'getplayers' }));
    }
};