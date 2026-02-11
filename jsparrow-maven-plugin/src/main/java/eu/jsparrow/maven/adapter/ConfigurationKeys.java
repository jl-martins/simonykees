package eu.jsparrow.maven.adapter;

/**
 * Constants serving as keys for the equinox BundleContext configuration. Any
 * change here must be reflected also in
 * {@link eu.jsparrow.standalone.RefactoringInvoker}.
 *
 * @since 2.6.0
 */
public class ConfigurationKeys {

	public static final String SELECTED_PROFILE = "PROFILE.SELECTED"; //$NON-NLS-1$
	public static final String USE_DEFAULT_CONFIGURATION = "DEFAULT.CONFIG"; //$NON-NLS-1$
	public static final String STANDALONE_MODE_KEY = "STANDALONE.MODE"; //$NON-NLS-1$
	public static final String INSTANCE_DATA_LOCATION_CONSTANT = "osgi.instance.area.default"; //$NON-NLS-1$
	public static final String FRAMEWORK_STORAGE_VALUE = "target/bundlecache"; //$NON-NLS-1$
	public static final String OSGI_INSTANCE_AREA_CONSTANT = "osgi.instance.area"; //$NON-NLS-1$
	public static final String DEBUG_ENABLED = "debug.enabled"; //$NON-NLS-1$
	public static final String ROOT_CONFIG_PATH = "ROOT.CONFIG.PATH"; //$NON-NLS-1$
	public static final String LIST_RULES_SELECTED_ID = "LIST.RULES.SELECTED.ID"; //$NON-NLS-1$
	public static final String DEMO_MODE_KEY = "DEMO.MODE"; //$NON-NLS-1$
	public static final String USER_DIR = "user.dir"; //$NON-NLS-1$
	public static final String ROOT_PROJECT_BASE_PATH = "ROOT.PROJECT.BASE.PATH"; //$NON-NLS-1$
	public static final String CONFIG_FILE_OVERRIDE = "CONFIG.FILE.OVERRIDE"; //$NON-NLS-1$
	public static final String FORMATTING_FILE = "formatting.file.path"; //$NON-NLS-1$
	public static final String PROXY_SETTINGS = "PROXY.SETTINGS"; //$NON-NLS-1$
	public static final String STATISTICS_START_TIME = "STATISTICS_START_TIME"; //$NON-NLS-1$
	public static final String STATISTICS_REPO_OWNER = "STATISTICS_REPO_OWNER"; //$NON-NLS-1$
	public static final String STATISTICS_REPO_NAME = "STATISTICS_REPO_NAME"; //$NON-NLS-1$
	public static final String STATISTICS_SEND = "STATISTICS_SEND"; //$NON-NLS-1$
	public static final String SELECTED_SOURCES = "SELECTED.SOURCES"; //$NON-NLS-1$
	public static final String REPORT_DESTINATION_PATH = "REPORT.DESTINATION.PATH"; //$NON-NLS-1$

	private ConfigurationKeys() {
		/*
		 * Hidding public constructor
		 */
	}

}
