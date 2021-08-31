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
package com.axelor.web.service;

import static com.axelor.common.ObjectUtils.isEmpty;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.dms.db.DMSFile;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailAddress;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.mail.service.MailService;
import com.axelor.mail.web.MailController;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.inject.servlet.RequestScoped;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.DatatypeConverter;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/rest/{model}")
public class RestService extends ResourceService {

  @Inject private MetaService service;

  @Inject private MailMessageRepository messages;

  @Inject private MailFollowerRepository followers;

  @Inject private HttpServletRequest httpRequest;

  private Response fail() {
    final Response response = new Response();
    return response.fail("invalid request");
  }

  @GET
  public Response find(
      @QueryParam("limit") @DefaultValue("40") int limit,
      @QueryParam("offset") @DefaultValue("0") int offset,
      @QueryParam("q") String query) {

    Request request = new Request();
    request.setModel(getModel());
    request.setOffset(offset);
    request.setLimit(limit);
    return getResource().search(request);
  }

  @SuppressWarnings("all")
  private void updateContext(Request request) {
    final Map<String, Object> data = request.getData();
    if (data == null || !data.containsKey("_domainAction")) {
      return;
    }

    // domain actions are used by portlets to evaluate view context
    // in the context of the current form
    final String action = (String) data.get("_domainAction");
    final Map<String, Object> old = (Map) data.get("_domainContext");

    ViewService.updateContext(action, old);
  }

  @POST
  @Path("search")
  public Response find(Request request) {
    if (request == null) {
      request = new Request();
      request.setOffset(0);
      request.setLimit(40);
    }

    request.setModel(getModel());
    updateContext(request);

    return getResource().search(request);
  }

  @POST
  public Response save(Request request) {
    if (request == null || (isEmpty(request.getRecords()) && isEmpty(request.getData()))) {
      return fail();
    }
    request.setModel(getModel());
    return getResource().save(request);
  }

  @PUT
  public Response create(Request request) {
    return save(request);
  }

  @GET
  @Path("{id}")
  public Response read(@PathParam("id") long id) {
    return getResource().read(id);
  }

  @POST
  @Path("{id}/fetch")
  public Response fetch(@PathParam("id") long id, Request request) {
    final User user = AuthUtils.getUser();

    if (request == null || user == null) {
      return fail();
    }

    request.setModel(getModel());
    Response response = getResource().fetch(id, request);
    Optional<Map<String, Object>> values =
        Optional.ofNullable(response.getItem(0)).map(Map.class::cast);

    if (values.isPresent()) {
      final long attachments = getAttachmentCount(id);
      final Object processInstanceId = findProcessInstanceId(request.getBeanClass(), id);
      final Map<String, Object> item = values.get();
      item.put("$attachments", attachments);
      item.put("$processInstanceId", processInstanceId);
    }

    return response;
  }

  private long getAttachmentCount(long relatedId) {
    final Filter securityFilter =
        Beans.get(JpaSecurity.class).getFilter(JpaSecurity.CAN_READ, DMSFile.class);
    final Filter filter =
        new JPQLFilter(
            "self.relatedModel = :relatedModel "
                + "AND self.relatedId = :relatedId "
                + "AND COALESCE(self.isDirectory, FALSE) = FALSE");
    return (securityFilter != null ? Filter.and(securityFilter, filter) : filter)
        .build(DMSFile.class)
        .bind("relatedModel", getModel())
        .bind("relatedId", relatedId)
        .cacheable()
        .count();
  }

  private Object findProcessInstanceId(Class<?> klass, long id) {
    final MetaModule bpm = Beans.get(MetaModuleRepository.class).findByName("axelor-bpm");
    if (bpm != null && Objects.equals(bpm.getInstalled(), Boolean.TRUE)) {
      Object bean = JPA.em().find(klass, id);
      return Mapper.of(klass).get(bean, "processInstanceId");
    }
    return null;
  }

  @POST
  @Path("{id}")
  public Response update(@PathParam("id") long id, Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    final List<Object> records = new ArrayList<>();
    final Map<String, Object> data = request.getData();

    data.put("id", id);
    records.add(data);

    request.setRecords(records);
    request.setModel(getModel());

    return getResource().save(request);
  }

  @POST
  @Path("updateMass")
  public Response updateMass(Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    request.setModel(getModel());
    return getResource().updateMass(request);
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") long id, @QueryParam("version") int version) {
    Request request = new Request();
    request.setModel(getModel());
    request.setData(ImmutableMap.of("id", (Object) id, "version", version));
    return getResource().remove(id, request);
  }

