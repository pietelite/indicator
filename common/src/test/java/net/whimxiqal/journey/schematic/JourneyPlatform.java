package net.whimxiqal.journey.schematic;

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.enginehub.piston.CommandManager;
import org.jetbrains.annotations.Nullable;

public class JourneyPlatform extends AbstractPlatform {

  @Override
  public Registries getRegistries() {
    return null;
  }

  @Override
  public int getDataVersion() {
    return 0;
  }

  @Override
  public boolean isValidMobType(String type) {
    return false;
  }

  @Nullable
  @Override
  public Player matchPlayer(Player player) {
    return null;
  }

  @Nullable
  @Override
  public World matchWorld(World world) {
    return null;
  }

  @Override
  public void registerCommands(CommandManager commandManager) {

  }

  @Override
  public void setGameHooksEnabled(boolean enabled) {

  }

  @Override
  public LocalConfiguration getConfiguration() {
    return new LocalConfiguration() {
      @Override
      public void load() {
        // do nothing
      }
    };
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public String getPlatformName() {
    return "Journey Platform";
  }

  @Override
  public String getPlatformVersion() {
    return "0";
  }

  @Override
  public Map<Capability, Preference> getCapabilities() {
    Map<Capability, Preference> out = new HashMap<>();
    out.put(Capability.CONFIGURATION, Preference.PREFERRED);
    out.put(Capability.WORLD_EDITING, Preference.PREFERRED);
    return out;
  }

  @Override
  public Set<SideEffect> getSupportedSideEffects() {
    return null;
  }
}
