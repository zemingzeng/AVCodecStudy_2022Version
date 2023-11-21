package com.mingzz__.h26x.web_socket;


import com.mingzz__.util.L;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class Client extends WebSocketClient {
    public Client(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        L.i("Client->onOpen->getHttpStatusMessage: " + handshake.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String message) {
        L.i("Client->onMessage String : " + message);
    }

    @Override
    public void onMessage(ByteBuffer byteBuffer) {
        //真实有效的数据长度
        int length = byteBuffer.remaining();
        L.i("WebSocketClient->onMessage Bytes: " + length);
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        if (null != dataCallBack)
            dataCallBack.getData(bytes);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        L.i("WebSocketClient->onClose reason: " + reason + "  remote:" + remote);
    }

    @Override
    public void onError(Exception ex) {
        L.i("WebSocketClient->onError : " + ex.toString());
    }

    public void startMe() {
        connect();
    }

    public void closeMe() {
        close();
    }

    private DataCallBack dataCallBack;

    public void setDataCallBack(DataCallBack dataCallBack) {
        this.dataCallBack = dataCallBack;
    }

   public interface DataCallBack {
        void getData(byte[] bytes);
    }



}
