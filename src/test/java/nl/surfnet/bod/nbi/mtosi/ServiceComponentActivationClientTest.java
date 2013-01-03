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

import static nl.surfnet.bod.util.TestHelper.productionProperties;

import javax.xml.bind.Marshaller;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.support.ReservationFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class ServiceComponentActivationClientTest {

  static {
    System.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, "true");
//    System.setProperty("com.sun.xml.ws.fault.SOAPFaultBuilder.disableCaptureStackTrace", "false");
//    System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//    System.setProperty("com.sun.xml.ws.util.pipe.StandaloneTubeAssembler.dump", "true");
//    System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
  }

  private ServiceComponentActivationClient subject;

  @Before
  public void setup() {
    String endPoint = productionProperties().getProperty("bi.mtosi.service.reserve.endpoint");
    subject = new ServiceComponentActivationClient(endPoint);
  }

  @Test
  @Ignore("Needs access to london server... is more like integration, but now only for testing..")
  public void reserve() {
    Reservation reservation = new ReservationFactory().create();
    subject.reserve(reservation, false);

  }
}
