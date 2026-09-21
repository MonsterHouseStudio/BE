package com.monsterhouse.common.init;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.about.entity.AboutIntro;
import com.monsterhouse.content.about.entity.Crew;
import com.monsterhouse.content.about.repository.AboutIntroRepository;
import com.monsterhouse.content.about.repository.CrewRepository;
import com.monsterhouse.content.competition.entity.Competition;
import com.monsterhouse.content.competition.entity.Country;
import com.monsterhouse.content.competition.repository.CompetitionRepository;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostCategory;
import com.monsterhouse.content.post.repository.PostRepository;
import com.monsterhouse.content.stat.entity.HomeStat;
import com.monsterhouse.content.stat.repository.HomeStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 개발용 콘텐츠 시드 (시합 일정 · 미디어 글).
 *
 * 갤러리는 넣지 않습니다. 실제 이미지 파일이 있어야 의미가 있고,
 * 관리자 화면에서 업로드하는 흐름을 그대로 확인하는 편이 낫습니다.
 *
 * ★ 일부러 "일본어 번역이 없는 글"을 하나 넣어뒀습니다.
 *   기획서 §3.2 폴백 정책(요청 언어 번역이 없으면 목록에서 제외)이
 *   실제로 동작하는지 /ja/media 에서 눈으로 확인하기 위해서입니다.
 */
@Slf4j
@Component
@Profile("local")
@Order(110)
@RequiredArgsConstructor
public class DevContentInitializer implements ApplicationRunner {

