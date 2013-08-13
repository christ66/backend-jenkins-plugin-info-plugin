package org.jenkinsci.confluence.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.confluence.plugins.exception.PluginHttpException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.SubRenderer;
import com.atlassian.renderer.v2.macro.MacroException;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsPluginInfoMacroTest {

	@Mock
	private HttpRetrievalService httpRetrievalService = Mockito
			.mock(HttpRetrievalService.class);

	@Mock
	private JenkinsRetriever jenkinsRetriever = Mockito
			.mock(JenkinsRetriever.class);

	@Mock
	private SubRenderer subRenderer = Mockito.mock(SubRenderer.class);

	@InjectMocks
	JenkinsPluginInfoMacro macro = new JenkinsPluginInfoMacro();

	@Before
	public void buildUp() throws IOException, PluginHttpException,
			ParseException {
		// return the correct update center details
		String jsonString = loadTextFile("update-center.json");
		JSONParser parser = new JSONParser();
		JSONObject updateCenter = (JSONObject) parser.parse(jsonString);
		Mockito.when(
				jenkinsRetriever.retrieveUpdateCenterDetails(Mockito
						.any(HttpRetrievalService.class))).thenReturn(
				updateCenter);
		// ensure the string to be rendered is returned as-is to the test
		Mockito.when(
				subRenderer.render(Mockito.anyString(),
						Mockito.any(RenderContext.class))).thenAnswer(
				new Answer<String>() {
					// @Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						Object[] args = invocation.getArguments();
						return (String) args[0];
					}
				});
	}

	@Test
	public void validGithub() throws MacroException, IOException,
			PluginHttpException {
		// return the correct stats center details
		String statsString = loadTextFile("cucumber-reports.stats.json");
		Mockito.when(
				jenkinsRetriever.retrieveStatsResponse(
						Mockito.any(HttpRetrievalService.class),
						Mockito.anyString())).thenReturn(statsString);
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = renderExpectedOutputCucumberReports(true);
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void validGithubWithDependencies() throws MacroException,
			IOException, PluginHttpException {
		// return the correct stats center details
		String statsString = loadTextFile("analysis-collector.stats.json");
		Mockito.when(
				jenkinsRetriever.retrieveStatsResponse(
						Mockito.any(HttpRetrievalService.class),
						Mockito.anyString())).thenReturn(statsString);
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "analysis-collector");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = renderExpectedOutputAnalysisCollector(true);
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void validNonGithub() throws MacroException, IOException,
			PluginHttpException {
		// return the correct stats center details
		String statsString = loadTextFile("AntepediaReporter-CI-plugin.stats.json");
		Mockito.when(
				jenkinsRetriever.retrieveStatsResponse(
						Mockito.any(HttpRetrievalService.class),
						Mockito.anyString())).thenReturn(statsString);
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "AntepediaReporter-CI-plugin");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = renderExpectedOutputAntepediaReporter();
		System.out.println("output: " + output);
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void pluginHttpExceptionThrown() throws MacroException, IOException,
			PluginHttpException, ParseException {
		Mockito.when(
				jenkinsRetriever.retrieveUpdateCenterDetails(Mockito
						.any(HttpRetrievalService.class))).thenThrow(
				new PluginHttpException(300));
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = "h4. Plugin Information\n{warning:title=Cannot Load Update Center}\nerror 300 loading update-center.json\n{warning}\n";
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void parseExceptionThrown() throws MacroException, IOException,
			PluginHttpException, ParseException {
		Mockito.when(
				jenkinsRetriever.retrieveUpdateCenterDetails(Mockito
						.any(HttpRetrievalService.class))).thenThrow(
				new ParseException(22341, "ParseException message"));
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = "h4. Plugin Information\n{warning:title=Cannot Load Update Center}\nParseException: error type 22341\n{warning}\n";
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void ioExceptionThrown() throws MacroException, IOException,
			PluginHttpException, ParseException {
		Mockito.when(
				jenkinsRetriever.retrieveUpdateCenterDetails(Mockito
						.any(HttpRetrievalService.class))).thenThrow(
				new IOException("IOException message"));
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = "h4. Plugin Information\n{warning:title=Cannot Load Update Center}\nIOException: IOException message\n{warning}\n";
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void getStatsParserPluginHttpExceptionThrown()
			throws MacroException, IOException, PluginHttpException {
		Mockito.when(
				jenkinsRetriever.retrieveStatsResponse(
						Mockito.any(HttpRetrievalService.class),
						Mockito.anyString())).thenThrow(
				new PluginHttpException(300));
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = renderExpectedOutputCucumberReports(false);
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void getStatsParserOtherExceptionThrown() throws MacroException,
			IOException, PluginHttpException {
		Mockito.when(
				jenkinsRetriever.retrieveStatsResponse(
						Mockito.any(HttpRetrievalService.class),
						Mockito.anyString())).thenThrow(
				new NullPointerException("npe"));
		macro.setHttpRetrievalService(httpRetrievalService);
		macro.setSubRenderer(subRenderer);
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "cucumber-reports");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = renderExpectedOutputCucumberReports(false);
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void missingPlugin() throws MacroException {
		Map<String, String> inputMap = new HashMap<String, String>();
		inputMap.put("pluginId", "does-not-exist");
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		String expectedOutput = "h4. Plugin Information\n|| No Information For This Plugin ||\n";
		Assert.assertEquals(expectedOutput, output);
	}

	@Test
	public void noPluginSpecified() throws MacroException {
		JenkinsPluginInfoMacro macro = new JenkinsPluginInfoMacro();
		Map<String, String> inputMap = new HashMap<String, String>();
		RenderContext renderContext = new RenderContext();
		String output = macro.execute(inputMap, null, renderContext);
		Assert.assertEquals("No plugin specified.", output);
	}

	@Test
	public void testIsInline() {
		Assert.assertFalse(macro.isInline());
	}

	@Test
	public void testHasBody() {
		Assert.assertFalse(macro.hasBody());
	}

	@Test
	public void testGetBodyRenderMode() {
		Assert.assertEquals(RenderMode.NO_RENDER, macro.getBodyRenderMode());
	}

	private String loadTextFile(String fileName)
			throws UnsupportedEncodingException {
		final InputStream stream = JenkinsPluginInfoMacroTest.class
				.getResourceAsStream("/" + fileName);
		Reader reader = new InputStreamReader(stream, "UTF-8");
		String json = null;
		try {
			json = IOUtils.toString(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}

	private String renderExpectedOutputCucumberReports(boolean renderStatistics) {
		WikiWriter toBeRendered = new WikiWriter().h4("Plugin Information");
		String name = "cucumber-reports";
		boolean isGithub = true;
		String prevVer = "0.0.20";
		String githubBaseUrl = "https://github.com/jenkinsci/cucumber-reports-plugin/compare/cucumber-reports";
		String version = "0.0.21";
		String requiredCore = "1.504";
		String buildDate = "Apr 14, 2013";
		String sourceDir = "cucumber-reports-plugin";
		String jiraComponent = "cucumber-reports";
		String devId = "kingsleyh";
		String devEmail = "kingsley@masterthought.net";
		String devName = "Kingsley Hendrickse";
		{// first row
			toBeRendered.append("|| Plugin ID | ").append(name)
					.append(" || Changes | [In Latest Release|");
			if (isGithub) {
				toBeRendered.append(githubBaseUrl).append('-').append(prevVer)
						.append("...").append(name).append('-').append(version)
						.append("]\n[Since Latest Release|")
						.append(githubBaseUrl).append('-').append(version)
						.append("...master]");
			}
			toBeRendered.append(" |\n ");
		}

		{// second row
			toBeRendered
					.append(" || Latest Release \\\\ Latest Release Date \\\\ Required Core \\\\ Dependencies | ")
					.href(version,
							"http://updates.jenkins-ci.org/latest/" + name
									+ ".hpi")
					.append(" ")
					.href("(archives)",
							"http://updates.jenkins-ci.org/download/plugins/"
									+ name + "/")
					.br()
					.append(buildDate)
					.br()
					.href(requiredCore,
							"http://updates.jenkins-ci.org/download/war/"
									+ requiredCore + "/jenkins.war")
					.br()
					.append("")
					.append(" || Source Code \\\\ Issue Tracking \\\\ Maintainer(s) | ")
					// JG this line too
					.append(isGithub ? "[GitHub|https://github.com/jenkinsci/"
							: "[Subversion|https://svn.jenkins-ci.org/trunk/hudson/plugins/")
					.append(sourceDir)
					.append(']')
					.br()
					.append("[Open Issues|http://issues.jenkins-ci.org/secure/IssueNavigator.jspa?mode=hide&reset=true&jqlQuery=project+%3D+JENKINS+AND+status+in+%28Open%2C+%22In+Progress%22%2C+Reopened%29+AND+component+%3D+%27")
					.append(jiraComponent).append("%27]").br();

			WikiWriter devString = new WikiWriter();

			if (devString.length() > 0) {
				devString.append("\n");
			}
			if (!devEmail.equals("n/a")) {
				devString.href(devName, "mailto:" + devEmail);
			} else {
				devString.append(devName);
			}
			devString.print(" (id: %s)", devId);
			if (devString.length() == 0) {
				devString.append("(not specified)");
			}
			toBeRendered.append(devString.toString());
			toBeRendered.append(" |\n ");
		}

		{// third row
			if (renderStatistics) {
				toBeRendered
						.append(" || Usage | ")
						.image("http://chart.apis.google.com/chart?cht=lc&chxl=1:%7C03%7C04%7C05%7C06%7C2:%7CMonth&chxp=2,50&chxr=0,0,749%7C1,0,12&chxs=1,676767,12&chxt=y,x,x&chs=300x225&chds=0,749&chd=t:382,551,675,749&chg=10,-1,0,0&chls=4&chco=d24939&chtt=cucumber-reports+-+installations");
				toBeRendered.append(" || Installations | ");
				toBeRendered
						.append("2013-Mar 382\n2013-Apr 551\n2013-May 675\n2013-Jun 749\n");

				toBeRendered.append("[(?)|Plugin Installation Statistics]");
				toBeRendered.append("|\n");
			}
		}

		return toBeRendered.toString();
	}

	private String renderExpectedOutputAnalysisCollector(
			boolean renderStatistics) {
		WikiWriter toBeRendered = new WikiWriter().h4("Plugin Information");
		String name = "analysis-collector";
		boolean isGithub = true;
		String prevVer = "1.35";
		String githubBaseUrl = "https://github.com/jenkinsci/analysis-collector-plugin/compare/analysis-collector";
		String version = "1.36";
		String requiredCore = "1.424";
		String buildDate = "Jul 13, 2013";
		String sourceDir = "analysis-collector-plugin";
		String jiraComponent = "analysis-collector";
		String devId = "drulli";
		String devEmail = "ullrich.hafner@gmail.com";
		String devName = "Ulli Hafner";
		{// first row
			toBeRendered.append("|| Plugin ID | ").append(name)
					.append(" || Changes | [In Latest Release|");
			if (isGithub) {
				toBeRendered.append(githubBaseUrl).append('-').append(prevVer)
						.append("...").append(name).append('-').append(version)
						.append("]\n[Since Latest Release|")
						.append(githubBaseUrl).append('-').append(version)
						.append("...master]");
			}
			toBeRendered.append(" |\n ");
		}

		{// second row
			toBeRendered
					.append(" || Latest Release \\\\ Latest Release Date \\\\ Required Core \\\\ Dependencies | ")
					.href(version,
							"http://updates.jenkins-ci.org/latest/" + name
									+ ".hpi")
					.append(" ")
					.href("(archives)",
							"http://updates.jenkins-ci.org/download/plugins/"
									+ name + "/")
					.br()
					.append(buildDate)
					.br()
					.href(requiredCore,
							"http://updates.jenkins-ci.org/download/war/"
									+ requiredCore + "/jenkins.war")
					.br()
					.append("[tasks|https://wiki.jenkins-ci.org/display/JENKINS/Task+Scanner+Plugin] (version:4.36, optional) \\\\ [findbugs|https://wiki.jenkins-ci.org/display/JENKINS/FindBugs+Plugin] (version:4.48, optional) \\\\ [pmd|https://wiki.jenkins-ci.org/display/JENKINS/PMD+Plugin] (version:3.34, optional) \\\\ [checkstyle|https://wiki.jenkins-ci.org/display/JENKINS/Checkstyle+Plugin] (version:3.35, optional) \\\\ [maven-plugin|https://wiki.jenkins-ci.org/display/JENKINS/Maven+Project+Plugin] (version:1.424, optional) \\\\ [dry|https://wiki.jenkins-ci.org/display/JENKINS/DRY+Plugin] (version:2.34, optional) \\\\ [ant|https://wiki.jenkins-ci.org/display/JENKINS/Ant+Plugin] (version:1.1) \\\\ [analysis-core|https://wiki.jenkins-ci.org/display/JENKINS/Static+Code+Analysis+Plug-ins] (version:1.50) \\\\ [token-macro|https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin] (version:1.5.1, optional) \\\\ [dashboard-view|https://wiki.jenkins-ci.org/display/JENKINS/Dashboard+View] (version:2.2, optional) \\\\ [warnings|https://wiki.jenkins-ci.org/display/JENKINS/Warnings+Plugin] (version:4.27, optional)")
					.append(" || Source Code \\\\ Issue Tracking \\\\ Maintainer(s) | ")
					// JG this line too
					.append(isGithub ? "[GitHub|https://github.com/jenkinsci/"
							: "[Subversion|https://svn.jenkins-ci.org/trunk/hudson/plugins/")
					.append(sourceDir)
					.append(']')
					.br()
					.append("[Open Issues|http://issues.jenkins-ci.org/secure/IssueNavigator.jspa?mode=hide&reset=true&jqlQuery=project+%3D+JENKINS+AND+status+in+%28Open%2C+%22In+Progress%22%2C+Reopened%29+AND+component+%3D+%27")
					.append(jiraComponent).append("%27]").br();

			WikiWriter devString = new WikiWriter();

			if (devString.length() > 0) {
				devString.append("\n");
			}
			if (!devEmail.equals("n/a")) {
				devString.href(devName, "mailto:" + devEmail);
			} else {
				devString.append(devName);
			}
			devString.print(" (id: %s)", devId);
			if (devString.length() == 0) {
				devString.append("(not specified)");
			}
			toBeRendered.append(devString.toString());
			toBeRendered.append(" |\n ");
		}

		{// third row
			if (renderStatistics) {
				toBeRendered
						.append(" || Usage | ")
						.image("http://chart.apis.google.com/chart?cht=lc&chxl=1:%7C07%7C08%7C09%7C10%7C11%7C12%7C01%7C02%7C03%7C04%7C05%7C06%7C2:%7CMonth&chxp=2,50&chxr=0,0,6718%7C1,0,12&chxs=1,676767,12&chxt=y,x,x&chs=300x225&chds=0,6718&chd=t:4714,4864,5070,5520,5719,5709,5984,5977,6449,6675,6580,6718&chg=10,-1,0,0&chls=4&chco=d24939&chtt=analysis-collector+-+installations");
				toBeRendered.append(" || Installations | ");
				toBeRendered
						.append("2012-Jul 4714\n2012-Aug 4864\n2012-Sep 5070\n2012-Oct 5520\n2012-Nov 5719\n2012-Dec 5709\n2013-Jan 5984\n2013-Feb 5977\n2013-Mar 6449\n2013-Apr 6675\n2013-May 6580\n2013-Jun 6718\n");

				toBeRendered.append("[(?)|Plugin Installation Statistics]");
				toBeRendered.append("|\n");
			}
		}

		return toBeRendered.toString();
	}

	private String renderExpectedOutputAntepediaReporter() {
		WikiWriter toBeRendered = new WikiWriter().h4("Plugin Information");
		String name = "AntepediaReporter-CI-plugin";
		boolean isGithub = false;
		String prevVer = "0.0.20";
		String githubBaseUrl = "https://github.com/jenkinsci/cucumber-reports-plugin/compare/cucumber-reports";
		String version = "1.6.3";
		String requiredCore = "1.466";
		String buildDate = "May 21, 2013";
		String sourceDir = "AntepediaReporter-CI-plugin";
		String jiraComponent = "AntepediaReporter-CI-plugin";
		String devId = "freddy";
		String devEmail = "n/a";
		String devName = "n/a";

		{// first row
			toBeRendered.append("|| Plugin ID | ").append(name)
					.append(" || Changes | [In Latest Release|");
			if (isGithub) {
				toBeRendered.append(githubBaseUrl).append('-').append(prevVer)
						.append("...").append(name).append('-').append(version)
						.append("]\n[Since Latest Release|")
						.append(githubBaseUrl).append('-').append(version)
						.append("...master]");
			} else {
				String previousTimestamp = "2013-04-26T16:12:10.00Z";
				String releaseTimestamp = "2013-05-21T18:23:24.00Z";
				String fisheyeBaseUrl = "http://fisheye.jenkins-ci.org/search/Jenkins"
						+ "/trunk/hudson/plugins/"
						+ sourceDir
						+ "?ql=select%20revisions%20from%20dir%20/trunk/hudson/plugins/"
						+ sourceDir + "%20where%20date%20>%20";
				String fisheyeEndUrl = "%20group%20by%20changeset"
						+ "%20return%20csid,%20comment,%20author,%20path";
				toBeRendered.append(fisheyeBaseUrl).append(previousTimestamp)
						.append("%20and%20date%20<%20")
						.append(releaseTimestamp).append(fisheyeEndUrl)
						.append("]\n[Since Latest Release|")
						.append(fisheyeBaseUrl).append(releaseTimestamp)
						.append(fisheyeEndUrl).append(']');

			}
			toBeRendered.append(" |\n ");
		}

		{// second row
			toBeRendered
					.append(" || Latest Release \\\\ Latest Release Date \\\\ Required Core \\\\ Dependencies | ")
					.href(version,
							"http://updates.jenkins-ci.org/latest/" + name
									+ ".hpi")
					.append(" ")
					.href("(archives)",
							"http://updates.jenkins-ci.org/download/plugins/"
									+ name + "/")
					.br()
					.append(buildDate)
					.br()
					.href(requiredCore,
							"http://updates.jenkins-ci.org/download/war/"
									+ requiredCore + "/jenkins.war")
					.br()
					.append("")
					.append(" || Source Code \\\\ Issue Tracking \\\\ Maintainer(s) | ")
					// JG this line too
					.append(isGithub ? "[GitHub|https://github.com/jenkinsci/"
							: "[Subversion|https://svn.jenkins-ci.org/trunk/hudson/plugins/")
					.append(sourceDir)
					.append(']')
					.br()
					.append("[Open Issues|http://issues.jenkins-ci.org/secure/IssueNavigator.jspa?mode=hide&reset=true&jqlQuery=project+%3D+JENKINS+AND+status+in+%28Open%2C+%22In+Progress%22%2C+Reopened%29+AND+component+%3D+%27")
					.append(jiraComponent).append("%27]").br();

			WikiWriter devString = new WikiWriter();

			if (devString.length() > 0) {
				devString.append("\n");
			}
			if (!devEmail.equals("n/a")) {
				devString.href(devName, "mailto:" + devEmail);
			} else {
				devString.append(devName);
			}
			devString.print(" (id: %s)", devId);
			if (devString.length() == 0) {
				devString.append("(not specified)");
			}
			toBeRendered.append(devString.toString());
			toBeRendered.append(" |\n ");
		}

		{// third row
			toBeRendered
					.append(" || Usage | ")
					.image("http://chart.apis.google.com/chart?cht=lc&chxl=1:%7C02%7C03%7C04%7C05%7C06%7C2:%7CMonth&chxp=2,50&chxr=0,0,15%7C1,0,12&chxs=1,676767,12&chxt=y,x,x&chs=300x225&chds=0,15&chd=t:4,7,6,10,15&chg=10,-1,0,0&chls=4&chco=d24939&chtt="
							+ jiraComponent + "+-+installations");
			toBeRendered.append(" || Installations | ");
			toBeRendered
					.append("2013-Feb 4\n2013-Mar 7\n2013-Apr 6\n2013-May 10\n2013-Jun 15\n");

			toBeRendered.append("[(?)|Plugin Installation Statistics]");
			toBeRendered.append("|\n");
		}

		return toBeRendered.toString();
	}
}
