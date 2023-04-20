package org.example;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class OrderData implements CEndpoint.UpdateOrderDataInterface {
    //To represent the order book, I decided to use a tree of prices,
    //with a hashmap mapping prices to volume.
    //This is better than simply using a hashmap as I would have to perform a sort every time I print.
    //This is also better than using a treemap as the insertion and deletion of prices are still O(log(n)),
    //but the modification of volume for existing prices is O(1) as I am updating a hashmap.
    //For comparison, the treemap modification would be O(log(n)), so my approach is faster.
    TreeSet<Float> priceTree;
    HashMap<Float, Float> countMap;
    boolean isBid;
    int lastUpdateId;
    Iterator<Float> treeItr;

    OrderData(boolean isBid, int lastUpdateId) {
        this.priceTree = new TreeSet<>();
        this.countMap = new HashMap<>();
        this.isBid = isBid;
        this.lastUpdateId = lastUpdateId;
    }

    //Apply the snapshot to our priceTree and countMap
    void applySnapshot(JSONObject snapshot) {
        try {
            JSONArray data;
            if (isBid) {
                data = snapshot.getJSONArray("bids");
            } else {
                data = snapshot.getJSONArray("asks");
            }
            for (int i = 0; i < data.length(); i++) {
                JSONArray entry = data.getJSONArray(i);
                priceTree.add((float) entry.getDouble(0));
                countMap.put((float) entry.getDouble(0), (float) entry.getDouble(1));
            }
        } catch (JSONException e) {
            System.err.printf("JSONException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Update the priceTree and countMap due to receiving an event from the stream
    void update(JSONObject depthUpdate) {
        try {
            if (lastUpdateId < depthUpdate.getInt("u")) {
                JSONArray updates;
                if (isBid) {
                    updates = depthUpdate.getJSONArray("b");
                } else {
                    updates = depthUpdate.getJSONArray("a");
                }

                for (int i = 0; i < updates.length(); i++) {
                    JSONArray currUpdate = updates.getJSONArray(i);
                    float currPrice = (float) currUpdate.getDouble(0);
                    float currCount = (float) currUpdate.getDouble(1);
                    if (currCount == 0) {
                        priceTree.remove(currPrice);
                        countMap.remove(currPrice);
                    } else {
                        countMap.put(currPrice, currCount);
                    }
                }
            }
        } catch (JSONException e) {
            System.err.printf("JSONException: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Reset the iterators so we can begin again
    void prepForItr() {
        if (isBid) {
            treeItr = priceTree.descendingIterator();
        } else {
            treeItr = priceTree.iterator();
        }
    }

    //Print the order book
    void print() {
        if (treeItr.hasNext()) {
            float currPrice = treeItr.next();
            if (isBid) {
                System.out.printf("%-10s %10s \t ", countMap.get(currPrice), currPrice);
            } else {
                System.out.printf("%-10s %10s\n", currPrice, countMap.get(currPrice));
            }
        } else {
            if (isBid) {
                System.out.printf("%-10s %10s \t ", " ", " ");
            } else {
                System.out.printf("%-10s %10s\n", " ", " ");
            }
        }
    }

    //Update the priceTree and countMap due to receiving an event from the stream
    public void updateOrderData(String message) throws JSONException {
        JSONObject messageObject = new JSONObject(message);
        update(messageObject);
    }
}
