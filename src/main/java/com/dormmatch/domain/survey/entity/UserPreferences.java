package com.dormmatch.domain.survey.entity;

import com.dormmatch.domain.survey.dto.SurveyAnswers;
import com.dormmatch.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    // 하드 코딩용 설문
    @Column(name = "smoking_status", nullable = false)
    private Integer smokingStatus;

    // 자기 소개
    @Column(columnDefinition = "TEXT")
    private String introduce;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb", nullable = false)
    private SurveyAnswers answers = new SurveyAnswers();

    // 정규화 벡터
    @Builder.Default
    @Column(name = "lifestyle_vector", columnDefinition = "vector(9)")
    private double[] lifestyleVector = new double[9];

    @PrePersist
    @PreUpdate
    public void onCreateOrUpdate() {
        convertToNormalizedVector();
    }

    private void convertToNormalizedVector() {
        this.lifestyleVector[0] = normalize(this.answers.getBedtime(), 1, 5);
        this.lifestyleVector[1] = normalize(this.answers.getSnoring(), 1, 5);
        this.lifestyleVector[2] = normalize(this.answers.getSleepTalking(), 1, 5);
        this.lifestyleVector[3] = normalize(this.answers.getOrganizingStyle(), 1, 5);
        this.lifestyleVector[4] = normalize(this.answers.getTemperaturePreference(), 1, 3);
        this.lifestyleVector[5] = normalize(this.answers.getShowerFrequency(), 1 ,5);
        this.lifestyleVector[6] = normalize(this.answers.getSpeakerStyle(), 1, 5);
        this.lifestyleVector[7] = normalize(this.answers.getCallInRoom(), 1, 5);
        this.lifestyleVector[8] = normalize(this.answers.getEatingInRoom(), 1, 3);
    }

    // 벡터 정규화
    private double normalize(Integer value, int min, int max){

        if(value == null)   return 0.0;

        if(max == min)  return 0.0;

        if(value < min) value = min;
        if(value > max) value = max;

        return ((double) value - min)/((double) max - min);
    }


    // 메타

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
