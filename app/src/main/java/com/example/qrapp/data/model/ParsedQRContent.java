package com.example.qrapp.data.model;

public class ParsedQRContent {
    private final QRContentType type;
    private final String rawContent;
    private String ssid;
    private String password;
    private String securityType;
    private double latitude;
    private double longitude;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String emailSubject;
    private String emailBody;
    private String smsBody;

    public ParsedQRContent(QRContentType type, String rawContent) {
        this.type = type;
        this.rawContent = rawContent;
    }

    public QRContentType getType() { return type; }
    public String getRawContent() { return rawContent; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSecurityType() { return securityType; }
    public void setSecurityType(String securityType) { this.securityType = securityType; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
    public String getEmailBody() { return emailBody; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }
    public String getSmsBody() { return smsBody; }
    public void setSmsBody(String smsBody) { this.smsBody = smsBody; }
}
