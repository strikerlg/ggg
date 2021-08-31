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
package com.axelor.dms.db.repo;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Model;
import com.axelor.db.annotations.Track;
import com.axelor.db.mapper.Mapper;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.DMSFileTag;
import com.axelor.dms.db.DMSPermission;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.rpc.Resource;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import org.apache.shiro.authz.UnauthorizedException;

public class DMSFileRepository extends JpaRepository<DMSFile> {

  @Inject private MetaFiles metaFiles;

  @Inject private JpaSecurity security;

  @Inject private DMSPermissionRepository dmsPermissions;

  @Inject private MetaAttachmentRepository attachments;

  private static final Pattern previewSupportedPattern = Pattern.compile("\\b(?:pdf|image)\\b");

  public DMSFileRepository() {
    super(DMSFile.class);
  }

  @Nullable
  public DMSFile findHomeByRelated(Model related) {
    return all()
        .filter(
            ""
                + "COALESCE(self.isDirectory, FALSE) = TRUE "
                + "AND self.relatedId = :id "
                + "AND self.relatedModel = :model "
                + "AND self.parent.relatedModel = :model "
                + "AND COALESCE(self.parent.relatedId, 0) = 0")
        .bind("id", related.getId())
        .bind("model", related.getClass().getName())
        .fetchOne();
  }

  @SuppressWarnings("all")
  private Model findRelated(DMSFile file) {
    if (file == null || file.getRelatedId() == null || file.getRelatedModel() == null) {
      return null;
    }
    Class<? extends Model> klass = null;
    try {
      klass = (Class) Class.forName(file.getRelatedModel());
    } catch (Exception e) {
      return null;
    }
    final Model entity = JpaRepository.of(klass).find(file.getRelatedId());
    return EntityHelper.getEntity(entity);
  }

  private void createMessage(DMSFile file, boolean delete) {
    final Model related = findRelated(file);
    if (related == null || related.getId() == null || file.getMetaFile() == null) {
      return;
    }
    final Class<?> klass = EntityHelper.getEntityClass(related);
    final Track track = klass.getAnnotation(Track.class);
    if (track == null || !track.files()) {
      return;
    }
    final ObjectMapper objectMapper = Beans.get(ObjectMapper.class);
    final MailMessageRepository messages = Beans.get(MailMessageRepository.class);
    final MailMessage message = new MailMessage();

    message.setRelatedId(related.getId());
    message.setRelatedModel(klass.getName());
    message.setAuthor(AuthUtils.getUser());

    message.setSubject(delete ? I18n.get("File removed") : I18n.get("File added"));

    final Map<String, Object> json = new HashMap<>();
    final Map<String, Object> attrs = new HashMap<>();

    attrs.put("id", file.getMetaFile().getId());
    attrs.put("fileName", file.getFileName());
    attrs.put("fileIcon", metaFiles.fileTypeIcon(file.getMetaFile()));

    json.put("files", Arrays.asList(attrs));
    try {
      message.setBody(objectMapper.writeValueAsString(json));
    } catch (JsonProcessingException e) {
    }

    messages.save(message);
  }

  @Override
  public DMSFile save(DMSFile entity) {
    DMSFile parent = entity.getParent();
    Model related = findRelated(entity);
    if (related == null && parent != null) {
      related = findRelated(parent);
    }

    final boolean isAttachment = related != null && entity.getMetaFile() != null;

    if (related != null) {
      entity.setRelatedId(related.getId());
      entity.setRelatedModel(related.getClass().getName());
    }

    // if new attachment, save attachment reference
    if (isAttachment) {
      // remove old attachment if file is moved
      MetaAttachment attachmentOld =
          attachments.all().filter("self.metaFile.id = ?", entity.getMetaFile().getId()).fetchOne();
      if (attachmentOld != null) {
        attachments.remove(attachmentOld);
      }

      MetaAttachment attachment =
          attachments
              .all()
              .filter(
                  "self.metaFile.id = ? AND self.objectId = ? AND self.objectName = ?",
                  entity.getMetaFile().getId(),
                  related.getId(),
                  related.getClass().getName())
              .fetchOne();
      if (attachment == null) {
        attachment = metaFiles.attach(entity.getMetaFile(), related);
        attachments.save(attachment);
      }

      // generate track message
      createMessage(entity, false);
    }

    // if not an attachment or has parent, do nothing
    if (parent == null && related != null) {
      // create parent folders
      final DMSFile dmsHome = findOrCreateHome(related);
      entity.setParent(dmsHome);
    }

    if (entity.getVersion() == null || entity.getVersion() == 0) {
      copyParentPermissions(entity);
    }

    return super.save(entity);
  }

