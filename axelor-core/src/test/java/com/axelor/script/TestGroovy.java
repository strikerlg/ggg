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
package com.axelor.script;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGroovy extends ScriptTest {

  private static final int COUNT = 1000;

  private static final String EXPR_INTERPOLATION =
      "\"(${title.name}) = $firstName $lastName ($fullName) = ($__user__)\"";

  private static final String EXPR_CONCAT =
      "'(' + title.name + ') = ' + firstName + ' ' + lastName + ' (' + fullName + ') = (' + __user__ + ')' ";

  private static final String EXPR_ELVIS =
      "'(' + __this__?.title?.name + ') = ' + __this__?.firstName + ' ' + __this__?.lastName + ' (' + __this__?.fullName + ') = (' + __user__ + ')' ";

  // false all, to evaluate all conditions
  private static final String EXPR_CONDITION =
      "(title instanceof Contact || fullName == 'foo') || (__ref__ instanceof Title) || (__parent__ == 0.102) || (__self__ == __this__)";

  private void doTestSpeed(String expr) {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    for (int i = 0; i < COUNT; i++) {
      Object result = helper.eval(expr);
      Assert.assertNotNull(result);
    }
  }

  private void doCastTest(int counter) {

    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object actual;

    actual = helper.eval("__parent__");
    Assert.assertTrue(actual instanceof Context);

    actual = helper.eval("__ref__");
    Assert.assertTrue(actual instanceof Contact);

    actual = helper.eval("__parent__ as Contact");
    Assert.assertTrue(actual instanceof Contact);

    actual = helper.eval("(__ref__ as Contact).fullName");
    Assert.assertTrue(actual instanceof String);

    actual = helper.eval("(__ref__ as Contact).fullName + ' (" + counter + ")'");
  }

  @Test
  public void doJpaTest() {
    final ScriptHelper helper = new GroovyScriptHelper(context());
    final Object bean = helper.eval("doInJPA({ em -> em.find(Contact, id) })");
    Assert.assertNotNull(bean);
    Assert.assertTrue(bean instanceof Contact);
  }

  @Test
  public void test01_casts() {
    doCastTest(0);
  }

  // @Test
  public void test02_permgen() {
    int counter = 0;
    while (counter++ < 5000) {
      doCastTest(counter);
    }
  }

  @Test
  public void test10_warmup() {
    doTestSpeed(EXPR_INTERPOLATION);
  }

  @Test
  public void test11_interpolation() {
    doTestSpeed(EXPR_INTERPOLATION);
  }

  @Test
  public void test12_concat() {
    doTestSpeed(EXPR_CONCAT);
  }

  @Test
  public void test13_elvis() {
    doTestSpeed(EXPR_ELVIS);
  }

  @Test
  public void test14_condition() {
    doTestSpeed(EXPR_CONDITION);
  }

  @Test
  public void test15_java_concat() {
    Context context = context();
    Contact contact = context.asType(Contact.class);
    for (int i = 0; i < COUNT; i++) {
      String result =
          "("
              + contact.getTitle().getName()
              + ") ="
              + " "
              + contact.getFirstName()
              + " "
              + contact.getLastName()
              + " ("
              + contact.getFullName()
              + ") ="
              + " ("
              + context.get("__user__")
              + ")";
      Assert.assertNotNull(result);
    }
  }
}
