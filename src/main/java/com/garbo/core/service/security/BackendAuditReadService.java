package com.garbo.core.service.security;

import com.garbo.common.logging.BackendFileAuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BackendAuditReadService {

    private final BackendFileAuditLogger backendFileAuditLogger;
    private final String hmacSecret;

    public BackendAuditReadService(
            BackendFileAuditLogger backendFileAuditLogger,
            @Value("${audit.file-change.hmac-secret:change-me-in-prod}") String hmacSecret) {
        this.backendFileAuditLogger = backendFileAuditLogger;
        this.hmacSecret = hmacSecret;
    }

    public List<String> getRecentEntries(int limit) throws IOException {
        int boundedLimit = Math.min(Math.max(limit, 1), 500);
        Path auditPath = backendFileAuditLogger.getAuditLogPath();
        if (!Files.exists(auditPath)) {
            return List.of();
        }

        List<String> lines = Files.readAllLines(auditPath, StandardCharsets.UTF_8)
                .stream()
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        int fromIndex = Math.max(lines.size() - boundedLimit, 0);
        return lines.subList(fromIndex, lines.size());
    }

    public Map<String, Object> verifyActiveLogIntegrity() throws IOException {
        Path auditPath = backendFileAuditLogger.getAuditLogPath();
        Map<String, Object> report = new LinkedHashMap<>();
        if (!Files.exists(auditPath)) {
            report.put("ok", true);
            report.put("checkedEntries", 0);
            report.put("issues", List.of());
            return report;
        }

        List<String> issues = new ArrayList<>();
        List<String> lines = Files.readAllLines(auditPath, StandardCharsets.UTF_8)
                .stream()
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        String expectedPrevHash = "GENESIS";
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            Map<String, String> fields = parseLine(line);
            String canonical = buildCanonical(fields);
            String prevHash = fields.getOrDefault("prevHash", "");
            String sig = fields.getOrDefault("sig", "");
            String entryHash = fields.getOrDefault("entryHash", "");

            if (!expectedPrevHash.equals(prevHash)) {
                issues.add("Line " + lineNumber + " prevHash mismatch");
            }

            String expectedSig = computeHmac(canonical);
            if (!expectedSig.equals(sig)) {
                issues.add("Line " + lineNumber + " signature mismatch");
            }

            String expectedHash = sha256(canonical + "|sig=" + sig);
            if (!expectedHash.equals(entryHash)) {
                issues.add("Line " + lineNumber + " entryHash mismatch");
            }

            expectedPrevHash = entryHash;
        }

        report.put("ok", issues.isEmpty());
        report.put("checkedEntries", lines.size());
        report.put("issues", issues);
        return report;
    }

    private Map<String, String> parseLine(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] parts = line.split("\\|");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = part.substring(0, idx).trim();
            String value = part.substring(idx + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    private String buildCanonical(Map<String, String> fields) {
        return "ts=" + fields.getOrDefault("ts", "")
                + "|event=" + fields.getOrDefault("event", "")
                + "|actor=" + fields.getOrDefault("actor", "")
                + "|ip=" + fields.getOrDefault("ip", "")
                + "|request=" + fields.getOrDefault("request", "")
                + "|target=" + fields.getOrDefault("target", "")
                + "|outcome=" + fields.getOrDefault("outcome", "")
                + "|detail=" + fields.getOrDefault("detail", "")
                + "|prevHash=" + fields.getOrDefault("prevHash", "");
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "hmac-error";
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "sha256-error";
        }
    }
}
