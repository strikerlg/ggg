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

import org.hibernate.cache.jcache.JCacheRegionFactory;

public interface AvailableAppSettings {

  String APPLICATION_NAME = "application.name";
  String APPLICATION_DESCRIPTION = "application.description";
  String APPLICATION_HOME = "application.home";
  String APPLICATION_VERSION = "application.version";
  String APPLICATION_AUTHOR = "application.author";
  String APPLICATION_LOCALE = "application.locale";
  String APPLICATION_THEME = "application.theme";
  String APPLICATION_HELP = "application.help";
  String APPLICATION_COPYRIGHT = "application.copyright";
  String APPLICATION_LOGO = "application.logo";
  String APPLICATION_MODE = "application.mode";
  String APPLICATION_BASE_URL = "application.baseUrl";
  String APPLICATION_CONFIG_PROVIDER = "application.config.provider";
  String CONFIG_MULTI_TENANCY = "application.multi_tenancy";

  String VIEW_MENUBAR_LOCATION = "view.menubar.location";
  String VIEW_CONFIRM_YESNO = "view.confirm.yes-no";
  String VIEW_SINGLE_TAB = "view.single.tab";
  String VIEW_TOOLBAR_TITLES = "view.toolbar.titles";
  String VIEW_TABS_MAX = "view.tabs.max";
  String VIEW_FORM_CHECKVERSION = "view.form.check-version";
  String VIEW_ADVSEARCH_EXPORT_FULL = "view.adv-search.export.full";
  String VIEW_ADVSEARCH_SHARE = "view.adv-search.share";
  String VIEW_GRID_SELECTION = "view.grid.selection";

  String AXELOR_SCRIPT_CACHE_SIZE = "axelor.ScriptCacheSize";
  String AXELOR_SCRIPT_CACHE_EXPIRETIME = "axelor.ScriptCacheExpireTime";

  String CONTEXT_APP_STYLE = "context.appStyle";
  String CONTEXT_APP_LOGO = "context.appLogo";

  String DB_DEFAULT_DATASOURCE = "db.default.datasource";
  String DB_DEFAULT_DRIVER = "db.default.driver";
  String DB_DEFAULT_URL = "db.default.url";
  String DB_DEFAULT_USER = "db.default.user";
  String DB_DEFAULT_PASSWORD = "db.default.password";

  String DOMAIN_BLACKLIST_PATTERN = "domain.blacklist.pattern";

  @Deprecated String AXELOR_REPORT_DIR = "axelor.report.dir";

  String REPORTS_DESIGN_DIR = "reports.design.dir";
  String REPORTS_FONTS_CONFIG = "reports.fonts.config";

  String TEMPLATE_SEARCH_DIR = "template.search.dir";

  String FILE_UPLOAD_DIR = "file.upload.dir";
  String FILE_UPLOAD_SIZE = "file.upload.size";

  String DATA_EXPORT_DIR = "data.export.dir";
  String DATA_EXPORT_MAX_SIZE = "data.export.max-size";
  String DATA_EXPORT_FETCH_SIZE = "data.export.fetch-size";
  String DATA_EXPORT_ENCODING = "data.export.encoding";
  String DATA_EXPORT_LOCALE = "data.export.locale";
  String DATA_EXPORT_SEPARTOR = "data.export.separator";

  String CORS_ALLOW_ORIGIN = "cors.allow.origin";
  String CORS_ALLOW_CREDENTIALS = "cors.allow.credentials";
  String CORS_ALLOW_METHODS = "cors.allow.methods";
  String CORS_ALLOW_HEADERS = "cors.allow.headers";
  String CORS_EXPOSE_HEADERS = "cors.expose.headers";
  String CORS_MAX_AGE = "cors.max.age";

  String SESSION_TIMEOUT = "session.timeout";
  String SESSION_COOKIE_SECURE = "session.cookie.secure";

  String QUARTZ_ENABLE = "quartz.enable";
  String QUARTZ_THREAD_COUNT = "quartz.threadCount";

  String FILE_UPLOAD_FILENAME_PATTERN = "file.upload.filename.pattern";
  String FILE_UPLOAD_WHITELIST_PATTERN = "file.upload.whitelist.pattern";
  String FILE_UPLOAD_BLACKLIST_PATTERN = "file.upload.blacklist.pattern";
  String FILE_UPLOAD_WHITELIST_TYPES = "file.upload.whitelist.types";
  String FILE_UPLOAD_BLACKLIST_TYPES = "file.upload.blacklist.types";

  String DATA_IMPORT_DEMO_DATA = "data.import.demo-data";

  String USER_PASSWORD_PATTERN = "user.password.pattern";
  String USER_PASSWORD_PATTERN_TITLE = /*$$(*/ "user.password.pattern.title" /*)*/;

  String ENCRYPTION_ALGORITHM = "encryption.algorithm";
  String ENCRYPTION_PASSWORD = "encryption.password";
  String ENCRYPTION_ALGORITHM_OLD = "encryption.algorithm.old";
  String ENCRYPTION_PASSWORD_OLD = "encryption.password.old";

