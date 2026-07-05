package com.dormmatch.domain.survey.service;

import com.dormmatch.domain.survey.dto.SurveyAnswers;
import com.dormmatch.domain.survey.dto.UserPreferencesRequestDto;
import com.dormmatch.domain.survey.dto.UserPreferencesResponseDto;
import com.dormmatch.domain.survey.entity.UserPreferences;
import com.dormmatch.domain.survey.repository.UserPreferencesRepository;
import com.dormmatch.domain.survey.util.UserPreferencesDtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyService {

    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    public SurveyService(UserPreferencesRepository userPreferencesRepository){
        this.userPreferencesRepository = userPreferencesRepository;
    }

    @Transactional
    public UserPreferencesResponseDto getSurveyStatus(Long userId){

        UserPreferences userPreferences = userPreferencesRepository.findByUserId(userId).get();

        return UserPreferencesDtoMapper.toDto(userPreferences);
    }

    @Transactional
    public void saveSurveyStatus(Long userId, UserPreferencesRequestDto requestDto){
        UserPreferences userPreferences = userPreferencesRepository.findByUserId(userId)
                .orElseThrow(()->new IllegalArgumentException("유저를 찾을 수 없습니다."));

        SurveyAnswers surveyAnswers = userPreferences.getAnswers();

        surveyAnswers.setBedtime(requestDto.getAnswers().getBedtime());
        surveyAnswers.setSnoring(requestDto.getAnswers().getSnoring());
        surveyAnswers.setSleepTalking(requestDto.getAnswers().getSleepTalking());
        surveyAnswers.setOrganizingStyle(requestDto.getAnswers().getOrganizingStyle());
        surveyAnswers.setEatingInRoom(requestDto.getAnswers().getEatingInRoom());
        surveyAnswers.setTemperaturePreference(requestDto.getAnswers().getTemperaturePreference());
        surveyAnswers.setShowerFrequency(requestDto.getAnswers().getShowerFrequency());
        surveyAnswers.setSpeakerStyle(requestDto.getAnswers().getSpeakerStyle());
        surveyAnswers.setCallInRoom(requestDto.getAnswers().getCallInRoom());
    }

}
