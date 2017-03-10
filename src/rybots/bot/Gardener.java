package rybots.bot;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

public strictfp class Gardener extends BaseBot {

    private Boolean inGoodLocation = false;
    private MapLocation spawningGap;
    private Set<MapLocation> gardenTreeLocations;

    public Gardener(RobotController rc) {
        super(rc);
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

                    // Randomly select degree in the circle to start from, so when we remove the first
                    // element to create a gap to spawn units from, it will be in a random position.
                    float offsetForSpawningGap = new Random().nextFloat() * (float)(Math.PI * 2);

                    List<MapLocation> sc = getSurroundingBuildLocations(rc.getLocation(), treeRadius, distance, offsetForSpawningGap);
                    spawningGap = sc.remove(0);
                    gardenTreeLocations = new HashSet(sc);

                    // Check all tree locations and plant a tree if it is missing (not yet planted or has been destroyed).
                    for (MapLocation treeLocation : gardenTreeLocations) {
                        rc.setIndicatorDot(treeLocation, 128, 0, 0);
                        Direction plantingLocation = rc.getLocation().directionTo( treeLocation );
                        if( rc.canPlantTree( plantingLocation ) ) {
                            rc.plantTree( plantingLocation );
                        }

                    }

                    // Get a list of all the trees in this garden, pick the first one then figure out which one
                    // is the weakest and finally water it.
                    TreeInfo[] gardenTrees = rc.senseNearbyTrees( rc.getLocation(), gardenRadius(), rc.getTeam() );

                    // If we've planted at least one tree already, we can try to water the weakest.
                    if (gardenTrees.length >= 1) {
                        TreeInfo weakestTree = gardenTrees[0]; // Start with the first tree
                        for (TreeInfo tree : gardenTrees) {
                            rc.setIndicatorDot(tree.location, 64, 128, 0);
                            if (tree.health < weakestTree.health) {
                                weakestTree = tree;
                            }
                        }

                        if (rc.canWater(weakestTree.location)) {
                            rc.water(weakestTree.location);
                            rc.setIndicatorDot(weakestTree.location, 0, 128, 255);
                        }
                    }

                    if( rc.canWater( weakestTree.location ) ) {
                        rc.water( weakestTree.location );
                        rc.setIndicatorDot(weakestTree.location, 0, 128, 255);
                    }

                    // Build soldiers on a random interval...
                    if ( Math.random() < 0.4 ) {
                        if( rc.canBuildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap)) ) {
                            rc.buildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap) );
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


                    // Look for some locations within sensor range that could fit our garden.
                    float distance = rc.getType().sensorRadius - gardenRadius() - 0.01f;
                    List<MapLocation> potentialLocations = getNSurroundingLocations(rc.getLocation(),12, distance, 0.0f);

                    // Debug: show all potential spots in yellow and any good spots in green
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
                    }

                    // No good sites nearby, set indicator to red, and move randomly.
                    tryMove( randomDirection() );
                    rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
                }


                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks whether a location is suitable for building a garden.
     * @param location
     * @return true if the given location can fit a circle of `gardenRadius()` size and there are no other robots there.
     * @throws GameActionException
     */
    private boolean isSuitableLocation(MapLocation location) throws GameActionException {
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

        return rc.onTheMap(location, gardenRadius()) && !rc.isCircleOccupiedExceptByThisRobot(location, gardenRadius());
    }

    /**
     * A "garden" is a circle of trees around a gardener, this is used to determine the size of the garden as a circle
     * so map locations that can fit one can be determined.
     * @return
     */
    private float gardenRadius() {
        float treeRadius = 1.00f;
        float myRadius   = rc.getType().bodyRadius;
        float buffer     = 1.1f; // Make the garden radius this much larger, to make larger gaps between gardens.

        return (2 * treeRadius) + myRadius + buffer;
    }

    /**
     * Returns a list of MapLocations, each one representing a circle of radius `buildItemRadius`,
     * arranged evenly in a circle of radius `outerRadius`.
     *
     * Useful to find potential locations nearby capable of fitting an item of radius `buildItemRadius`.
     *
     * @param center          the center of the outer circle
     * @param buildItemRadius the radius of the inner circles
     * @param outerRadius     the radius of the outer circle
     * @param offset          the offset, in radians, to start at
     * @return                a List of MapLocations
     */
    public static List<MapLocation> getSurroundingBuildLocations(MapLocation center, float buildItemRadius, float outerRadius, float offset) {
        double opposite = (double)buildItemRadius;
        double hypotenuse = (double)outerRadius;
        double wedgeAngle = Math.asin(opposite / hypotenuse) * 2;
        int numLocations = (int) ((Math.PI * 2) / wedgeAngle);

        return getNSurroundingLocations(center, numLocations, outerRadius, offset);
    }

    /**
     * Gets a list of MapLocations, equally spaced in a circle of radius `distance` from a `center` MapLocation
     *
     * Similar to `getSurroundingBuildLocations`, but allows you to specify the number of locations around the circle.
     *
     * @param center       the center point of the circle
     * @param numLocations number of locations to find around the circle
     * @param radius       the radius of the circle
     * @param offset       the offset, in radians, to start the circle of locations from
     * @return             a List of MapLocations
     */
    public static List<MapLocation> getNSurroundingLocations(MapLocation center, int numLocations, float radius, float offset) {
        double step = (Math.PI * 2) / numLocations;
        double currentAngle = offset;
        List<MapLocation> locations = new ArrayList<>(numLocations);

        for (int i = 0; i < numLocations; i++) {
            Direction direction = new Direction( (float)currentAngle );
            locations.add( center.add(direction, radius) );
            currentAngle += step;
        }

        return locations;
    }

}
