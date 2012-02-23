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
import java.util.Collections;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import nl.surfnet.bod.domain.Institute;
import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.service.InstituteService;
import nl.surfnet.bod.service.PhysicalResourceGroupService;
import nl.surfnet.bod.web.WebUtils;
import nl.surfnet.bod.web.view.PhysicalPortJsonView;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

@Controller("nocPhysicalResourceGroupController")
@RequestMapping("/noc/" + PhysicalResourceGroupController.PAGE_URL)
public class PhysicalResourceGroupController {

  static final String PAGE_URL = "physicalresourcegroups";
  static final String MODEL_KEY = "physicalResourceGroupCommand";
  static final String MODEL_KEY_LIST = "physicalResourceGroup" + WebUtils.LIST_POSTFIX;

  @Autowired
  private PhysicalResourceGroupService physicalResourceGroupService;

  @Autowired
  private InstituteService instituteService;

  @Autowired
  private MessageSource messageSource;

  @RequestMapping(method = RequestMethod.POST)
  public String create(@Valid final PhysicalResourceGroupCommand command, final BindingResult bindingResult,
      final RedirectAttributes redirectAttributes, final Model model) {

    PhysicalResourceGroup physicalResourceGroup = new PhysicalResourceGroup();
    command.copyFieldsTo(physicalResourceGroup);
    fillInstitute(command, physicalResourceGroup);

    if (bindingResult.hasErrors()) {
      model.addAttribute(MODEL_KEY, command);
      return PAGE_URL + CREATE;
    }

    model.asMap().clear();

    physicalResourceGroupService.save(physicalResourceGroup);
    physicalResourceGroupService.sendActivationRequest(physicalResourceGroup);

    WebUtils.addInfoMessage(redirectAttributes, messageSource, "info_activation_request_send",
        physicalResourceGroup.getName(), physicalResourceGroup.getManagerEmail());

    return "redirect:" + PAGE_URL;
  }

  @RequestMapping(value = CREATE, method = RequestMethod.GET)
  public String createForm(final Model model) {
    model.addAttribute(MODEL_KEY, new PhysicalResourceGroupCommand());

    return PAGE_URL + CREATE;
  }

