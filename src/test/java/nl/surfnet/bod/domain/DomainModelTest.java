/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

import nl.surfnet.bod.support.PhysicalPortFactory;
import nl.surfnet.bod.support.PhysicalResourceGroupFactory;
import nl.surfnet.bod.support.ReservationFactory;
import nl.surfnet.bod.support.VirtualPortFactory;
import nl.surfnet.bod.support.VirtualPortRequestLinkFactory;
import nl.surfnet.bod.support.VirtualResourceGroupFactory;

public class DomainModelTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ReservationFactory reservationFactory = new ReservationFactory();
  private final PhysicalResourceGroupFactory prgFactory = new PhysicalResourceGroupFactory();
  private final VirtualResourceGroupFactory vrgFactory = new VirtualResourceGroupFactory();
  private final VirtualPortFactory vpFactory = new VirtualPortFactory();
  private final PhysicalPortFactory ppFactory = new PhysicalPortFactory();
  private final VirtualPortRequestLinkFactory vprlFactory = new VirtualPortRequestLinkFactory();

  private Reservation reservation;
  private Reservation reservationTwo;
  private VirtualPortRequestLink link;
  private VirtualResourceGroup vrg;

  @Before
  public void onSetup() {
    PhysicalPort pp1 = ppFactory.create();
    PhysicalPort pp2 = ppFactory.create();
    PhysicalResourceGroup prg = prgFactory.addPhysicalPort(pp1, pp2).create();
    pp1.setPhysicalResourceGroup(prg);
    pp2.setPhysicalResourceGroup(prg);

    vrg = vrgFactory.create();
    VirtualPort vp1 = vpFactory.setVirtualResourceGroup(vrg).create();
    VirtualPort vp2 = vpFactory.setVirtualResourceGroup(vrg).create();
    vrg.setVirtualPorts(Lists.newArrayList(vp1, vp2));

    reservationFactory.setSourcePort(vp1);
    reservationFactory.setDestinationPort(vp2);
    reservation = reservationFactory.create();
    
    reservationTwo = reservationFactory.create();
    
    ReflectionTestUtils.setField(reservationTwo, "creationDateTime", reservation.getCreationDateTime());

    reservation.setVirtualResourceGroup(vrg);
    reservationTwo.setVirtualResourceGroup(vrg);
    vrg.setReservations(Lists.newArrayList(reservation, reservationTwo));

    link = vprlFactory.setVirtualResourceGroup(vrg).create();
    vrg.setVirtualPortRequestLinks(Lists.newArrayList(link));
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific toString methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldNotOverflowInReservationToString() {
    logger.info(reservation.toString());
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific toString methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldNotOverflowInVirtualPortRequestLinkToString() {
    logger.info(link.toString());
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific equal methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldNotOverflowInReservationEquals() {
    assertTrue(reservation.equals(reservationTwo));
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific equal methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldNotOverflowInReservationHashCode() {
    assertThat(reservation.hashCode(), is(reservationTwo.hashCode()));
    // for (int i = 0; i < 200; i++) {
    // this.onSetup();
    // // assertThat(reservation.hashCode(), is(reservationTwo.hashCode()));
    // assertEquals(
    // "Run: " + i + ", reservation# " + reservation.hashCode() +
    // ", reservationTwo#: " + reservationTwo.hashCode(),
    // reservationTwo.hashCode(), reservation.hashCode());
    // }
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific equal methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldOnlyConsiderIdInVirtualPortRequestLinkEquals() {
    VirtualPortRequestLink requestLink = new VirtualPortRequestLink();
    requestLink.setId(link.getId());
    assertTrue("Only on Id", link.equals(requestLink));
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific equal methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldOnlyConsiderIdInVirtualPortRequestLinkHashCode() {
    VirtualPortRequestLink requestLink = new VirtualPortRequestLink();
    requestLink.setId(link.getId());
    assertThat("Only on Id", link.hashCode(), is(requestLink.hashCode()));
  }

  /**
   * Bidirectional relations between the domains are treaded different in the
   * specific equal methods, to prevent stackoverFlow.
   */
  @Test
  public void shouldOnlyConsiderIdInVirtualResourceGroupEquals() {
    VirtualResourceGroup vrgTwo = new VirtualResourceGroup();
    vrgTwo.setId(vrg.getId());
    assertTrue("Only on Id", vrg.equals(vrgTwo));
  }

  @Test
  public void shouldOnlyconsiderIdInVirturalResourceGroupHashCode() {
    VirtualResourceGroup vrgTwo = new VirtualResourceGroup();
    vrgTwo.setId(vrg.getId());
    assertTrue("Only on Id", vrg.hashCode() == vrgTwo.hashCode());
  }
}