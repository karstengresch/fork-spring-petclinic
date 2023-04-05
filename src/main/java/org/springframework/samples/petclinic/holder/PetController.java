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

import java.util.Collection;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
@RequestMapping("/holders/{holderId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final HolderRepository holders;

	public PetController(HolderRepository holders) {
		this.holders = holders;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.holders.findPetTypes();
	}

	@ModelAttribute("holder")
	public Holder findHolder(@PathVariable("holderId") int holderId) {
		return this.holders.findById(holderId);
	}

	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("holderId") int holderId,
			@PathVariable(name = "petId", required = false) Integer petId) {
		return petId == null ? new Pet() : this.holders.findById(holderId).getPet(petId);
	}

	@InitBinder("holder")
	public void initHolderBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
	}

	@GetMapping("/pets/new")
	public String initCreationForm(Holder holder, ModelMap model) {
		Pet pet = new Pet();
		holder.addPet(pet);
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public String processCreationForm(Holder holder, @Valid Pet pet, BindingResult result, ModelMap model) {
		if (StringUtils.hasLength(pet.getName()) && pet.isNew() && holder.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}

		holder.addPet(pet);
		if (result.hasErrors()) {
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		this.holders.save(holder);
		return "redirect:/holders/{holderId}";
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm(Holder holder, @PathVariable("petId") int petId, ModelMap model) {
		Pet pet = holder.getPet(petId);
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(@Valid Pet pet, BindingResult result, Holder holder, ModelMap model) {
		if (result.hasErrors()) {
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		holder.addPet(pet);
		this.holders.save(holder);
		return "redirect:/holders/{holderId}";
	}

}
