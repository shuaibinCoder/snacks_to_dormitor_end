package com.tencent.wxcloudrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.w3c.dom.*;
import javax.sql.DataSource;
import javax.xml.parsers.*;
import java.io.File;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 不依赖数据库的结构检查；真实 SQL 和并发行为由 mysql-it 验证。 */
class PersistenceMappingTest {
  private static final String[] MAPPERS = {"User", "PlatformUser", "MerchantApplication", "MerchantPayment",
      "Store", "Product", "Order", "AuditLog", "SystemDictionary", "SystemCollectionSchema", "Notification",
      "NotificationDelivery", "NotificationPreference", "NotificationGrant", "NotificationTemplate"};

  /** 默认配置不装配 Flyway，也不触发 Spring SQL 脚本的数据源访问。 */
  @Test
  void defaultConfigurationDisablesAutomaticDatabaseInitialization() throws Exception {
    PropertySource<?> config = new YamlPropertySourceLoader()
        .load("application", new FileSystemResource("src/main/resources/application.yml")).get(0);
    assertEquals(false, config.getProperty("spring.flyway.enabled"));
    assertEquals("never", config.getProperty("spring.sql.init.mode"));
    assertEquals(false, config.getProperty("snacks.initialize-on-startup"));
    DataSource datasource = mock(DataSource.class);
    new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(config))
        .withBean(DataSource.class, () -> datasource)
        .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class))
        .run(context -> {
          assertNull(context.getStartupFailure());
          assertTrue(context.getBeansOfType(Flyway.class).isEmpty());
          verifyNoInteractions(datasource);
        });
  }

  @Test
  void everyBusinessTableHasEntityMapperAndMatchingXmlStatements() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    for (String name : MAPPERS) {
      Class<?> entity = Class.forName("com.tencent.wxcloudrun.model." + name);
      assertTrue(BusinessEntity.class.isAssignableFrom(entity), name + " 必须继承公共实体");
      Class<?> mapper = Class.forName("com.tencent.wxcloudrun.dao." + name + "Mapper");
      assertTrue(mapper.isInterface());
      File file = new File("src/main/resources/mapper/" + name + "Mapper.xml");
      assertTrue(file.isFile(), file.getPath());
      Document xml = factory.newDocumentBuilder().parse(file);
      assertEquals(mapper.getName(), xml.getDocumentElement().getAttribute("namespace"));
      Element resultMap = (Element) xml.getElementsByTagName("resultMap").item(0);
      assertNotNull(resultMap, name + " 缺少 resultMap");
      Map<String, Element> mappings = new HashMap<>();
      for (String tag : Arrays.asList("id", "result")) {
        NodeList fields = resultMap.getElementsByTagName(tag);
        for (int i = 0; i < fields.getLength(); i++) {
          Element mapping = (Element) fields.item(i);
          mappings.put(mapping.getAttribute("property"), mapping);
        }
      }
      for (Class<?> type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
        for (Field field : type.getDeclaredFields()) {
          if (Modifier.isStatic(field.getModifiers())) {
            continue;
          }
          Element mapping = mappings.get(field.getName());
          assertNotNull(mapping, name + " 未映射字段 " + field.getName());
          if (List.class.isAssignableFrom(field.getType()) || Data.class.equals(field.getType())) {
            assertTrue(mapping.getAttribute("typeHandler").contains("TypeHandler"),
                name + " JSON 字段缺少 TypeHandler: " + field.getName());
          }
        }
      }
      Set<String> statements = new HashSet<>();
      NodeList nodes = xml.getDocumentElement().getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        if (node instanceof Element
            && Arrays.asList("select", "insert", "update", "delete").contains(((Element) node).getTagName())) {
          statements.add(((Element) node).getAttribute("id"));
        }
      }
      for (Method method : mapper.getDeclaredMethods()) {
        assertTrue(statements.contains(method.getName()), name + " 缺少 " + method.getName());
      }
      String content = java.nio.file.Files.readAllBytes(file.toPath()).length == 0
          ? ""
          : new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
      assertFalse(content.contains("${"), "XML 不允许拼接客户端 SQL 片段");
    }
    assertEquals(15, MAPPERS.length);
  }

  @Test
  void entityJsonAndDtoDecorationKeepProtocolNames() throws Exception {
    User user = new User();
    user.setOpenid("user");
    user.setRoles(Collections.singletonList("CUSTOMER"));
    user.setStatus("ACTIVE");
    Entities.create(user);
    Data data = new EntityView(new ObjectMapper(), new Labels(new ObjectMapper())).data(user);
    assertTrue(data.containsKey("_id"));
    assertFalse(data.containsKey("id"));
    assertEquals(Collections.singletonList("普通用户"), data.get("roleLabels"));
    assertTrue(data.get("createdAt") instanceof Number);
  }

  @Test
  void jacksonHandlersRoundTripNullEmptyAndMalformedValues() throws Exception {
    StringListTypeHandler handler = new StringListTypeHandler();
    PreparedStatement statement = mock(PreparedStatement.class);
    handler.setNonNullParameter(statement, 1, Arrays.asList("CUSTOMER", "MERCHANT_OWNER"), null);
    verify(statement).setString(1, "[\"CUSTOMER\",\"MERCHANT_OWNER\"]");
    ResultSet result = mock(ResultSet.class);
    when(result.getString("roles")).thenReturn("[\"CUSTOMER\",\"MERCHANT_OWNER\"]");
    assertEquals(Arrays.asList("CUSTOMER", "MERCHANT_OWNER"), handler.getNullableResult(result, "roles"));
    when(result.getString("roles")).thenReturn(null);
    assertNull(handler.getNullableResult(result, "roles"));
    when(result.getString("roles")).thenReturn("");
    assertNull(handler.getNullableResult(result, "roles"));
    when(result.getString("roles")).thenReturn("not-json");
    assertThrows(SQLException.class, () -> handler.getNullableResult(result, "roles"));
  }

  /** 显式启用才读取本地凭据；只查询元数据，绝不启动应用、迁移或写入业务库。 */
  @Test
  @EnabledIfSystemProperty(named = "tests.local-schema-check", matches = "true")
  void localDatabaseMatchesMigrationBeforeBaseline() throws Exception {
    PropertySource<?> local = new YamlPropertySourceLoader()
        .load("local", new FileSystemResource("application-local.yml")).get(0);
    String migration = java.nio.file.Files.readString(
        new File("src/main/resources/db/migration/V1__snacks_business.sql").toPath());
    Matcher tables = Pattern.compile("CREATE TABLE `([^`]+)` \\((.*?)\\) ENGINE=InnoDB", Pattern.DOTALL)
        .matcher(migration);
    List<String> differences = new ArrayList<>();
    int checked = 0;
    try (Connection connection = DriverManager.getConnection(
        (String) local.getProperty("spring.datasource.url"),
        (String) local.getProperty("spring.datasource.username"),
        (String) local.getProperty("spring.datasource.password"))) {
      connection.setReadOnly(true);
      while (tables.find()) {
        String table = tables.group(1);
        Map<String, String> expectedColumns = new TreeMap<>();
        Map<String, String> expectedIndexes = new TreeMap<>();
        for (String line : tables.group(2).split("\\R")) {
          Matcher column = Pattern.compile("^\\s*`([^`]+)` ([A-Z]+(?:\\([0-9,]+\\))?).*").matcher(line);
          if (column.matches()) {
            Matcher defaultValue = Pattern.compile(" DEFAULT ('[^']*'|[0-9]+)").matcher(line);
            String value = defaultValue.find() ? defaultValue.group(1).replace("'", "") : null;
            String type = column.group(2).toLowerCase(Locale.ROOT);
            if (value != null && type.startsWith("decimal")) {
              value = new java.math.BigDecimal(value).stripTrailingZeros().toPlainString();
            }
            String collation = type.startsWith("varchar") || type.equals("text")
                ? line.contains("CHARACTER SET ascii") ? "ascii_bin" : "utf8mb4_unicode_ci" : null;
            expectedColumns.put(column.group(1), type + "|" + !line.contains("NOT NULL")
                + "|" + value + "|" + collation);
            if (line.contains("PRIMARY KEY")) {
              expectedIndexes.put("PRIMARY", "unique:" + column.group(1));
            }
          }
          Matcher index = Pattern.compile("^\\s*(UNIQUE )?KEY `([^`]+)` \\(([^)]+)\\),?\\s*$")
              .matcher(line);
          if (index.matches()) {
            expectedIndexes.put(index.group(2), (index.group(1) == null ? "index:" : "unique:")
                + index.group(3).replace("`", "").replace(" ", ""));
          }
        }
        Map<String, String> actualColumns = new TreeMap<>();
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,COLLATION_NAME "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
          query.setString(1, table);
          try (ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
              String type = rows.getString("COLUMN_TYPE").toLowerCase(Locale.ROOT)
                  .replaceAll("^(bigint|tinyint)\\([0-9]+\\)$", "$1");
              String value = rows.getString("COLUMN_DEFAULT");
              if (value != null && type.startsWith("decimal")) {
                value = new java.math.BigDecimal(value).stripTrailingZeros().toPlainString();
              }
              actualColumns.put(rows.getString("COLUMN_NAME"), type
                  + "|" + "YES".equals(rows.getString("IS_NULLABLE"))
                  + "|" + value + "|" + rows.getString("COLLATION_NAME"));
            }
          }
        }
        Map<String, String> actualIndexes = new TreeMap<>();
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT INDEX_NAME,NON_UNIQUE,COLUMN_NAME,SUB_PART FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER BY INDEX_NAME,SEQ_IN_INDEX")) {
          query.setString(1, table);
          try (ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
              String name = rows.getString("INDEX_NAME");
              String column = rows.getString("COLUMN_NAME");
              if (rows.getObject("SUB_PART") != null) {
                column += "(" + rows.getInt("SUB_PART") + ")";
              }
              String prefix = rows.getBoolean("NON_UNIQUE") ? "index:" : "unique:";
              actualIndexes.put(name, actualIndexes.containsKey(name)
                  ? actualIndexes.get(name) + "," + column : prefix + column);
            }
          }
        }
        if (!expectedColumns.equals(actualColumns)) {
          differences.add(table + " 字段不一致：预期=" + expectedColumns + "，实际=" + actualColumns);
        }
        if (!expectedIndexes.equals(actualIndexes)) {
          differences.add(table + " 索引不一致：预期=" + expectedIndexes + "，实际=" + actualIndexes);
        }
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT ENGINE,TABLE_COLLATION FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
          query.setString(1, table);
          try (ResultSet rows = query.executeQuery()) {
            if (!rows.next() || !"InnoDB".equals(rows.getString("ENGINE"))
                || !"utf8mb4_unicode_ci".equals(rows.getString("TABLE_COLLATION"))) {
              differences.add(table + " 存储引擎或表字符集不一致");
            }
          }
        }
        checked++;
      }
    }
    assertEquals(15, checked, "必须核对全部业务表");
    assertTrue(differences.isEmpty(), String.join("\n", differences));
  }

  @Test
  void migrationContainsTableAndColumnComments() throws Exception {
    String sql = new String(java.nio.file.Files.readAllBytes(
        new File("src/main/resources/db/migration/V1__snacks_business.sql").toPath()),
        java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(15, sql.split("CREATE TABLE", -1).length - 1);
    assertEquals(15, sql.split("COMMENT='", -1).length - 1, "每张表都必须有用途注释");
    assertFalse(sql.contains("仅由云托管业务服务按字段白名单读写"), "字段注释不能停留在通用兜底文本");
    for (String line : sql.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("CREATE TABLE") || trimmed.startsWith(") ENGINE=")) {
        assertTrue(trimmed.contains("COMMENT=") || trimmed.startsWith("CREATE TABLE"));
      }
      if (trimmed.startsWith("`")) {
        assertTrue(trimmed.contains(" COMMENT '"), trimmed);
      }
    }
  }
}
