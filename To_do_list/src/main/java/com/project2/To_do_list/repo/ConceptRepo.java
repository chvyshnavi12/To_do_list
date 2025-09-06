package com.project2.To_do_list.repo;

import com.project2.To_do_list.model.Concept;
import com.project2.To_do_list.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRepo extends JpaRepository<Concept, Long> {
    List<Concept> findByUser(User user);
    List<Concept> findByUserAndRevisionsCompleted(User user, boolean completed);
}