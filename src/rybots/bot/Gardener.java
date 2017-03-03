package rybots.bot;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public strictfp class Gardener {
    private RobotController rc;
    private Boolean has_built_a_garden = false;

    public Gardener(RobotController rc) {
        this.rc = rc;
    }

    public final void run() throws GameActionException {
        System.out.println("Spawning: Gardener");

        while (true) {
            try {

                if( has_built_a_garden ) {
                    // garden logic here...
                    System.out.println("[gardener] have a garden");
                }
                else {
                    System.out.println("[gardener] no garden, moving to good location");
                    // Not got a garden yet, so go build one!

                    // Where is the Archon?
                    int xPos = rc.readBroadcast(0);
                    int yPos = rc.readBroadcast(1);
                    MapLocation archonLoc = new MapLocation(xPos,yPos);

                    float distance = rc.getType().sensorRadius - gardenRadius() - 0.01f;
                    List<MapLocation> potentialSpots = getNSurroundingLocations(rc.getLocation(),12, distance);

                    for (MapLocation l : potentialSpots) {
//                        if (isGoodLocation(l)) {
                            System.out.println("found possible good location: " + l);
//                            moveTo(l);


                        if(rc.getLocation() == l) {
                            rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
                        }
                        else {
                            tryMove( rc.getLocation().directionTo(l) );
                            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
//                            return;
                        }
                        break;
//                        return;
//                        }
                    }



//                    tryMove( rc.getLocation().directionTo(archonLoc).opposite() );


                    // No good sites nearby, set indicator to red, and move randomly.
//                    tryMove( randomDirection() );
                }

//
//                // Listen for home archon's location
//                int xPos = rc.readBroadcast(0);
//                int yPos = rc.readBroadcast(1);
//                MapLocation archonLoc = new MapLocation(xPos,yPos);
//
//                // Generate a random direction
//                Direction dir = randomDirection();
//
//                // Randomly attempt to build a soldier or lumberjack in this direction
//                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01)
//                {
//                    rc.buildRobot(RobotType.SOLDIER, dir);
//                }
//                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady())
//                {
//                    rc.buildRobot(RobotType.LUMBERJACK, dir);
//                }
//
//                // Move randomly
//                tryMove(randomDirection());
//
////                if ( rc.hasTreeBuildRequirements() ) {
////                    rc.buil
////                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    private float gardenRadius() {
        float treeRadius = 1.00f;
        float myRadius = rc.getType().bodyRadius;
        float buffer = 1.0f; // so we don't set up too close to walls and whatnot

        return (2 * treeRadius) + myRadius + buffer;
    }


    /**
     * Gets a non-overlapping list surrounding locations that could fit a circle
     * of given radius and distance away from you.
     *
     * If the circles do not fit exactly, they will be evenly spaced around your
     * location.
     *
     * This method is suited to trying to find locations to spawn multiple
     * non-overlapping things.
     *
     * @param center the central location to spread the points around
     * @param radius the radius of the circles surrounding the center
     * @param distance how far away the points should be
     * @param offset the offset, in radians, to start at
     */
    public static List<MapLocation> getSurroundingLocations(MapLocation center,
                                                            float radius, float distance, float offset) {
        double opposite = (double)radius;
        double hypotenuse = (double)distance;
        double wedgeAngle = Math.asin(opposite / hypotenuse) * 2;
        int numWedges = (int)((Math.PI * 2) / wedgeAngle);

        return getNSurroundingLocations(center, numWedges, distance, offset);
    }

    /**
     * Gets a given number of equally spaced locations a given distance away from
     * a given point.
     *
     * Useful for scanning for a location to build a single thing.
     */
    public static List<MapLocation> getNSurroundingLocations(MapLocation center,
                                                             int count, float distance, float offset) {
        double step = (Math.PI * 2) / count;
        double currentAngle = offset;
        List<MapLocation> locations = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Direction d = new Direction((float)currentAngle);
            locations.add(center.add(d, distance));
            currentAngle += step;
        }

        return locations;
    }

    /**
     * See getSurroundingLocations(MapLocation, float, float, float).
     */
    public static List<MapLocation> getSurroundingLocations(MapLocation center,
                                                            float radius, float distance) {
        return getSurroundingLocations(center, radius, distance, 0.0f);
    }

    /**
     * See getNSurroundingLocations(MapLocation, int, float, float).
     */
    public static List<MapLocation> getNSurroundingLocations(MapLocation center,
                                                             int count, float distance) {
        return getNSurroundingLocations(center, count, distance, 0.0f);
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
