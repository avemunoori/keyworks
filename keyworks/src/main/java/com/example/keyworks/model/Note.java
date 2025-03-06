package com.example.keyworks.model;

public class Note {
    private int noteNumber;
    private int velocity;
    private long timestamp;
    private long duration;

    public Note(int noteNumber, int velocity, long timestamp, long duration) {
        this.noteNumber = noteNumber;
        this.velocity = velocity;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public int getNoteNumber() {
        return noteNumber;
    }

    public void setNoteNumber(int noteNumber) {
        this.noteNumber = noteNumber;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Note{" +
                "noteNumber=" + noteNumber +
                ", velocity=" + velocity +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                '}';
    }
}