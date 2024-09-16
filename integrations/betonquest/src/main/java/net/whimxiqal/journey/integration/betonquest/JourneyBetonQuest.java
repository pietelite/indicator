/*
 * MIT License
 *
 * Copyright (c) whimxiqal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
