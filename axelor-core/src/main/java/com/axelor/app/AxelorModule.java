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
package com.axelor.app;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

/**
 * The entry-point of application module.
 *
 * <p>Application module can provide an implementation of {@link AxelorModule} to configure {@link
 * Guice} bindings and do some initialization when application starts.
 */
public abstract class AxelorModule extends AbstractModule {}
