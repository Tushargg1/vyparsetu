package com.vyaparsetu.notification.whatsapp;

import com.vyaparsetu.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default WhatsApp client. When the feature flag is off it short-circuits;
 * when on (dev) it logs instead of calling a paid provider.
 */
@Component
public class NoopWhatsAppClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(NoopWhatsAppClient.class);
    private final AppProperties props;

    public NoopWhatsAppClient(AppProperties props) {
        this.props = props;
    }

    @Override
    public boolean isEnabled() {
        return props.getFeatures().getWhatsapp().isEnabled();
    }

    @Override
    public void sendText(String toPhone, String message) {
        if (!isEnabled()) return;
        log.info("[WHATSAPP] to {} : {}", toPhone, message);
    }

    @Override
    public void sendDocument(String toPhone, String caption, String documentUrl) {
        if (!isEnabled()) return;
        log.info("[WHATSAPP-DOC] to {} : {} ({})", toPhone, caption, documentUrl);
    }
}
