package nl.surfnet.bod.web.manager;

import nl.surfnet.bod.domain.ActivationEmailLink;
import nl.surfnet.bod.service.PhysicalResourceGroupService;

import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping("/manager/activate")
@Controller
public class ActivationEmailController {

  @Autowired
  private PhysicalResourceGroupService physicalResourceGroupService;

  @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
  public String activateEmail(@PathVariable String uuid) {
    ActivationEmailLink link = physicalResourceGroupService.findActivationLink(uuid);

    if (link == null || link.getCreationDateTime().isAfter(LocalDateTime.now().plusDays(5))) {
      // TODO different page?
      return "index";
    }

    physicalResourceGroupService.activate(link.getPhysicalResourceGroup());

    return "manager/emailConfirmed";
  }
}
