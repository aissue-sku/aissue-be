/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.response.HotTopicResponse;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.TrendingKeyword;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.TrendingKeywordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotTopicService {

  private static final int MIN_WORD_LENGTH = 2;
  private static final double SURGE_THRESHOLD = 1.2;
  private static final int MAX_HOT_TOPICS = 10;
  private static final int MIN_OLDER_DATA_FOR_SURGE = 3;
  private static final int MIN_DOC_FREQ = 1;

  // 조사 — 긴 것 먼저 (최장 일치)
  private static final String[] PARTICLES = {
    "에서의", "으로의", "이라는", "라는", "이라고", "라고", "이라며", "라며", "에서야", "에게는", "에게서", "에서", "에겐", "에게",
    "부터는", "부터", "까지도", "까지", "이랑", "하고", "이며", "이고", "으로", "에야", "에는", "에도", "에만", "에서", "이나",
    "조차", "마저", "의", "을", "를", "이", "가", "은", "는", "에", "로", "와", "과", "도", "만"
  };

  private static final Set<Character> VERB_ENDINGS_2CHAR =
      Set.of('지', '게', '며', '서', '고', '나', '면', '든', '냐', '봬', '봐');

  // 3글자 이상 단어의 끝 2글자가 동사·형용사 어미인 경우 제거
  private static final Set<String> VERB_ENDINGS_SUFFIX =
      Set.of(
          "으면",
          "아서",
          "어서",
          "면서",
          "지만",
          "는데",
          "은데",
          "한다",
          "됩니",
          "입니",
          "했다",
          "됐다",
          "된다",
          "없어",
          "있어",
          "이야",
          "이에",
          "이라",
          "거나",
          // 형용사 관형형 어미 (-은/-ㄴ 계열)
          "려운",
          "로운",
          "러운",
          "스런",
          "다운",
          "거운",
          "두운",
          "스러",
          // 동사 의무·이유형
          "해야",
          "하여",
          "해서",
          "하며");

  // 문장 종결형 / 피동·형용사형 마지막 글자 (길이 3 이상)
  private static final Set<Character> SENTENCE_FINAL_CHARS = Set.of('까', '죠', '요', '된', '진');

  private static final Set<String> STOP_WORDS =
      Set.of(
          // 동사/형용사 어간
          "있다",
          "없다",
          "하다",
          "이다",
          "되다",
          "같다",
          "위해",
          "통해",
          "대해",
          "따라",
          // 3글자 동사/형용사형
          "있는",
          "없는",
          "하는",
          "되는",
          "않는",
          "하며",
          "이며",
          "하고",
          "이고",
          // 시간 표현
          "이후",
          "이전",
          "오늘",
          "내일",
          "어제",
          "지난",
          "올해",
          "이번",
          "지금",
          "현재",
          "내달",
          "다음달",
          "지난달",
          "다음주",
          "지난주",
          "내주",
          "다음",
          // 보도 상투어
          "결과",
          "정도",
          "이상",
          "이하",
          "기준",
          "관련",
          "때문",
          "이유",
          "방법",
          "경우",
          "내용",
          "상황",
          "문제",
          "사건",
          "사실",
          "사람",
          "국가",
          "정부",
          "기자",
          "보도",
          "속보",
          "단독",
          "긴급",
          "종합",
          "업데이트",
          "전문",
          "전격",
          "발표",
          "각하",
          // 섹션 헤더
          "포토뉴스",
          "브리프",
          "팩트체크",
          "인터뷰",
          "칼럼",
          "사설",
          "오피니언",
          // 관형어/부사
          "모든",
          "각각",
          "다시",
          "새로",
          "매우",
          "정말",
          "가장",
          "더욱",
          "계속",
          "함께",
          // 불용 명사
          "낯선",
          "있었던",
          "사이",
          "우려",
          "논란",
          "격돌",
          "혐의",
          "기소",
          "구속",
          "입장",
          "예정",
          "진행",
          "시작",
          "완료",
          "확인",
          "제공",
          "운영",
          // 자극적·법률·사회면 상투어 (키워드로 부적합)
          "살해",
          "피해",
          "범죄",
          "폭행",
          "폭력",
          "징역",
          "벌금",
          "구금",
          "실형",
          "집유",
          "무죄",
          "유죄",
          "고소",
          "고발",
          "재판",
          "판결",
          "수사",
          "체포",
          "실종",
          "사망",
          "부상",
          "사고",
          "화재",
          "파문",
          "충격",
          "거짓",
          "거금",
          "수억",
          "수천",
          "억대",
          "열애",
          "이혼",
          "결별",
          "스캔들",
          "폭로",
          "저격",
          "비판",
          "반박",
          "해명",
          "사과",
          "부인",
          // 대명사/지시어
          "그들",
          "그것",
          "이것",
          "저것",
          "여기",
          "거기",
          "저기",
          "이런",
          "그런",
          "저런",
          "우리",
          "저희",
          "그녀",
          "그는",
          // 위치/방향
          "중에",
          "속에",
          "밖에",
          "위에",
          "아래",
          "앞에",
          "뒤에",
          // 수치·금융 표현 (키워드로 부적합)
          "저점",
          "고점",
          "대비",
          "수십개",
          "수백개",
          "수천개",
          "전액",
          "일부",
          "전후",
          "규모",
          "수준");

  private static final int SNAPSHOT_RETENTION_DAYS = 7;

  private final ContentRepository contentRepository;
  private final TrendingKeywordRepository trendingKeywordRepository;

  public List<HotTopicResponse> getHotTopics() {
    log.info("급상승 키워드 분석 요청 시작");

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneDayAgo = now.minusDays(1);
    LocalDateTime sevenDaysAgo = now.minusDays(7);

    List<Content> recentContents =
        dedup(
            contentRepository.findByCollectedAtAfterOrderByCollectedAtDesc(
                oneDayAgo, Pageable.unpaged()));
    List<Content> baselineContents =
        dedup(
            contentRepository.findByCollectedAtAfterOrderByCollectedAtDesc(
                sevenDaysAgo, Pageable.unpaged()));

    if (recentContents.isEmpty()) {
      log.info("급상승 키워드 분석 스킵 - 최근 수집 데이터 없음");
      return List.of();
    }

    Map<String, Long> recentDocFreq = documentFrequency(recentContents);

    int olderDataCount = Math.max(0, baselineContents.size() - recentContents.size());
    if (olderDataCount < MIN_OLDER_DATA_FOR_SURGE) {
      log.info(
          "과거 기준 데이터 부족 - 빈도순 반환 (olderCount: {}, recentCount: {})",
          olderDataCount,
          recentContents.size());
      return frequencyBasedResult(recentDocFreq, recentContents);
    }

    Map<String, Long> baselineDocFreq = documentFrequency(baselineContents);

    List<HotTopicResponse> surgedCandidates =
        recentDocFreq.entrySet().stream()
            .filter(e -> e.getValue() >= MIN_DOC_FREQ)
            .map(
                e -> {
                  String keyword = e.getKey();
                  long recentCount = e.getValue();
                  double dailyHourlyAvg =
                      Math.max(baselineDocFreq.getOrDefault(keyword, 0L), 1L) / 7.0; // 7일 기준 1일 평균
                  double surgeRatio = Math.round((recentCount / dailyHourlyAvg) * 10.0) / 10.0;
                  return buildResponse(keyword, recentCount, surgeRatio, recentContents);
                })
            .sorted(
                Comparator.comparingDouble(
                        (HotTopicResponse h) -> (double) h.getRecentCount() * h.getSurgeRatio())
                    .reversed())
            .collect(Collectors.toList());

    List<HotTopicResponse> hotTopics = deduplicateByTopic(surgedCandidates);
    log.info("급상승 키워드 분석 성공 - {}개", hotTopics.size());
    return hotTopics;
  }

  @Transactional
  public List<HotTopicResponse> saveSnapshot() {
    log.info("급상승 키워드 스냅샷 저장 시작");
    List<HotTopicResponse> hotTopics = getHotTopics();

    if (hotTopics.isEmpty()) {
      log.info("급상승 키워드 없음 - 스냅샷 저장 스킵");
      return hotTopics;
    }

    LocalDateTime snapshotAt = LocalDateTime.now();
    List<TrendingKeyword> entities =
        hotTopics.stream()
            .map(
                ht ->
                    TrendingKeyword.builder()
                        .keyword(ht.getKeyword())
                        .count(ht.getRecentCount())
                        .surgeRatio(ht.getSurgeRatio())
                        .snapshotAt(snapshotAt)
                        .build())
            .toList();

    trendingKeywordRepository.saveAll(entities);

    // 보관 기간 초과 데이터 정리
    trendingKeywordRepository.deleteBySnapshotAtBefore(
        snapshotAt.minusDays(SNAPSHOT_RETENTION_DAYS));

    log.info("급상승 키워드 스냅샷 저장 완료 - {}개", entities.size());
    return hotTopics;
  }

  private List<HotTopicResponse> frequencyBasedResult(
      Map<String, Long> docFreq, List<Content> contents) {
    List<HotTopicResponse> candidates =
        docFreq.entrySet().stream()
            .filter(e -> e.getValue() >= MIN_DOC_FREQ)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> buildResponse(e.getKey(), e.getValue(), 0.0, contents))
            .collect(Collectors.toList());
    return deduplicateByTopic(candidates);
  }

  // 바이그램 우선 선택 후, 단어가 겹치는 유니그램/바이그램은 중복 제거
  private List<HotTopicResponse> deduplicateByTopic(List<HotTopicResponse> sorted) {
    List<HotTopicResponse> ordered = new ArrayList<>();
    sorted.stream().filter(r -> r.getKeyword().contains(" ")).forEach(ordered::add);
    sorted.stream().filter(r -> !r.getKeyword().contains(" ")).forEach(ordered::add);

    Set<String> usedWords = new HashSet<>();
    List<HotTopicResponse> result = new ArrayList<>();

    for (HotTopicResponse r : ordered) {
      Set<String> words = new HashSet<>(Arrays.asList(r.getKeyword().split(" ")));
      if (words.stream().noneMatch(usedWords::contains)) {
        result.add(r);
        usedWords.addAll(words);
        if (result.size() >= MAX_HOT_TOPICS) break;
      }
    }
    return result;
  }

  private HotTopicResponse buildResponse(
      String keyword, long recentCount, double surgeRatio, List<Content> contents) {
    List<String> sampleTitles =
        contents.stream()
            .filter(c -> titleMatchesKeyword(c.getTitle(), keyword))
            .map(Content::getTitle)
            .distinct()
            .limit(3)
            .toList();

    return HotTopicResponse.builder()
        .keyword(keyword)
        .recentCount((int) recentCount)
        .surgeRatio(surgeRatio)
        .sampleTitles(sampleTitles)
        .build();
  }

  private boolean titleMatchesKeyword(String title, String keyword) {
    if (title == null) return false;
    if (title.contains(keyword)) return true;
    // 바이그램: 두 단어가 제목에 모두 포함되면 매칭
    if (keyword.contains(" ")) {
      return Arrays.stream(keyword.split(" ")).allMatch(title::contains);
    }
    return false;
  }

  private Map<String, Long> documentFrequency(List<Content> contents) {
    return contents.stream()
        .flatMap(
            c -> {
              List<String> all = extractTokens(c.getTitle());
              Set<String> candidates = new LinkedHashSet<>();

              // 유니그램: 한글 포함 또는 영문 대문자 브랜드명 (HYPE, AI 등)
              all.stream().filter(this::isKeywordCandidate).forEach(candidates::add);

              // 바이그램: 두 토큰 모두 키워드 후보인 경우 (복합 명사 추출)
              for (int i = 0; i < all.size() - 1; i++) {
                if (isKeywordCandidate(all.get(i)) && isKeywordCandidate(all.get(i + 1))) {
                  candidates.add(all.get(i) + " " + all.get(i + 1));
                }
              }

              return candidates.stream();
            })
        .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
  }

  /** 한글 포함 또는 영문 대문자 브랜드명 */
  private boolean isKeywordCandidate(String word) {
    return containsKorean(word) || isUppercaseBrand(word);
  }

  /** 영문 대문자로만 구성된 브랜드명 (HYPE, AI, GPT 등) */
  private boolean isUppercaseBrand(String word) {
    return word.length() >= 2 && word.chars().allMatch(c -> c >= 'A' && c <= 'Z');
  }

  /** 제목에서 명사형 토큰 추출 */
  private List<String> extractTokens(String title) {
    return Arrays.stream(title.split("[^가-힣A-Za-z0-9]+"))
        .filter(w -> w.length() >= MIN_WORD_LENGTH)
        .filter(w -> !Character.isDigit(w.charAt(0))) // 숫자 시작 토큰 제거 (1번, 5년, 2026년 등)
        .map(this::stripPlural)
        .filter(w -> w.length() >= MIN_WORD_LENGTH)
        .filter(w -> !isVerbForm(w))
        .map(this::stripParticles)
        .filter(w -> w.length() >= MIN_WORD_LENGTH)
        .filter(w -> !STOP_WORDS.contains(w))
        .collect(Collectors.toList());
  }

  private boolean containsKorean(String word) {
    return word.chars().anyMatch(c -> c >= '가' && c <= '힣');
  }

  // 복수형 접미사 "들" 제거: 기업들→기업, 사람들→사람
  private String stripPlural(String word) {
    if (word.endsWith("들") && word.length() > 2) {
      return word.substring(0, word.length() - 1);
    }
    return word;
  }

  // 2글자: 마지막 글자가 동사 어미 / 3글자 이상: 끝 2글자가 동사 어미 또는 문장 종결형
  private boolean isVerbForm(String word) {
    if (word.length() == 2) {
      return VERB_ENDINGS_2CHAR.contains(word.charAt(1));
    }
    if (word.length() >= 3) {
      if (VERB_ENDINGS_SUFFIX.contains(word.substring(word.length() - 2))) return true;
      return SENTENCE_FINAL_CHARS.contains(word.charAt(word.length() - 1));
    }
    return false;
  }

  private String stripParticles(String word) {
    for (String particle : PARTICLES) {
      if (word.endsWith(particle)) {
        int stemLen = word.length() - particle.length();
        if (stemLen >= MIN_WORD_LENGTH) {
          return word.substring(0, stemLen);
        }
        // 어간이 너무 짧으면 조사만 남은 토큰 → 빈 문자열 반환 후 필터
        return "";
      }
    }
    return word;
  }

  private List<Content> dedup(List<Content> contents) {
    return contents.stream()
        .collect(
            Collectors.collectingAndThen(
                Collectors.toMap(
                    c -> c.getUrl() != null ? c.getUrl() : c.getTitle(), c -> c, (a, b) -> a),
                map -> map.values().stream().toList()));
  }
}
