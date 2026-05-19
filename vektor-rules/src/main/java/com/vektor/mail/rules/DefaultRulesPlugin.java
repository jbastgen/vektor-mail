package com.vektor.mail.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vektor.mail.core.model.Rule;
import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.core.plugin.RulesPlugin;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Simple JSON-based rules engine.
 *
 * Condition format: {"field":"from|subject|to|size","op":"contains|equals|gt|lt","value":"..."}
 * Action format:    {"type":"move|copy|discard|forward","target":"<mailbox path or address>"}
 */
@Extension
@Component
@Slf4j
public class DefaultRulesPlugin implements RulesPlugin {

    private static final String ATTR_TARGET_MAILBOX = "targetMailbox";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getPluginId() { return "vektor-rules-default"; }

    @Override
    public void applyRules(MailContext ctx, List<Rule> rules) {
        for (Rule rule : rules) {
            try {
                if (matches(ctx, rule.getCondition())) {
                    applyAction(ctx, rule.getAction());
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate rule '{}': {}", rule.getName(), e.getMessage());
            }
        }
    }

    private boolean matches(MailContext ctx, String conditionJson) throws Exception {
        if (conditionJson == null || conditionJson.isBlank()) return false;
        JsonNode cond = mapper.readTree(conditionJson);
        String field = cond.get("field").asText();
        String op = cond.get("op").asText();
        String value = cond.get("value").asText();

        String fieldValue = switch (field) {
            case "from" -> ctx.getEnvelopeFrom();
            case "to" -> ctx.getEnvelopeTo().isEmpty() ? "" : ctx.getEnvelopeTo().get(0);
            case "subject" -> extractSubject(ctx);
            default -> "";
        };

        return switch (op) {
            case "contains" -> fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase());
            case "equals" -> value.equalsIgnoreCase(fieldValue);
            case "startsWith" -> fieldValue != null && fieldValue.toLowerCase().startsWith(value.toLowerCase());
            default -> false;
        };
    }

    private void applyAction(MailContext ctx, String actionJson) throws Exception {
        if (actionJson == null || actionJson.isBlank()) return;
        JsonNode action = mapper.readTree(actionJson);
        String type = action.get("type").asText();

        switch (type) {
            case "move" -> ctx.setAttribute(ATTR_TARGET_MAILBOX, action.get("target").asText());
            case "discard" -> ctx.setAttribute("discard", true);
            case "forward" -> ctx.setAttribute("forwardTo", action.get("target").asText());
            default -> log.warn("Unknown rule action type: {}", type);
        }
    }

    private String extractSubject(MailContext ctx) {
        if (ctx.getRawMessage() == null) return "";
        String raw = new String(ctx.getRawMessage(), StandardCharsets.UTF_8);
        for (String line : raw.split("\r?\n")) {
            if (line.toLowerCase().startsWith("subject:")) {
                return line.substring(8).trim();
            }
            if (line.isBlank()) break; // end of headers
        }
        return "";
    }
}
