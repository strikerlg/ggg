/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.gradle;

import com.axelor.gradle.support.EclipseSupport;
import com.axelor.gradle.support.IdeaSupport;
import com.axelor.gradle.support.JavaSupport;
import com.axelor.gradle.support.LicenseSupport;
import com.axelor.gradle.support.PublishSupport;
import com.axelor.gradle.tasks.GenerateCode;
import com.axelor.gradle.tasks.I18nTask;
import com.axelor.gradle.tasks.UpdateVersion;
import java.io.File;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.util.GradleVersion;

public class AxelorPlugin implements Plugin<Project> {

  public static final String AXELOR_APP_GROUP = "axelor application";
  public static final String AXELOR_BUILD_GROUP = "axelor build";

  public static final String GRADLE_VERSION = GradleVersion.current().getVersion();

  public static final boolean GRADLE_VERSION_3_X = GRADLE_VERSION.startsWith("3");
  public static final boolean GRADLE_VERSION_4_X = GRADLE_VERSION.startsWith("4");
  public static final boolean GRADLE_VERSION_5_X = GRADLE_VERSION.startsWith("5");

  public static File getClassOutputDir(Project project, String sourceType) {
    return GRADLE_VERSION_3_X
        ? new File(project.getBuildDir(), "classes/main")
        : new File(project.getBuildDir(), "classes/" + sourceType + "/main");
  }

  @Override
  public void apply(Project project) {

    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getExtensions().create(AxelorExtension.EXTENSION_NAME, AxelorExtension.class);

    project.getPlugins().apply(JavaSupport.class);
    project.getPlugins().apply(PublishSupport.class);
    project.getPlugins().apply(LicenseSupport.class);

    if (project.getPlugins().hasPlugin(EclipsePlugin.class)) {
      project.getPlugins().apply(EclipseSupport.class);
    }
    if (project.getPlugins().hasPlugin(IdeaPlugin.class)) {
      project.getPlugins().apply(IdeaSupport.class);
    }

    configureCodeGeneration(project);
  }

  private void configureCodeGeneration(Project project) {
    project
        .getTasks()
        .create(
            I18nTask.TASK_NAME,
            I18nTask.class,
            task -> {
              task.setDescription(I18nTask.TASK_DESCRIPTION);
              task.setGroup(I18nTask.TASK_GROUP);
            });

    project
        .getTasks()
        .create(
            UpdateVersion.TASK_NAME,
            UpdateVersion.class,
            task -> {
              task.setDescription(UpdateVersion.TASK_DESCRIPTION);
              task.setGroup(UpdateVersion.TASK_GROUP);
              final ConfigurableFileTree files = project.fileTree(project.getProjectDir());
              files.include("**/resources/**/*.xml", "**/data/**/*config.xml");
              task.setProcessFiles(files);
            });

    project
        .getTasks()
        .create(
            GenerateCode.TASK_NAME,
            GenerateCode.class,
            task -> {
              task.setDescription(GenerateCode.TASK_DESCRIPTION);
              task.setGroup(GenerateCode.TASK_GROUP);
            });

    project
        .getTasks()
        .withType(AbstractCompile.class)
        .all(task -> task.dependsOn(GenerateCode.TASK_NAME));

    // add src-gen dirs
    project
        .getConvention()
        .getPlugin(JavaPluginConvention.class)
        .getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getJava()
        .srcDir(GenerateCode.getJavaOutputDir(project));
    project
        .getConvention()
        .getPlugin(JavaPluginConvention.class)
        .getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getResources()
        .srcDir(GenerateCode.getResourceOutputDir(project));

    // XXX: prepend class output directory to compile classpath (see #26420)
    // XXX: https://github.com/gradle/gradle/issues/12575
    project
        .getConvention()
        .getPlugin(JavaPluginConvention.class)
        .getSourceSets()
        .getByName(
            SourceSet.MAIN_SOURCE_SET_NAME,
            sourceSet -> {
              sourceSet.setCompileClasspath(
                  project
                      .files(sourceSet.getJava().getOutputDir())
                      .plus(sourceSet.getCompileClasspath()));
            });
  }
}
