package com.mingzz__.h26x.web_socket;


import com.mingzz__.util.L;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server extends WebSocketServer {
    WebSocket webSocket;

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //client connect success
        L.i("Server onOpen remote address-->" + conn.getRemoteSocketAddress());
        this.webSocket = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        L.i("Server onClose ,reason,remote-->" + reason + "," + remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        L.i("Server onMessage message-->" + message);

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        L.i("Server onError-->" + ex.toString());
    }

    @Override
    public void onStart() {
        L.i("Server onStart ");

    }


    public boolean isOpen() {
        if (null != webSocket)
            return webSocket.isOpen();
        return false;
    }

    public void startMe() {
        start();
    }

    public void closeMe() throws IOException, InterruptedException {
        if (null != webSocket)
            webSocket.close();
        stop();
    }

    public void sendData(byte[] bytes) {
        if (null == webSocket || !webSocket.isOpen())
            return;
        webSocket.send(bytes);
    }
}
