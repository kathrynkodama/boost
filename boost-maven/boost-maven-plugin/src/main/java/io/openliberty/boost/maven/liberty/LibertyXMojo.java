package io.openliberty.boost.maven.liberty;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import io.openliberty.boost.common.boosters.AbstractBoosterConfig;
import io.openliberty.boost.common.config.BoostProperties;
import io.openliberty.boost.common.config.BoosterConfigurator;
import io.openliberty.boost.common.config.ConfigConstants;
import io.openliberty.boost.maven.utils.BoostLogger;
import io.openliberty.boost.maven.utils.MavenProjectUtil;

/**
 * Experimental liberty "x" Mojo recompiles project every 10 seconds
 * 
 * @author kathryn.kodama@ibm.com
 *
 */
@Mojo(name = "X")
public class LibertyXMojo extends AbstractLibertyMojo {

	String libertyServerPath = null;
	protected List<AbstractBoosterConfig> boosterPackConfigurators;

	private String devServer = "DevServer";
	/**
	 * Time in seconds to wait while verifying that the server has started.
	 */
	@Parameter(property = "verifyTimeout", defaultValue = "30")
	private int verifyTimeout = 30;

	/**
	 * Time in seconds to wait while verifying that the server has started.
	 */
	@Parameter(property = "serverStartTimeout", defaultValue = "30")
	private int serverStartTimeout = 30;

	/**
	 * Clean all cached information on server start up.
	 */
	@Parameter(property = "clean", defaultValue = "false")
	private boolean clean;

