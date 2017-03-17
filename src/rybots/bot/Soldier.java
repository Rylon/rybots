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
        // lookForTrouble();
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
    }

    /**
     * The soldier moves about randomly like a heavily armed bumblebee...
     *
     * @throws GameActionException
     */
    private void patrol() throws GameActionException {
        Direction randomDirection = randomDirection();
        tryMove(randomDirection);
    }
}
