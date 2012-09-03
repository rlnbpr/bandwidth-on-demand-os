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
package nl.surfnet.bod.web.manager;

import java.util.Collection;

import javax.annotation.Resource;

import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.domain.VirtualPortRequestLink;
import nl.surfnet.bod.service.PhysicalPortService;
import nl.surfnet.bod.service.PhysicalResourceGroupService;
import nl.surfnet.bod.service.ReservationService;
import nl.surfnet.bod.service.VirtualPortService;
import nl.surfnet.bod.support.ReservationFilterViewFactory;
import nl.surfnet.bod.util.Orderings;
import nl.surfnet.bod.web.WebUtils;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;
import nl.surfnet.bod.web.view.ManagerStatisticsView;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Optional;

@Controller("managerDashboardController")
@RequestMapping("/manager")
public class DashboardController {

  @Resource
  private PhysicalResourceGroupService physicalResourceGroupService;

  @Resource
  private VirtualPortService virtualPortService;

  @Resource
  private PhysicalPortService physicalPortService;

  @Resource
  private ReservationService reservationService;

  @RequestMapping(method = RequestMethod.GET)
  public String index(Model model) {
    Optional<Long> groupId = WebUtils.getSelectedPhysicalResourceGroupId();

    if (!groupId.isPresent()) {
      return "redirect:/";
    }

    PhysicalResourceGroup group = physicalResourceGroupService.find(groupId.get());
    Collection<VirtualPortRequestLink> requests = virtualPortService.findPendingRequests(group);

    model.addAttribute("prg", group);
    model.addAttribute("requests", Orderings.vpRequestLinkOrdering().sortedCopy(requests));

    model.addAttribute("stats", determineStatistics(Security.getUserDetails()));
    model.addAttribute("defaultDuration", ReservationFilterViewFactory.DEFAULT_FILTER_INTERVAL_STRING);

    return "manager/index";
  }

   ManagerStatisticsView determineStatistics(RichUserDetails manager) {
    ReservationFilterViewFactory reservationFilterViewFactory = new ReservationFilterViewFactory();
    PhysicalResourceGroup managerPrg = physicalResourceGroupService.find(manager.getSelectedRole()
        .getPhysicalResourceGroupId().get());

    long countPhysicalPorts = physicalPortService.countAllocatedForPhysicalResourceGroup(managerPrg);

    long countVirtualPorts = virtualPortService.countForManager(manager.getSelectedRole());

    long countElapsedReservations = reservationService.countForFilterAndManager(manager,
        reservationFilterViewFactory.create(ReservationFilterViewFactory.ELAPSED));

    long countActiveReservations = reservationService.countForFilterAndManager(manager,
        reservationFilterViewFactory.create(ReservationFilterViewFactory.ACTIVE));

    long countComingReservations = reservationService.countForFilterAndManager(manager,
        reservationFilterViewFactory.create(ReservationFilterViewFactory.COMING));

    return new ManagerStatisticsView(countPhysicalPorts, countVirtualPorts, countElapsedReservations,
        countActiveReservations, countComingReservations);
  }
}
