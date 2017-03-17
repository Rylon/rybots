package rybots.bot;
import battlecode.common.*;

public strictfp class Soldier extends BaseBot {

    Team enemy;

    public Soldier(RobotController rc) {
        super(rc);
        enemy = rc.getTeam().opponent();
    }

    public final void sayHello() {
        System.out.println("Spawning: Soldier");
    }

    public final void takeTurn() throws GameActionException {

        shootAtEnemies();
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
     * The soldier moves about randomly like a heavily armed bumblebee...
     *
     * @throws GameActionException
     */
    private void patrol() throws GameActionException {
        Direction randomDirection = randomDirection();
        tryMove(randomDirection);
    }
}
