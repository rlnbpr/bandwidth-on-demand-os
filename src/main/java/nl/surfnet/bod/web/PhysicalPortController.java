package nl.surfnet.bod.web;

import java.io.UnsupportedEncodingException;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/physicalports")
@Controller
public class PhysicalPortController {

	@Autowired
    PhysicalPortService physicalPortService;

	@Autowired
    PhysicalResourceGroupService physicalResourceGroupService;

	@RequestMapping(method = RequestMethod.POST)
    public String create(@Valid PhysicalPort physicalPort, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("physicalPort", physicalPort);
            return "physicalports/create";
        }
        uiModel.asMap().clear();
        physicalPortService.savePhysicalPort(physicalPort);
        return "redirect:/physicalports/" + encodeUrlPathSegment(physicalPort.getId().toString(), httpServletRequest);
    }

	@RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model uiModel) {
        uiModel.addAttribute("physicalPort", new PhysicalPort());
        return "physicalports/create";
    }

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("physicalport", physicalPortService.findPhysicalPort(id));
        uiModel.addAttribute("itemId", id);
        return "physicalports/show";
    }

	@RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("physicalports", physicalPortService.findPhysicalPortEntries(firstResult, sizeNo));
            float nrOfPages = (float) physicalPortService.countAllPhysicalPorts() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("physicalports", physicalPortService.findAllPhysicalPorts());
        }
        return "physicalports/list";
    }

	@RequestMapping(method = RequestMethod.PUT)
    public String update(@Valid PhysicalPort physicalPort, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("physicalPort", physicalPort);
            return "physicalports/update";
        }
        uiModel.asMap().clear();
        physicalPortService.updatePhysicalPort(physicalPort);
        return "redirect:/physicalports/" + encodeUrlPathSegment(physicalPort.getId().toString(), httpServletRequest);
    }

	@RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("physicalPort", physicalPortService.findPhysicalPort(id));
        return "physicalports/update";
    }

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        PhysicalPort physicalPort = physicalPortService.findPhysicalPort(id);
        physicalPortService.deletePhysicalPort(physicalPort);
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/physicalports";
    }

	@ModelAttribute("physicalports")
    public Collection<PhysicalPort> populatePhysicalPorts() {
        return physicalPortService.findAllPhysicalPorts();
    }

	@ModelAttribute("physicalresourcegroups")
    public Collection<PhysicalResourceGroup> populatePhysicalResourceGroups() {
        return physicalResourceGroupService.findAllPhysicalResourceGroups();
    }

	String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        try {
            pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        }
        catch (UnsupportedEncodingException uee) {}
        return pathSegment;
    }
}
