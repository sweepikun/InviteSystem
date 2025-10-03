# InviteSystem 邀请系统插件

一个功能完整的Minecraft服务器邀请系统插件，支持创建邀请码、奖励发放、防作弊等特性。

## 功能特性

- **邀请码系统**：玩家可以创建自定义或随机生成的邀请码
- **双向奖励机制**：邀请人和被邀请人都能获得奖励
- **延迟奖励发放**：奖励不会自动发放，需要玩家手动领取
- **多数据库支持**：支持SQLite和MySQL数据库
- **防作弊机制**：IP限制、在线时间验证、冷却时间等
- **灵活的奖励配置**：支持原版物品、ItemsAdder物品、Vault经济、PlayerPoints点券
- **命令自动补全**：提供完整的命令Tab补全支持

## 系统要求

- Minecraft 1.16+
- Spigot/Paper 服务端
- Vault 插件（必需）
- PlayerPoints 插件（可选，用于点券奖励）
- ItemsAdder 插件（可选，用于自定义物品奖励）

## 安装方法

1. 将 `InviteSystem.jar` 文件放入服务器的 `plugins` 文件夹
2. 启动服务器以生成配置文件
3. 根据需要修改 `config.yml` 配置文件
4. 重启服务器使配置生效

## 配置说明

### 数据库配置

```yaml
database:
  # 类型: sqlite 或 mysql
  type: sqlite

  # MySQL 配置（仅当 type: mysql 时生效）
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "your_password"
    useSSL: false
    pool-size: 10
```

### 邀请规则

```yaml
invite:
  # 新玩家必须达到的最小累计在线时间才能提交邀请码
  min-play-time: "10m"

  # 邀请码默认长度（仅用于随机生成）
  code-length: 8

  # 邀请码是否区分大小写
  case-sensitive: true

  # 邀请码有效期
  code-expire-after: "7d"

  # 每个邀请码最多可被使用次数
  max-uses-per-code: 1

  # 是否仅允许从未使用过邀请码的玩家提交
  only-allow-first-time-users: true

  # 同一 IP 地址在 24 小时内最多允许多少个被邀请人
  max-invitees-per-ip: 3
  ip-cooldown-hours: 24
```

### 奖励配置

```yaml
rewards:
  # 邀请人获得的奖励
  inviter:
    - type: "ITEM"
      material: "diamond"
      amount: 5
      name: "&b&l邀请奖励"
      lore:
        - "&7感谢你为服务器带来新朋友！"
    - type: "VAULT"
      amount: 500
    - type: "PLAYERPOINTS"
      amount: 100
    - type: "ITEMSADDER"
      id: "customitems:invite_token"
      amount: 2

  # 被邀请人获得的奖励
  invitee:
    - type: "ITEM"
      material: "bread"
      amount: 64
    - type: "ITEM"
      material: "iron_ingot"
      amount: 16
    - type: "VAULT"
      amount: 200
    - type: "PLAYERPOINTS"
      amount: 50
    - type: "ITEMSADDER"
      id: "customitems:welcome_kit"
      amount: 1
```

## 命令列表

| 命令 | 权限 | 说明 |
|------|------|------|
| `/invite create [code]` | `invite.create` | 创建邀请码（无参数则随机生成） |
| `/invite submit <code>` | 无 | 提交邀请码 |
| `/invite claim` | 无 | 领取奖励 |
| `/invite info` | 无 | 查看自己的邀请信息 |
| `/invite list [page]` | `invite.admin` | 查看所有邀请记录 |
| `/invite reload` | `invite.admin` | 重新加载配置文件 |

## 权限节点

- `invite.use` - 使用邀请系统命令（默认：所有玩家）
- `invite.create` - 创建邀请码（默认：OP）
- `invite.admin` - 管理邀请系统（默认：OP）

## 开发构建

本项目使用Gradle作为构建工具：

```bash
# 构建项目
./gradlew build

# 清理构建
./gradlew clean

# 运行测试
./gradlew test
```

## 许可证

本项目采用 MIT 许可证，详情请查看 [LICENSE](LICENSE) 文件。