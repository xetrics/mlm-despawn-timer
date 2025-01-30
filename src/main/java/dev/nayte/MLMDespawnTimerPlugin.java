package dev.nayte;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;

import static net.runelite.api.ObjectID.*;

@Slf4j
@PluginDescriptor(
	name = "MLM Despawn Timer"
)
public class MLMDespawnTimerPlugin extends Plugin
{
	private static final Set<Integer> MOTHERLODE_MAP_REGIONS = ImmutableSet.of(14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193);
	private static final Set<Integer> MINE_SPOTS = ImmutableSet.of(ORE_VEIN, ORE_VEIN_26662, ORE_VEIN_26663, ORE_VEIN_26664);
	private static final Set<Integer> DEPLETED_SPOTS = ImmutableSet.of(DEPLETED_VEIN_26665, DEPLETED_VEIN_26666, DEPLETED_VEIN_26667, DEPLETED_VEIN_26668);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MLMDespawnTimerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MLMDespawnTimerOverlay mlmDespawnTimerOverlay;

	@Getter(AccessLevel.PACKAGE)
	private boolean inMlm;

	@Getter(AccessLevel.PACKAGE)
	private final Set<VeinState> availableVeins = new HashSet<>();

	@Provides
	MLMDespawnTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MLMDespawnTimerConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(mlmDespawnTimerOverlay);
		inMlm = isPlayerInMlm();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(mlmDespawnTimerOverlay);
		availableVeins.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if(event.getGameState() == GameState.LOADING) {
			availableVeins.clear();
			inMlm = isPlayerInMlm();
		} else if(event.getGameState() == GameState.LOGIN_SCREEN) {
			inMlm = false;
		} else if(event.getGameState() == GameState.HOPPING) {
			availableVeins.clear();
		}
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event) {
		if(!inMlm) {
			return;
		}

		WallObject wallObject = event.getWallObject();
		VeinState existingVeinState = getVeinAtLocation(wallObject.getWorldLocation());

		if(existingVeinState != null) {
			VeinStatus newStatus = getVeinStatus(wallObject);

			if(newStatus == null) {
				availableVeins.remove(existingVeinState);
			} else {
				if (newStatus != existingVeinState.getStatus()) {
					if (newStatus == VeinStatus.DEPLETED) {
						clientThread.invokeLater(existingVeinState::onDepleted);
					} else {
						clientThread.invokeLater(existingVeinState::onRespawned);
					}
				}
			}
		} else {
			VeinStatus veinStatus = getVeinStatus(wallObject);
			if(veinStatus != null) {
				VeinState veinState = new VeinState(client, wallObject, veinStatus);
				availableVeins.add(veinState);
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if(!inMlm) {
			return;
		}

		if(event.getActor() instanceof Player) {
			Player player = (Player) event.getActor();
			clientThread.invokeLater(() -> {
				this.handlePlayerAnimationChange(player);
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		availableVeins.forEach(VeinState::tick);
	}

	private boolean isPlayerInMlm() {
		GameState gameState = client.getGameState();
		if(gameState != GameState.LOGGED_IN && gameState != GameState.LOADING) {
			return false;
		}

		int[] currentMapRegions = client.getMapRegions();

		for(int region : currentMapRegions) {
			if(!MOTHERLODE_MAP_REGIONS.contains(region)) {
				return false;
			}
		}

		return true;
	}

	private void handlePlayerAnimationChange(Player player) {
		boolean isMining = isPlayerMining(player);

		if(isMining) {
			VeinState target = findPlayerMiningTarget(player);
			if(target != null) {
				target.onMiningStarted();
			}
		}
	}

	private boolean isPlayerMining(Player player) {
		switch (player.getAnimation()) {
			case AnimationID.MINING_MOTHERLODE_3A:
			case AnimationID.MINING_MOTHERLODE_ADAMANT:
			case AnimationID.MINING_MOTHERLODE_BLACK:
			case AnimationID.MINING_MOTHERLODE_BRONZE:
			case AnimationID.MINING_MOTHERLODE_CRYSTAL:
			case AnimationID.MINING_MOTHERLODE_DRAGON:
			case AnimationID.MINING_MOTHERLODE_DRAGON_OR:
			case AnimationID.MINING_MOTHERLODE_DRAGON_OR_TRAILBLAZER:
			case AnimationID.MINING_MOTHERLODE_DRAGON_UPGRADED:
			case AnimationID.MINING_MOTHERLODE_GILDED:
			case AnimationID.MINING_MOTHERLODE_INFERNAL:
			case AnimationID.MINING_MOTHERLODE_IRON:
			case AnimationID.MINING_MOTHERLODE_MITHRIL:
			case AnimationID.MINING_MOTHERLODE_RUNE:
			case AnimationID.MINING_MOTHERLODE_STEEL:
			case AnimationID.MINING_MOTHERLODE_TRAILBLAZER:
				return true;
			default:
				return false;
		}
	}

	@Nullable
	private VeinState findPlayerMiningTarget(Player player) {
		WorldPoint actorLocation = player.getWorldLocation();
		Direction dir = new Angle(player.getOrientation()).getNearestDirection();
		WorldPoint facingPoint = getNeighborPoint(actorLocation, dir);
		return getVeinAtLocation(facingPoint);
	}

	private WorldPoint getNeighborPoint(WorldPoint point, Direction direction) {
		switch (direction) {
			case NORTH:
				return point.dy(1);
			case SOUTH:
				return point.dy(-1);
			case EAST:
				return point.dx(1);
			case WEST:
				return point.dx(-1);
			default:
				throw new IllegalStateException();
		}
	}

	private VeinState getVeinAtLocation(WorldPoint point) {
		for(VeinState veinState : availableVeins) {
			if(veinState.getVein().getWorldLocation().equals(point)) {
				return veinState;
			}
		}

		return null;
	}

	VeinStatus getVeinStatus(WallObject wallObject) {
		if(DEPLETED_SPOTS.contains(wallObject.getId())) {
			return VeinStatus.DEPLETED;
		} else if(MINE_SPOTS.contains(wallObject.getId())) {
			return VeinStatus.AVAILABLE;
		}

		return null;
	}

}
