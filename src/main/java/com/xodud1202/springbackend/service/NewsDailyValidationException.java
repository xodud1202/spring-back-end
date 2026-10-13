package com.xodud1202.springbackend.service;

import java.util.Map;

// Safe metadata only: never include the submitted article text.
public class NewsDailyValidationException extends IllegalArgumentException {
    private final int articleDispOrd;
    private final String field;
    private final int maxLength;
    private final int actualLength;

    public NewsDailyValidationException(int articleDispOrd, String field, int maxLength, int actualLength, String reason) {
        super("기사 dispOrd=" + articleDispOrd + ", 필드=" + field + ", 최대=" + maxLength
                + ", 실제=" + actualLength + "자: " + reason);
        this.articleDispOrd=articleDispOrd; this.field=field; this.maxLength=maxLength; this.actualLength=actualLength;
    }
    public Map<String,Object> details() {
        return Map.of("message",getMessage(),"articleDispOrd",articleDispOrd,"field",field,
                "maxLength",maxLength,"actualLength",actualLength);
    }
}

