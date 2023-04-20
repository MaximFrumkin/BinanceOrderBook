package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    static OrderData orderDataBid;
    static OrderData orderDataAsk;
    static CEndpoint cEndpoint;

    //Get the snapshot from Binance
    public static JSONObject getSnapshot(String address) {
        try {
            InputStream inputStream = new URL(address).openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            int currChar = bufferedReader.read();
            while (currChar != -1) {
                stringBuilder.append((char) currChar);
                currChar = bufferedReader.read();
            }
            inputStream.close();
            return new JSONObject(stringBuilder.toString());
        } catch (MalformedURLException e) {
            System.err.printf("MalformedURLException: %s", e.getMessage());
            throw new RuntimeException(e);
        } catch (JSONException e) {
            System.err.printf("JSONException: %s", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.printf("IOException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Apply the snapshot to my OrderData objects
    static void applySnapshot() {
        try {
            JSONObject snapshot = getSnapshot("https://api.binance.com/api/v3/depth?symbol=ETHBTC&limit=1000");
            final int lastUpdateId = snapshot.getInt("lastUpdateId");
            orderDataBid = new OrderData(true, lastUpdateId);
            orderDataAsk = new OrderData(false, lastUpdateId);
            orderDataBid.applySnapshot(snapshot);
            orderDataAsk.applySnapshot(snapshot);
            cEndpoint.assignOrderData(orderDataBid, orderDataAsk);
            cEndpoint.applyEarlyMessages();
        } catch (JSONException e) {
            System.err.printf("JSONException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Subscribe to the ethbtc stream
    static void subscribe() {
        try {
            cEndpoint = new CEndpoint(new URI("wss://stream.binance.com:9443/ws/ethbtc@depth"));
        } catch (URISyntaxException e) {
            System.err.printf("URISyntaxException: %s", e.getMessage());
        }

    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Please Enter the Number of Levels:");
        final int numLevels = sc.nextInt();
        Runnable reSubscribe = new Runnable() {
            public void run() {
                subscribe();
                applySnapshot();
            }
        };
        Runnable printOrderBook = new Runnable() {
            public void run() {
                orderDataBid.prepForItr();
                orderDataAsk.prepForItr();
                System.out.printf("%-10s %10s \t %-10s %10s\n", "BID_SIZE", "BID_PRICE", "ASK_PRICE", "ASK_SIZE");
                for (int i = 0; i < numLevels; i++) {
                    orderDataBid.print();
                    orderDataAsk.print();
                }
            }
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        //Binance only keeps streams open for 24 hours. To avoid this issue, every 12 hours I reopen the stream.
        executor.scheduleAtFixedRate(reSubscribe, 0, 12, TimeUnit.HOURS);
        //Every 10 seconds, print the order book.
        executor.scheduleAtFixedRate(printOrderBook, 0, 10, TimeUnit.SECONDS);
    }
}
