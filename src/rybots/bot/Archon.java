package rybots.bot;
import battlecode.common.*;

import java.util.List;
import java.util.ArrayList;

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

                // Generate a random direction
                Direction dir = randomDirection();

                System.out.println("[archon] Current bullet count: " + rc.getTeamBullets());

                // If we've enough history to gauge bullet growth, check if the rate of bullet growth is over
                // 40% and if it is, stop building gardeners for a bit...
                if(bulletCountHistory.size() >= 1501) {
                    System.out.println("[archon] Taking bullet sample! " + bulletCountHistory.get(0) + " to " +  bulletCountHistory.get(1500));
                    if( ((bulletCountHistory.get(1500) - bulletCountHistory.get(0)) / bulletCountHistory.get(1500) * 100) >= 30 ) {
                        System.out.println("[archon] Disabling gardener construction!");
                        hiringGardenersEnabled = false;
                    }
                    else {
                        System.out.println("[archon] Enabling gardener construction!");
                        hiringGardenersEnabled = true;
                    }
//                    bulletCountHistory = new Float[14];
                    bulletCountHistory.clear();
                }
                else {
                    // Add the current bullet count
                    bulletCountHistory.add( rc.getTeamBullets() );
                }

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
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
