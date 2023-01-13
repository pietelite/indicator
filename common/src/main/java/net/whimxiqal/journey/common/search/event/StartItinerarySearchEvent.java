/*
 * MIT License
 *
 * Copyright (c) Pieter Svenson
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

package net.whimxiqal.journey.common.search.event;

import net.whimxiqal.journey.common.search.ItineraryTrial;
import net.whimxiqal.journey.common.search.SearchSession;

/**
 * An event dispatched when a new itinerary is being searched for
 * in an {@link ItineraryTrial}.
 *
 * @see SearchSession
 * @see ItineraryTrial#attempt(boolean) 
 * @see SearchDispatcher
 */
public class StartItinerarySearchEvent extends SearchEvent {

  private final ItineraryTrial itineraryTrial;

  /**
   * General constructor.
   *
   * @param session        the session
   * @param itineraryTrial the itinerary trial which is being used to search
   */
  public StartItinerarySearchEvent(SearchSession session, ItineraryTrial itineraryTrial) {
    super(session);
    this.itineraryTrial = itineraryTrial;
  }

  /**
   * Get the itinerary trial which is being used to search for
   * a valid itinerary.
   *
   * @return the itinerary trial
   */
  public ItineraryTrial getItineraryTrial() {
    return itineraryTrial;
  }

  @Override
  EventType type() {
    return EventType.START_ITINERARY;
  }
}