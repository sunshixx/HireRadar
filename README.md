# HireRadar 职投雷达

地图发现附近企业 + 投递链接聚合 + 按需工商详情。

## 项目特色
- 附近企业检索：基于高德/OSM，支持半径与关键词，按距离排序。
- 一键投递链接：聚合站点模板（示例含牛客/OfferShow/搜索引擎/官网），点击即跳转投递页。
- 按需工商详情：列表点击时再查企查查，避免批量调用造成限流与成本。
- 每日自动刷新：定时任务预聚合热门公司投递链接，加速首次展示。
- 坐标对齐：内置 WGS‑84 ↔ GCJ‑02 转换，前端地图精准落点。
- 统一接口：`/api/companies/nearby`、`/api/companies/enrich`、`/api/companies/jobs`。
- 前端展示：Leaflet 地图 + 列表联动，支持数据源切换（高德/OSM）。

## 快速开始
- 配置密钥：在 `src/main/resources/application.properties` 填入 `map.amap.key`（高德）与可选的 `qcc.api.key`、`qcc.api.token`（企查查）。
- 运行（Windows）：`./mvnw.cmd -s .mvn/settings.xml spring-boot:run`
- 访问：`http://localhost:8080/`

> 说明：本仓库包含简单爬取模板用于演示，请按目标站点条款与 `robots.txt` 合理配置与使用。