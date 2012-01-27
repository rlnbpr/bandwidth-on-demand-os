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
package nl.surfnet.bod.web.noc;

import static nl.surfnet.bod.web.WebUtils.*;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.service.PhysicalPortService;
import nl.surfnet.bod.service.PhysicalResourceGroupService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/noc/" + PhysicalPortController.PAGE_URL)
public class PhysicalPortController {

  static final String PAGE_URL = "physicalports";
  static final String MODEL_KEY = "physicalPort";
  static final String MODEL_KEY_LIST = MODEL_KEY + LIST_POSTFIX;

  @Autowired
  private PhysicalPortService physicalPortService;

  @Autowired
  private PhysicalResourceGroupService physicalResourceGroupService;

  @RequestMapping(method = RequestMethod.POST)
  public String update(@Valid PhysicalPort physicalPort, final BindingResult bindingResult, final Model uiModel,
      final HttpServletRequest httpServletRequest) {

    if (bindingResult.hasErrors()) {
      uiModel.addAttribute(MODEL_KEY, physicalPort);
      return PAGE_URL + UPDATE;
    }

    PhysicalResourceGroup newPhysicalResourceGroup = physicalPort.getPhysicalResourceGroup();

    // Ignore changes made by user, fetch again and set group
    physicalPort = physicalPortService.findByNetworkElementPk(physicalPort.getNetworkElementPk());
    physicalPort.setPhysicalResourceGroup(newPhysicalResourceGroup);

    uiModel.asMap().clear();
    physicalPortService.save(physicalPort);

    return "redirect:" + PAGE_URL;
  }

  @RequestMapping(params = ID_KEY, method = RequestMethod.GET)
  public String show(@RequestParam(ID_KEY) final String networkElementPk, final Model uiModel) {
    uiModel.addAttribute(MODEL_KEY, physicalPortService.findByNetworkElementPk(networkElementPk));

    return PAGE_URL + SHOW;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String list(@RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model uiModel) {
    uiModel.addAttribute(MODEL_KEY_LIST, physicalPortService.findEntries(calculateFirstPage(page), MAX_ITEMS_PER_PAGE));
    uiModel.addAttribute(MAX_PAGES_KEY, calculateMaxPages(physicalPortService.count()));

    return PAGE_URL + LIST;
  }

  @RequestMapping(value = "/free", method = RequestMethod.GET)
  public String listUnallocated(@RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model uiModel) {
    uiModel.addAttribute(MODEL_KEY_LIST,
        physicalPortService.findUnallocatedEntries(calculateFirstPage(page), MAX_ITEMS_PER_PAGE));

    uiModel.addAttribute(MAX_PAGES_KEY, calculateMaxPages(physicalPortService.countUnallocated()));

    return PAGE_URL + "/listunallocated";
  }

  @RequestMapping(value = EDIT, params = ID_KEY, method = RequestMethod.GET)
  public String updateForm(@RequestParam(ID_KEY) final String networkElementPk, final Model uiModel) {
    uiModel.addAttribute(MODEL_KEY, physicalPortService.findByNetworkElementPk(networkElementPk));
    return PAGE_URL + UPDATE;
  }

  @RequestMapping(value = DELETE, params = ID_KEY, method = RequestMethod.DELETE)
  public String delete(@RequestParam(ID_KEY) final String networkElementPk,
      @RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model uiModel) {

    PhysicalPort physicalPort = physicalPortService.findByNetworkElementPk(networkElementPk);
    physicalPortService.delete(physicalPort);

    uiModel.asMap().clear();
    uiModel.addAttribute(PAGE_KEY, (page == null) ? "1" : page.toString());

    return "redirect:";
  }

  /**
   * Puts all {@link PhysicalResourceGroup}s on the model, needed to relate a
   * group to a {@link PhysicalPort}.
   *
   * @return Collection<PhysicalResourceGroup>
   */
  @ModelAttribute(PhysicalResourceGroupController.MODEL_KEY_LIST)
  public Collection<PhysicalResourceGroup> populatePhysicalResourceGroups() {
    return physicalResourceGroupService.findAll();
  }

  protected void setPhysicalPortService(PhysicalPortService physicalPortService) {
    this.physicalPortService = physicalPortService;
  }
}
