package dev.nayte;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.WallObject;

import java.awt.*;

public class VeinState {
    @Getter(AccessLevel.PACKAGE)
    private final WallObject vein;

    @Getter(AccessLevel.PACKAGE)
    private final VeinLocation location;

    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private VeinStatus status;

    @Getter(AccessLevel.PACKAGE)
    private int ticksSinceMiningStarted = 0; // TODO: add fuzzing to account for the time it takes a player to _actually_ start the timer by successfully mining an ore?

    @Getter(AccessLevel.PACKAGE)
    private boolean miningStarted = false;

    public VeinState(Client client, WallObject vein, VeinStatus status) {
        this.vein = vein;
        this.status = status;
        this.location = VeinLocation.getByLocalPoint(client, vein.getLocalLocation());
    }

    void tick() {
        if(miningStarted && status != VeinStatus.DEPLETED) {
            ticksSinceMiningStarted += 1;
        }
    }

    void onDepleted() {
        status = VeinStatus.DEPLETED;
    }

    void onRespawned() {
        status = VeinStatus.AVAILABLE;
        ticksSinceMiningStarted = 0;
        miningStarted = false;
    }

    void onMiningStarted() {
        miningStarted = true;
    }

    boolean shouldShowTimer() {
        return miningStarted && status == VeinStatus.AVAILABLE;
    }

    double getTimerProgress() {
        return Math.max(1 - ((double) ticksSinceMiningStarted / this.getLocation().getMinTicks()), 0);
    }

    Color getTimerColor() {
        double percent = getTimerProgress();

        int red = (int) Math.round(255 * (1 - percent));
        int green = (int) Math.round(255 * percent);
        int blue = 0;

        return new Color(red, green, blue);
    }
}
