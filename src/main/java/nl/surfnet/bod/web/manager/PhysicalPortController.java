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

import static nl.surfnet.bod.web.WebUtils.*;

import java.util.Collection;
import java.util.Collections;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.service.InstituteService;
import nl.surfnet.bod.service.PhysicalPortService;
import nl.surfnet.bod.service.VirtualPortService;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;
import nl.surfnet.bod.web.view.VirtualPortJsonView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

@Controller("managerPhysicalPortController")
@RequestMapping("/manager/physicalports")
public class PhysicalPortController {

  @Autowired
  private PhysicalPortService physicalPortService;

  @Autowired
  private VirtualPortService virtualPortService;

  @Autowired
  private InstituteService instituteService;

  private final Function<PhysicalPort, PhysicalPortView> toView = new Function<PhysicalPort, PhysicalPortView>() {
    @Override
    public PhysicalPortView apply(PhysicalPort port) {
      instituteService.fillInstituteForPhysicalResourceGroup(port.getPhysicalResourceGroup());
      Collection<VirtualPort> virtualPorts = virtualPortService.findAllForPhysicalPort(port);
      return new PhysicalPortView(port, virtualPorts);
    }
  };

  @RequestMapping(value = "/edit", params = "id", method = RequestMethod.GET)
  public String updateForm(@RequestParam("id") final Long id, final Model uiModel) {
    PhysicalPort port = physicalPortService.find(id);

    if (port == null || Security.managerMayNotEdit(port)) {
      return "manager/physicalports";
    }

    uiModel.addAttribute("updateManagerLabelCommand", new UpdateManagerLabelCommand(port));
    uiModel.addAttribute("physicalPort", toView.apply(port));

    return "manager/physicalports/update";
  }

  @RequestMapping(method = RequestMethod.PUT)
  public String update(final UpdateManagerLabelCommand command, final BindingResult result, final Model model) {
    PhysicalPort port = physicalPortService.find(command.getId());

    if (port == null || Security.managerMayNotEdit(port)) {
      return "redirect:physicalports";
    }

    port.setManagerLabel(command.getManagerLabel());
    physicalPortService.update(port);

    return "redirect:physicalports";
  }

  @RequestMapping(method = RequestMethod.GET)
  public String list(@RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model model) {
    RichUserDetails user = Security.getUserDetails();

    Collection<PhysicalPortView> ports = Collections2.transform(
        physicalPortService.findAllocatedEntriesForUser(user, calculateFirstPage(page), MAX_ITEMS_PER_PAGE),
        toView);

    model.addAttribute("physicalPorts", ports);
    model.addAttribute(MAX_PAGES_KEY,
        calculateMaxPages(physicalPortService.countAllocatedForUser(user)));

    return "manager/physicalports/list";
  }

  @RequestMapping(value = "/{id}/virtualports", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public Collection<VirtualPortJsonView> listVirtualPortsJson(@PathVariable Long id) {
    PhysicalPort physicalPort = physicalPortService.find(id);

    if (physicalPort == null) {
      return Collections.emptyList();
    }

    return Collections2.transform(virtualPortService.findAllForPhysicalPort(physicalPort),
        new Function<VirtualPort, VirtualPortJsonView>() {
          @Override
          public VirtualPortJsonView apply(VirtualPort port) {
            return new VirtualPortJsonView(port);
          }
        });
  }

  // ****                  **** //
  // ** View/Command objects ** //
  // ****                  **** //
  public static final class UpdateManagerLabelCommand {
    private Long id;
    private Integer version;
    private String managerLabel;

    public UpdateManagerLabelCommand() {
    }

    public UpdateManagerLabelCommand(PhysicalPort port) {
      version = port.getVersion();
      id = port.getId();
      managerLabel = port.getManagerLabel();
    }

    public Long getId() {
      return id;
    }

    public Integer getVersion() {
      return version;
    }

    public String getManagerLabel() {
      return managerLabel;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public void setVersion(Integer version) {
      this.version = version;
    }

    public void setManagerLabel(String managerLabel) {
      this.managerLabel = managerLabel;
    }
  }

  public static final class PhysicalPortView {
    private final Long id;
    private final String managerLabel;
    private final String nocLabel;
    private final PhysicalResourceGroup physicalResourceGroup;
    private final String networkElementPk;
    private final Collection<VirtualPort> virtualPorts;

    public PhysicalPortView(PhysicalPort port, Collection<VirtualPort> virtualPorts) {
      this.id = port.getId();
      this.managerLabel = port.getManagerLabel();
      this.nocLabel = port.getNocLabel();
      this.networkElementPk = port.getNetworkElementPk();
      this.physicalResourceGroup = port.getPhysicalResourceGroup();
      this.virtualPorts = virtualPorts;
    }

    public String getNetworkElementPk() {
      return networkElementPk;
    }

    public Integer getNumberOfVirtualPorts() {
      return virtualPorts.size();
    }

    public String getManagerLabel() {
      return managerLabel;
    }

    public PhysicalResourceGroup getPhysicalResourceGroup() {
      return physicalResourceGroup;
    }

    public Long getId() {
      return id;
    }

    public String getNocLabel() {
      return nocLabel;
    }

  }
}
