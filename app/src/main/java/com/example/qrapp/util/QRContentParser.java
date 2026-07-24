package com.example.qrapp.util;

import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.model.QRContentType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhận diện loại nội dung QR (Wifi / Location / Contact / Email / Phone / SMS / URL) từ chuỗi văn bản
 * theo các định dạng chuẩn: WIFI:, geo:, MECARD:, BEGIN:VCARD, mailto:, MATMSG:, tel:, sms:/smsto:, http(s)://.
 */
public final class QRContentParser {
    private static final Pattern GEO_PATTERN = Pattern.compile(
            "^geo:(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private QRContentParser() {}

    public static ParsedQRContent parse(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.regionMatches(true, 0, "WIFI:", 0, 5)) {
            return parseWifi(content);
        }
        if (content.regionMatches(true, 0, "geo:", 0, 4)) {
            return parseGeo(content);
        }
        if (content.regionMatches(true, 0, "MECARD:", 0, 7)) {
            return parseMeCard(content);
        }
        if (content.toUpperCase().contains("BEGIN:VCARD")) {
            return parseVCard(content);
        }
        if (content.regionMatches(true, 0, "mailto:", 0, 7)) {
            return parseMailto(content);
        }
        if (content.regionMatches(true, 0, "MATMSG:", 0, 7)) {
            return parseMatMsg(content);
        }
        if (content.regionMatches(true, 0, "smsto:", 0, 6)) {
            return parseSms(content, 6);
        }
        if (content.regionMatches(true, 0, "sms:", 0, 4)) {
            return parseSms(content, 4);
        }
        if (content.regionMatches(true, 0, "tel:", 0, 4)) {
            ParsedQRContent parsed = new ParsedQRContent(QRContentType.PHONE, content);
            parsed.setContactPhone(content.substring(4).trim());
            if (isEmpty(parsed.getContactPhone())) return new ParsedQRContent(QRContentType.TEXT, content);
            return parsed;
        }
        if (EMAIL_PATTERN.matcher(content).matches()) {
            ParsedQRContent parsed = new ParsedQRContent(QRContentType.EMAIL, content);
            parsed.setContactEmail(content);
            return parsed;
        }
        if (content.regionMatches(true, 0, "http://", 0, 7) || content.regionMatches(true, 0, "https://", 0, 8)) {
            return new ParsedQRContent(QRContentType.URL, content);
        }
        return new ParsedQRContent(QRContentType.TEXT, content);
    }

