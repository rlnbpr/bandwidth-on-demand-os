/**
 * The owner of the original code is SURFnet BV.
 *
 * Portions created by the original owner are Copyright (C) 2011-2012 the
 * original owner. All Rights Reserved.
 *
 * Portions created by other contributors are Copyright (C) the contributor.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   (Contributors insert name & email here)
 *
 * This file is part of the SURFnet7 Bandwidth on Demand software.
 *
 * The SURFnet7 Bandwidth on Demand software is free software: you can
 * redistribute it and/or modify it under the terms of the BSD license
 * included with this distribution.
 *
 * If the BSD license cannot be found with this distribution, it is available
 * at the following location <http://www.opensource.org/licenses/BSD-3-Clause>
 */
package nl.surfnet.bod.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.nbi.NbiClient;
import nl.surfnet.bod.nbi.generated.TerminationPoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Ciena NorthBoundInterface implementation of the {@link PhysicalPortService}
 *
 * @author Frank Mölder ($Author$)
 * @version $Revision$ $Date$
 */
@Service
class NbiPortServiceImpl implements NbiPortService {

  @Autowired
  private NbiClient nbiClient;

  @Override
  public List<PhysicalPort> findAll() {
    return transform(nbiClient.findAllPorts());
  }

  @Override
  public long count() {
    long size = 0;

    List<TerminationPoint> ports = nbiClient.findAllPorts();
    if (!CollectionUtils.isEmpty(ports)) {
      size = ports.size();
    }

    return size;
  }

  @Override
  public PhysicalPort findByName(final String name) {
    return selectByName(nbiClient.findAllPorts(), name);
  }

  /**
   * Transforms a List of {@link TerminationPoint} to a List of
   * {@link PhysicalPort}
   *
   * @param nbiPorts
   *          List of {@link TerminationPoint}
   * @return List of {@link PhysicalPort} which were transformed from the given
   *         {@link TerminationPoint}s. Empty list when param is null;
   */
  List<PhysicalPort> transform(final Collection<TerminationPoint> terminationPoints) {
    Collection<PhysicalPort> ports = null;
    List<PhysicalPort> portList = new ArrayList<PhysicalPort>();

    if (!CollectionUtils.isEmpty(terminationPoints)) {
      ports = Collections2.transform(terminationPoints, new Function<TerminationPoint, PhysicalPort>() {
        @Override
        public PhysicalPort apply(final TerminationPoint terminationPoint) {
          return mapTerminationPointToPhysicalPort(terminationPoint);
        }

      });
    }

    if (!CollectionUtils.isEmpty(ports)) {
      portList = new ArrayList<PhysicalPort>(ports);
    }

    return portList;
  }

  /**
   * Selects a port with the specified portId from the given Collection.
   *
   * @param terminationPoints
   *          Collection to search
   * @param name
   *          PortId to select on
   * @return Matched instance or null when no match or multiple matches were
   *         found.
   */
  PhysicalPort selectByName(final Collection<TerminationPoint> terminationPoints, final String name) {
    PhysicalPort result = null;
    Collection<TerminationPoint> filteredPorts = null;

    if (!CollectionUtils.isEmpty(terminationPoints) && (StringUtils.hasText(name))) {
      filteredPorts = Lists.newArrayList(Collections2.filter(terminationPoints, new Predicate<TerminationPoint>() {

        @Override
        public boolean apply(final TerminationPoint port) {
          return (port.getPortDetail() != null && name.equals(port.getPortDetail().getName()));
        };
      }));

      if (filteredPorts.size() > 1) {
        throw new IllegalStateException("Multiple ports found for name: " + name);
      }

      if (!filteredPorts.isEmpty()) {
        result = mapTerminationPointToPhysicalPort(filteredPorts.iterator().next());
      }
    }

    return result;
  }

  /**
   * Transforms a {@link TerminationPoint} into a {@link PhysicalPort}
   *
   * @param terminationPoint
   *          Object to transform
   * @return {@link PhysicalPort} transformed object
   */
  PhysicalPort mapTerminationPointToPhysicalPort(final TerminationPoint terminationPoint) {
    PhysicalPort physicalPort = new PhysicalPort();

    if (terminationPoint != null) {
      if (terminationPoint.getPortBasic() != null) {

      }

      if (terminationPoint.getPortDetail() != null) {
        physicalPort.setName(terminationPoint.getPortDetail().getName());
        physicalPort.setDisplayName(terminationPoint.getPortDetail().getDisplayName());
      }
    }

    return physicalPort;
  }

}
