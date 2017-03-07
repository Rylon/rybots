package rybots.bot;
import battlecode.common.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public strictfp class Archon {
    RobotController rc;

    List<Float> bulletCountHistory = new ArrayList<>();
    Boolean hiringGardenersEnabled = true;

    public Archon(RobotController rc) {
        this.rc = rc;
    }

    public final void run() throws GameActionException {
        System.out.println("Spawning: Archon");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

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
                // First: Wait for a random interval before attempting to hire.
                if( Math.random() < .01 ) {
                    // If hiring gardeners is allowed, and we have the resources, do it!
                    Direction dir = randomDirection();
                    if ( hiringGardenersEnabled && rc.canHireGardener( dir ) ) {
                        rc.hireGardener( dir );
                    }
                }

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

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    private Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }
}
