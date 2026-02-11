package eu.jsparrow.standalone;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.jsparrow.i18n.Messages;
import eu.jsparrow.standalone.exceptions.StandaloneException;
import eu.jsparrow.standalone.util.ProxyUtils;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Andreja Sambolec, Matthias Webhofer
 * @since 2.1.1
 */
public class Activator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	public static final String PLUGIN_ID = "eu.jsparrow.standalone"; //$NON-NLS-1$

	private static final String LIST_RULES_SELECTED_ID_KEY = "LIST.RULES.SELECTED.ID"; //$NON-NLS-1$
	private static final String STANDALONE_MODE_KEY = "STANDALONE.MODE"; //$NON-NLS-1$

	private RefactoringInvoker refactoringInvoker;
	ListRulesUtil listRulesUtil;

	public Activator() {
		this(new RefactoringInvoker(), new ListRulesUtil());
	}

	public Activator(RefactoringInvoker refactoringInvoker, ListRulesUtil listRulesUtil) {
		this.refactoringInvoker = refactoringInvoker;
		this.listRulesUtil = listRulesUtil;
	}

	@Override
	public void start(BundleContext context) throws Exception {

		// TODO: Discuss whether this is necessary
		// // start jSparrow logging bundle
		// for (Bundle bundle : context.getBundles()) {
		// if ("eu.jsparrow.logging".equals(bundle.getSymbolicName())
		// //$NON-NLS-1$
		// && bundle.getState() != Bundle.ACTIVE) {
		// bundle.start();
		// break;
		// }
		// }

		// Put both together because it looks nicer
		String startMessage = String.format("%s", Messages.Activator_start); //$NON-NLS-1$
		logger.info(startMessage);
		registerShutdownHook();
		ProxyUtils.configureProxy(context);
		StandaloneMode mode = parseMode(context);
		String listRulesId = context.getProperty(LIST_RULES_SELECTED_ID_KEY);
		switch (mode) {
		case REFACTOR:
			refactor(context);
			break;
		case REPORT:
			runInReportMode(context);
			break;
		case LIST_RULES:
			listRules(listRulesId);
			break;
		case LIST_RULES_SHORT:
			listRulesUtil.listRulesShort();
			break;
		case TEST:
			break;
		default:
			String errorMsg = "No mode has been selected!"; //$NON-NLS-1$
			logger.error(errorMsg);
			setExitErrorMessageAndCleanUp(context, errorMsg);
		}
	}

	@Override
	public void stop(BundleContext context) {
		try {
			/* Unregister as a save participant */
			if (ResourcesPlugin.getWorkspace() != null) {
				ResourcesPlugin.getWorkspace()
					.forgetSavedTree(PLUGIN_ID);
				ResourcesPlugin.getWorkspace()
					.removeSaveParticipant(PLUGIN_ID);
			}
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
			logger.error(e.getMessage());
		} finally {
			cleanUp();
		}

		logger.info(Messages.Activator_stop);
	}

	private void listRules(String listRulesId) {
		if (listRulesId != null && !listRulesId.isEmpty()) {
			listRulesUtil.listRules(listRulesId);
		} else {
			listRulesUtil.listRules();
		}
	}

	private void refactor(BundleContext context) {
		try {
			refactoringInvoker.startRefactoring(context);
		} catch (StandaloneException e) {
			logger.debug(e.getMessage(), e);
			logger.error(e.getMessage());
			setExitErrorMessageAndCleanUp(context, e.getMessage());
		}
	}

	private void runInReportMode(BundleContext context) {
		try {
			refactoringInvoker.runInReportMode(context);
		} catch (StandaloneException e) {
			logger.debug(e.getMessage(), e);
			logger.error(e.getMessage());
			setExitErrorMessageAndCleanUp(context, e.getMessage());
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime()
			.addShutdownHook(new Thread(this::cleanUp));
	}

	private void cleanUp() {
		refactoringInvoker.cleanUp();
	}

	private StandaloneMode parseMode(BundleContext context) {
		String value = context.getProperty(STANDALONE_MODE_KEY);
		return StandaloneMode.fromString(value);
	}

	private EnvironmentInfo getEnvironmentInfo(BundleContext ctx) {
		if (ctx == null) {
			return null;
		}

		ServiceReference<?> infoRev = ctx.getServiceReference(EnvironmentInfo.class.getName());
		if (infoRev == null) {
			return null;
		}

		EnvironmentInfo envInfo = (EnvironmentInfo) ctx.getService(infoRev);
		if (envInfo == null) {
			return null;
		}
		ctx.ungetService(infoRev);

		return envInfo;
	}

	public void setExitErrorMessageAndCleanUp(BundleContext ctx, String exitMessage) {
		cleanUp();

		setExitErrorMessage(ctx, exitMessage);
	}

	public void setExitErrorMessage(BundleContext ctx, String exitMessage) {
		String key = "eu.jsparrow.standalone.exit.message"; //$NON-NLS-1$
		EnvironmentInfo envInfo = getEnvironmentInfo(ctx);
		if (envInfo != null) {
			envInfo.setProperty(key, exitMessage);
		} else {
			System.setProperty(key, exitMessage);
		}
	}
}
