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
package com.axelor.gradle.support;

import com.axelor.common.VersionUtils;
import com.axelor.gradle.tasks.CopyWebapp;
import com.axelor.gradle.tasks.GenerateCode;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.bundling.War;

public class WarSupport extends AbstractSupport {

  public static final String COPY_WEBAPP_TASK_NAME = "copyWebapp";

  private final String version = VersionUtils.getVersion().version;

  @Override
  public void apply(Project project) {

    project.getPlugins().apply(WarPlugin.class);

    Configuration axelorTomcat = project.getConfigurations().create("axelorTomcat");

    // apply providedCompile dependencies
    applyConfigurationLibs(project, "provided", "compileOnly");

    // add axelor-tomcat dependency
    project.getDependencies().add("axelorTomcat", "com.axelor:axelor-tomcat:" + version);

    // copy webapp to root build dir
    project
        .getTasks()
        .create(
            COPY_WEBAPP_TASK_NAME,
            CopyWebapp.class,
            task -> {
              task.dependsOn(GenerateCode.TASK_NAME);
              task.dependsOn(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
            });

    project
        .getTasks()
        .withType(War.class)
        .all(
            task -> {
              task.dependsOn(COPY_WEBAPP_TASK_NAME);
              task.setClasspath(task.getClasspath().filter(file -> !axelorTomcat.contains(file)));
            });

    final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);
    war.from(project.getBuildDir() + "/webapp");
    war.exclude(
        "node_modules", "gulpfile.js", "package.json", "Brocfile.js", ".jshintignore", ".jshintrc");
    war.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
  }
}
