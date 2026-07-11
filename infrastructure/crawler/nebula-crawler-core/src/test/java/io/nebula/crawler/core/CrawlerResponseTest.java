package io.nebula.crawler.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerResponseTest {

    @Test
    void asDocumentResolvesRelativeLinksAgainstRequestUrl() {
        CrawlerResponse response = CrawlerResponse.builder()
                .url("https://example.test/articles/page.html")
                .content("<a href=\"../about\">About</a>")
                .build();

        assertThat(response.asDocument().selectFirst("a").absUrl("href"))
                .isEqualTo("https://example.test/about");
    }

    @Test
    void asDocumentPrefersFinalUrlAfterRedirect() {
        CrawlerResponse response = CrawlerResponse.builder()
                .url("https://example.test/redirect")
                .finalUrl("https://cdn.example.test/docs/index.html")
                .content("<a href=\"asset.css\">Asset</a>")
                .build();

        assertThat(response.asDocument().selectFirst("a").absUrl("href"))
                .isEqualTo("https://cdn.example.test/docs/asset.css");
    }
}
