package rybots.bot;
import battlecode.common.*;

import rybots.utils.Comms;

public strictfp class Scout extends BaseBot {

    public Scout(RobotController rc) {
        super(rc);
    }

    public final void sayHello() {
        System.out.println("Spawning: Scout");
    }

    public final void takeTurn() throws GameActionException {
        System.out.println("Spawning: Scout");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Move randomly
                tryMove(randomDirection());

                // Broadcast that we're alive!
                rc.broadcastBoolean( Comms.SCOUT_HEARTBEAT_CHANNEL, true );

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }
}
