package io.github.spring.middleware.registry.scanner;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "middleware.registry-scanner")
public class RegistryScannerProperties {

    private boolean enabled = true;
    private String cron = "*/30 * * * * *";
    private int timeoutMillis = 500;
    private int maxNoAvail = 5;
    private int concurrency = 20;
    private String healthPath = "/actuator/health";          // default: "/actuator/health"
    private String registerResourcePath = "/resource/register";  // default: "/resource/register"

    public RegistryScannerProperties() {
        // defaults set via field initializers
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        if (cron != null && !cron.isBlank()) this.cron = cron;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        if (timeoutMillis > 0) this.timeoutMillis = timeoutMillis;
    }

    public int getMaxNoAvail() {
        return maxNoAvail;
    }

    public void setMaxNoAvail(int maxNoAvail) {
        if (maxNoAvail > 0) this.maxNoAvail = maxNoAvail;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        if (concurrency > 0) this.concurrency = concurrency;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        if (healthPath != null && !healthPath.isBlank()) this.healthPath = healthPath;
    }

    public String getRegisterResourcePath() {
        return registerResourcePath;
    }

    public void setRegisterResourcePath(String registerResourcePath) {
        if (registerResourcePath != null && !registerResourcePath.isBlank()) this.registerResourcePath = registerResourcePath;
    }

}
