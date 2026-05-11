package com.vicente.storage.validation;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    private  ValidationPatterns() {}

    public static final String ACCESS_KEY_REGEX = "^[A-Za-z0-9\\-_]{3,64}$";
    public static final String BUCKET_NAME_REGEX = "^(?!.*--)[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$";
    public static final Pattern MD5_HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");
}
