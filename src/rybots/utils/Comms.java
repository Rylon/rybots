package rybots.utils;

import java.util.Map;
import java.util.HashMap;

public class Comms {

    public static final int GARDENERS_BUILD_GARDENS_CHANNEL = 1;
    public static final int SCOUT_CONSTRUCTION_ENABLED      = 4;
    public static final int SCOUT_HEARTBEAT_CHANNEL         = 5;
    public static final int SOLDIER_ENEMY_SPOTTED_X_CHANNEL = 10;
    public static final int SOLDIER_ENEMY_SPOTTED_Y_CHANNEL = 11;

    public static final Map<Integer, HashMap> SOLDIER_RALLY_POINTS = new HashMap<Integer, HashMap>() {
        {
            put(0, new HashMap<String, Integer>() {
                {
                    put("x", 100);
                    put("y", 101);
                }
            });
            put(1, new HashMap<String, Integer>() {
                {
                    put("x", 102);
                    put("y", 103);
                }
            });
            put(2, new HashMap<String, Integer>() {
                {
                    put("x", 104);
                    put("y", 105);
                }
            });
        }
    };

    public static final Map<Integer, HashMap> GARDENER_RALLY_POINTS = new HashMap<Integer, HashMap>() {
        {
            put(0, new HashMap<String, Integer>() {
                {
                    put("x", 106);
                    put("y", 107);
                }
            });
            put(1, new HashMap<String, Integer>() {
                {
                    put("x", 108);
                    put("y", 109);
                }
            });
        }
    };

}
