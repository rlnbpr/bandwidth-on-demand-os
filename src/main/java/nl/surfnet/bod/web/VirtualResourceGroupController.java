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
package nl.surfnet.bod.web;

import java.util.Collection;
import java.util.Collections;

import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.service.VirtualResourceGroupService;
import nl.surfnet.bod.web.security.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/virtualresourcegroups")
public class VirtualResourceGroupController {

  @Autowired
  private VirtualResourceGroupService virtualResourceGroupService;

  @RequestMapping(value = "/{id}/ports", method = RequestMethod.GET)
  public @ResponseBody Collection<VirtualPort> listForVirtualResourceGroup(@PathVariable Long id) {
    VirtualResourceGroup group = virtualResourceGroupService.find(id);

    if (group == null || Security.isUserNotMemberOf(group.getSurfConextGroupName())) {
      return Collections.emptyList();
    }

    return group.getVirtualPorts();
  }
}
