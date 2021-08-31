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

import com.axelor.common.VersionUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;

public class ModulePlugin implements Plugin<Project> {

  private final String version = VersionUtils.getVersion().version;

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(AxelorPlugin.class);

    // add core dependencies
    project.getDependencies().add("implementation", "com.axelor:axelor-core:" + version);
    project.getDependencies().add("implementation", "com.axelor:axelor-web:" + version);
    project.getDependencies().add("testImplementation", "com.axelor:axelor-test:" + version);

    // include webapp resources in jar
    project
        .getTasks()
        .withType(Jar.class, jar -> jar.into("webapp", spec -> spec.from("src/main/webapp")));
  }
}
