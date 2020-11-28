package com.azad.yearn.deployer.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = AppProperties.CONFIGURATION_PROPERTY_PREFIX,
                         ignoreUnknownFields = false)
public class AppProperties {
    static final String CONFIGURATION_PROPERTY_PREFIX = "application";
    private final Async async = new Async();
    private final Etherscan etherscan = new Etherscan();
    private final Twitter  twitter = new Twitter();

    public Async getAsync() {
        return async;
    }

    public static class Async {

        private Integer corePoolSize;
        private Integer maxPoolSize = Integer.MAX_VALUE;
        private Integer queueCapacity = Integer.MAX_VALUE;
        private Integer forkThreadSize = 10;

        public Integer getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(final Integer corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public Integer getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(final Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public Integer getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(final Integer queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public Integer getForkThreadSize() {
            return forkThreadSize;
        }

        public void setForkThreadSize(Integer forkThreadSize) {
            this.forkThreadSize = forkThreadSize;
        }
    }

    public Etherscan getEtherscan() {
        return etherscan;
    }

    public static class Etherscan {
        private String deployer;
        private String apikey;

        public String getDeployer() {
            return deployer;
        }

        public void setDeployer(final String deployer) {
            this.deployer = deployer;
        }

        public String getApikey() {
            return apikey;
        }

        public void setApikey(final String apikey) {
            this.apikey = apikey;
        }


    }

    public Twitter getTwitter() {
        return twitter;
    }

    public static class Twitter {

        private String consumerKey;
        private String consumerSecret;
        private String accessToken;
        private String tokenSecret;

        public String getConsumerKey() {
            return consumerKey;
        }

        public void setConsumerKey(String consumerKey) {
            this.consumerKey = consumerKey;
        }

        public String getConsumerSecret() {
            return consumerSecret;
        }

        public void setConsumerSecret(String consumerSecret) {
            this.consumerSecret = consumerSecret;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenSecret() {
            return tokenSecret;
        }

        public void setTokenSecret(String tokenSecret) {
            this.tokenSecret = tokenSecret;
        }
    }

}
