package mindustry.entities.units;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class AIController implements UnitController{
    protected static final Vec2 vec = new Vec2();
    protected static final float rotateBackTimer = 60f * 5f;
    protected static final int timerTarget = 0, timerTarget2 = 1, timerTarget3 = 2, timerTarget4 = 3;

    protected Unit unit;
    protected Interval timer = new Interval(4);
    protected AIController fallback;
    protected float noTargetTime;

    /** main target that is being faced */
    protected Teamc target;
    protected Teamc bomberTarget;


    public enum DroneState {
        S1_Idle("S1"),
        S2_Moving("S2"),
        S3_Repairing("S3"),
        S4_Waiting("S4");

        private final String stateCode;

        DroneState(String code) {
            this.stateCode = code;
        }

        public String getStateCode() {
            return stateCode;
        }
    }

    private DroneState currentState = DroneState.S1_Idle;
    private float stateTimer = 0f;
    private Building repairTarget = null;
    private float repairProgress = 0f;
    private static final float REPAIR_TIME = 300f;
    private static final float WAIT_TIME = 120f;
    private boolean hasLoggedState = false;

    public AIController() {
        resetTimers();


        System.out.println("[AIController] Initializing drone controller...");
        System.out.println("[AIController] Log file will be created at: " + DroneLogger.getLogFilePath());

        logStateChange(currentState);
    }

    private void logStateChange(DroneState newState) {
        if (currentState != newState || !hasLoggedState) {
            DroneState oldState = currentState;
            currentState = newState;
            hasLoggedState = true;

            DroneLogger.logState(newState.getStateCode());


            System.out.println("[AIController] State transition: " +
                    (oldState != null ? oldState.getStateCode() : "NULL") + " -> " + newState.getStateCode());


            Log.info("[DroneLog] Unit @ state changed to @",
                    unit != null ? unit.id : "unknown", newState.getStateCode());
        }
    }

    protected void resetTimers(){
        timer.reset(timerTarget, Mathf.random(40f));
        timer.reset(timerTarget2, Mathf.random(60f));
    }

    @Override
    public void updateUnit(){
        if(unit == null) return;

        //use fallback AI when possible
        if(useFallback() && (fallback != null || (fallback = fallback()) != null)){
            if(fallback.unit != unit) fallback.unit(unit);
            fallback.updateUnit();
            return;
        }

        updateDroneStateMachine();

        updateVisuals();
        updateTargeting();
        updateMovement();
    }

    private void updateDroneStateMachine() {
        if(unit == null) return;

        stateTimer += Time.delta;

        switch (currentState) {
            case S1_Idle:
                handleIdleState();
                break;

            case S2_Moving:
                handleMovingState();
                break;

            case S3_Repairing:
                handleRepairingState();
                break;

            case S4_Waiting:
                handleWaitingState();
                break;
        }
    }

    private void handleIdleState() {

        if (stateTimer > 60f) {
            repairTarget = findDamagedBuilding();

            if (repairTarget != null) {
                logStateChange(DroneState.S2_Moving);
                stateTimer = 0f;
            } else {

                if (Mathf.random() < 0.02f) {
                    createSimulatedRepairTarget();
                    if (repairTarget != null) {
                        logStateChange(DroneState.S2_Moving);
                        stateTimer = 0f;
                    }
                }
            }
        }
    }

    private void handleMovingState() {
        if (repairTarget == null) {
            logStateChange(DroneState.S1_Idle);
            stateTimer = 0f;
            return;
        }


        moveTo(repairTarget, 32f); // 移动到建筑32像素范围内

        if (unit.within(repairTarget, 40f)) {
            logStateChange(DroneState.S3_Repairing);
            stateTimer = 0f;
            repairProgress = 0f;
        }


        if (stateTimer > 600f) { // 10秒超时
            logStateChange(DroneState.S1_Idle);
            stateTimer = 0f;
            repairTarget = null;
        }
    }

    private void handleRepairingState() {
        if (repairTarget == null || !repairTarget.isValid()) {
            logStateChange(DroneState.S1_Idle);
            stateTimer = 0f;
            repairTarget = null;
            return;
        }

        if (!unit.within(repairTarget, 50f)) {
            logStateChange(DroneState.S2_Moving);
            stateTimer = 0f;
            return;
        }

        repairProgress += Time.delta;


        if (repairProgress >= REPAIR_TIME) {
            logStateChange(DroneState.S4_Waiting);
            stateTimer = 0f;
        }
    }

    private void handleWaitingState() {

        if (stateTimer >= WAIT_TIME) {
            logStateChange(DroneState.S1_Idle);
            stateTimer = 0f;
            repairTarget = null;
            repairProgress = 0f;
        }
    }

    private Building findDamagedBuilding() {
        if (unit == null) return null;


        Building closest = null;
        float minDist = 999999f;


        for(int x = (int)(unit.x/8f) - 25; x < (int)(unit.x/8f) + 25; x++){
            for(int y = (int)(unit.y/8f) - 25; y < (int)(unit.y/8f) + 25; y++){
                Building building = Vars.world.build(x, y);
                if(building != null && building.team == unit.team){
                    float dist = Mathf.dst(unit.x, unit.y, building.x, building.y);
                    if(dist < minDist && dist < 200f){
                        closest = building;
                        minDist = dist;
                    }
                }
            }
        }

        return closest;
    }

    private void createSimulatedRepairTarget() {

        repairTarget = findDamagedBuilding();
    }

    /**
     * @return whether controller state should not be reset after reading.
     * Do not override unless you know exactly what you are doing.
     * */
    public boolean keepState(){
        return false;
    }

    @Override
    public void afterRead(Unit unit){

        if (this.unit != unit) {
            hasLoggedState = false;
            logStateChange(currentState);
        }
    }

    @Override
    public boolean isLogicControllable(){
        return true;
    }

    public void stopShooting(){
        if(unit != null && unit.mounts != null) {
            for(var mount : unit.mounts){
                mount.shoot = false;
            }
        }
    }

    public AIController fallback(){
        return null;
    }

    public boolean useFallback(){
        return false;
    }

    public void updateVisuals(){
        if(unit != null && unit.isFlying()){
            if(unit.type != null && unit.type.wobble) {
                unit.wobble();
            }
            unit.lookAt(unit.prefRotation());
        }
    }

    public void updateMovement(){

    }

    public void updateTargeting(){

        if(currentState == DroneState.S3_Repairing) {
            return;
        }

        if(unit != null && unit.hasWeapons()){
            updateWeapons();
        }
    }

    /** For ground units: Looks at the target, or the movement position. Does not apply to non-omni units. */
    public void faceTarget(){
        if(unit == null || unit.type == null) return;

        if(unit.type.omniMovement || unit instanceof Mechc){
            if(!Units.invalidateTarget(target, unit, unit.range()) && unit.type.faceTarget && unit.type.hasWeapons()){
                if(unit.type.weapons != null && !unit.type.weapons.isEmpty()) {
                    unit.lookAt(Predict.intercept(unit, target, unit.type.weapons.first().bullet.speed));
                }
            }else if(unit.moving()){
                unit.lookAt(unit.vel().angle());
            }
        }
    }

    public void faceMovement(){
        if(unit != null && unit.type != null && (unit.type.omniMovement || unit instanceof Mechc) && unit.moving()){
            unit.lookAt(unit.vel().angle());
        }
    }

    public boolean invalid(Teamc target){
        if(unit == null) return true;
        return Units.invalidateTarget(target, unit.team, unit.x, unit.y);
    }

    public void pathfind(int pathTarget){
        pathfind(pathTarget, true);
    }

    public void pathfind(int pathTarget, boolean stopAtTargetTile){
        if(unit == null || unit.type == null) return;

        int costType = unit.type.flowfieldPathType;

        Tile tile = unit.tileOn();
        if(tile == null) return;

        if(pathfinder == null) return;

        Tile targetTile = pathfinder.getField(unit.team, costType, pathTarget).getNextTile(tile);

        if((tile == targetTile && stopAtTargetTile) || !unit.canPass(targetTile.x, targetTile.y)) return;

        unit.movePref(vec.trns(unit.angleTo(targetTile.worldx(), targetTile.worldy()), prefSpeed()));
    }

    public Vec2 alterPathfind(Vec2 vec){
        return vec;
    }

    public void targetInvalidated(){
        //TODO: try this for normal units, reset the target timer
    }

    public void updateWeapons(){
        if(unit == null || unit.type == null) return;

        float rotation = unit.rotation - 90;
        boolean ret = retarget();

        if(ret){
            target = findMainTarget(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        noTargetTime += Time.delta;

        if(invalid(target)){
            if(target != null && !target.isAdded()){
                targetInvalidated();
            }
            target = null;
        }else{
            noTargetTime = 0f;
        }

        unit.isShooting = false;

        if(unit.mounts == null) return;

        for(var mount : unit.mounts){
            if(mount == null || mount.weapon == null) continue;

            Weapon weapon = mount.weapon;
            float wrange = weapon.range();

            //let uncontrollable weapons do their own thing
            if(!weapon.controllable || weapon.noAttack) continue;

            if(!weapon.aiControllable){
                mount.rotate = false;
                continue;
            }

            float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y),
                    mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

            if(unit.type.singleTarget){
                mount.target = target;
            }else{
                if(ret){
                    mount.target = findTarget(mountX, mountY, wrange,
                            weapon.bullet != null ? weapon.bullet.collidesAir : true,
                            weapon.bullet != null ? weapon.bullet.collidesGround : true);
                }

                if(checkTarget(mount.target, mountX, mountY, wrange)){
                    mount.target = null;
                }
            }

            boolean shoot = false;

            if(mount.target != null){
                float hitSize = mount.target instanceof Sized ? ((Sized)mount.target).hitSize()/2f : 0f;
                shoot = mount.target.within(mountX, mountY, wrange + hitSize) && shouldShoot();

                if(unit.type.autoDropBombs && !shoot){
                    if(bomberTarget == null || !bomberTarget.isAdded() ||
                            !bomberTarget.within(unit, unit.hitSize/2f + ((Sized)bomberTarget).hitSize()/2f)){
                        bomberTarget = Units.closestTarget(unit.team, unit.x, unit.y, unit.hitSize,
                                u -> !u.isFlying(), t -> true);
                    }
                    shoot = bomberTarget != null;
                }

                if(weapon.bullet != null) {
                    Vec2 to = Predict.intercept(unit, mount.target, weapon.bullet.speed);
                    mount.aimX = to.x;
                    mount.aimY = to.y;
                }
            }

            mount.shoot = mount.rotate = shoot;
            if(!shouldFire()){
                mount.shoot = false;
            }

            unit.isShooting |= mount.shoot;

            if(mount.target == null && !shoot &&
                    !Angles.within(mount.rotation, mount.weapon.baseRotation, 0.01f) &&
                    noTargetTime >= rotateBackTimer){
                mount.rotate = true;
                Tmp.v1.trns(unit.rotation + mount.weapon.baseRotation, 5f);
                mount.aimX = mountX + Tmp.v1.x;
                mount.aimY = mountY + Tmp.v1.y;
            }

            if(shoot){
                unit.aimX = mount.aimX;
                unit.aimY = mount.aimY;
            }
        }
    }

    public boolean checkTarget(Teamc target, float x, float y, float range){
        if(unit == null) return true;
        return Units.invalidateTarget(target, unit.team, x, y, range);
    }

    /** @return whether the unit should actually fire bullets (as opposed to just targeting something) */
    public boolean shouldFire(){

        return currentState != DroneState.S3_Repairing;
    }

    public boolean shouldShoot(){

        return currentState != DroneState.S3_Repairing;
    }

    public Teamc targetFlag(float x, float y, BlockFlag flag, boolean enemy){
        if(unit == null || unit.team == Team.derelict || indexer == null) return null;
        return Geometry.findClosest(x, y,
                enemy ? indexer.getEnemy(unit.team, flag) : indexer.getFlagged(unit.team, flag));
    }

    public Teamc target(float x, float y, float range, boolean air, boolean ground){
        if(unit == null) return null;
        return Units.closestTarget(unit.team, x, y, range,
                u -> u.checkTarget(air, ground),
                t -> ground && (unit.type.targetUnderBlocks || !t.block.underBullets));
    }

    public boolean retarget(){
        return timer.get(timerTarget, target == null ? 40 : 90);
    }

    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        return findTarget(x, y, range, air, ground);
    }

    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return target(x, y, range, air, ground);
    }

    public void commandTarget(Teamc moveTo){

    }

    public void commandPosition(Vec2 pos){

    }

    /** Called after this controller is assigned a unit. */
    public void init(){
        hasLoggedState = false;
        logStateChange(currentState);
    }

    public Tile getClosestSpawner(){
        if(unit == null || Vars.spawner == null) return null;
        return Geometry.findClosest(unit.x, unit.y, Vars.spawner.getSpawns());
    }

    public void unloadPayloads(){
        if(unit instanceof Payloadc && target instanceof Building){
            Payloadc pay = (Payloadc)unit;
            if(pay.hasPayload() && pay.payloads().peek() instanceof UnitPayload){
                if(target.within(unit, Math.max(unit.type().range + 1f, 75f))){
                    pay.dropLastPayload();
                }
            }
        }
    }

    public void circleAttack(float circleLength){
        if(target == null || unit == null) return;

        vec.set(target).sub(unit);

        float ang = unit.angleTo(target);
        float diff = Angles.angleDist(ang, unit.rotation());

        if(diff > 70f && vec.len() < circleLength){
            vec.setAngle(unit.vel().angle());
        }else{
            vec.setAngle(Angles.moveToward(unit.vel().angle(), vec.angle(), 6f));
        }

        vec.setLength(prefSpeed());

        unit.movePref(vec);
    }

    public void circle(Position target, float circleLength){
        circle(target, circleLength, prefSpeed());
    }

    public void circle(Position target, float circleLength, float speed){
        if(target == null || unit == null) return;

        vec.set(target).sub(unit);

        if(vec.len() < circleLength){
            vec.rotate((circleLength - vec.len()) / circleLength * 180f);
        }

        vec.setLength(speed);

        unit.movePref(vec);
    }

    public void moveTo(Position target, float circleLength){
        moveTo(target, circleLength, 100f);
    }

    public void moveTo(Position target, float circleLength, float smooth){
        moveTo(target, circleLength, smooth, unit != null ? unit.isFlying() : false, null);
    }

    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, Vec2 offset){
        moveTo(target, circleLength, smooth, keepDistance, offset, false);
    }

    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, Vec2 offset, boolean arrive){
        if(target == null || unit == null || unit.type == null) return;

        float speed = prefSpeed();

        vec.set(target).sub(unit);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((unit.dst(target) - circleLength) / smooth, -1f, 1f);

        vec.setLength(speed * length);

        if(arrive){
            Tmp.v3.set(-unit.vel.x / unit.type.accel * 2f, -unit.vel.y / unit.type.accel * 2f)
                    .add((target.getX() - unit.x), (target.getY() - unit.y));
            vec.add(Tmp.v3).limit(speed * length);
        }

        if(length < -0.5f){
            if(keepDistance){
                vec.rotate(180f);
            }else{
                vec.setZero();
            }
        }else if(length < 0){
            vec.setZero();
        }

        if(offset != null){
            vec.add(offset);
            vec.setLength(speed * length);
        }


        if(vec.isNaN() || vec.isInfinite() || vec.isZero()) return;

        if(!unit.type.omniMovement && unit.type.rotateMoveFirst){
            float angle = vec.angle();
            unit.lookAt(angle);
            if(Angles.within(unit.rotation, angle, 3f)){
                unit.movePref(vec);
            }
        }else{
            unit.movePref(vec);
        }
    }

    public float prefSpeed(){
        return unit != null ? unit.speed() : 0f;
    }

    @Override
    public void unit(Unit unit){
        if(this.unit == unit) return;

        this.unit = unit;
        init();
    }

    @Override
    public Unit unit(){
        return unit;
    }


    public DroneState getCurrentState() {
        return currentState;
    }


    public void forceStateChange(DroneState newState) {
        logStateChange(newState);
        stateTimer = 0f;
    }
}