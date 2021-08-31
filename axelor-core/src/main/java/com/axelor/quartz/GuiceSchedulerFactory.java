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
package com.axelor.quartz;

import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;

/** Custom {@link StdSchedulerFactory} to use {@link GuiceJobRunShellFactory}. */
public class GuiceSchedulerFactory extends StdSchedulerFactory {

  public GuiceSchedulerFactory(Properties props) throws SchedulerException {
    super(props);
  }

  @Override
  protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
    Scheduler scheduler = super.instantiate(rsrcs, qs);
    JobRunShellFactory jrsf = new GuiceJobRunShellFactory(rsrcs);
    rsrcs.setJobRunShellFactory(jrsf);
    try {
      jrsf.initialize(scheduler);
    } catch (SchedulerConfigException e) {
      // this should not happen
    }
    return scheduler;
  }
}
