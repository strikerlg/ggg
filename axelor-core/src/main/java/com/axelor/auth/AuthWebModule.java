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
package com.axelor.auth;

import com.google.inject.Key;
import javax.servlet.ServletContext;
import org.apache.shiro.guice.web.ShiroWebModule;

public class AuthWebModule extends ShiroWebModule {

  public AuthWebModule(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected final void configureShiroWeb() {
    this.configureAnon();
    this.configureAuth();
  }

  protected void configureAnon() {
    this.addFilterChain("/ws/public/**", ANON);
    this.addFilterChain("/public/**", ANON);
    this.addFilterChain("/dist/**", ANON);
    this.addFilterChain("/lib/**", ANON);
    this.addFilterChain("/img/**", ANON);
    this.addFilterChain("/ico/**", ANON);
    this.addFilterChain("/css/**", ANON);
    this.addFilterChain("/js/**", ANON);
    this.addFilterChain("/error.jsp", ANON);
    this.addFilterChain("/favicon.ico", ANON);
  }

  protected void configureAuth() {
    this.bindRealm().to(AuthRealm.class);
    this.addFilterChain("/logout", LOGOUT);
    this.addFilterChain("/**", Key.get(AuthFilter.class));
  }
}
