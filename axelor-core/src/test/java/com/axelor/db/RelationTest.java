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
package com.axelor.db;

import com.axelor.JpaTest;
import com.axelor.test.db.Invoice;
import com.axelor.test.db.Move;
import com.axelor.test.db.MoveLine;
import com.axelor.test.db.repo.InvoiceRepository;
import com.axelor.test.db.repo.MoveRepository;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RelationTest extends JpaTest {

  @Inject private InvoiceRepository invoices;

  @Inject private MoveRepository moves;

  @Before
  @Transactional
  public void createInvoice() {
    Invoice invoice = new Invoice();
    invoices.save(invoice);
  }

  @Transactional
  protected void testErrors() {
    Invoice invoice = invoices.all().fetchOne();

    Move move = new Move();
    move.setMoveLines(Lists.<MoveLine>newArrayList());
    move.setInvoice(invoice);

    MoveLine line1 = new MoveLine();
    line1.setCredit(new BigDecimal("20"));
    line1.setDebit(BigDecimal.ZERO);
    line1.setMove(move);
    line1.setInvoiceReject(invoice);

    move.getMoveLines().add(line1);

    invoice.setRejectMoveLine(line1);
    invoice.setMove(move);

    invoices.save(invoice);

    Assert.assertSame(line1, invoice.getRejectMoveLine());
    Assert.assertSame(line1, move.getMoveLines().get(0));

    Assert.assertEquals(new BigDecimal("20"), line1.getCredit());
    Assert.assertEquals(BigDecimal.ZERO, line1.getDebit());

    moves.save(move);
  }

  @Transactional
  protected void testRelations() {
    Move move = all(Move.class).fetchOne();
    MoveLine line = all(MoveLine.class).fetchOne();
    Invoice invoice = all(Invoice.class).fetchOne();

    Assert.assertSame(move, line.getMove());
    Assert.assertSame(move, invoice.getMove());
    Assert.assertSame(line, invoice.getRejectMoveLine());

    Assert.assertSame(line, move.getMoveLines().get(0));
  }

  @Transactional
  protected void testRemoveCollection() {
    Move move = all(Move.class).fetchOne();

    // the moveLines is exclusive o2m field, so clear would delete all
    // the move lines but one of move line is referenced outside so the
    // entity manager will throws an exception
    move.getMoveLines().clear();
  }

  @Transactional
  protected void testRemoveCollectionProper() {
    Move move = all(Move.class).fetchOne();
    Invoice invoice = all(Invoice.class).fetchOne();

    // clear the reference
    invoice.setRejectMoveLine(null);

    // then clear the collection
    move.getMoveLines().clear();

    // or

    // invoices.remove(invoice); // this will auto detach invoice from all it's children
    // move.getMoveLines().clear();
  }

  @Test
  public void test() {
    testErrors();
    testRelations();

    try {
      testRemoveCollection();
      Assert.fail();
    } catch (PersistenceException e) {
    }

    testRemoveCollectionProper();
  }
}
