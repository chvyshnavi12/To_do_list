package com.project2.To_do_list.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "REVISION") // Explicitly map to your table name
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day") // Map to your column name
    private int day;

    @Column(name = "date") // Map to your column name
    private LocalDate date;

    @Column(name = "completed") // Map to your column name
    private boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id") // This matches your column name
    private Concept concept;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Concept getConcept() { return concept; }
    public void setConcept(Concept concept) { this.concept = concept; }
}