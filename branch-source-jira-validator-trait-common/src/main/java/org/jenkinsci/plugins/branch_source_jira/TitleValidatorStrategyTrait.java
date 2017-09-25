package org.jenkinsci.plugins.branch_source_jira;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.plugins.git.GitSCM;
import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.model.JiraIssue;
import hudson.scm.SCMDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author witokondoria
 */
public abstract class TitleValidatorStrategyTrait extends SCMSourceTrait{

    private int jiraServerIdx = -1;

    /**
     * Constructor for stapler.
     */
    public TitleValidatorStrategyTrait(int jiraServerIdx){
        this.jiraServerIdx = jiraServerIdx;
    }

    public int getJiraServerIdx() {
        return this.jiraServerIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract void decorateContext(SCMSourceContext<?, ?> context);

    /**
     * Our descriptor.
     */
    public abstract static class CommitSkipDescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Pull request title filtering strategy (Jira validation)";
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicableToSCM(@NonNull SCMDescriptor<?> scm) {
            return scm instanceof GitSCM.DescriptorImpl;
        }

        public ListBoxModel doFillJiraServerIdxItems() {
            ListBoxModel r = new ListBoxModel();
            r.add("", "");
            JiraProjectProperty.DescriptorImpl jiraSitesDescriptor = (JiraProjectProperty.DescriptorImpl) Jenkins.getInstance().getDescriptor("hudson.plugins.jira.JiraProjectProperty");
            JiraSite[] sites = jiraSitesDescriptor.getSites();
            for (int i = 0; i < sites.length; i++) {
                JiraSite site = sites[i];
                r.add(site.getUrl().toString(), String.valueOf(i));
            }

            return r;
        }
    }

    /**
     * Filter that excludes pull requests with titles not matching a single open Jira ticket formatted as [JENKINS-1234]).
     */
    public abstract static class ExcludeTitlePRsSCMHeadFilter extends SCMHeadFilter {

        private int jiraServerIdx = -1;

        public ExcludeTitlePRsSCMHeadFilter(int jiraServerIdx) {
            this.jiraServerIdx = jiraServerIdx;
        }

        public boolean containsOpenTicket(String changeRequestTitle) {
            boolean containsSingleOpenTicket = false;
            int singleTicket = 0;
            JiraSite[] jiraSites = ((JiraProjectProperty.DescriptorImpl) Jenkins.getInstance().getDescriptor("hudson.plugins.jira.JiraProjectProperty")).getSites();
            if (jiraSites.length < this.jiraServerIdx) {
                return true; //Avoiding an ArrayOut of bounds (the jira server list had elements removed or was wiped). Saving a proper jiraServer id would be better that just the index
            } else {
                JiraSite jiraSite = jiraSites[this.jiraServerIdx];
                Pattern pattern = jiraSite.getIssuePattern();
                Matcher m = pattern.matcher(changeRequestTitle);
                while (m.find()) {
                    if (m.groupCount() == 2) {
                        singleTicket++;
                        if (singleTicket > 1) {
                            return false;
                        }
                        String id = m.group(1);
                        try {
                            Issue issue = jiraSite.getSession().getIssue(id);
                            if ((issue != null) && (issue.getResolution() == null)) { // switch to accepted/invalid statuses would be cool
                                containsSingleOpenTicket = true;
                            }
                        } catch (IOException e) {
                            return true; //So we do not exclude each pull request in case of an unaccesible Jira API
                        }
                    }
                }
                return containsSingleOpenTicket;
            }
        }

        @Override
        abstract public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead) throws IOException, InterruptedException;
    }
}
