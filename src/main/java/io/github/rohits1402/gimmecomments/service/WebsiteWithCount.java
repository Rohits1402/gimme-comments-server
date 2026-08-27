package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.Website;

public record WebsiteWithCount(Website website, long commentCount) {
}
