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

package net.whimxiqal.journey.sponge.util;

import net.whimxiqal.journey.sponge.JourneySponge;
import net.whimxiqal.journey.util.CommonLogger;
import org.apache.logging.log4j.Logger;

/**
 * An implementation the simple common Journey logger.
 */
public class SpongeLogger extends CommonLogger {
  @Override
  protected void submit(CommonLogger.Message message) {
    Logger logger = JourneySponge.get().logger();
    switch (message.type()) {
      case WARNING:
        logger.warn(message.message());
        break;
      case SEVERE:
        logger.error(message.message());
        break;
      case INFO:
      default:
        logger.info(message.message());
    }
  }
}
