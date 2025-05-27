package com.everytime.Hackathon2025.Domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@NoArgsConstructor
@Getter
public class RoommateSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "dorm_name", nullable = false)
    private String dormName;

    @Column(name = "clean_level", nullable = false)
    private String cleanLevel;

    @Column(nullable = false)
    private boolean smoking;

    @Column(length = 500) //기타
    private String etc;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "wake_up_time")
    private String wakeUpTime;

    @Column(name = "sleep_time")
    private String sleepTime;

    @Column(name = "shower_time")
    private String showerTime;

    @Column(name = "call_time")
    private String callTime;


   public RoommateSurvey(User user, String dormName, String cleanLevel, boolean smoking, String etc,
                      String wakeUpTime, String sleepTime, String showerTime, String callTime) {
    this.user = user;
    this.dormName = dormName;
    this.cleanLevel = cleanLevel;
    this.smoking = smoking;
    this.etc = etc;
    this.wakeUpTime = wakeUpTime;
    this.sleepTime = sleepTime;
    this.showerTime = showerTime;
    this.callTime = callTime;
    this.createdAt = LocalDateTime.now();
  }
}