  private void copyParentPermissions(DMSFile entity) {
    Optional.ofNullable(entity.getParent())
        .map(DMSFile::getPermissions)
        .ifPresent(
            permissions ->
                permissions.stream()
                    .map(permission -> dmsPermissions.copy(permission, false))
                    .forEach(entity::addPermission));
  }

  /**
   * Finds or creates parent folders.
   *
   * @param related model
   * @return home parent
   */
  protected DMSFile findOrCreateHome(Model related) {
    final List<Filter> dmsRootFilters =
        Lists.newArrayList(
            new JPQLFilter(
                ""
                    + "COALESCE(self.isDirectory, FALSE) = TRUE "
                    + "AND self.relatedModel = :model "
                    + "AND COALESCE(self.relatedId, 0) = 0"));
    final DMSFile dmsRootParent = getRootParent(related);

    if (dmsRootParent != null) {
      dmsRootFilters.add(new JPQLFilter("self.parent = :rootParent"));
    }

    DMSFile dmsRoot =
        Filter.and(dmsRootFilters)
            .build(DMSFile.class)
            .bind("model", related.getClass().getName())
            .bind("rootParent", dmsRootParent)
            .fetchOne();

    if (dmsRoot == null) {
      final Inflector inflector = Inflector.getInstance();
      dmsRoot = new DMSFile();
      dmsRoot.setFileName(
          inflector.pluralize(inflector.humanize(related.getClass().getSimpleName())));
      dmsRoot.setRelatedModel(related.getClass().getName());
      dmsRoot.setIsDirectory(true);
      dmsRoot.setParent(dmsRootParent);
      dmsRoot = save(dmsRoot); // Should get id before its child.
    }

    DMSFile dmsHome = findHomeByRelated(related);

    if (dmsHome == null) {
      String homeName = null;

      try {
        final Mapper mapper = Mapper.of(related.getClass());
        homeName = mapper.getNameField().get(related).toString();
      } catch (Exception e) {
        // Ignore
      }

      if (homeName == null) {
        homeName = Strings.padStart("" + related.getId(), 5, '0');
      }

      dmsHome = new DMSFile();
      dmsHome.setFileName(homeName);
      dmsHome.setRelatedId(related.getId());
      dmsHome.setRelatedModel(related.getClass().getName());
      dmsHome.setParent(dmsRoot);
      dmsHome.setIsDirectory(true);
      dmsHome = save(dmsHome); // Should get id before its child.
    }

    return dmsHome;
  }

  /**
   * Gets root parent folder
   *
   * @param related model
   * @return root parent folder
   */
  @Nullable
  protected DMSFile getRootParent(Model related) {
    return null;
  }

  @Override
  public void remove(DMSFile entity) {
    // remove all children
    if (entity.getIsDirectory() == Boolean.TRUE) {
      final List<DMSFile> children = all().filter("self.parent.id = ?", entity.getId()).fetch();
      for (DMSFile child : children) {
        if (child != entity) {
          remove(child);
        }
      }
    }

    // remove attached file
    if (entity.getMetaFile() != null) {
      final MetaFile metaFile = entity.getMetaFile();
      long count = attachments.all().filter("self.metaFile = ?", metaFile).count();
      if (count == 1) {
        final MetaAttachment attachment =
            attachments.all().filter("self.metaFile = ?", metaFile).fetchOne();
        attachments.remove(attachment);

        // generate track message
        createMessage(entity, true);
      }
      count = all().filter("self.metaFile = ?", metaFile).count();
      if (count == 1) {
        entity.setMetaFile(null);
        try {
          metaFiles.delete(metaFile);
        } catch (IOException e) {
          throw new PersistenceException(e);
        }
      }
    }

    super.remove(entity);
  }

  private DMSFile findFrom(Map<String, Object> json) {
    if (json == null || json.get("id") == null) {
      return null;
    }
    final Long id = Longs.tryParse(json.get("id").toString());
    return find(id);
  }

  private boolean canCreate(DMSFile parent) {
    final User user = AuthUtils.getUser();
    final Group group = user.getGroup();
    if (parent.getCreatedBy() == user
        || security.hasRole("role.super")
        || security.hasRole("role.admin")) {
      return true;
    }

    // allow if the parent folder has no specific dms permissions
    if (dmsPermissions.all().filter("self.file = :file").bind("file", parent).count() == 0) {
      return true;
    }

    return dmsPermissions
            .all()
            .filter(
                "self.file = :file AND self.permission.canWrite = true AND "
                    + "(self.user = :user OR self.group = :group)")
            .bind("file", parent)
            .bind("user", user)
            .bind("group", group)
            .autoFlush(false)
            .count()
        > 0;
  }

