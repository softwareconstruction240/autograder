package passoff.server;

import com.google.gson.GsonBuilder;

public class TestFactory {

    public static Long getMessageTime() {
        /*
         * Changing this will change how long tests will wait for the server to send messages.
         * 3000 Milliseconds (3 seconds) will be enough for most computers. Feel free to change as you see fit,
         * just know increasing it can make tests take longer to run.
         * (On the flip side, if you've got a good computer feel free to decrease it)
         *
         * WHILE DEBUGGING the websocket tests, it might be useful to increase this time to give the tests
         * enough time to receive messages you send while debugging. Just make sure to decrease it when you
         * stop debugging and start running the tests again.
         */
        return 3000L;
    }

    public static GsonBuilder getGsonBuilder() {
        /*                  **NOT APPLICABLE TO MOST STUDENTS**
         * If you would like to change the way the web socket test cases serialize
         * or deserialize chess objects like ChessMove, you may add type adapters here.
         */
        GsonBuilder builder = new GsonBuilder();
        // builder.registerTypeAdapter(ChessMove.class, /*type adapter or json serializer */);
        return builder;
    }

}
