package org.jenkinsci.plugins.scm_filter;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketGitSCMBuilder;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSourceRequest;
import com.cloudbees.jenkins.plugins.bitbucket.PullRequestSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketPullRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.Symbol;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author witokondoria
 */
public class BitbucketJiraValidatorTrait extends JiraValidatorTrait{

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public BitbucketJiraValidatorTrait(int jiraServerIdx) {
        super(jiraServerIdx);
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (getJiraServerIdx() > -1) {
            context.withFilter(new BitbucketJiraValidatorTrait.ExcludeInvalidTitlePRsSCMHeadFilter(super.getJiraServerIdx()));
        }
    }

    /**
     * Our descriptor.
     */
    @Extension @Symbol("bitbucketJiraValidatorTrait")
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
            return BitbucketGitSCMBuilder.class.isAssignableFrom(builderClass);
        }

    }

    /**
     * Filter that excludes pull requests with titles not matching a single open Jira ticket formatted as [JENKINS-1234]).
     */
    public static class ExcludeInvalidTitlePRsSCMHeadFilter extends ExcludeTitlePRsSCMHeadFilter {

        public ExcludeInvalidTitlePRsSCMHeadFilter(int jiraServerIdx) {
            super(jiraServerIdx);
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead) throws IOException, InterruptedException {
            if (scmHead instanceof PullRequestSCMHead) {
                Iterable<BitbucketPullRequest> pulls = ((BitbucketSCMSourceRequest) scmSourceRequest).getPullRequests();
                Iterator<BitbucketPullRequest> pullIterator = pulls.iterator();
                while (pullIterator.hasNext()) {
                    BitbucketPullRequest pull = pullIterator.next();
                    if (pull.getSource().getBranch().getName().equals(scmHead.getName())) {
                        String title = pull.getTitle();
                        return !super.containsOpenTicket(title);
                    }
                }
            }
            return false;
        }
    }
}
