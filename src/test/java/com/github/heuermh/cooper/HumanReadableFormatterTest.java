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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test for HumanReadableFormatter.
 *
 * @author  Michael Heuer
 */
public final class HumanReadableFormatterTest {

    @Test(expected=IllegalArgumentException.class)
    public void testFormatLessThanZero() {
        new HumanReadableFormatter().format(-1L);
    }

    @Test
    public void testFormat() {
        HumanReadableFormatter formatter = new HumanReadableFormatter();
        assertEquals("0 Bytes", formatter.format(0L));
        assertEquals("1 Byte", formatter.format(1L));
        assertEquals("113 Bytes", formatter.format(113L));
        assertEquals("2.1 KiB", formatter.format(2122L));
        assertEquals("377.3 MiB", formatter.format(395610342L));
        assertEquals("1.7 GiB", formatter.format(1826577054L));
    }
}
