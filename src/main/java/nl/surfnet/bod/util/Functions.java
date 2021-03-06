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
package nl.surfnet.bod.util;

import java.util.Collection;
import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.UserGroup;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.service.ReservationService;
import nl.surfnet.bod.service.VirtualPortService;
import nl.surfnet.bod.web.view.ElementActionView;
import nl.surfnet.bod.web.view.PhysicalPortView;
import nl.surfnet.bod.web.view.UserGroupView;
import nl.surfnet.bod.web.view.VirtualPortView;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public final class Functions {

  public static final Function<PhysicalPort, String> TO_NMS_PORT_ID_FUNC = new Function<PhysicalPort, String>() {
    @Override
    public String apply(PhysicalPort physicalPort) {
      return physicalPort.getNmsPortId();
    }
  };

  public static final Predicate<PhysicalPort> MISSING_PORTS_PRED = new Predicate<PhysicalPort>() {
    @Override
    public boolean apply(PhysicalPort physicalPort) {
      return !physicalPort.isAlignedWithNMS();
    }
  };

  public static final Predicate<PhysicalPort> NON_MISSING_PORTS_PRED = new Predicate<PhysicalPort>() {
    @Override
    public boolean apply(PhysicalPort physicalPort) {
      return !MISSING_PORTS_PRED.apply(physicalPort);
    }
  };

  public static final Function<UserGroup, UserGroupView> FROM_USER_GROUP_TO_USER_GROUP_VIEW =
      new Function<UserGroup, UserGroupView>() {
        @Override
        public UserGroupView apply(UserGroup group) {
          return new UserGroupView(group);
        }
      };

  public static final Function<VirtualResourceGroup, UserGroupView> FROM_VRG_TO_USER_GROUP_VIEW =
      new Function<VirtualResourceGroup, UserGroupView>() {
        @Override
        public UserGroupView apply(VirtualResourceGroup group) {
          return new UserGroupView(group);
        }
      };

  public static final Function<VirtualPort, VirtualPortView> FROM_VIRTUALPORT_TO_VIRTUALPORT_VIEW =
      new Function<VirtualPort, VirtualPortView>() {
        @Override
        public VirtualPortView apply(VirtualPort port) {
          return new VirtualPortView(port);
        }
      };

  private Functions() {
  }

  /**
   * Calculates the amount of related {@link VirtualPort}s and transforms it to
   * a {@link PhysicalPortView}
   *
   * @param port
   *          {@link PhysicalPort} to enrich
   * @param virtualPortService
   *          {@link VirtualPortService} to retrieve the amount of related
   *          {@link VirtualPort}s
   * @return PhysicalPortView Transformed {@link PhysicalPort}
   */
  public static PhysicalPortView transformAllocatedPhysicalPort(PhysicalPort port,
      final VirtualPortService virtualPortService, final ReservationService reservationService) {

    long vpCount = virtualPortService.countForPhysicalPort(port);
    ElementActionView allocateActionView;
    if (vpCount == 0) {
      allocateActionView = new ElementActionView(true, "label_unallocate");
    }
    else {
      allocateActionView = new ElementActionView(false, "label_virtual_ports_related");
    }

    final PhysicalPortView physicalPortView = new PhysicalPortView(port, allocateActionView, vpCount);
    physicalPortView.setReservationsAmount(reservationService.findActiveByPhysicalPort(port).size());
    return physicalPortView;
  }

  /**
   * Transforms a Collection
   *
   * @see #transformAllocatedPhysicalPort(PhysicalPort, VirtualPortService)
   *
   */
  public static List<PhysicalPortView> transformAllocatedPhysicalPorts(List<PhysicalPort> ports,
      final VirtualPortService virtualPortService, final ReservationService reservationService) {

    List<PhysicalPortView> transformers = Lists.newArrayList();
    for (PhysicalPort port : ports) {
      transformers.add(transformAllocatedPhysicalPort(port, virtualPortService, reservationService));
    }

    return transformers;
  }

  public static PhysicalPortView transformUnallocatedPhysicalPort(PhysicalPort unallocatedPort) {
    return new PhysicalPortView(unallocatedPort, null);
  }

  public static List<PhysicalPortView> transformUnallocatedPhysicalPorts(Collection<PhysicalPort> unallocatedPorts) {
    List<PhysicalPortView> transformers = Lists.newArrayList();
    for (PhysicalPort port : unallocatedPorts) {
      transformers.add(transformUnallocatedPhysicalPort(port));
    }

    return transformers;
  }

}
