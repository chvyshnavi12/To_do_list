package com.project2.To_do_list.repo;

import com.project2.To_do_list.model.PdfFile;
import com.project2.To_do_list.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PdfFileRepo extends JpaRepository<PdfFile, Long> {
    List<PdfFile> findByUser(User user); // Now this works
}
