package com.project2.To_do_list.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CONCEPT")
public class Concept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Revision> revisions = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Revision> getRevisions() { return revisions; }
    public void setRevisions(List<Revision> revisions) { this.revisions = revisions; }

    // Add this method
    public void addRevision(Revision revision) {
        revision.setConcept(this);
        this.revisions.add(revision);
    }
    @Transient
    private int progress;

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}