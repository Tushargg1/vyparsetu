package com.vyaparsetu.auth.service;

import com.vyaparsetu.auth.entity.OtpToken;
import com.vyaparsetu.auth.repository.OtpTokenRepository;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.OtpException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OtpService {

    private final OtpTokenRepository otpRepository;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final Map<Enums.OtpChannel, OtpSender> senders;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpTokenRepository otpRepository,
                      PasswordEncoder encoder,
                      AppProperties props,
                      List<OtpSender> senderList) {
        this.otpRepository = otpRepository;
        this.encoder = encoder;
        this.props = props;
        this.senders = senderList.stream()
                .collect(Collectors.toMap(OtpSender::channel, Function.identity()));
    }

    @Transactional
    public void generateAndSend(String identifier, Enums.OtpChannel channel,
                                Enums.OtpPurpose purpose, Long userId) {
        // SECURITY: throttle OTP resends per identifier to limit SMS cost / bombing.
        otpRepository.findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(identifier, purpose)
                .ifPresent(last -> {
                    Instant created = last.getCreatedAt();
                    long cooldown = props.getSecurity().getOtp().getResendCooldownSeconds();
                    if (created != null && created.isAfter(Instant.now().minusSeconds(cooldown))) {
                        throw new OtpException("OTP_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                                "Please wait before requesting another OTP.");
                    }
                });

        String code = generateCode(props.getSecurity().getOtp().getLength());

        OtpToken token = new OtpToken();
        token.setUserId(userId);
        token.setIdentifier(identifier);
        token.setChannel(channel);
        token.setPurpose(purpose);
        token.setCodeHash(encoder.encode(code));
        token.setExpiresAt(Instant.now().plus(props.getSecurity().getOtp().getTtlMinutes(), ChronoUnit.MINUTES));
        otpRepository.save(token);

        OtpSender sender = senders.get(channel);
        if (sender == null) {
            throw new OtpException("No OTP sender configured for channel " + channel);
        }
        sender.send(identifier, code);
    }

    @Transactional
    public OtpToken verify(String identifier, String code, Enums.OtpPurpose purpose) {
        OtpToken token = otpRepository
                .findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new OtpException("No active OTP found. Please request a new one."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpException("OTP expired. Please request a new one.");
        }
        if (token.getAttempts() >= props.getSecurity().getOtp().getMaxAttempts()) {
            throw new OtpException("OTP_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please request a new OTP.");
        }
        if (!encoder.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            otpRepository.save(token);
            throw new OtpException("Invalid OTP.");
        }
        token.setConsumedAt(Instant.now());
        otpRepository.save(token);
        return token;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
