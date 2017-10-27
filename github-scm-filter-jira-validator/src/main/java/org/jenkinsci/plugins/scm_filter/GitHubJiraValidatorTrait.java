package org.jenkinsci.plugins.scm_filter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMBuilder;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceRequest;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author witokondoria
 */
public class GitHubJiraValidatorTrait extends JiraValidatorTrait {

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitHubJiraValidatorTrait(int jiraServerIdx) {
        super(jiraServerIdx);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (getJiraServerIdx() > -1) {
            context.withFilter(new GitHubJiraValidatorTrait.ExcludeInvalidTitlePRsSCMHeadFilter(super.getJiraServerIdx()));
        }
    }
    /**
     * Our descriptor.
     */
    @Extension
    @SuppressWarnings("unused") // instantiated by Jenkins
    public static class DescriptorImpl extends JiraValidatorDescriptorImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return super.getDisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicableToBuilder(@NonNull Class<? extends SCMBuilder> builderClass) {
            return GitHubSCMBuilder.class.isAssignableFrom(builderClass);
        }

        public ListBoxModel doFillJiraServerIdxItems() {
            return super.doFillJiraServerIdxItems();
        }
    }

    /**
     * Filter that excludes pull requests with titles not matching a single open Jira ticket formatted as [JENKINS-1234]).
     */
    public static class ExcludeInvalidTitlePRsSCMHeadFilter extends ExcludeTitlePRsSCMHeadFilter{

        public ExcludeInvalidTitlePRsSCMHeadFilter(int jiraServerIdx) {
            super(jiraServerIdx);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead) throws IOException, InterruptedException {
            if (scmHead instanceof PullRequestSCMHead) {
                Iterable<GHPullRequest> pulls = ((GitHubSCMSourceRequest) scmSourceRequest).getPullRequests();
                Iterator<GHPullRequest> pullIterator = pulls.iterator();
                while (pullIterator.hasNext()) {
                    GHPullRequest pull = pullIterator.next();
                    if (("PR-" + pull.getNumber()).equals(scmHead.getName())) {
                        String title = pull.getTitle();
                        return !super.containsOpenTicket(title);
                    }
                }
            }
            return false;
        }
    }
}
