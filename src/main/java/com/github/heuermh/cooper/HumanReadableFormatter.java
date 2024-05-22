/*
 * The authors of this file license it to you under the
 * Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.heuermh.cooper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Human readable size formatter.
 *
 * @author  Michael Heuer
 */
final class HumanReadableFormatter {

    /** Cached decimal format. */
    private final DecimalFormat decimalFormat;

    /** Multi-byte units, in binary.  See https://en.wikipedia.org/wiki/Byte#Multiple-byte_units.  */
    static final String[] UNITS = new String[] { "B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB" };


    /**
     * Create a new human readable size formatter.
     */
    HumanReadableFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');

        decimalFormat = new DecimalFormat("0.##", symbols);
        decimalFormat.setGroupingUsed(false);
    }


    /**
     * Format the specified size in bytes to human readable
     * multi-byte units, in binary.
     *
     * @param size, size in bytes
     * @return the specified size in bytes formatted to human readable
     *    multi-byte units, in binary
     */
    String format(final long size) {
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return decimalFormat.format(size / Math.pow(1024, digitGroups)) + " " + UNITS[digitGroups];
    }
}
