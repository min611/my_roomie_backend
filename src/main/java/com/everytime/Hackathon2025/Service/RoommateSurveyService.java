package com.everytime.Hackathon2025.Service;

import com.everytime.Hackathon2025.Domain.RoommateSurvey;
import com.everytime.Hackathon2025.Domain.User;
import com.everytime.Hackathon2025.Dto.MatchingResultResponseDto;
import com.everytime.Hackathon2025.Dto.RoommateSurveyRequestDto;
import com.everytime.Hackathon2025.Dto.RoommateSurveyResponseDto;
import com.everytime.Hackathon2025.Dto.UserSimpleResponseDto;
import com.everytime.Hackathon2025.Exception.InvalidAccessException;
import com.everytime.Hackathon2025.Repository.RoommateSurveyRepository;
import com.everytime.Hackathon2025.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoommateSurveyService {
    private final RoommateSurveyRepository roommateSurveyRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public RoommateSurveyService(RoommateSurveyRepository roommateSurveyRepository, UserService userService, UserRepository userRepository) {
        this.roommateSurveyRepository = roommateSurveyRepository;
        this.userService = userService;
        this.userRepository = userRepository;

    }

    public void saveSurvey(RoommateSurvey survey) {
        RoommateSurvey savedSurvey = roommateSurveyRepository.save(survey);
    }

    public RoommateSurveyResponseDto createSurvey(User user, RoommateSurveyRequestDto roommateSurveyRequestDto) {

       RoommateSurvey survey = new RoommateSurvey(
    user,
    roommateSurveyRequestDto.getDormName(),
    roommateSurveyRequestDto.getCleanLevel(),
    roommateSurveyRequestDto.isSmoking(),
    roommateSurveyRequestDto.getEtc(),
    roommateSurveyRequestDto.getSleepTime(),
    roommateSurveyRequestDto.getWakeUpTime(),
    roommateSurveyRequestDto.getPhoneTime(),
    roommateSurveyRequestDto.getShowerTime()
);


        saveSurvey(survey);

        return convertSurveyToDto(user, survey);



    }
    private RoommateSurveyResponseDto convertSurveyToDto(User currentUser, RoommateSurvey survey) {
        User author = survey.getUser();
        UserSimpleResponseDto userSimpleResponseDto = userService.convertUserToSimpleDto(author, author);
        boolean isMine = currentUser.getUsername().equals(author.getUsername());

       return new RoommateSurveyResponseDto(
    survey.getId(),
    userSimpleResponseDto,
    survey.getDormName(),
    survey.getCleanLevel(),
    survey.isSmoking(),
    survey.getEtc(),
    survey.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm")),
    isMine,
    survey.getSleepTime(),
    survey.getWakeUpTime(),
    survey.getPhoneTime(),
    survey.getShowerTime()
);

    }

    public List<RoommateSurveyResponseDto> getAllList(User currentUser){
        List<RoommateSurvey> list = roommateSurveyRepository.findAll();
        return list.stream()
                .map(roommateSurvey -> convertSurveyToDto(currentUser, roommateSurvey))
                .toList();
    }

    /** 현재 로그인한 사용자의 설문만 반환 */
    public List<RoommateSurveyResponseDto> getMySurveyList(User currentUser){
        List<RoommateSurvey> mySurveys = roommateSurveyRepository.findByUser(currentUser);

        return mySurveys.stream()
                .map(survey -> convertSurveyToDto(currentUser, survey))
                .toList();


    }

    /** 매칭 결과의 List를 반환 */
    public List<MatchingResultResponseDto> getMatchingResult(long id, User currentUser){
        RoommateSurvey submittedSurvey = roommateSurveyRepository.findById(id);
        if (submittedSurvey == null) {
            throw new EntityNotFoundException("Survey not found with id: " + id);
        }
        //모든 설문조사의 list를 받되, 내 설문조사들은 제외해야함
        List<RoommateSurvey> surveysExcludingCurrentUser = roommateSurveyRepository
                .findByUserIdNot(currentUser.getId());

        List<MatchingResultResponseDto> matchingResults = surveysExcludingCurrentUser.stream()
                .map(survey -> {
                    User author = survey.getUser();
                    UserSimpleResponseDto userSimpleResponseDto = userService.convertUserToSimpleDto(author, author);
                    double matchingRate = calculateMatchingRate(submittedSurvey, survey);
                    return new MatchingResultResponseDto(
                            survey.getId(),
                            userSimpleResponseDto,
                            survey.getDormName(),
                            survey.getCleanLevel(),
                            survey.isSmoking(),
                            survey.getEtc(),
                            survey.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm")),
                            matchingRate
                    );
                })
                .sorted(Comparator.comparing(MatchingResultResponseDto::getMatchingRate).reversed()) // 일치율 높은 순으로 정렬
                .collect(Collectors.toList());

        return matchingResults;

    }
    /**
     * 두 설문조사 간의 일치율을 계산합니다.
     * etc와 createdAt은 일치율 계산에 포함되지 않습니다.
     *
     * @param survey1 기준 설문조사 (자신의 설문조사)
     * @param survey2 비교 대상 설문조사
     * @return 일치율 (0.0 ~ 100.0)
     */
    /**
     * update방안 : 나한테는 ‘청결도’가 훨씬 중요한데 ‘흡연 여부’는 별로 신경 쓰지 않는다” 같은 상황을 반영할 수 있도록 수정
     * 각 항목에 중요도 1(전혀 중요x) ~ 5(매우 중요함)을 매길 수 있도록 하여,
     *
     * 또한, cleanLevel같이 순서형(1회~5회)의 경우, 비교대상이 3회면, 기준설문조사가 1회면, 1 - (abs(3-1))/ (5 - 1) 등으로, 비교대상과 기준 설문조사 간격이 좁을수록 높은 점수를 부여하도록
     * 흡연여부는 일치시 1, 불일치시 0
     * 기숙사명은 동일시에만 점수부여
     * (각 점수에 각 중요도가중치 (1~5)를 곱해서 더함 / 총 중요도가중치합) * 100 = 최종 유사도 점수
     * @param survey1
     * @param survey2
     * @return
     */
    private double calculateMatchingRate(RoommateSurvey survey1, RoommateSurvey survey2) {
        int totalCriteria = 3; // dormName, cleanLevel, smoking 3가지 기준
        int matchCount = 0;

        // 기숙사 이름 비교
        if (survey1.getDormName().equals(survey2.getDormName())) {
            matchCount++;
        }

        // 청결도 비교
        if (survey1.getCleanLevel().equals(survey2.getCleanLevel())) {
            matchCount++;
        }

        // 흡연 여부 비교
        if (survey1.isSmoking() == survey2.isSmoking()) {
            matchCount++;
        }

        // 일치율 계산 (백분율)
        return (double) matchCount / totalCriteria * 100.0;
    }
    //가중치를 반영한 매칭 점수 계산
    //고려할점 -> 각자 지정한 가중치가 다르기 때문에 내 기준 상대방의 점수와, 상대방 기준 내 점수가 다름을 유의
    private double calculateWeightedMatchScore(RoommateSurvey survey1, RoommateSurvey survey2) {
        //각 항목별 가중치는 1~5의 정수값

        // 1) 내 가중치 총합
        // int totalWeight = survey1.getSmokeWeight() + survey1.getDormWeight() + survey1.getCleanLevelWeight();
        int matchCount = 0;

        // 2) 각 항목 유사도에 내 가중치 곱하기
        /*
        int weightedSum = 0;
        if (mine.getDormName().equals(other.getDormName())) { //기숙사의 경우 이름이 일치하면 점수
            weightedSum += mineWeights.getDormWeight();
        }
        //청소 횟수의 경우는 내가 선택한 청소횟수가 가까울수록 높은 점수
        weightedSum += mineWeights.getCleanWeight()
                * (1.0 - Math.abs(mine.getCleanLevel() - other.getCleanLevel()) / (double)(maxCleanLevel - 1));

        //흡연여부
        if (mine.isSmoking() == other.isSmoking()) {
            weightedSum += mineWeights.getSmokeWeight();
        }
        */


        // 3) 0~100 백분율 반환
        // return 100.0 * weightedSum / totalWeight;
        return 0.0;
    }

    public void deleteSurvey(long id, User requestUser) {
        RoommateSurvey survey = roommateSurveyRepository.findById(id);

        if (survey == null) {
            throw new EntityNotFoundException("Survey not found with id: " + id);
        }
        //만약 survey의 author가 user게 아니라면
        if(!survey.getUser().getUsername().equals(requestUser.getUsername())) {
            throw new InvalidAccessException();

        }

        roommateSurveyRepository.deleteById(id);


    }

}