    private final CompetitionRepository competitionRepository;
    private final PostRepository postRepository;
    private final HomeStatRepository homeStatRepository;
    private final CrewRepository crewRepository;
    private final AboutIntroRepository aboutIntroRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCompetitions();
        seedPosts();
        seedHomeStats();
        seedAbout();
    }

    /** 로컬은 Flyway 를 쓰지 않으므로 소개 페이지(인트로·크루) 기본값을 여기서 시드합니다(운영은 V7). */
    private void seedAbout() {
        if (aboutIntroRepository.count() == 0) {
            aboutIntroRepository.save(new AboutIntro(
                    "우리는 무대 뒤를 찍습니다",
                    "私たちはステージの裏側を撮ります",
                    "MONSTER HOUSE는 보디빌딩 선수와 센터를 위한 영상·사진 미디어입니다. 결과가 아니라 과정을, 포즈가 아니라 사람을 기록합니다.",
                    "MONSTER HOUSE はボディビル選手とジムのための映像・写真メディアです。結果ではなく過程を、ポーズではなく人を記録します。",
                    null, null, null));
        }
        if (crewRepository.count() == 0) {
            crewRepository.save(Crew.builder().nameKo("정재윤").nameJa("チョン・ジェユン")
                    .roleKo("디렉터 · 촬영").roleJa("ディレクター・撮影")
                    .bioKo("기록하는 사람. 무대보다 무대 뒤를 오래 본다.")
                    .bioJa("記録する人。ステージよりも舞台裏を長く見つめる。")
                    .active(true).sortOrder(0).build());
            crewRepository.save(Crew.builder().nameKo("크루 A").nameJa("クルー A")
                    .roleKo("편집 · 컬러").roleJa("編集・カラー")
                    .bioKo("숫자보다 톤을 먼저 맞춘다.").bioJa("数値よりトーンを先に合わせる。")
                    .active(true).sortOrder(1).build());
            crewRepository.save(Crew.builder().nameKo("크루 B").nameJa("クルー B")
                    .roleKo("통역 · 코디네이션").roleJa("通訳・コーディネート")
                    .bioKo("한국과 일본 사이를 오간다.").bioJa("韓国と日本の間を行き来する。")
                    .active(true).sortOrder(2).build());
        }
    }

    /** 로컬은 Flyway 를 쓰지 않으므로 홈 통계 기본 4개를 여기서 시드합니다(운영은 V6 마이그레이션). */
    private void seedHomeStats() {
        if (homeStatRepository.count() > 0) {
            return;
        }
        homeStatRepository.save(HomeStat.builder().valueNumber(480).suffix("+")
                .labelKo("누적 촬영").labelJa("累計撮影")
                .descKo("무대 뒤부터 결과물까지, 그동안 쌓아 올린 촬영의 기록입니다.")
                .descJa("舞台裏から仕上がりまで、積み重ねてきた撮影の記録です。")
                .active(true).sortOrder(0).build());
        homeStatRepository.save(HomeStat.builder().valueNumber(120).suffix("+")
                .labelKo("함께한 선수").labelJa("共に歩んだ選手")
                .descKo("한 무대를 위해 함께 준비한 선수들의 숫자입니다.")
                .descJa("一つの舞台のために共に準備した選手の数です。")
                .active(true).sortOrder(1).build());
        homeStatRepository.save(HomeStat.builder().valueNumber(4).suffix("")
                .labelKo("운영 연차").labelJa("運営年数")
                .descKo("현장에서 쌓아 온 시간이 곧 이해의 깊이가 됩니다.")
                .descJa("現場で積み重ねた時間が、そのまま理解の深さになります。")
                .active(true).sortOrder(2).build());
        homeStatRepository.save(HomeStat.builder().valueText("KR · JP")
                .labelKo("한국·일본").labelJa("韓国・日本")
                .descKo("한국과 일본, 두 무대를 잇는 촬영과 통역을 합니다.")
                .descJa("韓国と日本、二つの舞台をつなぐ撮影と通訳を行います。")
                .active(true).sortOrder(3).build());
    }

    private void seedCompetitions() {
        if (competitionRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        Competition a = Competition.builder()
                .country(Country.KR)
                .startDate(today.plusDays(12))
                .endDate(today.plusDays(13))
                .published(true)
                .build();
        a.putTranslation(LocaleCode.KO, "전국 보디빌딩 선수권대회",
                "국내 최대 규모 아마추어 대회. 체급별 예선과 결선이 이틀에 걸쳐 진행됩니다.",
                "서울 올림픽공원 핸드볼경기장", "대한보디빌딩협회");
        a.putTranslation(LocaleCode.JA, "全国ボディビル選手権大会",
                "韓国最大規模のアマチュア大会。階級別の予選と決勝が2日間にわたり行われます。",
                "ソウル オリンピック公園 ハンドボール競技場", "大韓ボディビル協会");
        competitionRepository.save(a);

        Competition b = Competition.builder()
                .country(Country.JP)
                .startDate(today.plusDays(34))
                .endDate(today.plusDays(34))
                .published(true)
                .build();
        b.putTranslation(LocaleCode.KO, "재팬 클래식 피지크 오픈",
                "일본 클래식 피지크 오픈 대회. 해외 선수 참가가 가능합니다.",
                "도쿄 시부야 공회당", "JBBF");
        b.putTranslation(LocaleCode.JA, "ジャパン クラシックフィジーク オープン",
                "日本のクラシックフィジークオープン大会。海外選手の参加が可能です。",
                "東京 渋谷公会堂", "JBBF");
        competitionRepository.save(b);

        Competition past = Competition.builder()
                .country(Country.KR)
                .startDate(today.minusDays(24))
                .endDate(today.minusDays(24))
                .published(true)
                .build();
        past.putTranslation(LocaleCode.KO, "스프링 클래식 챔피언십",
                "시즌 개막을 여는 대회. 신인부가 별도로 운영되었습니다.",
                "부산 벡스코", "한국피트니스협회");
        past.putTranslation(LocaleCode.JA, "スプリング クラシック チャンピオンシップ",
                "シーズン開幕の大会。新人部門が別途運営されました。",
                "釜山 BEXCO", "韓国フィットネス協会");
        competitionRepository.save(past);

        log.info("[local] 시합 일정 시드 3건 생성");
    }

    private void seedPosts() {
        if (postRepository.count() > 0) {
            return;
        }

        Post p1 = Post.builder()
                .slug("junyoung-week-01")
                .category(PostCategory.CREW)
                .published(true)
                .build();
        p1.putTranslation(LocaleCode.KO, "준영의 첫 시합", "첫 대회를 신청했다",
                "3년을 미뤘다. 몸이 준비되면 나가겠다고 했는데, 준비된 몸이라는 건 애초에 없었다.",
                "3년을 미뤘다. 몸이 준비되면 나가겠다고 했는데, 준비된 몸이라는 건 애초에 없었다.\n\n"
                        + "접수 버튼을 누르고 나니 계획이 생겼다. 12주. 체중 8kg 감량. "
                        + "매일 아침 공복 유산소 40분. 계획은 늘 아름답고 실행은 늘 추하다.");
        p1.putTranslation(LocaleCode.JA, "ジュニョンの初大会", "初めての大会に申し込んだ",
                "3年間先延ばしにした。体が仕上がったら出ると言っていたが、仕上がった体などそもそも存在しない。",
                "3年間先延ばしにした。体が仕上がったら出ると言っていたが、仕上がった体などそもそも存在しない。\n\n"
                        + "申込ボタンを押した瞬間、計画ができた。12週間。体重8kg減。"
                        + "毎朝空腹有酸素40分。計画はいつも美しく、実行はいつも醜い。");
        postRepository.save(p1);

        // ★ 일본어 번역 없음 — /ja/media 에서 이 글이 안 보여야 정상입니다.
        Post p2 = Post.builder()
                .slug("junyoung-week-04")
                .category(PostCategory.CREW)
                .published(true)
                .build();
        p2.putTranslation(LocaleCode.KO, "준영의 첫 시합", "4주차, 거울이 거짓말을 한다",
                "체중은 4kg 빠졌는데 거울 속 몸은 그대로다. 숫자와 눈이 다른 말을 할 때 무엇을 믿어야 하나.",
                "체중은 4kg 빠졌는데 거울 속 몸은 그대로다.\n\n"
                        + "코치는 사진을 찍으라고 했다. 매주 같은 자리, 같은 조명, 같은 포즈. "
                        + "4주치를 나란히 놓고 나서야 변화가 보였다. 매일 보는 사람은 변화를 못 본다.");
        postRepository.save(p2);

        Post p3 = Post.builder()
                .slug("osaka-expedition")
                .category(PostCategory.CREW)
                .published(true)
                .build();
        p3.putTranslation(LocaleCode.KO, "원정", "오사카 원정기 — 언어가 무대를 막을 때",
                "접수 서류를 못 읽어서 체급을 잘못 신청할 뻔했다. 통역이 필요한 순간은 무대 위가 아니라 그 전에 있다.",
                "접수 서류를 못 읽어서 체급을 잘못 신청할 뻔했다.\n\n"
                        + "일본 대회는 접수부터 계량, 대기 순서 안내까지 전부 현지어로 진행된다. "
                        + "영어 안내는 없다고 봐야 한다. 우리가 통역 창구를 만든 이유가 여기 있다.");
        p3.putTranslation(LocaleCode.JA, "遠征", "大阪遠征記 — 言葉がステージを塞ぐとき",
                "申込書類が読めず、階級を間違えて申請するところだった。通訳が必要な瞬間はステージの上ではなく、その前にある。",
                "申込書類が読めず、階級を間違えて申請するところだった。\n\n"
                        + "日本の大会は申込から計量、待機順の案内まですべて現地語で進む。"
                        + "英語の案内はないと考えたほうがいい。私たちが通訳窓口を作った理由がここにある。");
        postRepository.save(p3);

        log.info("[local] 미디어 글 시드 3건 생성 (1건은 일본어 미번역 — 폴백 정책 확인용)");
    }
}
