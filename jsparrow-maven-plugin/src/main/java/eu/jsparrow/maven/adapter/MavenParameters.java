package eu.jsparrow.maven.adapter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import eu.jsparrow.maven.i18n.Messages;

/**
 * A class for wrapping the parameters injected in the maven plugin.
 * 
 * @author Ardit Ymeri
 * @since 2.5.0
 *
 */
public class MavenParameters {

	private String profile = ""; //$NON-NLS-1$
	private String mode;
	private boolean useDefaultConfig = false;
	private String ruleId;
	private StatisticsMetadata statisticsMetadata;
	private boolean sendStatistics;
	private String selectedSources;
	private String reportDestinationPath;
	private String tempWorkspaceLocation;

	public MavenParameters(String mode, String profile, boolean useDefault, StatisticsMetadata statisticsMetadata,
			boolean sendStatistics, String selectedSources, String tempWorkspaceLocation) {
		this.mode = mode;
		this.profile = profile;
		this.useDefaultConfig = useDefault;
		this.statisticsMetadata = statisticsMetadata;
		this.sendStatistics = sendStatistics;
		this.selectedSources = selectedSources;
		this.tempWorkspaceLocation = tempWorkspaceLocation;
	}

	public MavenParameters(String mode) {
		this.mode = mode;
	}

	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	public String getProfile() {
		return profile;
	}

	public String getMode() {
		return mode;
	}

	public boolean getUseDefaultConfig() {
		return useDefaultConfig;
	}

	public Optional<String> getRuleId() {
		return Optional.ofNullable(ruleId)
			.filter(s -> !s.isEmpty());
	}

	public StatisticsMetadata getStatisticsMetadata() {
		return statisticsMetadata;
	}

	public void setStatisticsMetadata(StatisticsMetadata statisticsMetadata) {
		this.statisticsMetadata = statisticsMetadata;
	}

	public boolean isSendStatistics() {
		return sendStatistics;
	}

	public void setSendStatistics(boolean sendStatistics) {
		this.sendStatistics = sendStatistics;
	}

	public String getSelectedSources() {
		return this.selectedSources;
	}

	public String computeValidateReportDestinationPath(MavenProject project, String providedPath, Log log) {
		Build projectBuild = project.getBuild();
		String target = projectBuild.getDirectory();
		if(!providedPath.equals(target)) {
			Path path = Paths.get(providedPath);
			if(Files.exists(path)) {
				return providedPath;
			} else {
				log.warn(Messages.MavenParameters_missingReportsDestinationDirectory);
			}
		}
		return target;
	}
	
	public void setReportDestinationPath(String destination) {
		this.reportDestinationPath = destination;
	}

	public String getReportDestinationPath() {
		return this.reportDestinationPath;
	}

	public String getTempWorkspaceLocation() {
		return this.tempWorkspaceLocation;
	}
}
