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

package net.whimxiqal.journey.integration.essentials;

import com.earth2me.essentials.IEssentials;
import java.util.logging.Logger;
import net.whimxiqal.journey.JourneyApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class JourneyEssentials extends JavaPlugin {

  private static Logger logger;

  public static Logger logger() {
    return logger;
  }

  @Override
  public void onEnable() {
    JourneyEssentials.logger = getLogger();
    JourneyApi.get().registerScope(getName(), "essentials", new EssentialsScope());
    JourneyApi.get().registerTunnels(getName(), new EssentialsTunnelSupplier());
  }

  public static IEssentials essentials() {
    Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
    if (!(plugin instanceof IEssentials)) {
      throw new RuntimeException("Essentials class could not be found");
    }
    return (IEssentials) plugin;
  }

}
