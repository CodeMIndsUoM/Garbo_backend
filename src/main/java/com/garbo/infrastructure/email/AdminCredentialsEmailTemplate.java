package com.garbo.infrastructure.email;

import java.time.Year;

final class AdminCredentialsEmailTemplate {

    private static final String BRAND_GREEN = "#16a34a";
    private static final String BRAND_GREEN_DARK = "#15803d";
    private static final String TEXT_PRIMARY = "#111827";
    private static final String TEXT_MUTED = "#6b7280";
    private static final String SURFACE = "#f9fafb";
    private static final String BORDER = "#e5e7eb";

    private AdminCredentialsEmailTemplate() {
    }

    static String buildHtml(String toEmail, String tempPassword) {
        String safeEmail = escapeHtml(toEmail);
        String safePassword = escapeHtml(tempPassword);
        int year = Year.now().getValue();

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Your Garbo Account</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:%s;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f3f4f6;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border:1px solid %s;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(17,24,39,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,%s 0%%,%s 100%%);padding:28px 32px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td>
                                    <div style="display:inline-block;width:40px;height:40px;border-radius:10px;background-color:rgba(255,255,255,0.18);text-align:center;line-height:40px;font-size:20px;color:#ffffff;font-weight:700;">G</div>
                                  </td>
                                  <td style="padding-left:12px;">
                                    <div style="font-size:22px;font-weight:700;color:#ffffff;letter-spacing:-0.02em;">Garbo</div>
                                    <div style="font-size:13px;color:rgba(255,255,255,0.88);margin-top:2px;">Waste Management System</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <div style="font-size:24px;font-weight:700;line-height:1.3;color:%s;margin-bottom:8px;">Welcome to Garbo</div>
                              <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:%s;">
                                Your internal account has been created. Use the credentials below to sign in for the first time.
                              </p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:%s;border:1px solid %s;border-radius:12px;margin-bottom:20px;">
                                <tr>
                                  <td style="padding:20px 22px;">
                                    <div style="font-size:12px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:%s;margin-bottom:14px;">Login credentials</div>
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                      <tr>
                                        <td style="padding:10px 0;border-bottom:1px solid %s;">
                                          <div style="font-size:12px;color:%s;margin-bottom:4px;">Email</div>
                                          <div style="font-size:15px;font-weight:600;color:%s;">%s</div>
                                        </td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;">
                                          <div style="font-size:12px;color:%s;margin-bottom:4px;">Temporary password</div>
                                          <div style="font-size:18px;font-weight:700;letter-spacing:0.04em;color:%s;font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;">%s</div>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
                                <tr>
                                  <td style="padding:16px 18px;background-color:#ecfdf5;border:1px solid #bbf7d0;border-radius:12px;">
                                    <div style="font-size:14px;font-weight:700;color:#166534;margin-bottom:8px;">First login steps</div>
                                    <ol style="margin:0;padding-left:18px;font-size:14px;line-height:1.7;color:#166534;">
                                      <li>Open the Garbo mobile app.</li>
                                      <li>Sign in with the email and temporary password above.</li>
                                      <li>Create a new password when prompted.</li>
                                      <li>Your duty status will switch to <strong>On Duty</strong> after setup.</li>
                                    </ol>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0;font-size:13px;line-height:1.6;color:%s;">
                                For security, do not share this email. If you did not expect this account, contact your council administrator.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px 24px;border-top:1px solid %s;background-color:%s;text-align:center;">
                              <div style="font-size:12px;line-height:1.5;color:%s;">&copy; %d Garbo. All rights reserved.</div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                TEXT_PRIMARY,
                BORDER,
                BRAND_GREEN,
                BRAND_GREEN_DARK,
                TEXT_PRIMARY,
                TEXT_MUTED,
                SURFACE,
                BORDER,
                TEXT_MUTED,
                BORDER,
                TEXT_MUTED,
                TEXT_PRIMARY,
                safeEmail,
                TEXT_MUTED,
                BRAND_GREEN_DARK,
                safePassword,
                TEXT_MUTED,
                BORDER,
                SURFACE,
                TEXT_MUTED,
                year);
    }

    static String buildPlainText(String toEmail, String tempPassword) {
        return """
                Welcome to Garbo
                ================

                Your internal account has been created.

                Login credentials
                -----------------
                Email: %s
                Temporary password: %s

                First login steps
                -----------------
                1. Open the Garbo mobile app.
                2. Sign in with the email and temporary password above.
                3. Create a new password when prompted.
                4. Your duty status will switch to On Duty after setup.

                For security, do not share this email. If you did not expect this account, contact your council administrator.
                """.formatted(
                toEmail == null ? "" : toEmail,
                tempPassword == null ? "" : tempPassword);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
