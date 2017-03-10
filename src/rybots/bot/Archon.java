package rybots.bot;

import battlecode.common.*;
import rybots.utils.Comms;

import java.util.List;
import java.util.ArrayList;

public strictfp class Archon extends BaseBot {

    List<Float> bulletCountHistory = new ArrayList<>();
    Boolean hiringGardenersEnabled = true;
    Boolean archonFirstTurn        = true;

    public Archon(RobotController rc) {
        super(rc);
    }

    public final void run() throws GameActionException {
        System.out.println("Spawning: Archon");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                if( archonFirstTurn ) {
                    // Set up some controls for the first turn...
                    rc.broadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED, true );

                    archonFirstTurn = false;
                }

                // Measure the percentage rate of change of bullets over 100 turns, and if it is 30% or more,
                // stop hiring Gardeners and hire Soldiers!
                if(bulletCountHistory.size() >= 101) {
                    System.out.println("[archon] Taking bullet sample!" + bulletCountHistory.get(0) + " to " +  bulletCountHistory.get(100) );
                    System.out.println("[archon]   - % diff : " + ((bulletCountHistory.get(100) - bulletCountHistory.get(0)) / bulletCountHistory.get(100) * 100) );
                    System.out.println("[archon]   - start  : " + bulletCountHistory.get(0)  );
                    System.out.println("[archon]   - mid    : " + bulletCountHistory.get(50) );
                    System.out.println("[archon]   - end    : " + bulletCountHistory.get(100));

                    // The rules for disabling gardeners:
                    //   * Rate of change over the sampling period is 30% or more.
                    //   * The start, mid and end points all showed a surplus of 1000 bullets or more.
                    if( (((bulletCountHistory.get(100) - bulletCountHistory.get(0)) / bulletCountHistory.get(100) * 100) >= 30) ||
                        ((bulletCountHistory.get(0) >= 1000) && (bulletCountHistory.get(50) >= 1000) && (bulletCountHistory.get(100) >= 1000)) ) {
                            System.out.println("[archon]   = disabling gardener construction!");
                            hiringGardenersEnabled = false;
                    }
                    else {
                        System.out.println("[archon]   = enabling gardener construction!");
                        hiringGardenersEnabled = true;
                    }
//                    bulletCountHistory = new Float[14];
                    bulletCountHistory.clear();
                }
                else {
                    // Add the current bullet count
                    bulletCountHistory.add( rc.getTeamBullets() );
                }

                // Debug gardener hiring or not...
                if( hiringGardenersEnabled ) {
                    rc.setIndicatorDot(rc.getLocation(), 128, 255, 0);
                }
                else {
                    rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
                }

                // Hiring time!
                // Wait for a random interval before attempting to hire a gardener.
                if( Math.random() < .05 ) {
                    // If hiring gardeners is allowed, and we have the resources, do it!
                    Direction dir = randomDirection();
                    if ( hiringGardenersEnabled && rc.canHireGardener( dir ) ) {
                        rc.hireGardener( dir );
                    }
                }

                // Look for some locations around the Archon that could be used as a rally point for soldiers.
//                float distance = 10.0f;
//                List<MapLocation> potentialLocations = getNSurroundingLocations(rc.getLocation(),12, 10.0f, 0.0f);








//                // Move randomly
//                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
//                MapLocation myLocation = rc.getLocation();
//                rc.broadcast(0,(int)myLocation.x);
//                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
}
