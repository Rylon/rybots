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
        MapLocation myLocation = rc.getLocation();

        // See if there are any nearby enemy robots
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

        // If there are some...
        if (robots.length > 0) {
            // And we have enough bullets, and haven't attacked yet this turn...
            if (rc.canFireTriadShot()) {
                // ...Then fire a bullet in the direction of the enemy.
                rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
            }
        }

        Direction randomDirection = randomDirection();
//        System.out.println("Moving: " + randomDirection);

        // Move randomly
        tryMove(randomDirection);
    }
}
