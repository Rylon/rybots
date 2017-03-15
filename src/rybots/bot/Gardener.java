package rybots.bot;

import battlecode.common.*;

import rybots.utils.Comms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

public strictfp class Gardener extends BaseBot {

    private Boolean inGoodLocation = false;
    private MapLocation spawningGap;
    private Set<MapLocation> gardenTreeLocations;

    List<Boolean> scoutHealthChecks = new ArrayList<>();

    public Gardener(RobotController rc) {
        super(rc);
    }

    public final void sayHello() {
        System.out.println("Spawning: Gardener");
    }

    public final void takeTurn() throws GameActionException {

        if( inGoodLocation ) {
            buildAndMaintainGarden();
        }
        else {
            searchForGardenLocation();
        }

    }
    
    /**
     * The gardener stays put and plants/waters trees.
     *
     * @throws GameActionException
     */
    private void buildAndMaintainGarden() throws GameActionException {
        // We're in a good location, so stay put and build/maintain the garden.

//                    System.out.println("[gardener] gardening...");

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

//                    /*
//                    * Gardener builds a scout if there is no scout...
//                    *   * scout sets a boolean on a heartbeat channel
//                    *   * gardener reads this boolean
//                    *   * if during the last three turns, the boolean was false this means the scout
//                    *     hasn't checked in so must be destroyed in which case so we build another...
//                    *   * archon sets the boolean to false each turn.
//                    */
//                    if( ! rc.readBroadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED ) ) {
//                        scoutHealthChecks.add(rc.readBroadcastBoolean(Comms.SCOUT_HEARTBEAT_CHANNEL));
//                        if (scoutHealthChecks.size() >= 6) {
//                            if (!scoutHealthChecks.get(0) && !scoutHealthChecks.get(1) && !scoutHealthChecks.get(2) && !scoutHealthChecks.get(3) && !scoutHealthChecks.get(4)) {
//                                rc.broadcastBoolean(Comms.SCOUT_CONSTRUCTION_ENABLED, true);
//                                System.out.println("[gardener] Scout construction enabled...");
//                            }
//                            scoutHealthChecks.clear();
//                        }
//                    }

        Direction dir = randomDirection();

        Boolean scoutBuildingEnabled = rc.readBroadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED );
        System.out.println("[gardener] Scout building status:  " + scoutBuildingEnabled);

        if( scoutBuildingEnabled && rc.canBuildRobot( RobotType.SCOUT, rc.getLocation().directionTo(spawningGap) )) {
            System.out.println("[gardener] BUILD A SCOUT!");
            rc.buildRobot( RobotType.SCOUT, rc.getLocation().directionTo(spawningGap) );
            rc.broadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED, false );
            return;
        }

        // Build soldiers on a random interval...
        if ( Math.random() < 0.4 ) {
            if( rc.canBuildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap)) ) {
                rc.buildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap) );
            }
        }
    }


    /**
     * The gardener searches for a suitable location to set up a garden.
     *
     * @throws GameActionException
     */
    private void searchForGardenLocation() throws GameActionException {
        // If we're in a good location right now, set the flag and end turn.
        if ( isSuitableLocation( rc.getLocation() ) ) {
            inGoodLocation = true;
            return;
        }

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
                return;
            }
        }

        // No good sites nearby, set indicator to red, and move randomly.
        tryMove( randomDirection() );
        rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
    }

    /**
     * Checks whether a location is suitable for building a garden.
     *
     * @param  location the MapLocation to check
     * @return          true if the given location can fit a circle of `gardenRadius()` size and there are no other robots there.
     * @throws GameActionException
     */
    private boolean isSuitableLocation(MapLocation location) throws GameActionException {
        try {
            return rc.onTheMap(location, gardenRadius()) && !rc.isCircleOccupiedExceptByThisRobot(location, gardenRadius());
        }
        catch (Exception GameActionException) {
            // Catches the following exception which was occasionally occurring.
            //   battlecode.common.GameActionException: Target circle not completely within sensor range
            return false;
        }
    }

    /**
     * A "garden" is a circle of trees around a gardener, this is used to determine the size of the garden as a circle
     * so map locations that will fit one can be determined.
     *
     * @return the radius of the garden
     */
    private float gardenRadius() {
        float treeRadius = 1.00f;
        float myRadius   = rc.getType().bodyRadius;
        float buffer     = 1.1f; // Make the garden radius this much larger, to make larger gaps between gardens.

        return (2 * treeRadius) + myRadius + buffer;
    }
}