  @POST
  @Path("{id}/remove")
  public Response remove(@PathParam("id") long id, Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    request.setModel(getModel());
    return getResource().remove(id, request);
  }

  @POST
  @Path("removeAll")
  public Response remove(Request request) {
    if (request == null || isEmpty(request.getRecords())) {
      return fail();
    }
    request.setModel(getModel());
    return getResource().remove(request);
  }

  @GET
  @Path("{id}/copy")
  public Response copy(@PathParam("id") long id) {
    return getResource().copy(id);
  }

  @GET
  @Path("{id}/details")
  public Response details(@PathParam("id") long id, @QueryParam("name") String name) {
    Request request = new Request();
    Map<String, Object> data = new HashMap<String, Object>();

    data.put("id", id);
    request.setModel(getModel());
    request.setData(data);
    request.setFields(Lists.newArrayList(name));

    return getResource().getRecordName(request);
  }

  private void uploadSave(InputStream in, OutputStream out) throws IOException {
    int read = 0;
    byte[] bytes = new byte[1024];
    while ((read = in.read(bytes)) != -1) {
      out.write(bytes, 0, read);
    }
    out.flush();
    out.close();
    in.close();
  }

  private String getFileName(MultivaluedMap<String, String> headers) {
    final String[] parts = headers.getFirst("Content-Disposition").split(";");
    for (String filename : parts) {
      if ((filename.trim().startsWith("filename"))) {
        String name = filename.split("=")[1].trim();
        // remove quotes
        name = name.replaceAll("^\"|\"$", "");
        // on IE11, sometime we get full path, so get name only
        return new File(name).getName();
      }
    }
    return null;
  }

  @POST
  @Path("upload")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response upload(final MultipartFormDataInput input) throws IOException {

    final Map<String, List<InputPart>> formData = input.getFormDataMap();
    final InputPart filePart = formData.get("file").get(0);
    final InputPart requestPart = formData.get("request").get(0);
    final InputPart fieldPart = formData.get("field").get(0);

    final boolean isAttachment = MetaFile.class.getName().equals(getModel());
    final String field = fieldPart.getBodyAsString();
    final String fileName = getFileName(filePart.getHeaders());
    final String fileType = filePart.getHeaders().getFirst("Content-Type");

    // check if file name is valid
    MetaFiles.checkPath(fileName);
    MetaFiles.checkType(fileType);

    final InputStream fileStream = filePart.getBody(InputStream.class, null);
    final Request request =
        Beans.get(ObjectMapper.class).readValue(requestPart.getBodyAsString(), Request.class);

    request.setModel(getModel());

    final Map<String, Object> data = request.getData();

    if (!isAttachment) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      uploadSave(fileStream, out);
      data.put(field, out.toByteArray());
      return getResource().save(request);
    }

    final MetaFiles files = Beans.get(MetaFiles.class);
    final MetaFileRepository repo = Beans.get(MetaFileRepository.class);
    final MetaFile metaFile = Mapper.toBean(MetaFile.class, data);

    MetaFile entity = metaFile;
    if (metaFile.getId() != null) {
      entity = repo.find(metaFile.getId());
    }

    entity.setFileName(metaFile.getFileName());
    entity.setFileType(metaFile.getFileType());

    File tmp = files.upload(fileStream, 0, -1, fileName);
    entity = files.upload(tmp, entity);

    final Response response = new Response();
    response.setData(Arrays.asList(entity));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  private static final String BLANK_IMAGE =
      "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

