package com.leets.k_beauty.domain.survey.config;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;
import com.leets.k_beauty.domain.survey.repository.SurveyOptionRepository;
import com.leets.k_beauty.domain.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SurveyDataInitializer implements ApplicationRunner {

    private final SurveyRepository surveyRepository;
    private final SurveyOptionRepository surveyOptionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (surveyRepository.count() > 0) {
            return;
        }

        Survey concern = surveyRepository.save(Survey.create(
                QuestionCode.CONCERN, "현재 가장 고민되는 피부 고민을 선택해주세요.",
                SelectionType.SINGLE, 1, 1, 1, true, true));
        saveOption(concern, "MOISTURE", "수분 부족", null, 1, false, null);
        saveOption(concern, "TONE", "피부톤 불균형·잡티·칙칙함", null, 2, false, null);
        saveOption(concern, "SENSITIVE", "예민함", null, 3, false, null);
        saveOption(concern, "AGING", "처짐·주름·탄력", null, 4, false, null);
        saveOption(concern, "TROUBLE", "트러블·모공", null, 5, false, null);

        Survey skinType = surveyRepository.save(Survey.create(
                QuestionCode.SKIN_TYPE, "평소 본인의 피부 타입에 가까운 것을 선택해주세요.",
                SelectionType.SINGLE, 1, 2, 1, true, true));
        saveOption(skinType, "DRY", "건성", "세안 후 얼굴 전체가 당겨요.", 1, false, null);
        saveOption(skinType, "OILY", "지성", "금방 번들거리고 유분이 많아요.", 2, false, null);
        saveOption(skinType, "COMBINATION", "복합성", "T존은 번들거리지만 볼은 건조해요.", 3, false, null);
        saveOption(skinType, "DEHYDRATED_OILY", "수부지", "겉은 번들거리는데 속은 당겨요.", 4, false, null);
        saveOption(skinType, "UNKNOWN", "잘 모르겠어요", null, 5, false, null);

        Survey sensitivity = surveyRepository.save(Survey.create(
                QuestionCode.SENSITIVITY, "새로운 제품 사용 시 예민해지나요?",
                SelectionType.SINGLE, 1, 3, 1, true, true));
        saveOption(sensitivity, "SENSITIVE_YES", "예", null, 1, false, QuestionCode.CAUTION);
        saveOption(sensitivity, "SENSITIVE_NO", "아니요", null, 2, false, QuestionCode.EXPLORATION_HABIT);

        Survey caution = surveyRepository.save(Survey.create(
                QuestionCode.CAUTION, "어떤 제품이 불편했나요?",
                SelectionType.MULTIPLE, 4, 4, 1, true, true));
        saveOption(caution, "FRAGRANCE", "향이 강한 제품", null, 1, false, null);
        saveOption(caution, "ALCOHOL", "알코올 냄새가 강하거나 따가웠던 제품", null, 2, false, null);
        saveOption(caution, "OILY_TEXTURE", "오일감이 많은 제품", null, 3, false, null);
        saveOption(caution, "EXFOLIATION", "각질 케어 제품(AHA·BHA)", null, 4, false, null);
        saveOption(caution, "UNKNOWN", "잘 모르겠어요", null, 5, true, null);

        Survey explorationHabit = surveyRepository.save(Survey.create(
                QuestionCode.EXPLORATION_HABIT, "성분이나 리뷰를 찾아보나요?",
                SelectionType.SINGLE, 1, 5, 1, true, true));
        saveOption(explorationHabit, "FREQUENTLY", "자주 찾아봐요", null, 1, false, null);
        saveOption(explorationHabit, "OCCASIONALLY", "가끔 봐요", null, 2, false, null);
        saveOption(explorationHabit, "RARELY", "거의 안 봐요", null, 3, false, null);
    }

    private void saveOption(Survey survey, String optionCode, String optionText, String guideText,
                             int displayOrder, boolean exclusive, QuestionCode nextQuestionCode) {
        surveyOptionRepository.save(SurveyOption.create(
                survey, optionCode, optionText, guideText, displayOrder, exclusive, true, nextQuestionCode));
    }
}
