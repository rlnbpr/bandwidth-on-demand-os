/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.nbi.mtosi;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MtosiUtilsTest {

  @Test
  public void convertPtp() {
    assertThat(
        MtosiUtils.physicalTerminationPointToNmsPortId("/rack=1/shelf=1/slot=1/port=48"),
        is("1-1-1-48"));
  }

  @Test
  public void convertPtpWithSubSlot() {
    assertThat(
        MtosiUtils.physicalTerminationPointToNmsPortId("/rack=1/shelf=1/slot=3/sub_slot=1/port=5"),
        is("1-1-3-1-5"));
  }

  @Test
  public void convertNmsPortId() {
    assertThat(
      MtosiUtils.nmsPortIdToPhysicalTerminationPoint("1-2-3-4"),
      is("/rack=1/shelf=2/slot=3/port=4"));
  }

  @Test
  public void convertNmsPortIdWithSubSlot() {
    assertThat(
      MtosiUtils.nmsPortIdToPhysicalTerminationPoint("2-3-4-5-10"),
      is("/rack=2/shelf=3/slot=4/sub_slot=5/port=10"));
  }
}