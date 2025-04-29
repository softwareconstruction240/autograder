package passoff.server;

import com.google.gson.GsonBuilder;

public class TestFactory {

    /*
     * Changing the return value will change how long tests will wait for the server to send messages.
     * The default for runtime is 3000 Milliseconds (3 seconds), and this will be enough for most computers. 
     * Feel free to change this as you see fit, just know increasing it can make tests take longer to run.
     * (On the flip side, if you've got a good computer feel free to decrease it)
     *
     * WHILE DEBUGGING the websocket tests, the default runtime is 300000 Milliseconds (5 minutes).
     * If you feel like you would like more time to debug, you may increase the time as you please.
     * 
     * If for some reason the tests seem to time out before reaching a point in the test you feel like they
     * should be, consider changing the last return value, instead of the default debug value.
     */
    public static Long getMessageTime() {
        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
            .toString().contains("jdwp");

        if (isDebug){
            return 300000L;
        }

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
