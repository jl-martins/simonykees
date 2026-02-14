package eu.jsparrow.maven.adapter;

import static eu.jsparrow.maven.adapter.ConfigurationKeys.CONFIG_FILE_OVERRIDE;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.DEBUG_ENABLED;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.FORMATTING_FILE;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.FRAMEWORK_STORAGE_VALUE;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.INSTANCE_DATA_LOCATION_CONSTANT;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.LIST_RULES_SELECTED_ID;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.OSGI_INSTANCE_AREA_CONSTANT;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.PROXY_SETTINGS;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.ROOT_CONFIG_PATH;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.ROOT_PROJECT_BASE_PATH;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.SELECTED_PROFILE;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.STANDALONE_MODE_KEY;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.STATISTICS_REPO_NAME;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.STATISTICS_REPO_OWNER;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.STATISTICS_SEND;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.STATISTICS_START_TIME;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.USER_DIR;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.USE_DEFAULT_CONFIGURATION;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.SELECTED_SOURCES;
import static eu.jsparrow.maven.adapter.ConfigurationKeys.REPORT_DESTINATION_PATH;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Constants;

import eu.jsparrow.maven.i18n.Messages;
import eu.jsparrow.maven.util.MavenProjectUtil;
import eu.jsparrow.maven.util.ProxyUtil;

/**
 * Sets up the configuration used for starting the equinox framework.
 * Distinguishes the properties of different projects.
 * 
 * @author Andreja Sambolec, Matthias Webhofer, Ardit Ymeri
 * @since 2.5.0
 *
 */
public class MavenAdapter {

	public static final String DOT = "."; //$NON-NLS-1$

	private Log log;
	private Map<String, String> configuration = new HashMap<>();
	private MavenProject rootProject;
	private Set<String> sessionProjects;
	private boolean jsparrowAlreadyRunningError = false;

	public MavenAdapter(MavenProject rootProject, Log log) {
		this.rootProject = rootProject;
		this.log = log;
		this.sessionProjects = new HashSet<>();
	}

	/**
	 * Creates a map with the required configurations for starting the equinox
	 * framework based on the provided {@link MavenParameter}. Additionally,
	 * adds the all projects related information to the configuration map.
	 * 
	 * @param parameters
	 *            a set of parameters provided from the mojo.
	 * @param projects
	 *            the list of all projects in the current session
	 * @param configFileOverride
	 *            path to the provided yml configuration file.
	 * @param fallbackConfigFile
	 *            the default {@code jsparrow.yml} file.
	 * @param proxies
	 *            list of proxy configurations for equinox
	 * @return an instance of {@link WorkingDirectory} for managing the working
	 *         directory of the equinox framework.
	 * @throws InterruptedException
	 *             if the working directory cannot be created
	 * @throws MojoExecutionException
	 *             if jSparrow is already started in the root project of the
	 *             current session.
	 */
	public WorkingDirectory setUpConfiguration(MavenParameters parameters, List<MavenProject> projects,
			File configFileOverride, File fallbackConfigFile, File formatterFile, Stream<Proxy> proxies)
			throws InterruptedException, MojoExecutionException {

		log.info(Messages.MavenAdapter_setUpConfiguration);

		setProjectIds(projects);
		configuration.put(PROXY_SETTINGS, ProxyUtil.getSettingsStringFrom(proxies));
		if(parameters.getReportDestinationPath() != null) {
			configuration.put(REPORT_DESTINATION_PATH, parameters.getReportDestinationPath());
		}

		WorkingDirectory workingDirectory = setUpConfiguration(parameters);
		String rootProjectIdentifier = MavenProjectUtil.findProjectIdentifier(rootProject);

		if (workingDirectory.isJsparrowStarted(rootProjectIdentifier)) {
			jsparrowAlreadyRunningError = true;
			log.error(NLS.bind(Messages.MavenAdapter_jSparrowAlreadyRunning, rootProject.getArtifactId()));
			throw new MojoExecutionException(Messages.MavenAdapter_jSparrowIsAlreadyRunning);
		}

		configuration.put(ROOT_CONFIG_PATH, fallbackConfigFile.getAbsolutePath());
		configuration.put(CONFIG_FILE_OVERRIDE,
				(configFileOverride == null) ? null : configFileOverride.getAbsolutePath());
		configuration.put(FORMATTING_FILE, formatterFile == null ? null : formatterFile.getAbsolutePath());
		configuration.put(ROOT_PROJECT_BASE_PATH, rootProject.getBasedir()
			.getAbsolutePath());
		configuration.put(STATISTICS_SEND, Boolean.toString(parameters.isSendStatistics()));

		workingDirectory.lockProjects();

		log.info(Messages.MavenAdapter_configurationSetUp);
		return workingDirectory;
	}

