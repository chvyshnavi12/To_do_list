package com.project2.To_do_list.service;

import com.project2.To_do_list.model.Habit;
import com.project2.To_do_list.model.User;
import com.project2.To_do_list.repo.HabitRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HabitService {
    @Autowired
    private HabitRepo repo;

    public void addHabit(Habit habit) {
        repo.save(habit);
    }

    public List<Habit> getHabits(User user) {
        return repo.findByUser(user);
    }

    public void incrementDay(Long habitId) {
        Habit habit = repo.findById(habitId).orElseThrow();
        if (habit.getCompletedDays() < habit.getDays()) {
            habit.setCompletedDays(habit.getCompletedDays() + 1);
            repo.save(habit);
        }
    }


    public void resetHabit(Long habitId) {
        Habit habit = repo.findById(habitId).orElseThrow();
        habit.setCompletedDays(0);
        repo.save(habit);
    }
}
