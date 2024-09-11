package net.whimxiqal.journey.integration.betonquest;

import java.util.logging.Logger;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.JourneyApiProvider;
import org.betonquest.betonquest.BetonQuest;
import org.bukkit.plugin.java.JavaPlugin;

public final class JourneyBetonQuest extends JavaPlugin {

  public static Logger logger;

  @Override
  public void onEnable() {
    JourneyBetonQuest.logger = this.getLogger();
    JourneyApi api = JourneyApiProvider.get();
    api.registerScope(getName(), "betonquest", new BetonQuestScope());
    BetonQuest.getInstance().getQuestRegistries().getEventTypes().register("journey", JourneyQuestEvent::new);
    BetonQuest.getInstance().getQuestRegistries().getEventTypes().register("stopjourney", JourneyQuestStopEvent::new);
  }

}
