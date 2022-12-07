package org.eclipse.tycho.test.baseline;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("This test fails on the CI only...")
public class BaselineMojoTest extends AbstractTychoIntegrationTest {

	/**
	 * Compares the baseline against itself...
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnchangedApi() throws Exception {
		buildBaselineProject("api-bundle", false);
	}

	/**
	 * This adds a method to the interface
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddMethod() throws Exception {
		Verifier verifier = buildBaselineProject("add-method", true);
		verifyBaselineProblem(verifier, "ADDED", "METHOD", "concat(java.lang.String,java.lang.String)", "1.0.0",
				"1.1.0");
	}

	/**
	 * This adds a resource to the bundle
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddResource() throws Exception {
		Verifier verifier = buildBaselineProject("add-resource", true);
		verifyBaselineProblem(verifier, "ADDED", "RESOURCE", "NewFile.txt", "1.0.0", "1.0.100");
	}

	/**
	 * This adds a resource to the bundle but with a version bump
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddResourceWithBump() throws Exception {
		buildBaselineProject("add-resource-with-bump", false);
	}

	/// Helper methods for baseline verifications ///

	private void verifyBaselineProblem(Verifier verifier, String delta, String type, String name, String projectVersion,
			String suggestVersion) throws VerificationException {
		verifyTextInLogMatches(verifier,
				Pattern.compile("\\[ERROR\\].*" + delta + ".*" + type + ".*" + Pattern.quote(name)));
		verifier.verifyTextInLog("Baseline problems found! Project version: " + projectVersion
				+ ", baseline version: 1.0.0, suggested version: " + suggestVersion);
	}

	private Verifier buildBaselineProject(String project, boolean compareShouldFail) throws Exception {
		File baseRepo = buildBaseRepo();
		Verifier verifier = getBaselineProject(project);
		verifier.addCliOption("-Dbaseline-url=" + baseRepo.toURI());
		try {
			verifier.executeGoals(List.of("clean", "verify"));
			if (compareShouldFail) {
				fail("Build should fail for baseline project " + project);
			}
			verifier.verifyErrorFreeLog();
		} catch (VerificationException e) {
			if (compareShouldFail) {
				try {
					verifier.verifyTextInLog("Baseline problems found!");
				} catch (VerificationException textnotfound) {
					throw e;
				}
			} else {
				throw e;
			}
		}
		return verifier;
	}

	private File buildBaseRepo() throws Exception, VerificationException {
		Verifier verifier = getBaselineProject("base-repo");
		verifier.addCliOption("-Dtycho.baseline.skip=true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File repoBase = new File(verifier.getBasedir(), "base-repo/site/target/repository");
		assertTrue("base repository was not created at " + repoBase.getAbsolutePath(), repoBase.isDirectory());
		assertTrue("content.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "content.jar").isFile());
		assertTrue("artifacts.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "artifacts.jar").isFile());
		return repoBase;
	}

	private Verifier getBaselineProject(String project) throws Exception {
		Verifier verifier = getVerifier("baseline", false, true);
		verifier.addCliOption("-f");
		verifier.addCliOption(project + "/pom.xml");
		return verifier;
	}
}