  private boolean canOffline(DMSFile file, User user) {
    return file.getIsDirectory() != Boolean.TRUE
        && file.getMetaFile() != null
        && dmsPermissions
                .all()
                .filter("self.file = ? AND self.value = 'OFFLINE' AND self.user = ?", file, user)
                .count()
            > 0;
  }

  @Transactional
  public DMSFile setOffline(DMSFile file, boolean offline) {
    Preconditions.checkNotNull(file, "file can't be null");

    // directory can't be marked as offline
    if (file.getIsDirectory() == Boolean.TRUE) {
      return file;
    }

    final User user = AuthUtils.getUser();
    boolean canOffline = canOffline(file, user);

    if (offline == canOffline) {
      return file;
    }

    DMSPermission permission;

    if (offline) {
      permission = new DMSPermission();
      permission.setValue("OFFLINE");
      permission.setFile(file);
      permission.setUser(user);
      file.addPermission(permission);
    } else {
      permission =
          dmsPermissions
              .all()
              .filter("self.file = ? AND self.value = 'OFFLINE' AND self.user = ?", file, user)
              .fetchOne();
      file.removePermission(permission);
      dmsPermissions.remove(permission);
    }

    return this.save(file);
  }

  public List<DMSFile> findOffline(int limit, int offset) {
    return all()
        .filter("self.permissions[].value = 'OFFLINE' AND self.permissions[].user = :user")
        .bind("user", AuthUtils.getUser())
        .fetch(limit, offset);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    final DMSFile file = findFrom(json);
    final DMSFile parent = findFrom((Map<String, Object>) json.get("parent"));
    if (parent == null) {
      return json;
    }
    if (file != null && file.getParent() == parent) {
      return json;
    }

    // check whether user can create/move document here
    if (file == null && !canCreate(parent)) {
      throw new UnauthorizedException(I18n.get("You can't create document here."));
    }
    if (file != null && file.getParent() != parent && !canCreate(parent)) {
      throw new UnauthorizedException(I18n.get("You can't move document here."));
    }

    return json;
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    final DMSFile file = findFrom(json);
    if (file == null) {
      return json;
    }

    boolean isFile = file.getIsDirectory() != Boolean.TRUE;
    LocalDateTime dt = file.getUpdatedOn();
    if (dt == null) {
      dt = file.getCreatedOn();
    }

    final User user = AuthUtils.getUser();
    final MetaFile metaFile = file.getMetaFile();

    boolean canShare =
        file.getCreatedBy() == user
            || security.isPermitted(AccessType.CREATE, DMSFile.class, file.getId())
            || dmsPermissions
                    .all()
                    .filter(
                        "self.file = ? AND self.value = 'FULL' AND (self.user = ? OR self.group = ?)",
                        file,
                        user,
                        user.getGroup())
                    .count()
                > 0;

    json.put("typeIcon", isFile ? "fa fa-file" : "fa fa-folder");
    json.put("downloadIcon", "fa fa-download");
    json.put("detailsIcon", "fa fa-info-circle");

    json.put("canShare", canShare);
    json.put("canWrite", canCreate(file));

    if (canOffline(file, user)) {
      json.put("offline", true);
    }

    json.put("createdBy", Resource.toMapCompact(file.getCreatedBy()));
    json.put("createdOn", file.getCreatedOn());
    json.put("updatedBy", Resource.toMapCompact(file.getUpdatedBy()));
    json.put("updatedOn", dt);

    if ("html".equals(file.getContentType())) {
      json.put("fileType", "text/html");
      json.put("contentType", "html");
      json.put("typeIcon", "fa fa-file-text-o");
    }
    if ("spreadsheet".equals(file.getContentType())) {
      json.put("fileType", "text/json");
      json.put("contentType", "spreadsheet");
      json.put("typeIcon", "fa fa-file-excel-o");
    }

    if (metaFile != null) {
      String fileType = metaFile.getFileType();
      String fileIcon = metaFiles.fileTypeIcon(metaFile);
      json.put("fileType", fileType);
      json.put("typeIcon", "fa fa-colored " + fileIcon);
      json.put("metaFile.sizeText", metaFile.getSizeText());

      // Put inlineUrl only if preview for that file type is supported, to prevent auto-downloading
      if (StringUtils.notBlank(fileType) && previewSupportedPattern.matcher(fileType).find()) {
        json.put("inlineUrl", String.format("ws/dms/inline/%d", file.getId()));
      }
    }

    if (file.getTags() != null) {
      final List<Object> tags = new ArrayList<>();
      for (DMSFileTag tag : file.getTags()) {
        tags.add(Resource.toMap(tag, "id", "code", "name", "style"));
      }
      json.put("tags", tags);
    }

    return json;
  }
}
