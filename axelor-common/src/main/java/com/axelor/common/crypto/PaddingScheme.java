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
package com.axelor.common.crypto;

import javax.crypto.Cipher;

/** {@link Cipher} padding schemes. */
public enum PaddingScheme {
  NONE("NoPadding"),

  PKCS5("PKCS5Padding");

  private String name;

  private PaddingScheme(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
