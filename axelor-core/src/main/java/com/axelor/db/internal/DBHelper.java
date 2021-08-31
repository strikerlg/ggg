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
package com.axelor.db.internal;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.w3c.dom.Document;

/** This class provides some database helper methods (for internal use only). */
public class DBHelper {

  private static Boolean unaccentSupport = null;

  private static final int DEFAULT_BATCH_SIZE = 20;
  private static final int DEFAULT_FETCH_SIZE = 20;

  private static final String UNACCENT_CHECK = "SELECT unaccent('text')";
  private static final String UNACCENT_CREATE = "CREATE EXTENSION IF NOT EXISTS unaccent";

  private static final String XPATH_ROOT = "/persistence/persistence-unit";

  private static final String XPATH_NON_JTA_DATA_SOURCE = "non-jta-data-source";
  private static final String XPATH_SHARED_CACHE_MODE = "shared-cache-mode";

  private static final String XPATH_PERSISTENCE_DRIVER =
      "properties/property[@name='javax.persistence.jdbc.driver']/@value";
  private static final String XPATH_PERSISTENCE_URL =
      "properties/property[@name='javax.persistence.jdbc.url']/@value";
  private static final String XPATH_PERSISTENCE_USER =
      "properties/property[@name='javax.persistence.jdbc.user']/@value";
  private static final String XPATH_PERSISTENCE_PASSWORD =
      "properties/property[@name='javax.persistence.jdbc.password']/@value";

  private static final String XPATH_BATCH_SIZE =
      "properties/property[@name='hibernate.jdbc.batch_size']/@value";
  private static final String XPATH_FETCH_SIZE =
      "properties/property[@name='hibernate.jdbc.fetch_size']/@value";

  private static String jndiName;
  private static String cacheMode;

  private static String jdbcDriver;
  private static String jdbcUrl;
  private static String jdbcUser;
  private static String jdbcPassword;

  private static int jdbcBatchSize;
  private static int jdbcFetchSize;

  static {
    initialize();
  }

  private DBHelper() {}

  private static String evaluate(XPath xpath, String base, String path, Document document) {
    try {
      return xpath.evaluate(base + "/" + path, document).trim();
    } catch (Exception e) {
    }
    return null;
  }

