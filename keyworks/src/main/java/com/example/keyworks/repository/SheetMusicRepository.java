package com.example.keyworks.repository;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetMusicRepository extends JpaRepository<SheetMusic, Long> {
    List<SheetMusic> findByUser(User user);
    List<SheetMusic> findByUserOrderByCreatedAtDesc(User user);
    List<SheetMusic> findByTitleContainingIgnoreCase(String title);
}