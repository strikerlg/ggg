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
package com.axelor.common;

import com.google.common.base.Joiner;
import org.junit.Assert;
import org.junit.Test;

public class TestStringUtils {

  @Test
  public void testIsEmpty() {
    Assert.assertTrue(StringUtils.isEmpty(null));
    Assert.assertTrue(StringUtils.isEmpty(""));
    Assert.assertFalse(StringUtils.isEmpty(" "));
  }

  @Test
  public void testIsBlank() {
    Assert.assertTrue(StringUtils.isBlank(null));
    Assert.assertTrue(StringUtils.isBlank(""));
    Assert.assertTrue(StringUtils.isBlank(" "));
    Assert.assertFalse(StringUtils.isBlank("some value"));
  }

  static final String text1 =
      "" + "  this is some text\n" + "  this is some text\n" + "  this is some text\n";

  static final String text2 =
      "" + "  this is some text\n" + "    \tthis is some text\n" + "   this is some text\n";

  static final String text3 =
      "" + "  this is some text\n" + "  this is some text\n" + " this is some text\n";

  static final String text4 =
      "" + "this is some text\n" + "    |this is some text\n" + "    |this is some text\n";

  @Test
  public void testStripIndent() {
    String[] lines = StringUtils.stripIndent(text1).split("\n");
    Assert.assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    Assert.assertFalse(Character.isWhitespace(lines[1].charAt(0)));
    Assert.assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    Assert.assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "this is some text\n" + "this is some text");

    lines = StringUtils.stripIndent(text2).split("\n");
    Assert.assertFalse(Character.isWhitespace(lines[0].charAt(0)));
    Assert.assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    Assert.assertTrue(Character.isWhitespace(lines[2].charAt(0)));
    Assert.assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "  \tthis is some text\n" + " this is some text");

    lines = StringUtils.stripIndent(text3).split("\n");
    Assert.assertTrue(Character.isWhitespace(lines[0].charAt(0)));
    Assert.assertTrue(Character.isWhitespace(lines[1].charAt(0)));
    Assert.assertFalse(Character.isWhitespace(lines[2].charAt(0)));
    Assert.assertEquals(
        Joiner.on("\n").join(lines),
        "" + " this is some text\n" + " this is some text\n" + "this is some text");
  }

  @Test
  public void testStripMargin() {
    String[] lines = StringUtils.stripMargin(text4).split("\n");
    Assert.assertEquals(
        Joiner.on("\n").join(lines),
        "" + "this is some text\n" + "this is some text\n" + "this is some text");
  }

  @Test
  public void testStripAccent() {
    Assert.assertEquals(
        "AAAAAACEEEEIIIINOOOOOUUUUY", StringUtils.stripAccent("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ"));
    Assert.assertEquals(
        "aaaaaaceeeeiiiinooooouuuuyy", StringUtils.stripAccent("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ"));
    Assert.assertEquals("L", StringUtils.stripAccent("Ł"));
    Assert.assertEquals("l", StringUtils.stripAccent("ł"));
    Assert.assertEquals("Cesar", StringUtils.stripAccent("César"));
    Assert.assertEquals("Andre", StringUtils.stripAccent("André"));
  }
}
