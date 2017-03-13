package rybots;

import rybots.bot.*;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        RobotType thisRobotType = rc.getType();
        BaseBot thisRobot;

        switch (thisRobotType) {
            case ARCHON:
                thisRobot = new Archon(rc);
                break;
            case GARDENER:
                thisRobot = new Gardener(rc);
                break;
            case SOLDIER:
                thisRobot = new Soldier(rc);
                break;
            case SCOUT:
                thisRobot = new Scout(rc);
                break;
//            case LUMBERJACK:
//                thisRobot = new Lumberjack(rc);
//                break;
            default:
                System.out.printf("Error: Unknown robot type '" + thisRobotType + "'.");
                return;
        }

        thisRobot.sayHello();
        // Robot game loop - repeatedly call `takeTurn()` then yield to the clock.
        while (true) {
            try {
                thisRobot.takeTurn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(thisRobotType + " Exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
