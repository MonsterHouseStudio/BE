package com.monsterhouse.content.seo;

import com.monsterhouse.common.config.SiteProperties;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * sitemap.xml / robots.txt (기획서 §2.1, §10 Phase5).
 *
 * ★ hreflang 을 sitemap 에 넣는 이유
 *   같은 글의 한국어판과 일본어판이 "중복 콘텐츠"가 아니라 "서로의 번역"이라는 걸
 *   검색엔진에 알려야 합니다. 그러지 않으면 둘 중 하나만 색인되거나
 *   일본 사용자에게 한국어 페이지가 노출됩니다.
 *
 * ★ 번역이 없는 글은 그 언어의 URL 을 넣지 않습니다
 *   /ja/media/xxx 가 404 인데 sitemap 에 올리면 크롤 예산만 낭비하고
 *   사이트 신뢰도가 떨어집니다. 실제 존재하는 URL 만 싣습니다.
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 언어별로 존재하는 정적 페이지 */
    private static final List<String> STATIC_PATHS = List.of(
            "", "/shooting", "/shooting/booking", "/schedule",
            "/interpreter", "/gallery", "/about", "/media", "/privacy");

    private final SiteProperties siteProperties;
    private final PostRepository postRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public String sitemap() {
        String base = siteProperties.base();
        StringBuilder xml = new StringBuilder();

        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        xmlns:xhtml="http://www.w3.org/1999/xhtml">
                """);

        // 정적 페이지 — 모든 언어에 존재하므로 서로를 alternate 로 가리킵니다.
        for (String path : STATIC_PATHS) {
            for (LocaleCode locale : LocaleCode.values()) {
                appendUrl(xml, base + "/" + locale.getCode() + path, null,
                        LocaleCode.values(), base, path);
            }
        }

        // 미디어 글 — 번역이 있는 언어만
        for (Post post : postRepository.findAllForAdmin()) {
            if (!post.isPublished()) {
                continue;
            }
            LocaleCode[] available = java.util.Arrays.stream(LocaleCode.values())
                    .filter(post::hasTranslation)
                    .toArray(LocaleCode[]::new);

            if (available.length == 0) {
                continue;
            }

            String path = "/media/" + post.getSlug();
            String lastmod = post.getUpdatedAt() == null
                    ? null : post.getUpdatedAt().toLocalDate().format(W3C);

            for (LocaleCode locale : available) {
                appendUrl(xml, base + "/" + locale.getCode() + path, lastmod,
                        available, base, path);
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                # 관리자 화면과 API 는 색인 대상이 아닙니다.
                Disallow: /admin
                Disallow: /api/
                Disallow: /uploads/

                Sitemap: %s/sitemap.xml
                """.formatted(siteProperties.base());
    }

    private void appendUrl(StringBuilder xml, String loc, String lastmod,
                           LocaleCode[] alternates, String base, String path) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escape(loc)).append("</loc>\n");

        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }

        for (LocaleCode alt : alternates) {
            xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"")
                    .append(alt.getCode())
                    .append("\" href=\"")
                    .append(escape(base + "/" + alt.getCode() + path))
                    .append("\"/>\n");
        }

        // x-default = 언어를 특정할 수 없는 방문자에게 보여줄 기본 페이지
        xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"")
                .append(escape(base + "/" + LocaleCode.DEFAULT.getCode() + path))
                .append("\"/>\n");

        xml.append("  </url>\n");
    }

    /** slug 에 &, < 등이 들어가면 XML 이 깨집니다. */
    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}