  private static void initialize() {

    final AppSettings settings = AppSettings.get();

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final XPathFactory xpf = XPathFactory.newInstance();
    final XPath xpath = xpf.newXPath();

    try (final InputStream res = ResourceUtils.getResourceStream("META-INF/persistence.xml")) {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document document = db.parse(res);

      final String jpaUnit = evaluate(xpath, XPATH_ROOT, "@name", document);
      final String pu = jpaUnit.replaceAll("(PU|Unit)$", "").replaceAll("^persistence$", "default");

      if (StringUtils.isBlank(pu)) {
        throw new RuntimeException("Invalid persistence.xml, missing persistence unit name.");
      }

      final String configDataSource = String.format("db.%s.datasource", pu);
      final String configDriver = String.format("db.%s.driver", pu);
      final String configUrl = String.format("db.%s.url", pu);
      final String configUser = String.format("db.%s.user", pu);
      final String configPassword = String.format("db.%s.password", pu);

      jndiName = settings.get(configDataSource);
      jdbcDriver = settings.get(configDriver);
      jdbcUrl = settings.get(configUrl);
      jdbcUser = settings.get(configUser);
      jdbcPassword = settings.get(configPassword);

      jdbcBatchSize =
          settings.getInt(AvailableAppSettings.HIBERNATE_JDBC_BATCH_SIZE, DEFAULT_BATCH_SIZE);
      jdbcFetchSize =
          settings.getInt(AvailableAppSettings.HIBERNATE_JDBC_FETCH_SIZE, DEFAULT_FETCH_SIZE);

      cacheMode = evaluate(xpath, XPATH_ROOT, XPATH_SHARED_CACHE_MODE, document);

      if (isBlank(jndiName)) {
        try {
          jdbcBatchSize = Integer.parseInt(evaluate(xpath, XPATH_ROOT, XPATH_BATCH_SIZE, document));
        } catch (Exception e) {
        }
        try {
          jdbcFetchSize = Integer.parseInt(evaluate(xpath, XPATH_ROOT, XPATH_FETCH_SIZE, document));
        } catch (Exception e) {
        }
      }

      if (isBlank(jndiName)) {
        jndiName = evaluate(xpath, XPATH_ROOT, XPATH_NON_JTA_DATA_SOURCE, document);
      }

      if (isBlank(jndiName) && isBlank(jdbcDriver)) {
        jdbcDriver = evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_DRIVER, document);
        jdbcUrl = evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_URL, document);
        jdbcUser = evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_USER, document);
        jdbcPassword = evaluate(xpath, XPATH_ROOT, XPATH_PERSISTENCE_PASSWORD, document);
      }
    } catch (Exception e) {
    }
  }

  /**
   * Get the JDBC connection configured for the application.
   *
   * <p>The connection is independent of JPA connection, so use carefully. It should be used only
   * when JPA context is not available.
   *
   * @return a {@link Connection}
   * @throws NamingException if configured JNDI name can't be resolved
   * @throws SQLException if connection can't be obtained
   * @throws ClassNotFoundException if JDBC driver is not found
   */
  public static Connection getConnection() throws NamingException, SQLException {

    if (!isBlank(jndiName)) {
      final DataSource ds = (DataSource) InitialContext.doLookup(jndiName);
      return ds.getConnection();
    }

    try {
      Class.forName(jdbcDriver);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
  }

  /** Run database migration scripts using flyway migration engine. */
  public static void migrate() {
    final Flyway flyway = new Flyway();
    if (!isBlank(jndiName)) {
      try {
        flyway.setDataSource((DataSource) InitialContext.doLookup(jndiName));
      } catch (NamingException e) {
        throw new FlywayException(e);
      }
    } else {
      flyway.setDataSource(jdbcUrl, jdbcUser, jdbcPassword);
    }
    flyway.migrate();
  }

  public static String getDataSourceName() {
    return jndiName;
  }

  /** Check whether non-jta data source is used. */
  public static boolean isDataSourceUsed() {
    return !isBlank(jndiName);
  }

  /** Check whether shared cache is enabled. */
  public static boolean isCacheEnabled() {
    if (isBlank(cacheMode)) return false;
    if (cacheMode.equals("ALL")) return true;
    if (cacheMode.equals("ENABLE_SELECTIVE")) return true;
    return false;
  }

  /** Whether using oracle database. */
  public static boolean isOracle() {
    return jdbcDriver != null && jdbcDriver.contains("Oracle");
  }

  /** Whether using MySQL database. */
  public static boolean isMySQL() {
    return jdbcDriver != null && jdbcDriver.contains("mysql");
  }

  /**
   * Get the jdbc batch size configured with <code>hibernate.jdbc.batch_size</code> property.
   *
   * @return batch size
   */
  public static int getJdbcBatchSize() {
    return jdbcBatchSize;
  }

  /**
   * Get the jdbc fetch size configured with <code>hibernate.jdbc.fetch_size</code> property.
   *
   * @return batch size
   */
  public static int getJdbcFetchSize() {
    return jdbcFetchSize;
  }

  /** Check whether the database has unaccent support. */
  public static boolean isUnaccentEnabled() {
    if (unaccentSupport == null) {
      try {
        unaccentSupport = testUnaccent();
      } catch (Exception e) {
        unaccentSupport = Boolean.FALSE;
      }
    }
    return unaccentSupport == Boolean.TRUE;
  }

  private static boolean testUnaccent() throws Exception {
    Connection connection = getConnection();
    Statement stmt = connection.createStatement();
    try {
      try {
        stmt.executeQuery(UNACCENT_CHECK);
        return true;
      } catch (Exception e) {
      }
      try {
        stmt.executeUpdate(UNACCENT_CREATE);
        return true;
      } catch (Exception e) {
      }
    } finally {
      stmt.close();
      connection.close();
    }
    return false;
  }
}
