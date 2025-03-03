package com.example.keyworks.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sheet_music")
public class SheetMusic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "lily_pond_code", columnDefinition = "TEXT", nullable = false)
    private String lilyPondCode;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "midi_path")
    private String midiPath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Default constructor
    public SheetMusic() {
    }

    // Constructor with fields
    public SheetMusic(String title, String description, String lilyPondCode, User user) {
        this.title = title;
        this.description = description;
        this.lilyPondCode = lilyPondCode;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLilyPondCode() {
        return lilyPondCode;
    }

    public void setLilyPondCode(String lilyPondCode) {
        this.lilyPondCode = lilyPondCode;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getMidiPath() {
        return midiPath;
    }

    public void setMidiPath(String midiPath) {
        this.midiPath = midiPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Pre-persist and pre-update callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}