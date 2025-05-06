package com.dotcms.plugin.sqs;

import java.io.Serializable;
import java.util.Arrays;
import javax.annotation.Nonnull;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AppConfig.Builder.class)
public class AppConfig implements Serializable {


    private static final long serialVersionUID = 1L;

    public AppConfig() {
        this.awsAccessKey = null;
        this.awsSecretKey = null;
    }

    public final String awsAccessKey;
    public final String awsSecretKey;


    
    @Override
    public String toString() {
        return "AppConfig {awsAccessKey:" + awsAccessKey + ", awsSecretKey:" + awsSecretKey + "}";
    }


    private AppConfig(Builder builder) {
        this.awsAccessKey = builder.awsAccessKey;
        this.awsSecretKey = builder.awsSecretKey;
    }


    /**
     * Creates builder to build {@link AnalyticsConfig}.
     * 
     * @return created builder
     */

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link AnalyticsConfig} and initialize it with the given object.
     * 
     * @param analyticsConfig to initialize the builder with
     * @return created builder
     */

    public static Builder from(AppConfig appConfig) {
        return new Builder(appConfig);
    }

    /**
     * Builder to build {@link AnalyticsConfig}.
     */

    public static final class Builder {
        private String awsAccessKey;
        private String awsSecretKey;

        private Builder() {}

        private Builder(AppConfig appConfig) {
            this.awsAccessKey = appConfig.awsAccessKey;
            this.awsSecretKey = appConfig.awsSecretKey;


        }

        public Builder awsAccessKey(@Nonnull String awsAccessKey) {
            this.awsAccessKey = awsAccessKey;
            return this;
        }


        public Builder awsSecretKey(@Nonnull String awsSecretKey) {
            this.awsSecretKey = awsSecretKey;
            return this;
        }


        public AppConfig build() {
            return new AppConfig(this);
        }

        final String[] strToArray(final String val) {

            return UtilMethods.isSet(val)
                            ? Arrays.stream(val.toLowerCase().split(",")).map(String::trim).toArray(String[]::new)
                            : new String[0];

        }

    }


}
