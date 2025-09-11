package com.project2.To_do_list.repo;

import com.project2.To_do_list.model.Note;
import com.project2.To_do_list.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepo extends JpaRepository<Note, Long> {
    List<Note> findByUser(User user);
}