	/**
	 * Creates a map with the required configurations for starting the equinox
	 * framework based on the provided {@link MavenParameter}.
	 * 
	 * @param parameters
	 *            a set of parameters provided from the mojo.
	 * @return an instance of {@link WorkingDirectory} for managing the working
	 *         directory of the equinox.
	 * @throws InterruptedException
	 *             if the working directory cannot be created
	 */
	public WorkingDirectory setUpConfiguration(MavenParameters parameters) throws InterruptedException {
		addInitialConfiguration(parameters);
		return prepareWorkingDirectory(parameters.getTempWorkspaceLocation());
	}

	public WorkingDirectory setUpConfiguration(MavenParameters parameters, Stream<Proxy> proxies)
			throws InterruptedException {
		configuration.put(PROXY_SETTINGS, ProxyUtil.getSettingsStringFrom(proxies));
		return setUpConfiguration(parameters);
	}

	void addInitialConfiguration(MavenParameters config) {
		boolean useDefaultConfig = config.getUseDefaultConfig();
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		configuration.put(Constants.FRAMEWORK_STORAGE, FRAMEWORK_STORAGE_VALUE);
		configuration.put(INSTANCE_DATA_LOCATION_CONSTANT, System.getProperty(USER_DIR));

		/*
		 * This is solution B from this article:
		 * https://spring.io/blog/2009/01/19/exposing-the-boot-classpath-in-
		 * osgi/
		 */
		configuration.put(Constants.FRAMEWORK_BOOTDELEGATION, "javax.*,org.xml.*,sun.*,com.sun.*,jdk.internal.reflect,jdk.internal.reflect.*"); //$NON-NLS-1$
		configuration.put(DEBUG_ENABLED, Boolean.toString(log.isDebugEnabled()));
		configuration.put(STANDALONE_MODE_KEY, config.getMode());
		configuration.put(SELECTED_PROFILE, config.getProfile());
		configuration.put(USE_DEFAULT_CONFIGURATION, Boolean.toString(useDefaultConfig));
		configuration.put(SELECTED_SOURCES, config.getSelectedSources());

		StatisticsMetadata statisticsMetadata = config.getStatisticsMetadata();
		if (statisticsMetadata != null && statisticsMetadata.isValid()) {
			configuration.put(STATISTICS_START_TIME, statisticsMetadata.getStartTime()
				.toString());
			configuration.put(STATISTICS_REPO_OWNER, statisticsMetadata.getRepoOwner());
			configuration.put(STATISTICS_REPO_NAME, statisticsMetadata.getRepoName());
		}
		config.getRuleId()
			.ifPresent(ruleId -> configuration.put(LIST_RULES_SELECTED_ID, ruleId));
	}

	/**
	 * Creates and prepares the temporary working directory and sets its path in
	 * system properties and equinox configuration
	 * 
	 * @param configuration
	 * 
	 * @throws InterruptedException
	 */
	public WorkingDirectory prepareWorkingDirectory(String tempWorkspaceLocation) throws InterruptedException {

		log.debug(Messages.MavenAdapter_prepareWorkingDirectory);
		File directory = createJsparrowTempDirectory(tempWorkspaceLocation);

		if (directory.exists() || directory.mkdirs()) {
			String directoryAbsolutePath = directory.getAbsolutePath();
			setSystemProperty(USER_DIR, directoryAbsolutePath);
			configuration.put(OSGI_INSTANCE_AREA_CONSTANT, directoryAbsolutePath);

			String loggerInfo = NLS.bind(Messages.MavenAdapter_setUserDir, directoryAbsolutePath);
			log.debug(loggerInfo);
		} else {
			throw new InterruptedException(Messages.MavenAdapter_couldnotCreateTempFolder);
		}
		log.debug(Messages.MavenAdapter_workingDirectoryPrepared);
		logTempWorkspaceContents(directory);
		return createWorkingDirectory(directory, tempWorkspaceLocation);
	}

	protected WorkingDirectory createWorkingDirectory(File directory, String tempWorkspaceLocation) {
		return new WorkingDirectory(directory, sessionProjects, tempWorkspaceLocation, log);
	}

	protected void setSystemProperty(String key, String directoryAbsolutePath) {
		System.setProperty(key, directoryAbsolutePath);
	}

	protected File createJsparrowTempDirectory(String tempWorkspaceLocation) {
		return new File(WorkingDirectory.calculateJsparrowTempFolderPath(tempWorkspaceLocation)).getAbsoluteFile();
	}

	public void setProjectIds(List<MavenProject> allProjects) {
		log.debug(Messages.MavenAdapter_setProjectIds);
		this.sessionProjects = allProjects.stream()
			.map(MavenProjectUtil::findProjectIdentifier)
			.collect(Collectors.toSet());
		log.debug(Messages.MavenAdapter_projectIdsSet);
	}

	public boolean isJsparrowRunningFlag() {
		return jsparrowAlreadyRunningError;
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	private void logTempWorkspaceContents(File workspaceRoot) {
		log.debug("Workspace directory initial contents:"); //$NON-NLS-1$
		try {
			Files.walkFileTree(workspaceRoot.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					log.debug(file.toString());
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error("Cannot log initial workpsace directory contents", e); //$NON-NLS-1$
		}

	}

}
