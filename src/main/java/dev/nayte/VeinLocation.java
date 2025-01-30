package dev.nayte;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;

@Getter
public enum VeinLocation {
    UPPER_LEVEL(36, 40),
    LOWER_LEVEL(23, 27);

    private static final int UPPER_FLOOR_HEIGHT = -490;

    private final int minTicks;
    private final int maxTicks;

    VeinLocation(int minSeconds, int maxSeconds) {
        this.minTicks = (int) Math.round(minSeconds / 0.6d);
        this.maxTicks = (int) Math.round(maxSeconds / 0.6d);
    }

    static VeinLocation getByLocalPoint(Client client, LocalPoint point) {
        if(Perspective.getTileHeight(client, point, 0) < UPPER_FLOOR_HEIGHT) {
            return VeinLocation.UPPER_LEVEL;
        } else {
            return VeinLocation.LOWER_LEVEL;
        }
    }
}
