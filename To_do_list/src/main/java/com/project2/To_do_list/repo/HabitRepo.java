package com.project2.To_do_list.repo;

import com.project2.To_do_list.model.Habit;
import com.project2.To_do_list.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepo extends JpaRepository<Habit, Long> {
    List<Habit> findByUser(User user);
}
