package org.example;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import javax.websocket.*;

@ClientEndpoint
public class CEndpoint {
    Session depthStreamSession = null;
    private final UpdateOrderDataInterface orderDataBid;
    private final UpdateOrderDataInterface orderDataAsk;

    //connect to the stream
    public CEndpoint(URI endpointURI, UpdateOrderDataInterface orderDataBid, UpdateOrderDataInterface orderDataAsk) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
            this.orderDataBid = orderDataBid;
            this.orderDataAsk = orderDataAsk;
        } catch (DeploymentException e) {
            System.err.printf("DeploymentException: %s", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.printf("IOException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
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
        if (this.orderDataBid != null) {
            this.orderDataBid.updateOrderData(message);
        }
        if (this.orderDataAsk != null) {
            this.orderDataAsk.updateOrderData(message);
        }
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.depthStreamSession = null;
    }

}