    private static ParsedQRContent parseSms(String content, int schemeLength) {
        String withoutScheme = content.substring(schemeLength);
        String number = withoutScheme;
        String query = null;
        int queryIndex = withoutScheme.indexOf('?');
        if (queryIndex >= 0) {
            number = withoutScheme.substring(0, queryIndex);
            query = withoutScheme.substring(queryIndex + 1);
        }
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.SMS, content);
        parsed.setContactPhone(number.trim());
        if (query != null) parsed.setSmsBody(decodeQueryParam(query, "body"));
        if (isEmpty(parsed.getContactPhone())) return new ParsedQRContent(QRContentType.TEXT, content);
        return parsed;
    }

    private static ParsedQRContent parseWifi(String content) {
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.WIFI, content);
        parsed.setSsid(unescape(extractField(content, "S")));
        parsed.setPassword(unescape(extractField(content, "P")));
        String security = extractField(content, "T");
        parsed.setSecurityType(security == null || security.isEmpty() ? "nopass" : security);
        if (parsed.getSsid() == null || parsed.getSsid().isEmpty()) {
            return new ParsedQRContent(QRContentType.TEXT, content);
        }
        return parsed;
    }

    private static ParsedQRContent parseGeo(String content) {
        Matcher matcher = GEO_PATTERN.matcher(content);
        if (!matcher.find()) return new ParsedQRContent(QRContentType.TEXT, content);
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.LOCATION, content);
        try {
            parsed.setLatitude(Double.parseDouble(matcher.group(1)));
            parsed.setLongitude(Double.parseDouble(matcher.group(2)));
        } catch (NumberFormatException exception) {
            return new ParsedQRContent(QRContentType.TEXT, content);
        }
        return parsed;
    }

    private static ParsedQRContent parseMeCard(String content) {
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.CONTACT, content);
        String name = unescape(extractField(content, "N"));
        parsed.setContactName(name == null ? "" : name.replace(",", " ").trim());
        parsed.setContactPhone(unescape(extractField(content, "TEL")));
        parsed.setContactEmail(unescape(extractField(content, "EMAIL")));
        if (isEmpty(parsed.getContactName()) && isEmpty(parsed.getContactPhone()) && isEmpty(parsed.getContactEmail())) {
            return new ParsedQRContent(QRContentType.TEXT, content);
        }
        return parsed;
    }

    private static ParsedQRContent parseVCard(String content) {
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.CONTACT, content);
        parsed.setContactName(firstNonEmpty(extractLine(content, "FN"), extractLine(content, "N")));
        parsed.setContactPhone(extractLine(content, "TEL"));
        parsed.setContactEmail(extractLine(content, "EMAIL"));
        if (isEmpty(parsed.getContactName()) && isEmpty(parsed.getContactPhone()) && isEmpty(parsed.getContactEmail())) {
            return new ParsedQRContent(QRContentType.TEXT, content);
        }
        return parsed;
    }

    private static ParsedQRContent parseMailto(String content) {
        String withoutScheme = content.substring(7);
        String email = withoutScheme;
        String query = null;
        int queryIndex = withoutScheme.indexOf('?');
        if (queryIndex >= 0) {
            email = withoutScheme.substring(0, queryIndex);
            query = withoutScheme.substring(queryIndex + 1);
        }
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.EMAIL, content);
        parsed.setContactEmail(email.trim());
        if (query != null) {
            parsed.setEmailSubject(decodeQueryParam(query, "subject"));
            parsed.setEmailBody(decodeQueryParam(query, "body"));
        }
        if (isEmpty(parsed.getContactEmail())) return new ParsedQRContent(QRContentType.TEXT, content);
        return parsed;
    }

    private static ParsedQRContent parseMatMsg(String content) {
        ParsedQRContent parsed = new ParsedQRContent(QRContentType.EMAIL, content);
        parsed.setContactEmail(unescape(extractField(content, "TO")));
        parsed.setEmailSubject(unescape(extractField(content, "SUB")));
        parsed.setEmailBody(unescape(extractField(content, "BODY")));
        if (isEmpty(parsed.getContactEmail())) return new ParsedQRContent(QRContentType.TEXT, content);
        return parsed;
    }

    private static String decodeQueryParam(String query, String key) {
        for (String pair : query.split("&")) {
            int eqIndex = pair.indexOf('=');
            String pairKey = eqIndex >= 0 ? pair.substring(0, eqIndex) : pair;
            if (!pairKey.equalsIgnoreCase(key)) continue;
            String value = eqIndex >= 0 ? pair.substring(eqIndex + 1) : "";
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException | IllegalArgumentException exception) {
                return value;
            }
        }
        return null;
    }

    /** Trích giá trị của field dạng ";KEY:value" trong chuỗi MECARD/WIFI (không tính dấu ';' bị escape bằng '\'). */
    private static String extractField(String content, String key) {
        Pattern pattern = Pattern.compile("(?:^|;)" + Pattern.quote(key) + ":((?:\\\\.|[^;])*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Trích giá trị của một dòng vCard dạng "KEY[;...]:value". */
    private static String extractLine(String content, String key) {
        Pattern pattern = Pattern.compile("(?:^|\\r?\\n)" + Pattern.quote(key) + "(?:;[^:\\r\\n]*)?:([^\\r\\n]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String unescape(String value) {
        if (value == null) return null;
        return value.replace("\\;", ";").replace("\\,", ",").replace("\\:", ":").replace("\\\\", "\\");
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String firstNonEmpty(String a, String b) {
        return !isEmpty(a) ? a : b;
    }
}