  String HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER =
      "hibernate.search.default.directory_provider";
  String HIBERNATE_SEARCH_DEFAULT_INDEX_BASE = "hibernate.search.default.indexBase";

  String HIBERNATE_HIKARI_MINIMUN_IDLE = "hibernate.hikari.minimumIdle";
  String HIBERNATE_HIKARI_MAXIMUN_POOL_SIZE = "hibernate.hikari.maximumPoolSize";
  String HIBERNATE_HIKARI_IDLE_TIMEOUT = "hibernate.hikari.idleTimeout";

  String HIBERNATE_JDBC_BATCH_SIZE = "hibernate.jdbc.batch_size";
  String HIBERNATE_JDBC_FETCH_SIZE = "hibernate.jdbc.fetch_size";

  String HIBERNATE_JAVAX_CACHE_PROVIDER = JCacheRegionFactory.PROVIDER;
  String HIBERNATE_JAVAX_CACHE_URI = JCacheRegionFactory.CONFIG_URI;

  String MAIL_SMTP_HOST = "mail.smtp.host";
  String MAIL_SMTP_PORT = "mail.smtp.port";
  String MAIL_SMTP_USER = "mail.smtp.user";
  String MAIL_SMTP_PASS = "mail.smtp.pass";
  String MAIL_SMTP_CHANNEL = "mail.smtp.channel";
  String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
  String MAIL_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectionTimeout";
  String MAIL_SMTP_FROM = "mail.smtp.from";

  String MAIL_IMAP_HOST = "mail.imap.host";
  String MAIL_IMAP_PORT = "mail.imap.port";
  String MAIL_IMAP_USER = "mail.imap.user";
  String MAIL_IMAP_PASS = "mail.imap.pass";
  String MAIL_IMAP_CHANNEL = "mail.imap.channel";
  String MAIL_IMAP_TIMEOUT = "mail.imap.timeout";
  String MAIL_IMAP_CONNECTION_TIMEOUT = "mail.imap.connectionTimeout";

  String LOGGING_PATH = "logging.path";
  String LOGGING_CONFIG = "logging.config";
  String LOGGING_PATTERN_FILE = "logging.pattern.file";
  String LOGGING_PATTERN_CONSOLE = "logging.pattern.console";

  String AUTH_LDAP_SERVER_URL = "auth.ldap.server.url";
  String AUTH_LDAP_USER_BASE = "auth.ldap.user.base";
  String AUTH_LDAP_USER_FILTER = "auth.ldap.user.filter";
  String AUTH_LDAP_USER_DN_FORMAT = "auth.ldap.user.dn.format";
  String AUTH_LDAP_USER_ID_ATTRIBUTE = "auth.ldap.user.id.attribute";
  String AUTH_LDAP_USER_USERNAME_ATTRIBUTE = "auth.ldap.user.username.attribute";
  String AUTH_LDAP_GROUP_BASE = "auth.ldap.group.base";
  String AUTH_LDAP_GROUP_FILTER = "auth.ldap.group.filter";
  String AUTH_LDAP_SYSTEM_USER = "auth.ldap.system.user";
  String AUTH_LDAP_SYSTEM_PASSWORD = "auth.ldap.system.password";
  String AUTH_LDAP_AUTH_TYPE = "auth.ldap.auth.type";
  String AUTH_LDAP_USE_SSL = "auth.ldap.use.ssl";
  String AUTH_LDAP_USE_STARTTLS = "auth.ldap.use.starttls";
  String AUTH_LDAP_CREDENTIAL_TRUST_STORE = "auth.ldap.credential.trust.store";
  String AUTH_LDAP_CREDENTIAL_KEY_STORE = "auth.ldap.credential.key.store";
  String AUTH_LDAP_CREDENTIAL_STORE_PASSWORD = "auth.ldap.credential.store.password";
  String AUTH_LDAP_CREDENTIAL_STORE_TYPE = "auth.ldap.credential.store.type";
  String AUTH_LDAP_CREDENTIAL_STORE_ALIASES = "auth.ldap.credential.store.aliases";
  String AUTH_LDAP_CREDENTIAL_TRUST_CERTIFICATES = "auth.ldap.credential.trust.certificates";
  String AUTH_LDAP_CREDENTIAL_AUTHENTICATION_CERTIFICATE =
      "auth.ldap.credential.authentication.certificate";
  String AUTH_LDAP_CREDENTIAL_AUTHENTICATION_KEY = "auth.ldap.credential.authentication.key";
  String AUTH_LDAP_CONNECT_TIMEOUT = "auth.ldap.connect.timeout";
  String AUTH_LDAP_RESPONSE_TIMEOUT = "auth.ldap.response.timeout";

  String AUTH_LOCAL_BASIC_AUTH_ENABLED = "auth.local.basic.auth.enabled";

