package org.jenkinsci.plugins.p4.publish;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.p4.credentials.P4CredentialsImpl;
import org.jenkinsci.plugins.p4.tasks.PublishTask;
import org.jenkinsci.plugins.p4.workspace.Workspace;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Logger;

public class PublishNotifier extends Notifier {

	private static Logger logger = Logger.getLogger(PublishNotifier.class.getName());

	private final String credential;
	private final Workspace workspace;
	private final Publish publish;

	public String getCredential() {
		return credential;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public Publish getPublish() {
		return publish;
	}

	@DataBoundConstructor
	public PublishNotifier(String credential, Workspace workspace, Publish publish) {
		this.credential = credential;
		this.workspace = workspace;
		this.publish = publish;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		// return early if publish not required
		if (publish.isOnlyOnSuccess() && build.getResult() != Result.SUCCESS) {
			return true;
		}

		FilePath filePath = build.getWorkspace();
		if (filePath == null) {
			logger.warning("FilePath is null!");
			return false;
		}

		Workspace ws = (Workspace) workspace.clone();
		try {
			EnvVars envVars = build.getEnvironment(listener);
			ws.setExpand(envVars);
			ws.setRootPath(filePath.getRemote());
			String desc = publish.getDescription();
			desc = ws.getExpand().format(desc, false);
			publish.setExpandedDesc(desc);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create task
		PublishTask task = new PublishTask(publish);
		task.setListener(listener);
		task.setCredential(credential);
		task.setWorkspace(ws);

		boolean success = filePath.act(task);
		return success;
	}

	public static DescriptorImpl descriptor() {
		Jenkins j = Jenkins.getInstance();
		if (j != null) {
			j.getDescriptorByType(PublishNotifier.DescriptorImpl.class);
		}
		return null;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Perforce: Publish assets";
		}

		public ListBoxModel doFillCredentialItems() {
			return P4CredentialsImpl.doFillCredentialItems();
		}
	}
}
