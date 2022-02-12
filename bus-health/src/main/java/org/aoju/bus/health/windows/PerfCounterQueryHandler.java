/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2022 aoju.org OSHI and other contributors.                 *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.health.windows;

import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import org.aoju.bus.core.annotation.NotThreadSafe;
import org.aoju.bus.health.Formats;
import org.aoju.bus.health.windows.PerfDataKit.PerfCounter;
import org.aoju.bus.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to handle Performance Counter Queries
 * This class is not thread safe. Each query handler instance should only be
 * used in a single thread, preferably in a try-with-resources block.
 *
 * @author Kimi Liu
 * @version 6.3.3
 * @since JDK 1.8+
 */
@NotThreadSafe
public final class PerfCounterQueryHandler implements AutoCloseable {

    // Map of counter handles
    private final Map<PerfCounter, HANDLEByReference> counterHandleMap = new HashMap<>();
    // The query handle
    private HANDLEByReference queryHandle = null;

    /**
     * Begin monitoring a Performance Data counter.
     *
     * @param counter A PerfCounter object.
     * @return True if the counter was successfully added to the query.
     */
    public boolean addCounterToQuery(PerfCounter counter) {
        // Open a new query or get the handle to an existing one
        if (this.queryHandle == null) {
            this.queryHandle = new HANDLEByReference();
            if (!PerfDataKit.openQuery(this.queryHandle)) {
                Logger.warn("Failed to open a query for PDH counter: {}", counter.getCounterPath());
                this.queryHandle = null;
                return false;
            }
        }
        // Get a new handle for the counter
        HANDLEByReference p = new HANDLEByReference();
        if (!PerfDataKit.addCounter(this.queryHandle, counter.getCounterPath(), p)) {
            Logger.warn("Failed to add counter for PDH counter: {}", counter.getCounterPath());
            return false;
        }
        counterHandleMap.put(counter, p);
        return true;
    }

    /**
     * Stop monitoring a Performance Data counter.
     *
     * @param counter A PerfCounter object
     * @return True if the counter was successfully removed.
     */
    public boolean removeCounterFromQuery(PerfCounter counter) {
        boolean success = false;
        HANDLEByReference href = counterHandleMap.remove(counter);
        // null if handle wasn't present
        if (href != null) {
            success = PerfDataKit.removeCounter(href);
        }
        if (counterHandleMap.isEmpty()) {
            PerfDataKit.closeQuery(queryHandle);
            queryHandle = null;
        }
        return success;
    }

    /**
     * Stop monitoring all Performance Data counters and release their resources
     */
    public void removeAllCounters() {
        // Remove all counters from counterHandle map
        for (HANDLEByReference href : counterHandleMap.values()) {
            PerfDataKit.removeCounter(href);
        }
        counterHandleMap.clear();
        // Remove query
        if (this.queryHandle != null) {
            PerfDataKit.closeQuery(this.queryHandle);
        }
        this.queryHandle = null;
    }

    /**
     * Update all counters on this query.
     *
     * @return The timestamp for the update of all the counters, in milliseconds
     * since the epoch, or 0 if the update failed
     */
    public long updateQuery() {
        if (queryHandle == null) {
            Logger.warn("Query does not exist to update.");
            return 0L;
        }
        return PerfDataKit.updateQueryTimestamp(queryHandle);
    }

    /**
     * Query the raw counter value of a Performance Data counter. Further
     * mathematical manipulation/conversion is left to the caller.
     *
     * @param counter The counter to query
     * @return The raw value of the counter
     */
    public long queryCounter(PerfCounter counter) {
        if (!counterHandleMap.containsKey(counter)) {
            if (Logger.get().isWarn()) {
                Logger.warn("Counter {} does not exist to query.", counter.getCounterPath());
            }
            return 0;
        }
        long value = counter.isBaseCounter() ? PerfDataKit.querySecondCounter(counterHandleMap.get(counter))
                : PerfDataKit.queryCounter(counterHandleMap.get(counter));
        if (value < 0) {
            if (Logger.get().isWarn()) {
                Logger.warn("Error querying counter {}: {}", counter.getCounterPath(),
                        String.format(Formats.formatError((int) value)));
            }
            return 0L;
        }
        return value;
    }

    @Override
    public void close() {
        removeAllCounters();
    }

}
