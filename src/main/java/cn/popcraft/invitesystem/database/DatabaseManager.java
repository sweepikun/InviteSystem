package cn.popcraft.invitesystem.database;

import cn.popcraft.invitesystem.InviteSystem;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * 数据库管理器
 */
public class DatabaseManager {
    private final InviteSystem plugin;
    private HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;
    private final String tablePrefix;
    private final Logger logger;
    
    // DAO实例
    private InviteCodeDAO inviteCodeDAO;
    private InvitationDAO invitationDAO;
    private PlayerFirstJoinDAO playerFirstJoinDAO;
    
    // 数据库类型
    private String databaseType;

    public DatabaseManager(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.tablePrefix = plugin.getConfig().getString("database.table-prefix", "");
    }

    public void init() {
        setupDatabase();
        createTables();
        initDAOs();
    }

    private void setupDatabase() {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        this.databaseType = type;
        
        if ("mysql".equalsIgnoreCase(type)) {
            setupMySQL();
        } else {
            setupSQLite();
        }
    }

    private void setupMySQL() {
        try {
            HikariConfig config = new HikariConfig();
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "minecraft");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");
            boolean useSSL = plugin.getConfig().getBoolean("database.mysql.useSSL", false);
            int poolSize = plugin.getConfig().getInt("database.mysql.pool-size", 10);
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            logger.info("MySQL database connected successfully");
        } catch (Exception e) {
            logger.severe("Failed to setup MySQL database: " + e.getMessage());
            // 如果MySQL连接失败，回退到SQLite
            logger.info("Falling back to SQLite database");
            setupSQLite();
        }
    }

    private void setupSQLite() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "invitesystem.db").getAbsolutePath());
            config.setMaximumPoolSize(10);
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            logger.info("SQLite database connected successfully");
        } catch (Exception e) {
            logger.severe("Failed to setup SQLite database: " + e.getMessage());
        }
    }

    public void createTables() {
        // 根据数据库类型选择合适的自增主键语法
        String autoIncrementSyntax = "mysql".equalsIgnoreCase(databaseType) ? 
            "INTEGER PRIMARY KEY AUTO_INCREMENT" : 
            "INTEGER PRIMARY KEY AUTOINCREMENT";
            
        String booleanTrue = "mysql".equalsIgnoreCase(databaseType) ? "TRUE" : "1";
        String booleanFalse = "mysql".equalsIgnoreCase(databaseType) ? "FALSE" : "0";

        String sqlInviteCodes = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "invite_codes (" +
                "code VARCHAR(32) PRIMARY KEY," +
                "creator_uuid CHAR(36) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "expires_at TIMESTAMP NULL," +
                "max_uses INT NOT NULL DEFAULT 1," +
                "used_count INT NOT NULL DEFAULT 0" +
                ");";

        String sqlInvitations = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "invitations (" +
                "id " + autoIncrementSyntax + "," +
                "invitee_uuid CHAR(36) NOT NULL," +
                "inviter_uuid CHAR(36) NOT NULL," +
                "code VARCHAR(32) NOT NULL," +
                "play_time_seconds BIGINT NOT NULL," +
                "claimed_inviter BOOLEAN NOT NULL DEFAULT " + booleanFalse + "," +
                "claimed_invitee BOOLEAN NOT NULL DEFAULT " + booleanFalse + "," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String sqlFirstJoin = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_first_join (" +
                "uuid CHAR(36) PRIMARY KEY," +
                "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        executeUpdate(sqlInviteCodes);
        executeUpdate(sqlInvitations);
        executeUpdate(sqlFirstJoin);
        
        // 创建索引
        createIndexes();
    }
    
    private void createIndexes() {
        String idxInvitee = "CREATE INDEX IF NOT EXISTS idx_invitations_invitee ON " 
                + tablePrefix + "invitations(invitee_uuid)";
        String idxInviter = "CREATE INDEX IF NOT EXISTS idx_invitations_inviter ON " 
                + tablePrefix + "invitations(inviter_uuid)";
        String idxExpires = "CREATE INDEX IF NOT EXISTS idx_invite_codes_expires ON " 
                + tablePrefix + "invite_codes(expires_at)";
                
        executeUpdate(idxInvitee);
        executeUpdate(idxInviter);
        executeUpdate(idxExpires);
    }

    private void executeUpdate(String sql) {
        asyncExecutor.execute(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                handleSqlException(e);
            }
        });
    }

    public Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        throw new SQLException("Database not initialized");
    }

    public void handleSqlException(SQLException e) {
        logger.severe("Database error: " + e.getMessage());
        e.printStackTrace();
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        asyncExecutor.shutdown();
    }
    
    // 初始化DAO
    private void initDAOs() {
        inviteCodeDAO = new InviteCodeDAO(this);
        invitationDAO = new InvitationDAO(this);
        playerFirstJoinDAO = new PlayerFirstJoinDAO(this);
    }
    
    // Getter方法
    public InviteCodeDAO getInviteCodeDAO() {
        return inviteCodeDAO;
    }
    
    public InvitationDAO getInvitationDAO() {
        return invitationDAO;
    }
    
    public PlayerFirstJoinDAO getPlayerFirstJoinDAO() {
        return playerFirstJoinDAO;
    }
    
    public String getDatabaseType() {
        return databaseType;
    }
}