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

import nl.surfnet.bod.web.security.Security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shibboleth")
public class ShibbolethController {

  @RequestMapping("/groups")
  public String list(final Model uiModel) {
    uiModel.addAttribute("groups", Security.getUserDetails().getUserGroups());

    return "shibboleth/groups";
  }

  @RequestMapping("/refresh")
  public String refreshGroups() {
    SecurityContextHolder.clearContext();

    return "redirect:groups";
  }

  @RequestMapping("/info")
  public String info() {
    return "shibboleth/info";
  }
}
