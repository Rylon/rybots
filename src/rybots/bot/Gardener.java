package rybots.bot;
import battlecode.common.*;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Random;

public strictfp class Gardener {
    private RobotController rc;
    private Boolean inGoodLocation = false;
    private MapLocation spawningGap;
    private Set<MapLocation> gardenTreeLocations;

    public Gardener(RobotController rc) {
        this.rc = rc;
    }

    public final void run() throws GameActionException {
        System.out.println("Spawning: Gardener");

        while (true) {
            try {

                if( inGoodLocation ) {
                    // We're in a good location, so stay put and build/maintain the garden.
                    System.out.println("[gardener] gardening...");

                    float treeRadius = 1.0f;
                    float distance = rc.getType().bodyRadius + 0.01f + treeRadius;
                    float offset = new Random().nextFloat() * (float)(Math.PI * 2);

                    System.out.println("[gardener] offset... " + offset);

                    List<MapLocation> sc = getSurroundingLocations(treeRadius, distance, offset);
                    spawningGap = sc.remove(0);
                    gardenTreeLocations = new HashSet(sc);

                    for (MapLocation treeLocation : gardenTreeLocations) {

                        // Plant a tree at this location if it is missing (not yet planted or has been destroyed).
                        Direction plantingLocation = rc.getLocation().directionTo( treeLocation );
                        if( rc.canPlantTree( plantingLocation ) ) {
                            rc.plantTree( plantingLocation );
                        }

                    }









                }
                else {
                    // If we're in a good location right now, set the flag and yield until the next turn.
                    if ( isSuitableLocation( rc.getLocation() ) ) {
                        inGoodLocation = true;
                        Clock.yield();
                    }

                    System.out.println("[gardener] unemployed! moving to good location");

                    // Where is the Archon?
                    // int xPos = rc.readBroadcast(0);
                    // int yPos = rc.readBroadcast(1);
                    // MapLocation archonLoc = new MapLocation(xPos,yPos);

                    // Look for some locations within sensor range that could fit our garden.
                    float distance = rc.getType().sensorRadius - gardenRadius() - 0.01f;
                    List<MapLocation> potentialLocations = getNSurroundingLocations(rc.getLocation(),12, distance);

                    // Debug: show all potential spots in yello and any good spots in green
                    for (MapLocation location : potentialLocations) {
                        if( isSuitableLocation(location) ) {
                            rc.setIndicatorDot(location, 64, 128, 0);
                        }
                        else {
                            rc.setIndicatorDot(location, 255, 255, 0);
                        }
                    }

                    // Behaviour: iterate through the potential spots and move towards to the first suitable looking one.
                    for (MapLocation location : potentialLocations) {
                        if ( isSuitableLocation(location) ) {
                            tryMove( rc.getLocation().directionTo(location) );
                            // Break out so we don't keep trying to move if there are multiple suitable locations.
                            Clock.yield();
                        }


//
//                        if(rc.getLocation() == l) {
//                            rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
//                        }
//                        else {
//                            tryMove( rc.getLocation().directionTo(l) );
//                            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
////                            return;
//                        }
//                        break;
////                        return;
////                        }
                    }

                    // No good sites nearby, set indicator to red, and move randomly.
                    tryMove( randomDirection() );
                    rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);



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

    private boolean isSuitableLocation(MapLocation l) throws GameActionException {
        // TODO: This seems to cause an exception if the target circle does not fit entirely on the map:
//        [A:GARDENER#12950@224] Gardener Exception
//        battlecode.common.GameActionException: Target circle not completely within sensor range
//        at battlecode.world.RobotControllerImpl.assertCanSenseAllOfCircle(RobotControllerImpl.java:193)
//        at battlecode.world.RobotControllerImpl.onTheMap(RobotControllerImpl.java:208)
//        at rybots.bot.Gardener.isSuitableLocation(Gardener.java:160)
//        at rybots.bot.Gardener.run(Gardener.java:89)
//        at rybots.RobotPlayer.run(RobotPlayer.java:27)
//        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
//        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
//        at java.lang.reflect.Method.invoke(Method.java:498)
//        at battlecode.instrumenter.SandboxedRobotPlayer.loadAndRunPlayer(SandboxedRobotPlayer.java:259)
//        at battlecode.instrumenter.SandboxedRobotPlayer.lambda$new$2(SandboxedRobotPlayer.java:180)
//        at java.lang.Thread.run(Thread.java:745)

        return rc.onTheMap(l, gardenRadius()) && !rc.isCircleOccupiedExceptByThisRobot(l, gardenRadius());
    }

    private float gardenRadius() {
        float treeRadius = 1.00f;
        float myRadius = rc.getType().bodyRadius;
        float buffer = 1.0f; // so we don't set up too close to walls and whatnot

        return (2 * treeRadius) + myRadius + buffer;
    }

    /**
     * Gets a list surrounding locations around this unit.
     *
     * See Utils.getSurroundingLocations.
     */
    private List<MapLocation> getSurroundingLocations(float radius, float distance) {
        return getSurroundingLocations(rc.getLocation(), radius, distance);
    }

    /**
     * Gets a list surrounding locations around this unit.
     *
     * See Utils.getSurroundingLocations.
     */
    private List<MapLocation> getSurroundingLocations(float radius, float distance,
                                              float offset) {
        return getSurroundingLocations(
                rc.getLocation(), radius, distance, offset);
    }

    /**
     * Gets a list N surrounding locations around this unit.
     *
     * See Utils.getNSurroundingLocations.
     */
    List<MapLocation> getNSurroundingLocations(int count, float distance) {
        return getNSurroundingLocations(rc.getLocation(), count, distance);
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