	@Override
	public void execute() throws MojoExecutionException {
		super.execute();
		BoostLogger.getInstance().debug("project packaging: " + project.getPackaging());

		/*
		 * libertyServerPath = projectBuildDir + "/liberty/wlp/usr/servers/" +
		 * devServer;
		 * 
		 * createServer(); BoostLogger.getInstance().debug("Server created"); String
		 * javaCompilerTargetVersion =
		 * MavenProjectUtil.getJavaCompilerTargetVersion(project);
		 * System.setProperty(BoostProperties.INTERNAL_COMPILER_TARGET,
		 * javaCompilerTargetVersion);
		 * 
		 * try { Map<String, String> dependencies =
		 * MavenProjectUtil.getAllDependencies(project, repoSystem, repoSession,
		 * remoteRepos, BoostLogger.getInstance());
		 * 
		 * this.boosterPackConfigurators =
		 * BoosterConfigurator.getBoosterPackConfigurators(dependencies,
		 * BoostLogger.getInstance());
		 * 
		 * } catch (Exception e) { throw new MojoExecutionException(e.getMessage(), e);
		 * }
		 * 
		 * copyBoosterDependencies();
		 * 
		 * generateServerConfigEE();
		 * 
		 * installMissingFeatures(); // we install the app now, after server.xml is
		 * configured. This is // so that we can specify a custom config-root in
		 * server.xml ("/"). // If we installed the app prior to server.xml
		 * configuration, then // the LMP would write out a webapp stanza into config
		 * dropins that // would include a config-root setting set to the app name. if
		 * (project.getPackaging().equals("war")) {
		 * installApp(ConfigConstants.INSTALL_PACKAGE_ALL); } else { // This is
		 * temporary. When packing type is "jar", if we // set installAppPackages=all,
		 * the LMP will try to install // the project jar and fail. Once this is fixed,
		 * we can always // set installAppPackages=all.
		 * installApp(ConfigConstants.INSTALL_PACKAGE_DEP); }
		 */
		
        Set<String> dependencyFeatures = new HashSet<String>();
        dependencyFeatures = getDependencyFeatures();
		BoostLogger.getInstance().debug("result: " + dependencyFeatures.toString());


		if (project.getPackaging().equals("war")) {

			executeMojo(getPlugin(), goal("install-server"),
					configuration(element(name("serverName"), devServer), getRuntimeArtifactElement()),
					getExecutionEnvironment());
			BoostLogger.getInstance().debug("Installed server");

			createServer();
			BoostLogger.getInstance().debug("Server created");

			installMissingFeatures(dependencyFeatures);
			BoostLogger.getInstance().debug("Missing features installed");

			installApp(ConfigConstants.INSTALL_PACKAGE_ALL);
			BoostLogger.getInstance().debug("App installed");
		}

		executeMojo(getPlugin(), goal("start"),
				configuration(element(name("serverName"), devServer),
						element(name("verifyTimeout"), String.valueOf(verifyTimeout)),
						element(name("serverStartTimeout"), String.valueOf(serverStartTimeout)),
						element(name("clean"), String.valueOf(clean)), getRuntimeArtifactElement()),
				getExecutionEnvironment());

		BoostLogger.getInstance().debug("Hello, world");

		int count = 0;
		if (project.getPackaging().equals("war")) {
			while (true) {
				try {
					count++;
					getLog().info("Running 'mvn compile': " + count);
					ProcessBuilder processBuilder = new ProcessBuilder();
					// Run a shell command
					processBuilder.command("bash", "-c", "mvn compile");

					try {
						Process process = processBuilder.start();

						StringBuilder output = new StringBuilder();

						BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

						String line;
						while ((line = reader.readLine()) != null) {
							output.append(line + "\n");
						}

						int exitVal = process.waitFor();
						if (exitVal == 0) {
							System.out.println("Success!");
							System.out.println(output);
							// System.exit(0);
						} else {
							// abnormal...
						}

					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Invoke the liberty-maven-plugin to run the install-app goal.
	 */
	private void installApp(String installAppPackagesVal) throws MojoExecutionException {
		Element installAppPackages = element(name("installAppPackages"), installAppPackagesVal);
		Element serverName = element(name("serverName"), devServer);

		Xpp3Dom configuration = configuration(installAppPackages, serverName, getRuntimeArtifactElement());
		if (!ConfigConstants.INSTALL_PACKAGE_SPRING.equals(installAppPackagesVal)) {
			configuration.addChild(element(name("appsDirectory"), "apps").toDom());
			configuration.addChild(element(name("looseApplication"), "true").toDom());
		}

		executeMojo(getPlugin(), goal("install-apps"), configuration, getExecutionEnvironment());

	}

	/**
	 * Invoke the liberty-maven-plugin to run the create-server goal
	 */
	private void createServer() throws MojoExecutionException {

		Plugin plugin = getPlugin();
		
		BoostLogger.getInstance().debug("plugin : " + plugin.getConfiguration());

		executeMojo(getPlugin(), goal("create-server"),
				configuration(element(name("serverName"), devServer), element(name("configFile"), "src/main/liberty/config/server.xml"), getRuntimeArtifactElement()),
				getExecutionEnvironment());
	}

	/**
	 * Invoke the liberty-maven-plugin to run the install-feature goal.
	 *
	 * This will install any missing features defined in the server.xml or
	 * configDropins.
	 *
	 */
	private void installMissingFeatures(Set<String> dependencyFeatures) throws MojoExecutionException {
		executeMojo(getPlugin(), goal("install-feature"), configuration(element(name("serverName"), devServer)), getExecutionEnvironment());
				
//		executeMojo(getPlugin(), goal("install-feature"), configuration(element(name("serverName"), devServer),
//				element(name("features"), element(name("acceptLicense"), "true"))), getExecutionEnvironment());
	}

	/**
	 * Get all booster dependencies and invoke the maven-dependency-plugin to copy
	 * them to the Liberty server.
	 * 
	 * @throws MojoExecutionException
	 *
	 */
	private void copyBoosterDependencies() throws MojoExecutionException {

		List<String> dependenciesToCopy = BoosterConfigurator.getDependenciesToCopy(boosterPackConfigurators,
				BoostLogger.getInstance());

		for (String dep : dependenciesToCopy) {

			String[] dependencyInfo = dep.split(":");

			executeMojo(getMavenDependencyPlugin(), goal("copy"),
					configuration(element(name("outputDirectory"), libertyServerPath + "/resources"),
							element(name("artifactItems"),
									element(name("artifactItem"), element(name("groupId"), dependencyInfo[0]),
											element(name("artifactId"), dependencyInfo[1]),
											element(name("version"), dependencyInfo[2])))),
					getExecutionEnvironment());
		}
	}

	/**
	 * Generate config for the Liberty server based on the Maven Spring Boot
	 * project.
	 * 
	 * @throws MojoExecutionException
	 */
	private void generateServerConfigEE() throws MojoExecutionException {

		List<String> warNames = getWarNames();
		try {
			// Generate server config
			BoosterConfigurator.generateLibertyServerConfig(libertyServerPath, boosterPackConfigurators, warNames,
					BoostLogger.getInstance());

		} catch (Exception e) {
			throw new MojoExecutionException("Unable to generate server configuration for the Liberty server.", e);
		}
	}

	private List<String> getWarNames() {
		List<String> warNames = new ArrayList<String>();

		for (Artifact artifact : project.getArtifacts()) {
			if (artifact.getType().equals("war")) {
				warNames.add(artifact.getArtifactId() + "-" + artifact.getVersion());
			}
		}

		if (project.getPackaging().equals(ConfigConstants.WAR_PKG_TYPE)) {
			if (project.getVersion() == null) {
				warNames.add(project.getArtifactId());
			} else {
				warNames.add(project.getArtifactId() + "-" + project.getVersion());
			}
		}

		return warNames;
	}
	
	private Set<String> getDependencyFeatures() {
        Set<String> result = new HashSet<String>();
        List<org.apache.maven.model.Dependency> dependencyArtifacts = project.getDependencies();
		BoostLogger.getInstance().debug("dependency artifacts: " + dependencyArtifacts);
        for (org.apache.maven.model.Dependency dependencyArtifact: dependencyArtifacts){
        	BoostLogger.getInstance().debug("dependency artifact: " + dependencyArtifact.toString());
        	BoostLogger.getInstance().debug("dependency type: " + dependencyArtifact.getType().toString());
;
            if (("esa").equals(dependencyArtifact.getType())) {
                result.add(dependencyArtifact.getArtifactId());
                //log.debug("Dependency feature: " + dependencyArtifact.getArtifactId());
            }
        }
        return result;
    }

}
