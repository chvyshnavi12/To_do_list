package com.project2.To_do_list.service;

import com.project2.To_do_list.model.Concept;
import com.project2.To_do_list.model.Revision;
import com.project2.To_do_list.model.User;
import com.project2.To_do_list.repo.ConceptRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ConceptService {

    @Autowired
    private ConceptRepo conceptRepo;

    @Transactional
    public List<Concept> getUserConcepts(User user) {
        return conceptRepo.findByUser(user);
    }

    @Transactional
    public Concept addConcept(String name, User user) {
        Concept concept = new Concept();
        concept.setName(name);
        concept.setUser(user);

        LocalDate today = LocalDate.now();

        // Create revisions
        Revision rev1 = new Revision();
        rev1.setDay(1);
        rev1.setDate(today);
        rev1.setCompleted(false);

        Revision rev4 = new Revision();
        rev4.setDay(4);
        rev4.setDate(today.plusDays(3));
        rev4.setCompleted(false);

        Revision rev7 = new Revision();
        rev7.setDay(7);
        rev7.setDate(today.plusDays(6));
        rev7.setCompleted(false);

        // Add revisions to concept using the helper method
        concept.addRevision(rev1);
        concept.addRevision(rev4);
        concept.addRevision(rev7);

        return conceptRepo.save(concept);
    }

    @Transactional
    public void toggleRevision(Long conceptId, int revisionIndex, User user) {
        Concept concept = conceptRepo.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));

        if (!concept.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (revisionIndex >= 0 && revisionIndex < concept.getRevisions().size()) {
            Revision revision = concept.getRevisions().get(revisionIndex);
            revision.setCompleted(!revision.isCompleted());
            conceptRepo.save(concept);
        }
    }

    @Transactional
    public void deleteConcept(Long conceptId, User user) {
        Concept concept = conceptRepo.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));

        if (!concept.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        conceptRepo.delete(concept);
    }

    @Transactional
    public ConceptStats getStats(User user) {
        List<Concept> concepts = conceptRepo.findByUser(user);
        int totalConcepts = concepts.size();
        int completedRevisions = 0;
        int pendingRevisions = 0;

        for (Concept concept : concepts) {
            for (Revision revision : concept.getRevisions()) {
                if (revision.isCompleted()) {
                    completedRevisions++;
                } else {
                    pendingRevisions++;
                }
            }
        }

        return new ConceptStats(totalConcepts, completedRevisions, pendingRevisions);
    }

    public static class ConceptStats {
        private int totalConcepts;
        private int completedRevisions;
        private int pendingRevisions;

        public ConceptStats(int totalConcepts, int completedRevisions, int pendingRevisions) {
            this.totalConcepts = totalConcepts;
            this.completedRevisions = completedRevisions;
            this.pendingRevisions = pendingRevisions;
        }

        public int getTotalConcepts() { return totalConcepts; }
        public int getCompletedRevisions() { return completedRevisions; }
        public int getPendingRevisions() { return pendingRevisions; }
    }
}