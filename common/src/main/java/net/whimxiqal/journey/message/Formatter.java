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

package net.whimxiqal.journey.message;

import java.net.MalformedURLException;
import java.net.URL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellBox;
import net.whimxiqal.journey.Journey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class to store static fields and methods pertaining to
 * formatted messages with the purpose of sending meaningfully colored
 * and enhanced messages to players and other message receivers.
 */
public final class Formatter {

  public static final TextColor ACCENT = TextColor.color(20, 166, 219);  // #14A6DB
  public static final TextColor DARK = TextColor.color(64, 64, 64);
  public static final TextColor DULL = TextColor.color(179, 179, 179);
  public static final TextColor ERROR = TextColor.color(194, 56, 60);
  public static final TextColor GOLD = TextColor.color(222, 185, 0);
  public static final TextColor INFO = TextColor.color(255, 191, 245);
  //  public static final TextColor DEBUG = TextColor.color(128, 255, 233);
  public static final TextColor SUCCESS = TextColor.color(11, 181, 38);
  public static final TextColor THEME = TextColor.color(172, 21, 219);  // #AC15DB
  public static final TextColor URL = TextColor.color(66, 105, 224);
  public static final TextColor WARN = TextColor.color(255, 157, 10);

  public static Component welcome() {
    return Component.text()
        .append(Component.newline())
        .append(Component.text("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%").color(DULL))
        .append(Component.newline())
        .append(Component.text("   ✳ ").color(THEME))
        .append(Component.text("Journey").color(THEME).decorate(TextDecoration.UNDERLINED))
        .append(Component.text(" (" + Journey.get().proxy().version() + ")").color(DARK))
        .append(Component.text(" ✳").color(THEME))
        .append(Component.newline())
        .append(Component.text("      by ").color(DULL))
        .append(Component.text("whimxiqal").color(ACCENT))
        .append(Component.newline())
        .append(Component.newline())
        .append(Component.text("   Wiki >      ").color(INFO))
        .append(url("journey.whimxiqal.net", "https://journey.whimxiqal.net").decorate(TextDecoration.ITALIC))
        .append(Component.newline())
        .append(Component.text("   Source > ").color(INFO))
        .append(url("github.com/whimxiqal/journey", "https://github.com/whimxiqal/journey").decorate(TextDecoration.ITALIC))
        .append(Component.newline())
        .append(Component.text("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%").color(DULL))
        .append(Component.newline())
        .build();
  }

  /**
   * Return the plugin's standard prefix text, to prepend to some general text
   * sent on behalf of the plugin.
   *
   * @return the text component
   */
  public static Component prefix() {
    return Component.text()
        .append(Component.text("[").color(DARK))
        .append(Component.text("✳").color(THEME))
        .append(Component.text("] ").color(DARK))
        .build();
  }

  public static Component accent(String message) {
    return Component.text(message).color(ACCENT);
  }

  public static Component hover(Component label, Component onHover) {
    return label.hoverEvent(HoverEvent.showText(onHover));
  }

  /**
   * Format some text to link to a given url.
   *
   * @param label the text to show in the chat
   * @param url   the url to link to on click
   * @return the component text
   */
  public static Component url(@NotNull String label, @NotNull String url) {
    TextComponent.Builder textBuilder = Component.text();
    textBuilder.append(Component.text(label).color(URL));
    textBuilder.hoverEvent(HoverEvent.showText(Component.text(url)));
    try {
      textBuilder.clickEvent(ClickEvent.openUrl(new URL(url)));
    } catch (MalformedURLException ex) {
      textBuilder.clickEvent(ClickEvent.suggestCommand(url));
      Journey.logger().error("A url was not formed correctly for a"
          + " click action: " + url);
    }

    return textBuilder.build();
  }

  public static Component cell(@Nullable Cell cell) {
    if (cell == null) {
      return Component.text("null").color(DULL);
    }
    return Component.text()
        .append(Component.text("[x: ").color(DULL))
        .append(Component.text(cell.blockX()).color(ACCENT))
        .append(Component.text(", y: ").color(DULL))
        .append(Component.text(cell.blockY()).color(ACCENT))
        .append(Component.text(", z: ").color(DULL))
        .append(Component.text(cell.blockZ()).color(ACCENT))
        .append(Component.text("] (").color(DULL))
        .append(Component.text(Journey.get().proxy().platform().domainName(cell.domain())).color(ACCENT))
        .append(Component.text(")").color(DULL))
        .build();
  }

  public static Component box(@NotNull CellBox box) {
    return cell(box.min()).append(Component.text(" - ")).append(cell(box.max()));
  }

}
