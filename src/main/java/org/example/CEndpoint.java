package org.example;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.*;

@ClientEndpoint
public class CEndpoint {
    Session depthStreamSession = null;
    private UpdateOrderDataInterface orderDataBid;
    private UpdateOrderDataInterface orderDataAsk;
    private final List<String> earlyMessages = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean reSubscribeSoon = new AtomicBoolean(false);

    //connect to the stream
    public CEndpoint(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (DeploymentException e) {
            System.err.printf("DeploymentException: %s", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.printf("IOException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void assignOrderData(UpdateOrderDataInterface orderDataBid, UpdateOrderDataInterface orderDataAsk){
        this.orderDataBid = orderDataBid;
        this.orderDataAsk = orderDataAsk;
    }

    public interface UpdateOrderDataInterface {
        void updateOrderData(String message) throws JSONException;
    }

    @OnOpen
    public void onOpen(Session depthStreamSession) {
        this.depthStreamSession = depthStreamSession;
    }

    //handle the event messages by updating the OrderData objects
    @OnMessage
    public void onMessage(String message) throws JSONException {
        if (this.orderDataBid != null && this.orderDataAsk != null) {
            this.orderDataBid.updateOrderData(message);
            this.orderDataAsk.updateOrderData(message);
            if(reSubscribeSoon.get()) {
                earlyMessages.add(message);
            }
        } else {
            earlyMessages.add(message);
        }
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.depthStreamSession = null;
    }
    public void applyEarlyMessages() {
        try {
            reSubscribeSoon.set(false);
            for (String earlyMessage : earlyMessages) {
                if (this.orderDataBid != null && this.orderDataAsk != null) {
                    this.orderDataBid.updateOrderData(earlyMessage);
                    this.orderDataAsk.updateOrderData(earlyMessage);
                }
            }
            earlyMessages.clear();
        } catch (JSONException e) {
            System.err.printf("JSONException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void applyEarlyMessagesSoon() {
        reSubscribeSoon.set(true);
    }
}