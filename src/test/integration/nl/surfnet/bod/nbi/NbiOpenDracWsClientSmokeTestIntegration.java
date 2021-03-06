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
package nl.surfnet.bod.nbi;

import static nl.surfnet.bod.util.TestHelper.accProperties;
import static nl.surfnet.bod.util.TestHelper.productionProperties;
import static nl.surfnet.bod.util.TestHelper.testProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.util.TestHelper.PropertiesEnvironment;

import org.junit.Test;

public class NbiOpenDracWsClientSmokeTestIntegration {

  private NbiOpenDracWsClient subject;

  @Test
  public void smokeTestAcceptance() {
    initAcceptance();

    List<PhysicalPort> allPorts = subject.findAllPhysicalPorts();

    assertThat(allPorts, hasSize(greaterThan(0)));
  }

  @Test
  public void smokeTestProducation() {
    initProduction();

    List<PhysicalPort> allPorts = subject.findAllPhysicalPorts();

    assertThat(allPorts, hasSize(greaterThan(0)));
  }

  @Test
  public void smokeTestTest() {
    initTest();

    List<PhysicalPort> allPorts = subject.findAllPhysicalPorts();

    assertThat(allPorts, hasSize(greaterThan(0)));
  }

  private void initAcceptance() {
    init(accProperties());
  }

  private void initProduction() {
    init(productionProperties());
  }

  private void initTest() {
    init(testProperties());
  }

  private void init(PropertiesEnvironment env) {
    subject = new NbiOpenDracWsClient();
    subject.setInventoryServiceUrl(env.getProperty("nbi.drac.service.inventory"));
    subject.setPassword(env.getDecryptedProperty("nbi.drac.password"));
    subject.setUsername(env.getProperty("nbi.drac.user"));
  }

}