  String AUTH_CAS_LOGIN_URL = "auth.cas.login.url";
  String AUTH_CAS_PREFIX_URL = "auth.cas.prefix.url";
  String AUTH_CAS_PROTOCOL = "auth.cas.protocol";
  String AUTH_CAS_ENCODING = "auth.cas.encoding";
  String AUTH_CAS_RENEW = "auth.cas.renew";
  String AUTH_CAS_GATEWAY = "auth.cas.gateway";
  String AUTH_CAS_TIME_TOLERANCE = "auth.cas.time.tolerance";
  String AUTH_CAS_URL_RESOLVER_CLASS = "auth.cas.url.resolver.class";
  String AUTH_CAS_DEFAULT_TICKET_VALIDATOR_CLASS = "auth.cas.default.ticket.validator.class";
  String AUTH_CAS_PROXY_SUPPORT = "auth.cas.proxy.support";
  String AUTH_CAS_LOGOUT_HANDLER_CLASS = "auth.cas.logout.handler.class";
  String AUTH_CAS_CLIENT_TYPE = "auth.cas.client.type";
  String AUTH_CAS_SERVICE_URL = "auth.cas.service.url";
  String AUTH_CAS_USERNAME_PARAMETER = "auth.cas.username.parameter";
  String AUTH_CAS_PASSWORD_PARAMETER = "auth.cas.password.parameter";
  String AUTH_CAS_HEADER_NAME = "auth.cas.header.name";
  String AUTH_CAS_PREFIX_HEADER = "auth.cas.prefix.header";
  String AUTH_CAS_SERVER_PREFIX_URL = "auth.cas.server.url.prefix";
  String AUTH_CAS_SERVICE = "auth.cas.service";
  String AUTH_CAS_LOGOUT_URL = "auth.cas.logout.url";
  String AUTH_CAS_ATTRS_USER_NAME = "auth.cas.attrs.user.name";
  String AUTH_CAS_ATTRS_USER_EMAIL = "auth.cas.attrs.user.email";

  String AUTH_CALLBACK_URL = "auth.callback.url";
  String AUTH_USER_PROVISIONING = "auth.user.provisioning";
  String AUTH_USER_DEFAULT_GROUP = "auth.user.default.group";
  String AUTH_USER_PRINCIPAL_ATTRIBUTE = "auth.user.principal.attribute";

  String AUTH_LOGOUT_URL = "auth.logout.url";
  String AUTH_LOGOUT_URL_PATTERN = "auth.logout.url.pattern";
  String AUTH_LOGOUT_LOCAL = "auth.logout.local";
  String AUTH_LOGOUT_CENTRAL = "auth.logout.central";

  String AUTH_SAML_KEYSTORE_PATH = "auth.saml.keystore.path";
  String AUTH_SAML_KEYSTORE_PASSWORD = "auth.saml.keystore.password";
  String AUTH_SAML_PRIVATE_KEY_PASSWORD = "auth.saml.private.key.password";
  String AUTH_SAML_IDENTITY_PROVIDER_METADATA_PATH = "auth.saml.identity.provider.metadata.path";
  String AUTH_SAML_MAXIMUM_AUTHENTICATION_LIFETIME = "auth.saml.maximum.authentication.lifetime";
  String AUTH_SAML_SERVICE_PROVIDER_ENTITY_ID = "auth.saml.service.provider.entity.id";
  String AUTH_SAML_SERVICE_PROVIDER_METADATA_PATH = "auth.saml.service.provider.metadata.path";
  String AUTH_SAML_FORCE_AUTH = "auth.saml.force.auth";
  String AUTH_SAML_PASSIVE = "auth.saml.passive";
  String AUTH_SAML_AUTHN_REQUEST_BINDING_TYPE = "auth.saml.authn.request.binding.type";
  String AUTH_SAML_RESPONSE_BINDING_TYPE = "auth.saml.response.binding.type";
  String AUTH_SAML_USE_NAME_QUALIFIER = "auth.saml.use.name.qualifier";
  String AUTH_SAML_ATTRIBUTE_CONSUMING_SERVICE_INDEX =
      "auth.saml.attribute.consuming.service.index";
  String AUTH_SAML_ASSERTION_CONSUMER_SERVICE_INDEX = "auth.saml.assertion.consumer.service.index";
  String AUTH_SAML_BLACKLISTED_SIGNATURE_SIGNING_ALGORITHMS =
      "auth.saml.blacklisted.signature.signing.algorithms";
  String AUTH_SAML_SIGNATURE_ALGORITHMS = "auth.saml.signature.algorithms";
  String AUTH_SAML_SIGNATURE_REFERENCE_DIGEST_METHODS =
      "auth.saml.signature.reference.digest.methods";
  String AUTH_SAML_SIGNATURE_CANONICALIZATION_ALGORITHM =
      "auth.saml.signature.canonicalization.algorithm";
  String AUTH_SAML_WANTS_ASSERTIONS_SIGNED = "auth.saml.wants.assertions.signed";
  String AUTH_SAML_AUTHN_REQUEST_SIGNED = "auth.saml.authn.request.signed";
}
