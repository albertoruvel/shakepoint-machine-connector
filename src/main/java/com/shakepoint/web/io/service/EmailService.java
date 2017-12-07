package com.shakepoint.web.io.service;

import com.shakepoint.web.io.email.Email;

import java.util.Map;

public interface EmailService {
    void sendEmail(final Email emailType, final String to, final Map<String, Object> params);
}
