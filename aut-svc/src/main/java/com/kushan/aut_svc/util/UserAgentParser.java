package com.kushan.aut_svc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserAgentParser {

	private UserAgentParser() {
	}

	public static String deviceType(String ua) {
		if (ua == null || ua.isBlank()) {
			return "Unknown device";
		}
		if (ua.contains("iPhone") || ua.contains("iPod")) {
			return "iPhone";
		}
		if (ua.contains("iPad")) {
			return "iPad";
		}
		if (ua.contains("Android")) {
			if (ua.contains("Mobile")) {
				return "Android phone";
			}
			return "Android tablet";
		}
		if (ua.contains("Windows Phone")) {
			return "Windows phone";
		}
		if (ua.contains("Macintosh") || ua.contains("Mac OS")) {
			return "Mac";
		}
		if (ua.contains("Windows")) {
			return "Windows PC";
		}
		if (ua.contains("Linux") && !ua.contains("Android")) {
			return "Linux PC";
		}
		if (ua.contains("CrOS")) {
			return "ChromeOS";
		}
		return "Desktop";
	}

	public static String os(String ua) {
		if (ua == null || ua.isBlank()) {
			return "Unknown OS";
		}
		if (ua.contains("Windows NT 10.0")) {
			return "Windows 10/11";
		}
		if (ua.contains("Windows NT 6.3")) {
			return "Windows 8.1";
		}
		if (ua.contains("Windows NT 6.1")) {
			return "Windows 7";
		}
		if (ua.contains("Windows")) {
			return "Windows";
		}
		if (ua.contains("iPhone OS ")) {
			Matcher m = Pattern.compile("iPhone OS (\\d+[_.]\\d+)").matcher(ua);
			return m.find() ? "iOS " + m.group(1).replace('_', '.') : "iOS";
		}
		if (ua.contains("iPad; CPU OS ")) {
			Matcher m = Pattern.compile("CPU OS (\\d+[_.]\\d+)").matcher(ua);
			return m.find() ? "iPadOS " + m.group(1).replace('_', '.') : "iPadOS";
		}
		if (ua.contains("Android ")) {
			Matcher m = Pattern.compile("Android (\\d+(?:\\.\\d+)?)").matcher(ua);
			return m.find() ? "Android " + m.group(1) : "Android";
		}
		if (ua.contains("Mac OS X")) {
			Matcher m = Pattern.compile("Mac OS X (\\d+[_.]\\d+(?:[_.]\\d+)?)").matcher(ua);
			return m.find() ? "macOS " + m.group(1).replace('_', '.') : "macOS";
		}
		if (ua.contains("CrOS")) {
			return "ChromeOS";
		}
		if (ua.contains("Linux")) {
			return "Linux";
		}
		return "Unknown OS";
	}

	public static String browser(String ua) {
		if (ua == null || ua.isBlank()) {
			return "Unknown browser";
		}
		if (ua.contains("Edg/") || ua.contains("Edge/")) {
			return "Edge";
		}
		if (ua.contains("OPR/") || ua.contains("Opera/")) {
			return "Opera";
		}
		if (ua.contains("Firefox/")) {
			return "Firefox";
		}
		if (ua.contains("Chrome/") && ua.contains("Safari/")) {
			return "Chrome";
		}
		if (ua.contains("Safari/") && ua.contains("Version/")) {
			return "Safari";
		}
		if (ua.contains("PostmanRuntime")) {
			return "Postman";
		}
		return "Other";
	}
}
