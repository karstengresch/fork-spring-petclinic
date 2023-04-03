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

package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.holder.Holder;
import org.springframework.samples.petclinic.holder.HolderRepository;
import org.springframework.samples.petclinic.holder.Pet;
import org.springframework.samples.petclinic.holder.PetType;
import org.springframework.samples.petclinic.holder.Visit;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the Service and the Repository layer.
 * <p>
 * ClinicServiceSpringDataJpaTests subclasses benefit from the following services provided
 * by the Spring TestContext Framework:
 * </p>
 * <ul>
 * <li><strong>Spring IoC container caching</strong> which spares us unnecessary set up
 * time between test execution.</li>
 * <li><strong>Dependency Injection</strong> of test fixture instances, meaning that we
 * don't need to perform application context lookups. See the use of
 * {@link Autowired @Autowired} on the <code> </code> instance variable, which uses
 * autowiring <em>by type</em>.
 * <li><strong>Transaction management</strong>, meaning each test method is executed in
 * its own transaction, which is automatically rolled back by default. Thus, even if tests
 * insert or otherwise change database state, there is no need for a teardown or cleanup
 * script.
 * <li>An {@link org.springframework.context.ApplicationContext ApplicationContext} is
 * also inherited and can be used for explicit bean lookup if necessary.</li>
 * </ul>
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */
@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
// Ensure that if the mysql profile is active we connect to the real database:
@AutoConfigureTestDatabase(replace = Replace.NONE)
// @TestPropertySource("/application-postgres.properties")
class ClinicServiceTests {

	@Autowired
	protected HolderRepository holders;

	@Autowired
	protected VetRepository vets;

	Pageable pageable;

	@Test
	void shouldFindHoldersByLastName() {
		Page<Holder> holders = this.holders.findByLastName("Davis", pageable);
		assertThat(holders).hasSize(2);

		holders = this.holders.findByLastName("Daviss", pageable);
		assertThat(holders).isEmpty();
	}

	@Test
	void shouldFindSingleHolderWithPet() {
		Holder holder = this.holders.findById(1);
		assertThat(holder.getLastName()).startsWith("Franklin");
		assertThat(holder.getPets()).hasSize(1);
		assertThat(holder.getPets().get(0).getType()).isNotNull();
		assertThat(holder.getPets().get(0).getType().getName()).isEqualTo("cat");
	}

	@Test
	@Transactional
	void shouldInsertHolder() {
		Page<Holder> holders = this.holders.findByLastName("Schultz", pageable);
		int found = (int) holders.getTotalElements();

		Holder holder = new Holder();
		holder.setFirstName("Sam");
		holder.setLastName("Schultz");
		holder.setAddress("4, Evans Street");
		holder.setCity("Wollongong");
		holder.setTelephone("4444444444");
		this.holders.save(holder);
		assertThat(holder.getId().longValue()).isNotEqualTo(0);

		holders = this.holders.findByLastName("Schultz", pageable);
		assertThat(holders.getTotalElements()).isEqualTo(found + 1);
	}

	@Test
	@Transactional
	void shouldUpdateHolder() {
		Holder holder = this.holders.findById(1);
		String oldLastName = holder.getLastName();
		String newLastName = oldLastName + "X";

		holder.setLastName(newLastName);
		this.holders.save(holder);

		// retrieving new name from database
		holder = this.holders.findById(1);
		assertThat(holder.getLastName()).isEqualTo(newLastName);
	}

	@Test
	void shouldFindAllPetTypes() {
		Collection<PetType> petTypes = this.holders.findPetTypes();

		PetType petType1 = EntityUtils.getById(petTypes, PetType.class, 1);
		assertThat(petType1.getName()).isEqualTo("cat");
		PetType petType4 = EntityUtils.getById(petTypes, PetType.class, 4);
		assertThat(petType4.getName()).isEqualTo("snake");
	}

	@Test
	@Transactional
	void shouldInsertPetIntoDatabaseAndGenerateId() {
		Holder holder6 = this.holders.findById(6);
		int found = holder6.getPets().size();

		Pet pet = new Pet();
		pet.setName("bowser");
		Collection<PetType> types = this.holders.findPetTypes();
		pet.setType(EntityUtils.getById(types, PetType.class, 2));
		pet.setBirthDate(LocalDate.now());
		holder6.addPet(pet);
		assertThat(holder6.getPets().size()).isEqualTo(found + 1);

		this.holders.save(holder6);

		holder6 = this.holders.findById(6);
		assertThat(holder6.getPets().size()).isEqualTo(found + 1);
		// checks that id has been generated
		pet = holder6.getPet("bowser");
		assertThat(pet.getId()).isNotNull();
	}

	@Test
	@Transactional
	void shouldUpdatePetName() throws Exception {
		Holder holder6 = this.holders.findById(6);
		Pet pet7 = holder6.getPet(7);
		String oldName = pet7.getName();

		String newName = oldName + "X";
		pet7.setName(newName);
		this.holders.save(holder6);

		holder6 = this.holders.findById(6);
		pet7 = holder6.getPet(7);
		assertThat(pet7.getName()).isEqualTo(newName);
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.getLastName()).isEqualTo("Douglas");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
		assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");
	}

	@Test
	@Transactional
	void shouldAddNewVisitForPet() {
		Holder holder6 = this.holders.findById(6);
		Pet pet7 = holder6.getPet(7);
		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.setDescription("test");

		holder6.addVisit(pet7.getId(), visit);
		this.holders.save(holder6);

		holder6 = this.holders.findById(6);

		assertThat(pet7.getVisits()) //
			.hasSize(found + 1) //
			.allMatch(value -> value.getId() != null);
	}

	@Test
	void shouldFindVisitsByPetId() throws Exception {
		Holder holder6 = this.holders.findById(6);
		Pet pet7 = holder6.getPet(7);
		Collection<Visit> visits = pet7.getVisits();

		assertThat(visits) //
			.hasSize(2) //
			.element(0)
			.extracting(Visit::getDate)
			.isNotNull();
	}

}
