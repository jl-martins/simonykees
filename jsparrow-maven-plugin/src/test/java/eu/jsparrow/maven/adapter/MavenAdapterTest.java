package eu.jsparrow.maven.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class MavenAdapterTest {

	private MavenProject project;
	private Log log;
	private MavenAdapter mavenAdapter;
	private Path path;
	private WorkingDirectory workingDirectory;
	private String groupId = "group-id";
	private String artifactId = "artifact-id";
	Properties properties;
	private File jsparrowTempDirectory;
	private File jsparrowYml;
	private File formatterFile;
	private File projectBaseDir;
	private Proxy proxy;
	private StatisticsMetadata statisticsMetadata;

	@Rule
	public TemporaryFolder directory = new TemporaryFolder();

	@Before
	public void setUp() throws IOException {
		project = mock(MavenProject.class);
		log = mock(Log.class);
		path = mock(Path.class);
		workingDirectory = mock(WorkingDirectory.class);
		mavenAdapter = new TestableMavenAdapter(project, log);

		jsparrowTempDirectory = directory.newFolder("temp_jSparrow");
		projectBaseDir = directory.newFolder("project_base_dir");
		jsparrowYml = new File(projectBaseDir.getPath() + File.separator + "jsparrow.yml");
		jsparrowYml.createNewFile();

		formatterFile = Files.createTempFile("formatting", ".xml")
			.toFile();

		statisticsMetadata = mock(StatisticsMetadata.class);

		proxy = mock(Proxy.class);

		when(project.getGroupId()).thenReturn(groupId);
		when(project.getArtifactId()).thenReturn(artifactId);
		when(project.getBasedir()).thenReturn(projectBaseDir);
		properties = mock(Properties.class);
		when(project.getProperties()).thenReturn(properties);

	}

	@Test
	public void prepareConfiguration_additionalConfigurationNotNull() throws Exception {
		final MavenParameters config = mock(MavenParameters.class);
		when(config.getUseDefaultConfig()).thenReturn(false);
		when(config.getMode()).thenReturn(""); //$NON-NLS-1$
		when(config.getProfile()).thenReturn(""); //$NON-NLS-1$
		when(config.getRuleId()).thenReturn(Optional.empty());
		when(config.getStatisticsMetadata()).thenReturn(statisticsMetadata);
		when(config.getSelectedSources()).thenReturn("");

		mavenAdapter.addInitialConfiguration(config); // $NON-NLS-1$

		verify(config).getUseDefaultConfig();
		verify(config).getMode();
		verify(config).getProfile();
		verify(config).getSelectedSources();
	}

	@Test
	public void prepareWorkingDirectory_directoryDoesNotExistAndMkdirsNotWorking() throws Exception {
		jsparrowTempDirectory = mock(File.class);
		when(jsparrowTempDirectory.exists()).thenReturn(false);
		when(jsparrowTempDirectory.mkdirs()).thenReturn(false);

		assertThrows(InterruptedException.class, () -> mavenAdapter.prepareWorkingDirectory(""));
	}

	@Test
	public void prepareWorkingDirectory_directoryDoesNotExistAndMkdirsIsWorking() throws Exception {

		String absolutePath = "somePath";
		jsparrowTempDirectory = mock(File.class);
		when(jsparrowTempDirectory.exists()).thenReturn(false);
		when(jsparrowTempDirectory.mkdirs()).thenReturn(true);
		when(jsparrowTempDirectory.getAbsolutePath()).thenReturn(absolutePath);

		mavenAdapter.prepareWorkingDirectory("");

		verify(jsparrowTempDirectory).getAbsolutePath();
		assertEquals(absolutePath, mavenAdapter.getConfiguration()
			.getOrDefault("osgi.instance.area", "asdf")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void setUp_listOfProjects() throws Exception {
		MavenParameters mavenParameters = new MavenParameters("list-rules");

		when(workingDirectory.isJsparrowStarted(any(String.class))).thenReturn(false);
		when(project.getPackaging()).thenReturn("jar");
		when(path.toFile()).thenReturn(jsparrowYml);

		mavenAdapter.setUpConfiguration(mavenParameters, Collections.singletonList(project), jsparrowYml, jsparrowYml,
				formatterFile, Stream.of(proxy));

		Map<String, String> configurations = mavenAdapter.getConfiguration();

		assertTrue(configurations.containsKey(ConfigurationKeys.ROOT_CONFIG_PATH));
		assertEquals(jsparrowYml.getAbsolutePath(), configurations.get(ConfigurationKeys.ROOT_CONFIG_PATH));

		assertTrue(configurations.containsKey(ConfigurationKeys.ROOT_PROJECT_BASE_PATH));
		assertEquals(projectBaseDir.getAbsolutePath(), configurations.get(ConfigurationKeys.ROOT_PROJECT_BASE_PATH));

		assertTrue(configurations.containsKey(ConfigurationKeys.FORMATTING_FILE));
		assertEquals(formatterFile.getAbsolutePath(), configurations.get(ConfigurationKeys.FORMATTING_FILE));
	}

	@Test
	public void setUp_jsparrowAlreadyRunning() throws Exception {
		MavenParameters mavenParameters = new MavenParameters("list-rules");

		when(workingDirectory.isJsparrowStarted(any(String.class))).thenReturn(true);

		assertThrows(MojoExecutionException.class,
				() -> mavenAdapter.setUpConfiguration(mavenParameters, Collections.singletonList(project), jsparrowYml,
						jsparrowYml,
						formatterFile, Stream.of(proxy)));
	}

	@Test
	public void setUp_initialConfiguration() throws InterruptedException {
		String expectedMode = "list-rules";
		String expectedBootFrameworkDelegation = "javax.*,org.xml.*,sun.*,com.sun.*,jdk.internal.reflect,jdk.internal.reflect.*";
		MavenParameters mavenParameters = new MavenParameters(expectedMode);

		mavenAdapter.setUpConfiguration(mavenParameters);

		Map<String, String> configuration = mavenAdapter.getConfiguration();
		assertEquals(expectedMode, configuration.getOrDefault("STANDALONE.MODE", expectedMode));
		assertEquals(expectedBootFrameworkDelegation, configuration.get("org.osgi.framework.bootdelegation"));
	}

	class TestableMavenAdapter extends MavenAdapter {

		public TestableMavenAdapter(MavenProject project, Log log) {
			super(project, log);
		}

		@Override
		protected File createJsparrowTempDirectory(String lcoation) {
			return jsparrowTempDirectory;
		}

		@Override
		protected void setSystemProperty(String key, String value) {

		}

		@Override
		protected WorkingDirectory createWorkingDirectory(File directory, String tempWorkspaceLocation) {
			return workingDirectory;
		}
	}
}
