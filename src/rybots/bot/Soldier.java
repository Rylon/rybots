package rybots.bot;

import rybots.utils.Comms;

import battlecode.common.*;

public strictfp class Soldier extends BaseBot {

    private Team enemy;
    private MapLocation currentDestination = null;
    private Integer failedMoves = 0;

    public Soldier(RobotController rc) {
        super(rc);
        enemy = rc.getTeam().opponent();
    }

    public final void sayHello() {
        System.out.println("Spawning: Soldier");
    }

    public final void takeTurn() throws GameActionException {

        shootAtEnemies();
        lookForTrouble();
        patrol();

    }

    /**
     * The soldier stays put and fires at nearby enemy soldiers.
     *
     * @throws GameActionException
     */
    private void shootAtEnemies() throws GameActionException {
        // See if there are any nearby enemy robots
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

        // If there are some...
        if (robots.length > 0) {

            // Report the location for other soldiers to read
            rc.broadcastFloat( Comms.SOLDIER_ENEMY_SPOTTED_X_CHANNEL, robots[0].location.x );
            rc.broadcastFloat( Comms.SOLDIER_ENEMY_SPOTTED_Y_CHANNEL, robots[0].location.y );

            // And we have enough bullets, and haven't attacked yet this turn...
            if (rc.canFireTriadShot()) {
                // ...Then fire a bullet in the direction of the enemy.
                rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
            }
            // Return so the robot stays near these enemies until they are destroyed.
            return;
        }
    }

    /**
     * The soldier attempts to move toward a location which was broadcast, which should contain enemies.
     *
     * @throws GameActionException
     */
    private void lookForTrouble() throws GameActionException {

        if ( currentDestination == null ) {

            // See if there are any coordinates broadcasted yet...
            Float x = rc.readBroadcastFloat( Comms.SOLDIER_ENEMY_SPOTTED_X_CHANNEL );
            Float y = rc.readBroadcastFloat( Comms.SOLDIER_ENEMY_SPOTTED_Y_CHANNEL );

            // System.out.println( "Broadcast coordinates: " + x + ":" + y);

            if ( x != 0.0 && y != 0.0 ) {
                currentDestination = new MapLocation(x,y);
            }

        }
        else {
            rc.setIndicatorLine( rc.getLocation(), currentDestination, 180, 0, 0 );
            // Continue toward the current destination...

            if ( !rc.hasMoved() ) {
                // If we are unable to move to the destination this time, increment a counter.
                if (!tryMove(rc.getLocation().directionTo(currentDestination))) {
                    failedMoves++;
                }
            }

            // If we have failed to move to the destination too many times, give up and pick a new destination
            // to avoid getting stuck.
            if( failedMoves >= 10 ) {
                failedMoves = 0;
                currentDestination = null;
                return;
            }

            // Have we arrived yet? If the distance is less than the radius of this robot, we've made it!
            // System.out.println( rc.getLocation().distanceTo( currentDestination ));
            // if ( rc.getLocation().distanceTo( currentDestination ) <= rc.getType().bodyRadius ) {
        }
    }

    /**
     * The soldier moves about randomly like a heavily armed bumblebee...
     *
     * @throws GameActionException
     */
    private void patrol() throws GameActionException {
        Direction randomDirection = randomDirection();
        if (canMove(randomDirection)) {
            tryMove(randomDirection);
        }
    }
}
