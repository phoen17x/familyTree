package org.example.familyTree.dao;

import org.example.familyTree.models.Parents;
import org.example.familyTree.models.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersonDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PersonDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Return random person id
    public Integer getRandomPersonID() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM family OFFSET floor(random() * (SELECT COUNT(*) FROM family)) LIMIT 1",
                Integer.class);
    }


    // Searching by id return person
    public Person getPerson(int id) {

        return jdbcTemplate.query("SELECT * FROM family WHERE ID = ?",
                        new BeanPropertyRowMapper<>(Person.class), id)
                .stream().findAny().orElse(null);

    }


    // Searching by person's id return list of parents
    public List<Person> getParents(int id) {

        return jdbcTemplate.query(
                "SELECT * FROM family WHERE id IN (SELECT parent_id FROM relation WHERE child_id = ?)",
                new BeanPropertyRowMapper<>(Person.class), id);

    }

    // Searching by person's id return list of children
    public List<Person> getChildren(int id) {

        return jdbcTemplate.query(
                "SELECT * FROM family WHERE id IN (SELECT child_id FROM relation WHERE parent_id = ?)",
                new BeanPropertyRowMapper<>(Person.class), id);

    }

    public void addParents(Parents parents) {

        Integer childID = parents.getChildID();

        // Add first parent to database and get id
        Integer firstParentID = jdbcTemplate.queryForObject(
                "INSERT INTO family (name) VALUES (?) RETURNING id",
                Integer.class, parents.getParentName1());

        // Add first parent to database and get id
        Integer secondParentID = jdbcTemplate.queryForObject(
                "INSERT INTO family (name, spouse_id) VALUES (?, ?) RETURNING id",
                Integer.class, parents.getParentName2(), firstParentID);

        jdbcTemplate.update(
                "UPDATE family SET spouse_id = ? WHERE id = ?",
                secondParentID, firstParentID);

        // Add relations
        jdbcTemplate.update(
                "INSERT INTO relation (parent_id, child_id) VALUES (?, ?), (?, ?)",
                firstParentID, childID, secondParentID, childID);

    }

    public void addSpouse(Person person) {

        // Add spouse to database and get id
        Integer spouseID = jdbcTemplate.queryForObject(
                "INSERT INTO family (name, spouse_id) VALUES (?, ?) RETURNING id",
                Integer.class, person.getName(), person.getSpouse_id());

        jdbcTemplate.update(
                "UPDATE family SET spouse_id = ? WHERE id = ?", spouseID, person.getSpouse_id());

    }

    public void addChild(Person person, Integer id) {

        // Add child to database and get id
        Integer childID = jdbcTemplate.queryForObject(
                "INSERT INTO family (name) VALUES (?) RETURNING id",
                Integer.class, person.getName());

        Integer spouseID = jdbcTemplate.queryForObject(
                "SELECT spouse_id FROM family WHERE id = ?",
                Integer.class, id);

        // Add relations
        jdbcTemplate.update(
                "INSERT INTO relation (parent_id, child_id) VALUES (?, ?), (?, ?)",
                id, childID, spouseID, childID);

    }

    // Delete person, their parents, grandparents, etc.
    // Same process for person's spouse
    public void deletePerson(Integer id) {

        // Recursively delete rows from "relation" starting with specified element
        String recursiveSQL = "WITH RECURSIVE cte AS (SELECT parent_id, child_id FROM relation WHERE parent_id = ? UNION ALL " +
                "SELECT t.parent_id, t.child_id FROM relation t INNER JOIN cte c on c.parent_id = t.child_id" +
                ") DELETE FROM relation WHERE parent_id IN (SELECT parent_id FROM cte)";

        // Delete everyone who is not in "relation"
        String deleteSQL = "DELETE FROM family WHERE id NOT IN (SELECT parent_id FROM relation) " +
                "AND id NOT IN (SELECT child_id FROM relation)";

        Integer spouseID = getPerson(id).getSpouse_id();

        jdbcTemplate.update(recursiveSQL, id);
        jdbcTemplate.update(recursiveSQL, spouseID);

        jdbcTemplate.update(deleteSQL);

    }
}
