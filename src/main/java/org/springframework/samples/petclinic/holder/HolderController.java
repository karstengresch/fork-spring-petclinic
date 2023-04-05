/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.holder;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class HolderController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "holders/createOrUpdateHolderForm";

	private final HolderRepository holders;

	public HolderController(HolderRepository clinicService) {
		this.holders = clinicService;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@ModelAttribute("holder")
	public Holder findHolder(@PathVariable(name = "holderId", required = false) Integer holderId) {
		return holderId == null ? new Holder() : this.holders.findById(holderId);
	}

	@GetMapping("/holders/new")
	public String initCreationForm(Map<String, Object> model) {
		Holder holder = new Holder();
		model.put("holder", holder);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/holders/new")
	public String processCreationForm(@Valid Holder holder, BindingResult result) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		this.holders.save(holder);
		return "redirect:/holders/" + holder.getId();
	}

	@GetMapping("/holders/find")
	public String initFindForm() {
		return "holders/findHolders";
	}

	@GetMapping("/holders")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Holder holder, BindingResult result,
			Model model) {
		// allow parameterless GET request for /holders to return all records
		if (holder.getLastName() == null) {
			holder.setLastName(""); // empty string signifies broadest possible search
		}

		// find holders by last name
		Page<Holder> holdersResults = findPaginatedForHoldersLastName(page, holder.getLastName());
		if (holdersResults.isEmpty()) {
			// no holders found
			result.rejectValue("lastName", "notFound", "not found");
			return "holders/findHolders";
		}

		if (holdersResults.getTotalElements() == 1) {
			// 1 holder found
			holder = holdersResults.iterator().next();
			return "redirect:/holders/" + holder.getId();
		}

		// multiple holders found
		return addPaginationModel(page, model, holdersResults);
	}

	private String addPaginationModel(int page, Model model, Page<Holder> paginated) {
		model.addAttribute("listHolders", paginated);
		List<Holder> listHolders = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listHolders", listHolders);
		return "holders/holdersList";
	}

	private Page<Holder> findPaginatedForHoldersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return holders.findByLastName(lastname, pageable);
	}

	@GetMapping("/holders/{holderId}/edit")
	public String initUpdateHolderForm(@PathVariable("holderId") int holderId, Model model) {
		Holder holder = this.holders.findById(holderId);
		model.addAttribute(holder);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/holders/{holderId}/edit")
	public String processUpdateHolderForm(@Valid Holder holder, BindingResult result,
			@PathVariable("holderId") int holderId) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		holder.setId(holderId);
		this.holders.save(holder);
		return "redirect:/holders/{holderId}";
	}

	/**
	 * Custom handler for displaying an holder.
	 * @param holderId the ID of the holder to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/holders/{holderId}")
	public ModelAndView showHolder(@PathVariable("holderId") int holderId) {
		ModelAndView mav = new ModelAndView("holders/holderDetails");
		Holder holder = this.holders.findById(holderId);
		mav.addObject(holder);
		return mav;
	}

}
