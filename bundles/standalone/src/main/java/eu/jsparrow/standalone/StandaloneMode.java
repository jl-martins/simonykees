package eu.jsparrow.standalone;

import org.apache.commons.lang3.StringUtils;

public enum StandaloneMode {
	TEST,
	REFACTOR,
	REPORT,
	LIST_RULES,
	LIST_RULES_SHORT,
	NONE;

	@SuppressWarnings("nls")
	public static StandaloneMode fromString(String value) {
		if (value == null) {
			return NONE;
		}
		String upperCaseValue = StringUtils.upperCase(value);

		switch (upperCaseValue) {
		case "TEST":
			return TEST;
		case "REFACTOR":
			return REFACTOR;
		case "REPORT":
			return REPORT;
		case "LIST_RULES":
			return LIST_RULES;
		case "LIST_RULES_SHORT":
			return LIST_RULES_SHORT;
		default:
			return NONE;

		}
	}
}