  @SuppressWarnings("all")
  private javax.ws.rs.core.Response download(MetaFile metaFile) {
    final Mapper mapper = Mapper.of(MetaFile.class);
    final String fileName = (String) mapper.get(metaFile, "fileName");
    final String filePath = (String) mapper.get(metaFile, "filePath");
    final File inputFile = Beans.get(MetaFiles.class).getPath(filePath).toFile();
    if (!inputFile.exists()) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }
    return javax.ws.rs.core.Response.ok(
            new StreamingOutput() {

              @Override
              public void write(OutputStream output) throws IOException, WebApplicationException {
                uploadSave(new FileInputStream(inputFile), output);
              }
            })
        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        .build();
  }

  @GET
  @Path("{id}/{field}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @SuppressWarnings("all")
  public javax.ws.rs.core.Response download(
      @PathParam("id") Long id,
      @PathParam("field") String field,
      @QueryParam("image") boolean isImage,
      @QueryParam("parentId") Long parentId,
      @QueryParam("parentModel") String parentModel)
      throws IOException {

    final Class klass = getResource().getModel();
    final boolean permittedByParent;

    if (MetaFile.class.isAssignableFrom(klass)) {
      permittedByParent = checkMetaFileParentPermission(id, parentId, parentModel);
    } else {
      permittedByParent = false;
    }

    if (!permittedByParent && !getResource().isPermitted(JpaSecurity.CAN_READ, id)) {
      return javax.ws.rs.core.Response.status(Status.FORBIDDEN).build();
    }

    final Mapper mapper = Mapper.of(klass);
    final Model bean = JPA.find(klass, id);

    if (bean == null) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    if (bean instanceof MetaFile) {
      return download((MetaFile) bean);
    }

    String fileName = getModel() + "_" + field;
    Object data = mapper.get(bean, field);

    if (data instanceof MetaFile) {
      return download((MetaFile) data);
    }

    if (isImage) {
      String base64 = BLANK_IMAGE;
      if (data instanceof byte[]) {
        base64 = new String((byte[]) data);
      }
      try {
        base64 = base64.substring(base64.indexOf(";base64,") + 8);
        data = DatatypeConverter.parseBase64Binary(base64);
      } catch (Exception e) {
      }
      return javax.ws.rs.core.Response.ok(data).build();
    }

    fileName = fileName.replaceAll("\\s", "") + "_" + id;
    fileName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fileName);

    if (data == null) {
      return javax.ws.rs.core.Response.noContent().build();
    }

    return javax.ws.rs.core.Response.ok(data)
        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        .build();
  }

  private boolean checkMetaFileParentPermission(Long id, Long parentId, String parentModel) {
    // Check for permission on specified parent.
    if (parentId != null && StringUtils.notBlank(parentModel)) {
      try {
        if (checkMetaFileParent(id, parentId, parentModel)) {
          return true;
        }
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }

    // Check for permission on related DMS file.
    final User user = AuthUtils.getUser();
    return user != null
        && JpaRepository.of(DMSFile.class)
                .all()
                .filter(
                    ""
                        + "self.metaFile.id = :id "
                        + "AND (self.permissions.group = :group "
                        + "OR self.permissions.user = :user)")
                .bind("id", id)
                .bind("group", user.getGroup())
                .bind("user", user)
                .fetchOne()
            != null;
  }

  private boolean checkMetaFileParent(Long id, Long parentId, String parentModel)
      throws ClassNotFoundException {
    @SuppressWarnings("unchecked")
    final Class<? extends Model> parentClass = (Class<? extends Model>) Class.forName(parentModel);
    final Model parent = JpaRepository.of(parentClass).find(parentId);

    // Check permission on parent.
    if (parent == null
        || !Beans.get(JpaSecurity.class)
            .isPermitted(JpaSecurity.CAN_READ, parentClass, parent.getId())) {
      return false;
    }

    final Mapper mapper = Mapper.of(parentClass);
    final Context context = new Context(Mapper.toMap(parent), parentClass);

    // Make sure specified meta file exists on parent.
    for (final Property property : mapper.getProperties()) {
      if (property.isJson() && checkMetaFileJsonProperty(property, id, context, parentModel)
          || property.getTarget() != null
              && MetaFile.class.isAssignableFrom(property.getTarget())
              && checkMetaFileProperty(property, id, context)) {
        return true;
      }
    }

    return false;
  }

  private boolean checkMetaFileProperty(Property property, Long id, Context context) {
    return property.isReference() && checkMetaFileExists(id, context.get(property.getName()))
        || property.isCollection()
            && checkMetaFileInCollection(id, context.get(property.getName()));
  }

  private boolean checkMetaFileJsonProperty(
      Property property, Long id, Context context, String parentModel)
      throws ClassNotFoundException {
    final Map<String, Object> jsonFields =
        MetaStore.findJsonFields(parentModel, property.getName());

    for (final Entry<String, Object> entry : jsonFields.entrySet()) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> value = (Map<String, Object>) entry.getValue();

      if (value == null) {
        continue;
      }

      final String target = (String) value.get("target");

      if (target != null && MetaFile.class.isAssignableFrom(Class.forName(target))) {
        final String type = (String) value.getOrDefault("type", "null");

        if (type.endsWith("-to-one") && checkMetaFileExists(id, context.get(entry.getKey()))
            || type.endsWith("-to-many")
                && checkMetaFileInCollection(id, context.get(entry.getKey()))) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean checkMetaFileExists(Long id, Object metaFileObj) {
    return checkMetaFileInCollection(id, Collections.singletonList(metaFileObj));
  }

  private boolean checkMetaFileInCollection(Long id, Object metaFilesObj) {
    @SuppressWarnings("unchecked")
    final Collection<MetaFile> metaFiles = (Collection<MetaFile>) metaFilesObj;

    return metaFiles
        .parallelStream()
        .anyMatch(metaFile -> metaFile != null && Objects.equals(metaFile.getId(), id));
  }

  @POST
  @Path("{id}/attachment")
  public Response attachment(@PathParam("id") long id, Request request) {
    if (request == null || isEmpty(request.getFields())) {
      return fail();
    }
    request.setModel(getModel());
    return service.getAttachment(id, getModel(), request);
  }

  @POST
  @Path("removeAttachment")
  public Response removeAttachment(Request request) {
    if (request == null || isEmpty(request.getRecords())) {
      return fail();
    }
    request.setModel(getModel());
    return service.removeAttachment(request);
  }

  @POST
  @Path("{id}/addAttachment")
  public Response addAttachment(@PathParam("id") long id, Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    request.setModel(getModel());
    return service.addAttachment(id, request);
  }

  @POST
  @Path("verify")
  public Response verify(Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    request.setModel(getModel());
    return getResource().verify(request);
  }

  @GET
  @Path("perms")
  public Response perms(@QueryParam("action") String action, @QueryParam("id") Long id) {
    if (action != null && id != null) {
      return getResource().perms(id, action);
    }
    if (id != null) {
      return getResource().perms(id);
    }
    return getResource().perms();
  }

  private static final Charset CSV_CHARSET;
  private static final Locale CSV_LOCALE;
  private static final Character CSV_SEPARATOR;

  static {
    final AppSettings settings = AppSettings.get();

    final String encoding = settings.get(AvailableAppSettings.DATA_EXPORT_ENCODING, "UTF-8");
    CSV_CHARSET = Charset.forName(encoding);

    final String locale = settings.get(AvailableAppSettings.DATA_EXPORT_LOCALE, null);
    if (locale != null) {
      CSV_LOCALE = Locale.forLanguageTag(locale.replace("_", "-"));
    } else {
      CSV_LOCALE = null;
    }

    final String separator =
        Optional.ofNullable(settings.get(AvailableAppSettings.DATA_EXPORT_SEPARTOR))
            .filter(StringUtils::notEmpty)
            .orElse(";");
    if (separator.length() != 1) {
      throw new IllegalArgumentException(
          String.format("Illegal data export separator: %s", separator));
    }
    CSV_SEPARATOR = separator.charAt(0);
  }

  @HEAD
  @Path("export/{name}")
  public javax.ws.rs.core.Response exportCheck(@PathParam("name") final String name) {
    return Files.exists(MetaFiles.findTempFile(name))
        ? javax.ws.rs.core.Response.ok().build()
        : javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("export/{name}")
  @Produces("text/csv")
  public StreamingOutput export(@PathParam("name") final String name) {

    final java.nio.file.Path temp = MetaFiles.findTempFile(name);
    if (Files.notExists(temp)) {
      throw new IllegalArgumentException(name);
    }

    return new StreamingOutput() {

      @Override
      public void write(OutputStream output) throws IOException, WebApplicationException {
        try (final InputStream is = new FileInputStream(temp.toFile())) {
          uploadSave(is, output);
        } finally {
          Files.deleteIfExists(temp);
        }
      }
    };
  }

  @POST
  @Path("export")
  public Response export(Request request) {
    if (request == null || request.getFields() == null) {
      return fail();
    }

    request.setModel(getModel());
    updateContext(request);

    // ServletRequest#getLocale() returns server locale if no Accept-Language header is set.
    final Locale locale = CSV_LOCALE != null ? CSV_LOCALE : httpRequest.getLocale();
    return getResource().export(request, CSV_CHARSET, locale, CSV_SEPARATOR);
  }

  @GET
  @Path("messages")
  public Response messages(
      @QueryParam("folder") String folder,
      @QueryParam("parent") Long parentId,
      @QueryParam("count") boolean count,
      @QueryParam("limit") @DefaultValue("10") Integer limit,
      @QueryParam("offset") @DefaultValue("0") Integer offset,
      @QueryParam("relatedId") Long relatedId,
      @QueryParam("relatedModel") String relatedModel) {

    final MailController ctrl = Beans.get(MailController.class);

    final ActionRequest req = new ActionRequest();
    final ActionResponse res = new ActionResponse();

    req.setModel(relatedModel);
    req.setOffset(offset);
    req.setLimit(limit);

    Class<?> relatedClass = null;
    Model related = null;

    try {
      relatedClass = Class.forName(relatedModel);
    } catch (Exception e) {
    }

    if (relatedClass != null && relatedId != null) {
      @SuppressWarnings("all")
      JpaRepository<?> repo = JpaRepository.of((Class) relatedClass);
      if (repo != null) {
        related = repo.find(relatedId);
      }
    }

    if (related != null) {
      final List<Object> records = new ArrayList<>();
      records.add(related);
      req.setRecords(records);
      ctrl.related(req, res);
      return res;
    }

    if (count) {
      ctrl.countUnread(req, res);
      return res;
    }

    if (parentId != null) {
      List<Object> records = new ArrayList<>();
      records.add(parentId);
      req.setRecords(records);
      ctrl.replies(req, res);
      return res;
    }

    if (folder == null) {
      return res;
    }

    switch (folder) {
      case "archive":
        ctrl.archived(req, res);
        return res;
      case "important":
        ctrl.important(req, res);
        return res;
      case "unread":
        ctrl.unread(req, res);
        return res;
      default:
        ctrl.inbox(req, res);
        return res;
    }
  }

  @GET
  @Path("{id}/followers")
  public Response messageFollowers(@PathParam("id") long id) {

    @SuppressWarnings("all")
    final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
    final Model entity = repo.find(id);
    final Response response = new Response();

    final Object all = followers.findFollowers(entity);

    response.setData(all);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @POST
  @Path("{id}/follow")
  @SuppressWarnings("all")
  public Response messageFollow(@PathParam("id") long id, Request request) {
    final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
    final Model entity = repo.find(id);

    if (entity == null) {
      return messageFollowers(id);
    }
    if (request == null || request.getData() == null) {
      followers.follow(entity, AuthUtils.getUser());
      return messageFollowers(id);
    }

    final MailMessage message = Mapper.toBean(MailMessage.class, request.getData());
    if (message == null || message.getRecipients() == null || message.getRecipients().isEmpty()) {
      return messageFollowers(id);
    }

    for (MailAddress address : message.getRecipients()) {
      followers.follow(entity, address);
    }

    if (!StringUtils.isBlank(message.getBody())) {
      final MailService mailService = Beans.get(MailService.class);
      try {
        mailService.send(message);
      } catch (Exception e) {
        LOG.error("Unable to send email message to new followers", e);
      }
    }

    return messageFollowers(id);
  }

  @POST
  @Path("{id}/unfollow")
  @SuppressWarnings("all")
  public Response messageUnfollow(@PathParam("id") long id, Request request) {
    final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
    final Model entity = repo.find(id);
    if (entity == null) {
      return messageFollowers(id);
    }

    if (request == null || request.getRecords() == null || request.getRecords().isEmpty()) {
      followers.unfollow(entity, AuthUtils.getUser());
      return messageFollowers(id);
    }

    for (Object item : request.getRecords()) {
      final MailFollower follower = followers.find(Longs.tryParse(item.toString()));
      if (follower != null) {
        followers.unfollow(follower);
      }
    }

    return messageFollowers(id);
  }

  @POST
  @Path("{id}/message")
  public Response messagePost(@PathParam("id") long id, Request request) {
    if (request == null || isEmpty(request.getData())) {
      return fail();
    }
    final Response response = new Response();
    @SuppressWarnings("all")
    final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
    final Context ctx = new Context(request.getData(), MailMessage.class);
    final MailMessage msg = EntityHelper.getEntity(ctx.asType(MailMessage.class));

    final Model entity = repo.find(id);
    final List<?> ids = (List<?>) request.getData().get("files");
    List<MetaFile> files = null;

    if (ids != null && ids.size() > 0) {
      final MetaFileRepository repoFiles = Beans.get(MetaFileRepository.class);
      files = repoFiles.all().filter("self.id IN :ids").bind("ids", ids).fetch();
    }

    MailMessage saved = messages.post(entity == null ? msg : entity, msg, files);
    Object item = messages.details(saved);

    response.setData(Lists.newArrayList(item));
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }
}
