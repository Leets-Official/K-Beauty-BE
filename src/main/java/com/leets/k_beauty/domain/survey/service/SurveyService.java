package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.common.enums.CautionCategory;
import com.leets.k_beauty.domain.common.enums.DiagnosisType;
import com.leets.k_beauty.domain.common.enums.SkinConcern;
import com.leets.k_beauty.domain.common.enums.SkinType;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.*;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.entity.UserSurveyAnswer;
import com.leets.k_beauty.domain.survey.enums.*;
import com.leets.k_beauty.domain.survey.repository.SurveyOptionRepository;
import com.leets.k_beauty.domain.survey.repository.SurveyRepository;
import com.leets.k_beauty.domain.survey.repository.UserConditionRepository;
import com.leets.k_beauty.domain.survey.repository.UserSurveyAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private static final String SENSITIVE_YES = "SENSITIVE_YES";
    private static final String SENSITIVE_NO = "SENSITIVE_NO";
    private static final String SKIN_TYPE_UNKNOWN = "UNKNOWN";

    private final SurveyRepository surveyRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserSurveyAnswerRepository userSurveyAnswerRepository;
    private final SessionRepository sessionRepository;

    // 새 설문 생성
    @Transactional
    public UserConditionResponse create(String sessionToken) {
        Long sessionId = resolveSessionId(sessionToken);
        // ABANDONED가 아닌(=IN_PROGRESS 또는 COMPLETED) 컨텍스트가 남아있으면 새로 만들지 않는다.
        // COMPLETED 상태를 그냥 통과시키면 restart를 거치지 않고도 완료 설문 위에 새 설문이 쌓여
        // 한 세션에 여러 개의 "활성" 컨텍스트가 공존하는 상태 오염이 발생.
        userConditionRepository.findFirstBySessionIdOrderByIdDesc(sessionId)
                .filter(uc -> uc.getStatus() != SurveyStatus.ABANDONED)
                .ifPresent(uc -> {
                    throw new BusinessException(ErrorCode.SURVEY_IN_PROGRESS_EXISTS);
                });

        UserCondition userCondition = UserCondition.start(sessionId);
        userConditionRepository.save(userCondition);
        return UserConditionResponse.from(userCondition);
    }

    // 세션의 가장 최근 설문 컨텍스트 조회 (재진입 라우팅용: IN_PROGRESS→이어하기, COMPLETED→결과 화면, 없음→온보딩)
    public UserConditionResponse getCurrent(String sessionToken) {
        Long sessionId = resolveSessionId(sessionToken);
        UserCondition userCondition = userConditionRepository.findFirstBySessionIdOrderByIdDesc(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_NOT_FOUND));
        return UserConditionResponse.from(userCondition);
    }

    // 설문 진행 정보 및 답변 이력 조회
    public SurveyProgressResponse getProgress(Long surveyId, String sessionToken) {
        UserCondition userCondition = getOwnedCondition(surveyId, sessionToken);
        return buildProgress(userCondition);
    }

    // 설문 질문 및 선택지 조회
    public SurveyQuestionResponse getQuestion(QuestionCode questionCode) {
        Survey survey = findActiveSurvey(questionCode);
        List<SurveyOption> options = surveyOptionRepository.findOptions(survey);
        return SurveyQuestionResponse.from(survey, options);
    }

    // 이전 질문 조회 (뒤로 가기 시 프리필용, from은 클라이언트가 현재 보고 있는 질문)
    public PreviousQuestionResponse getPreviousQuestion(Long surveyId, QuestionCode from, String sessionToken) {
        UserCondition userCondition = getOwnedCondition(surveyId, sessionToken);
        List<QuestionCode> answeredOrder = answeredQuestionCodesInFlowOrder(userCondition);

        int index = (from != null) ? answeredOrder.indexOf(from) : -1;

        QuestionCode previousCode;
        if (index > 0) {
            previousCode = answeredOrder.get(index - 1);
        } else if (index == -1 && !answeredOrder.isEmpty()) {
            previousCode = answeredOrder.get(answeredOrder.size() - 1);
        } else {
            // 첫 질문에서 뒤로 간 경우. 정상 동선이므로 오류 대신 온보딩으로 가라고 알려준다
            return PreviousQuestionResponse.onboarding();
        }

        Survey survey = findActiveSurvey(previousCode);
        List<SurveyOption> options = surveyOptionRepository.findOptions(survey);
        return PreviousQuestionResponse.of(survey, options, groupAnswers(userCondition).get(previousCode));
    }

    // 설문 처음부터 다시 시작
    @Transactional
    public RestartResponse restart(Long surveyId, String sessionToken) {
        Session session = resolveSession(sessionToken);
        UserCondition previous = getOwnedCondition(surveyId, session.getId());
        previous.abandon();
        session.resetCondition();

        UserCondition next = UserCondition.start(previous.getSessionId());
        userConditionRepository.save(next);

        Survey first = surveyRepository.findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_QUESTION_NOT_FOUND));

        return new RestartResponse(
                previous.getId(), next.getId(), next.getSessionId(),
                next.getStatus(), first.getQuestionCode(), first.getSurveyStep()
        );
    }

    // 답변된 질문을 설문 흐름 순서(surveyStep)대로 정렬
    private List<QuestionCode> answeredQuestionCodesInFlowOrder(UserCondition userCondition) {
        Map<QuestionCode, List<String>> grouped = groupAnswers(userCondition);
        Map<QuestionCode, Integer> steps = surveyStepsOf(grouped.keySet());
        return grouped.keySet().stream()
                .sorted(Comparator.comparing(steps::get))
                .toList();
    }

    // 설문 답변 저장 및 변경
    @Transactional
    public AnswerSaveResponse saveAnswer(Long surveyId, QuestionCode questionCode, AnswerSaveRequest request, String sessionToken) {
        Session session = resolveSession(sessionToken);
        UserCondition userCondition = getOwnedCondition(surveyId, session.getId());
        Survey survey = findActiveSurvey(questionCode);
        if (questionCode == QuestionCode.CAUTION && !isSensitiveYesAnswered(userCondition)) {
            throw new BusinessException(ErrorCode.CAUTION_PREREQUISITE_MISSING);
        }

        validateSelectionCount(survey, request.optionCodes());
        List<SurveyOption> selectedOptions = surveyOptionRepository.findByCodes(survey, request.optionCodes());
        if (selectedOptions.size() != request.optionCodes().size()) {
            throw new BusinessException(ErrorCode.SURVEY_OPTION_INVALID);
        }
        validateExclusive(selectedOptions);

        // "다음" 버튼은 답변을 바꿨는지와 상관없이 항상 이 API를 호출.
        // 값이 그대로면 저장할 필요가 없으므로 지우고 다시 넣지 않음.
        // 불필요하게 행이 새로 만들어져 저장 시각과 순서가 바뀌는 것을 막음.
        if (isSameAsSaved(userCondition, survey, request.optionCodes())) {
            return unchangedResponse(userCondition, survey, questionCode, selectedOptions, request.optionCodes());
        }

        if (questionCode == QuestionCode.SKIN_TYPE) {
            boolean isTypeNeutral = selectedOptions.get(0).getOptionCode().equals(SKIN_TYPE_UNKNOWN);
            userCondition.updateTypeNeutralMode(isTypeNeutral);
            session.updateSkinType(SkinType.valueOf(selectedOptions.get(0).getOptionCode()));
        }

        if (questionCode == QuestionCode.CONCERN) {
            session.updateSkinConcern(SkinConcern.valueOf(selectedOptions.get(0).getOptionCode()));
        }

        List<QuestionCode> clearedQuestionCodes = new ArrayList<>();
        if (questionCode == QuestionCode.SENSITIVITY) {
            boolean wasYes = hasAnswerWithCode(userCondition, survey, SENSITIVE_YES);
            boolean nowNo = selectedOptions.get(0).getOptionCode().equals(SENSITIVE_NO);
            if (wasYes && nowNo) {
                Survey cautionSurvey = findActiveSurvey(QuestionCode.CAUTION);
                userSurveyAnswerRepository.deleteByConditionAndSurvey(userCondition, cautionSurvey);
                clearedQuestionCodes.add(QuestionCode.CAUTION);
                session.updateCautionCategories(List.of());
            }
        }

        userSurveyAnswerRepository.deleteByConditionAndSurvey(userCondition, survey);
        selectedOptions.forEach(option -> userSurveyAnswerRepository.save(UserSurveyAnswer.of(userCondition, option)));

        SensitivityStatus derivedSensitivityStatus = null;
        if (questionCode == QuestionCode.SENSITIVITY) {
            boolean isNo = selectedOptions.get(0).getOptionCode().equals(SENSITIVE_NO);
            if (isNo) {
                derivedSensitivityStatus = SensitivityStatus.LOW;
            } else {
                // "예"를 선택한 시점엔 아직 CAUTION 응답 여부를 모른다.
                // 이미 CAUTION 응답이 있으면(재저장 케이스) 그 값으로 재계산하고,
                // 없으면 "민감도 정보를 아직 확인하지 않은" UNASSESSED로 둠
                List<SurveyOption> existingCaution = userSurveyAnswerRepository
                        .findByConditionAndSurvey(userCondition, findActiveSurvey(QuestionCode.CAUTION))
                        .stream().map(UserSurveyAnswer::getOption).toList();
                derivedSensitivityStatus = existingCaution.isEmpty()
                        ? SensitivityStatus.UNASSESSED
                        : deriveFromCaution(existingCaution);
            }
            userCondition.updateSensitivityStatus(derivedSensitivityStatus);
            session.updateSensitivityStatus(
                    com.leets.k_beauty.domain.common.enums.SensitivityStatus.valueOf(derivedSensitivityStatus.name()));
        } else if (questionCode == QuestionCode.CAUTION) {
            derivedSensitivityStatus = deriveFromCaution(selectedOptions);
            userCondition.updateSensitivityStatus(derivedSensitivityStatus);
            session.updateSensitivityStatus(
                    com.leets.k_beauty.domain.common.enums.SensitivityStatus.valueOf(derivedSensitivityStatus.name()));
            List<CautionCategory> cautions = selectedOptions.stream()
                    .map(opt -> toCautionCategory(opt.getOptionCode()))
                    .filter(c -> c != CautionCategory.UNKNOWN)
                    .toList();
            session.updateCautionCategories(cautions);
        }

        QuestionCode nextQuestionCode = determineNextQuestionCode(survey, selectedOptions);
        NextAction nextAction = determineNextAction(questionCode, nextQuestionCode);
        RecommendationImpact impact = questionCode == QuestionCode.EXPLORATION_HABIT
                ? RecommendationImpact.NONE
                : RecommendationImpact.RECALCULATE_REQUIRED;

        // 추천에 반영되는 답변이 바뀌었으면 완료 상태를 풀어 다시 완료를 거치도록 설정
        // 탐색 습관은 추천 결과를 바꾸지 않으므로(기획 9.2) 완료 상태를 유지
        if (impact == RecommendationImpact.RECALCULATE_REQUIRED) {
            userCondition.reopenIfCompleted();
        }

        return new AnswerSaveResponse(
                userCondition.getId(), questionCode, request.optionCodes(), clearedQuestionCodes,
                derivedSensitivityStatus, nextAction, nextQuestionCode, impact
        );
    }

    // 진단 경로(QUICK/DETAILED) 저장
    @Transactional
    public DiagnosisModeResponse setDiagnosisMode(Long surveyId, DiagnosisModeRequest request, String sessionToken) {
        Session session = resolveSession(sessionToken);
        UserCondition userCondition = getOwnedCondition(surveyId, session.getId());
        DiagnosisMode mode = parseDiagnosisMode(request.diagnosisMode());

        List<QuestionCode> missing = missingRequiredAnswers(userCondition, List.of(QuestionCode.CONCERN, QuestionCode.SKIN_TYPE));
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_MODE_PREREQUISITE_MISSING);
        }

        // 추천 화면에서 뒤로 와 경로를 실제로 바꾼 경우에만 완료 상태를 푼다.
        // 같은 경로를 다시 고른 것(이동만 한 경우)은 완료 상태를 유지한다.
        if (mode != userCondition.getDiagnosisMode()) {
            userCondition.reopenIfCompleted();
        }

        userCondition.updateDiagnosisMode(mode);
        // 진단 경로가 바뀌면 민감도 상태도 다시 계산한다.
        // 원본 답변은 지우지 않고(프리필 유지) 파생값만 현재 경로에 맞춘다.
        SensitivityStatus newSensitivityStatus = mode == DiagnosisMode.QUICK
                ? SensitivityStatus.UNASSESSED
                : deriveSensitivityStatus(userCondition);
        userCondition.updateSensitivityStatus(newSensitivityStatus);
        session.updateDiagnosisType(DiagnosisType.valueOf(mode.name()));
        session.updateSensitivityStatus(
                com.leets.k_beauty.domain.common.enums.SensitivityStatus.valueOf(newSensitivityStatus.name()));
        // 상세 진단이면 다음 질문으로, 빠른 진단이면 바로 완료 단계
        // 다음 질문은 시드 데이터에서 찾으므로 설문 구성이 바뀌어도 따라감.
        QuestionCode nextQuestionCode = (mode == DiagnosisMode.DETAILED)
                ? surveyRepository.findNext(findActiveSurvey(QuestionCode.SKIN_TYPE).getSurveyStep())
                        .map(Survey::getQuestionCode).orElse(null)
                : null;
        NextAction nextAction = (nextQuestionCode != null)
                ? NextAction.ANSWER_QUESTION
                : NextAction.READY_TO_COMPLETE;

        return DiagnosisModeResponse.of(userCondition, nextAction, nextQuestionCode);
    }

    // 설문 완료 처리
    @Transactional
    public CompletionResponse complete(Long surveyId, String sessionToken) {
        UserCondition userCondition = getOwnedCondition(surveyId, sessionToken);

        // 이미 완료된 설문을 다시 완료해도 오류로 보지 않고, 답을 바꿨다면 저장 시점에 진행 중으로 되돌아가므로 여기까지 오지 않음
        if (userCondition.getStatus() == SurveyStatus.COMPLETED) {
            return CompletionResponse.from(userCondition);
        }

        List<QuestionCode> missing = missingRequiredAnswers(userCondition, requiredQuestionCodes(userCondition));
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.SURVEY_ANSWER_MISSING);
        }

        userCondition.complete();
        return CompletionResponse.from(userCondition);
    }

    // ---- private helpers ----

    // 세션 토큰으로 세션 조회
    private Session resolveSession(String sessionToken) {
        return sessionRepository.findBySessionTokenHash(Session.hashToken(sessionToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    // 세션 토큰으로 세션 ID 조회
    private Long resolveSessionId(String sessionToken) {
        return resolveSession(sessionToken).getId();
    }

    // 설문 소유권(세션 일치) 확인 후 조회 - sessionId 기반
    private UserCondition getOwnedCondition(Long surveyId, Long sessionId) {
        UserCondition userCondition = userConditionRepository.findById(surveyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_NOT_FOUND));
        if (!userCondition.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.SURVEY_FORBIDDEN);
        }
        return userCondition;
    }

    // 설문 소유권(세션 일치) 확인 후 조회 - sessionToken 기반
    private UserCondition getOwnedCondition(Long surveyId, String sessionToken) {
        return getOwnedCondition(surveyId, resolveSessionId(sessionToken));
    }

    // 활성화된 질문 조회
    private Survey findActiveSurvey(QuestionCode questionCode) {
        return surveyRepository.findActive(questionCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_QUESTION_NOT_FOUND));
    }

    // 선택 개수 검증
    private void validateSelectionCount(Survey survey, List<String> optionCodes) {
        if (survey.getSelectionType() == SelectionType.SINGLE && optionCodes.size() != 1) {
            throw new BusinessException(ErrorCode.SINGLE_SELECTION_VIOLATION);
        }
        if (survey.getSelectionType() == SelectionType.MULTIPLE
                && (optionCodes.isEmpty() || optionCodes.size() > survey.getMaxSelections())) {
            throw new BusinessException(ErrorCode.MAX_SELECTION_EXCEEDED);
        }
    }

    // 배타 선택지 검증
    private void validateExclusive(List<SurveyOption> selectedOptions) {
        if (selectedOptions.size() <= 1) {
            return;
        }
        selectedOptions.stream()
                .filter(SurveyOption::isExclusive)
                .findFirst()
                .ifPresent(option -> {
                    throw new BusinessException(ErrorCode.EXCLUSIVE_OPTION_VIOLATION);
                });
    }

    // 이미 저장된 답변과 요청한 답변이 같은지 확인 
    private boolean isSameAsSaved(UserCondition userCondition, Survey survey, List<String> optionCodes) {
        List<String> savedOptionCodes = userSurveyAnswerRepository
                .findByConditionAndSurvey(userCondition, survey).stream()
                .map(answer -> answer.getOption().getOptionCode())
                .toList();
        return !savedOptionCodes.isEmpty()
                && new HashSet<>(savedOptionCodes).equals(new HashSet<>(optionCodes));
    }

    // 저장할 내용이 없을 때의 응답.
    // 값이 그대로이므로 삭제한 질문도 없고 추천 재계산도 필요 없음.
    private AnswerSaveResponse unchangedResponse(UserCondition userCondition, Survey survey, QuestionCode questionCode,
                                                  List<SurveyOption> selectedOptions, List<String> optionCodes) {
        QuestionCode nextQuestionCode = determineNextQuestionCode(survey, selectedOptions);
        SensitivityStatus derivedSensitivityStatus =
                (questionCode == QuestionCode.SENSITIVITY || questionCode == QuestionCode.CAUTION)
                        ? userCondition.getSensitivityStatus()
                        : null;

        return new AnswerSaveResponse(
                userCondition.getId(), questionCode, optionCodes, List.of(),
                derivedSensitivityStatus,
                determineNextAction(questionCode, nextQuestionCode), nextQuestionCode,
                RecommendationImpact.NONE
        );
    }

    // CAUTION 선택지 개수 기준 민감도 산출 (1개=MEDIUM, 2개 이상=HIGH)
    private SensitivityStatus deriveFromCaution(List<SurveyOption> cautionOptions) {
        return cautionOptions.size() >= 2 ? SensitivityStatus.HIGH : SensitivityStatus.MEDIUM;
    }

    // 저장된 답변만으로 민감도 상태를 산출한다.
    // 진단 경로(diagnosisMode)는 보지 않는 순수 조회 함수
    private SensitivityStatus deriveSensitivityStatus(UserCondition userCondition) {
        List<UserSurveyAnswer> sensitivityAnswers = userSurveyAnswerRepository
                .findByConditionAndSurvey(userCondition, findActiveSurvey(QuestionCode.SENSITIVITY));

        if (sensitivityAnswers.isEmpty()) {
            return SensitivityStatus.UNASSESSED;                 // 민감 여부 미응답
        }
        if (sensitivityAnswers.get(0).getOption().getOptionCode().equals(SENSITIVE_NO)) {
            return SensitivityStatus.LOW;                        // 아니요
        }

        List<SurveyOption> cautionOptions = userSurveyAnswerRepository
                .findByConditionAndSurvey(userCondition, findActiveSurvey(QuestionCode.CAUTION))
                .stream().map(UserSurveyAnswer::getOption).toList();

        return cautionOptions.isEmpty()
                ? SensitivityStatus.UNASSESSED                   // 예 + 주의 요소 미응답
                : deriveFromCaution(cautionOptions);             // 예 + 주의 요소 응답
    }

    // 특정 선택지 응답 여부 확인
    private boolean hasAnswerWithCode(UserCondition userCondition, Survey survey, String optionCode) {
        return userSurveyAnswerRepository.findByConditionAndSurvey(userCondition, survey).stream()
                .anyMatch(answer -> answer.getOption().getOptionCode().equals(optionCode));
    }

    // 질문 응답 여부 확인
    private boolean hasAnyAnswer(UserCondition userCondition, QuestionCode questionCode) {
        Survey survey = findActiveSurvey(questionCode);
        return !userSurveyAnswerRepository.findByConditionAndSurvey(userCondition, survey).isEmpty();
    }

    // 다음 질문 코드 결정
    private QuestionCode determineNextQuestionCode(Survey survey, List<SurveyOption> selectedOptions) {
        if (survey.getQuestionCode() == QuestionCode.SKIN_TYPE) {
            return null;
        }
        QuestionCode branched = selectedOptions.stream()
                .map(SurveyOption::getNextQuestionCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (branched != null) {
            return branched;
        }
        return surveyRepository.findNext(survey.getSurveyStep())
                .map(Survey::getQuestionCode)
                .orElse(null);
    }

    // 다음 행동(nextAction) 결정
    private NextAction determineNextAction(QuestionCode questionCode, QuestionCode nextQuestionCode) {
        if (nextQuestionCode != null) {
            return NextAction.ANSWER_QUESTION;
        }
        return questionCode == QuestionCode.SKIN_TYPE ? NextAction.SELECT_DIAGNOSIS_MODE : NextAction.READY_TO_COMPLETE;
    }

    // 진단 경로 문자열 파싱
    private DiagnosisMode parseDiagnosisMode(String raw) {
        try {
            return DiagnosisMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_DIAGNOSIS_MODE);
        }
    }

    // 완료에 필요한 질문 목록 계산
    private List<QuestionCode> requiredQuestionCodes(UserCondition userCondition) {
        if (userCondition.getDiagnosisMode() == DiagnosisMode.QUICK) {
            return List.of(QuestionCode.CONCERN, QuestionCode.SKIN_TYPE);
        }
        List<QuestionCode> required = new ArrayList<>();
        required.add(QuestionCode.CONCERN);
        required.add(QuestionCode.SKIN_TYPE);
        required.add(QuestionCode.SENSITIVITY);
        if (isSensitiveYesAnswered(userCondition)) {
            required.add(QuestionCode.CAUTION);
        }
        required.add(QuestionCode.EXPLORATION_HABIT);
        return required;
    }

    // 민감도 질문에 '예' 응답했는지 확인
    private boolean isSensitiveYesAnswered(UserCondition userCondition) {
        Survey sensitivitySurvey = findActiveSurvey(QuestionCode.SENSITIVITY);
        return hasAnswerWithCode(userCondition, sensitivitySurvey, SENSITIVE_YES);
    }

    // 누락된 필수 응답 조회
    private List<QuestionCode> missingRequiredAnswers(UserCondition userCondition, List<QuestionCode> requiredCodes) {
        return requiredCodes.stream()
                .filter(code -> !hasAnyAnswer(userCondition, code))
                .toList();
    }

    // 질문코드 -> surveyStep 매핑.
    // 정렬할 때마다 질문을 다시 조회하지 않도록 미리 만들어 둠.
    private Map<QuestionCode, Integer> surveyStepsOf(Collection<QuestionCode> questionCodes) {
        Map<QuestionCode, Integer> steps = new HashMap<>();
        questionCodes.forEach(code -> steps.put(code, findActiveSurvey(code).getSurveyStep()));
        return steps;
    }

    // 질문별 답변 코드 그룹핑 (답변된 순서 유지)
    private Map<QuestionCode, List<String>> groupAnswers(UserCondition userCondition) {
        List<UserSurveyAnswer> answers = userSurveyAnswerRepository.findAllByCondition(userCondition);
        Map<QuestionCode, List<String>> grouped = new LinkedHashMap<>();
        for (UserSurveyAnswer answer : answers) {
            QuestionCode code = answer.getOption().getSurvey().getQuestionCode();
            grouped.computeIfAbsent(code, key -> new ArrayList<>()).add(answer.getOption().getOptionCode());
        }
        return grouped;
    }

    // 답변 이력 기반 진행 상태(현재 질문·다음 행동) 조합
    private SurveyProgressResponse buildProgress(UserCondition userCondition) {
        Map<QuestionCode, List<String>> grouped = groupAnswers(userCondition);

        // 답변을 다시 저장하면 저장 순서가 바뀌므로, 화면에 보이는 순서(surveyStep)로 맞춰서 내려줌.
        Map<QuestionCode, Integer> steps = surveyStepsOf(grouped.keySet());
        List<SurveyProgressResponse.AnsweredQuestion> answeredQuestions = grouped.keySet().stream()
                .sorted(Comparator.comparing(steps::get))
                .map(code -> new SurveyProgressResponse.AnsweredQuestion(code, grouped.get(code)))
                .toList();

        QuestionCode currentQuestionCode = null;
        Integer currentStep = null;
        NextAction nextAction;

        if (userCondition.getDiagnosisMode() == null) {
            if (grouped.containsKey(QuestionCode.SKIN_TYPE)) {
                nextAction = NextAction.SELECT_DIAGNOSIS_MODE;
            } else {
                Survey next = grouped.containsKey(QuestionCode.CONCERN)
                        ? findActiveSurvey(QuestionCode.SKIN_TYPE)
                        : findActiveSurvey(QuestionCode.CONCERN);
                currentQuestionCode = next.getQuestionCode();
                currentStep = next.getSurveyStep();
                nextAction = NextAction.ANSWER_QUESTION;
            }
        } else {
            List<QuestionCode> missing = missingRequiredAnswers(userCondition, requiredQuestionCodes(userCondition));
            if (missing.isEmpty()) {
                nextAction = NextAction.READY_TO_COMPLETE;
            } else {
                Survey next = findActiveSurvey(missing.get(0));
                currentQuestionCode = next.getQuestionCode();
                currentStep = next.getSurveyStep();
                nextAction = NextAction.ANSWER_QUESTION;
            }
        }

        return new SurveyProgressResponse(
                userCondition.getId(),
                userCondition.getStatus(),
                userCondition.getDiagnosisMode(),
                userCondition.getSensitivityStatus(),
                userCondition.isTypeNeutralMode(),
                answeredQuestions,
                currentQuestionCode,
                currentStep,
                nextAction
        );
    }

    // CAUTION 선택지 코드 → CautionCategory 변환
    private CautionCategory toCautionCategory(String optionCode) {
        return switch (optionCode) {
            case "FRAGRANCE" -> CautionCategory.FRAGRANCE;
            case "ALCOHOL" -> CautionCategory.ALCOHOL;
            case "OILY_TEXTURE" -> CautionCategory.OIL;
            case "EXFOLIATION" -> CautionCategory.EXFOLIANT;
            default -> CautionCategory.UNKNOWN;
        };
    }
}
