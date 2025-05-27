package com.everytime.Hackathon2025.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoommateSurveyRequestDto {
    private String dormName;
    private String cleanLevel;
    private boolean smoking;
    private String etc;

    private String wakeUpTime;
    private String sleepTime;
    private String showerTime;
    private String callTime;
}
