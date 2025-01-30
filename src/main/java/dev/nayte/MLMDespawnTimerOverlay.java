package dev.nayte;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.inject.Inject;
import java.awt.*;

public class MLMDespawnTimerOverlay extends Overlay {
    private final MLMDespawnTimerPlugin plugin;
    private final MLMDespawnTimerConfig config;
    private final Client client;

    @Inject
    private MLMDespawnTimerOverlay(MLMDespawnTimerPlugin plugin, MLMDespawnTimerConfig config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
//        if(config.showTileIndicator()) {
//            for (VeinState veinState : plugin.getAvailableVeins()) {
//                WallObject wallObject = veinState.getVein();
//                LocalPoint localPoint = LocalPoint.fromWorld(client, wallObject.getWorldLocation());
//                if (localPoint == null) {
//                    continue;
//                }
//
//                Point point = Perspective.localToCanvas(client, localPoint, client.getPlane());
//
//                if (point == null) {
//                    continue;
//                }
//
//                String str = MessageFormat.format("Ticks: {0}", veinState.getTicksSinceMiningStarted());
//
//                Color color = null;
//                if(veinState.getStatus() == VeinStatus.DEPLETED) {
//                    color = new Color(255, 0, 0);
//                } else if(veinState.isMiningStarted()) {
//                    color = new Color(255, 255, 0);
//                } else {
//                    color = new Color(0, 255, 0);
//                }
//
//                OverlayUtil.renderTileOverlay(graphics, wallObject, str, color);
//            }
//        }

        for(VeinState veinState : plugin.getAvailableVeins()) {
            if(!veinState.shouldShowTimer()) {
                continue;
            }

            LocalPoint lp = LocalPoint.fromWorld(client, veinState.getVein().getWorldLocation());
            if(lp == null) {
                continue;
            }

            Point point = Perspective.localToCanvas(client, lp, client.getPlane(), 150);
            if(point == null) {
                continue;
            }

            ProgressPieComponent pie = new ProgressPieComponent();
            pie.setPosition(point);
            pie.setFill(veinState.getTimerColor());
            pie.setBorderColor(veinState.getTimerColor());
            pie.setProgress(veinState.getTimerProgress());
            pie.render(graphics);
        }

        return null;
    }
}
