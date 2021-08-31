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
package com.axelor.events;

import com.axelor.auth.db.User;
import org.apache.shiro.authc.AuthenticationToken;

public class PostLogin extends LoginEvent {

  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";

  private final User user;

  private final Throwable error;

  public PostLogin(AuthenticationToken token, User user, Throwable error) {
    super(token);
    this.user = user;
    this.error = error;
  }

  public User getUser() {
    return user;
  }

  public Throwable getError() {
    return error;
  }

  public boolean isSuccess() {
    return user != null && error == null;
  }

  public boolean isFailure() {
    return !isSuccess();
  }
}
