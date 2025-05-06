package com.dotcms.plugin.sqs;

public enum AppKeys {
    AWS_ACCESS_KEY("awsAccessKey"),
    AWS_SECRET_KEY("awsSecretKey");

    final public String key;

    AppKeys(String key) {
        this.key = key;
    }

    public final static String APP_KEY = "dotSqsApp";

    public final static String APP_YAML_NAME = APP_KEY + ".yml";

}
