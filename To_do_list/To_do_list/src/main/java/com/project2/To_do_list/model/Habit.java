package com.project2.To_do_list.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
public class Habit {
    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer days; // Total target days
    private Integer completedDays = 0;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public void setId(Long id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setDays(Integer days) { this.days = days; }

    public void setCompletedDays(Integer completedDays) { this.completedDays = completedDays; }

    public void setUser(User user) { this.user = user; }

    // Helper method for progress percentage
    public Integer getProgressPercent() {
        if (days == null || days == 0) return 0;
        return (completedDays * 100) / days;
    }

    // Helper method for calendar days (simplified version)
    public List<DayStatus> getDaysList() {
        List<DayStatus> daysList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            DayStatus day = new DayStatus();
            day.setDayNumber(i + 1);
            day.setCompleted(i < completedDays);
            daysList.add(day);
        }
        return daysList;
    }

    // Inner class for day status
    public static class DayStatus {
        private int dayNumber;
        private boolean completed;

        public int getDayNumber() { return dayNumber; }
        public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}