  @RequestMapping(params = ID_KEY, method = RequestMethod.GET)
  public String show(@RequestParam(ID_KEY) final Long id, final Model uiModel) {
    uiModel.addAttribute(MODEL_KEY, physicalResourceGroupService.find(id));

    return PAGE_URL + SHOW;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String list(@RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model model) {
    model.addAttribute(MODEL_KEY_LIST,
        physicalResourceGroupService.findEntries(calculateFirstPage(page), MAX_ITEMS_PER_PAGE));

    model.addAttribute(MAX_PAGES_KEY, calculateMaxPages(physicalResourceGroupService.count()));

    return PAGE_URL + LIST;
  }

  @RequestMapping(value = "/{id}/ports", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public Collection<PhysicalPortJsonView> listPortsJson(@PathVariable Long id) {
    PhysicalResourceGroup group = physicalResourceGroupService.find(id);

    if (group == null) {
      return Collections.emptyList();
    }

    return Collections2.transform(group.getPhysicalPorts(), new Function<PhysicalPort, PhysicalPortJsonView>() {
      @Override
      public PhysicalPortJsonView apply(PhysicalPort port) {
        return new PhysicalPortJsonView(port);
      }
    });
  }

  @RequestMapping(method = RequestMethod.PUT)
  public String update(@Valid final PhysicalResourceGroupCommand command, final BindingResult result,
      final Model model, final RedirectAttributes redirectAttributes) {

    PhysicalResourceGroup physicalResourceGroup = physicalResourceGroupService.find(command.getId());

    if (physicalResourceGroup == null) {
      return "redirect:" + PAGE_URL;
    }

    command.copyFieldsTo(physicalResourceGroup);
    fillInstitute(command, physicalResourceGroup);

    if (result.hasErrors()) {
      model.addAttribute(MODEL_KEY, command);
      return PAGE_URL + UPDATE;
    }

    model.asMap().clear();

    physicalResourceGroupService.update(physicalResourceGroup);

    if (command.isManagerEmailChanged()) {
      physicalResourceGroupService.sendActivationRequest(physicalResourceGroup);
      addInfoMessage(redirectAttributes, messageSource, "info_activation_request_send",
          physicalResourceGroup.getName(), physicalResourceGroup.getManagerEmail());
    }

    return "redirect:" + PAGE_URL;
  }

  @RequestMapping(value = EDIT, params = ID_KEY, method = RequestMethod.GET)
  public String updateForm(@RequestParam(ID_KEY) final Long id, final Model uiModel) {
    PhysicalResourceGroup group = physicalResourceGroupService.find(id);
    instituteService.fillInstituteForPhysicalResourceGroup(group);

    uiModel.addAttribute(MODEL_KEY, new PhysicalResourceGroupCommand(group));
    return PAGE_URL + UPDATE;
  }

  @RequestMapping(value = DELETE, params = ID_KEY, method = RequestMethod.DELETE)
  public String delete(@RequestParam(ID_KEY) final Long id,
      @RequestParam(value = PAGE_KEY, required = false) final Integer page, final Model uiModel) {

    PhysicalResourceGroup physicalResourceGroup = physicalResourceGroupService.find(id);
    physicalResourceGroupService.delete(physicalResourceGroup);

    uiModel.asMap().clear();

    uiModel.addAttribute(PAGE_KEY, (page == null) ? "1" : page.toString());

    return "redirect:";
  }

  private void fillInstitute(final PhysicalResourceGroupCommand command, PhysicalResourceGroup group) {
    if (command.getInstituteId() == null) {
      return;
    }

    Institute institute = instituteService.findInstitute(command.getInstituteId());
    command.setInstitute(institute);
    group.setInstitute(institute);
  }

  public static final class PhysicalResourceGroupCommand {

    private Long id;
    private Integer version;
    @NotNull
    private Long instituteId;
    private Institute institute;
    @NotEmpty
    private String adminGroup;
    @NotEmpty
    @Email(message = "Not a valid email address")
    private String managerEmail;
    private boolean active = false;
    private boolean managerEmailChanged;

    public PhysicalResourceGroupCommand() {
    }

    public PhysicalResourceGroupCommand(PhysicalResourceGroup physicalResourceGroup) {
      this.id = physicalResourceGroup.getId();
      this.version = physicalResourceGroup.getVersion();

      this.instituteId = physicalResourceGroup.getInstituteId();
      this.institute = physicalResourceGroup.getInstitute();
      this.adminGroup = physicalResourceGroup.getAdminGroup();
      this.active = physicalResourceGroup.isActive();
      this.managerEmail = physicalResourceGroup.getManagerEmail();
    }

    /**
     * Copies fields this command object to the given domainOjbect. Only the
     * fields that can be changed in the UI will be copied. Determines if the
     * {@link #managerEmail} has changed.
     *
     * @param physicalResourceGroup
     *          The {@link PhysicalResourceGroup} the copy the field to.
     */
    public void copyFieldsTo(PhysicalResourceGroup physicalResourceGroup) {
      managerEmailChanged = hasManagerEmailChanged(physicalResourceGroup.getManagerEmail());

      // Never copy id, should be generated by jpa
      physicalResourceGroup.setInstituteId(instituteId);
      physicalResourceGroup.setAdminGroup(adminGroup);
      physicalResourceGroup.setManagerEmail(managerEmail);
    }

    private boolean hasManagerEmailChanged(String newEmail) {
      return managerEmail == null || !managerEmail.equals(newEmail);
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getId() {
      return id;
    }

    public Integer getVersion() {
      return version;
    }

    public Long getInstituteId() {
      return instituteId;
    }

    public void setInstituteId(Long instituteId) {
      this.instituteId = instituteId;
    }

    public String getAdminGroup() {
      return adminGroup;
    }

    public Institute getInstitute() {
      return institute;
    }

    public void setInstitute(Institute institute) {
      this.institute = institute;
    }

    public void setAdminGroup(String adminGroup) {
      this.adminGroup = adminGroup;
    }

    public String getManagerEmail() {
      return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
      this.managerEmail = managerEmail;
    }

    public boolean isActive() {
      return active;
    }

    public boolean isManagerEmailChanged() {
      return managerEmailChanged;
    }

    public String getName() {
      return institute != null ? institute.getName() : String.valueOf(instituteId);
    }

  }
}
