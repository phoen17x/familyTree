package org.example.familyTree.controllers;

import org.example.familyTree.dao.PersonDAO;
import org.example.familyTree.models.Parents;
import org.example.familyTree.models.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
public class PeopleController {
    private final PersonDAO personDAO;

    @Autowired
    public PeopleController(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }

    // Redirect to random person in family tree
    @GetMapping
    public String index() {

        Integer id = personDAO.getRandomPersonID();
        return String.format("redirect:/%d", id);

    }

    // Create model with person, their spouse, parents and children
    @GetMapping("/{id}")
    public String getRelatives(@PathVariable("id") int id,
                               Model model) {

        Person person = personDAO.getPerson(id);
        Integer spouseID = person.getSpouse_id();

        model.addAttribute("person", person);
        if (spouseID != null)
            model.addAttribute("spouse", personDAO.getPerson(spouseID));
        model.addAttribute("parents", personDAO.getParents(id));
        model.addAttribute("children", personDAO.getChildren(id));

        return "index";

    }

    // Go to parent-creation form
    @GetMapping("/{id}/parents")
    public String addParents(@ModelAttribute("parents") Parents parents,
                             @ModelAttribute("id") @PathVariable("id") Integer id) {

        return "parentsform";

    }

    // Add parents to database if valid
    @PostMapping("{id}/parents")
    public String saveParents(@PathVariable("id") Integer id,
                              @ModelAttribute("parents") @Valid Parents parents,
                              BindingResult bindingResult) {

        if (bindingResult.hasErrors())
            return "parentsform";

        parents.setChildID(id);
        personDAO.addParents(parents);

        return String.format("redirect:/%d", id);

    }

    // Go to spouse-creation form
    @GetMapping("/{id}/spouse")
    public String addSpouse(@ModelAttribute("person") Person person,
                            @ModelAttribute("id") @PathVariable("id") Integer id) {

        return "spouseform";

    }

    //Add spouse to database if valid
    @PostMapping("{id}/spouse")
    public String saveSpouse(@PathVariable("id") Integer id,
                             @ModelAttribute("person") @Valid Person person,
                             BindingResult bindingResult) {

        if (bindingResult.hasErrors())
            return "spouseform";

        person.setSpouse_id(id);
        personDAO.addSpouse(person);
        return String.format("redirect:/%d", id);

    }

    // Go to child-creation form
    @GetMapping("/{id}/child")
    public String addChild(@ModelAttribute("person") Person person,
                            @ModelAttribute("id") @PathVariable("id") Integer id) {

        return "childform";

    }

    // Add child to database if valid
    @PostMapping("{id}/child")
    public String saveChild(@PathVariable("id") Integer id,
                            @ModelAttribute("person") @Valid Person person,
                            BindingResult bindingResult) {

        if (bindingResult.hasErrors())
            return "childform";

        personDAO.addChild(person, id);
        return String.format("redirect:/%d", id);

    }

    // Deletes person, their parents, grandparents, etc.
    // Same process for person's spouse
    @DeleteMapping("/{id}")
    public String deletePerson(@PathVariable("id") Integer id) {

        personDAO.deletePerson(id);
        return "redirect:";

    }

}
