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
package com.axelor.mail.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import java.util.List;

public class MailFlagsRepository extends JpaRepository<MailFlags> {

  public MailFlagsRepository() {
    super(MailFlags.class);
  }

  public MailFlags findBy(MailMessage message, User user) {
    return all()
        .filter("self.message = :message AND self.user = :user")
        .bind("message", message)
        .bind("user", user)
        .fetchOne();
  }

  @Override
  public MailFlags save(MailFlags entity) {
    final MailFlags flags = super.save(entity);
    final MailMessage message = flags.getMessage();
    final MailMessage root = message.getRoot();

    if (flags.getIsStarred() == Boolean.FALSE) {
      // message is root, so unflag children
      if (root == null) {
        List<MailFlags> childFlags =
            all().filter("self.message.root.id = ?", message.getId()).fetch();
        for (MailFlags child : childFlags) {
          child.setIsStarred(flags.getIsStarred());
        }
      }
    }

    if (root == null) {
      return flags;
    }

    MailFlags rootFlags = findBy(root, flags.getUser());
    if (rootFlags == null) {
      rootFlags = new MailFlags();
      rootFlags.setMessage(root);
      rootFlags.setUser(flags.getUser());
      super.save(rootFlags);
    }

    rootFlags.setIsStarred(flags.getIsStarred());

    // mark root as unread
    if (flags.getIsRead() != Boolean.TRUE) {
      rootFlags.setIsRead(false);
    }

    return flags;
  }
}
